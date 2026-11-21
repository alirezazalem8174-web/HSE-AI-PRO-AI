package com.ekbatan.hse;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;

/**
 * Application service boundary. Long-running or cross-feature operations live here so the
 * activity remains a composition/lifecycle host rather than an operational service locator.
 */
public final class HseAppServices implements AutoCloseable {
    private final Activity activity;
    private final HseDataRepository data;
    private HseVisionAi visionAi;

    public HseAppServices(Activity activity, HseDataRepository data) {
        if (activity == null) throw new IllegalArgumentException("activity == null");
        if (data == null) throw new IllegalArgumentException("data == null");
        this.activity = activity;
        this.data = data;
    }

    public PdfImporter.Result importPdf(Uri uri) throws Exception {
        if (uri == null) throw new IllegalArgumentException("uri == null");
        String name = "منبع PDF";
        android.database.Cursor c = activity.getContentResolver().query(uri,
                new String[]{"_display_name"}, null, null, null);
        if (c != null) {
            try { if (c.moveToFirst()) name = c.getString(0); }
            finally { c.close(); }
        }
        return PdfImporter.importPdf(activity, uri, dataDatabase(), name);
    }

    /** Returns the underlying DB only inside this service boundary for legacy importer compatibility. */
    private LibraryDb dataDatabase() {
        return data.rawDatabaseForService();
    }

    public File createBackup() throws Exception {
        return BackupManager.create(activity, dataDatabase());
    }

    public void saveBackup(File source, Uri destination) throws Exception {
        if (source == null || !source.isFile()) throw new IOException("فایل پشتیبان پیدا نشد");
        if (destination == null) throw new IllegalArgumentException("destination == null");
        try (InputStream in = new java.io.FileInputStream(source);
             OutputStream out = activity.getContentResolver().openOutputStream(destination)) {
            if (out == null) throw new IOException("مسیر ذخیره قابل نوشتن نیست");
            byte[] b = new byte[32768]; int n;
            while ((n = in.read(b)) != -1) out.write(b, 0, n);
            out.flush();
        }
    }

    public File latestBackup() throws IOException {
        File[] files = activity.getCacheDir().listFiles((d, n) ->
                n.startsWith("HSE_AI_PRO_Backup_") && n.endsWith(".zip"));
        if (files == null || files.length == 0) throw new IOException("پشتیبان پیدا نشد");
        Arrays.sort(files, (a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return files[0];
    }

    public void restoreBackup(File stagedZip) throws Exception {
        if (stagedZip == null || !stagedZip.isFile()) throw new IOException("فایل بازیابی پیدا نشد");
        BackupManager.restore(activity, stagedZip);
    }

    public boolean canScheduleReminders() {
        return (Build.VERSION.SDK_INT < 33 ||
                activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED)
                && ReminderScheduler.canScheduleExact(activity);
    }

    public void scheduleReminder(LibraryDb.ReminderRow reminder) {
        if (reminder == null) throw new IllegalArgumentException("reminder == null");
        if (!canScheduleReminders()) return;
        ReminderScheduler.schedule(activity, reminder);
    }

    public void scheduleReminder(long id, String title, String body, long at) {
        if (!canScheduleReminders()) return;
        ReminderScheduler.schedule(activity, id, title, body, at);
    }

    public void cancelReminder(long id) {
        ReminderScheduler.cancel(activity, id);
    }

    public void reschedulePendingReminders() {
        if (!canScheduleReminders()) return;
        for (LibraryDb.ReminderRow r : data.pendingReminders(System.currentTimeMillis())) {
            ReminderScheduler.schedule(activity, r);
        }
    }

    public String analyzeImage(File image, HseVisionAi.Progress progress) throws Exception {
        if (image == null || !image.isFile()) throw new IOException("تصویر پیدا نشد");
        if (visionAi == null) visionAi = new HseVisionAi(activity);
        return visionAi.analyzeImage(image, progress);
    }

    public HseVisionAi visionAi() { return visionAi; }

    @Override public void close() {
        HseVisionAi ai = visionAi;
        visionAi = null;
        if (ai != null) try { ai.close(); } catch (Exception ignored) {}
    }
}
