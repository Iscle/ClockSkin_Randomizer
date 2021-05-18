package me.iscle.clockskinrandomizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import androidx.documentfile.provider.DocumentFile;

import java.io.File;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class RandomizeReceiver extends BroadcastReceiver {
    private static final String TAG = "RandomizeReceiver";
    private static final String INTENT_ACTION = "me.iscle.clockskinrandomizer.randomize";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction() != null && intent.getAction().equals(INTENT_ACTION)) {
            Log.d(TAG, "onReceive: Updating watchface...");
            updateWatchface(context);
        }
    }

    private void updateWatchface(Context context) {
        Intent intent = new Intent();
        intent.setAction("com.android.watchengine.changeface");
        intent.putExtra("faceName", getRandomClockskin(context));
        context.sendBroadcast(intent);
    }

    private int getRandomNumber(int max) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            return ThreadLocalRandom.current().nextInt(0, max);
        } else {
            return new Random().nextInt(max);
        }
    }

    private String getRandomClockskin(Context context) {
        File clockskinFolder = new File(Environment.getExternalStorageDirectory(), "clockskin");
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Uri folderUri = Uri.fromFile(clockskinFolder);
            DocumentFile clockskinFolderDf = DocumentFile.fromTreeUri(context, folderUri);
            DocumentFile[] clockskins = clockskinFolderDf.listFiles();
            return clockskins[getRandomNumber(clockskins.length)].getName();
        } else {
            File[] clockskins = clockskinFolder.listFiles();
            return clockskins[getRandomNumber(clockskins.length)].getName();
        }

    }
}
