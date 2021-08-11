package com.idwell.cloudframe.widget.transformer;

import androidx.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@IntDef({TransformerEffect.Default, TransformerEffect.Alpha, TransformerEffect.Rotate, TransformerEffect.Cube, TransformerEffect.Flip, TransformerEffect.Accordion, TransformerEffect.ZoomFade, TransformerEffect.Fade, TransformerEffect.ZoomCenter, TransformerEffect.ZoomStack, TransformerEffect.Depth, TransformerEffect.Zoom})
public @interface TransformerEffect {
    int Default = 0;
    int Alpha = 1;
    int Rotate = 2;
    int Cube = 3;
    int Flip = 4;
    int Accordion = 5;
    int ZoomFade = 6;
    int Fade = 7;
    int ZoomCenter = 8;
    int ZoomStack = 9;
    int Depth = 10;
    int Zoom = 11;
}