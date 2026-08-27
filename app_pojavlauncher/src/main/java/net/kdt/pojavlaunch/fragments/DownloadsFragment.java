package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.JMinecraftVersionList;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.tasks.AsyncVersionList;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** PCL2-style download page: lists downloadable Minecraft versions, tap to install + launch. */
public class DownloadsFragment extends Fragment {

    public static final String TAG = "DownloadsFragment";
    private static final int FILTER_ALL = 0;
    private static final int FILTER_RELEASE = 1;
    private static final int FILTER_SNAPSHOT = 2;

    private LinearLayout mList;
    private List<JMinecraftVersionList.Version> mVersions = new ArrayList<>();
    private int mFilter = FILTER_ALL;
    private final Button[] mFilterButtons = new Button[3];

    public DownloadsFragment() {
        super(R.layout.fragment_downloads);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.download_list);
        mFilterButtons[0] = view.findViewById(R.id.filter_all);
        mFilterButtons[1] = view.findViewById(R.id.filter_release);
        mFilterButtons[2] = view.findViewById(R.id.filter_snapshot);

        mFilterButtons[0].setOnClickListener(v -> setFilter(FILTER_ALL));
        mFilterButtons[1].setOnClickListener(v -> setFilter(FILTER_RELEASE));
        mFilterButtons[2].setOnClickListener(v -> setFilter(FILTER_SNAPSHOT));

        loadVersions();
    }

    private void loadVersions() {
        new AsyncVersionList().getVersionList(versionList -> {
            if (!isAdded()) return;
            if (versionList == null || versionList.versions == null) {
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;
                    TextView empty = new TextView(requireContext());
                    empty.setText(R.string.ui_downloads_none);
                    empty.setTextColor(getResources().getColor(R.color.ui_text_secondary));
                    empty.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
                    mList.addView(empty);
                });
                return;
            }
            mVersions.clear();
            for (JMinecraftVersionList.Version v : versionList.versions) {
                if (v.id == null) continue;
                if (v.id.startsWith("latest-")) continue;
                mVersions.add(v);
            }
            requireActivity().runOnUiThread(() -> {
                if (!isAdded()) return;
                refresh();
            });
        }, false);
    }

    private void setFilter(int filter) {
        mFilter = filter;
        for (int i = 0; i < 3; i++) {
            if (mFilterButtons[i] != null) {
                mFilterButtons[i].setBackgroundResource(i == filter
                        ? R.drawable.btn_primary : R.drawable.btn_secondary);
            }
        }
        refresh();
    }

    private void refresh() {
        mList.removeAllViews();
        for (JMinecraftVersionList.Version v : mVersions) {
            String type = v.type == null ? "" : v.type;
            if (mFilter == FILTER_RELEASE && !"release".equals(type)) continue;
            if (mFilter == FILTER_SNAPSHOT && !"snapshot".equals(type)) continue;
            addRow(v);
        }
    }

    private void addRow(JMinecraftVersionList.Version v) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_download_row, mList, false);
        TextView title = row.findViewById(R.id.dl_title);
        TextView subtitle = row.findViewById(R.id.dl_subtitle);
        Button button = row.findViewById(R.id.dl_button);

        title.setText(v.id);
        String sub = (v.type == null ? "" : v.type);
        if (v.releaseTime != null && v.releaseTime.length() >= 10) {
            sub += "  ·  " + v.releaseTime.substring(0, 10);
        }
        subtitle.setText(sub);

        boolean installed = new File(Tools.DIR_HOME_VERSION, v.id).exists();
        if (installed) {
            button.setText(R.string.ui_downloads_installed);
            button.setBackgroundResource(R.drawable.btn_secondary);
            button.setOnClickListener(null);
        } else {
            button.setText(R.string.ui_downloads_download);
            button.setOnClickListener(v1 -> downloadVersion(v));
        }
        mList.addView(row);
    }

    private void downloadVersion(JMinecraftVersionList.Version v) {
        LauncherProfiles.load();
        MinecraftProfile profile = MinecraftProfile.createTemplate();
        profile.name = v.id;
        profile.lastVersionId = v.id;
        String key = LauncherProfiles.getFreeProfileKey();
        LauncherProfiles.mainProfileJson.profiles.put(key, profile);
        LauncherProfiles.write();
        LauncherPreferences.DEFAULT_PREF.edit()
                .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
        Toast.makeText(requireContext(),
                getString(R.string.ui_downloads_downloading, v.id), Toast.LENGTH_LONG).show();
        // The launch listener downloads the version if needed, then boots the game.
        ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
    }
}
