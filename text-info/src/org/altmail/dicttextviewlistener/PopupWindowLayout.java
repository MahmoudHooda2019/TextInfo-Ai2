package org.altmail.dicttextviewlistener;

import android.content.Context;
import android.graphics.Typeface;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

public class PopupWindowLayout extends ConstraintLayout {

    private ImageView img;
    private TextView word;

    public PopupWindowLayout(Context context) {
        super(context);
        setId(View.generateViewId());
        setTag("root");
        initializeViews(context);
    }

    private void initializeViews(Context context) {
        // Create and set the ImageView
        img = new ImageView(context);
        img.setId(View.generateViewId()); // Dynamically generate an ID
        img.setLayoutParams(new LayoutParams(0, 0)); // Use ConstraintLayout.LayoutParams
        img.setContentDescription(null);
        img.setTag("img"); // Set a unique tag

        // Create and set the TextView
        word = new TextView(context);
        word.setId(View.generateViewId()); // Dynamically generate an ID
        word.setPadding(16, 8, 16, 12);
        word.setTextSize(20);
        word.setTypeface(null, Typeface.BOLD);
        word.setTextColor(getResources().getColor(android.R.color.secondary_text_light));
        word.setTag("word"); // Set a unique tag

        // Add constraints for the TextView
        ConstraintLayout.LayoutParams wordLayoutParams = new ConstraintLayout.LayoutParams(
                ConstraintLayout.LayoutParams.WRAP_CONTENT,
                ConstraintLayout.LayoutParams.WRAP_CONTENT
        );
        wordLayoutParams.leftToLeft = img.getId(); // Reference the dynamically generated ID
        wordLayoutParams.rightToRight = img.getId(); // Reference the dynamically generated ID
        wordLayoutParams.topToTop = img.getId(); // Reference the dynamically generated ID
        wordLayoutParams.bottomToBottom = img.getId(); // Reference the dynamically generated ID
        word.setLayoutParams(wordLayoutParams);

        // Add views to the ConstraintLayout
        this.addView(img);
        this.addView(word);

        // Set constraints for the ImageView
        ConstraintSet constraintSet = new ConstraintSet();
        constraintSet.clone(this);
        constraintSet.connect(img.getId(), ConstraintSet.TOP, word.getId(), ConstraintSet.TOP, 0);
        constraintSet.connect(img.getId(), ConstraintSet.LEFT, word.getId(), ConstraintSet.LEFT, 0);
        constraintSet.connect(img.getId(), ConstraintSet.RIGHT, word.getId(), ConstraintSet.RIGHT, 0);
        constraintSet.connect(img.getId(), ConstraintSet.BOTTOM, word.getId(), ConstraintSet.BOTTOM, 0);
        constraintSet.applyTo(this);
    }
}
