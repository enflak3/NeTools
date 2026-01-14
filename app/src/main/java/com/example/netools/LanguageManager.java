package com.example.netools;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public class LanguageManager {
    private static final String PREFS_NAME = "prefs";
    private static final String KEY_LANG = "lang_code";

    public static void setLocale(Context context, String langCode) {
        Locale locale = new Locale(langCode);
        Locale.setDefault(locale);
        Resources resources = context.getResources();
        Configuration config = resources.getConfiguration();
        config.setLocale(locale);
        resources.updateConfiguration(config, resources.getDisplayMetrics());
        
        // Save to prefs
        SharedPreferences.Editor editor = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.putString(KEY_LANG, langCode);
        editor.apply();
    }

    public static void loadLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String langCode = prefs.getString(KEY_LANG, "fr");
        setLocale(context, langCode);
    }
    
    public static String getLangCode(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_LANG, "fr");
    }
}
