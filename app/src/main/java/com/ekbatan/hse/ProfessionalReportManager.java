package com.ekbatan.hse;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.graphics.pdf.PdfDocument;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Offline professional HSE report renderer. No cloud, API, or paid service. */
public final class ProfessionalReportManager {
    public static final class Meta {
        public String title, company, department, area, location, responsible;
        public Meta(String title,String company,String department,String area,String location,String responsible){
            this.title=clean(title,"گزارش بازرسی HSE"); this.company=clean(company,"شرکت"); this.department=clean(department,"واحد HSE");
            this.area=clean(area,"ثبت نشده"); this.location=clean(location,"ثبت نشده"); this.responsible=clean(responsible,"ثبت نشده");
        }
    }
    private ProfessionalReportManager(){}
    public static File create(Context context, File target, Meta meta, LibraryDb.FindingRow finding, LibraryDb.RiskRow risk) throws IOException {
        if(context==null||target==null||finding==null) throw new IllegalArgumentException("اطلاعات گزارش ناقص است");
        File parent=target.getParentFile(); if(parent!=null&&!parent.exists()&&!parent.mkdirs()) throw new IOException("مسیر گزارش قابل ایجاد نیست");
        String stamp=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date());
        String number="HSE-"+stamp+"-"+finding.id;
        int W=2480,H=3508,M=150, contentW=W-2*M;
        PdfDocument pdf=new PdfDocument(); PageWriter pw=new PageWriter(W,H,M);
        try{
            PdfDocument.Page page=pdf.startPage(new PdfDocument.PageInfo.Builder(W,H,1).create()); Canvas c=page.getCanvas();
            pw.header(c,meta,number); int y=M+330;
            y=pw.section(c,y,"۱. مشخصات گزارش");
            y=pw.field(c,y,"تاریخ و ساعت",new SimpleDateFormat("yyyy/MM/dd  HH:mm:ss",Locale.US).format(new Date()));
            y=pw.field(c,y,"شرکت / سازمان",meta.company); y=pw.field(c,y,"واحد",meta.department); y=pw.field(c,y,"محل / ناحیه",meta.area); y=pw.field(c,y,"موقعیت",meta.location); y=pw.field(c,y,"مسئول / بازرس",meta.responsible);
            y+=25; y=pw.section(c,y,"۲. تصویر و یافته HSE");
            if(finding.imagePath!=null&&!finding.imagePath.trim().isEmpty()){
                Bitmap bm=decodeBounded(finding.imagePath,contentW,760); if(bm!=null){int x=M+(contentW-bm.getWidth())/2;c.drawBitmap(bm,x,y,pw.paint);y+=bm.getHeight()+25;bm.recycle();}
            }
            y=pw.field(c,y,"عنوان یافته",finding.title); y=pw.field(c,y,"دسته HSE",finding.category); y=pw.field(c,y,"شاهد قابل مشاهده",finding.evidence); y=pw.field(c,y,"وضعیت تشخیص",finding.certainty); y=pw.field(c,y,"خطر",finding.hazard); y=pw.field(c,y,"پیامد",finding.consequence);
            if(y>H-650){pdf.finishPage(page); page=pdf.startPage(new PdfDocument.PageInfo.Builder(W,H,2).create());c=page.getCanvas();pw=new PageWriter(W,H,M);y=M;}
            y=pw.section(c,y,"۳. کنترل و اقدام"); y=pw.field(c,y,"اقدام فوری",finding.immediate); y=pw.field(c,y,"اقدام اصلاحی پیشنهادی",finding.corrective); y=pw.field(c,y,"اولویت",finding.priority); y=pw.field(c,y,"اطمینان",finding.confidence);
            if(risk!=null){
                y+=20; y=pw.section(c,y,"۴. ارزیابی ریسک ۵×۵ — تأییدشده توسط کاربر");
                y=pw.field(c,y,"احتمال",String.valueOf(risk.likelihood)); y=pw.field(c,y,"شدت",String.valueOf(risk.severity)); y=pw.field(c,y,"امتیاز",String.valueOf(risk.score)); y=pw.field(c,y,"سطح ریسک",level(risk.score)); y=pw.field(c,y,"کنترل‌های موجود",risk.controls); y=pw.field(c,y,"اقدام اصلاحی",risk.action); y=pw.field(c,y,"مسئول اقدام",risk.responsible); y=pw.field(c,y,"وضعیت",risk.status);
            }
            y+=25; y=pw.section(c,y,"۵. ملاحظات و اعتبار گزارش");
            y=pw.field(c,y,"منبع",finding.source+" — تحلیل روی دستگاه");
            y=pw.field(c,y,"شماره گزارش",number);
            pw.footer(c,number);
            pdf.finishPage(page);
            try(FileOutputStream out=new FileOutputStream(target)){pdf.writeTo(out);out.flush();}
            return target;
        } finally { pdf.close(); }
    }
    private static Bitmap decodeBounded(String path,int maxW,int maxH){
        BitmapFactory.Options o=new BitmapFactory.Options();o.inJustDecodeBounds=true;BitmapFactory.decodeFile(path,o);if(o.outWidth<=0||o.outHeight<=0)return null;
        int sample=1;while(o.outWidth/sample>maxW*2||o.outHeight/sample>maxH*2)sample*=2;o.inJustDecodeBounds=false;o.inSampleSize=sample;o.inPreferredConfig=Bitmap.Config.RGB_565;return BitmapFactory.decodeFile(path,o);
    }
    private static String level(int n){if(n>=20)return "بحرانی";if(n>=12)return "زیاد";if(n>=5)return "متوسط";return "کم";}
    private static String clean(String s,String d){s=s==null?"":s.trim();return s.isEmpty()?d:s.length()>500?s.substring(0,500):s;}
    private static final class PageWriter{
        final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG); final int W,H,M; final int blue=android.graphics.Color.rgb(20,63,70),teal=android.graphics.Color.rgb(24,126,132),muted=android.graphics.Color.rgb(85,95,100);
        PageWriter(int w,int h,int m){W=w;H=h;M=m;paint.setTypeface(Typeface.create("sans",Typeface.NORMAL));}
        void header(Canvas c,Meta meta,String number){paint.setColor(blue);c.drawRect(0,0,W,260,paint);paint.setColor(android.graphics.Color.WHITE);paint.setTextSize(66);paint.setTypeface(Typeface.DEFAULT_BOLD);drawRtl(c,meta.title,W-M,100);paint.setTextSize(34);paint.setTypeface(Typeface.DEFAULT);drawRtl(c,meta.company,W-M,165);paint.setTextSize(27);drawRtl(c,"شماره گزارش: "+number,W-M,215);}
        int section(Canvas c,int y,String s){paint.setColor(teal);c.drawRoundRect(M,y,M+contentW(),y+72,18,18,paint);paint.setColor(android.graphics.Color.WHITE);paint.setTextSize(32);paint.setTypeface(Typeface.DEFAULT_BOLD);drawRtl(c,s,W-M,y+49);return y+95;}
        int field(Canvas c,int y,String label,String value){
            String v=clean(value,"ثبت نشده");
            int boxH=Math.max(96,estimateLines(v,27,contentW()-430)*42+36);
            paint.setColor(android.graphics.Color.rgb(245,248,249));c.drawRoundRect(M,y,M+contentW(),y+boxH,12,12,paint);
            paint.setColor(muted);paint.setTextSize(27);paint.setTypeface(Typeface.DEFAULT_BOLD);drawRtl(c,label,W-M-28,y+36);
            paint.setTypeface(Typeface.DEFAULT);paint.setTextSize(27);
            drawWrappedRtl(c,v,W-M-390,y+34,contentW()-420,boxH-18);
            return y+boxH+15;
        }
        int estimateLines(String s,float size,int width){if(s==null||s.isEmpty())return 1;return Math.max(1,(int)Math.ceil((double)(s.length()*size*0.52f)/Math.max(1,width)));}
        void drawWrappedRtl(Canvas c,String s,int right,int top,int width,int height){
            TextPaint tp=new TextPaint(Paint.ANTI_ALIAS_FLAG|Paint.SUBPIXEL_TEXT_FLAG);tp.setColor(paint.getColor());tp.setTextSize(paint.getTextSize());tp.setTypeface(paint.getTypeface());tp.setTextAlign(Paint.Align.RIGHT);
            StaticLayout sl=new StaticLayout(s==null?"":s,tp,width,Layout.Alignment.ALIGN_OPPOSITE,1.0f,0f,false);
            c.save();c.translate(right-width,top);sl.draw(c);c.restore();
        }
        void footer(Canvas c,String number){paint.setColor(android.graphics.Color.LTGRAY);c.drawRect(M,H-M-55,W-M,H-M-52,paint);paint.setColor(muted);paint.setTextSize(22);paint.setTypeface(Typeface.DEFAULT);drawRtl(c,"HSE AI PRO  •  گزارش کمکی بازرسی  •  تأیید نهایی با مسئول HSE",W-M,H-M-20);}
        int contentW(){return W-2*M;}
        void drawRtl(Canvas c,String s,float x,float y){paint.setTextAlign(Paint.Align.RIGHT);c.drawText(s==null?"":s,x,y,paint);paint.setTextAlign(Paint.Align.LEFT);}
    }
}
