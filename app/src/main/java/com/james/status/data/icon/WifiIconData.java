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

package com.james.status.data.icon;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;

import com.james.status.R;
import com.james.status.data.IconStyleData;
import com.james.status.receivers.IconUpdateReceiver;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class WifiIconData extends IconData<WifiIconData.WifiReceiver> {

    private WifiManager wifiManager;
    private ConnectivityManager connectivityManager;

    public WifiIconData(Context context) {
        super(context);
        wifiManager = (WifiManager) getContext().getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @Override
    public String[] getPermissions() {
        return new String[]{Manifest.permission.ACCESS_NETWORK_STATE};
    }

    @Override
    public WifiReceiver getReceiver() {
        return new WifiReceiver(this);
    }

    @Override
    public IntentFilter getIntentFilter() {
        return new IntentFilter(WifiManager.NETWORK_STATE_CHANGED_ACTION);
    }

    @Override
    public void register() {
        super.register();

        int level = WifiManager.calculateSignalLevel(wifiManager.getConnectionInfo().getRssi(), 5);
        if (level > 0)
            onIconUpdate(level); //temporary fix, cannot determine if wifi is enabled without BroadcastReceiver for some reason
    }

    @Override
    public String getTitle() {
        return getContext().getString(R.string.icon_wifi);
    }

    @Override
    public int getIconStyleSize() {
        return 5;
    }

    @Override
    public List<IconStyleData> getIconStyles() {
        List<IconStyleData> styles = super.getIconStyles();

        styles.addAll(
                Arrays.asList(
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_default),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_wifi_0,
                                R.drawable.ic_wifi_1,
                                R.drawable.ic_wifi_2,
                                R.drawable.ic_wifi_3,
                                R.drawable.ic_wifi_4
                        ),
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_radial),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_mdi_wifi_radial_0,
                                R.drawable.ic_mdi_wifi_radial_1,
                                R.drawable.ic_mdi_wifi_radial_2,
                                R.drawable.ic_mdi_wifi_radial_3,
                                R.drawable.ic_mdi_wifi_radial_4
                        ),
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_outline),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_mdi_wifi_outline_0,
                                R.drawable.ic_mdi_wifi_outline_1,
                                R.drawable.ic_mdi_wifi_outline_2,
                                R.drawable.ic_mdi_wifi_outline_3,
                                R.drawable.ic_mdi_wifi_outline_4
                        ),
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_triangle),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_wifi_triangle_0,
                                R.drawable.ic_wifi_triangle_1,
                                R.drawable.ic_wifi_triangle_2,
                                R.drawable.ic_wifi_triangle_3,
                                R.drawable.ic_wifi_triangle_4
                        ),
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_retro),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_wifi_retro_0,
                                R.drawable.ic_wifi_retro_1,
                                R.drawable.ic_wifi_retro_2,
                                R.drawable.ic_wifi_retro_3,
                                R.drawable.ic_wifi_retro_4
                        ),
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_classic),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_wifi_classic_0,
                                R.drawable.ic_wifi_classic_1,
                                R.drawable.ic_wifi_classic_2,
                                R.drawable.ic_wifi_classic_3,
                                R.drawable.ic_wifi_classic_4
                        ),
                        new IconStyleData(
                                getContext().getString(R.string.icon_style_clip),
                                IconStyleData.TYPE_VECTOR,
                                R.drawable.ic_number_clip_0,
                                R.drawable.ic_number_clip_1,
                                R.drawable.ic_number_clip_2,
                                R.drawable.ic_number_clip_3,
                                R.drawable.ic_number_clip_4
                        )
                )
        );

        styles.removeAll(Collections.singleton(null));
        return styles;
    }

    @Override
    public String[] getIconNames() {
        return new String[]{
                getContext().getString(R.string.icon_wifi_no_signal),
                getContext().getString(R.string.icon_wifi_1_bar),
                getContext().getString(R.string.icon_wifi_2_bars),
                getContext().getString(R.string.icon_wifi_3_bars),
                getContext().getString(R.string.icon_wifi_4_bars)
        };
    }

    static class WifiReceiver extends IconUpdateReceiver<WifiIconData> {

        private WifiReceiver(WifiIconData iconData) {
            super(iconData);
        }

        @Override
        public void onReceive(WifiIconData icon, Intent intent) {
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if (networkInfo == null) networkInfo = icon.connectivityManager.getActiveNetworkInfo();

            if (networkInfo.getType() == ConnectivityManager.TYPE_WIFI && networkInfo.isConnected())
                icon.onIconUpdate(WifiManager.calculateSignalLevel(icon.wifiManager.getConnectionInfo().getRssi(), 5));
            else icon.onIconUpdate(-1);
        }
    }
}
