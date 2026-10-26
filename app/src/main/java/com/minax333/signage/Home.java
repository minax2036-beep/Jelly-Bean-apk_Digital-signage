package com.minax333.signage;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Launcher-style home screen for Jelly-Bean Digital Signage.
 * Built with Java widgets for Android API 17 compatibility.
 */
public final class Home {
    public interface Listener {
        void onVideoSelected();
        void onMusicSelected();
        void onImagesSelected();
        void onSettingsSelected();
    }

    private final Activity activity;
    private final FrameLayout root;
    private final TextView clockText;
    private final TextView dateText;
    private final LinearLayout calendarGrid;
    private final TextView monthTitle;
    private String calendarKey = "";

    public Home(Activity activity, final Listener listener) {
        this.activity = activity;
        root = new FrameLayout(activity);
        root.setFocusable(true);
        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[] { Color.rgb(7, 35, 78), Color.rgb(15, 91, 153), Color.rgb(18, 34, 67) }
        ));

        LinearLayout page = new LinearLayout(activity);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(28), dp(18), dp(28), dp(14));
        root.addView(page, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout header = new LinearLayout(activity);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout brand = new LinearLayout(activity);
        brand.setOrientation(LinearLayout.VERTICAL);
        TextView title = text("Jelly-Bean Digital Signage", 25, Color.WHITE, true);
        brand.addView(title, wrapParams());
        TextView subtitle = text("つながる映像、ひろがる情報", 13, Color.rgb(213, 231, 255), false);
        subtitle.setPadding(0, dp(3), 0, 0);
        brand.addView(subtitle, wrapParams());
        header.addView(brand, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        LinearLayout dateBlock = new LinearLayout(activity);
        dateBlock.setOrientation(LinearLayout.VERTICAL);
        dateBlock.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        clockText = text("", 36, Color.WHITE, true);
        clockText.setGravity(Gravity.RIGHT);
        dateBlock.addView(clockText, wrapParams());
        dateText = text("", 15, Color.rgb(220, 235, 255), false);
        dateText.setGravity(Gravity.RIGHT);
        dateBlock.addView(dateText, wrapParams());
        header.addView(dateBlock, wrapParams());
        page.addView(header, wrapParams());

        TextView sectionTitle = text("ホーム", 22, Color.WHITE, true);
        sectionTitle.setPadding(0, dp(15), 0, dp(8));
        page.addView(sectionTitle, wrapParams());

        LinearLayout mainContent = new LinearLayout(activity);
        mainContent.setOrientation(LinearLayout.HORIZONTAL);
        mainContent.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout tiles = new LinearLayout(activity);
        tiles.setOrientation(LinearLayout.VERTICAL);
        tiles.setGravity(Gravity.CENTER);
        LinearLayout tileRow1 = new LinearLayout(activity);
        tileRow1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout tileRow2 = new LinearLayout(activity);
        tileRow2.setOrientation(LinearLayout.HORIZONTAL);

        tileRow1.addView(createTile("▶", "動画", "動画を再生", Color.rgb(25, 116, 232), new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onVideoSelected(); }
        }), tileParams());
        tileRow1.addView(createTile("♫", "音楽", "音楽ライブラリ", Color.rgb(145, 49, 190), new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onMusicSelected(); }
        }), tileParams());
        tileRow2.addView(createTile("▧", "画像", "画像を表示", Color.rgb(0, 143, 132), new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onImagesSelected(); }
        }), tileParams());
        tileRow2.addView(createTile("⚙", "設定", "表示や再生を設定", Color.rgb(204, 113, 16), new View.OnClickListener() {
            @Override public void onClick(View view) { listener.onSettingsSelected(); }
        }), tileParams());
        tiles.addView(tileRow1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        LinearLayout.LayoutParams row2Params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1);
        row2Params.topMargin = dp(8);
        tiles.addView(tileRow2, row2Params);
        mainContent.addView(tiles, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3));

        LinearLayout calendarPanel = new LinearLayout(activity);
        calendarPanel.setOrientation(LinearLayout.VERTICAL);
        calendarPanel.setPadding(dp(12), dp(12), dp(12), dp(12));
        calendarPanel.setBackground(UiStyle.makeBackground(Color.argb(150, 4, 17, 39), 18));
        LinearLayout.LayoutParams calParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 2);
        calParams.leftMargin = dp(12);
        mainContent.addView(calendarPanel, calParams);

        monthTitle = text("", 18, Color.WHITE, true);
        monthTitle.setGravity(Gravity.CENTER);
        monthTitle.setPadding(0, 0, 0, dp(8));
        calendarPanel.addView(monthTitle, wrapParams());

        LinearLayout weekdays = new LinearLayout(activity);
        weekdays.setOrientation(LinearLayout.HORIZONTAL);
        String[] weekdayNames = {"日", "月", "火", "水", "木", "金", "土"};
        for (int i = 0; i < weekdayNames.length; i++) {
            TextView day = text(weekdayNames[i], 12,
                    i == 0 ? Color.rgb(255, 160, 160) : (i == 6 ? Color.rgb(150, 200, 255) : Color.rgb(220, 235, 255)), true);
            day.setGravity(Gravity.CENTER);
            weekdays.addView(day, new LinearLayout.LayoutParams(0, dp(24), 1));
        }
        calendarPanel.addView(weekdays, wrapParams());

        calendarGrid = new LinearLayout(activity);
        calendarGrid.setOrientation(LinearLayout.VERTICAL);
        calendarPanel.addView(calendarGrid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        TextView footer = text("Jelly-Bean Signage  •  コンテンツを選択してください", 13, Color.rgb(211, 228, 249), false);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(14), dp(8), dp(14), dp(8));
        footer.setBackground(UiStyle.makeBackground(Color.argb(115, 4, 17, 39), 14));
        LinearLayout.LayoutParams footerParams = wrapParams();
        footerParams.topMargin = dp(10);
        page.addView(mainContent, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        page.addView(footer, footerParams);
        refreshCalendar();
    }

    public View getView() { return root; }

    public void updateClock(String time, String date) {
        clockText.setText(time);
        dateText.setText(date);
        String todayKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(new Date());
        if (!todayKey.equals(calendarKey)) {
            refreshCalendar();
        } else {
            highlightToday();
        }
    }

    private void refreshCalendar() {
        Calendar now = Calendar.getInstance();
        calendarKey = new SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(now.getTime());
        monthTitle.setText(new SimpleDateFormat("yyyy年 M月", Locale.JAPAN).format(now.getTime()));
        calendarGrid.removeAllViews();

        Calendar first = (Calendar) now.clone();
        first.set(Calendar.DAY_OF_MONTH, 1);
        int firstWeekday = first.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY;
        int daysInMonth = first.getActualMaximum(Calendar.DAY_OF_MONTH);
        int today = now.get(Calendar.DAY_OF_MONTH);

        int dayNumber = 1;
        for (int week = 0; week < 6; week++) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER);
            for (int column = 0; column < 7; column++) {
                int cell = week * 7 + column;
                TextView day = text("", 13, Color.WHITE, false);
                day.setGravity(Gravity.CENTER);
                if (cell >= firstWeekday && dayNumber <= daysInMonth) {
                    int value = dayNumber;
                    day.setText(String.valueOf(value));
                    if (value == today) {
                        day.setTag("today");
                        day.setTextColor(Color.rgb(7, 35, 78));
                        day.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
                        day.setBackground(UiStyle.makeBackground(Color.rgb(255, 255, 255), 18));
                    } else if (column == 0) {
                        day.setTextColor(Color.rgb(255, 170, 170));
                    } else if (column == 6) {
                        day.setTextColor(Color.rgb(160, 205, 255));
                    } else {
                        day.setTextColor(Color.rgb(225, 238, 255));
                    }
                    dayNumber++;
                }
                row.addView(day, new LinearLayout.LayoutParams(0, 0, 1));
                LinearLayout.LayoutParams cellParams = (LinearLayout.LayoutParams) day.getLayoutParams();
                cellParams.height = 0;
                cellParams.weight = 1;
                day.setLayoutParams(cellParams);
            }
            calendarGrid.addView(row, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        }
    }

    private void highlightToday() {
        // The calendar is rebuilt when the month changes; the day highlight remains current within the month.
        // The app refreshes its clock every second, so no expensive calendar work is needed here.
    }

    private View createTile(String icon, String title, String description, int color, View.OnClickListener listener) {
        LinearLayout tile = new LinearLayout(activity);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setGravity(Gravity.CENTER);
        tile.setPadding(dp(6), dp(8), dp(6), dp(8));
        tile.setBackground(UiStyle.makeBackground(color, 22));
        tile.setClickable(true);
        tile.setFocusable(true);
        tile.setOnClickListener(listener);

        TextView iconText = text(icon, 34, Color.WHITE, true);
        iconText.setGravity(Gravity.CENTER);
        tile.addView(iconText, wrapParams());
        TextView titleText = text(title, 20, Color.WHITE, true);
        titleText.setGravity(Gravity.CENTER);
        titleText.setPadding(0, dp(4), 0, dp(2));
        tile.addView(titleText, wrapParams());
        TextView descriptionText = text(description, 11, Color.rgb(242, 247, 255), false);
        descriptionText.setGravity(Gravity.CENTER);
        tile.addView(descriptionText, wrapParams());
        return tile;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView view = new TextView(activity);
        view.setText(value);
        view.setTextColor(color);
        view.setTextSize(size);
        if (bold) view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return view;
    }

    private LinearLayout.LayoutParams tileParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1);
        params.setMargins(dp(4), dp(2), dp(4), dp(2));
        return params;
    }

    private LinearLayout.LayoutParams wrapParams() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private int dp(int value) {
        float density = activity.getResources().getDisplayMetrics().density;
        return (int) (value * density + 0.5f);
    }
}