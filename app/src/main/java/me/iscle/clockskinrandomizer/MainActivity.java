package me.iscle.clockskinrandomizer;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.UriPermission;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.DocumentsContract;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, CompoundButton.OnCheckedChangeListener {
    private static final String TAG = "MainActivity";
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 1;
    private static final int REQUEST_RANDOMIZE = 2;
    private static final int REQUEST_NEW_EXTERNAL_STORAGE = 3;
    private static final String INTENT_ACTION = "me.iscle.clockskinrandomizer.randomize";

    private ConstraintLayout mainLayout;
    private ToggleButton enable;
    private EditText value;
    private Spinner units;
    private Button set;

    private SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mainLayout = findViewById(R.id.main_layout);
        enable = findViewById(R.id.enable);
        enable.setOnCheckedChangeListener(this);
        value = findViewById(R.id.value);
        units = findViewById(R.id.units);
        set = findViewById(R.id.set);
        set.setOnClickListener(this);

        sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        enable.setChecked(sharedPrefs.getBoolean("enable", false));
        value.setText(sharedPrefs.getString("value", ""));
        units.setSelection(sharedPrefs.getInt("units", 0));

        adjustInset();
    }

    private void adjustInset() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getResources().getConfiguration().isScreenRound()) {
                DisplayMetrics dm = getResources().getDisplayMetrics();
                int inset = (int) (0.146467f * (float) (dm.widthPixels / 2));
                mainLayout.setPadding(inset, inset, inset, inset);
            }
        }
    }

    @Override
    public void onClick(View view) {
        if (view == set) {
            if (value.getText().length() == 0) {
                Log.d(TAG, "onClick: Value is missing...");
                Toast.makeText(this, "Value is missing!", Toast.LENGTH_SHORT).show();
                value.requestFocus();
                return;
            }

            int minutes = getDelayMinutes(false);

            if (minutes < 1) {
                Log.d(TAG, "onClick: Delay is less than 1 minute...");
                Toast.makeText(this, "Delay can't be less than 1 minute!", Toast.LENGTH_SHORT).show();
                value.requestFocus();
                return;
            }

            setDelay();
        }
    }

    private void setDelay() {
        int minutes = getDelayMinutes(false);

        Log.d(TAG, "onClick: Setting delay to " + minutes + " minutes...");

        if (sharedPrefs.getBoolean("enable", false)) {
            startWork(minutes);
        }

        SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
        sharedPrefsEditor.putString("value", value.getText().toString());
        sharedPrefsEditor.putInt("units", units.getSelectedItemPosition());
        sharedPrefsEditor.apply();

        Toast.makeText(this, "Delay set!", Toast.LENGTH_SHORT).show();
    }

    private int getDelayMinutes(boolean useSharedPrefs) {
        int minutes;

        if (useSharedPrefs) {
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
        } else {
            int valueInt = Integer.valueOf(value.getText().toString());

            switch (units.getSelectedItemPosition()) {
                case 1:
                    minutes = valueInt * 60;
                    break;
                case 2:
                    minutes = valueInt * 60 * 24;
                    break;
                default:
                    minutes = valueInt;
            }
        }

        return minutes;
    }

    private void startWorkIfRequired() {
        if (sharedPrefs.getBoolean("enable", false)) {
            startWork(getDelayMinutes(false));
        }
    }

    private void startWork(int minutes) {
        stopWork();

        if (!requestExternalStoragePermissions()) {
            return;
        }

        Intent intent = new Intent(this, RandomizeReceiver.class);
        intent.setAction(INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this,
                REQUEST_RANDOMIZE, intent, 0);
        ((AlarmManager) getSystemService(ALARM_SERVICE)).setRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime(),
                minutes * 60 * 1000, pendingIntent);

        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    private void stopWork() {
        ComponentName receiver = new ComponentName(this, BootReceiver.class);
        PackageManager pm = getPackageManager();

        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);

        Intent intent = new Intent(this, RandomizeReceiver.class);
        intent.setAction(INTENT_ACTION);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REQUEST_RANDOMIZE,
                intent, PendingIntent.FLAG_NO_CREATE);

        if (pendingIntent != null)
            ((AlarmManager) getSystemService(ALARM_SERVICE)).cancel(pendingIntent);
    }

    @Override
    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
        if (compoundButton == enable) {
            if (sharedPrefs.getBoolean("enable", false) != b) {
                if (b) {
                    Log.d(TAG, "onCheckedChanged: Enabling worker...");
                    startWork(getDelayMinutes(true));
                } else {
                    Log.d(TAG, "onCheckedChanged: Disabling worker...");
                    stopWork();
                }

                SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
                sharedPrefsEditor.putBoolean("enable", b);
                sharedPrefsEditor.apply();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_EXTERNAL_STORAGE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                enable.setChecked(true);

                startWork(getDelayMinutes(true));

                SharedPreferences.Editor sharedPrefsEditor = sharedPrefs.edit();
                sharedPrefsEditor.putBoolean("enable", true);
                sharedPrefsEditor.apply();
            }
        }
    }

    private boolean requestExternalStoragePermissions() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Uri folderUri = Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "clockskin"));
            for (UriPermission up : getContentResolver().getPersistedUriPermissions()) {
                if (up.getUri().equals(folderUri) && up.isReadPermission()) {
                    return true;
                }
            }

            Toast.makeText(this, "Select the clockskin folder", Toast.LENGTH_LONG).show();
            // Choose a directory using the system's file picker.
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);

            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker when it loads.
            intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, folderUri);

            // Check for the freshest data.
            getContentResolver().takePersistableUriPermission(folderUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivityForResult(intent, REQUEST_NEW_EXTERNAL_STORAGE);
            return false;
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                enable.setChecked(false);
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_READ_EXTERNAL_STORAGE);
                return false;
            }

            return true;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_NEW_EXTERNAL_STORAGE) {
            if (resultCode == RESULT_OK) {
                startWorkIfRequired();
            } else {
                Toast.makeText(this, "You must select a valid clockskin folder!", Toast.LENGTH_LONG).show();
                finishAffinity();
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
