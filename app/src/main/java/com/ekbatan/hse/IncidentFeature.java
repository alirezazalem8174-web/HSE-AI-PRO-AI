package com.ekbatan.hse;

import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.util.ArrayList;

/** Incident and near-miss feature extracted from the application shell. */
public final class IncidentFeature implements HseFeature {
    private final MainActivity host;
    public IncidentFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "incident";}
    @Override public String title(){return "ثبت حادثه / شبه‌حادثه";}
    @Override public String icon(){return "🚨";}
    @Override public View build(){
        LinearLayout c=host.card();host.incidentView=c;host.navigator.register(key(),c);
        c.addView(host.sectionHead(icon(),title()));c.addView(host.text("ثبت رویداد، علت و اقدام اصلاحی برای پیگیری HSE",13,host.MUTED));host.gapIn(c,8);
        EditText it=host.input("عنوان رویداد");c.addView(it);host.gapIn(c,7);EditText id=host.input("شرح");id.setMinLines(3);c.addView(id);host.gapIn(c,7);
        EditText ia=host.input("اقدام فوری");c.addView(ia);host.gapIn(c,7);EditText ir=host.input("علت ریشه‌ای");c.addView(ir);host.gapIn(c,7);EditText ic=host.input("اقدام اصلاحی");c.addView(ic);host.gapIn(c,8);
        Button is=host.action("✓ ثبت رویداد",host.RED);c.addView(is);LinearLayout list=new LinearLayout(host);list.setOrientation(LinearLayout.VERTICAL);host.gapIn(c,8);c.addView(list);
        is.setOnClickListener(v->{String t=it.getText().toString().trim();if(t.isEmpty()){host.toast("عنوان رویداد را وارد کنید");return;}is.setEnabled(false);host.submitIo(()->{try{host.data.addIncident(t,System.currentTimeMillis(),id.getText().toString().trim(),ia.getText().toString().trim(),ir.getText().toString().trim(),ic.getText().toString().trim());host.postUi(()->{it.setText("");id.setText("");ia.setText("");ir.setText("");ic.setText("");is.setEnabled(true);host.toast("رویداد ثبت شد");refresh(list);host.refreshAll();});}catch(Exception e){host.postUi(()->{is.setEnabled(true);host.toast("خطا در ثبت رویداد: "+host.safeMsg(e));});}});});
        host.addCard(c);refresh(list);return c;
    }
    private void refresh(LinearLayout target){host.submitIo(()->{try{ArrayList<LibraryDb.IncidentRow> rows=host.data.listIncidents(10);host.postUi(()->{target.removeAllViews();if(rows.isEmpty()){host.clearList(target,"رویدادی ثبت نشده است.");return;}for(LibraryDb.IncidentRow r:rows){LinearLayout row=new LinearLayout(host);row.setPadding(0,host.dp(6),0,host.dp(6));TextView info=host.text("🚨 "+r.title+"\n"+host.formatDate(r.eventDate)+" — "+r.status+"\n"+host.shortText(r.corrective,120),13,host.INK);Button del=host.outline("حذف");row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(del,new LinearLayout.LayoutParams(host.dp(62),host.dp(42)));target.addView(row);del.setOnClickListener(v->new AlertDialog.Builder(host).setTitle("حذف رویداد؟").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->host.submitIo(()->{host.data.deleteIncident(r.id);host.postUi(()->{refresh(target);host.refreshAll();});})).show());}});}catch(Exception e){host.postUi(()->host.clearList(target,"خطا در بارگذاری رویدادها"));}});}
}
