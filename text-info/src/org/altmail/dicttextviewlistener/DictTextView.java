package org.altmail.dicttextviewlistener;

import android.content.Context;
//import android.content.res.TypedArray;
//import android.support.annotation.Nullable;
//import android.support.v7.widget.AppCompatTextView;
import android.graphics.Color;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;

public class DictTextView extends AppCompatTextView {

    private final DictTouchListener mTouchListener;
    private final DictParams mDictParams;

    public DictTextView(Context context) {
        this(context, null);
    }

    public DictTextView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DictTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        final int accentColor = Color.parseColor("#FF4081");
        final int bodyTextColor = Color.parseColor("#b3ffffff");
        final int countdown = 2000;
        final int longPressCountdown = 400;
        final int primaryColor = Color.parseColor("#3F51B5");
        final int titleTextColor = Color.parseColor("#AA000000");
        final int backgroundColor = Color.WHITE;
        final float cornerRadius = dpToPixels(context, 16);
        final float strokeWidth = dpToPixels(context, 1);
        final boolean enableTwoDimensionsScroll = true;


        mDictParams = new DictParams(accentColor, bodyTextColor, primaryColor,
                titleTextColor, countdown, backgroundColor, longPressCountdown, strokeWidth, cornerRadius, enableTwoDimensionsScroll);

        mTouchListener = new DictTouchListener(this, mDictParams);

        setOnTouchListener(mTouchListener);
    }

    public static float dpToPixels(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    public boolean dismissPopup() {

        return mTouchListener.dismissPopup();
    }


    public void setPopupBodyTextColor(int bodyTextColor) {

        mDictParams.mBodyTextColor = bodyTextColor;
    }

    public void setPopupTitleTextColor(int titleTextColor) {

        mDictParams.mTitleTextColor = titleTextColor;
    }

    public void setPopupPrimaryColor(int primaryColor) {

        mDictParams.mPrimaryColor = primaryColor;
    }

    public void setPopupAccentColor(int accentColor) {

        mDictParams.mAccentColor = accentColor;
    }

    public void setLookupCountdown(int countdown) {

        mDictParams.mCountdown = countdown;
    }

    public void setLongPressCountdown(int longPressCountdown) {

        mDictParams.mLongPressCountdown = longPressCountdown;
    }

    public void setPopupBackgroundColor(int backgroundColor) {

        mDictParams.mBackgroundColor = backgroundColor;
    }

    public void setPopupStrokeWidth(float strokeWidth) {

        mDictParams.mStrokeWidth = strokeWidth;
    }

    public void setPopupCornerRadius(float cornerRadius) {

        mDictParams.mCornerRadius = cornerRadius;
    }
}
