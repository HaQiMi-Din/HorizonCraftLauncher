/*
 * Horizon Craft Launcher - Terracotta (陶瓦联机) launcher-side wrapper.
 *
 * Adapted from Fold Craft Launcher Terracotta integration (GPL-3.0).
 * Terracotta itself is AGPL-3.0 by Burning_TNT; the native backend is loaded
 * from libterracotta.so via the net.burningtnt.terracotta JNI API.
 */
package com.horizon.launcher.terracotta;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.horizon.launcher.R;

import net.burningtnt.terracotta.TerracottaAndroidAPI;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.LockSupport;

/**
 * Thin singleton over {@link TerracottaAndroidAPI} that owns the background
 * state poller, the VPN permission flow and the mode (HOST/GUEST) tracking.
 */
public final class Terracotta {

    public enum TerracottaMode { HOST, GUEST }

    public interface StateListener {
        void onStateChanged(TerracottaState state);
    }

    /** Implemented by the UI that can ask the user for the VPN permission dialog. */
    public interface VpnPermissionCallback {
        void requestVpnPermission();
    }

    private static volatile boolean initialized = false;
    private static TerracottaAndroidAPI.Metadata metadata = null;
    private static volatile TerracottaMode mode = null;
    private static volatile TerracottaState currentState = null;
    private static volatile VpnPermissionCallback vpnCallback = null;

    private static final CopyOnWriteArrayList<StateListener> LISTENERS = new CopyOnWriteArrayList<>();
    private static final AtomicReference<TerracottaState> STATE = new AtomicReference<>(null);

    private Terracotta() {}

    @Nullable
    public static TerracottaMode getMode() {
        return mode;
    }

