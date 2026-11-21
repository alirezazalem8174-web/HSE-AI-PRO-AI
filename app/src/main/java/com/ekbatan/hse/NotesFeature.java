package com.ekbatan.hse;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.util.ArrayList;

/** Daily HSE notes feature. */
public final class NotesFeature implements HseFeature {
    private final MainActivity host;
    public NotesFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "notes";}
    @Override public String title(){return "یادداشت‌ها";}
    @Override public String icon(){return "📝";}
    @Override public View build(){
        LinearLayout c=host.card();host.notesView=c;host.navigator.register(key(),c);c.addView(host.sectionHead(icon(),"یادداشت روزانه"));c.addView(host.text("ثبت نکات بازدید، پیگیری‌ها، جلسات و کارهای روزانه",13,host.MUTED));host.gapIn(c,8);
        EditText nt=host.input("عنوان یادداشت");c.addView(nt);host.gapIn(c,7);EditText nb=host.input("متن یادداشت");nb.setMinLines(4);c.addView(nb);host.gapIn(c,8);
        Button save=host.action("✓ ذخیره یادداشت",host.GREEN);c.addView(save);LinearLayout list=new LinearLayout(host);list.setOrientation(LinearLayout.VERTICAL);host.gapIn(c,8);c.addView(list);
        save.setOnClickListener(v->{String a=nt.getText().toString().trim(),b=nb.getText().toString().trim();if(a.isEmpty()&&b.isEmpty()){host.toast("عنوان یا متن یادداشت را وارد کنید");return;}save.setEnabled(false);host.submitIo(()->{try{host.data.addNote(a,b);host.postUi(()->{nt.setText("");nb.setText("");save.setEnabled(true);host.toast("یادداشت ذخیره شد");refresh(list);host.refreshAll();});}catch(Exception e){host.postUi(()->{save.setEnabled(true);host.toast("خطا در ذخیره یادداشت: "+host.safeMsg(e));});}});});
        host.addCard(c);refresh(list);return c;
    }
    private void refresh(LinearLayout target){host.submitIo(()->{try{ArrayList<LibraryDb.NoteRow> rows=host.data.listNotes(8);host.postUi(()->{target.removeAllViews();if(rows.isEmpty()){host.clearList(target,"یادداشتی ثبت نشده است.");return;}for(LibraryDb.NoteRow r:rows){LinearLayout row=new LinearLayout(host);row.setPadding(0,host.dp(6),0,host.dp(6));TextView info=host.text("📝 "+(r.title.isEmpty()?"بدون عنوان":r.title)+"\n"+host.shortText(r.body,140),13,host.INK);Button del=host.outline("حذف");row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(del,new LinearLayout.LayoutParams(host.dp(62),host.dp(42)));target.addView(row);del.setOnClickListener(v->confirmDelete(r.id,target));}});}catch(Exception e){host.postUi(()->host.clearList(target,"خطا در بارگذاری یادداشت‌ها"));}});}
    private void confirmDelete(long id,LinearLayout target){new AlertDialog.Builder(host).setTitle("حذف یادداشت؟").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->host.submitIo(()->{try{host.data.deleteNote(id);host.postUi(()->{refresh(target);host.refreshAll();});}catch(Exception e){host.postUi(()->host.toast("خطا در حذف یادداشت: "+host.safeMsg(e)));}})).show();}
}
