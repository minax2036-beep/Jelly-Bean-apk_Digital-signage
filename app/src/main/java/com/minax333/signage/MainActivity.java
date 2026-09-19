package com.minax333.signage;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends Activity {

    private TextView clockTextView;

    private final SimpleDateFormat timeFormat =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();

            clockTextView.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        clockTextView = new TextView(this);

        clockTextView.setTextColor(Color.WHITE);
        clockTextView.setTextSize(48);
        clockTextView.setGravity(Gravity.CENTER);
        clockTextView.setBackgroundColor(Color.BLACK);

        clockTextView.setLayoutParams(
                new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                )
        );

        setContentView(clockTextView);

        updateClock();
        clockTextView.post(clockRunnable);
    }

    private void updateClock() {
        String currentTime = timeFormat.format(new Date());
        clockTextView.setText(currentTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (clockTextView != null) {
            clockTextView.removeCallbacks(clockRunnable);
        }
    }
}