package com.ekbatan.hse;

import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Space;
import com.ekbatan.hse.ui.HseFeature;

/** Backup/restore feature UI. The host owns the SAF transaction lifecycle. */
public final class BackupFeature implements HseFeature {
    private final MainActivity host;
    public BackupFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "backup";}
    @Override public String title(){return "پشتیبان‌گیری";}
    @Override public String icon(){return "💾";}
    @Override public View build(){
        LinearLayout c=host.card();host.navigator.register(key(),c);c.addView(host.sectionHead(icon(),"پشتیبان و بازیابی"));c.addView(host.text("پشتیبان کامل دیتابیس و کتابخانه PDF؛ بدون سرویس ابری و بدون هزینه",13,host.MUTED));host.gapIn(c,8);
        LinearLayout row=new LinearLayout(host);row.setOrientation(LinearLayout.HORIZONTAL);Button backup=host.action("💾 پشتیبان‌گیری",host.BLUE),restore=host.action("♻️ بازیابی",host.GREEN);
        row.addView(backup,new LinearLayout.LayoutParams(0,host.dp(48),1));row.addView(new Space(host),new LinearLayout.LayoutParams(host.dp(8),1));row.addView(restore,new LinearLayout.LayoutParams(0,host.dp(48),1));c.addView(row);
        backup.setOnClickListener(v->host.doBackup());restore.setOnClickListener(v->host.confirmRestore());host.addExportTools(c,"وضعیت پشتیبان",host::exportBackupStatus);host.addCard(c);return c;
    }
}
