package com.ekbatan.hse.ui;

import android.app.Activity;
import android.view.View;
import android.widget.ScrollView;
import java.util.LinkedHashMap;
import java.util.Map;

/** Central navigation registry for the current single-screen shell. */
public final class HseSectionNavigator {
    private final Activity activity;
    private final ScrollView scroll;
    private final Map<String, View> sections = new LinkedHashMap<>();

    public HseSectionNavigator(Activity activity, ScrollView scroll) {
        if (activity == null) throw new IllegalArgumentException("activity == null");
        if (scroll == null) throw new IllegalArgumentException("scroll == null");
        this.activity = activity;
        this.scroll = scroll;
    }

    /** Registers a section once; duplicate keys do not silently replace an existing target. */
    public boolean register(String key, View view) {
        if (key == null || key.trim().isEmpty() || view == null) return false;
        if (sections.containsKey(key)) return false;
        sections.put(key, view);
        return true;
    }

    public View get(String key) { return sections.get(key); }
    public int size() { return sections.size(); }
    public boolean contains(String key) { return key != null && sections.containsKey(key); }

    public void open(String key) {
        final View view = sections.get(key);
        if (view == null || scroll == null) return;
        scroll.postDelayed(() -> {
            if (scroll.getHandler() == null || view.getParent() == null) return;
            scroll.smoothScrollTo(0, Math.max(0,
                    view.getTop() - HseUiTheme.dp(activity, 8)));
        }, 80);
    }

    public void clear() { sections.clear(); }
}
