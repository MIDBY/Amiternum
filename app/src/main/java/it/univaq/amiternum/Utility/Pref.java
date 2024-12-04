package it.univaq.amiternum.Utility;

import android.content.Context;
import android.content.SharedPreferences;

public class Pref {

    public static void save(Context context, String key, boolean value){
        SharedPreferences preferences = context.getSharedPreferences("amiternumPref", Context.MODE_PRIVATE);
        preferences.edit().putBoolean(key, value).apply();

    }

    public static boolean load(Context context, String key, boolean defaultvalue){
        SharedPreferences preferences = context.getSharedPreferences("amiternumPref", Context.MODE_PRIVATE);
        return preferences.getBoolean(key, defaultvalue);
    }
}
