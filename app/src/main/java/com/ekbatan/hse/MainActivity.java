package com.ekbatan.hse;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.ekbatan.hse.ui.HseSectionNavigator;
import com.ekbatan.hse.ui.HseFeature;
import com.ekbatan.hse.ui.HseFeatureCoordinator;
import com.ekbatan.hse.ui.HseUiTheme;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

/** HSE AI PRO 4.6.5 - hardened offline-first HSE workspace. */
public class MainActivity extends Activity {
    LibraryDb db; HseDataRepository data; ExecutorService io; ScrollView scroll; volatile boolean destroyed=false; LinearLayout content; TextView statsView, selectedReminderTime;
    HseSectionNavigator navigator; HseFeatureCoordinator features; ReminderFeature reminderFeature;
    View libraryView, notesView, reminderView, riskView, incidentView, glossaryView;
    interface ExportLoader { String load() throws Exception; }
    File pendingBackupFile, pendingAiPdfFile;
    String pendingAiPdfTitle="";
    HseAppServices services;
    /** Compatibility handle retained for diagnostics; the service owns lifecycle and execution. */
    HseVisionAi visionAi;
    Uri pendingAiImageUri;
    File pendingAiImageFile;
    Runnable aiImageReadyAction;
    final int PICK_AI_IMAGE=14;
    volatile boolean backupFlowActive=false;
    String latestSearchText="";
    float fontScale=1.0f;
    String pendingReminderTitle="", pendingReminderBody="";
    long pendingReminderAt=0, pendingRetryReminderId=-1;
    Button pendingReminderSave; EditText pendingReminderTitleView, pendingReminderBodyView; Calendar[] pendingReminderPicked; LinearLayout pendingReminderList;
    boolean exactAlarmSettingsOpened=false;
    final int PICK_PDF=11,PICK_BACKUP=12,CREATE_BACKUP=13,CREATE_AI_PDF=15,NOTIFICATION_PERMISSION=91;
    final int INK=HseUiTheme.INK, MUTED=HseUiTheme.MUTED, BG=HseUiTheme.BG, CARD=HseUiTheme.CARD, BORDER=HseUiTheme.BORDER;
    final int GREEN=HseUiTheme.GREEN, BLUE=HseUiTheme.BLUE, ORANGE=HseUiTheme.ORANGE, RED=HseUiTheme.RED, TEAL=HseUiTheme.TEAL, NAVY=HseUiTheme.NAVY, CYAN=HseUiTheme.CYAN, GOLD=HseUiTheme.GOLD;
    int dp(float x){return HseUiTheme.dp(this,x);}
    boolean uiAlive(){return !destroyed&&!isFinishing()&&(Build.VERSION.SDK_INT<17||!isDestroyed());}
    void postUi(Runnable r){if(!uiAlive())return;runOnUiThread(()->{if(uiAlive())r.run();});}
    void submitIo(Runnable r){ExecutorService e=io;if(destroyed||e==null||e.isShutdown())return;try{e.execute(()->{if(destroyed)return;r.run();});}catch(RejectedExecutionException ignored){}}

