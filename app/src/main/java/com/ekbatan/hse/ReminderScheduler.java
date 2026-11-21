package com.ekbatan.hse;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.Manifest;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

/** Single source of truth for reminder alarms. Exact alarms are required on Android 12+. */
public final class ReminderScheduler {
    private ReminderScheduler() {}

    public static boolean hasNotificationPermission(Context c){
        return Build.VERSION.SDK_INT<33 || c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED;
    }

    public static boolean canScheduleExact(Context c){
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return false;
        return Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms();
    }

    public static boolean openExactAlarmSettings(Context c){
        if(Build.VERSION.SDK_INT<31)return false;
        try{
            Intent i=new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
            i.setData(Uri.parse("package:"+c.getPackageName()));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        }catch(Exception e){
            try{
                Intent i=new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                i.setData(Uri.parse("package:"+c.getPackageName()));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return true;
            }catch(Exception ignored){return false;}
        }
    }

    public static boolean schedule(Context c, LibraryDb.ReminderRow r){
        if(r==null||r.id<=0||r.done||r.at<=0)return false;
        return schedule(c,r.id,r.title,r.body,r.at);
    }

    public static boolean schedule(Context c,long id,String title,String body,long when){
        if(id<=0||when<=0)return false;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return false;
        if(Build.VERSION.SDK_INT>=31 && !am.canScheduleExactAlarms()) return false;
        Intent i=new Intent(c,ReminderReceiver.class).setPackage(c.getPackageName()).setData(Uri.parse("hse://reminder/"+id)).putExtra("id",id).putExtra("title",title==null?"":title).putExtra("body",body==null?"":body);
        PendingIntent p=PendingIntent.getBroadcast(c,requestCode(id),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        try {
            if(Build.VERSION.SDK_INT>=23) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,p);
            else am.setExact(AlarmManager.RTC_WAKEUP,when,p);
            return true;
        } catch (SecurityException | IllegalArgumentException e) {
            return false;
        }
    }

    /** Cancels a previously scheduled reminder without allowing an alarm cleanup failure to crash the app. */
    public static void cancel(Context c,long id){
        if(c==null||id<=0)return;
        AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
        if(am==null)return;
        Intent i=new Intent(c,ReminderReceiver.class).setPackage(c.getPackageName()).setData(Uri.parse("hse://reminder/"+id));
        PendingIntent p=null;
        try{
            p=PendingIntent.getBroadcast(c,requestCode(id),i,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            am.cancel(p);
        }catch(SecurityException | IllegalArgumentException ignored){
            // Cancellation is best-effort; the app must remain usable even if the OS rejects it.
        }finally{
            if(p!=null)try{p.cancel();}catch(Exception ignored){}
        }
    }
    public static int requestCode(long id){return (int)(id ^ (id>>>32));}
}