    @Nullable
    public static TerracottaState getState() {
        return currentState;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void addStateListener(StateListener l) {
        LISTENERS.addIfAbsent(l);
        TerracottaState s = currentState;
        if (s != null) {
            l.onStateChanged(s);
        }
    }

    public static void removeStateListener(StateListener l) {
        LISTENERS.remove(l);
    }

    private static volatile java.lang.ref.WeakReference<Activity> activityRef = new java.lang.ref.WeakReference<>(null);

    /** Initialize the native backend. Must be called once, on the main thread. */
    public static synchronized void initialize(Activity activity, VpnPermissionCallback cb) {
        if (initialized) return;
        activityRef = new java.lang.ref.WeakReference<>(activity);
        vpnCallback = cb;
        try {
            metadata = TerracottaAndroidAPI.initialize(activity.getApplicationContext(),
                    () -> {
                        Activity a = activityRef.get();
                        if (a != null) {
                            a.runOnUiThread(() -> {
                                if (vpnCallback != null) vpnCallback.requestVpnPermission();
                            });
                        }
                    });
        } catch (Exception e) {
            metadata = null;
            return;
        }
        Thread daemon = new Thread(() -> {
            int lastIndex = -1;
            while (true) {
                try {
                    String json = TerracottaAndroidAPI.getState();
                    TerracottaState parsed = TerracottaState.parse(json);
                    if (parsed != null && parsed.index > lastIndex) {
                        lastIndex = parsed.index;
                        currentState = parsed;
                        STATE.set(parsed);
                        Activity a = activityRef.get();
                        if (a != null) {
                            a.runOnUiThread(() -> {
                                for (StateListener l : LISTENERS) l.onStateChanged(parsed);
                            });
                        }
                    }
                } catch (Throwable ignored) {
                    // backend not ready yet or shutting down
                }
                LockSupport.parkNanos(500_000L);
            }
        }, "Terracotta Background Daemon");
        daemon.setDaemon(true);
        daemon.start();
        initialized = true;
    }

    /** Go back to the idle state. If {@code manual}, also tears the VPN tunnel down. */
    public static void setWaiting(Context context, boolean manual) {
        if (!initialized) return;
        if (manual) stopVpnService(context);
        try {
            TerracottaAndroidAPI.setWaiting();
        } catch (Throwable ignored) {
        }
        mode = null;
    }

    /** Host a room. {@code room} may be a requested room code (PCL2CE style) or null. */
    public static void setScanning(@Nullable String room, @Nullable String player,
                                   @Nullable List<String> extraNodes) throws IllegalStateException {
        requireInitialized();
        mode = TerracottaMode.HOST;
        TerracottaAndroidAPI.setScanning(room, player, extraNodes);
    }

    /** Join a room as guest. Returns false if the room code is invalid. */
    public static boolean setGuesting(String room, @Nullable String player,
                                      @Nullable List<String> extraNodes) throws IllegalStateException {
        requireInitialized();
        mode = TerracottaMode.GUEST;
        return TerracottaAndroidAPI.setGuesting(room, player, extraNodes);
    }

    @Nullable
    public static TerracottaAndroidAPI.RoomType parseRoomCode(String room) {
        if (!initialized || room == null) return null;
        try {
            return TerracottaAndroidAPI.parseRoomCode(room);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static TerracottaAndroidAPI.Metadata getMetadata() {
        return metadata == null ? new TerracottaAndroidAPI.Metadata("unknown", 0, "unknown") : metadata;
    }

    /** Start the VPN foreground service (called after the user grants VPN permission). */
    public static void startVpnService(Context context) {
        Intent intent = new Intent(context, TerracottaVPNService.class)
                .setAction(TerracottaVPNService.ACTION_START);
        ContextCompat.startForegroundService(context, intent);
    }

    public static void stopVpnService(Context context) {
        if (TerracottaVPNService.isRunning()) {
            Intent intent = new Intent(context, TerracottaVPNService.class)
                    .setAction(TerracottaVPNService.ACTION_STOP);
            ContextCompat.startForegroundService(context, intent);
        }
    }

    /** The user denied the VPN permission dialog; reject the pending request. */
    public static void rejectVpn(Context context) {
        try {
            TerracottaAndroidAPI.getPendingVpnServiceRequest().reject();
        } catch (Throwable ignored) {
        }
        setWaiting(context, true);
        Toast.makeText(context, R.string.terracotta_permission_vpn, Toast.LENGTH_SHORT).show();
    }

    /** Localized, human-readable description of the current state. */
    public static String describeState(Context context, TerracottaState state) {
        if (state instanceof TerracottaState.Waiting) {
            return context.getString(R.string.terracotta_status_waiting);
        } else if (state instanceof TerracottaState.HostScanning) {
            return context.getString(R.string.terracotta_status_host_scanning);
        } else if (state instanceof TerracottaState.HostStarting) {
            return context.getString(R.string.terracotta_status_host_starting);
        } else if (state instanceof TerracottaState.HostOK) {
            return context.getString(R.string.terracotta_status_host_ok);
        } else if (state instanceof TerracottaState.GuestConnecting) {
            return context.getString(R.string.terracotta_status_guest_connecting);
        } else if (state instanceof TerracottaState.GuestStarting) {
            return context.getString(R.string.terracotta_status_guest_starting);
        } else if (state instanceof TerracottaState.GuestOK) {
            return context.getString(R.string.terracotta_status_guest_ok);
        } else if (state instanceof TerracottaState.Exception) {
            TerracottaState.Exception e = (TerracottaState.Exception) state;
            return describeException(context, e.getType());
        }
        return context.getString(R.string.terracotta_status_unknown);
    }

    public static String describeException(Context context, TerracottaState.Exception.Type type) {
        switch (type) {
            case PING_HOST_FAIL:
                return context.getString(R.string.terracotta_status_exception_ping_host_fail);
            case PING_HOST_RST:
                return context.getString(R.string.terracotta_status_exception_ping_host_rst);
            case GUEST_ET_CRASH:
                return context.getString(R.string.terracotta_status_exception_guest_et_crash);
            case HOST_ET_CRASH:
                return context.getString(R.string.terracotta_status_exception_host_et_crash);
            case PING_SERVER_RST:
                return context.getString(R.string.terracotta_status_exception_ping_server_rst);
            case SCAFFOLDING_INVALID_RESPONSE:
                return context.getString(R.string.terracotta_status_exception_scaffolding);
            default:
                return type.name().toLowerCase(Locale.ROOT);
        }
    }

    private static void requireInitialized() {
        if (!initialized) {
            throw new IllegalStateException("initialize Terracotta first!");
        }
    }
}
