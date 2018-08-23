package com.james.status.services;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v7.app.NotificationCompat;

import com.james.status.R;
import com.james.status.data.AppData;
import com.james.status.data.NotificationData;
import com.james.status.data.PreferenceData;
import com.james.status.data.icon.NotificationsIconData;
import com.james.status.utils.StaticUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@TargetApi(18)
public class StatusService extends NotificationListenerService {

    public static final String ACTION_GET_NOTIFICATIONS = "com.james.status.ACTION_GET_NOTIFICATIONS";
    public static final String ACTION_CANCEL_NOTIFICATION = "com.james.status.ACTION_CANCEL_NOTIFICATION";
    public static final String EXTRA_NOTIFICATION = "com.james.status.EXTRA_NOTIFICATION";

    public static final int BLANK_NOTIFICATION = 254231;

    private boolean isConnected, shouldSendOnConnect;
    private PackageManager packageManager;
    private NotificationManagerCompat notificationManager;

    private StatusServiceImpl impl;

    @Override
    public void onCreate() {
        super.onCreate();
        if (impl == null)
            impl = new StatusServiceImpl(this);

        impl.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (intent == null) return START_STICKY;
        String action = intent.getAction();
        if (action == null) return START_STICKY;

        switch (action) {
            case ACTION_GET_NOTIFICATIONS:
                if (isConnected) sendNotifications();
                else shouldSendOnConnect = true;
                break;
            case ACTION_CANCEL_NOTIFICATION:
                NotificationData notification = intent.getParcelableExtra(EXTRA_NOTIFICATION);
                if (isConnected && notification != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        cancelNotification(notification.key);
                    else
                        cancelNotification(notification.packageName, notification.tag, notification.id);
                }
                break;
        }

        return impl.onStartCommand(intent, flags, startId);
    }

    public void tryReconnectService() {
        toggleNotificationListenerService();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            requestRebind(new ComponentName(getApplicationContext(), StatusService.class));
    }

    private void toggleNotificationListenerService() {
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, StatusService.class),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, StatusService.class),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        packageManager = getPackageManager();
        notificationManager = NotificationManagerCompat.from(this);
        isConnected = true;

        if (shouldSendOnConnect) {
            sendNotifications();
            shouldSendOnConnect = false;
        }
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        isConnected = false;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        AppData app = null;
        try {
            app = new AppData(packageManager, packageManager.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA), packageManager.getPackageInfo(sbn.getPackageName(), PackageManager.GET_ACTIVITIES));
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        Boolean isEnabled = null;
        if (app != null)
            isEnabled = app.getSpecificBooleanPreference(this, AppData.PreferenceIdentifier.NOTIFICATIONS);
        if (isEnabled == null) isEnabled = true;

        if ((boolean) PreferenceData.STATUS_ENABLED.getValue(this) && isEnabled && !StaticUtils.shouldUseCompatNotifications(this) && !sbn.getPackageName().matches("com.james.status")) {
            NotificationData notification = new NotificationData(sbn, getKey(sbn));

            if (notification.shouldShowHeadsUp(this)) {
                if (sbn.getId() != BLANK_NOTIFICATION) {
                    notificationManager.notify(BLANK_NOTIFICATION, new Notification.Builder(this)
                            .setContentTitle("").setContentText("")
                            .setSmallIcon(R.drawable.transparent)
                            .setPriority(Notification.PRIORITY_DEFAULT)
                            .setFullScreenIntent(PendingIntent.getBroadcast(this, 0, new Intent(), 0), true)
                            .setAutoCancel(true)
                            .build());

                    notificationManager.cancel(BLANK_NOTIFICATION);
                } else return;
            }

            Intent intent = new Intent(NotificationsIconData.ACTION_NOTIFICATION_ADDED);
            intent.putExtra(NotificationsIconData.EXTRA_NOTIFICATION, notification);
            sendBroadcast(intent);
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if ((boolean) PreferenceData.STATUS_ENABLED.getValue(this) && !StaticUtils.shouldUseCompatNotifications(this)) {
            Intent intent = new Intent(NotificationsIconData.ACTION_NOTIFICATION_REMOVED);
            intent.putExtra(NotificationsIconData.EXTRA_NOTIFICATION, new NotificationData(sbn, getKey(sbn)));
            sendBroadcast(intent);
        }
    }

    private ArrayList<StatusBarNotification> getNotifications() {
        ArrayList<StatusBarNotification> activeNotifications = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            try {
                activeNotifications.addAll(Arrays.asList(getActiveNotifications()));
            } catch (NullPointerException ignored) {
            }
        }
        return activeNotifications;
    }

    private void sendNotifications() {
        if ((boolean) PreferenceData.STATUS_ENABLED.getValue(this) && !StaticUtils.shouldUseCompatNotifications(this)) {
            List<StatusBarNotification> notifications = getNotifications();
            Collections.reverse(notifications);

            for (StatusBarNotification sbn : notifications) {
                if (sbn.getPackageName().matches("com.james.status"))
                    continue;

                AppData app = null;
                try {
                    app = new AppData(packageManager, packageManager.getApplicationInfo(sbn.getPackageName(), PackageManager.GET_META_DATA), packageManager.getPackageInfo(sbn.getPackageName(), PackageManager.GET_ACTIVITIES));
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }

                Boolean isEnabled = null;
                if (app != null)
                    isEnabled = app.getSpecificBooleanPreference(this, AppData.PreferenceIdentifier.NOTIFICATIONS);
                if (isEnabled != null && !isEnabled) continue;

                NotificationData notification = new NotificationData(sbn, getKey(sbn));
                notification.priority = NotificationCompat.PRIORITY_DEFAULT;

                Intent intent = new Intent(NotificationsIconData.ACTION_NOTIFICATION_ADDED);
                intent.putExtra(NotificationsIconData.EXTRA_NOTIFICATION, notification);

                sendBroadcast(intent);
            }
        }
    }

    private String getKey(StatusBarNotification statusBarNotification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return statusBarNotification.getKey();
        else
            return statusBarNotification.getPackageName() + "/" + String.valueOf(statusBarNotification.getId());
    }
}
