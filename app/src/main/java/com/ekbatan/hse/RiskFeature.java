package com.ekbatan.hse;

import android.app.AlertDialog;
import android.graphics.Color;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.Spinner;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.util.ArrayList;

/** Risk feature extracted from the application shell; persistence remains owned by the host. */
public final class RiskFeature implements HseFeature {
    private final MainActivity host;
    private LinearLayout section;
    public RiskFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "risk";}
    @Override public String title(){return "ارزیابی ریسک";}
    @Override public String icon(){return "⚠️";}
    @Override public View build(){
        LinearLayout c=host.card(); section=c; host.riskView=c; host.navigator.register(key(),c);
        c.addView(host.sectionHead(icon(),title()+" ۵×۵"));
        c.addView(host.text("احتمال × شدت = امتیاز؛ ۱–۴ کم، ۵–۱۱ متوسط، ۱۲–۱۹ زیاد، ۲۰–۲۵ بحرانی",13,host.MUTED));
        host.gapIn(c,8);
        EditText rh=host.input("خطر / فعالیت");c.addView(rh);host.gapIn(c,7);
        EditText rc=host.input("پیامد");c.addView(rc);host.gapIn(c,7);
        LinearLayout spinners=new LinearLayout(host);spinners.setOrientation(LinearLayout.HORIZONTAL);
        String[] vals={"1","2","3","4","5"};Spinner sl=new Spinner(host),ss=new Spinner(host);
        sl.setAdapter(new ArrayAdapter<String>(host,android.R.layout.simple_spinner_dropdown_item,vals));
        ss.setAdapter(new ArrayAdapter<String>(host,android.R.layout.simple_spinner_dropdown_item,vals));
        LinearLayout a=new LinearLayout(host),b=new LinearLayout(host);a.setOrientation(LinearLayout.VERTICAL);b.setOrientation(LinearLayout.VERTICAL);
        a.addView(host.text("احتمال",13,host.MUTED));a.addView(sl);b.addView(host.text("شدت",13,host.MUTED));b.addView(ss);
        spinners.addView(a,new LinearLayout.LayoutParams(0,-2,1));spinners.addView(new Space(host),new LinearLayout.LayoutParams(host.dp(8),1));spinners.addView(b,new LinearLayout.LayoutParams(0,-2,1));c.addView(spinners);
        host.gapIn(c,7);EditText ctrl=host.input("کنترل‌های موجود");c.addView(ctrl);host.gapIn(c,7);
        EditText ra=host.input("اقدام اصلاحی");c.addView(ra);host.gapIn(c,7);
        EditText rp=host.input("مسئول");c.addView(rp);host.gapIn(c,8);
        Button rs=host.action("✓ ثبت ارزیابی ریسک",host.ORANGE);c.addView(rs);
        LinearLayout list=new LinearLayout(host);list.setOrientation(LinearLayout.VERTICAL);host.gapIn(c,8);c.addView(list);
        rs.setOnClickListener(v->{int lv=Integer.parseInt((String)sl.getSelectedItem()),sv=Integer.parseInt((String)ss.getSelectedItem()),score=lv*sv;String h=rh.getText().toString().trim();if(h.isEmpty()){host.toast("خطر یا فعالیت را وارد کنید");return;}rs.setEnabled(false);host.submitIo(()->{try{host.data.addRisk(h,rc.getText().toString().trim(),lv,sv,ctrl.getText().toString().trim(),ra.getText().toString().trim(),rp.getText().toString().trim());host.postUi(()->{rh.setText("");rc.setText("");ctrl.setText("");ra.setText("");rp.setText("");rs.setEnabled(true);host.toast("ریسک ثبت شد — امتیاز: "+score+" — "+host.riskLevel(score));refresh(list);host.refreshAll();});}catch(Exception e){host.postUi(()->{rs.setEnabled(true);host.toast("خطا در ثبت ریسک: "+host.safeMsg(e));});}});});
        host.addCard(c);refresh(list);return c;
    }
    private void refresh(LinearLayout target){host.submitIo(()->{try{ArrayList<LibraryDb.RiskRow> rows=host.data.listRisks(12);host.postUi(()->{target.removeAllViews();if(rows.isEmpty()){host.clearList(target,"ارزیابی ریسکی ثبت نشده است.");return;}for(LibraryDb.RiskRow r:rows){LinearLayout row=new LinearLayout(host);row.setPadding(0,host.dp(6),0,host.dp(6));TextView info=host.text("⚠️ "+r.hazard+"\n"+host.riskLevel(r.score)+" — امتیاز "+r.score+" ("+r.likelihood+"×"+r.severity+")\n"+host.shortText(r.action,100),13,host.INK);info.setBackground(host.box(Color.rgb(250,250,250),host.riskColor(r.score),host.dp(10)));info.setPadding(host.dp(10),host.dp(8),host.dp(10),host.dp(8));Button del=host.outline("حذف");row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(del,new LinearLayout.LayoutParams(host.dp(62),host.dp(42)));target.addView(row);del.setOnClickListener(v->new AlertDialog.Builder(host).setTitle("حذف ارزیابی؟").setNegativeButton("انصراف",null).setPositiveButton("حذف",(d,w)->host.submitIo(()->{host.data.deleteRisk(r.id);host.postUi(()->{refresh(target);host.refreshAll();});})).show());}});}catch(Exception e){host.postUi(()->host.clearList(target,"خطا در بارگذاری ریسک‌ها"));}});}
}
