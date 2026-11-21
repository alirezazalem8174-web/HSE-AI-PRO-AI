package com.ekbatan.hse;

import android.app.AlertDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.util.ArrayList;

/** Offline risk/finding library: filtering, image comparison and NCR creation. */
public final class RiskLibraryFeature implements HseFeature {
    private final MainActivity host;
    private LinearLayout section, list;
    private final ArrayList<LibraryDb.FindingRow> selected = new ArrayList<>();
    public RiskLibraryFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "risk_library";}
    @Override public String title(){return "کتابخانه یافته‌های HSE";}
    @Override public String icon(){return "🗂️";}
    @Override public View build(){
        section=host.card(); host.navigator.register(key(),section);
        section.addView(host.sectionHead(icon(),title()));
        section.addView(host.text("یافته‌های تأییدشده روی دستگاه ذخیره می‌شوند؛ می‌توانید آن‌ها را فیلتر، با دو تصویر مقایسه و به NCR تبدیل کنید.",12,host.MUTED));
        host.gapIn(section,8);
        LinearLayout filters=new LinearLayout(host); Spinner cat=new Spinner(host); String[] cats={"همه دسته‌ها","داربست","ارتفاع","PPE","برق","حریق","ماشین‌آلات","سایر"}; cat.setAdapter(new ArrayAdapter<String>(host,android.R.layout.simple_spinner_dropdown_item,cats));
        EditText q=host.input("جست‌وجوی یافته / شاهد / خطر"); filters.setOrientation(LinearLayout.VERTICAL);filters.addView(q);host.gapIn(filters,5);filters.addView(cat);section.addView(filters);host.gapIn(section,7);
        LinearLayout actions=new LinearLayout(host); Button compare=host.action("🖼 مقایسه دو تصویر",host.TEAL); Button clear=host.outline("پاک‌کردن انتخاب"); actions.addView(compare,new LinearLayout.LayoutParams(0,dp(46),1)); actions.addView(clear,new LinearLayout.LayoutParams(0,dp(46),1)); section.addView(actions);host.gapIn(section,7);
        list=new LinearLayout(host);list.setOrientation(LinearLayout.VERTICAL);section.addView(list);
        Runnable refresh=()->refresh(list,q.getText().toString().trim(),(String)cat.getSelectedItem()); q.setOnEditorActionListener((v,a,e)->{refresh.run();return false;}); cat.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){} public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}});
        compare.setOnClickListener(v->compareSelected()); clear.setOnClickListener(v->{selected.clear();refresh.run();host.toast("انتخاب تصاویر پاک شد.");});
        host.addCard(section); refresh.run(); return section;
    }
    private int dp(int n){return host.dp(n);}
    private void refresh(LinearLayout target,String query,String category){host.submitIo(()->{try{ArrayList<LibraryDb.FindingRow> rows=host.data.listInspectionFindings(200);String q=PersianNormalizer.normalize(query);String cat=category==null?"همه دسته‌ها":category;ArrayList<LibraryDb.FindingRow> filtered=new ArrayList<>();for(LibraryDb.FindingRow r:rows){String blob=PersianNormalizer.normalize(r.title+" "+r.evidence+" "+r.hazard+" "+r.consequence+" "+r.category);boolean ok=q.isEmpty()||blob.contains(q);if(!"همه دسته‌ها".equals(cat))ok=ok&&PersianNormalizer.normalize(r.category).contains(PersianNormalizer.normalize(cat));if(ok)filtered.add(r);}host.postUi(()->{target.removeAllViews();if(filtered.isEmpty()){host.clearList(target,"یافته‌ای مطابق فیلتر پیدا نشد.");return;}for(LibraryDb.FindingRow r:filtered) addRow(target,r);});}catch(Exception e){host.postUi(()->host.clearList(target,"خطا در بارگذاری کتابخانه یافته‌ها"));}});}
    private void addRow(LinearLayout target,LibraryDb.FindingRow r){LinearLayout row=host.card(); TextView t=host.text("🔎 "+r.title+"\n"+r.category+" • اولویت: "+r.priority+" • اطمینان: "+r.confidence+"\n"+host.shortText(r.evidence,150),13,host.INK); row.addView(t); LinearLayout b=new LinearLayout(host); Button sel=host.outline(selected.contains(r)?"✓ انتخاب شد":"انتخاب تصویر");Button ncr=host.outline("NCR");b.addView(sel,new LinearLayout.LayoutParams(0,42,1));b.addView(ncr,new LinearLayout.LayoutParams(0,42,1));row.addView(b);sel.setOnClickListener(v->{if(selected.contains(r))selected.remove(r);else if(selected.size()<2)selected.add(r);else {host.toast("برای مقایسه فقط دو تصویر انتخاب کنید.");return;}sel.setText(selected.contains(r)?"✓ انتخاب شد":"انتخاب تصویر");});ncr.setOnClickListener(v->showNcr(r));target.addView(row);host.gap(5);}
    private void compareSelected(){if(selected.size()!=2){host.toast("ابتدا دقیقاً دو یافته دارای تصویر را انتخاب کنید.");return;}LibraryDb.FindingRow a=selected.get(0),b=selected.get(1);if(a.imagePath==null||a.imagePath.isEmpty()||b.imagePath==null||b.imagePath.isEmpty()){host.toast("هر دو یافته باید تصویر ذخیره‌شده داشته باشند.");return;}LinearLayout box=new LinearLayout(host);box.setOrientation(LinearLayout.HORIZONTAL);ImageView ia=image(a.imagePath),ib=image(b.imagePath);box.addView(ia,new LinearLayout.LayoutParams(0,host.dp(190),1));box.addView(ib,new LinearLayout.LayoutParams(0,host.dp(190),1));TextView info=host.text("تصویر اول: "+a.title+"\n\nتصویر دوم: "+b.title+"\n\nاین مقایسه صرفاً بصری است و جایگزین بازدید حضوری یا قضاوت مهندسی نیست.",12,host.MUTED);ScrollView sv=new ScrollView(host);LinearLayout all=new LinearLayout(host);all.setOrientation(LinearLayout.VERTICAL);all.addView(box);all.addView(info);sv.addView(all);new AlertDialog.Builder(host).setTitle("مقایسه دو یافته HSE").setView(sv).setPositiveButton("بستن",null).show();}
    private ImageView image(String path){ImageView v=new ImageView(host);v.setScaleType(ImageView.ScaleType.CENTER_CROP);Bitmap bm=BitmapFactory.decodeFile(path);if(bm!=null)v.setImageBitmap(bm);else v.setImageResource(android.R.drawable.ic_menu_report_image);return v;}
    private void showNcr(LibraryDb.FindingRow f){LinearLayout c=new LinearLayout(host);c.setOrientation(LinearLayout.VERTICAL);EditText title=host.input("عنوان NCR");title.setText(f.title);EditText desc=host.input("شرح عدم انطباق / وضعیت مشاهده‌شده");desc.setText(f.evidence+"\nخطر: "+f.hazard+"\nپیامد: "+f.consequence);EditText req=host.input("مرجع / الزام مرتبط (اختیاری)");EditText immediate=host.input("اقدام فوری");immediate.setText(f.immediate);EditText corrective=host.input("اقدام اصلاحی");corrective.setText(f.corrective);EditText responsible=host.input("مسئول اقدام");c.addView(title);c.addView(desc);c.addView(req);c.addView(immediate);c.addView(corrective);c.addView(responsible);new AlertDialog.Builder(host).setTitle("ایجاد گزارش عدم انطباق (NCR)").setView(c).setNegativeButton("انصراف",null).setPositiveButton("ثبت NCR",(d,w)->host.submitIo(()->{try{long id=host.data.addNcr(f.id,title.getText().toString(),desc.getText().toString(),req.getText().toString(),immediate.getText().toString(),corrective.getText().toString(),responsible.getText().toString(),0);host.postUi(()->host.toast("NCR با موفقیت ثبت شد — شناسه: "+id));}catch(Exception e){host.postUi(()->host.toast("خطا در ثبت NCR: "+host.safeMsg(e)));}})).show();}
}
