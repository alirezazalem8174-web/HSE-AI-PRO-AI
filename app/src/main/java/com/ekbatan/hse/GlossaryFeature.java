package com.ekbatan.hse;

import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.ekbatan.hse.ui.HseFeature;

/** Offline HSE glossary feature. */
public final class GlossaryFeature implements HseFeature {
    private final MainActivity host;
    public GlossaryFeature(MainActivity host){if(host==null)throw new IllegalArgumentException("host == null");this.host=host;}
    @Override public String key(){return "glossary";}
    @Override public String title(){return "واژه‌نامه";}
    @Override public String icon(){return "📖";}
    @Override public View build(){
        LinearLayout c=host.card();host.glossaryView=c;host.navigator.register(key(),c);
        c.addView(host.sectionHead(icon(),"واژه‌ها و کلمات اختصاری HSE"));c.addView(host.text("واژه‌های پرکاربرد ایمنی، بهداشت، محیط‌زیست و مدیریت ریسک — کاملاً آفلاین",13,host.MUTED));host.gapIn(c,8);
        EditText q=host.input("جست‌وجو: PPE، LOTO، ارزیابی ریسک، مجوز کار...");c.addView(q);host.gapIn(c,8);
        TextView out=host.text("",13,host.INK);out.setGravity(android.view.Gravity.RIGHT|android.view.Gravity.TOP);out.setTextIsSelectable(true);c.addView(out);
        host.addExportTools(c,"واژه‌نامه HSE",host::exportGlossary);
        q.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){out.setText(host.filterGlossary(s.toString()));}public void afterTextChanged(android.text.Editable e){}});
        out.setText(host.filterGlossary(""));host.addCard(c);return c;
    }
}
