package me.iscle.clockskinrandomizer;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;

import androidx.preference.PreferenceManager;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";
    private static final int REQUEST_RANDOMIZE = 2;
    private static final String INTENT_ACTION = "me.iscle.clockskinrandomizer.randomize";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            Intent alarmIntent = new Intent(context, RandomizeReceiver.class);
            intent.setAction(INTENT_ACTION);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context,
                    REQUEST_RANDOMIZE, alarmIntent, PendingIntent.FLAG_NO_CREATE);
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME,
                    SystemClock.elapsedRealtime(),
                    getDelayMinutes(context) * 1000, pendingIntent);
        }
    }

    private int getDelayMinutes(Context context) {
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        int minutes;
        int valueInt = Integer.valueOf(sharedPrefs.getString("value", "1"));

        switch (sharedPrefs.getInt("units", 0)) {
            case 1:
                minutes = valueInt * 60;
                break;
            case 2:
                minutes = valueInt * 60 * 24;
                break;
            default:
                minutes = valueInt;
        }

        return minutes;
    }
}