    @Override public void onCreate(Bundle b){super.onCreate(b);getWindow().setStatusBarColor(Color.rgb(16,58,62));getWindow().setNavigationBarColor(BG);getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);db=new LibraryDb(this);data=new HseDataRepository(db);services=new HseAppServices(this,data);io=Executors.newSingleThreadExecutor();fontScale=getSharedPreferences("hse_ui",MODE_PRIVATE).getFloat("font_scale",1.0f);build();rescheduleReminders();}

    TextView text(String s,float sp,int color){return HseUiTheme.text(this,s,sp,color,fontScale);}
    TextView title(String s,float sp){return HseUiTheme.title(this,s,sp,fontScale);}
    EditText input(String hint){return HseUiTheme.input(this,hint,fontScale);}
    Button action(String s,int bg){return HseUiTheme.action(this,s,bg,fontScale);}
    Button outline(String s){return HseUiTheme.outline(this,s,fontScale);}
    GradientDrawable box(int fill,int stroke,int radius){GradientDrawable g=HseUiTheme.box(this,fill,stroke,(int)Math.max(1,Math.round(radius/getResources().getDisplayMetrics().density)));return g;}
    GradientDrawable gradient(int start,int end,int radius){GradientDrawable g=HseUiTheme.gradient(this,start,end,(int)Math.max(1,Math.round(radius/getResources().getDisplayMetrics().density)));return g;}
    TextView badge(String s,int bg){return HseUiTheme.badge(this,s,bg,fontScale);}
    LinearLayout card(){return HseUiTheme.card(this);}
    LinearLayout accentCard(int accent){LinearLayout c=card();c.setPadding(dp(18),dp(16),dp(18),dp(16));LinearLayout bar=new LinearLayout(this);bar.setBackground(box(accent,accent,dp(3)));bar.setLayoutParams(new LinearLayout.LayoutParams(dp(4),-1));LinearLayout body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);body.setPadding(dp(14),0,0,0);c.addView(body,new LinearLayout.LayoutParams(-1,-2));return c;}
    void addCard(LinearLayout c){content.addView(c);gap(10);}
    void gap(int h){Space s=new Space(this);content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    void gapIn(LinearLayout c,int h){Space s=new Space(this);c.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    TextView sectionHead(String icon,String s){TextView t=title(icon+"  "+s,19);t.setPadding(0,0,0,dp(8));return t;}

    void build(){
        scroll=new ScrollView(this);scroll.setFillViewport(true);scroll.setBackgroundColor(BG);content=new LinearLayout(this);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(dp(12),dp(10),dp(12),dp(30));scroll.addView(content);setContentView(scroll);navigator=new HseSectionNavigator(this,scroll);
        // Professional product header: brand, system status and typography controls.
        LinearLayout top=card();top.setPadding(dp(16),dp(12),dp(16),dp(12));top.setBackground(gradient(NAVY,TEAL,dp(18)));
        LinearLayout brand=new LinearLayout(this);brand.setGravity(Gravity.CENTER_VERTICAL);
        TextView mark=text("H",24,Color.WHITE);mark.setGravity(Gravity.CENTER);mark.setTypeface(Typeface.DEFAULT_BOLD);mark.setBackground(box(RED,RED,dp(30)));brand.addView(mark,new LinearLayout.LayoutParams(dp(48),dp(48)));
        LinearLayout brandText=new LinearLayout(this);brandText.setOrientation(LinearLayout.VERTICAL);brandText.setPadding(dp(12),0,0,0);TextView appName=text("HSE AI PRO",20,Color.WHITE);appName.setTypeface(Typeface.DEFAULT_BOLD);brandText.addView(appName);TextView appSub=text("سامانه هوشمند مدیریت ایمنی و HSE",12,Color.rgb(208,229,232));brandText.addView(appSub);brand.addView(brandText,new LinearLayout.LayoutParams(0,-2,1));brand.addView(badge("OFFLINE",GREEN),new LinearLayout.LayoutParams(-2,-2));top.addView(brand);
        gapIn(top,10);TextView status=text("●  آماده‌به‌کار  •  داده‌ها روی دستگاه  •  بدون API پولی",12,Color.rgb(190,222,224));top.addView(status);
        LinearLayout fontRow=new LinearLayout(this);fontRow.setGravity(Gravity.CENTER_VERTICAL);TextView fontLabel=text("اندازه متن",12,Color.rgb(208,229,232));Button smaller=outline("A−"),larger=outline("A+");smaller.setTextColor(Color.WHITE);larger.setTextColor(Color.WHITE);smaller.setBackground(box(Color.rgb(34,83,90),Color.rgb(74,126,132),dp(10)));larger.setBackground(box(Color.rgb(34,83,90),Color.rgb(74,126,132),dp(10)));fontRow.addView(fontLabel,new LinearLayout.LayoutParams(0,dp(40),1));fontRow.addView(smaller,new LinearLayout.LayoutParams(dp(58),dp(40)));fontRow.addView(new Space(this),new LinearLayout.LayoutParams(dp(8),1));fontRow.addView(larger,new LinearLayout.LayoutParams(dp(58),dp(40)));top.addView(fontRow);
        smaller.setOnClickListener(v->changeFontScale(-0.1f));larger.setOnClickListener(v->changeFontScale(0.1f));content.addView(top);gap(12);
        buildDashboard();
        features=new HseFeatureCoordinator(content,navigator);
        features.add(LibraryFeature::new);
        features.add(GlossaryFeature::new);
        features.add(NotesFeature::new);
        features.add(() -> { reminderFeature = new ReminderFeature(this); return reminderFeature; });
        features.add(RiskFeature::new);
        features.add(RiskLibraryFeature::new);
        features.add(IncidentFeature::new);
        features.add(BackupFeature::new);
        features.add(VisionAiFeature::new);
        LinearLayout foot=card();foot.setGravity(Gravity.CENTER);foot.setPadding(dp(12),dp(12),dp(12),dp(12));TextView f1=text("HSE AI PRO  •  نسخه ۴.۶.۵",12,INK);f1.setGravity(Gravity.CENTER);f1.setTypeface(Typeface.DEFAULT_BOLD);foot.addView(f1);TextView f2=text("Offline-First  •  پوسته لایه‌ای  •  اطلاعات اصلی روی دستگاه",11,MUTED);f2.setGravity(Gravity.CENTER);foot.addView(f2);content.addView(foot);refreshAll();
    }

    void buildDashboard(){
        LinearLayout nav=card();nav.setPadding(dp(16),dp(16),dp(16),dp(16));
        LinearLayout head=new LinearLayout(this);head.setGravity(Gravity.CENTER_VERTICAL);TextView sh=sectionHead("📊","داشبورد HSE");head.addView(sh,new LinearLayout.LayoutParams(0,-2,1));head.addView(badge("مرکز کنترل",TEAL));nav.addView(head);
        TextView desc=text("نمای سریع از وضعیت ثبت‌شده سامانه و دسترسی سریع به مهم‌ترین ابزارهای HSE",12,MUTED);nav.addView(desc);gapIn(nav,10);
        LinearLayout stats=new LinearLayout(this);stats.setOrientation(LinearLayout.HORIZONTAL);int[] statBg={Color.rgb(232,244,247),Color.rgb(237,247,240),Color.rgb(255,245,226),Color.rgb(253,235,237)};int[] statAccent={CYAN,GREEN,ORANGE,RED};for(int i=0;i<4;i++){TextView st=text("…",13,INK);st.setGravity(Gravity.CENTER);st.setTypeface(Typeface.DEFAULT_BOLD);st.setPadding(dp(5),dp(8),dp(5),dp(8));st.setBackground(box(statBg[i],statAccent[i],dp(14)));stats.addView(st,new LinearLayout.LayoutParams(0,dp(74),1));if(i<3)stats.addView(new Space(this),new LinearLayout.LayoutParams(dp(6),1));}nav.addView(stats);statsView=(TextView)stats.getChildAt(0);
        TextView s2=(TextView)stats.getChildAt(2),s3=(TextView)stats.getChildAt(4),s4=(TextView)stats.getChildAt(6);statsView.setTag("منابع\n…");s2.setTag("یادداشت\n…");s3.setTag("ریسک بالا\n…");s4.setTag("رویداد باز\n…");
        gapIn(nav,12);TextView qh=text("دسترسی سریع",12,MUTED);qh.setTypeface(Typeface.DEFAULT_BOLD);nav.addView(qh);gapIn(nav,5);LinearLayout q=new LinearLayout(this);q.setOrientation(LinearLayout.HORIZONTAL);Button b1=outline("📚 کتابخانه"),b2=outline("📝 یادداشت"),b3=outline("⚠️ ریسک"),b4=outline("🚨 حوادث");Button[] bs={b1,b2,b3,b4};for(int i=0;i<bs.length;i++){q.addView(bs[i],new LinearLayout.LayoutParams(0,dp(46),1));if(i<3)q.addView(new Space(this),new LinearLayout.LayoutParams(dp(5),1));}nav.addView(q);b1.setOnClickListener(v->jump(libraryView));b2.setOnClickListener(v->jump(notesView));b3.setOnClickListener(v->jump(riskView));b4.setOnClickListener(v->jump(incidentView));addCard(nav);
    }
    void jump(View v){if(v==null)return; for(Map.Entry<String,View> e: navigatorSnapshot().entrySet()){if(e.getValue()==v){navigator.open(e.getKey());return;}} scroll.postDelayed(()->scroll.smoothScrollTo(0,Math.max(0,v.getTop()-dp(8))),80);}
    Map<String,View> navigatorSnapshot(){Map<String,View> m=new LinkedHashMap<>();m.put("library",libraryView);m.put("notes",notesView);m.put("reminder",reminderView);m.put("risk",riskView);m.put("incident",incidentView);m.put("glossary",glossaryView);return m;}

    void buildBuiltInBooks(LinearLayout c){
        c.addView(title("📚 ۱۰ کتاب/راهنمای تخصصی داخلی HSE",15));
        c.addView(text("۱۰ راهنمای تألیفی فارسی، آفلاین و قابل جست‌وجو؛ برای آموزش و کار عملی. این متن‌ها جایگزین متن رسمی قوانین و آیین‌نامه‌ها نیستند.",12,MUTED));
        gapIn(c,7);
        EditText q=input("جست‌وجو در ۱۰ کتاب: کار در ارتفاع، LOTO، حریق..."); c.addView(q);
        gapIn(c,7);
        LinearLayout list=new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); c.addView(list);
        Runnable render=()->{ String query=q.getText().toString().trim(); list.removeAllViews(); String nq=PersianNormalizer.normalize(query).toLowerCase(Locale.ROOT); for(int i=0;i<BuiltInBooks.BOOKS.length;i++){ BuiltInBooks.Book b=BuiltInBooks.BOOKS[i]; String hay=PersianNormalizer.normalize(b.title+" "+b.body).toLowerCase(Locale.ROOT); if(!query.isEmpty() && !hay.contains(nq)) continue; final int pos=i; TextView item=text("📘 "+b.title+"\n"+b.source,14,INK); item.setTextIsSelectable(true); item.setPadding(dp(12),dp(10),dp(12),dp(10)); item.setBackground(box(Color.rgb(250,252,253),BORDER,dp(10))); item.setOnClickListener(v->showBuiltInBook(pos)); list.addView(item,new LinearLayout.LayoutParams(-1,-2)); gapIn(list,6); } if(list.getChildCount()==0) list.addView(text("کتابی با این عبارت پیدا نشد.",13,MUTED)); };
        q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){} public void onTextChanged(CharSequence s,int st,int before,int count){render.run();} public void afterTextChanged(android.text.Editable e){}});
        render.run();
    }

    void showBuiltInBook(int index){
        if(index<0||index>=BuiltInBooks.BOOKS.length)return; BuiltInBooks.Book b=BuiltInBooks.BOOKS[index];
        final float[] scale={fontScale}; final TextView body=text(b.body,15,INK); body.setGravity(Gravity.RIGHT|Gravity.TOP); body.setTextIsSelectable(true); body.setTextDirection(View.TEXT_DIRECTION_RTL); body.setLineSpacing(dp(2),1.12f); body.setPadding(dp(14),dp(10),dp(14),dp(10));
        ScrollView sv=new ScrollView(this); sv.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); sv.addView(body);
        LinearLayout wrap=new LinearLayout(this); wrap.setOrientation(LinearLayout.VERTICAL); wrap.setPadding(dp(8),0,dp(8),0); TextView meta=text(b.source,12,MUTED); wrap.addView(meta); wrap.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle(b.title).setView(wrap).setNegativeButton("بستن",null).create();
        dialog.setOnShowListener(x->{ LinearLayout buttons=new LinearLayout(this); buttons.setGravity(Gravity.CENTER); Button minus=outline("A−"), plus=outline("A+"), copy=outline("📋 کپی"), print=outline("🖨 چاپ"); buttons.addView(minus,new LinearLayout.LayoutParams(dp(58),dp(44))); buttons.addView(plus,new LinearLayout.LayoutParams(dp(58),dp(44))); buttons.addView(copy,new LinearLayout.LayoutParams(dp(90),dp(44))); buttons.addView(print,new LinearLayout.LayoutParams(dp(90),dp(44))); wrap.addView(buttons,0); minus.setOnClickListener(v->{scale[0]=Math.max(.8f,scale[0]-.1f);body.setTextSize(15*scale[0]);}); plus.setOnClickListener(v->{scale[0]=Math.min(1.6f,scale[0]+.1f);body.setTextSize(15*scale[0]);}); copy.setOnClickListener(v->{ExportPrintManager.copy(this,b.title,b.body);toast("متن کتاب کپی شد");}); print.setOnClickListener(v->{try{ExportPrintManager.print(this,b.title,b.body);}catch(Exception e){toast("خطا در چاپ: "+safeMsg(e));}}); });
        dialog.show();
        android.view.Window w=dialog.getWindow(); if(w!=null){w.setLayout((int)(getResources().getDisplayMetrics().widthPixels*.96f),(int)(getResources().getDisplayMetrics().heightPixels*.88f));}
    }

    void buildRecommendedBooks(LinearLayout c){
        c.addView(title("📚 ۱۰ مرجع منتخب ایمنی و HSE",15));
        c.addView(text("فهرست پیشنهادی فارسی؛ موارد دارای ترجمه فارسی انتخاب شده‌اند. متن کامل آثار دارای حق نشر داخل برنامه کپی نشده است؛ در صورت داشتن PDF مجاز، می‌توانید آن را با «افزودن PDF» وارد کنید.",12,MUTED));
        gapIn(c,7);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);
        for(String[] b:RECOMMENDED_BOOKS){
            TextView item=text("📘 "+b[0]+"\n"+b[1]+"\nمترجم: "+b[2]+"\nحوزه: "+b[3],13,INK);
            item.setTextIsSelectable(true);item.setPadding(dp(10),dp(9),dp(10),dp(9));item.setBackground(box(Color.rgb(250,252,253),BORDER,dp(10)));list.addView(item,new LinearLayout.LayoutParams(-1,-2));gapIn(list,6);
        }
        c.addView(list);addExportTools(c,"۱۰ مرجع منتخب ایمنی",this::exportRecommendedBooks);
    }
    void changeFontScale(float delta){float next=Math.max(0.8f,Math.min(1.4f,fontScale+delta));if(Math.abs(next-fontScale)<0.01f)return;fontScale=next;getSharedPreferences("hse_ui",MODE_PRIVATE).edit().putFloat("font_scale",fontScale).apply();build();scroll.postDelayed(()->scroll.fullScroll(ScrollView.FOCUS_UP),50);toast("اندازه متن: "+Math.round(fontScale*100)+"٪");}

    static final String[][] RECOMMENDED_BOOKS={
        {"مهندسی ایمنی سیستم و ارزیابی ریسک","نیکلاس ژ. باهر","حجت‌الله رضازاده","مهندسی ایمنی سیستم و ارزیابی ریسک"},
        {"ایمنی و بهداشت برای مهندسین (ایمنی در صنعت)","راجر ال. برور","غلامحسین حلوانی","ایمنی صنعتی، ماشین‌آلات، PPE و مدیریت ریسک"},
        {"ارزیابی کمی ایمنی","الف. رئوف و ب.س. دهیلون","ایرج محمدفام","ارزیابی کمی عملکرد و ایمنی"},
        {"تحلیل اثربخش سیستم ایمنی","دانیل پیترسون","رامین آذربرزین","فرهنگ ایمنی، بازرسی و ممیزی"},
        {"مهندسی بهداشت و ایمنی در معدن زغال‌سنگ","اثر ترجمه‌شده از منابع مهندسی معدن آمریکا","شهریار بهرامی اقدم","ایمنی و بهداشت در معدن"},
        {"ایمنی و بهداشت شغلی در صنایع آهن و فولاد","منابع تخصصی خارجی حوزه فولاد","مترجمان ایرانی اثر","ایمنی و بهداشت صنایع آهن و فولاد"},
        {"NFPA 10 — استاندارد خاموش‌کننده‌های قابل حمل آتش","NFPA","رضا امیرنژاد و یوسف احمدزاده‌وسطی‌کلائی","حفاظت در برابر حریق و خاموش‌کننده‌ها"},
        {"کمک‌های اولیه — ترجمه راهنمای صلیب سرخ آمریکا","American Red Cross Guidelines 2011–2016","ابراهیم هوشمندی","کمک‌های اولیه و امداد"},
        {"بازرسی‌های OSHA","OSHA","فرح قبادی‌تبار","بازرسی و الزامات ایمنی شغلی"},
        {"ایمنی در برق","منبع تخصصی ایمنی برق","محمدرضا حسن‌بیگی","ایمنی برق و پیشگیری از برق‌گرفتگی"}
    };
    static final String[][] GLOSSARY={
        {"HSE","Health, Safety and Environment — بهداشت، ایمنی و محیط‌زیست"},{"EHS","Environment, Health and Safety — محیط‌زیست، بهداشت و ایمنی"},{"PPE","Personal Protective Equipment — تجهیزات حفاظت فردی"},{"HIRA","Hazard Identification and Risk Assessment — شناسایی خطر و ارزیابی ریسک"},{"JSA","Job Safety Analysis — تجزیه و تحلیل ایمنی شغل"},{"JHA","Job Hazard Analysis — تحلیل خطرات شغلی"},{"LOTO","Lockout/Tagout — قفل‌گذاری و برچسب‌گذاری"},{"PTW","Permit to Work — مجوز انجام کار"},{"ALARP","As Low As Reasonably Practicable — تا حدی که عملاً و معقولانه امکان‌پذیر است"},{"MOC","Management of Change — مدیریت تغییر"},{"ERP","Emergency Response Plan — طرح واکنش در شرایط اضطراری"},{"EAP","Emergency Action Plan — برنامه اقدام اضطراری"},{"BBS","Behavior Based Safety — ایمنی مبتنی بر رفتار"},{"RCA","Root Cause Analysis — تحلیل علت ریشه‌ای"},{"CAPA","Corrective and Preventive Action — اقدام اصلاحی و پیشگیرانه"},{"KPI","Key Performance Indicator — شاخص کلیدی عملکرد"},{"TRIR","Total Recordable Incident Rate — نرخ کل حوادث قابل ثبت"},{"LTIFR","Lost Time Injury Frequency Rate — نرخ تکرار آسیب‌های منجر به اتلاف زمان"},{"LTIR","Lost Time Injury Rate — نرخ آسیب منجر به اتلاف زمان"},{"FAC","First Aid Case — مورد نیازمند کمک‌های اولیه"},{"MTC","Medical Treatment Case — مورد نیازمند درمان پزشکی"},{"LTI","Lost Time Injury — آسیب منجر به از دست رفتن زمان کار"},{"NM","Near Miss — شبه‌حادثه"},{"UA","Unsafe Act — عمل ناایمن"},{"UC","Unsafe Condition — شرایط ناایمن"},{"TBT","Toolbox Talk — جلسه کوتاه ایمنی پیش از کار"},{"SDS","Safety Data Sheet — برگه اطلاعات ایمنی مواد"},{"MSDS","Material Safety Data Sheet — برگه اطلاعات ایمنی مواد"},{"LEL","Lower Explosive Limit — حد پایین انفجار"},{"UEL","Upper Explosive Limit — حد بالای انفجار"},{"STEL","Short-Term Exposure Limit — حد مواجهه کوتاه‌مدت"},{"TWA","Time-Weighted Average — میانگین وزنی زمانی مواجهه"},{"OEL","Occupational Exposure Limit — حد مجاز مواجهه شغلی"},{"TLV","Threshold Limit Value — مقدار حد آستانه‌ای"},{"PEL","Permissible Exposure Limit — حد مجاز مواجهه"},{"ACGIH","American Conference of Governmental Industrial Hygienists — انجمن متخصصان بهداشت صنعتی دولتی آمریکا"},{"NIOSH","National Institute for Occupational Safety and Health — مؤسسه ملی ایمنی و بهداشت شغلی آمریکا"},{"OSHA","Occupational Safety and Health Administration — اداره ایمنی و بهداشت شغلی آمریکا"},{"ISO","International Organization for Standardization — سازمان بین‌المللی استانداردسازی"},{"ISO 45001","Occupational Health and Safety Management Systems — سیستم مدیریت ایمنی و بهداشت شغلی"},{"ISO 14001","Environmental Management System — سیستم مدیریت محیط‌زیست"},{"ISO 9001","Quality Management System — سیستم مدیریت کیفیت"},{"FMEA","Failure Mode and Effects Analysis — تحلیل حالات خرابی و آثار آن"},{"FTA","Fault Tree Analysis — تحلیل درخت خطا"},{"ETA","Event Tree Analysis — تحلیل درخت رویداد"},{"HAZOP","Hazard and Operability Study — مطالعه خطر و قابلیت بهره‌برداری"},{"HAZID","Hazard Identification — شناسایی خطر"},{"SIF","Serious Injury and Fatality — آسیب شدید و مرگبار"},{"SIMOPS","Simultaneous Operations — عملیات همزمان"},{"MDR","Material Delivery/Receiving — دریافت و تحویل مواد"},{"MHE","Material Handling Equipment — تجهیزات جابه‌جایی مواد"},{"MEWP","Mobile Elevating Work Platform — سکوی کار متحرک بالابر"},{"SCBA","Self-Contained Breathing Apparatus — دستگاه تنفسی مستقل"},{"BA","Breathing Apparatus — دستگاه تنفسی"},{"AED","Automated External Defibrillator — دفیبریلاتور خارجی خودکار"},{"CPR","Cardiopulmonary Resuscitation — احیای قلبی ریوی"},{"FA","First Aid — کمک‌های اولیه"},{"EHSMS","Environment, Health and Safety Management System — سیستم مدیریت محیط‌زیست، بهداشت و ایمنی"},{"PSSR","Pre-Startup Safety Review — بازبینی ایمنی پیش از راه‌اندازی"},{"MARP","Management Action and Review Process — فرایند اقدام و بازبینی مدیریتی"},{"DCS","Distributed Control System — سیستم کنترل توزیع‌شده"},{"PLC","Programmable Logic Controller — کنترل‌کننده منطقی قابل برنامه‌ریزی"},{"SOP","Standard Operating Procedure — دستورالعمل اجرایی استاندارد"},{"SWP","Safe Work Procedure — روش اجرایی کار ایمن"},{"RAMS","Risk Assessment and Method Statement — ارزیابی ریسک و شرح روش اجرا"},{"RIDDOR","Reporting of Injuries, Diseases and Dangerous Occurrences Regulations — مقررات گزارش‌دهی صدمات، بیماری‌ها و رخدادهای خطرناک"}
    };
    String filterGlossary(String q){String n=PersianNormalizer.normalize(q==null?"":q).trim();StringBuilder b=new StringBuilder();for(String[] x:GLOSSARY){String z=PersianNormalizer.normalize(x[0]+" "+x[1]);if(n.isEmpty()||z.contains(n))b.append("• ").append(x[0]).append("\n").append(x[1]).append("\n\n");}return b.length()==0?"موردی پیدا نشد.":b.toString().trim();}

    String riskLevel(int n){if(n>=20)return "بحرانی";if(n>=12)return "زیاد";if(n>=5)return "متوسط";return "کم";}
    int riskColor(int n){if(n>=20)return RED;if(n>=12)return ORANGE;if(n>=5)return Color.rgb(170,125,0);return GREEN;}

    void addExportTools(LinearLayout c,String title,ExportLoader loader){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);Button copy=outline("📋 کپی"),print=outline("🖨 چاپ");row.addView(copy,new LinearLayout.LayoutParams(0,dp(44),1));row.addView(new Space(this),new LinearLayout.LayoutParams(dp(8),1));row.addView(print,new LinearLayout.LayoutParams(0,dp(44),1));copy.setOnClickListener(v->runExport(title,loader,false));print.setOnClickListener(v->runExport(title,loader,true));c.addView(row);gapIn(c,7);}
    void runExport(String title,ExportLoader loader,boolean print){submitIo(()->{try{String data=loader.load();postUi(()->{try{if(print)ExportPrintManager.print(this,title,data);else{ExportPrintManager.copy(this,title,data);toast("اطلاعات «"+title+"» کپی شد");}}catch(Exception e){toast("خطا در "+(print?"چاپ":"کپی")+": "+safeMsg(e));}});}catch(Exception e){postUi(()->toast("خطا در آماده‌سازی داده: "+safeMsg(e)));}});}
    String exportDashboard(){LibraryDb.Stats s=data.stats();return "گزارش داشبورد HSE AI PRO\n=====================\nمنابع: "+s.documents+"\nیادداشت‌ها: "+s.notes+"\nریسک‌های بالا: "+s.highRisks+"\nرویدادهای باز: "+s.openIncidents+"\nکل یادآوری‌ها: "+s.reminders+"\n\nزمان گزارش: "+formatTime(System.currentTimeMillis());}
    String exportDocuments(){StringBuilder b=new StringBuilder("گزارش کتابخانه HSE AI PRO\n====================\n");for(LibraryDb.DocumentRow r:data.listDocuments())b.append("عنوان: ").append(r.title).append("\nصفحات: ").append(r.pages).append("\nدسته‌بندی: ").append(r.category).append("\n\n");return b.toString();}
    String exportNotes(){StringBuilder b=new StringBuilder("گزارش یادداشت‌های HSE AI PRO\n========================\n");for(LibraryDb.NoteRow r:data.listNotes(200))b.append("عنوان: ").append(r.title.isEmpty()?"بدون عنوان":r.title).append("\nمتن: ").append(r.body).append("\nتاریخ: ").append(formatDate(r.updatedAt)).append("\n\n--------------------\n\n");return b.toString();}
    String exportReminders(){StringBuilder b=new StringBuilder("گزارش یادآوری‌های HSE AI PRO\n===========================\n");for(LibraryDb.ReminderRow r:data.listReminders(200))b.append(r.done?"وضعیت: انجام‌شده\n":"وضعیت: باز\n").append("عنوان: ").append(r.title).append("\nزمان: ").append(formatTime(r.at)).append("\nتوضیح: ").append(r.body).append("\n\n--------------------\n\n");return b.toString();}
    String exportRisks(){StringBuilder b=new StringBuilder("گزارش ارزیابی ریسک HSE AI PRO\n=============================\n");for(LibraryDb.RiskRow r:data.listRisks(200))b.append("خطر: ").append(r.hazard).append("\nپیامد: ").append(r.consequence).append("\nاحتمال: ").append(r.likelihood).append("\nشدت: ").append(r.severity).append("\nامتیاز: ").append(r.score).append(" — ").append(riskLevel(r.score)).append("\nکنترل‌ها: ").append(r.controls).append("\nاقدام: ").append(r.action).append("\nمسئول: ").append(r.responsible).append("\nوضعیت: ").append(r.status).append("\n\n--------------------\n\n");return b.toString();}
    String exportIncidents(){StringBuilder b=new StringBuilder("گزارش حوادث و شبه‌حوادث HSE AI PRO\n===================================\n");for(LibraryDb.IncidentRow r:data.listIncidents(200))b.append("عنوان: ").append(r.title).append("\nتاریخ: ").append(formatDate(r.eventDate)).append("\nوضعیت: ").append(r.status).append("\nشرح: ").append(r.description).append("\nاقدام فوری: ").append(r.immediate).append("\nعلت ریشه‌ای: ").append(r.rootCause).append("\nاقدام اصلاحی: ").append(r.corrective).append("\n\n--------------------\n\n");return b.toString();}
    String exportRecommendedBooks(){StringBuilder b=new StringBuilder("۱۰ مرجع منتخب ایمنی و HSE\n==============================\n");for(String[] x:RECOMMENDED_BOOKS)b.append("عنوان: ").append(x[0]).append("\nمولف/منبع: ").append(x[1]).append("\nمترجم: ").append(x[2]).append("\nحوزه: ").append(x[3]).append("\n\n--------------------\n\n");return b.toString();}
    String exportGlossary(){StringBuilder b=new StringBuilder("واژه‌نامه و کلمات اختصاری HSE\n==============================\n");for(String[] x:GLOSSARY)b.append(x[0]).append(" — ").append(x[1]).append("\n");return b.toString();}
    String exportBackupStatus(){LibraryDb.Stats s=data.stats();return "گزارش وضعیت پشتیبان HSE AI PRO\n===============================\nمنابع: "+s.documents+"\nیادداشت‌ها: "+s.notes+"\nیادآوری‌ها: "+s.reminders+"\nریسک‌ها: "+s.risks+"\nحوادث: "+s.incidents+"\n\nپشتیبان شامل دیتابیس و کتابخانه PDF داخلی برنامه است.\nمدل هوش مصنوعی محلی به‌صورت جداگانه نگهداری می‌شود و در Backup قرار نمی‌گیرد.";}

    void refreshAll(){if(io==null)return;submitIo(()->{try{LibraryDb.Stats s=data.stats();ArrayList<LibraryDb.ReminderRow> rr=data.listReminders(5);postUi(()->{if(statsView!=null){statsView.setText("منابع\n"+s.documents);ViewGroup parent=(ViewGroup)statsView.getParent();if(parent!=null&&parent.getChildCount()>=7){((TextView)parent.getChildAt(2)).setText("یادداشت\n"+s.notes);((TextView)parent.getChildAt(4)).setText("ریسک بالا\n"+s.highRisks);((TextView)parent.getChildAt(6)).setText("رویداد باز\n"+s.openIncidents);}}});}catch(Exception ignored){}});}

    void pick(int code,String type){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType(type);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);startActivityForResult(i,code);}
    @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(c!=RESULT_OK||d==null||d.getData()==null){if(r==CREATE_BACKUP){File f=pendingBackupFile;pendingBackupFile=null;backupFlowActive=false;if(f!=null)f.delete();}else if(r==CREATE_AI_PDF){File f=pendingAiPdfFile;pendingAiPdfFile=null;if(f!=null)f.delete();}return;}Uri u=d.getData();if(r==PICK_PDF)importPdf(u);else if(r==PICK_BACKUP)restore(u);else if(r==CREATE_BACKUP)saveBackup(u);else if(r==CREATE_AI_PDF)saveAiPdf(u);else if(r==PICK_AI_IMAGE)prepareAiImage(u);}
    interface FindingSaved { void done(long id); }
    void saveAiFinding(HseAiFinding f,String imagePath,FindingSaved after){submitIo(()->{try{long id=data.addInspectionFinding(f,imagePath);postUi(()->{toast("یافته HSE تأیید و ثبت شد.");if(after!=null)after.done(id);});}catch(Exception e){postUi(()->toast("خطا در ثبت یافته: "+safeMsg(e)));}});}
    void addAiRisk(long findingId,HseAiFinding f){final String[] vals={"1","2","3","4","5"};Spinner sl=new Spinner(this),ss=new Spinner(this);sl.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,vals));ss.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,vals));int d=priorityDefault(f.priority);sl.setSelection(d-1);ss.setSelection(d-1);LinearLayout wrap=new LinearLayout(this);wrap.setOrientation(LinearLayout.VERTICAL);wrap.setPadding(dp(8),dp(4),dp(8),0);wrap.addView(text("این اعداد فقط پیشنهاد اولیه هوش مصنوعی هستند؛ قبل از ثبت بازبینی و اصلاح کنید.",12,MUTED));LinearLayout row=new LinearLayout(this);LinearLayout a=new LinearLayout(this),b=new LinearLayout(this);a.setOrientation(LinearLayout.VERTICAL);b.setOrientation(LinearLayout.VERTICAL);a.addView(text("احتمال",12,MUTED));a.addView(sl);b.addView(text("شدت",12,MUTED));b.addView(ss);row.addView(a,new LinearLayout.LayoutParams(0,-2,1));row.addView(new Space(this),new LinearLayout.LayoutParams(dp(8),1));row.addView(b,new LinearLayout.LayoutParams(0,-2,1));wrap.addView(row);EditText ctrl=input("کنترل‌های موجود"),act=input("اقدام اصلاحی"),resp=input("مسئول");act.setText(f.correctiveAction);wrap.addView(ctrl);gapIn(wrap,6);wrap.addView(act);gapIn(wrap,6);wrap.addView(resp);AlertDialog dlg=new AlertDialog.Builder(this).setTitle("افزودن یافته به ارزیابی ریسک").setView(wrap).setNegativeButton("انصراف",null).setPositiveButton("ثبت ریسک",null).create();dlg.setOnShowListener(x->dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{int l=sl.getSelectedItemPosition()+1,s=ss.getSelectedItemPosition()+1;try{long risk=data.addRisk(f.title(),f.consequence,l,s,ctrl.getText().toString().trim(),act.getText().toString().trim(),resp.getText().toString().trim());data.linkFindingToRisk(findingId,risk);dlg.dismiss();toast("ریسک ثبت شد — امتیاز: "+(l*s)+" — "+riskLevel(l*s));refreshAll();}catch(Exception e){toast("خطا در ثبت ریسک: "+safeMsg(e));}}));dlg.show();}
    int priorityDefault(String p){String n=PersianNormalizer.normalize(p);if(n.contains("بحرانی"))return 5;if(n.contains("بالا"))return 4;if(n.contains("متوسط"))return 3;if(n.contains("پایین"))return 2;return 1;}
    void createProfessionalHseReport(){
        ArrayList<LibraryDb.FindingRow> fs=data.listInspectionFindings(1);
        if(fs.isEmpty()){toast("ابتدا حداقل یک یافته HSE را تأیید و ثبت کنید.");return;}
        LibraryDb.FindingRow f=fs.get(0);
        LinearLayout w=new LinearLayout(this);
        w.setOrientation(LinearLayout.VERTICAL); w.setPadding(dp(8),dp(4),dp(8),0);
        EditText title=input("عنوان گزارش"); title.setText("گزارش بازرسی HSE");
        EditText company=input("شرکت / سازمان"); company.setText("HSE AI PRO");
        EditText dep=input("واحد / بخش"), area=input("محل / ناحیه"), loc=input("موقعیت / توضیح محل (اختیاری)"), resp=input("مسئول / بازرس");
        w.addView(title);gapIn(w,6);w.addView(company);gapIn(w,6);w.addView(dep);gapIn(w,6);w.addView(area);gapIn(w,6);w.addView(loc);gapIn(w,6);w.addView(resp);
        AlertDialog dlg=new AlertDialog.Builder(this).setTitle("گزارش حرفه‌ای HSE")
                .setMessage("گزارش بر اساس آخرین یافته تأییدشده ساخته می‌شود. تاریخ/ساعت خودکار ثبت خواهد شد.")
                .setView(w).setNegativeButton("انصراف",null).setPositiveButton("ساخت PDF",null).create();
        dlg.setOnShowListener(x->{
            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{
                dlg.dismiss();
                submitIo(()->{
                    try{
                        LibraryDb.RiskRow risk=f.riskId>0?data.getRisk(f.riskId):null;
                        String stamp=new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date());
                        File out=new File(getCacheDir(),"HSE_Report_"+stamp+"_"+f.id+".pdf");
                        ProfessionalReportManager.Meta m=new ProfessionalReportManager.Meta(title.getText().toString(),company.getText().toString(),dep.getText().toString(),area.getText().toString(),loc.getText().toString(),resp.getText().toString());
                        ProfessionalReportManager.create(this,out,m,f,risk);
                        postUi(()->shareProfessionalPdf(out));
                    }catch(Exception e){postUi(()->toast("خطا در ساخت گزارش: "+safeMsg(e)));}
                });
            });
        });
        dlg.show();
    }
    void shareProfessionalPdf(File file){try{if(file==null||!file.isFile())throw new IOException("فایل گزارش پیدا نشد");android.net.Uri uri=androidx.core.content.FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");i.putExtra(Intent.EXTRA_STREAM,uri);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);startActivity(Intent.createChooser(i,"اشتراک‌گذاری گزارش HSE"));}catch(Exception e){toast("گزارش ساخته شد، اما اشتراک‌گذاری انجام نشد: "+safeMsg(e));}}
    String buildAiInspectionReport(){StringBuilder b=new StringBuilder();b.append("گزارش بازرسی HSE — یافته‌های هوش مصنوعی\nHSE AI PRO\n========================================\nتاریخ: ").append(formatTime(System.currentTimeMillis())).append("\nمنبع: تحلیل تصویر با Gemma 4 E2B + لایه قواعد HSE\n\n");for(LibraryDb.FindingRow r:data.listInspectionFindings(200)){b.append("یافته #").append(r.id).append(" — ").append(r.title).append("\nدسته HSE: ").append(r.category).append("\nشاهد: ").append(r.evidence).append("\nوضعیت: ").append(r.certainty).append("\nخطر/پیامد: ").append(r.hazard).append(" — ").append(r.consequence).append("\nاقدام فوری: ").append(r.immediate).append("\nاقدام اصلاحی: ").append(r.corrective).append("\nاولویت: ").append(r.priority).append("\nاطمینان: ").append(r.confidence).append("\nریسک مرتبط: ").append(r.riskId>0?String.valueOf(r.riskId):"ثبت نشده").append("\nوضعیت ثبت: ").append(r.status).append("\n----------------------------------------\n");}b.append("\nیادآوری: تشخیص تصویری ابزار کمکی است و تأیید نهایی، ارزیابی ریسک و تصمیم اصلاحی بر عهده مسئول HSE/بازرس ذی‌صلاح است.");return b.toString();}
    void createAiInspectionPdf(){submitIo(()->{try{String report=buildAiInspectionReport();File f=new File(getCacheDir(),"HSE_AI_Inspection_"+System.currentTimeMillis()+".pdf");ExportPrintManager.createPdf(f,"گزارش بازرسی HSE AI PRO",report);pendingAiPdfFile=f;pendingAiPdfTitle=f.getName();postUi(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/pdf");i.putExtra(Intent.EXTRA_TITLE,pendingAiPdfTitle);startActivityForResult(i,CREATE_AI_PDF);});}catch(Exception e){postUi(()->toast("خطا در ساخت PDF: "+safeMsg(e)));}});}
    void saveAiPdf(Uri u){submitIo(()->{File f=pendingAiPdfFile;try{if(f==null||!f.isFile())throw new IOException("PDF موقت پیدا نشد");try(InputStream in=new FileInputStream(f);OutputStream out=getContentResolver().openOutputStream(u)){if(out==null)throw new IOException("مسیر ذخیره قابل نوشتن نیست");byte[] b=new byte[32768];int n;while((n=in.read(b))!=-1)out.write(b,0,n);out.flush();}f.delete();pendingAiPdfFile=null;postUi(()->toast("گزارش بازرسی PDF ذخیره شد."));}catch(Exception e){postUi(()->toast("خطا در ذخیره PDF: "+safeMsg(e)));}});}
    void prepareAiImage(Uri u){
        submitIo(()->{
            File f=new File(getCacheDir(),"hse_ai_image_"+System.nanoTime()+".jpg");
            try(InputStream in=getContentResolver().openInputStream(u);OutputStream out=new FileOutputStream(f)){
                if(in==null)throw new IOException("تصویر قابل خواندن نیست");
                byte[] b=new byte[32768];int n;long total=0;
                while((n=in.read(b))!=-1){total+=n;if(total>20L*1024L*1024L)throw new IOException("حجم تصویر بیش از ۲۰MB است");out.write(b,0,n);}
                out.flush();
                File old=pendingAiImageFile;pendingAiImageFile=f;pendingAiImageUri=u;if(old!=null)old.delete();
                postUi(()->{if(aiImageReadyAction!=null)aiImageReadyAction.run();toast("تصویر آماده تحلیل است. اکنون «تحلیل ناایمنی» را بزنید.");});
            }catch(Exception e){f.delete();postUi(()->toast("خطا در خواندن تصویر: "+safeMsg(e)));}
        });
    }

    void importPdf(Uri u){submitIo(()->{try{PdfImporter.Result x=services.importPdf(u);postUi(()->{toast("PDF وارد شد — "+x.pages+" صفحه، "+x.textPages+" صفحه متنی");refreshAll();});}catch(Exception e){postUi(()->toast("خطا در ورود PDF: "+safeMsg(e)));}});}
    void doBackup(){
        if(backupFlowActive){toast("یک عملیات پشتیبان‌گیری در حال انجام است.");return;}
        backupFlowActive=true;
        submitIo(()->{try{File f=services.createBackup();pendingBackupFile=f;postUi(()->{Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,f.getName());startActivityForResult(i,CREATE_BACKUP);});}catch(Exception e){backupFlowActive=false;postUi(()->toast("خطا در پشتیبان: "+safeMsg(e)));}});
    }
    void saveBackup(Uri u){submitIo(()->{File f=pendingBackupFile;try{f=(f!=null&&f.isFile())?f:services.latestBackup();services.saveBackup(f,u);final File done=f;pendingBackupFile=null;backupFlowActive=false;done.delete();postUi(()->toast("پشتیبان ذخیره شد"));}catch(Exception e){final File retry=f;postUi(()->{toast("خطا: "+safeMsg(e)+"\nفایل پشتیبان موقت حفظ شد؛ می‌توانید دوباره محل ذخیره را انتخاب کنید.");if(retry!=null&&retry.isFile()){Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT);i.addCategory(Intent.CATEGORY_OPENABLE);i.setType("application/zip");i.putExtra(Intent.EXTRA_TITLE,retry.getName());startActivityForResult(i,CREATE_BACKUP);}});}});}
    File latestBackup(){try{return services.latestBackup();}catch(Exception e){throw new RuntimeException(e);}}
    void confirmRestore(){
        if(backupFlowActive){toast("ابتدا عملیات ذخیره پشتیبان جاری را تمام یا لغو کنید.");return;}
        new AlertDialog.Builder(this).setTitle("بازیابی پشتیبان").setMessage("اطلاعات فعلی برنامه با اطلاعات داخل فایل پشتیبان جایگزین می‌شود. قبل از ادامه، اگر لازم است یک Backup جدید بگیرید.").setNegativeButton("انصراف",null).setPositiveButton("انتخاب فایل",(d,w)->pick(PICK_BACKUP,"application/zip")).show();
    }
    void restore(Uri u){submitIo(()->{File f=new File(getCacheDir(),"restore_"+System.nanoTime()+".zip");try{try(InputStream in=getContentResolver().openInputStream(u)){if(in==null)throw new IOException("فایل قابل خواندن نیست");try(OutputStream out=new FileOutputStream(f)){byte[]b=new byte[32768];long total=0;int n;while((n=in.read(b))!=-1){total+=n;if(total>512L*1024L*1024L)throw new IOException("حجم فایل Backup بیش از 512MB است");out.write(b,0,n);}}}if(db!=null){db.close();db=null;}HseAppServices oldServices=services;if(oldServices==null)throw new IllegalStateException("services unavailable");oldServices.close();oldServices.restoreBackup(f);services=null;db=new LibraryDb(this);data=new HseDataRepository(db);if(!data.integrityOk())throw new IOException("سلامت دیتابیس بازیابی‌شده تأیید نشد");services=new HseAppServices(this,data);rescheduleReminders();postUi(()->{toast("بازیابی با موفقیت انجام شد");refreshAll();});}catch(Exception e){try{if(db!=null){db.close();db=null;}}catch(Exception ignored){}boolean recovered=false;try{File dbFile=getDatabasePath("hse.db");if(dbFile.isFile()){LibraryDb candidate=new LibraryDb(this);if(candidate.integrityOk()){db=candidate;data=new HseDataRepository(db);services=new HseAppServices(this,data);recovered=true;}else candidate.close();}}catch(Exception ignored){}final boolean ok=recovered;postUi(()->{toast(ok?"بازیابی ناموفق بود؛ اطلاعات قبلی حفظ شد.":"بازیابی ناموفق بود و برنامه برای جلوگیری از ایجاد دیتابیس خالی بسته می‌شود.");if(!ok)finish();});}finally{f.delete();}});}

    void schedule(long id,String title,String body,long at){if(services!=null)services.scheduleReminder(id,title,body,at);}
    void cancelReminder(long id){if(services!=null)services.cancelReminder(id);}
    void rescheduleReminders(){submitIo(()->{try{if(services!=null)services.reschedulePendingReminders();}catch(Exception ignored){}});}
    String analyzeVisionImage(File image, HseVisionAi.Progress progress) throws Exception { if(services==null) throw new IllegalStateException("services unavailable"); String result=services.analyzeImage(image, progress); visionAi=services.visionAi(); return result; }

    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){super.onRequestPermissionsResult(requestCode,permissions,grantResults);if(requestCode==NOTIFICATION_PERMISSION&&reminderFeature!=null){if(reminderFeature.hasNotificationPermission()){reminderFeature.continuePendingReminderRegistration();}else{reminderFeature.finishPendingReminder(true,"مجوز اعلان‌ها رد شد؛ یادآوری ثبت نشد.");}}}
    String formatTime(long m){return new SimpleDateFormat("yyyy/MM/dd  HH:mm",Locale.US).format(new Date(m));}
    String formatDate(long m){return new SimpleDateFormat("yyyy/MM/dd",Locale.US).format(new Date(m));}
    String shortText(String s,int n){if(s==null)return "";String x=s.replaceAll("[\\r\\n]+"," ").trim();return x.length()>n?x.substring(0,n)+"…":x;}
    String safeMsg(Exception e){String s=e.getMessage();return s==null||s.trim().isEmpty()?"خطای نامشخص":s;}
    void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
    @Override protected void onResume(){super.onResume();if(io!=null){rescheduleReminders();if(exactAlarmSettingsOpened&&pendingReminderAt>System.currentTimeMillis()&&ReminderScheduler.canScheduleExact(this)&&reminderFeature!=null)reminderFeature.continuePendingReminderRegistration();}}
    @Override protected void onDestroy(){
        destroyed=true;
        aiImageReadyAction=null;
        final ExecutorService executor=io;
        io=null;
        if(executor!=null){
            executor.shutdown();
            new Thread(()->{
                boolean terminated=false;
                try{
                    terminated=executor.awaitTermination(30,TimeUnit.SECONDS);
                    if(!terminated){
                        executor.shutdownNow();
                        terminated=executor.awaitTermination(5,TimeUnit.SECONDS);
                    }
                }catch(InterruptedException e){
                    Thread.currentThread().interrupt();
                    executor.shutdownNow();
                }finally{
                    if(terminated){
                        HseAppServices s=services;
                        services=null;
                        visionAi=null;
                        if(s!=null)try{s.close();}catch(Exception ignored){}
                        File aiImg=pendingAiImageFile;
                        pendingAiImageFile=null;
                        if(aiImg!=null)aiImg.delete();
                        LibraryDb d=db;
                        db=null;
                        if(d!=null)try{d.close();}catch(Exception ignored){}
                    }
                }
            },"hse-db-close").start();
        }else{
            HseAppServices s=services;
            services=null;
            visionAi=null;
            if(s!=null)try{s.close();}catch(Exception ignored){}
            File aiImg=pendingAiImageFile;
            pendingAiImageFile=null;
            if(aiImg!=null)aiImg.delete();
            LibraryDb d=db;
            db=null;
            if(d!=null)try{d.close();}catch(Exception ignored){}
        }
        super.onDestroy();
    }
}
