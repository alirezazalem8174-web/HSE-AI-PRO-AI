package com.ekbatan.hse;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.util.ArrayList;

/** HSE library feature. The activity remains the composition host; this class owns the library UI. */
public final class LibraryFeature implements HseFeature {
    private final MainActivity host;
    public LibraryFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "library";}
    @Override public String title(){return "کتابخانه";}
    @Override public String icon(){return "📚";}
    @Override public View build(){
        LinearLayout c=host.card();host.libraryView=c;host.navigator.register(key(),c);
        c.addView(host.sectionHead(icon(),"کتابخانه HSE"));
        c.addView(host.text("مدیریت منابع PDF، جست‌وجوی فارسی و جلوگیری از تکرار فایل",13,host.MUTED));host.gapIn(c,8);
        EditText q=host.input("مثلاً: کلاه ایمنی، ارزیابی ریسک، آتش‌سوزی...");c.addView(q);host.gapIn(c,8);
        LinearLayout row=new LinearLayout(host);row.setOrientation(LinearLayout.HORIZONTAL);
        Button add=host.action("＋ افزودن PDF",host.BLUE),search=host.action("🔎 جست‌وجو",host.GREEN);
        row.addView(add,new LinearLayout.LayoutParams(0,host.dp(48),1));row.addView(new Space(host),new LinearLayout.LayoutParams(host.dp(8),1));row.addView(search,new LinearLayout.LayoutParams(0,host.dp(48),1));c.addView(row);
        TextView results=host.text("",13,host.INK);results.setPadding(0,host.dp(12),0,0);c.addView(results);
        add.setOnClickListener(v->host.pick(host.PICK_PDF,"application/pdf"));
        search.setOnClickListener(v->{String query=q.getText().toString().trim();if(query.isEmpty()){results.setText("عبارت جست‌وجو را وارد کنید.");return;}results.setText("در حال جست‌وجو…");host.submitIo(()->{try{ArrayList<LibraryDb.SearchHit> r=host.data.searchHits(query);StringBuilder sb=new StringBuilder();for(LibraryDb.SearchHit h:r)sb.append("📄 ").append(h.title).append(" — صفحه ").append(h.page).append(" — امتیاز ").append(h.score).append("\n").append(h.snippet).append("\n\n────────────\n\n");if(sb.length()>0)sb.setLength(sb.length()-16);String finalText=sb.length()==0?"نتیجه‌ای پیدا نشد.":sb.toString();host.postUi(()->{host.latestSearchText=finalText;results.setText(finalText);});}catch(Exception e){host.postUi(()->results.setText("خطا در جست‌وجو: "+host.safeMsg(e)));}});});
        host.gapIn(c,8);host.addExportTools(c,"نتایج آخرین جست‌وجو",()->host.latestSearchText.isEmpty()?"هنوز جست‌وجویی انجام نشده است.":host.latestSearchText);
        host.gapIn(c,10);host.buildRecommendedBooks(c);host.gapIn(c,12);host.buildBuiltInBooks(c);host.gapIn(c,12);
        c.addView(host.title("منابع واردشده",15));host.addExportTools(c,"کتابخانه HSE",host::exportDocuments);
        LinearLayout list=new LinearLayout(host);list.setOrientation(LinearLayout.VERTICAL);c.addView(list);host.addCard(c);refreshDocuments(list);return c;
    }
    private void refreshDocuments(LinearLayout target){host.submitIo(()->{try{ArrayList<LibraryDb.DocumentRow> rows=host.data.listDocuments();host.postUi(()->{target.removeAllViews();if(rows.isEmpty()){host.clearList(target,"هنوز PDFی وارد نشده است.");return;}for(LibraryDb.DocumentRow r:rows){LinearLayout row=new LinearLayout(host);row.setGravity(Gravity.CENTER_VERTICAL);row.setPadding(0,host.dp(7),0,host.dp(7));TextView info=host.text("📄 "+r.title+"\n"+r.pages+" صفحه"+(r.category.isEmpty()?"":"  •  "+r.category),14,host.INK);Button del=host.outline("حذف");row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(del,new LinearLayout.LayoutParams(host.dp(62),host.dp(42)));target.setGravity(Gravity.RIGHT);del.setOnClickListener(v->confirmDelete(r.id,r.title,target));target.addView(row);}});}catch(Exception e){host.postUi(()->host.clearList(target,"خطا در بارگذاری منابع"));}});}
    private void confirmDelete(long id,String title,LinearLayout target){new AlertDialog.Builder(host).setTitle("حذف منبع").setMessage("این PDF و اطلاعات صفحات آن حذف می‌شود:\n\n"+title).setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->host.submitIo(()->{try{host.data.deleteDocument(id);host.postUi(()->{host.toast("منبع حذف شد");refreshDocuments(target);host.refreshAll();});}catch(Exception e){host.postUi(()->host.toast("خطا در حذف: "+host.safeMsg(e)));}})).show();}
}
