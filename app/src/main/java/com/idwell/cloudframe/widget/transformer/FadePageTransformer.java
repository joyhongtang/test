package com.idwell.cloudframe.widget.transformer;

import androidx.core.view.ViewCompat;
import android.view.View;

public class FadePageTransformer extends Transformer {

    @Override
    public void handleInvisiblePage(View view, float position) {
    }

    @Override
    public void handleLeftPage(View view, float position) {
        ViewCompat.setTranslationX(view, -view.getWidth() * position);
        ViewCompat.setAlpha(view, 1 + position);
    }

    @Override
    public void handleRightPage(View view, float position) {
        ViewCompat.setTranslationX(view, -view.getWidth() * position);
        ViewCompat.setAlpha(view, 1 - position);
    }

}