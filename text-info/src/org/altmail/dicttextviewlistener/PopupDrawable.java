package org.altmail.dicttextviewlistener;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
//import android.support.annotation.NonNull;


public class PopupDrawable extends Drawable {

    private static final float HALF_DIVIDER = 2f;

    private final DictParams mDictParams;
    private final Paint mPaint;
    private final RectF mRectF;
    private final float mInnerCornerRadius;
    private final boolean mIsBackgroundLayer;

    PopupDrawable(final DictParams dictParams, boolean isBackgroundLayer) {

        mDictParams = dictParams;
        mInnerCornerRadius = dictParams.mCornerRadius - (mDictParams.mStrokeWidth / HALF_DIVIDER);
        mIsBackgroundLayer = isBackgroundLayer;
        mPaint = new Paint();
        mRectF = new RectF();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {

        final int height = getBounds().height();
        final int width = getBounds().width();

        final DictParams dictParams = mDictParams;
        final Paint paint = mPaint;
        final RectF rect = mRectF;

        final float innerCornerRadius = mInnerCornerRadius;

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(mIsBackgroundLayer? dictParams.mBackgroundColor : dictParams.mPrimaryColor);

        rect.set(dictParams.mStrokeWidth, dictParams.mStrokeWidth, width - dictParams.mStrokeWidth, height - dictParams.mStrokeWidth);
        canvas.drawRoundRect(rect, innerCornerRadius, innerCornerRadius, paint);

        if (mIsBackgroundLayer) {

            paint.setStyle(Paint.Style.STROKE);

            final float halfStroke = dictParams.mStrokeWidth / HALF_DIVIDER;
            paint.setStrokeWidth(dictParams.mStrokeWidth);
            paint.setColor(mDictParams.mPrimaryColor);

            rect.set(halfStroke, halfStroke, width - halfStroke, height - halfStroke);
            canvas.drawRoundRect(rect, dictParams.mCornerRadius, dictParams.mCornerRadius, paint);
        }
    }

    @Override
    public void setAlpha(int alpha) {

        mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(ColorFilter cf) {

        mPaint.setColorFilter(cf);
    }

    @Override
    public int getOpacity() {

        return PixelFormat.TRANSLUCENT;
    }
}
