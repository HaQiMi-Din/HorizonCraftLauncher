package com.horizon.launcher.fragments;

import com.horizon.launcher.modloaders.FabriclikeUtils;
import com.horizon.launcher.modloaders.ModloaderListenerProxy;

public class FabricInstallFragment extends FabriclikeInstallFragment {

    public static final String TAG = "FabricInstallFragment";

    public FabricInstallFragment() {
        super(FabriclikeUtils.FABRIC_UTILS, TAG);
    }
}
