/*
 * Horizon Craft Launcher - Terracotta (陶瓦联机) page.
 *
 * 房主：创建房间 -> 获得邀请码 (u/xxxx-xxxx-xxxx-xxxx) -> 分享给好友。
 * 房客：输入邀请码 -> 建立 P2P 连接 -> 在游戏内多人游戏中使用本地地址加入。
 */
package com.horizon.launcher.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.horizon.launcher.PojavProfile;
import com.horizon.launcher.R;
import com.horizon.launcher.Tools;
import com.horizon.launcher.terracotta.Terracotta;
import com.horizon.launcher.terracotta.TerracottaNodeList;
import com.horizon.launcher.terracotta.TerracottaState;

import net.burningtnt.terracotta.TerracottaAndroidAPI;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** PCL2-style multiplayer page backed by Terracotta (陶瓦联机). */
public class TerracottaFragment extends Fragment implements Terracotta.StateListener {

    public static final String TAG = "TerracottaFragment";

    private TextView mStatusText;
    private TextView mStatusDetail;
    private TextView mRoomCodeLabel;
    private TextView mGuestUrlLabel;
    private Button mExitButton;
    private View mHostPanel;
    private View mGuestPanel;

    private com.kdt.mcgui.MineEditText mHostName;
    private com.kdt.mcgui.MineEditText mRoomCode;
    private com.kdt.mcgui.MineEditText mGuestCode;
    private com.kdt.mcgui.MineEditText mGuestName;
    private com.kdt.mcgui.MineEditText mGuestUrl;

    private ActivityResultLauncher<Intent> mVpnPermissionLauncher;

