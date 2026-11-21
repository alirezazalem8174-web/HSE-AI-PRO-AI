package com.ekbatan.hse;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.io.File;
import java.util.ArrayList;

/** Local multimodal HSE inspection feature with human-approved finding/risk workflow. */
public final class VisionAiFeature implements HseFeature {
    private final MainActivity host;
    private LinearLayout findingsList;
    public VisionAiFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "vision-ai";}
    @Override public String title(){return "بازرس هوشمند HSE";}
    @Override public String icon(){return "🤖";}
    @Override public View build(){
        LinearLayout c=host.card();c.addView(host.sectionHead(icon(),title()));
        c.addView(host.text("مسیر کامل: تصویر → تحلیل HSE → تأیید انسانی → ثبت یافته → پیشنهاد ۵×۵ با تأیید کاربر → گزارش بازرسی → PDF",13,host.MUTED));host.gapIn(c,8);
        TextView info=host.text(HseVisionAi.modelDescription()+"\nلایه بازرسی HSE روی مدل؛ حدود ۲.۶ گیگابایت؛ فقط در اولین استفاده دانلود می‌شود؛ تحلیل روی دستگاه و بدون API پولی انجام می‌شود.",13,host.INK);c.addView(info);host.gapIn(c,8);
        LinearLayout row=new LinearLayout(host);row.setOrientation(LinearLayout.HORIZONTAL);Button pick=host.action("🖼️ انتخاب عکس",host.BLUE),analyze=host.action("🔍 تحلیل ناایمنی",host.GREEN);analyze.setEnabled(false);
        row.addView(pick,new LinearLayout.LayoutParams(0,host.dp(48),1));row.addView(new Space(host),new LinearLayout.LayoutParams(host.dp(8),1));row.addView(analyze,new LinearLayout.LayoutParams(0,host.dp(48),1));c.addView(row);
        TextView status=host.text("ابتدا یک عکس از محیط کار انتخاب کنید.",13,host.MUTED);status.setPadding(0,host.dp(10),0,0);c.addView(status);
        TextView result=host.text("",13,host.INK);result.setPadding(0,host.dp(12),0,0);c.addView(result);
        LinearLayout reportRow=new LinearLayout(host);reportRow.setOrientation(LinearLayout.HORIZONTAL);Button report=host.outline("📄 گزارش حرفه‌ای HSE + PDF");reportRow.addView(report,new LinearLayout.LayoutParams(-1,host.dp(46)));c.addView(reportRow);host.gapIn(c,8);
        findingsList=new LinearLayout(host);findingsList.setOrientation(LinearLayout.VERTICAL);c.addView(findingsList);
        host.aiImageReadyAction=()->analyze.setEnabled(host.pendingAiImageFile!=null&&host.pendingAiImageFile.isFile());
        pick.setOnClickListener(v->host.pick(host.PICK_AI_IMAGE,"image/*"));
        analyze.setOnClickListener(v->{File image=host.pendingAiImageFile;if(image==null||!image.isFile()){host.toast("ابتدا یک تصویر انتخاب کنید.");return;}analyze.setEnabled(false);pick.setEnabled(false);result.setText("");host.submitIo(()->{try{String x=host.analyzeVisionImage(image,(m,pct)->host.postUi(()->status.setText(m)));ArrayList<HseAiFinding> fs=HseAiFinding.parse(x);host.postUi(()->{status.setText("تحلیل تکمیل شد — یافته‌ها برای تأیید انسانی آماده‌اند.");result.setText(x);renderFindings(fs,image.getAbsolutePath());analyze.setEnabled(true);pick.setEnabled(true);});}catch(Exception e){host.postUi(()->{status.setText("تحلیل انجام نشد.");result.setText("خطا: "+host.safeMsg(e)+"\n\nاین قابلیت به دستگاه مناسب و در اولین استفاده به اینترنت برای دانلود مدل نیاز دارد؛ پس از دانلود، تحلیل تصویر آفلاین است.");analyze.setEnabled(true);pick.setEnabled(true);});}});});
        report.setOnClickListener(v->host.createAiInspectionPdf());
        host.addCard(c);return c;
    }
    private void renderFindings(ArrayList<HseAiFinding> fs,String imagePath){findingsList.removeAllViews();if(fs.isEmpty()){findingsList.addView(host.text("یافته ساختاریافته‌ای از خروجی مدل استخراج نشد؛ متن خام بالا برای بررسی دستی نگه داشته شده است.",13,host.MUTED));return;}findingsList.addView(host.sectionHead("🧾","یافته‌های قابل ثبت"));for(HseAiFinding f:fs)addFindingCard(f,imagePath);}
    private void addFindingCard(HseAiFinding f,String imagePath){
        LinearLayout card=host.card();card.setPadding(host.dp(12),host.dp(12),host.dp(12),host.dp(12));card.addView(host.text("⚠️ "+f.title(),15,host.INK));
        card.addView(host.text("دسته: "+f.category+"\nشاهد: "+f.evidence+"\nوضعیت: "+f.certainty+"\nخطر/پیامد: "+f.hazard+"\nاقدام فوری: "+f.immediateAction+"\nاقدام اصلاحی: "+f.correctiveAction+"\nاولویت: "+f.priority+"  |  اطمینان: "+f.confidence,12,host.INK));host.gapIn(card,8);
        LinearLayout row=new LinearLayout(host);row.setOrientation(LinearLayout.HORIZONTAL);Button approve=host.action("✓ تأیید و ثبت یافته",host.TEAL),risk=host.outline("⚠️ افزودن به ریسک");risk.setEnabled(false);row.addView(approve,new LinearLayout.LayoutParams(0,44,1));row.addView(new Space(host),new LinearLayout.LayoutParams(host.dp(7),1));row.addView(risk,new LinearLayout.LayoutParams(0,44,1));card.addView(row);findingsList.addView(card);host.gapIn(findingsList,8);
        final long[] savedId={-1}; approve.setOnClickListener(v->{approve.setEnabled(false);host.saveAiFinding(f,imagePath,id->{savedId[0]=id;risk.setEnabled(true);});});
        risk.setOnClickListener(v->{if(savedId[0]>0)host.addAiRisk(savedId[0],f);else host.toast("ابتدا یافته را تأیید و ثبت کنید.");});
    }
}
