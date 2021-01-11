package com.idwell.cloudframe.widget.transformer;

import androidx.annotation.NonNull;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import android.view.View;

public abstract class Transformer implements ViewPager.PageTransformer {

	@Override
	public void transformPage(@NonNull View page, float position) {
		onPreTransform(page, position);

		if (position < -1.0f) {
			// [-Infinity,-1)
			// This page is way off-screen to the left.
			handleInvisiblePage(page, position);
		} else if (position <= 0.0f) {
			// [-1,0]
			// Use the default slide transition when moving to the left page
			handleLeftPage(page, position);
		} else if (position <= 1.0f) {
			// (0,1]
			handleRightPage(page, position);
		} else {
			// (1,+Infinity]
			// This page is way off-screen to the right.
			handleInvisiblePage(page, position);
		}
	}

	public abstract void handleInvisiblePage(View view, float position);

	public abstract void handleLeftPage(View view, float position);

	public abstract void handleRightPage(View view, float position);

	public static Class<? extends Transformer> getPageTransformer(@TransformerEffect int effect) {
		switch (effect) {
			case TransformerEffect.Default:
				return DefaultPageTransformer.class;
			case TransformerEffect.Alpha:
				return AlphaPageTransformer.class;
			case TransformerEffect.Rotate:
				return RotatePageTransformer.class;
			case TransformerEffect.Cube:
				return CubePageTransformer.class;
			case TransformerEffect.Flip:
				return FlipPageTransformer.class;
			case TransformerEffect.Accordion:
				return AccordionPageTransformer.class;
			case TransformerEffect.ZoomFade:
				return ZoomFadePageTransformer.class;
			case TransformerEffect.Fade:
				return FadePageTransformer.class;
			case TransformerEffect.ZoomCenter:
				return ZoomCenterPageTransformer.class;
			case TransformerEffect.ZoomStack:
				return ZoomStackPageTransformer.class;
			case TransformerEffect.Depth:
				return DepthPageTransformer.class;
			case TransformerEffect.Zoom:
				return ZoomPageTransformer.class;
			default:
				return DefaultPageTransformer.class;
		}
	}

	/**
	 * Called each {@link #transformPage(View, float)} before {{@link #transformPage(View, float)}.
	 * <p>
	 * The default implementation attempts to reset all view properties. This is useful when toggling transforms that do
	 * not modify the same page properties. For instance changing from a transformation that applies rotation to a
	 * transformation that fades can inadvertently leave a fragment stuck with a rotation or with some degree of applied
	 * alpha.
	 *
	 * @param page
	 *            Apply the transformation to this page
	 * @param position
	 *            Position of page relative to the current front-and-center position of the pager. 0 is front and
	 *            center. 1 is one full page position to the right, and -1 is one page mPosition to the left.
	 */
	protected void onPreTransform(View page, float position) {
		page.setVisibility(View.VISIBLE);
		page.setAlpha(1);
		page.setPivotX(page.getMeasuredWidth() * 0.5f);
		page.setPivotY(page.getMeasuredHeight() * 0.5f);
		page.setTranslationX(0);
		page.setTranslationY(0);
		page.setScaleX(1);
		page.setScaleY(1);
		page.setRotationX(0);
		page.setRotationY(0);
		page.setRotation(0);
	}
}
