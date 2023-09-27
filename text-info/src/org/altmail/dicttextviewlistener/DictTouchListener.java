package org.altmail.dicttextviewlistener;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
//import android.support.constraint.ConstraintLayout;
//import android.support.constraint.ConstraintSet;
//import android.support.v4.content.ContextCompat;
//import android.support.v4.view.ViewCompat;
import android.text.Layout;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.*;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.ListIterator;

import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_MOVE;
import static android.view.MotionEvent.ACTION_UP;

/**
 * Created by rom on 02/02/18.
 */

public class DictTouchListener implements View.OnTouchListener {

    static final String MESSAGE_TAG = "message";
    static final String RESULT_TAG = "result";

    private static final String LAST_WORD_DEFAULT_VALUE = "";
    private static final String DEFAULT_SERVER_ADDRESS = "dict.org";

    private static final float POPUP_PERCENT_WIDTH_SCREEN_SIZE = 0.90f;
    private static final float POPUP_PERCENT_HEIGHT_SCREEN_SIZE = 0.70f;

    private static final float MAX_FLOAT_ANIMATOR_VALUE = 1f;
    private static final float MIN_FLOAT_ANIMATOR_VALUE = 0f;

    private static final int MIN_PROGRESS_DRAWABLE_LEVEL = 0;
    private static final int MAX_PROGRESS_DRAWABLE_LEVEL = 10000;

    private static final int DISPLAY_RESULT_FADE_IN_ANIMATION_TIME = 600;

    private static final int POPUP_EXPANSION_ANIMATION_TIME = 300;
    private static final int DEFAULT_SOCKET_PORT = 2628;

    private static final int CURRENT_POPUP_LIST_INDEX = 0;

    private static final int SCROLL_THRESHOLD = 15;

    private static final int SHOW_PROGRESS_EVENT_MESSAGE_ID = 0;
    private static final int DISPLAY_RESULT_EVENT_MESSAGE_ID = 1;
    private static final int DISPLAY_ERROR_EVENT_MESSAGE_ID = 2;

    private PointF mPointerPosition;
    private ImageView mProgressBackground;
    private CharSequence mLastWord;
    private View mBackgroundFilter;
    private ConstraintLayout mRoot;
    private FrameLayout mPopupContent;
    private ProgressBar mProgressBar;
    private DefineTask mDefineTask;

    private boolean mTriggered, mPopupVisible;
    private int[] mWordOffsetInterval;

    private final TextView mTextView;
    private final ValueAnimator mValueAnimator;
    private ViewGroup mContainer;
    private final ArrayList<View> mPopupList;
    private final Context mContext;

    private final int mPadding, mGradientHeight;
    private final boolean mIsPortraitMode;
    private final DictParams mDictParams;

    private Boolean mHasLockableScrollView;

    public static float dpToPixels(Context context, float dp) {
        float density = context.getResources().getDisplayMetrics().density;
        return dp * density;
    }

    private final Handler mHandler = new Handler(new Handler.Callback() {

        @Override
        public boolean handleMessage(Message message) {

            switch (message.what) {

                case SHOW_PROGRESS_EVENT_MESSAGE_ID:

                    final Vibrator v = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

                    if (v != null) {

                        v.vibrate(100);
                    }

                    if (mHasLockableScrollView) {

                        ((DictScrollView)mTextView.getParent()).setScrollingEnabled(false);
                    }

                    showProgress(mPointerPosition.x, mPointerPosition.y);

                    break;

                case DISPLAY_RESULT_EVENT_MESSAGE_ID:

                    final ParcelableLinkedList result = message.getData().getParcelable(RESULT_TAG);

                    if (result != null && mPopupVisible && mTriggered) {

                        mProgressBar.setVisibility(View.GONE);
                        displayDefinitions(new DictParser(result.getLinkedList()));
                    }

                    break;

                case DISPLAY_ERROR_EVENT_MESSAGE_ID:

                    final String msg = message.getData().getString(MESSAGE_TAG);

                    if (mPopupVisible && mTriggered) {

                        mProgressBar.setVisibility(View.GONE);
                        displayError(msg);
                    }

                    break;
            }

            return false;
        }
    });

