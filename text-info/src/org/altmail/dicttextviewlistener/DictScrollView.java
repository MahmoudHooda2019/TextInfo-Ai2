package org.altmail.dicttextviewlistener;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.ScrollView;

/**
 * Created by rom on 05/02/18.
 */

public class DictScrollView extends ScrollView {


    private boolean mScrollable = true;
    private Handler handler;

    public DictScrollView(Context context) {
        super(context);
    }

    public DictScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DictScrollView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setScrollingEnabled(boolean enabled) {

        mScrollable = enabled;
    }

    public boolean isScrollable() {
        return mScrollable;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {

        switch (ev.getAction()) {

            case MotionEvent.ACTION_DOWN:

                return mScrollable && super.onTouchEvent(ev);

            default:

                return super.onTouchEvent(ev);
        }
    }

    @Override
    protected void onOverScrolled(int scrollX, int scrollY, boolean clampedX, boolean clampedY) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY);

        if (handler != null) {

            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {

        return mScrollable && super.onInterceptTouchEvent(ev);
    }

    public void setHandler(final Handler handler) {

        if (handler != null) {

            this.handler = handler;
        }
    }
}
