package com.james.status.utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.util.TypedValue;

import com.james.status.BuildConfig;
import com.james.status.Status;
import com.james.status.activities.StartActivity;
import com.james.status.data.PreferenceData;
import com.james.status.data.icon.IconData;
import com.james.status.services.AccessibilityService;
import com.james.status.services.StatusService;

import java.util.ArrayList;
import java.util.List;

public class StaticUtils {

    public static int getStatusBarHeight(Context context) {
        int height = PreferenceData.STATUS_HEIGHT.getValue(context);
        if (height > 0)
            return height;

        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) return context.getResources().getDimensionPixelSize(resId);
        else return 0;
    }

    public static boolean shouldShowTutorial(Context context, String tutorialName) {
        return shouldShowTutorial(context, tutorialName, 0);
    }

    public static boolean shouldShowTutorial(Context context, String tutorialName, int limit) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int shown = prefs.getInt("tutorial" + tutorialName, 0);
        prefs.edit().putInt("tutorial" + tutorialName, shown + 1).apply();
        return limit == shown;
    }

    public static int getNavigationBarHeight(Context context) {
        int resId = context.getResources().getIdentifier("navigation_bar_height", "dimen", "android");
        if (resId > 0) return context.getResources().getDimensionPixelSize(resId);
        else return 0;
    }

    public static float getPixelsFromDp(int dp) {
        return dp * Resources.getSystem().getDisplayMetrics().density;
    }

    public static float getDpFromPixels(int px) {
        return px / Resources.getSystem().getDisplayMetrics().density;
    }

    public static int getPixelsFromSp(Context context, float sp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, context.getResources().getDisplayMetrics());
    }

    public static int getBluetoothState(Context context) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter != null) return adapter.getState();
        else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
                adapter = ((BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE)).getAdapter();
            if (adapter != null) return adapter.getState();
            else return BluetoothAdapter.STATE_OFF;
        }
    }

    public static boolean isNotificationGranted(Context context) {
        for (String packageName : NotificationManagerCompat.getEnabledListenerPackages(context)) {
            if (packageName.contains(context.getPackageName()) || packageName.equals(context.getPackageName()))
                return true;
        }
        return shouldUseCompatNotifications(context);
    }

    public static boolean isIgnoringOptimizations(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null)
                return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }

        return true;
    }

    public static boolean canDrawOverlays(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(context);
    }

    public static int getMergedValue(int v1, int v2, float r) {
        return (int) ((v1 * r) + (v2 * (1 - r)));
    }

    public static boolean isPermissionsGranted(Context context, String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG)
                    Log.wtf("Permission", "missing " + permission);
                return PreferenceData.STATUS_IGNORE_PERMISSION_CHECKING.getValue(context);
            }
        }

        return true;
    }

    public static boolean isAllPermissionsGranted(Context context) {
        for (IconData icon : StatusService.getIcons(context)) {
            for (String permission : icon.getPermissions()) {
                if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (BuildConfig.DEBUG)
                        Log.wtf("Permission", "missing " + permission);
                    return PreferenceData.STATUS_IGNORE_PERMISSION_CHECKING.getValue(context);
                }
            }
        }

        return true;
    }

    public static void requestPermissions(Activity activity, String[] permissions) {
        List<String> unrequestedPermissions = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED)
                unrequestedPermissions.add(permission);
        }

        if (unrequestedPermissions.size() > 0)
            ActivityCompat.requestPermissions(activity, unrequestedPermissions.toArray(new String[unrequestedPermissions.size()]), StartActivity.REQUEST_PERMISSIONS);
    }

    public static boolean isStatusServiceRunning(Context context) {
        if ((boolean) PreferenceData.STATUS_ENABLED.getValue(context) && isReady(context)) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (manager != null) {
                for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                    if (StatusService.class.getName().equals(service.service.getClassName())) {
                        return true;
                    }
                }

                Intent intent = new Intent(StatusService.ACTION_START);
                intent.setClass(context, StatusService.class);
                context.startService(intent);
                return true;
            }
        }

        return false;
    }

    /**
     * Sends an intent to apply preference changes to the StatusService
     *
     * @param context         current context to send intent from
     * @param shouldKeepIcons whether to reuse existing instances of IconDatas
     */
    public static void updateStatusService(Context context, boolean shouldKeepIcons) {
        if (isStatusServiceRunning(context)) {
            Intent intent = new Intent(StatusService.ACTION_START);
            intent.setClass(context, StatusService.class);
            intent.putExtra(StatusService.EXTRA_KEEP_OLD, shouldKeepIcons);
            context.startService(intent);
        }

        ((Status) context.getApplicationContext()).onPreferenceChanged();
    }

    public static boolean isAccessibilityServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (AccessibilityService.class.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            return false;
        } else return true;
    }

    public static boolean shouldUseCompatNotifications(Context context) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || (boolean) PreferenceData.STATUS_NOTIFICATIONS_COMPAT.getValue(context);
    }

    public static boolean isReady(Context context) {
        return StaticUtils.isAccessibilityServiceRunning(context) && StaticUtils.isNotificationGranted(context) && StaticUtils.canDrawOverlays(context);
    }
}
