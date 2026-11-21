package com.ekbatan.hse;

import android.content.*;
import android.os.*;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c,Intent intent){
        final PendingResult pending=goAsync();
        final Context app=c.getApplicationContext();
        new Thread(()->{
            try{
                for(int attempt=0;attempt<3;attempt++){
                    LibraryDb db=null;
                    try{
                        db=new LibraryDb(app);
                        if(!db.integrityOk()) throw new IllegalStateException("سلامت دیتابیس در زمان بوت تأیید نشد");
                        if(ReminderScheduler.canScheduleExact(app) && ReminderScheduler.hasNotificationPermission(app)){
                            for(LibraryDb.ReminderRow r:db.pendingReminders(System.currentTimeMillis())) ReminderScheduler.schedule(app,r);
                        }
                        break;
                    }catch(Exception e){
                        if(attempt==2) break;
                        try{Thread.sleep(350L*(attempt+1));}catch(InterruptedException x){Thread.currentThread().interrupt();break;}
                    }finally{if(db!=null)db.close();}
                }
            }finally{pending.finish();}
        },"hse-reminder-boot").start();
    }
}
