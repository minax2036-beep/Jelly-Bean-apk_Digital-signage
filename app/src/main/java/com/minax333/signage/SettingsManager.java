package com.minax333.signage;

import android.content.SharedPreferences;

/**
 * Owns persisted signage settings.
 * Compatible with the project's minimum Android API level 17.
 */
public final class SettingsManager {

    public static final String PREFS_NAME = "signage_settings";

    private static final String KEY_SLIDESHOW_ENABLED = "slideshow_enabled";
    private static final String KEY_SLIDESHOW_SECONDS = "slideshow_seconds";
    private static final String KEY_AUDIO_ENABLED = "audio_enabled";
    private static final String KEY_SELECTED_AUDIO = "selected_audio_name";

    public boolean slideshowEnabled = true;
    public boolean audioEnabled = false;
    public int slideshowSeconds = 10;
    public String selectedAudioName = "";

    public void load(SharedPreferences preferences) {
        slideshowEnabled = preferences.getBoolean(KEY_SLIDESHOW_ENABLED, true);
        slideshowSeconds = preferences.getInt(KEY_SLIDESHOW_SECONDS, 10);
        audioEnabled = preferences.getBoolean(KEY_AUDIO_ENABLED, false);
        selectedAudioName = preferences.getString(KEY_SELECTED_AUDIO, "");

        if (slideshowSeconds < 1) {
            slideshowSeconds = 1;
        }
        if (slideshowSeconds > 3600) {
            slideshowSeconds = 3600;
        }
    }

    public void save(SharedPreferences preferences) {
        preferences.edit()
                .putBoolean(KEY_SLIDESHOW_ENABLED, slideshowEnabled)
                .putInt(KEY_SLIDESHOW_SECONDS, slideshowSeconds)
                .putBoolean(KEY_AUDIO_ENABLED, audioEnabled)
                .putString(KEY_SELECTED_AUDIO, selectedAudioName)
                .apply();
    }
}
