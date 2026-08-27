package com.horizon.launcher.fragments;

import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import com.google.android.material.materialswitch.MaterialSwitch;

import com.horizon.launcher.R;
import com.horizon.launcher.Tools;
import com.horizon.launcher.UiTheme;
import com.horizon.launcher.prefs.LauncherPreferences;
import com.horizon.launcher.prefs.screens.LauncherPreferenceFragment;

/** GNOME-settings-style customization page: appearance / layout / launch / advanced. */
public class SettingsFragment extends Fragment {

    public static final String TAG = "SettingsFragment";

    private TextView[] mSidebarItems = new TextView[4];
    private View[] mSections = new View[4];

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSidebarItems[0] = view.findViewById(R.id.side_appearance);
        mSidebarItems[1] = view.findViewById(R.id.side_layout);
        mSidebarItems[2] = view.findViewById(R.id.side_launch);
        mSidebarItems[3] = view.findViewById(R.id.side_advanced);
        mSections[0] = view.findViewById(R.id.sec_appearance);
        mSections[1] = view.findViewById(R.id.sec_layout);
        mSections[2] = view.findViewById(R.id.sec_launch);
        mSections[3] = view.findViewById(R.id.sec_advanced);

        for (int i = 0; i < 4; i++) {
            final int index = i;
            mSidebarItems[i].setOnClickListener(v -> selectSection(index));
        }
        selectSection(0);

        setupAppearance(view);
        setupLayout(view);
        setupLaunch(view);
        setupAdvanced(view);
    }

    private void selectSection(int index) {
        int accent = UiTheme.getAccentColor(requireContext());
        for (int i = 0; i < 4; i++) {
            mSections[i].setVisibility(i == index ? View.VISIBLE : View.GONE);
            if (i == index) {
                GradientDrawable pill = new GradientDrawable();
                pill.setColor(accent);
                pill.setCornerRadius(UiTheme.dp(requireContext(), 10));
                mSidebarItems[i].setBackground(pill);
                mSidebarItems[i].setTextColor(UiTheme.getOnAccentColor(accent));
            } else {
                mSidebarItems[i].setBackground(null);
                mSidebarItems[i].setTextColor(requireContext().getResources().getColor(R.color.ui_text_primary));
            }
        }
    }

    private void setupAppearance(View view) {
        Button dark = view.findViewById(R.id.theme_dark);
        Button light = view.findViewById(R.id.theme_light);
        dark.setOnClickListener(v -> setTheme(true));
        light.setOnClickListener(v -> setTheme(false));
        refreshThemeButtons(dark, light);

        buildAccentRow(view);

        MaterialSwitch dynamic = view.findViewById(R.id.toggle_dynamic);
        dynamic.setChecked(UiTheme.useDynamicColor());
        dynamic.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean(UiTheme.PREF_DYNAMIC, isChecked).apply();
            requireActivity().recreate();
        });
    }

    private void setTheme(boolean dark) {
        LauncherPreferences.DEFAULT_PREF.edit().putBoolean(UiTheme.PREF_DARK, dark).apply();
        UiTheme.applyTheme();
        requireActivity().recreate();
    }

    private void refreshThemeButtons(Button dark, Button light) {
        boolean darkMode = UiTheme.isDark();
        dark.setBackgroundResource(darkMode ? R.drawable.btn_primary : R.drawable.btn_secondary);
        light.setBackgroundResource(!darkMode ? R.drawable.btn_primary : R.drawable.btn_secondary);
        if (darkMode) UiTheme.applyAccentTint(dark); else UiTheme.applyAccentTint(light);
    }

    private void buildAccentRow(View view) {
        LinearLayout row = view.findViewById(R.id.accent_row);
        row.removeAllViews();
        String current = UiTheme.getAccentName();
        for (int i = 0; i < UiTheme.ACCENTS.length; i++) {
            final String name = UiTheme.ACCENTS[i];
            boolean selected = name.equals(current);
            int color = UiTheme.ACCENT_COLORS[i];
            int stroke = requireContext().getResources().getColor(R.color.ui_text_primary);
            ImageView dot = new ImageView(requireContext());
            int size = UiTheme.dp(requireContext(), selected ? 34 : 28);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(size, size);
            lp.setMargins(UiTheme.dp(requireContext(), 4), 0, UiTheme.dp(requireContext(), 4), 0);
            dot.setLayoutParams(lp);
            dot.setBackground(UiTheme.makeAccentDot(requireContext(), color, selected, stroke));
            dot.setOnClickListener(v -> {
                UiTheme.setAccent(name);
                requireActivity().recreate();
            });
            row.addView(dot);
        }
    }

    private void setupLayout(View view) {
        MaterialSwitch dockLabels = view.findViewById(R.id.toggle_dock_labels);
        dockLabels.setChecked(UiTheme.showDockLabels());
        dockLabels.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean(UiTheme.PREF_DOCK_LABELS, isChecked).apply();
            requireActivity().recreate();
        });

        MaterialSwitch clock = view.findViewById(R.id.toggle_clock);
        clock.setChecked(UiTheme.showClock());
        clock.setOnCheckedChangeListener((buttonView, isChecked) -> {
            LauncherPreferences.DEFAULT_PREF.edit().putBoolean(UiTheme.PREF_CLOCK, isChecked).apply();
            requireActivity().recreate();
        });
    }

    private void setupLaunch(View view) {
        TextView value = view.findViewById(R.id.settings_ram_value);
        SeekBar seekBar = view.findViewById(R.id.settings_ram_seekbar);
        int ram = LauncherPreferences.DEFAULT_PREF.getInt("allocation", 1024);
        seekBar.setProgress(Math.max(0, ram - 256));
        value.setText(ram + " MB");
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int val = progress + 256;
                value.setText(val + " MB");
                if (fromUser) LauncherPreferences.DEFAULT_PREF.edit().putInt("allocation", val).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        Button sourceDefault = view.findViewById(R.id.source_default);
        Button sourceBmclapi = view.findViewById(R.id.source_bmclapi);
        String source = LauncherPreferences.PREF_DOWNLOAD_SOURCE;
        sourceDefault.setOnClickListener(v -> setDownloadSource("default", sourceDefault, sourceBmclapi));
        sourceBmclapi.setOnClickListener(v -> setDownloadSource("bmclapi", sourceDefault, sourceBmclapi));
        refreshSourceButtons(source, sourceDefault, sourceBmclapi);
    }

    private void setDownloadSource(String value, Button def, Button bmcl) {
        LauncherPreferences.DEFAULT_PREF.edit().putString("downloadSource", value).apply();
        LauncherPreferences.PREF_DOWNLOAD_SOURCE = value;
        refreshSourceButtons(value, def, bmcl);
    }

    private void refreshSourceButtons(String source, Button def, Button bmcl) {
        def.setBackgroundResource("default".equals(source) ? R.drawable.btn_primary : R.drawable.btn_secondary);
        bmcl.setBackgroundResource("bmclapi".equals(source) ? R.drawable.btn_primary : R.drawable.btn_secondary);
        if ("default".equals(source)) UiTheme.applyAccentTint(def); else UiTheme.applyAccentTint(bmcl);
    }

    private void setupAdvanced(View view) {
        Button advanced = view.findViewById(R.id.advanced_button);
        advanced.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), LauncherPreferenceFragment.class,
                        com.horizon.launcher.LauncherActivity.SETTING_FRAGMENT_TAG, null));
    }
}
