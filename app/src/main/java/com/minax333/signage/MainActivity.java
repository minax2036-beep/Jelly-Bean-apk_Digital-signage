package com.minax333.signage;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView textView = new TextView(this);

        textView.setText("Signage\nAndroid 4.2.2");
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(32);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(Color.BLACK);

        textView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(textView);
    }
}
