package com.yzc.bookmall.util;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private static final String PREF_NAME = "book_mall_session";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USERNAME = "username";
    private final SharedPreferences preferences;

    public SessionManager(Context context) { preferences = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE); }
    public void saveSession(long userId, String username) { preferences.edit().putLong(KEY_USER_ID, userId).putString(KEY_USERNAME, username).apply(); }
    public boolean isLoggedIn() { return getUserId() > 0; }
    public long getUserId() { return preferences.getLong(KEY_USER_ID, -1L); }
    public String getUsername() { return preferences.getString(KEY_USERNAME, ""); }
    public void clearSession() { preferences.edit().clear().apply(); }
}