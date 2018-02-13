package com.james.status.dialogs;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.Toast;

import com.james.status.R;
import com.james.status.adapters.IconStyleAdapter;
import com.james.status.data.IconStyleData;
import com.james.status.data.PreferenceData;
import com.james.status.data.icon.IconData;
import com.james.status.utils.ImageUtils;
import com.james.status.utils.StaticUtils;

import java.util.List;

public class IconDialog extends PreferenceDialog<IconStyleData> implements IconStyleAdapter.OnCheckedChangeListener {

    private IconData icon;
    private List<IconStyleData> styles;
    private IconStyleAdapter adapter;
    private RecyclerView recycler;

    private String title;

    public IconDialog(Context context, IconData icon) {
        super(context, R.style.AppTheme_Dialog_FullScreen);
        this.icon = icon;
        styles = icon.getIconStyles();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_icon_picker);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (title != null) toolbar.setTitle(title);

        Drawable back = ImageUtils.getVectorDrawable(getContext(), R.drawable.ic_back);
        DrawableCompat.setTint(back, Color.BLACK);
        toolbar.setNavigationIcon(back);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isShowing()) dismiss();
            }
        });

        recycler = (RecyclerView) findViewById(R.id.recycler);
        recycler.setLayoutManager(new GridLayoutManager(getContext(), 1));

        adapter = new IconStyleAdapter(getContext(), icon, styles, this);
        adapter.setIconStyle(getPreference());

        recycler.setAdapter(adapter);

        findViewById(R.id.add).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
                if (!StaticUtils.isPermissionsGranted(getContext(), permissions)) {
                    if (getOwnerActivity() != null)
                        StaticUtils.requestPermissions(getOwnerActivity(), permissions);
                    else if (getContext() instanceof Activity)
                        StaticUtils.requestPermissions((Activity) getContext(), permissions);
                    else
                        Toast.makeText(getContext(), R.string.msg_missing_storage_permission, Toast.LENGTH_SHORT).show();
                } else {
                    new IconCreatorDialog(getContext(), styles.get(0).getSize(), (String[]) PreferenceData.ICON_ICON_STYLE_NAMES.getSpecificValue(getContext(), icon.getIdentifierArgs()), icon.getIconNames()).setListener(new IconCreatorDialog.OnIconStyleListener() {
                        @Override
                        public void onIconStyle(IconStyleData style) {
                            icon.addIconStyle(style);
                            styles = icon.getIconStyles();

                            adapter = new IconStyleAdapter(getContext(), icon, styles, IconDialog.this);
                            adapter.setIconStyle(style);
                            setPreference(style);
                            recycler.setAdapter(adapter);
                        }
                    }).show();
                }
            }
        });

        findViewById(R.id.cancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                cancel();
            }
        });

        findViewById(R.id.confirm).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                confirm();
            }
        });
    }

    @Override
    public void setTitle(CharSequence title) {
        this.title = title.toString();
    }

    @Override
    public void setTitle(int titleId) {
        title = getContext().getString(titleId);
    }

    @Override
    public IconStyleData getDefaultPreference() {
        if (styles.size() > 0) return styles.get(0);
        else return null;
    }

    @Override
    public void onCheckedChange(IconStyleData selected) {
        setPreference(selected);
    }
}