    DictTouchListener(final TextView textView, final DictParams dictParams) {

        mTextView = textView;
        mDictParams = dictParams;
        mLastWord = LAST_WORD_DEFAULT_VALUE;
        mPopupList = new ArrayList<>();
        mContext = textView.getContext();

        mPadding = (int) dpToPixels(mContext, 16);
                //mContext.getResources().getDimensionPixelSize(org.altmail.dicttextviewlistener.R.dimen.scroll_view_padding);
        mIsPortraitMode = mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
        mGradientHeight = (int) dpToPixels(mContext, 16);
                //mContext.getResources().getDimensionPixelSize(org.altmail.dicttextviewlistener.R.dimen.gradient_border_height);
        mValueAnimator = ValueAnimator.ofInt(MIN_PROGRESS_DRAWABLE_LEVEL, MAX_PROGRESS_DRAWABLE_LEVEL);

        mValueAnimator.setDuration(mDictParams.mCountdown);
        mValueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                if (mProgressBackground != null) {

                    mProgressBackground.setImageLevel((Integer) valueAnimator.getAnimatedValue());
                }
            }
        });

        mValueAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {

                if (mPopupVisible) {

                    mPointerPosition = null;

                    expandPopup();
                }
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });
    }

    private void expandPopup() {

        mTriggered = true;

        final int outHeight = (int) (mContainer.getHeight() * (mIsPortraitMode?POPUP_PERCENT_HEIGHT_SCREEN_SIZE:POPUP_PERCENT_WIDTH_SCREEN_SIZE));
        final int outWidth = (int) (mContainer.getWidth() * (mIsPortraitMode?POPUP_PERCENT_WIDTH_SCREEN_SIZE:POPUP_PERCENT_HEIGHT_SCREEN_SIZE));
        final int outX = (mContainer.getWidth() - outWidth) / 2;
        final int outY = (mContainer.getHeight() - outHeight) / 2;
        final FrameLayout.LayoutParams l = (FrameLayout.LayoutParams)mPopupList.get(CURRENT_POPUP_LIST_INDEX).getLayoutParams();
        final int currentWidth = mRoot.getWidth();
        final int currentHeight = mRoot.getHeight();
        final int currentX = l.leftMargin;
        final int currentY = l.topMargin;

        ViewCompat.setBackground(mRoot, new RoundedCornerDrawable(mDictParams.mCornerRadius, mDictParams.mPrimaryColor));

        mProgressBackground.setVisibility(View.GONE);

        final ValueAnimator valueAnimator = ValueAnimator.ofFloat(MIN_FLOAT_ANIMATOR_VALUE, MAX_FLOAT_ANIMATOR_VALUE);

        valueAnimator.setDuration(POPUP_EXPANSION_ANIMATION_TIME);
        valueAnimator.setInterpolator(new AccelerateDecelerateInterpolator());

        mBackgroundFilter = new View(mContext);

        mBackgroundFilter.setLayoutParams(new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mBackgroundFilter.setBackgroundColor(ContextCompat.getColor(mContext, Color.parseColor("#AA000000")));
        mBackgroundFilter.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                removeExpandedPopup();
            }
        });

        mContainer.addView(mBackgroundFilter);
        mPopupList.get(CURRENT_POPUP_LIST_INDEX).bringToFront();

        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                final int width = (int) (currentWidth + ((float)valueAnimator.getAnimatedValue() * (outWidth - currentWidth)));
                final int height = (int) (currentHeight + ((float)valueAnimator.getAnimatedValue() * (outHeight - currentHeight)));
                final int X = (int) (currentX + ((float)valueAnimator.getAnimatedValue() * (outX - currentX)));
                final int Y = (int) (currentY + ((float)valueAnimator.getAnimatedValue() * (outY - currentY)));
                final FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)mPopupList.get(0).getLayoutParams();

                layoutParams.width = width;
                layoutParams.height = height;

                layoutParams.setMargins(X, Y, 0, 0);
                mPopupList.get(CURRENT_POPUP_LIST_INDEX).setLayoutParams(layoutParams);
                mBackgroundFilter.setAlpha((float)valueAnimator.getAnimatedValue());
            }
        });

        valueAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {
                addPopupContent();
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}
        });

        valueAnimator.start();
    }

    private void addPopupContent() {

        mRoot.setPadding(0, 0, 0, mPadding);

        if (mDictParams.mEnableTwoDimensionsScroll) {

            mPopupContent = new TwoDScrollView(mContext);

        } else {

            mPopupContent = new ScrollView(mContext);
        }

        mPopupContent.setOverScrollMode(View.OVER_SCROLL_NEVER);

        mProgressBar = new ProgressBar(mContext);

        mProgressBar.getIndeterminateDrawable().setColorFilter(mDictParams.mAccentColor, PorterDuff.Mode.MULTIPLY);

        final View topGradient = new View(mContext);
        final View bottomGradient = new View(mContext);

        final FrameLayout fl = new FrameLayout(mContext);

        mPopupContent.setId(View.generateViewId());
        mPopupContent.setTag("popup_content");
        mPopupContent.setPadding(mPadding, mPadding, mPadding, mPadding);
        mPopupContent.setClipToPadding(false);
        mPopupContent.setScrollBarStyle(View.SCROLLBARS_OUTSIDE_OVERLAY);
        mProgressBar.setId(View.generateViewId());
        mProgressBar.setTag("progress_bar");

        final int[] gradient = new int[] {mDictParams.mPrimaryColor, Color.TRANSPARENT};

        final GradientDrawable topGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, gradient);
        final GradientDrawable bottomGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP, gradient);

        ViewCompat.setBackground(topGradient, topGradientDrawable);
        ViewCompat.setBackground(bottomGradient, bottomGradientDrawable);

        topGradient.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mGradientHeight, Gravity.TOP));
        bottomGradient.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, mGradientHeight, Gravity.BOTTOM));

        fl.setId(View.generateViewId());
        fl.setTag("frame_layout");
        fl.addView(topGradient);
        fl.addView(bottomGradient);

        if (mDictParams.mEnableTwoDimensionsScroll) {

            final View leftGradient = new View(mContext);
            final View rightGradient = new View(mContext);

            final GradientDrawable leftGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, gradient);
            final GradientDrawable rightGradientDrawable = new GradientDrawable(GradientDrawable.Orientation.RIGHT_LEFT, gradient);

            ViewCompat.setBackground(leftGradient, leftGradientDrawable);
            ViewCompat.setBackground(rightGradient, rightGradientDrawable);

            leftGradient.setLayoutParams(new FrameLayout.LayoutParams(mGradientHeight, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.START));
            rightGradient.setLayoutParams(new FrameLayout.LayoutParams(mGradientHeight, FrameLayout.LayoutParams.MATCH_PARENT, Gravity.END));

            fl.addView(leftGradient);
            fl.addView(rightGradient);
        }

        mRoot.addView(mPopupContent);
        mRoot.addView(mProgressBar);
        mRoot.addView(fl);

        final ConstraintSet constraintSet = new ConstraintSet();
        PopupWindowLayout popupWindowLayout = new PopupWindowLayout(mContext);

        constraintSet.constrainHeight(mPopupContent.getId(), ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainWidth(mPopupContent.getId(), ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(mProgressBar.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainWidth(mProgressBar.getId(), ConstraintSet.WRAP_CONTENT);
        constraintSet.constrainHeight(fl.getId(), ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.constrainHeight(fl.getId(), ConstraintSet.MATCH_CONSTRAINT);
        constraintSet.connect(mPopupContent.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        constraintSet.connect(mPopupContent.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        constraintSet.connect(mPopupContent.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(mPopupContent.getId(), ConstraintSet.TOP, popupWindowLayout.findViewWithTag("word").getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.connect(fl.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        constraintSet.connect(fl.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        constraintSet.connect(fl.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.connect(fl.getId(), ConstraintSet.TOP, popupWindowLayout.findViewWithTag("word").getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.connect(mProgressBar.getId(), ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP, 0);
        constraintSet.connect(mProgressBar.getId(), ConstraintSet.LEFT, ConstraintSet.PARENT_ID, ConstraintSet.LEFT, 0);
        constraintSet.connect(mProgressBar.getId(), ConstraintSet.RIGHT, ConstraintSet.PARENT_ID, ConstraintSet.RIGHT, 0);
        constraintSet.connect(mProgressBar.getId(), ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo(mRoot);

        doLookup();
    }

    private void showProgress(float x, float y) {

        final CharSequence text = mTextView.getText();

        int offset = mTextView.getOffsetForPosition(x, y);

        if (offset<mTextView.length()) {

            if (!Character.isLetter(text.charAt(offset))) {

                if (mLastWord.equals(LAST_WORD_DEFAULT_VALUE)) {

                    offset = getClosestWord(offset, text);

                    if (offset == -1) {

                        return;
                    }

                } else {

                    return;
                }
            }

            int left = offset;
            int right = offset;
            boolean isLetter = true;

            while (isLetter) {

                if (left > 0 && Character.isLetter(text.charAt(left - 1))) {

                    left--;

                } else {

                    isLetter = false;
                }
            }

            isLetter = true;

            while (isLetter) {

                if (right < text.length() - 1 && Character.isLetter(text.charAt(right + 1))) {

                    right++;

                } else {

                    right++;
                    isLetter = false;
                }
            }

            if (right <= text.length() - 1) {

                final CharSequence word = text.subSequence(left, right);

                if (word.length() > 1) {

                    if (!word.equals(mLastWord)) {

                        if (mPopupVisible) {

                            removePopup();

                            mPointerPosition = new PointF(x, y);
                        }

                        mLastWord = word;
                        mWordOffsetInterval = new int[] {left, right};
                        final Rect rect = getCoordinates(left, right);

                        //final LayoutInflater layoutInflater = LayoutInflater.from(mContext);
                        //final View v = layoutInflater.inflate(org.altmail.dicttextviewlistener.R.layout.popup_window_layout, null);
                        //mProgressBackground = v.findViewById(org.altmail.dicttextviewlistener.R.id.img);
                        //mRoot = v.findViewById(org.altmail.dicttextviewlistener.R.id.root);
                        //final TextView textView = v.findViewById(org.altmail.dicttextviewlistener.R.id.word);

                        PopupWindowLayout v = new PopupWindowLayout(mContext);
                        mProgressBackground = v.findViewWithTag("img");
                        mRoot = v.findViewWithTag("root");
                        final TextView textView = v.findViewWithTag("word");

                        mProgressBackground.setImageDrawable(createDrawable());
                        textView.setText(word);
                        textView.setTextColor(mDictParams.mTitleTextColor);
                        v.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

                        int width = v.getMeasuredWidth();
                        int height = v.getMeasuredHeight();
                        int outLeft;

                        if (width >= rect.width()) {

                            outLeft = rect.left - ((width - rect.width()) / 2);

                        } else {

                            outLeft = rect.left + ((rect.width() - width) / 2);
                        }

                        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.WRAP_CONTENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT);

                        layoutParams.setMargins(outLeft, rect.top - (int) mTextView.getTextSize() - height, 0, 0);
                        v.setLayoutParams(layoutParams);

                        AnimationSet fadeIn = new AnimationSet(true);

                        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
                        alphaAnimation.setInterpolator(new AccelerateInterpolator());
                        alphaAnimation.setDuration(200);
                        alphaAnimation.setRepeatCount(0);

                        fadeIn.addAnimation(alphaAnimation);

                        //final Animation fadeIn = AnimationUtils.loadAnimation(mContext, fade_in);

                        if (mContainer == null) {

                            mContainer = mTextView.getRootView().findViewById(android.R.id.content);
                        }

                        mContainer.addView(v);
                        v.startAnimation(fadeIn);
                        mPopupList.add(CURRENT_POPUP_LIST_INDEX, v);
                        mPopupVisible = true;
                        mValueAnimator.start();
                    }
                }
            }
        }
    }

    private Drawable createDrawable() {

        final PopupDrawable backgroundDrawable = new PopupDrawable(mDictParams, true);

        final ClipDrawable clipDrawable = new ClipDrawable(new PopupDrawable(mDictParams, false), Gravity.START, ClipDrawable.HORIZONTAL);

        return new LayerDrawable(new Drawable[] {backgroundDrawable, clipDrawable});
    }

    // case with space or comma, point ...
    private static int getClosestWord(int offset, CharSequence text) {

        if (offset > 1 && Character.isLetter(text.charAt(offset-1)) && Character.isLetter(text.charAt(offset-2))) {

            return offset-1;

        } else if (offset<text.length()-2 && Character.isLetter(text.charAt(offset+1)) && Character.isLetter(text.charAt(offset+2))) {

            return offset+1;

        } else {

            return -1;
        }
    }

    private Rect getCoordinates(int left, int right) {

        final Rect textViewRect = new Rect();
        final Layout textViewLayout = mTextView.getLayout();
        final double startXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(left);
        final double endXCoordinatesOfClickedText = textViewLayout.getPrimaryHorizontal(right);
        final int currentLineStartOffset = textViewLayout.getLineForOffset(left);

        textViewLayout.getLineBounds(currentLineStartOffset, textViewRect);

        double parentTextViewTopAndBottomOffset = (mTextView.getY() - (mHasLockableScrollView?((DictScrollView)mTextView.getParent()).getScrollY():0) +
                mTextView.getCompoundPaddingTop());

        textViewRect.top += parentTextViewTopAndBottomOffset;
        textViewRect.bottom += parentTextViewTopAndBottomOffset;
        textViewRect.left += (mTextView.getX() + startXCoordinatesOfClickedText + mTextView.getCompoundPaddingLeft() -
                (mHasLockableScrollView?((DictScrollView)mTextView.getParent()).getScrollX():0));

        textViewRect.right = (int) (textViewRect.left + endXCoordinatesOfClickedText - startXCoordinatesOfClickedText);

        return textViewRect;
    }

    private void removeExpandedPopup() {

        mPopupVisible = false;
        mTriggered = false;

        if (mDefineTask != null && ! mDefineTask.isCancelled()) {

            mDefineTask.cancel(true);
        }

        if (mHasLockableScrollView) {

            ((DictScrollView)mTextView.getParent()).setScrollingEnabled(true);
        }

        mLastWord = LAST_WORD_DEFAULT_VALUE;
        ValueAnimator valueAnimator = ValueAnimator.ofFloat(MAX_FLOAT_ANIMATOR_VALUE, MIN_FLOAT_ANIMATOR_VALUE);

        valueAnimator.setDuration(POPUP_EXPANSION_ANIMATION_TIME);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {

                mPopupList.get(CURRENT_POPUP_LIST_INDEX).setAlpha((float)valueAnimator.getAnimatedValue());
                mBackgroundFilter.setAlpha((float)valueAnimator.getAnimatedValue());
            }
        });

        valueAnimator.addListener(new Animator.AnimatorListener() {

            @Override
            public void onAnimationStart(Animator animator) {}

            @Override
            public void onAnimationEnd(Animator animator) {

                mContainer.removeView(mBackgroundFilter);
                mContainer.removeView(mPopupList.get(CURRENT_POPUP_LIST_INDEX));
                mPopupList.remove(CURRENT_POPUP_LIST_INDEX);

                mPopupContent = null;
                mProgressBar = null;
                mBackgroundFilter = null;
            }

            @Override
            public void onAnimationCancel(Animator animator) {}

            @Override
            public void onAnimationRepeat(Animator animator) {}

        });

        valueAnimator.start();
    }

    private void doLookup() {

        final LinkedList<String> commands = new LinkedList<>();

        commands.add("DEFINE * " + mLastWord);
        commands.add("QUIT");

        mDefineTask = new DefineTask(mHandler, DEFAULT_SERVER_ADDRESS, DEFAULT_SOCKET_PORT, commands);

        mDefineTask.execute();
    }

    private void removePopup() {

        mPopupVisible = false;
        mValueAnimator.cancel();
        final View v = mPopupList.get(CURRENT_POPUP_LIST_INDEX);
        mLastWord = LAST_WORD_DEFAULT_VALUE;

        AnimationSet fadeOut = new AnimationSet(true);

        AlphaAnimation alphaAnimation = new AlphaAnimation(1.0f, 0.0f);
        alphaAnimation.setInterpolator(new AccelerateInterpolator());
        alphaAnimation.setDuration(200);
        alphaAnimation.setRepeatCount(0);

        fadeOut.addAnimation(alphaAnimation);

        //final Animation fadeOut = AnimationUtils.loadAnimation(mContext, fade_out);

        fadeOut.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {

                mContainer.post(new Runnable() {

                    @Override
                    public void run() {

                        mContainer.removeView(v);
                        mPopupList.remove(v);
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

        });

        v.startAnimation(fadeOut);
    }

    private void displayDefinitions(DictParser dictParser) {

        final LinearLayout linearLayout = new LinearLayout(mContext);
        final ScrollView.LayoutParams layoutParams = new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        linearLayout.setLayoutParams(layoutParams);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setVisibility(View.INVISIBLE);

        mPopupContent.addView(linearLayout);

        final LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        final LinkedList<String> definitions = dictParser.result();
        final ListIterator<String> i = definitions.listIterator();

        while (i.hasNext()) {

            final SpannableString dictionary = new SpannableString(i.next() + DictParser.NEWLINE);

            dictionary.setSpan(new StyleSpan(Typeface.BOLD), 0, dictionary.length(), 0);

            final TextView dictionaryView = new TextView(mContext);

            dictionaryView.setHorizontallyScrolling(true);
            dictionaryView.setTextColor(mDictParams.mBodyTextColor);
            dictionaryView.setLayoutParams(lp);
            dictionaryView.setText(dictionary);
            linearLayout.addView(dictionaryView);

            if (i.hasNext()) {

                final TextView definitionView = new TextView(mContext);
                definitionView.setTextColor(mDictParams.mBodyTextColor);
                definitionView.setLayoutParams(lp);
                definitionView.setText(i.next());
                linearLayout.addView(definitionView);
            }
        }

        AnimationSet fadeIn = new AnimationSet(true);

        AlphaAnimation alphaAnimation = new AlphaAnimation(0.0f, 1.0f);
        alphaAnimation.setInterpolator(new AccelerateInterpolator());
        alphaAnimation.setDuration(200);
        alphaAnimation.setRepeatCount(0);

        fadeIn.addAnimation(alphaAnimation);

        //final Animation fadeIn = AnimationUtils.loadAnimation(mContext, fade_in);

        fadeIn.setDuration(DISPLAY_RESULT_FADE_IN_ANIMATION_TIME);
        fadeIn.setAnimationListener(new Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {}

            @Override
            public void onAnimationEnd(Animation animation) {

                linearLayout.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {}

        });

        linearLayout.startAnimation(fadeIn);
    }

    private void displayError(String message) {

        final TextView textView = new TextView(mContext);
        final ScrollView.LayoutParams layoutParams = new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);

        textView.setLayoutParams(layoutParams);
        mPopupContent.addView(textView);
        textView.setText(message);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {

        switch (motionEvent.getAction()) {

            case ACTION_DOWN :

                if (mHasLockableScrollView == null) {

                    if (mTextView.getParent() instanceof DictScrollView) {

                        mHasLockableScrollView = Boolean.TRUE;

                        ((DictScrollView)mTextView.getParent()).setHandler(mHandler);

                    } else {

                        mHasLockableScrollView = Boolean.FALSE;
                    }
                }

                mPointerPosition = new PointF(motionEvent.getX(), motionEvent.getY());

                mHandler.sendEmptyMessageDelayed(SHOW_PROGRESS_EVENT_MESSAGE_ID, mDictParams.mLongPressCountdown);

                break;

            case  ACTION_MOVE:

                if (mPointerPosition != null) {

                    if (mPopupVisible) {

                        int offset = mTextView.getOffsetForPosition(motionEvent.getX(), motionEvent.getY());

                        if (offset < mWordOffsetInterval[0] || offset > mWordOffsetInterval[1]) {

                            showProgress(motionEvent.getX(), motionEvent.getY());
                        }

                    } else if (Math.abs(mPointerPosition.x - motionEvent.getX()) > SCROLL_THRESHOLD
                            || Math.abs(mPointerPosition.y - motionEvent.getY()) > SCROLL_THRESHOLD) {

                        mHandler.removeMessages(SHOW_PROGRESS_EVENT_MESSAGE_ID);
                        mPointerPosition = null;
                    }
                }

                break;

            case ACTION_UP :

                mHandler.removeMessages(SHOW_PROGRESS_EVENT_MESSAGE_ID);

                if (mPointerPosition != null) {

                    mPointerPosition = null;
                }

                if (!mTriggered && mPopupVisible) {

                    if (mHasLockableScrollView) {

                        ((DictScrollView)mTextView.getParent()).setScrollingEnabled(true);
                    }

                    removePopup();
                }

                break;
        }

        return true;
    }

    boolean dismissPopup() {

        if (mPopupVisible && mTriggered) {

            removeExpandedPopup();

            return true;

        } else {

            return false;
        }
    }
}