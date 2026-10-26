package com.minax333.signage;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.view.View;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * Decorative full-screen modes implemented with Canvas for Android API 17.
 */
public final class CreativeDisplayView extends View {
    public static final int MODE_VISUALIZER = 1;
    public static final int MODE_CLOCK = 2;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, yyyy年 M月 d日", Locale.JAPAN);
    private int mode = MODE_VISUALIZER;
    private long startedAt = System.currentTimeMillis();

    public CreativeDisplayView(Context context) {
        super(context);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        textPaint.setColor(Color.WHITE);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        setFocusable(true);
    }

    public void setMode(int newMode) {
        mode = newMode;
        startedAt = System.currentTimeMillis();
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mode == MODE_CLOCK) {
            drawClock(canvas);
        } else {
            drawVisualizer(canvas);
        }
        if (getVisibility() == View.VISIBLE) {
            postInvalidateDelayed(mode == MODE_CLOCK ? 1000L : 40L);
        }
    }

    private void drawVisualizer(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        float time = (System.currentTimeMillis() - startedAt) / 1000f;

        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[] { Color.rgb(4, 10, 31), Color.rgb(30, 20, 87), Color.rgb(0, 91, 126) },
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        float centerX = width * 0.5f;
        float centerY = height * 0.52f;
        float base = Math.min(width, height) * 0.11f;

        for (int i = 0; i < 9; i++) {
            float phase = time * (0.22f + i * 0.018f) + i * 0.72f;
            float radius = base * (1.2f + i * 0.32f) + (float) Math.sin(phase) * base * 0.13f;
            int alpha = 25 + i * 7;
            paint.setColor(Color.argb(alpha, 75 + i * 15, 100 + i * 9, 255));
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1.5f + (i % 3));
            canvas.drawCircle(centerX, centerY, radius, paint);
        }

        for (int i = 0; i < 38; i++) {
            float seed = i * 2.3999632f;
            float drift = time * (12f + (i % 5) * 5f);
            float orbit = base * (1.5f + (i % 9) * 0.55f);
            float x = centerX + (float) Math.cos(seed + time * (0.12f + (i % 4) * 0.025f)) * orbit;
            float y = centerY + (float) Math.sin(seed + time * (0.17f + (i % 3) * 0.03f)) * orbit
                    + (float) Math.sin(time + i) * 10f;
            y = (y + drift) % (height + 30f);
            if (y < 0) y += height + 30f;
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.argb(110 + (i % 5) * 25, 105, 190 + (i % 3) * 20, 255));
            canvas.drawCircle(x, y, 1.8f + (i % 4) * 0.8f, paint);
        }

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(1.2f);
        paint.setColor(Color.argb(70, 120, 210, 255));
        for (int i = 0; i < 7; i++) {
            float y = height * (0.28f + i * 0.075f);
            float wave = (float) Math.sin(time * 0.8f + i * 0.7f) * width * 0.08f;
            canvas.drawLine(width * 0.08f, y + wave, width * 0.92f, y - wave, paint);
        }

        paint.setStyle(Paint.Style.FILL);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(Math.max(18f, width * 0.022f));
        textPaint.setColor(Color.argb(210, 225, 239, 255));
        canvas.drawText("JELLY-BEAN  /  AMBIENT VISUALIZER", centerX, height * 0.12f, textPaint);
        textPaint.setTextSize(Math.max(12f, width * 0.012f));
        textPaint.setColor(Color.argb(145, 205, 221, 255));
        canvas.drawText("LIGHT  •  PARTICLES  •  MOTION", centerX, height * 0.16f, textPaint);
    }

    private void drawClock(Canvas canvas) {
        float width = getWidth();
        float height = getHeight();
        paint.setShader(new LinearGradient(0, 0, width, height,
                new int[] { Color.rgb(3, 12, 28), Color.rgb(7, 35, 69), Color.rgb(12, 73, 93) },
                null, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, width, height, paint);
        paint.setShader(null);

        float cx = width * 0.5f;
        float cy = height * 0.48f;
        float radius = Math.min(width, height) * 0.28f;

        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(Math.max(2f, radius * 0.012f));
        paint.setColor(Color.argb(55, 130, 220, 255));
        canvas.drawCircle(cx, cy, radius, paint);
        paint.setStrokeWidth(Math.max(1f, radius * 0.004f));
        paint.setColor(Color.argb(100, 130, 220, 255));
        canvas.drawCircle(cx, cy, radius * 0.86f, paint);

        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.argb(175, 30, 165, 220));
        canvas.drawCircle(cx, cy, radius * 0.035f, paint);

        Calendar now = Calendar.getInstance();
        String time = timeFormat.format(now.getTime());
        String date = dateFormat.format(now.getTime());

        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(Math.min(width * 0.19f, height * 0.29f));
        textPaint.setShadowLayer(14f, 0, 0, Color.rgb(20, 165, 255));
        canvas.drawText(time, cx, cy + textPaint.getTextSize() * 0.28f, textPaint);
        textPaint.clearShadowLayer();

        textPaint.setTextSize(Math.max(18f, width * 0.026f));
        textPaint.setColor(Color.rgb(163, 221, 255));
        canvas.drawText(date, cx, cy + radius * 0.53f, textPaint);

        textPaint.setTextSize(Math.max(12f, width * 0.013f));
        textPaint.setColor(Color.argb(170, 180, 215, 239));
        canvas.drawText("J E L L Y  -  B E A N", cx, height * 0.88f, textPaint);
        paint.setStyle(Paint.Style.FILL);
    }
}
