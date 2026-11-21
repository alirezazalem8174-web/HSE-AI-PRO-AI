package com.ekbatan.hse;

import android.app.ActivityManager;
import android.content.Context;
import android.os.StatFs;
import android.os.Looper;
import android.graphics.BitmapFactory;

import com.google.ai.edge.litertlm.Backend;
import com.google.ai.edge.litertlm.Content;
import com.google.ai.edge.litertlm.Contents;
import com.google.ai.edge.litertlm.Conversation;
import com.google.ai.edge.litertlm.Engine;
import com.google.ai.edge.litertlm.EngineConfig;
import com.google.ai.edge.litertlm.Message;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Completely local visual HSE assistant.
 * The model is downloaded once, verified, and then inference happens on-device.
 */
public final class HseVisionAi implements AutoCloseable {
    public interface Progress { void onProgress(String message, int percent); }

    private static final String MODEL_FILE = "gemma-4-E2B-it.litertlm";
    private static final String MODEL_URL =
            "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/"
                    + "6e5c4f1e395deb959c494953478fa5cec4b8008f/" + MODEL_FILE;
    private static final long MODEL_BYTES = 2588147712L;
    private static final String MODEL_SHA256 =
            "181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c";
    private static final String MODEL_OK = MODEL_FILE + ".verified";
    private static final long MIN_RAM_BYTES = 8L * 1024L * 1024L * 1024L;
    private static final long MIN_FREE_BYTES = 3L * 1024L * 1024L * 1024L;

    private final Context context;
    private final Object lock = new Object();
    private Engine engine;
    private boolean closed;

    public HseVisionAi(Context context) {
        this.context = context.getApplicationContext();
    }

    public static String modelDescription() {
        return "Gemma 4 E2B — تحلیل تصویر روی خود گوشی، بدون API پولی";
    }

    public static boolean isModelInstalled(Context c) {
        File f = modelFile(c);
        return f.isFile() && f.length() == MODEL_BYTES;
    }

    public static long modelSizeBytes() { return MODEL_BYTES; }

    public static boolean deviceSuitable(Context c) {
        ActivityManager am = (ActivityManager) c.getSystemService(Context.ACTIVITY_SERVICE);
        if (am == null) return false;
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        return mi.totalMem >= MIN_RAM_BYTES;
    }

    private static File modelDir(Context c) {
        File base = c.getExternalFilesDir("ai_models");
        if (base == null) base = new File(c.getFilesDir(), "ai_models");
        if (!base.exists() && !base.mkdirs()) throw new IllegalStateException("ساخت پوشه مدل هوش مصنوعی ناموفق بود");
        return base;
    }

    private static File modelFile(Context c) { return new File(modelDir(c), MODEL_FILE); }
    private static File partFile(Context c) { return new File(modelDir(c), MODEL_FILE + ".part"); }
    private static File verifiedFile(Context c) { return new File(modelDir(c), MODEL_OK); }
    private static File cacheDir(Context c) {
        File d = new File(modelDir(c), "cache");
        if (!d.exists() && !d.mkdirs()) throw new IllegalStateException("ساخت پوشه کش هوش مصنوعی ناموفق بود");
        return d;
    }

    public void ensureModel(Progress progress) throws Exception {
        synchronized (lock) {
            ensureOpen();
            if (!deviceSuitable(context)) {
                throw new IOException("این مدل برای دستگاه‌هایی با حداقل حدود ۸ گیگابایت RAM طراحی شده است.");
            }
            File model = modelFile(context);
            File verified = verifiedFile(context);
            if (model.isFile() && model.length() == MODEL_BYTES && isVerifiedMarkerValid(model, verified)) {
                if (progress != null) progress.onProgress("مدل هوش مصنوعی آماده است.", 100);
                return;
            }
            if (model.isFile() && model.length() == MODEL_BYTES) {
                if (!MODEL_SHA256.equalsIgnoreCase(sha256(model))) {
                    if (!model.delete()) throw new IOException("حذف مدل نامعتبر ناموفق بود");
                    verified.delete();
                } else {
                    writeVerifiedMarker(model, verified);
                    if (progress != null) progress.onProgress("مدل هوش مصنوعی آماده است.", 100);
                    return;
                }
            }
            if (model.exists() && !model.delete()) throw new IOException("حذف مدل ناقص قبلی ناموفق بود");
            File part = partFile(context);
            long existing = part.isFile() ? part.length() : 0L;
            StatFs stat = new StatFs(modelDir(context).getAbsolutePath());
            long free = stat.getAvailableBytes();
            if (free < MIN_FREE_BYTES || free < MODEL_BYTES - existing + 256L * 1024L * 1024L) {
                throw new IOException("فضای خالی کافی برای مدل هوش مصنوعی وجود ندارد؛ حداقل حدود ۳.۰ گیگابایت فضای آزاد لازم است.");
            }
            download(part, existing, progress);
            if (part.length() != MODEL_BYTES) throw new IOException("حجم مدل دانلودشده صحیح نیست");
            if (!MODEL_SHA256.equalsIgnoreCase(sha256(part))) {
                part.delete();
                throw new IOException("صحت مدل هوش مصنوعی تأیید نشد؛ دانلود دوباره لازم است.");
            }
            if (!part.renameTo(model)) {
                copyFile(part, model);
                if (!model.isFile() || model.length() != MODEL_BYTES) throw new IOException("نصب مدل ناموفق بود");
                part.delete();
            }
            writeVerifiedMarker(model, verifiedFile(context));
            if (progress != null) progress.onProgress("مدل با موفقیت نصب و بررسی شد.", 100);
        }
    }

