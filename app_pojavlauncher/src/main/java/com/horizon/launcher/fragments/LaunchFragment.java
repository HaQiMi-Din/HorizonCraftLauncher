package com.horizon.launcher.fragments;

import static com.horizon.launcher.Tools.openPath;
import static com.horizon.launcher.Tools.shareLog;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.kdt.mcgui.mcVersionSpinner;

import com.horizon.launcher.PojavProfile;
import com.horizon.launcher.R;
import com.horizon.launcher.Tools;
import com.horizon.launcher.UiTheme;
import com.horizon.launcher.extra.ExtraConstants;
import com.horizon.launcher.extra.ExtraCore;
import com.horizon.launcher.prefs.LauncherPreferences;
import com.horizon.launcher.tasks.AsyncMinecraftDownloader;
import com.horizon.launcher.value.MinecraftAccount;
import com.horizon.launcher.value.launcherprofiles.LauncherProfiles;
import com.horizon.launcher.value.launcherprofiles.MinecraftProfile;

import java.io.File;

/**
 * PCL2-style launch page: version picker + big play button on the left,
 * version info and quick actions on the right.
 */
public class LaunchFragment extends Fragment {

    public static final String TAG = "LaunchFragment";

    private mcVersionSpinner mVersionSpinner;
    private TextView mAccountText;
    private TextView mRamValue;
    private SeekBar mRamSeekBar;
    private TextView mRuntimeText;
    private TextView mInfoVersion;
    private TextView mInfoType;

    public LaunchFragment() {
        super(R.layout.fragment_launch);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mVersionSpinner = view.findViewById(R.id.version_spinner);
        mAccountText = view.findViewById(R.id.launch_account);
        mRamValue = view.findViewById(R.id.ram_value);
        mRamSeekBar = view.findViewById(R.id.ram_seekbar);
        mRuntimeText = view.findViewById(R.id.launch_runtime);
        mInfoVersion = view.findViewById(R.id.info_version);
        mInfoType = view.findViewById(R.id.info_type);

        Button playButton = view.findViewById(R.id.play_button);
        playButton.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true));
        UiTheme.applyAccentTint(playButton);

        Button editProfile = view.findViewById(R.id.quick_edit_profile);
        editProfile.setOnClickListener(v -> mVersionSpinner.openProfileEditor(requireActivity()));

        Button openDir = view.findViewById(R.id.quick_open_dir);
        openDir.setOnClickListener(v -> {
            if (Tools.isDemoProfile(v.getContext())) {
                android.widget.Toast.makeText(v.getContext(), R.string.toast_not_available_demo,
                        android.widget.Toast.LENGTH_LONG).show();
                return;
            }
            openPath(v.getContext(), getCurrentProfileDirectory(), false);
        });

        Button installJar = view.findViewById(R.id.quick_install_jar);
        installJar.setOnClickListener(v -> Tools.installMod(requireActivity(), false));

        Button shareLogs = view.findViewById(R.id.quick_share_logs);
        shareLogs.setOnClickListener(v -> shareLog(requireContext()));

        // RAM slider (SeekBar has no min on old APIs, offset manually)
        int ram = LauncherPreferences.DEFAULT_PREF.getInt("allocation", 1024);
        mRamSeekBar.setProgress(Math.max(0, ram - 256));
        mRamValue.setText(ram + " MB");
        mRamSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = progress + 256;
                mRamValue.setText(value + " MB");
                if (fromUser) {
                    LauncherPreferences.DEFAULT_PREF.edit().putInt("allocation", value).apply();
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mVersionSpinner.reloadProfiles();
        refreshInfo();
    }

    private void refreshInfo() {
        // Account
        try {
            MinecraftAccount account = PojavProfile.getCurrentProfileContent(requireContext(), null);
            if (account != null) {
                mAccountText.setText(getString(R.string.ui_launch_account, account.username));
            } else {
                mAccountText.setText(R.string.ui_launch_account_none);
            }
        } catch (Exception ignored) {
            mAccountText.setText(R.string.ui_launch_account_none);
        }

        // Runtime
        String runtime = LauncherPreferences.PREF_DEFAULT_RUNTIME;
        mRuntimeText.setText(getString(R.string.ui_launch_runtime,
                runtime == null || runtime.isEmpty() ? "默认" : runtime));

        // Current profile info
        LauncherProfiles.load();
        MinecraftProfile profile = LauncherProfiles.getCurrentProfile();
        if (profile != null && profile.lastVersionId != null) {
            mInfoVersion.setText(getString(R.string.ui_launch_version, profile.lastVersionId));
            String type = "?";
            try {
                com.horizon.launcher.JMinecraftVersionList.Version v =
                        AsyncMinecraftDownloader.getListedVersion(
                                AsyncMinecraftDownloader.normalizeVersionId(profile.lastVersionId));
                if (v != null && v.type != null) type = v.type;
            } catch (Exception ignored) {}
            mInfoType.setText(getString(R.string.ui_launch_type, type));
        } else {
            mInfoVersion.setText("");
            mInfoType.setText("");
        }
    }

    private File getCurrentProfileDirectory() {
        String currentProfile = LauncherPreferences.DEFAULT_PREF.getString(
                LauncherPreferences.PREF_KEY_CURRENT_PROFILE, null);
        if (!Tools.isValidString(currentProfile)) return new File(Tools.DIR_GAME_NEW);
        LauncherProfiles.load();
        MinecraftProfile profileObject = LauncherProfiles.mainProfileJson.profiles.get(currentProfile);
        if (profileObject == null) return new File(Tools.DIR_GAME_NEW);
        return Tools.getGameDirPath(profileObject);
    }
}
