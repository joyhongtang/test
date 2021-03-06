package com.joyhong.test.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ReplacementSpan;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import androidx.annotation.NonNull;


public class SysUtils {

    /**
     * 将dp转换为px
     *
     * @param dp
     * @return
     */
    public static int convertDpToPixel(Context context, float dp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (metrics.density * dp + 0.5f);
    }


    public static int convertSpToPixel(Context context, float sp) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        return (int) (TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP,
                sp, metrics));
    }

    /**
     * 将字符串转换成算术式并进行计算
     *
     * @param str 算术
     * @return 结果
     */
    public static double eval(final String str) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < str.length()) ? str.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') {
                    nextChar();
                }
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < str.length()) {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }
                return x;
            }

            // Grammar:
            // expression = term | expression `+` term | expression `-` term
            // term = factor | term `*` factor | term `/` factor
            // factor = `+` factor | `-` factor | `(` expression `)`
            //        | number | functionName factor | factor `^` factor

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) {
                        // addition
                        x += parseTerm();
                    } else if (eat('-')) {
                        // subtraction
                        x -= parseTerm();
                    } else {
                        return x;
                    }
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) {
                        // multiplication
                        x *= parseFactor();
                    } else if (eat('/')) {
                        // division
                        x /= parseFactor();
                    } else {
                        return x;
                    }
                }
            }

            double parseFactor() {
                if (eat('+')) {
                    // unary plus
                    return parseFactor();
                }
                if (eat('-')) {
                    // unary minus
                    return -parseFactor();
                }

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    // parentheses
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    // numbers
                    while ((ch >= '0' && ch <= '9') || ch == '.') {
                        nextChar();
                    }
                    x = Double.parseDouble(str.substring(startPos, this.pos));
                } else if (ch >= 'a' && ch <= 'z') {
                    // functions
                    while (ch >= 'a' && ch <= 'z') {
                        nextChar();
                    }
                    String func = str.substring(startPos, this.pos);
                    x = parseFactor();
                    if (func.equals("sqrt")) {
                        x = Math.sqrt(x);
                    } else if (func.equals("sin")) {
                        x = Math.sin(Math.toRadians(x));
                    } else if (func.equals("cos")) {
                        x = Math.cos(Math.toRadians(x));
                    } else if (func.equals("tan")) {
                        x = Math.tan(Math.toRadians(x));
                    } else {
                        throw new RuntimeException("Unknown function: " + func);
                    }
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                if (eat('^')) {
                    // exponentiation
                    x = Math.pow(x, parseFactor());
                }
                return x;
            }
        }.parse();
    }

    /**
     * 字符转化为浮点数
     */
    public static float parseFloat(String nStr) {
        float n;
        try {
            n = Float.parseFloat(nStr);
        } catch (Exception e) {
            n = 0.0f;
        }
        return n;
    }

    /**
     * 字符转化为数字(正整数)
     */
    public static int parseInt(String nStr) {
        int n;
        try {
            n = Integer.parseInt(nStr);
        } catch (Exception e) {
            n = -1;
        }
        return n;
    }

    /**
     * 字符转化为数字(正整数)
     */
    public static long parseLong(String nStr) {
        long n;
        try {
            n = Long.parseLong(nStr);
        } catch (Exception e) {
            n = -1;
        }
        return n;
    }

    /**
     * 字符转化为浮点数
     */
    public static double parseDouble(String nStr) {
        double n;
        try {
            n = Double.parseDouble(nStr);
        } catch (Exception e) {
            n = 0.0f;
        }
        return n;
    }

    public static boolean isEmpty(String string) {
        if (string == null || string.length() == 0) {
            return true;
        } else {
            return false;
        }
    }


    /**
     * 获取不为空的字符串
     *
     * @param string
     * @return string
     */
    public static String getSafeString(String string) {
        if (!isEmpty(string)) {
            return string;
        } else {
            return "";
        }
    }

    /**
     * 判断String是不是为纯字母数字
     */
    public static boolean IsNumLetter(String letter) {
        if (letter == null || letter.length() <= 0) {
            return false;
        }
        for (int i = 0; i < letter.length(); i++) {
            char c = letter.charAt(i);
            if (!(c >= 'a' && c <= 'z') && !(c >= 'A' && c <= 'Z') && !(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 判断String是不是为纯数字
     */
    public static boolean IsNumber(String nNum) {
        if (nNum == null || nNum.length() <= 0) {
            return false;
        }
        for (int i = 0; i < nNum.length(); i++) {
            char c = nNum.charAt(i);
            if (!(c >= '0' && c <= '9')) {
                return false;
            }
        }
        return true;
    }

    /**
     * 获取屏幕宽度
     *
     * @param context context
     * @return int
     */
    public static int getScreenWidth(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.widthPixels;
    }

    /**
     * 获取屏幕高度
     *
     * @param context context
     * @return int
     */
    public static int getScreenHeight(Context context) {
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        return dm.heightPixels;
    }

    /**
     * 针对全面屏的屏幕高度
     *
     * @param activity activity
     * @return int
     */
    public static int getScreenRealHeight(Activity activity) {
        DisplayMetrics dm = new DisplayMetrics();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            activity.getWindowManager().getDefaultDisplay().getRealMetrics(dm);
        } else {
            activity.getWindowManager().getDefaultDisplay().getMetrics(dm);
        }
        return dm.heightPixels;
    }

    /**
     * 获取状态栏的高度
     *
     * @param context context
     * @return int
     */
    public static int getStatusBarHeight(Context context) {
        int result = 0;
        int resourceId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = context.getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
    /**
     * 获取文字大小混排的文字
     *
     * @param str    str
     * @param indexs 一段文字中可能间隔某几个会大字体，第二维是开始index和字符串长度
     * @return SpannableString
     */
    public static SpannableString getMixedText(String str, int[][] indexs, boolean isBig) {
        if (TextUtils.isEmpty(str) || indexs == null || indexs.length <= 0) {
            return new SpannableString("");
        }
        SpannableString spannableString = new SpannableString(str);
        int fontSizePx1 = SysUtils.convertSpToPixel(TestConstant.application, isBig ? 20 : 16);
        for (int[] index : indexs) {
            if (index.length < 2) {
                return new SpannableString("");
            }
            if (index[0] >= 0 && (index[0] + index[1]) < spannableString.length()) {
                spannableString.setSpan(new VerticalCenterSpan(fontSizePx1, Color.parseColor("#F72428")), index[0], index[0] + index[1], Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
        return spannableString;
    }
    /**
     * 使TextView中不同大小字体垂直居中
     */
    public static class VerticalCenterSpan extends ReplacementSpan {

        private float mFontSizePx;

        private int mTextColor;

        private VerticalCenterSpan(float fontSizePx, int textColor) {
            this.mFontSizePx = fontSizePx;
            this.mTextColor = textColor;
        }

        @Override
        public int getSize(@NonNull Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            text = text.subSequence(start, end);
            Paint p = getCustomTextPaint(paint);
            return (int) p.measureText(text.toString());
        }

        @Override
        public void draw(@NonNull Canvas canvas, CharSequence text, int start, int end, float x, int top, int y, int bottom, Paint paint) {
            text = text.subSequence(start, end);
            Paint p = getCustomTextPaint(paint);
            Paint.FontMetricsInt fm = p.getFontMetricsInt();
            p.setColor(mTextColor);
            // 此处重新计算y坐标，使字体居中
            canvas.drawText(text.toString(), x, y - ((y + fm.descent + y + fm.ascent) / 2 - (bottom + top) / 2), p);
        }

        private TextPaint getCustomTextPaint(Paint srcPaint) {
            TextPaint paint = new TextPaint(srcPaint);
            //设定字体大小, sp转换为px
            paint.setTextSize(mFontSizePx);
            return paint;
        }
    }

}
