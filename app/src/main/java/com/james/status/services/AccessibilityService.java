package com.james.status.services;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.Parcelable;
import android.support.annotation.ColorInt;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.accessibility.AccessibilityEvent;
import android.widget.Toast;

import com.james.status.R;
import com.james.status.Status;
import com.james.status.data.AppData;
import com.james.status.data.NotificationData;
import com.james.status.data.PreferenceData;
import com.james.status.utils.ColorUtils;
import com.james.status.utils.StaticUtils;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class AccessibilityService extends android.accessibilityservice.AccessibilityService {

    public static final String ACTION_GET_COLOR = "com.james.status.ACTION_GET_COLOR";

    private PackageManager packageManager;
    private List<NotificationData> notifications;

    private AppData.ActivityData activityData;
    private VolumeReceiver volumeReceiver;

    private int color = Color.BLACK;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case ACTION_GET_COLOR:
                        Intent i = new Intent(StatusServiceImpl.ACTION_UPDATE);
                        i.setClass(this, StatusServiceImpl.getCompatClass(this));
                        i.putExtra(StatusServiceImpl.EXTRA_COLOR, color);
                        startService(i);
                        break;
                }
            }
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();

        packageManager = getPackageManager();

        volumeReceiver = new VolumeReceiver(this);
        registerReceiver(volumeReceiver, new IntentFilter("android.media.VOLUME_CHANGED_ACTION"));

        notifications = new ArrayList<>();
        AccessibilityServiceInfo config = new AccessibilityServiceInfo();
        config.eventTypes = AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        config.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            config.flags = AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;

        setServiceInfo(config);
    }

    @Override
    public void onAccessibilityEvent(final AccessibilityEvent event) {
        if (PreferenceData.STATUS_ENABLED.getValue(this)) {
            switch (event.getEventType()) {
                case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:
                    if (StaticUtils.shouldUseCompatNotifications(this) && !event.getPackageName().toString().matches("com.james.status")) {
                        Parcelable parcelable = event.getParcelableData();
                        if (parcelable instanceof Notification) {
                            NotificationData notification = new NotificationData((Notification) parcelable, event.getPackageName().toString());

                            Intent intent = new Intent(StatusServiceCompat.ACTION_NOTIFICATION_ADDED);
                            intent.putExtra(StatusServiceCompat.EXTRA_NOTIFICATION, notification);
                            intent.setClass(this, StatusServiceCompat.class);
                            sendBroadcast(intent);

                            notifications.add(notification);
                        }
                    }
                    return;
                case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
                    final CharSequence packageName = event.getPackageName();
                    final CharSequence className = event.getClassName();
                    Status.showDebug(this, event.toString(), Toast.LENGTH_LONG);

                    if (packageManager != null && packageName != null && packageName.length() > 0 && className != null && className.length() > 0) {
                        try {
                            activityData = new AppData.ActivityData(packageManager, packageManager.getActivityInfo(new ComponentName(packageName.toString(), className.toString()), PackageManager.GET_META_DATA));
                        } catch (PackageManager.NameNotFoundException | NullPointerException e) {
                            if (activityData != null) {
                                if (packageName.toString().equals("com.android.systemui")) {
                                    if (event.getText().toString().toLowerCase().contains("volume")) {
                                        if (event.getText().toString().toLowerCase().contains("hidden")) {
                                            volumeReceiver.cancel();
                                            setStatusBar(null, false, null, false, null, null);
                                        } else if (!VolumeReceiver.canReceive())
                                            volumeReceiver.onVolumeChanged();
                                    } else setStatusBar(null, false, null, true, null, null);

                                    if (StaticUtils.shouldUseCompatNotifications(this)) {
                                        for (NotificationData notification : notifications) {
                                            Intent intent = new Intent(StatusServiceCompat.ACTION_NOTIFICATION_REMOVED);
                                            intent.putExtra(StatusServiceCompat.EXTRA_NOTIFICATION, notification);
                                            sendBroadcast(intent);
                                        }

                                        notifications.clear();
                                    }
                                } else setStatusBar(null, false, null, false, null, null);
                            }
                            return;
                        }

                        Boolean isFullscreen = activityData.getBooleanPreference(this, AppData.PreferenceIdentifier.FULLSCREEN);
                        boolean isHome = false;

                        if (packageManager != null) {
                            Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                            homeIntent.addCategory(Intent.CATEGORY_HOME);
                            ResolveInfo homeInfo = packageManager.resolveActivity(homeIntent, PackageManager.MATCH_DEFAULT_ONLY);

                            isHome = homeInfo != null && packageName.toString().matches(homeInfo.activityInfo.packageName);
                        }

                        Integer color = activityData.getIntegerPreference(this, AppData.PreferenceIdentifier.COLOR);
                        if (color != null && (!isHome || !(boolean) PreferenceData.STATUS_HOME_TRANSPARENT.getValue(this))) {
                            setStatusBar(color, null, isFullscreen, false, packageName.toString(), activityData);
                            return;
                        } else if (isHome) {
                            setStatusBar(null, true, isFullscreen, false, packageName.toString(), activityData);
                            return;
                        }

                        if (!(boolean) PreferenceData.STATUS_COLOR_AUTO.getValue(this)) {
                            setStatusBar((int) PreferenceData.STATUS_COLOR.getValue(this), null, isFullscreen, false, packageName.toString(), activityData);
                            return;
                        }

                        if (packageName.toString().equals("com.android.systemui")) {
                            //prevents the creation of some pretty nasty looking color schemes below Lollipop
                            setStatusBar((int) PreferenceData.STATUS_COLOR.getValue(this), null, false, false, packageName.toString(), activityData);
                            return;
                        }

                        if (packageName.toString().matches("com.james.status")) {
                            //prevents recursive heads up notifications
                            setStatusBar(ContextCompat.getColor(this, R.color.colorPrimaryDark), null, isFullscreen, false, packageName.toString(), activityData);
                            return;
                        }

                        Integer cacheVersion = activityData.getIntegerPreference(this, AppData.PreferenceIdentifier.CACHE_VERSION);
                        if (cacheVersion != null && cacheVersion == activityData.version) {
                            color = activityData.getIntegerPreference(this, AppData.PreferenceIdentifier.CACHE_COLOR);
                        }

                        if (color == null) {
                            color = ColorUtils.getPrimaryColor(AccessibilityService.this, new ComponentName(packageName.toString(), className.toString()));

                            if (color != null) {
                                activityData.putPreference(this, AppData.PreferenceIdentifier.CACHE_COLOR, color);
                                activityData.putPreference(this, AppData.PreferenceIdentifier.CACHE_VERSION, activityData.version);
                            }
                        }

                        setStatusBar(color != null ? color : (int) PreferenceData.STATUS_COLOR.getValue(this), null, isFullscreen, false, packageName.toString(), activityData);
                    }
            }
        }
    }

    private void setStatusBar(@Nullable @ColorInt Integer color, @Nullable Boolean isTransparent, @Nullable Boolean isFullscreen, @Nullable Boolean isSystemFullscreen, @Nullable String packageName, @Nullable AppData.ActivityData activityData) {
        Intent intent = new Intent(StatusServiceImpl.ACTION_UPDATE);
        intent.setClass(this, StatusServiceImpl.getCompatClass(this));

        if (color != null) intent.putExtra(StatusServiceImpl.EXTRA_COLOR, color);

        if (isTransparent != null)
            intent.putExtra(StatusServiceImpl.EXTRA_IS_TRANSPARENT, isTransparent);

        if (isFullscreen != null)
            intent.putExtra(StatusServiceImpl.EXTRA_IS_FULLSCREEN, isFullscreen);

        if (isSystemFullscreen != null)
            intent.putExtra(StatusServiceImpl.EXTRA_IS_SYSTEM_FULLSCREEN, isSystemFullscreen);

        if (packageName != null)
            intent.putExtra(StatusServiceImpl.EXTRA_PACKAGE, packageName);

        if (activityData != null)
            intent.putExtra(StatusServiceImpl.EXTRA_ACTIVITY, activityData);

        startService(intent);

        if (color != null) this.color = color;
    }

    @Override
    public void onInterrupt() {
    }

    @Override
    public void onDestroy() {
        if (volumeReceiver != null) unregisterReceiver(volumeReceiver);
        super.onDestroy();
    }

    private static class VolumeReceiver extends BroadcastReceiver {

        private SoftReference<AccessibilityService> reference;
        private Handler handler;
        private Runnable runnable;

        private VolumeReceiver(AccessibilityService service) {
            reference = new SoftReference<>(service);
            handler = new Handler();
            runnable = new Runnable() {
                @Override
                public void run() {
                    AccessibilityService service = reference.get();
                    if (service != null) {
                        Status.showDebug(service, "Volume callback called", Toast.LENGTH_SHORT);
                        service.setStatusBar(null, false, null, false, null, null);
                    }
                }
            };
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getExtras() != null)
                Status.showDebug(context, intent.getExtras().toString(), Toast.LENGTH_SHORT);
            onVolumeChanged();
        }

        private void onVolumeChanged() {
            AccessibilityService service = reference.get();
            if (service != null && (boolean) PreferenceData.STATUS_HIDE_ON_VOLUME.getValue(service)) {
                Status.showDebug(service, "Volume callback added", Toast.LENGTH_SHORT);
                service.setStatusBar(null, false, null, true, null, null);
                handler.removeCallbacks(runnable);

                handler.postDelayed(runnable, 3000);
            }
        }

        private void cancel() {
            AccessibilityService service = reference.get();
            if (service != null)
                Status.showDebug(service, "Volume callback removed", Toast.LENGTH_SHORT);
            handler.removeCallbacks(runnable);
        }

        private static boolean canReceive() {
            return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && Build.VERSION.SDK_INT < Build.VERSION_CODES.M;
        }
    }
}
