package com.ekbatan.hse;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;
import java.util.ArrayList;
import java.util.Calendar;

/** Local reminder feature. The activity keeps lifecycle callbacks and shared reminder state. */
public final class ReminderFeature implements HseFeature {
    private final MainActivity host;
    public ReminderFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "reminder";}
    @Override public String title(){return "یادآوری‌ها";}
    @Override public String icon(){return "⏰";}
    @Override public View build(){
        LinearLayout c=host.card();host.reminderView=c;host.navigator.register(key(),c);c.addView(host.sectionHead(icon(),"یادآوری"));
        c.addView(host.text("یادآوری محلی روی دستگاه؛ ثبت فقط وقتی انجام می‌شود که مجوز اعلان و (در اندروید ۱۲+) زمان‌بندی دقیق فراهم باشد.",13,host.MUTED));host.gapIn(c,8);
        EditText rt=host.input("عنوان یادآوری");host.pendingReminderTitleView=rt;c.addView(rt);host.gapIn(c,7);EditText rb=host.input("توضیح");host.pendingReminderBodyView=rb;c.addView(rb);host.gapIn(c,8);
        host.selectedReminderTime=host.text("زمان انتخاب نشده",14,host.INK);host.selectedReminderTime.setGravity(android.view.Gravity.CENTER);host.selectedReminderTime.setBackground(host.box(Color.rgb(248,250,251),host.BORDER,host.dp(10)));c.addView(host.selectedReminderTime);host.gapIn(c,7);
        LinearLayout row=new LinearLayout(host);row.setOrientation(LinearLayout.HORIZONTAL);Button choose=host.outline("🗓 انتخاب زمان"),save=host.action("⏰ ثبت یادآوری",host.ORANGE);host.pendingReminderSave=save;
        row.addView(choose,new LinearLayout.LayoutParams(0,host.dp(48),1));row.addView(new Space(host),new LinearLayout.LayoutParams(host.dp(8),1));row.addView(save,new LinearLayout.LayoutParams(0,host.dp(48),1));c.addView(row);
        LinearLayout list=new LinearLayout(host);host.pendingReminderList=list;list.setOrientation(LinearLayout.VERTICAL);host.gapIn(c,8);c.addView(list);final Calendar[] picked={null};host.pendingReminderPicked=picked;
        choose.setOnClickListener(v->pickDateTime(picked));save.setOnClickListener(v->{String a=rt.getText().toString().trim(),b=rb.getText().toString().trim();if(a.isEmpty()){host.toast("عنوان یادآوری را وارد کنید");return;}if(picked[0]==null||picked[0].getTimeInMillis()<=System.currentTimeMillis()){host.toast("یک زمان آینده انتخاب کنید");return;}host.pendingReminderTitle=a;host.pendingReminderBody=b;host.pendingReminderAt=picked[0].getTimeInMillis();host.pendingRetryReminderId=-1;save.setEnabled(false);continuePendingReminderRegistration();});
        host.addCard(c);refresh(list);return c;
    }
    public boolean hasNotificationPermission(){return Build.VERSION.SDK_INT<33||host.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;}
    public void continuePendingReminderRegistration(){
        if(host.pendingReminderAt<=System.currentTimeMillis()||host.pendingReminderTitle.trim().isEmpty()){finish(false,"اطلاعات یادآوری نامعتبر است");return;}
        if(!hasNotificationPermission()){if(Build.VERSION.SDK_INT>=33){host.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},host.NOTIFICATION_PERMISSION);host.toast("برای ثبت یادآوری، اجازه اعلان‌ها را فعال کنید.");return;}}
        if(!ReminderScheduler.canScheduleExact(host)){host.exactAlarmSettingsOpened=true;boolean opened=ReminderScheduler.openExactAlarmSettings(host);host.toast(opened?"برای ثبت یادآوری، مجوز زمان‌بندی دقیق را در تنظیمات فعال کنید و سپس به برنامه برگردید.":"باز کردن تنظیمات مجوز زمان‌بندی دقیق ممکن نشد.");return;}
        host.exactAlarmSettingsOpened=false;final long retryId=host.pendingRetryReminderId;final String title=host.pendingReminderTitle,body=host.pendingReminderBody;final long at=host.pendingReminderAt;
        host.submitIo(()->{try{long id=retryId>0?retryId:host.data.addReminder(title,body,at);if(retryId>0)host.data.setReminderDone(id,false);boolean scheduled=ReminderScheduler.schedule(host,id,title,body,at);if(!scheduled){if(retryId<=0)host.data.deleteReminder(id);throw new IllegalStateException("زمان‌بندی دقیق مجاز نیست");}host.postUi(()->{if(host.pendingReminderTitleView!=null&&retryId<=0){host.pendingReminderTitleView.setText("");host.pendingReminderBodyView.setText("");}if(host.pendingReminderPicked!=null&&retryId<=0)host.pendingReminderPicked[0]=null;host.selectedReminderTime.setText("زمان انتخاب نشده");if(host.pendingReminderSave!=null)host.pendingReminderSave.setEnabled(true);host.pendingReminderTitle="";host.pendingReminderBody="";host.pendingReminderAt=0;host.pendingRetryReminderId=-1;host.toast(retryId>0?"یادآوری دوباره زمان‌بندی شد":"یادآوری با موفقیت ثبت و زمان‌بندی شد");if(host.pendingReminderList!=null)refresh(host.pendingReminderList);host.refreshAll();});}catch(Exception e){host.postUi(()->finish(true,"خطا در ثبت یادآوری: "+host.safeMsg(e)));}});
    }
    public void finishPendingReminder(boolean keep,String message){finish(keep,message);}
    private void finish(boolean keep,String message){if(host.pendingReminderSave!=null)host.pendingReminderSave.setEnabled(true);if(!keep){host.pendingReminderTitle="";host.pendingReminderBody="";host.pendingReminderAt=0;host.pendingRetryReminderId=-1;}host.toast(message);}
    private void pickDateTime(Calendar[] holder){Calendar now=Calendar.getInstance();DatePickerDialog d=new DatePickerDialog(host,(v,y,m,day)->{Calendar c=Calendar.getInstance();c.set(Calendar.YEAR,y);c.set(Calendar.MONTH,m);c.set(Calendar.DAY_OF_MONTH,day);TimePickerDialog t=new TimePickerDialog(host,(vv,h,min)->{c.set(Calendar.HOUR_OF_DAY,h);c.set(Calendar.MINUTE,min);c.set(Calendar.SECOND,0);c.set(Calendar.MILLISECOND,0);holder[0]=c;host.selectedReminderTime.setText(host.formatTime(c.getTimeInMillis()));},now.get(Calendar.HOUR_OF_DAY),now.get(Calendar.MINUTE),true);t.show();},now.get(Calendar.YEAR),now.get(Calendar.MONTH),now.get(Calendar.DAY_OF_MONTH));d.show();}
    private void refresh(LinearLayout target){host.submitIo(()->{try{ArrayList<LibraryDb.ReminderRow> rows=host.data.listReminders(10);host.postUi(()->{target.removeAllViews();if(rows.isEmpty()){host.clearList(target,"یادآوری ثبت نشده است.");return;}for(LibraryDb.ReminderRow r:rows){String state=r.done?"✓ ":("failed".equals(r.deliveryState)?"⚠️ ":"⏰ ");String detail="\n"+host.formatTime(r.at)+(r.body.isEmpty()?"":"  •  "+host.shortText(r.body,90));if("failed".equals(r.deliveryState)&&!r.lastDeliveryError.isEmpty())detail+="\nخطا: "+r.lastDeliveryError;LinearLayout row=new LinearLayout(host);row.setPadding(0,host.dp(6),0,host.dp(6));TextView info=host.text(state+r.title+detail,13,r.done?host.MUTED:("failed".equals(r.deliveryState)?host.RED:host.INK));Button done=host.outline(r.done?"بازکردن":("failed".equals(r.deliveryState)?"تلاش مجدد":"انجام شد"));Button del=host.outline("حذف");LinearLayout acts=new LinearLayout(host);acts.setOrientation(LinearLayout.VERTICAL);acts.addView(done);acts.addView(del);row.addView(info,new LinearLayout.LayoutParams(0,-2,1));row.addView(acts,new LinearLayout.LayoutParams(host.dp(82),-2));target.addView(row);done.setOnClickListener(v->{if(r.done){if(r.at<=System.currentTimeMillis()){host.toast("زمان این یادآوری گذشته است؛ برای زمان جدید دوباره ثبت کنید");return;}host.pendingReminderTitle=r.title;host.pendingReminderBody=r.body;host.pendingReminderAt=r.at;host.pendingRetryReminderId=r.id;host.pendingReminderSave.setEnabled(false);continuePendingReminderRegistration();}else if("failed".equals(r.deliveryState)){host.pendingReminderTitle=r.title;host.pendingReminderBody=r.body;host.pendingReminderAt=r.at;host.pendingRetryReminderId=r.id;host.pendingReminderSave.setEnabled(false);continuePendingReminderRegistration();}else{host.submitIo(()->{host.data.setReminderDone(r.id,true);host.cancelReminder(r.id);host.postUi(()->{refresh(target);host.refreshAll();});});}});del.setOnClickListener(v->host.submitIo(()->{host.data.deleteReminder(r.id);host.cancelReminder(r.id);host.postUi(()->{refresh(target);host.refreshAll();});}));}});}catch(Exception e){host.postUi(()->host.clearList(target,"خطا در بارگذاری یادآوری‌ها"));}});}
}