    public TerracottaFragment() {
        super(R.layout.fragment_terracotta);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mVpnPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Terracotta.startVpnService(requireContext());
                    } else {
                        Terracotta.rejectVpn(requireContext());
                        resetUi();
                    }
                });
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mStatusText = view.findViewById(R.id.terracotta_status_text);
        mStatusDetail = view.findViewById(R.id.terracotta_status_detail);
        mRoomCodeLabel = view.findViewById(R.id.terracotta_room_code_label);
        mGuestUrlLabel = view.findViewById(R.id.terracotta_guest_url_label);
        mExitButton = view.findViewById(R.id.terracotta_exit_button);
        mHostPanel = view.findViewById(R.id.terracotta_host_panel);
        mGuestPanel = view.findViewById(R.id.terracotta_guest_panel);
        mHostName = view.findViewById(R.id.terracotta_host_name);
        mRoomCode = view.findViewById(R.id.terracotta_room_code);
        mGuestCode = view.findViewById(R.id.terracotta_guest_code);
        mGuestName = view.findViewById(R.id.terracotta_guest_name);
        mGuestUrl = view.findViewById(R.id.terracotta_guest_url);

        // Pre-fill player name with the current Minecraft account if any.
        String accountName = PojavProfile.getCurrentProfileName(requireContext());
        if (accountName == null || accountName.isEmpty()) accountName = "Player";
        mHostName.setText(accountName);
        mGuestName.setText(accountName);

        Button hostConfirm = view.findViewById(R.id.terracotta_host_confirm);
        hostConfirm.setOnClickListener(v -> onHostCreate());
        Button guestConfirm = view.findViewById(R.id.terracotta_guest_confirm);
        guestConfirm.setOnClickListener(v -> onGuestJoin());
        view.findViewById(R.id.terracotta_copy_code).setOnClickListener(v ->
                copyToClipboard(mRoomCode.getText().toString(), R.string.terracotta_toast_code_copied));
        view.findViewById(R.id.terracotta_copy_url).setOnClickListener(v ->
                copyToClipboard(mGuestUrl.getText().toString(), R.string.terracotta_toast_url_copied));
        mExitButton.setOnClickListener(v -> {
            Terracotta.setWaiting(requireContext(), true);
            resetUi();
        });

        TextView meta = view.findViewById(R.id.terracotta_meta);
        if (Terracotta.isInitialized()) {
            TerracottaAndroidAPI.Metadata md = Terracotta.getMetadata();
            meta.setText(getString(R.string.terracotta_meta_format,
                    md.getTerracottaVersion(), md.getEasyTierVersion()));
        } else {
            meta.setText(getString(R.string.terracotta_meta_unavailable));
        }

        // Start the backend (idempotent) and subscribe to state changes.
        Terracotta.initialize(requireActivity(), this::requestVpnPermission);
        Terracotta.addStateListener(this);
        updateStatus(Terracotta.getState());
        updateModeVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatus(Terracotta.getState());
        updateModeVisibility();
    }

    @Override
    public void onDestroyView() {
        Terracotta.removeStateListener(this);
        super.onDestroyView();
    }

    /* ---------------- Actions ---------------- */

    private void onHostCreate() {
        final String player = mHostName.getText().toString().trim();
        final Context ctx = requireContext();
        new Thread(() -> {
            try {
                final List<String> nodes = uriListToStrings(TerracottaNodeList.fetch());
                Terracotta.setWaiting(ctx, false);
                Terracotta.setScanning(null, player.isEmpty() ? "Player" : player, nodes);
                runOnUiThreadSafe(() -> setBusy(true, getString(R.string.terracotta_status_host_scanning)));
            } catch (Exception e) {
                runOnUiThreadSafe(() -> Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "TerracottaHost").start();
    }

    private void onGuestJoin() {
        final String code = mGuestCode.getText().toString().trim();
        final String player = mGuestName.getText().toString().trim();
        final Context ctx = requireContext();
        if (code.isEmpty()) {
            Toast.makeText(ctx, R.string.terracotta_toast_code_empty, Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            try {
                final List<String> nodes = uriListToStrings(TerracottaNodeList.fetch());
                Terracotta.setWaiting(ctx, false);
                final boolean ok = Terracotta.setGuesting(code, player.isEmpty() ? "Player" : player, nodes);
                runOnUiThreadSafe(() -> {
                    if (!ok) {
                        Toast.makeText(ctx, R.string.terracotta_toast_code_invalid, Toast.LENGTH_LONG).show();
                        setBusy(false, getString(R.string.terracotta_status_waiting));
                    } else {
                        setBusy(true, getString(R.string.terracotta_status_guest_connecting));
                    }
                });
            } catch (Exception e) {
                runOnUiThreadSafe(() -> Toast.makeText(ctx, e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }, "TerracottaGuest").start();
    }

    /** Called by the Terracotta wrapper when the native backend needs the VPN tunnel. */
    private void requestVpnPermission() {
        Intent prepare = VpnService.prepare(requireContext());
        if (prepare != null) {
            mVpnPermissionLauncher.launch(prepare);
        } else {
            Terracotta.startVpnService(requireContext());
        }
    }

    /* ---------------- State handling ---------------- */

    @Override
    public void onStateChanged(TerracottaState state) {
        if (getView() == null) return;
        updateStatus(state);
        updateModeVisibility();
    }

    private void updateStatus(@Nullable TerracottaState state) {
        if (mStatusText == null) return;
        if (state == null) {
            mStatusText.setText(R.string.terracotta_status_waiting);
            mStatusDetail.setText("");
            return;
        }
        Context ctx = requireContext();
        mStatusText.setText(Terracotta.describeState(ctx, state));
        if (state instanceof TerracottaState.HostOK) {
            TerracottaState.HostOK ok = (TerracottaState.HostOK) state;
            String code = ok.getRoomCode();
            mRoomCode.setText(code == null ? "" : code);
            mRoomCodeLabel.setVisibility(View.VISIBLE);
            mStatusDetail.setText(ctx.getString(R.string.terracotta_status_detail_host_ok));
            mExitButton.setVisibility(View.VISIBLE);
            copyToClipboard(code, R.string.terracotta_toast_code_copied);
        } else if (state instanceof TerracottaState.GuestOK) {
            TerracottaState.GuestOK ok = (TerracottaState.GuestOK) state;
            String url = ok.getUrl();
            mGuestUrl.setText(url == null ? "" : url);
            mGuestUrlLabel.setVisibility(View.VISIBLE);
            mStatusDetail.setText(ctx.getString(R.string.terracotta_status_detail_guest_ok, url == null ? "" : url));
            mExitButton.setVisibility(View.VISIBLE);
        } else if (state instanceof TerracottaState.Waiting) {
            mStatusDetail.setText("");
            mExitButton.setVisibility(View.GONE);
            setBusy(false, null);
        } else if (state instanceof TerracottaState.Exception) {
            TerracottaState.Exception e = (TerracottaState.Exception) state;
            mStatusDetail.setText(Terracotta.describeException(ctx, e.getType()));
            mExitButton.setVisibility(View.VISIBLE);
            setBusy(false, null);
        } else {
            setBusy(true, null);
            mExitButton.setVisibility(View.VISIBLE);
        }
    }

    /** Toggle between "waiting" (editable) and "in room" (locked) UI. */
    private void setBusy(boolean busy, @Nullable String statusOverride) {
        mHostPanel.setEnabled(!busy);
        mGuestPanel.setEnabled(!busy);
        if (statusOverride != null) {
            mStatusText.setText(statusOverride);
        }
    }

    private void updateModeVisibility() {
        // Both panels stay visible; the disabled state is handled by setBusy.
    }

    private void resetUi() {
        mRoomCodeLabel.setVisibility(View.GONE);
        mGuestUrlLabel.setVisibility(View.GONE);
        mExitButton.setVisibility(View.GONE);
        mStatusDetail.setText("");
        setBusy(false, getString(R.string.terracotta_status_waiting));
        mStatusText.setText(R.string.terracotta_status_waiting);
    }

    /* ---------------- Helpers ---------------- */

    private static List<String> uriListToStrings(List<URI> uris) {
        List<String> out = new ArrayList<>(uris.size());
        for (URI u : uris) out.add(u.toString());
        return out;
    }

    private void runOnUiThreadSafe(Runnable r) {
        if (getActivity() != null) getActivity().runOnUiThread(r);
        else r.run();
    }

    private void copyToClipboard(String text, int toastRes) {
        if (text == null || text.isEmpty()) return;
        ClipboardManager cm = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm != null) cm.setPrimaryClip(ClipData.newPlainText("terracotta", text));
        Toast.makeText(requireContext(), toastRes, Toast.LENGTH_SHORT).show();
    }
}