    private void download(File part, long existing, Progress progress) throws Exception {
        HttpURLConnection con = null;
        try {
            URL url = new URL(MODEL_URL);
            con = (HttpURLConnection) url.openConnection();
            con.setConnectTimeout(20000);
            con.setReadTimeout(60000);
            con.setInstanceFollowRedirects(true);
            con.setRequestProperty("User-Agent", "HSE-AI-PRO/4.6.5");
            if (existing > 0) con.setRequestProperty("Range", "bytes=" + existing + "-");
            int code = con.getResponseCode();
            boolean append = existing > 0 && code == HttpURLConnection.HTTP_PARTIAL;
            if (code != HttpURLConnection.HTTP_OK && !append) {
                throw new IOException("دریافت مدل از منبع رسمی ناموفق بود؛ کد HTTP " + code);
            }
            long start = append ? existing : 0L;
            if (!append && existing > 0) {
                if (!part.delete()) throw new IOException("پاک‌سازی دانلود ناقص ناموفق بود");
            }
            long total = start + Math.max(0L, con.getContentLengthLong());
            if (total > 0 && total < MODEL_BYTES) {
                // Content-Length can be absent on redirects/range proxies; do not reject prematurely.
            }
            try (InputStream in = new BufferedInputStream(con.getInputStream());
                 OutputStream out = new BufferedOutputStream(new FileOutputStream(part, append), 1024 * 1024)) {
                byte[] buf = new byte[1024 * 1024];
                long done = start;
                int last = -1;
                int n;
                while ((n = in.read(buf)) != -1) {
                    done += n;
                    if (done > MODEL_BYTES) throw new IOException("حجم مدل بیش از مقدار مورد انتظار است");
                    out.write(buf, 0, n);
                    int pct = (int) Math.min(99L, done * 100L / MODEL_BYTES);
                    if (pct != last && progress != null) {
                        last = pct;
                        progress.onProgress("در حال دریافت مدل هوش مصنوعی… " + pct + "%", pct);
                    }
                }
                out.flush();
            }
        } finally {
            if (con != null) con.disconnect();
        }
    }

    public String analyzeImage(File image, Progress progress) throws Exception {
        if (image == null || !image.isFile()) throw new IOException("تصویر قابل خواندن نیست");
        if (Looper.myLooper() == Looper.getMainLooper()) throw new IllegalStateException("تحلیل هوش مصنوعی باید خارج از Thread اصلی اجرا شود");
        validateImage(image);
        synchronized (lock) {
            ensureOpen();
            ensureModel(progress);
            if (progress != null) progress.onProgress("در حال آماده‌سازی موتور هوش مصنوعی…", 0);
            ensureEngine(progress);
            if (progress != null) progress.onProgress("در حال تحلیل تصویر HSE…", 0);

            // A fresh conversation is intentional: it keeps each image analysis isolated and
            // avoids growing the KV cache between unrelated photos.
            try (Conversation conversation = engine.createConversation()) {
                String prompt =
                        "تو بازرس تصویری تخصصی HSE در یک محیط صنعتی هستی. این تصویر را مانند یک چک‌لیست بازرسی میدانی بررسی کن. "
                                + "مدل پایه عمومی است و تو نباید ادعا کنی که تصویر را با استاندارد یا قانون خاصی تطبیق داده‌ای مگر اینکه خود تصویر مدرک کافی داشته باشد. "
                                + "فقط بر اساس شواهد قابل مشاهده نتیجه بده؛ چیزی را که دیده نمی‌شود حدس نزن. "
                                + "«دیده نمی‌شود» به معنی «وجود ندارد» نیست. "
                                + "اگر تصویر برای قضاوت کافی نیست، صریحاً «قابل تشخیص نیست» بنویس.\n\n"
                                + HseHazardKnowledge.inspectionRubric() + "\n"
                                + HseHazardKnowledge.outputContract() + "\n"
                                + "پاسخ را فارسی، کوتاه اما عملیاتی و مناسب گزارش HSE بنویس. اگر هیچ ناایمنی مشخصی مشاهده نشد، همین موضوع را اعلام کن و از ساختن مورد فرضی خودداری کن.";
                Contents contents = Contents.of(new Content.ImageFile(image.getAbsolutePath()), new Content.Text(prompt));
                Message response = conversation.sendMessage(contents);
                return response == null ? "پاسخی از مدل دریافت نشد." : response.toString().trim();
            }
        }
    }

