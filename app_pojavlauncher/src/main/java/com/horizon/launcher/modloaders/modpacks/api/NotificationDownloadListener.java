package com.horizon.launcher.modloaders.modpacks.api;

import android.content.Context;
import android.content.Intent;

import com.horizon.launcher.LauncherActivity;
import com.horizon.launcher.R;
import com.horizon.launcher.Tools;
import com.horizon.launcher.modloaders.ModloaderDownloadListener;
import com.horizon.launcher.modloaders.modpacks.ModloaderInstallTracker;
import com.horizon.launcher.utils.NotificationUtils;

import java.io.File;

public class NotificationDownloadListener implements ModloaderDownloadListener {
    private final Context mContext;
    private final ModLoader mModLoader;
    
    public NotificationDownloadListener(Context context, ModLoader modLoader) {
        mModLoader = modLoader;
        mContext = context.getApplicationContext();
    }

    @Override
    public void onDownloadFinished(File downloadedFile) {
        if(mModLoader.requiresGuiInstallation()) {
            ModloaderInstallTracker.saveModLoader(mContext, mModLoader, downloadedFile);
            Intent mainActivityIntent = new Intent(mContext, LauncherActivity.class);
            sendIntentNotification(mainActivityIntent, R.string.modpack_install_notification_success);
        }
    }

    @Override
    public void onDataNotAvailable() {
        sendEmptyNotification(R.string.modpack_install_notification_data_not_available);
    }

    @Override
    public void onDownloadError(Exception e) {
        Tools.showErrorRemote(mContext, R.string.modpack_install_modloader_download_failed, e);
    }

    private void sendIntentNotification(Intent intent, int localeString) {
        Tools.runOnUiThread(() -> NotificationUtils.sendBasicNotification(mContext,
                R.string.modpack_install_notification_title,
                localeString,
                intent,
                NotificationUtils.PENDINGINTENT_CODE_DOWNLOAD_SERVICE,
                NotificationUtils.NOTIFICATION_ID_DOWNLOAD_LISTENER
        ));
    }

    private void sendEmptyNotification(int localeString) {
        Tools.runOnUiThread(()->NotificationUtils.sendBasicNotification(mContext,
                R.string.modpack_install_notification_title,
                localeString,
                null,
                NotificationUtils.PENDINGINTENT_CODE_DOWNLOAD_SERVICE,
                NotificationUtils.NOTIFICATION_ID_DOWNLOAD_LISTENER
        ));
    }
}
