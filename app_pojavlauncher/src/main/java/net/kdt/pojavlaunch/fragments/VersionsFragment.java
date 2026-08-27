package net.kdt.pojavlaunch.fragments;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.prefs.LauncherPreferences;
import net.kdt.pojavlaunch.value.launcherprofiles.LauncherProfiles;
import net.kdt.pojavlaunch.value.launcherprofiles.MinecraftProfile;

import java.util.Map;

/** PCL2-style installed version (profile) management page. */
public class VersionsFragment extends Fragment {

    public static final String TAG = "VersionsFragment";
    private LinearLayout mList;

    public VersionsFragment() {
        super(R.layout.fragment_versions);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.version_list);

        Button newButton = view.findViewById(R.id.versions_new_button);
        newButton.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                        ProfileTypeSelectFragment.TAG, null));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        mList.removeAllViews();
        LauncherProfiles.load();
        Map<String, MinecraftProfile> profiles = LauncherProfiles.mainProfileJson.profiles;
        if (profiles == null || profiles.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.ui_versions_empty);
            empty.setTextColor(getResources().getColor(R.color.ui_text_secondary));
            empty.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
            mList.addView(empty);
            return;
        }

        String current = LauncherPreferences.DEFAULT_PREF.getString(
                LauncherPreferences.PREF_KEY_CURRENT_PROFILE, "");
        for (Map.Entry<String, MinecraftProfile> entry : profiles.entrySet()) {
            addRow(entry.getKey(), entry.getValue(), entry.getKey().equals(current));
        }
    }

    private void addRow(String key, MinecraftProfile profile, boolean isCurrent) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_version_row, mList, false);
        TextView title = row.findViewById(R.id.row_title);
        TextView subtitle = row.findViewById(R.id.row_subtitle);
        title.setText(profile.name == null ? key : profile.name);
        subtitle.setText((profile.lastVersionId == null ? "?" : profile.lastVersionId)
                + (isCurrent ? "  ·  " + getString(R.string.ui_account_current) : ""));

        Button play = row.findViewById(R.id.row_play);
        play.setOnClickListener(v -> {
            LauncherPreferences.DEFAULT_PREF.edit()
                    .putString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE, key).apply();
            ExtraCore.setValue(ExtraConstants.LAUNCH_GAME, true);
        });

        Button edit = row.findViewById(R.id.row_edit);
        edit.setOnClickListener(v ->
                Tools.swapFragment(requireActivity(), ProfileEditorFragment.class,
                        ProfileEditorFragment.TAG, null));

        Button delete = row.findViewById(R.id.row_delete);
        delete.setOnClickListener(v -> new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.ui_delete_confirm,
                        profile.name == null ? key : profile.name))
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    LauncherProfiles.mainProfileJson.profiles.remove(key);
                    LauncherProfiles.write();
                    refresh();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show());

        mList.addView(row);
    }
}
