package com.ekbatan.hse;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.os.Build;

public class ReminderReceiver extends BroadcastReceiver {
    static final String CHANNEL="hse_reminders";
    @Override public void onReceive(Context c,Intent i){
        final PendingResult pending=goAsync();
        new Thread(()->{
            LibraryDb db=null;
            try{
                long id=i==null?0:i.getLongExtra("id",0);
                db=new LibraryDb(c);
                LibraryDb.ReminderRow r=db.getReminder(id);
                if(r==null||r.done)return;
                String failure=null;
                if(Build.VERSION.SDK_INT>=33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED) failure="مجوز اعلان‌ها فعال نیست";
                NotificationManager nm=(NotificationManager)c.getSystemService(Context.NOTIFICATION_SERVICE);
                if(failure==null && nm==null) failure="سرویس اعلان در دسترس نیست";
                if(failure==null && Build.VERSION.SDK_INT>=24 && !nm.areNotificationsEnabled()) failure="اعلان‌های برنامه در تنظیمات غیرفعال شده‌اند";
                if(failure==null){
                    if(Build.VERSION.SDK_INT>=26){
                        NotificationChannel ch=nm.getNotificationChannel(CHANNEL);
                        if(ch==null){ch=new NotificationChannel(CHANNEL,"یادآوری‌های HSE",NotificationManager.IMPORTANCE_HIGH);nm.createNotificationChannel(ch);}
                        if(ch.getImportance()==NotificationManager.IMPORTANCE_NONE) failure="کانال اعلان یادآوری‌ها غیرفعال است";
                    }
                }
                if(failure!=null){db.markReminderDelivery(id,"pending",failure);return;}
                Notification.Builder b=Build.VERSION.SDK_INT>=26?new Notification.Builder(c,CHANNEL):new Notification.Builder(c);
                Notification n=b.setContentTitle(r.title.isEmpty()?"یادآوری HSE":r.title).setContentText(r.body).setSmallIcon(android.R.drawable.ic_dialog_info).setAutoCancel(true).build();
                try{nm.notify(ReminderScheduler.requestCode(id),n);db.markReminderDelivery(id,"delivered","");db.setReminderDone(id,true);}
                catch(SecurityException se){db.markReminderDelivery(id,"pending","ارسال اعلان توسط سیستم مجاز نیست");}
            }catch(Exception e){if(db!=null)try{db.markReminderDelivery(i==null?0:i.getLongExtra("id",0),"failed",e.getMessage()==null?"خطای ناشناخته":e.getMessage());}catch(Exception ignored){}}
            finally{if(db!=null)db.close();pending.finish();}
        },"hse-reminder-delivery").start();
    }
}
