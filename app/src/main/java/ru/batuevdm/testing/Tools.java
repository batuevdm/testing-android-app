package ru.batuevdm.testing;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

public class Tools extends Application {
    private static SharedPreferences storage;

    public void onCreate() {
        super.onCreate();
        storage = getApplicationContext().getSharedPreferences("UserSettings", Context.MODE_PRIVATE);
    }

    private static boolean saveSettings(String name, String value) {
        SharedPreferences.Editor editor = storage.edit();
        editor.putString(name, value);
        return editor.commit();
    }

    private static String getSettings(String name) {
        if (storage.contains(name)) {
            return storage.getString(name, "");
        }
        return "";
    }

    public static void hideSoftKeyboard(Activity activity, View view) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);
    }

    public static String secondsToTime(int seconds) {
        int hours = seconds / 3600;
        seconds -= hours * 3600;

        int minutes = seconds / 60;
        seconds -= minutes * 60;

        StringBuilder time = new StringBuilder();

        if (hours > 0)
            time.append(hours)
                    .append(" ч ");
        if (minutes > 0)
            time.append(minutes)
                    .append(" мин ");
        if (seconds > 0)
            time.append(seconds)
                    .append(" сек");

        return time.toString();
    }

    public static void clearToken() {
        saveSettings("token", "");
    }

    public static String getToken() {
        return getSettings("token");
    }

    public static void setToken(String token) {
        saveSettings("token", token);
    }
}