package org.altmail.dicttextviewlistener;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
//import android.support.annotation.NonNull;

public class RoundedCornerDrawable extends Drawable {

    private final Paint mPaint;
    private final RectF mRectF;
    private final float mCornerRadius;

    RoundedCornerDrawable(float cornerRadius, int color) {

        mPaint = new Paint();
        mRectF = new RectF();
        mCornerRadius = cornerRadius;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);
    }

    @Override
    public void draw(@NonNull Canvas canvas) {

        mRectF.set(0, 0, getBounds().width(), getBounds().height());

        canvas.drawRoundRect(mRectF, mCornerRadius, mCornerRadius, mPaint);
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
