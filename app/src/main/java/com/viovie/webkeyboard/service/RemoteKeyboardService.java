package com.viovie.webkeyboard.service;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.inputmethodservice.KeyboardView.OnKeyboardActionListener;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.ExtractedTextRequest;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.viovie.webkeyboard.R;
import com.viovie.webkeyboard.Schema;
import com.viovie.webkeyboard.WebServer;
import com.viovie.webkeyboard.activity.MainActivity;
import com.viovie.webkeyboard.util.InternetUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

public class RemoteKeyboardService extends InputMethodService implements
        OnKeyboardActionListener {

    public static final String TAG = "RemoteKeyboardService";

    /**
     * For referencing our notification in the notification area.
     */
    public static final int NOTIFICATION = 42;
    /**
     * Reference to the running service
     */
    public static RemoteKeyboardService self;

    /**
     * For posting InputActions on the UI thread.
     */
    public Handler handler;

    /**
     * Contains key/value replacement pairs
     */
    public HashMap<String, String> replacements;

    private WebServer webServer;

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        self = this;
        handler = new Handler();

        try {
            webServer = new WebServer(this, 8080);
            webServer.start();
            updateNotification(null);
            loadReplacements();
        } catch (IOException e) {
            Log.w(TAG, e);
        }
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        return p.getBoolean("pref_fullscreen", false);
    }

    @Override
    public View onCreateInputView() {
        KeyboardView ret = new KeyboardView(this, null);
        ret.setKeyboard(new Keyboard(this, R.xml.keyboarddef));
        ret.setOnKeyboardActionListener(this);
        ret.setPreviewEnabled(false);
        return ret;
    }

    @Override
    public void onInitializeInterface() {
        super.onInitializeInterface();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        webServer.stop();
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION);
        self = null;
    }

    @Override
    public void onPress(int primaryCode) {
        switch (primaryCode) {
            case 0: {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.showInputMethodPicker();
                break;
            }
        }
    }

    @Override
    public void onRelease(int primaryCode) {
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
    }

    @Override
    public void onText(CharSequence text) {
    }

    @Override
    public void swipeLeft() {
    }

    @Override
    public void swipeRight() {
    }

    @Override
    public void swipeDown() {
    }

    @Override
    public void swipeUp() {
    }

    /**
     * Update the message in the notification area
     *
     * @param remote the remote host we are connected to or null if not connected.
     */
    protected void updateNotification(InetAddress remote) {
        String title = getString(R.string.notification_title);
        String content = null;
        if (remote == null) {
            String ipAddress = InternetUtil.getWifiIpAddress(this);
            content = getString(R.string.notification_waiting, "" + ipAddress);
        } else {
            content = getString(R.string.notification_peer, remote.getHostName());
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder
                .setContentText(content)
                .setContentTitle(title)
                .setOngoing(true)
                .setContentIntent(
                        PendingIntent.getActivity(this, 0, new Intent(this,
                                MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT))
                .setSmallIcon(R.drawable.ic_stat_service);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION, builder.build());
    }

    /**
     * Load the replacements map from the database
     */
    public void loadReplacements() {
        HashMap<String, String> tmp = new HashMap<String, String>();
        SQLiteDatabase database = new Schema(RemoteKeyboardService.self)
                .getReadableDatabase();
        String[] columns = {Schema.COLUMN_KEY, Schema.COLUMN_VALUE};
        Cursor cursor = database.query(Schema.TABLE_REPLACEMENTS, columns, null,
                null, null, null, null);
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            tmp.put(cursor.getString(0), cursor.getString(1));
            cursor.moveToNext();
        }
        database.close();
        replacements = tmp;
    }

}