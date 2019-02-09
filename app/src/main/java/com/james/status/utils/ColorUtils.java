/*
 *    Copyright 2019 James Fenn
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.james.status.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Color;

import com.james.status.data.AppData;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;

public class ColorUtils {

    @ColorInt
    public static int darkColor(@ColorInt int color) {
        return Color.argb(255, addToColorPart(Color.red(color), -70), addToColorPart(Color.green(color), -70), addToColorPart(Color.blue(color), -70));
    }

    @ColorInt
    public static int lightColor(@ColorInt int color) {
        return Color.argb(255, addToColorPart(Color.red(color), 70), addToColorPart(Color.green(color), 70), addToColorPart(Color.blue(color), 70));
    }

    private static int addToColorPart(int colorPart, int variable) {
        return Math.max(0, Math.min(255, colorPart + variable));
    }

    public static int muteColor(int color, int variant) {
        int mutedColor = Color.argb(255, (int) (127.5 + Color.red(color)) / 2, (int) (127.5 + Color.green(color)) / 2, (int) (127.5 + Color.blue(color)) / 2);
        switch (variant % 3) {
            case 1:
                return Color.argb(255, Color.red(mutedColor) + 10, Color.green(mutedColor) + 10, Color.blue(mutedColor) + 10);
            case 2:
                return Color.argb(255, Color.red(mutedColor) - 10, Color.green(mutedColor) - 10, Color.blue(mutedColor) - 10);
            default:
                return mutedColor;
        }
    }

    @Nullable
    public static Integer getPrimaryColor(Context context, ComponentName componentName) {
        PackageManager packageManager = context.getPackageManager();

        ActivityInfo activityInfo = null;
        PackageInfo packageInfo = null;
        Resources resources = null, activityResources = null;
        try {
            packageInfo = packageManager.getPackageInfo(componentName.getPackageName(), PackageManager.GET_META_DATA);
            resources = packageManager.getResourcesForApplication(packageInfo.applicationInfo);
            activityInfo = packageManager.getActivityInfo(componentName, 0);
            activityResources = packageManager.getResourcesForActivity(componentName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (packageInfo != null && resources != null) {
            if (activityInfo != null && activityResources != null) {
                List<Integer> activityStatusBarColors = getStatusBarColors(activityInfo.packageName, resources, activityInfo.theme);
                if (activityStatusBarColors.size() > 0) {
                    return activityStatusBarColors.get(0);
                }
            }

            List<Integer> statusBarColors = getStatusBarColors(packageInfo.packageName, resources, packageInfo.applicationInfo.theme);
            if (statusBarColors.size() > 0) {
                return statusBarColors.get(0);
            }

            if (packageInfo.activities != null) {
                for (ActivityInfo otherActivityInfo : packageInfo.activities) {
                    List<Integer> otherStatusBarColors = getStatusBarColors(packageInfo.packageName, resources, otherActivityInfo.theme);
                    if (otherStatusBarColors.size() > 0) {
                        return otherStatusBarColors.get(0);
                    }
                }
            }
        }

        return null;
    }

    private static List<Integer> getPrimaryColors(Context context, ComponentName componentName) {
        List<Integer> colors = new ArrayList<>();

        PackageManager packageManager = context.getPackageManager();

        ActivityInfo activityInfo = null;
        PackageInfo packageInfo = null;
        Resources resources = null, activityResources = null;
        try {
            packageInfo = packageManager.getPackageInfo(componentName.getPackageName(), PackageManager.GET_META_DATA);
            resources = packageManager.getResourcesForApplication(packageInfo.applicationInfo);
            activityInfo = packageManager.getActivityInfo(componentName, 0);
            activityResources = packageManager.getResourcesForActivity(componentName);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (packageInfo != null && resources != null) {
            if (activityInfo != null && activityResources != null) {
                colors.addAll(getStatusBarColors(activityInfo.packageName, resources, activityInfo.theme));
            }

            colors.addAll(getStatusBarColors(packageInfo.packageName, resources, packageInfo.applicationInfo.theme));

            if (packageInfo.activities != null) {
                for (ActivityInfo otherActivityInfo : packageInfo.activities) {
                    colors.addAll(getStatusBarColors(packageInfo.packageName, resources, otherActivityInfo.theme));
                }
            }
        }

        return colors;
    }

    public static List<Integer> getColors(Context context, String packageName) {
        List<Integer> colors = new ArrayList<>();

        PackageManager packageManager = context.getPackageManager();

        PackageInfo packageInfo = null;
        Resources resources = null;
        try {
            packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_META_DATA);
            resources = packageManager.getResourcesForApplication(packageInfo.applicationInfo);
        } catch (PackageManager.NameNotFoundException ignored) {
        }

        if (packageInfo != null && resources != null) {
            colors.addAll(getStatusBarColors(packageInfo.packageName, resources, packageInfo.applicationInfo.theme));

            if (packageInfo.activities != null) {
                for (ActivityInfo activityInfo : packageInfo.activities) {
                    colors.addAll(getPrimaryColors(context, new ComponentName(activityInfo.packageName, activityInfo.name)));
                }
            }
        }

        return colors;
    }


    public static List<Integer> getColors(Context context, AppData app) {
        List<Integer> colors = new ArrayList<>();
        colors.addAll(getColors(context, app.packageName));
        for (AppData.ActivityData activity : app.activities) {
            Integer color = getPrimaryColor(context, activity.getComponentName());
            if (color != null) colors.add(color);
        }

        return colors;
    }

    private static List<Integer> getStatusBarColors(String packageName, Resources resources, int style) {
        List<Integer> colors = new ArrayList<>();

        Resources.Theme theme = resources.newTheme();
        theme.applyStyle(style, true);

        TypedArray typedArray = theme.obtainStyledAttributes(style, new int[]{
                resources.getIdentifier("colorPrimaryDark", "attr", packageName),
                resources.getIdentifier("statusBarColor", "attr", packageName),
                resources.getIdentifier("colorPrimaryDark", "color", packageName)
        });

        for (int i = 0; i < typedArray.length(); i++) {
            int statusBarRes = typedArray.getResourceId(i, 0);
            if (statusBarRes != 0) {
                try {
                    colors.add(ResourcesCompat.getColor(resources, statusBarRes, theme) | 0xFF000000);
                } catch (Resources.NotFoundException ignored) {
                }
            }
        }

        typedArray = theme.obtainStyledAttributes(style, new int[]{
                resources.getIdentifier("colorPrimary", "attr", packageName),
                resources.getIdentifier("colorPrimary", "color", packageName),
                resources.getIdentifier("navigationBarColor", "attr", packageName),
                resources.getIdentifier("colorAccent", "color", packageName)
        });

        for (int i = 0; i < typedArray.length(); i++) {
            int statusBarRes = typedArray.getResourceId(i, 0);
            if (statusBarRes != 0) {
                try {
                    colors.add(darkColor(ResourcesCompat.getColor(resources, statusBarRes, theme) | 0xFF000000));
                } catch (Resources.NotFoundException ignored) {
                }
            }
        }

        return colors;
    }

    @ColorInt
    public static int getAverageColor(@NonNull Bitmap bitmap) {
        int red = 0, green = 0, blue = 0;
        int width = bitmap.getWidth(), height = bitmap.getHeight(), size = width * height;

        int[] pixels = new int[size];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int color = pixels[x + y * width];
                red += (color >> 16) & 0xFF;
                green += (color >> 8) & 0xFF;
                blue += (color & 0xFF);
            }
        }

        return Color.rgb(red / size, green / size, blue / size);
    }

    /**
     * Calculates the visible difference between two colors.
     *
     * @param color1 a color
     * @param color2 another color
     * @return the calculated difference between 0 and 255
     */
    public static double getDifference(@ColorInt int color1, @ColorInt int color2) {
        double diff = Math.abs(0.299 * (Color.red(color1) - Color.red(color2)));
        diff += Math.abs(0.587 * (Color.green(color1) - Color.green(color2)));
        diff += Math.abs(0.114 * (Color.blue(color1) - Color.blue(color2)));
        return diff;
    }
}
