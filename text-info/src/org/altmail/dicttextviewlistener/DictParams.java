package org.altmail.dicttextviewlistener;

class DictParams {

    int mBodyTextColor, mTitleTextColor, mPrimaryColor, mAccentColor, mCountdown, mLongPressCountdown, mBackgroundColor;

    float mStrokeWidth, mCornerRadius;

    boolean mEnableTwoDimensionsScroll;

    DictParams(int accentColor, int bodyTextColor, int primaryColor, int titleTextColor, int countdown, int backgroundColor,
               int longPressCountdown, float strokeWidth, float cornerRadius, boolean enableTwoDimensionsScroll) {

        mAccentColor = accentColor;
        mBodyTextColor = bodyTextColor;
        mCountdown = countdown;
        mPrimaryColor = primaryColor;
        mTitleTextColor = titleTextColor;
        mLongPressCountdown = longPressCountdown;
        mCornerRadius = cornerRadius;
        mStrokeWidth = strokeWidth;
        mBackgroundColor = backgroundColor;
        mEnableTwoDimensionsScroll = enableTwoDimensionsScroll;
    }
}
