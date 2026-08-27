package net.kdt.pojavlaunch.fragments;

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

import net.kdt.pojavlaunch.PojavProfile;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;
import net.kdt.pojavlaunch.value.MinecraftAccount;

import java.io.File;

/** PCL2-style account management page. */
public class AccountFragment extends Fragment {

    public static final String TAG = "AccountFragment";
    private LinearLayout mList;

    public AccountFragment() {
        super(R.layout.fragment_account);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mList = view.findViewById(R.id.account_list);
        Button add = view.findViewById(R.id.account_add_button);
        add.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true));
    }

    @Override
    public void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        mList.removeAllViews();
        String current = PojavProfile.getCurrentProfileName(requireContext());
        File accountFolder = new File(Tools.DIR_ACCOUNT_NEW);
        boolean any = false;
        if (accountFolder.exists()) {
            String[] files = accountFolder.list();
            if (files != null) {
                for (String fileName : files) {
                    if (!fileName.endsWith(".json")) continue;
                    String name = fileName.substring(0, fileName.length() - 5);
                    addRow(name, name.equals(current));
                    any = true;
                }
            }
        }
        if (!any) {
            TextView empty = new TextView(requireContext());
            empty.setText(R.string.ui_account_empty);
            empty.setTextColor(getResources().getColor(R.color.ui_text_secondary));
            empty.setPadding(0, (int) (16 * getResources().getDisplayMetrics().density), 0, 0);
            mList.addView(empty);
        }
    }

    private void addRow(String name, boolean isCurrent) {
        View row = LayoutInflater.from(requireContext())
                .inflate(R.layout.item_account_row, mList, false);
        TextView accName = row.findViewById(R.id.acc_name);
        TextView accType = row.findViewById(R.id.acc_type);
        TextView accCurrent = row.findViewById(R.id.acc_current);
        accName.setText(name);

        MinecraftAccount account = MinecraftAccount.load(name);
        accType.setText(account != null && account.isMicrosoft
                ? getString(R.string.ui_account_type_ms)
                : getString(R.string.ui_account_type_offline));
        accCurrent.setText(isCurrent ? getString(R.string.ui_account_current) : "");

        Button use = row.findViewById(R.id.acc_use);
        use.setOnClickListener(v -> {
            PojavProfile.setCurrentProfile(requireContext(), name);
            refresh();
        });

        Button logout = row.findViewById(R.id.acc_logout);
        logout.setOnClickListener(v -> {
            File f = new File(Tools.DIR_ACCOUNT_NEW, name + ".json");
            if (f.exists()) f.delete();
            if (isCurrent) PojavProfile.setCurrentProfile(requireContext(), null);
            refresh();
        });

        mList.addView(row);
    }
}
