package com.horizon.launcher.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.horizon.launcher.R;
import com.horizon.launcher.Tools;

/** PCL2-style modpack/modloader hub. Each card opens the corresponding installer. */
public class ModpacksFragment extends Fragment {

    public static final String TAG = "ModpacksFragment";
    private LinearLayout mList;

    public ModpacksFragment() {
        super(R.layout.fragment_modpacks);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.modpack_list);

        addCard(R.string.ui_modpacks_vanilla, R.string.ui_modpacks_vanilla_desc,
                v -> Tools.swapFragment(requireActivity(), ProfileTypeSelectFragment.class,
                        ProfileTypeSelectFragment.TAG, null));
        addCard(R.string.ui_modpacks_forge, R.string.ui_modpacks_forge_desc,
                v -> tryInstall(ForgeInstallFragment.class, ForgeInstallFragment.TAG));
        addCard(R.string.ui_modpacks_fabric, R.string.ui_modpacks_fabric_desc,
                v -> tryInstall(FabricInstallFragment.class, FabricInstallFragment.TAG));
        addCard(R.string.ui_modpacks_quilt, R.string.ui_modpacks_quilt_desc,
                v -> tryInstall(QuiltInstallFragment.class, QuiltInstallFragment.TAG));
        addCard(R.string.ui_modpacks_optifine, R.string.ui_modpacks_optifine_desc,
                v -> tryInstall(OptiFineInstallFragment.class, OptiFineInstallFragment.TAG));
        addCard(R.string.ui_modpacks_bta, R.string.ui_modpacks_bta_desc,
                v -> tryInstall(BTAInstallFragment.class, BTAInstallFragment.TAG));
        addCard(R.string.ui_modpacks_search, R.string.ui_modpacks_search_desc,
                v -> tryInstall(SearchModFragment.class, SearchModFragment.TAG));
        addCard(R.string.ui_modpacks_jar, R.string.ui_modpacks_jar_desc,
                v -> Tools.installMod(requireActivity(), false));
    }

    private void tryInstall(Class<? extends Fragment> fragmentClass, String tag) {
        Tools.swapFragment(requireActivity(), fragmentClass, tag, null);
    }

    private void addCard(int titleRes, int descRes, View.OnClickListener listener) {
        View card = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_modpack_card, mList, false);
        ((TextView) card.findViewById(R.id.mp_title)).setText(titleRes);
        ((TextView) card.findViewById(R.id.mp_desc)).setText(descRes);
        card.findViewById(R.id.mp_arrow).setVisibility(View.VISIBLE);
        card.setOnClickListener(listener);
        mList.addView(card);
    }
}
