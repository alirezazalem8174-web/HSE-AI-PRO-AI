package com.ekbatan.hse.ui;

import android.view.View;

/** Small feature contract used to keep the application shell independent from feature details. */
public interface HseFeature {
    String key();
    String title();
    String icon();
    View build();
}
