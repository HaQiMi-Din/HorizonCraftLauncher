/*
 * Horizon Craft Launcher - Terracotta (陶瓦联机) VPN foreground service.
 *
 * Adapted from Fold Craft Launcher Terracotta integration (GPL-3.0).
 */
package com.horizon.launcher.terracotta;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.horizon.launcher.R;

import net.burningtnt.terracotta.TerracottaAndroidAPI;

import java.io.IOException;

/**
 * Owns the {@link VpnService} tunnel that EasyTier / Terracotta uses to build
 * the P2P virtual network. Also keeps a foreground notification so the system
 * does not kill the tunnel while a room is active.
 */
@SuppressLint("VpnServicePolicy")
public class TerracottaVPNService extends VpnService {

    private static final String TAG = "TerracottaVPNService";
    private static final String CHANNEL_ID = "terracotta_vpn_channel";
    private static final int VPN_NOTIFICATION_ID = 1;

    public static final String ACTION_START = "com.horizon.launcher.terracotta.action.START";
    public static final String ACTION_STOP = "com.horizon.launcher.terracotta.action.STOP";
    public static final String ACTION_REPOST = "com.horizon.launcher.terracotta.action.REPOST";
    public static final String ACTION_UPDATE_STATE = "com.horizon.launcher.terracotta.action.UPDATE_STATE";
    private static final String EXTRA_FROM_DELETE = "from_delete";
    public static final String EXTRA_STATE_TEXT = "terracotta_state_text";

    private NotificationManager notificationManager;
    private String currentStateText = null;
    private boolean isStopping = false;
    private ParcelFileDescriptor vpnInterface;
    private static boolean running = false;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        running = true;
        String action = intent != null ? intent.getAction() : null;
        if (notificationManager == null) {
            notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        }
        if (ACTION_STOP.equals(action)) {
            isStopping = true;
            cleanup();
            stopForeground(true);
            stopSelf();
            return Service.START_NOT_STICKY;
        }
        if (ACTION_UPDATE_STATE.equals(action)) {
            if (intent.hasExtra(EXTRA_STATE_TEXT)) {
                currentStateText = intent.getStringExtra(EXTRA_STATE_TEXT);
            }
            if (!isStopping) {
                Notification n = buildVpnNotification();
                if (n != null) notificationManager.notify(VPN_NOTIFICATION_ID, n);
            }
            return Service.START_STICKY;
        }
        boolean fromDelete = intent != null && intent.getBooleanExtra(EXTRA_FROM_DELETE, false);
        if (ACTION_REPOST.equals(action) && fromDelete && !isStopping) {
            if (intent.hasExtra(EXTRA_STATE_TEXT)) {
                currentStateText = intent.getStringExtra(EXTRA_STATE_TEXT);
            }
            Notification notification = buildVpnNotification();
            if (notification == null) return Service.START_NOT_STICKY;
            startForeground(VPN_NOTIFICATION_ID, notification);
            return Service.START_STICKY;
        }
        isStopping = false;
        createNotificationChannelIfNeeded();
        Notification notification = buildVpnNotification();
        if (notification == null) return Service.START_NOT_STICKY;
        startForeground(VPN_NOTIFICATION_ID, notification);
        Builder vpnBuilder = new Builder().setSession("Terracotta Connection");
        try {
            vpnBuilder.addDisallowedApplication(getPackageName());
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        TerracottaAndroidAPI.VpnServiceRequest request = TerracottaAndroidAPI.getPendingVpnServiceRequest();
        vpnInterface = request.startVpnService(vpnBuilder);
        return Service.START_STICKY;
    }

    @Override
    public void onRevoke() {
        Log.w(TAG, "onRevoke(): preempted by another VPN or revoked by user; tearing down.");
        isStopping = true;
        Terracotta.setWaiting(this, false);
        cleanup();
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        isStopping = true;
        Terracotta.setWaiting(this, false);
        cleanup();
        super.onDestroy();
    }

    private void createNotificationChannelIfNeeded() {
        if (notificationManager == null) return;
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                getString(R.string.terracotta_notification_channel),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setShowBadge(false);
        notificationManager.createNotificationChannel(channel);
    }

    private Notification buildVpnNotification() {
        Terracotta.TerracottaMode mode = Terracotta.getMode();
        if (mode == null) return null;
        String title = getString(R.string.terracotta_notification_title);
        String modeText = mode == Terracotta.TerracottaMode.HOST
                ? getString(R.string.terracotta_player_kind_host)
                : getString(R.string.terracotta_player_kind_guest);
        if (currentStateText == null) {
            TerracottaState state = Terracotta.getState();
            if (state != null && !(state instanceof TerracottaState.Waiting)) {
                currentStateText = Terracotta.describeState(this, state);
            }
        }
        String contentText = String.format(getString(R.string.terracotta_notification_desc),
                modeText, currentStateText == null ? "" : currentStateText);
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID);
        Intent deleteIntent = new Intent(this, TerracottaVPNService.class)
                .setAction(ACTION_REPOST)
                .putExtra(EXTRA_FROM_DELETE, true)
                .putExtra(EXTRA_STATE_TEXT, currentStateText);
        PendingIntent deletePendingIntent = PendingIntent.getService(
                this,
                0x5443, // "TC"
                deleteIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        builder.setSmallIcon(R.drawable.ic_multiplayer)
                .setContentTitle(title)
                .setContentText(contentText)
                .setWhen(System.currentTimeMillis())
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setDeleteIntent(deletePendingIntent);
        return builder.build();
    }

    private void cleanup() {
        if (notificationManager != null) {
            notificationManager.cancel(VPN_NOTIFICATION_ID);
        }
        if (vpnInterface != null) {
            try {
                vpnInterface.close();
            } catch (IOException ignored) {
            }
            vpnInterface = null;
        }
        running = false;
    }
}
