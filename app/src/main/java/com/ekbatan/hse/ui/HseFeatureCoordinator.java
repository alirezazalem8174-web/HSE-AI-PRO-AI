package com.ekbatan.hse.ui;

import android.view.View;
import android.widget.LinearLayout;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Coordinates feature registration/building without moving persistence or business logic
 * into the application shell.
 *
 * The coordinator deliberately prefers an explicit feature navigator registration over
 * the View returned by build(). This protects legacy feature builders that add spacing,
 * footer views, or other children after registering their actual section.
 */
public final class HseFeatureCoordinator {
    public interface Factory { HseFeature create(); }
    private final LinearLayout content;
    private final HseSectionNavigator navigator;
    private final Map<String, HseFeature> features = new LinkedHashMap<>();

    public HseFeatureCoordinator(LinearLayout content, HseSectionNavigator navigator) {
        if (content == null) throw new IllegalArgumentException("content == null");
        if (navigator == null) throw new IllegalArgumentException("navigator == null");
        this.content = content;
        this.navigator = navigator;
    }

    /** Adds and builds one feature. Duplicate keys are ignored to prevent silent replacement. */
    public boolean add(Factory factory) {
        if (factory == null) return false;
        final HseFeature feature = factory.create();
        if (feature == null) return false;
        final String key = feature.key();
        if (key == null || key.trim().isEmpty() || features.containsKey(key)) return false;

        final int before = content.getChildCount();
        final View returned = feature.build();
        final View registered = navigator.get(key);

        // Explicit registration is authoritative. Only fall back to the returned View
        // when a feature has not registered itself yet.
        if (registered == null) {
            View fallback = returned;
            if (fallback == null && content.getChildCount() > before) {
                fallback = content.getChildAt(content.getChildCount() - 1);
            }
            if (fallback != null) navigator.register(key, fallback);
        }

        if (navigator.get(key) == null) return false;
        features.put(key, feature);
        return true;
    }

    public HseFeature get(String key) { return features.get(key); }
    public int size() { return features.size(); }
    public boolean contains(String key) { return key != null && features.containsKey(key); }

    /** Clears only coordinator-owned references; Android Views remain owned by their parent. */
    public void clear() { features.clear(); }
}