    private static void validateImage(File image) throws IOException {
        if (image.length() <= 0 || image.length() > 20L * 1024L * 1024L) {
            throw new IOException("حجم تصویر باید بین ۱ بایت و ۲۰MB باشد.");
        }
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(image.getAbsolutePath(), o);
        if (o.outWidth <= 0 || o.outHeight <= 0) throw new IOException("فرمت یا محتوای تصویر قابل تشخیص نیست.");
        if ((long)o.outWidth * (long)o.outHeight > 40_000_000L) {
            throw new IOException("ابعاد تصویر بیش از حد بزرگ است؛ لطفاً تصویر را کوچک‌تر کنید.");
        }
    }

    private void ensureEngine(Progress progress) throws Exception {
        if (engine != null && engine.isInitialized()) return;
        File model = modelFile(context);
        Exception first = null;
        try {
            if (progress != null) progress.onProgress("فعال‌سازی موتور GPU…", 0);
            EngineConfig gpu = new EngineConfig(model.getAbsolutePath(), new Backend.GPU(), new Backend.GPU(), null, null, null, cacheDir(context).getAbsolutePath(), null, null);
            Engine e = new Engine(gpu);
            e.initialize();
            engine = e;
            return;
        } catch (Exception e) {
            first = e;
        }
        // CPU fallback is deliberate. A phone-specific GPU delegate failure must not crash HSE AI PRO.
        try {
            if (progress != null) progress.onProgress("GPU در این دستگاه در دسترس نبود؛ فعال‌سازی CPU…", 0);
            EngineConfig cpu = new EngineConfig(model.getAbsolutePath(), new Backend.CPU(), new Backend.CPU(), null, null, null, cacheDir(context).getAbsolutePath(), null, null);
            Engine e = new Engine(cpu);
            e.initialize();
            engine = e;
        } catch (Exception second) {
            throw new IOException("راه‌اندازی موتور هوش مصنوعی ناموفق بود: " + message(second), first);
        }
    }

    private static String message(Throwable t) {
        String m = t == null ? null : t.getMessage();
        return m == null || m.trim().isEmpty() ? "خطای ناشناخته" : m;
    }

    private static void copyFile(File src, File dst) throws IOException {
        File tmp = new File(dst.getParentFile(), dst.getName() + ".installing");
        try (InputStream in = new BufferedInputStream(new FileInputStream(src), 1024 * 1024);
             OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp), 1024 * 1024)) {
            byte[] b = new byte[1024 * 1024]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            out.flush();
        }
        if (!tmp.renameTo(dst)) throw new IOException("کپی نهایی مدل ناموفق بود");
    }


    private static boolean isVerifiedMarkerValid(File model, File marker) {
        if (!marker.isFile()) return false;
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(marker), 256)) {
            byte[] b = new byte[256]; int n = in.read(b);
            if (n <= 0) return false;
            String s = new String(b, 0, n, java.nio.charset.StandardCharsets.UTF_8).trim();
            return s.equals(MODEL_SHA256 + "|" + model.length() + "|" + model.lastModified());
        } catch (Exception ignored) { return false; }
    }

    private static void writeVerifiedMarker(File model, File marker) throws IOException {
        File tmp = new File(marker.getParentFile(), marker.getName() + ".part");
        String s = MODEL_SHA256 + "|" + model.length() + "|" + model.lastModified();
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp), 256)) {
            out.write(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            out.flush();
        }
        if (!tmp.renameTo(marker)) throw new IOException("ثبت وضعیت صحت مدل ناموفق بود");
    }

    private static String sha256(File f) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new BufferedInputStream(new FileInputStream(f), 1024 * 1024)) {
            byte[] b = new byte[1024 * 1024]; int n;
            while ((n = in.read(b)) != -1) md.update(b, 0, n);
        }
        StringBuilder sb = new StringBuilder(64);
        for (byte x : md.digest()) sb.append(String.format(Locale.ROOT, "%02x", x));
        return sb.toString();
    }

    private void ensureOpen() {
        if (closed) throw new IllegalStateException("موتور هوش مصنوعی بسته شده است");
    }

    @Override public void close() {
        synchronized (lock) {
            closed = true;
            if (engine != null) {
                try { engine.close(); } catch (Exception ignored) {}
                engine = null;
            }
        }
    }
}
