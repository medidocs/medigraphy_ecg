package com.medigraphy.ecg;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.WindowManager;

public class StartUp extends Activity {

    SharedPreferences appPreferences1;
    public static boolean isAppInstalled = false;
    boolean copydata = true;
    boolean filec = false;
    SharedPreferences preferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_up);

        try {
            Thread.sleep(2000);
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
//        Intent i=new Intent(getApplicationContext(),MainActivity.class);
//        startActivity(i);
//        finish();
        preferences = getSharedPreferences("LOGIN_PREF", MODE_PRIVATE);
        editor = preferences.edit();

        if (isNetworkConnected()) {
            if (preferences.getBoolean("isLogin", false)) {
                startActivity(new Intent(StartUp.this, MainActivity.class));
                finish();
            } else {
                editor.putBoolean("isLogin", false);
                editor.apply();
                startActivity(new Intent(StartUp.this, MainActivity.class));
                finish();
            }
        }

        try {
            createShortcut();
            if (copydata)
                copyAssets();
        } catch (Exception e) {
            System.out.println("startUp->" + e.getMessage());
        }
    }

    //copies the dat file to sdcard
    @SuppressWarnings("unused")
    private void copyAssets() {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            Log.e("tag", "Failed to get asset file list.", e);
        }

        for (String filename : files) {

            InputStream in = null;
            OutputStream out = null;
            try {
                in = assetManager.open("Demo.dat");
                File outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS) + "/ECG/ECG-Recorder Reports");
                if (!outFile.isDirectory()) {
                    filec = outFile.mkdirs();
                }
                //Log.e("iscreated?", "copyAssets: "+filec);

                File ECG_Data = new File(outFile, "Demo.dat");
                out = new FileOutputStream(ECG_Data);
                copyFile(in, out);
            } catch (IOException e) {
                Log.e("tag", "Failed to copy asset file: " + "Demo.dat", e);
            } finally {
                if (in != null) {
                    try {
                        in.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        int size = in.available();
        byte[] buffer = new byte[size];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
    }

    private void createShortcut() {
        /**
         * check if application is running first time, only then create shorcut
         */
        appPreferences1 = PreferenceManager.getDefaultSharedPreferences(this);
        isAppInstalled = appPreferences1.getBoolean("isAppInstalled", false);
        if (isAppInstalled == false) {
            copydata = true;
            /**
             * create short code
             */
            Intent shortcutIntent = new Intent(getApplicationContext(), StartUp.class);
            shortcutIntent.setAction(Intent.ACTION_MAIN);
            Intent intent = new Intent();
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, "ECG Connect");
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(getApplicationContext(), R.drawable.ic_ecg));
            intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
            getApplicationContext().sendBroadcast(intent);
            /**
             * Make preference true
             */
            SharedPreferences.Editor editor = appPreferences1.edit();
            editor.putBoolean("isAppInstalled", true);
            editor.commit();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {//SNB: WHY DO WE HAVE THIS METHOD??
        switch (requestCode) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.start_up, menu);
        return true;
    }

    private boolean isNetworkConnected() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo() != null && cm.getActiveNetworkInfo().isConnected();
    }
}
