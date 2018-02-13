package com.james.status.data.preference;

import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.james.status.R;
import com.james.status.data.IconStyleData;
import com.james.status.data.icon.IconData;
import com.james.status.dialogs.IconDialog;
import com.james.status.dialogs.PreferenceDialog;
import com.james.status.views.IconStyleImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IconPreferenceData extends BasePreferenceData<String> {

    private IconStyleData iconStyle;
    private IconData iconData;
    private Map<String, IconStyleData> styles;
    private OnPreferenceChangeListener<IconStyleData> listener;

    public IconPreferenceData(Context context, Identifier<String> identifier, IconData iconData, OnPreferenceChangeListener<IconStyleData> listener) {
        super(context, identifier);
        this.iconData = iconData;
        styles = new HashMap<>();

        List<IconStyleData> styleList = iconData.getIconStyles();
        for (IconStyleData style : styleList) {
            styles.put(style.name, style);
        }

        iconStyle = styles.get(identifier.getPreferenceValue(context, styleList.get(0).name));
    }

    @Override
    public ViewHolder getViewHolder(LayoutInflater inflater, ViewGroup parent) {
        return new ViewHolder(inflater.inflate(R.layout.item_preference_icon, parent, false));
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        ((IconStyleImageView) holder.v.findViewById(R.id.icon)).setIconStyle(iconStyle);
    }

    @Override
    public void onClick(final View v) {
        Dialog dialog = new IconDialog(getContext(), iconData).setPreference(iconStyle).setListener(new PreferenceDialog.OnPreferenceListener<IconStyleData>() {
            @Override
            public void onPreference(PreferenceDialog dialog, IconStyleData preference) {
                if (preference != null) {
                    ((IconStyleImageView) v.findViewById(R.id.icon)).setIconStyle(preference);

                    IconPreferenceData.this.iconStyle = preference;
                    getIdentifier().setPreferenceValue(getContext(), preference.name);
                    onPreferenceChange(preference.name);
                    if (listener != null)
                        listener.onPreferenceChange(preference);
                }
            }

            @Override
            public void onCancel(PreferenceDialog dialog) {
            }
        });
        dialog.setTitle(getIdentifier().getTitle());
        dialog.show();
    }
}
