package com.ekbatan.hse;

import android.view.View;
import com.ekbatan.hse.ui.HseFeature;

/** Transitional adapter for legacy builders that have not yet been extracted into feature classes. */
public final class HostFeatureAdapter implements HseFeature {
    private final MainActivity host;
    private final String key,title,icon;
    private final Runnable builder;
    public HostFeatureAdapter(MainActivity host,String key,String title,String icon,Runnable builder){
        if(host==null)throw new IllegalArgumentException("host == null");
        if(key==null||key.trim().isEmpty())throw new IllegalArgumentException("key is empty");
        if(builder==null)throw new IllegalArgumentException("builder == null");
        this.host=host;this.key=key;this.title=title;this.icon=icon;this.builder=builder;
    }
    @Override public String key(){return key;}
    @Override public String title(){return title;}
    @Override public String icon(){return icon;}
    @Override public View build(){
        int before=host.content.getChildCount();
        builder.run();
        View registered=host.navigator.get(key);
        if(registered!=null)return registered;
        if(host.content.getChildCount()>before){
            View fallback=host.content.getChildAt(host.content.getChildCount()-1);
            host.navigator.register(key,fallback);
            return fallback;
        }
        return null;
    }
}
