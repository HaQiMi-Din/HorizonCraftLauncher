package com.horizon.launcher;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;
import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentContainerView;
import androidx.fragment.app.FragmentManager;
import com.kdt.mcgui.ProgressLayout;
import com.horizon.launcher.contracts.OpenDocumentWithExtension;
import com.horizon.launcher.extra.ExtraConstants;
import com.horizon.launcher.extra.ExtraCore;
import com.horizon.launcher.extra.ExtraListener;
import com.horizon.launcher.fragments.AccountFragment;
import com.horizon.launcher.fragments.DownloadsFragment;
import com.horizon.launcher.fragments.LaunchFragment;
import com.horizon.launcher.fragments.MicrosoftLoginFragment;
import com.horizon.launcher.fragments.ModpacksFragment;
import com.horizon.launcher.fragments.SelectAuthFragment;
import com.horizon.launcher.fragments.SettingsFragment;
import com.horizon.launcher.fragments.TerracottaFragment;
import com.horizon.launcher.fragments.VersionsFragment;
import com.horizon.launcher.PojavProfile;
import com.horizon.launcher.lifecycle.ContextAwareDoneListener;
import com.horizon.launcher.lifecycle.ContextExecutor;
import com.horizon.launcher.modloaders.modpacks.ModloaderInstallTracker;
import com.horizon.launcher.modloaders.modpacks.imagecache.IconCacheJanitor;
import com.horizon.launcher.prefs.LauncherPreferences;
import com.horizon.launcher.prefs.screens.LauncherPreferenceFragment;
import com.horizon.launcher.progresskeeper.ProgressKeeper;
import com.horizon.launcher.progresskeeper.TaskCountListener;
import com.horizon.launcher.services.ProgressServiceKeeper;
import com.horizon.launcher.tasks.AsyncMinecraftDownloader;
import com.horizon.launcher.tasks.AsyncVersionList;
import com.horizon.launcher.tasks.MinecraftDownloader;
import com.horizon.launcher.utils.DateUtils;
import com.horizon.launcher.utils.NotificationUtils;
import com.horizon.launcher.value.MinecraftAccount;
import com.horizon.launcher.value.launcherprofiles.LauncherProfiles;
import com.horizon.launcher.value.launcherprofiles.MinecraftProfile;
import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
public class LauncherActivity extends BaseActivity {
    public static final String SETTING_FRAGMENT_TAG = "SETTINGS_FRAGMENT";
    public final ActivityResultLauncher<Object> modInstallerLauncher =
            registerForActivityResult(new OpenDocumentWithExtension("jar"), (data)->{
                if(data != null) Tools.launchModInstaller(this, data);
            });
    private FragmentContainerView mFragmentView;
    private TextView mClockView;
    private ProgressLayout mProgressLayout;
    private ProgressServiceKeeper mProgressServiceKeeper;
    private ModloaderInstallTracker mInstallTracker;
    private NotificationManager mNotificationManager;

    /* Dock navigation items: launch / versions / downloads / modpacks / settings / account */
    private final TextView[] mDockItems = new TextView[7];
    private static final int DOCK_LAUNCH = 0;
    private static final int DOCK_VERSIONS = 1;
    private static final int DOCK_DOWNLOADS = 2;
    private static final int DOCK_MODPACKS = 3;
    private static final int DOCK_TERRACOTTA = 4;
    private static final int DOCK_SETTINGS = 5;
    private static final int DOCK_ACCOUNT = 6;

    /* Clock ticker for the GNOME-style top dock */
    private final Handler mClockHandler = new Handler(Looper.getMainLooper());
    private final Runnable mClockRunnable = new Runnable() {
        @Override public void run() {
            updateClock();
            mClockHandler.postDelayed(this, 1000);
        }
    };

    /* Keep the dock highlight in sync with the visible fragment */
    private final FragmentManager.FragmentLifecycleCallbacks mFragmentCallbackListener = new FragmentManager.FragmentLifecycleCallbacks() {
        @Override
        public void onFragmentResumed(@NonNull FragmentManager fm, @NonNull Fragment f) {
            syncDockFromFragment(f);
        }
    };

    /* Listener for the back button in settings */
    private final ExtraListener<String> mBackPreferenceListener = (key, value) -> {
        if(value.equals("true")) onBackPressed();
        return false;
    };

    /* Listener for the auth method selection screen */
    private final ExtraListener<Boolean> mSelectAuthMethod = (key, value) -> {
        Fragment fragment = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        if(fragment instanceof LaunchFragment || fragment instanceof AccountFragment) {
            Tools.swapFragment(this, SelectAuthFragment.class, SelectAuthFragment.TAG, null);
        }
        return false;
    };

    /* Right-side settings button also opens the settings dock page */
    private final View.OnClickListener mSettingButtonListener = v -> openDockPage(DOCK_SETTINGS);

    private final ExtraListener<Boolean> mLaunchGameListener = (key, value) -> {
        if(mProgressLayout.hasProcesses()){
            Toast.makeText(this, R.string.tasks_ongoing, Toast.LENGTH_LONG).show();
            return false;
        }
        String selectedProfile = LauncherPreferences.DEFAULT_PREF.getString(LauncherPreferences.PREF_KEY_CURRENT_PROFILE,"");
        if (LauncherProfiles.mainProfileJson == null || !LauncherProfiles.mainProfileJson.profiles.containsKey(selectedProfile)){
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }
        MinecraftProfile prof = LauncherProfiles.mainProfileJson.profiles.get(selectedProfile);
        if (prof == null || prof.lastVersionId == null || "Unknown".equals(prof.lastVersionId)){
            Toast.makeText(this, R.string.error_no_version, Toast.LENGTH_LONG).show();
            return false;
        }
        if(PojavProfile.getCurrentProfileContent(this, null) == null){
            Toast.makeText(this, R.string.no_saved_accounts, Toast.LENGTH_LONG).show();
            ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true);
            return false;
        }
        String normalizedVersionId = AsyncMinecraftDownloader.normalizeVersionId(prof.lastVersionId);
        JMinecraftVersionList.Version mcVersion = AsyncMinecraftDownloader.getListedVersion(normalizedVersionId);
        // Do not load when is a modded version or older than minecraft 1.3 on demo account
        MinecraftAccount currentAccount = PojavProfile.getCurrentProfileContent(this, null);
        if (currentAccount != null && currentAccount.isDemo()) {
            boolean isOlderThan13 = true;
            if (mcVersion != null) {
                try {
                    isOlderThan13 = DateUtils.dateBefore(DateUtils.parseReleaseDate(mcVersion.releaseTime), 2012, 6, 22);
                } catch (ParseException ignored) {}
            }
            if (isOlderThan13) {
                Toast.makeText(this, R.string.toast_not_available_demo, Toast.LENGTH_LONG).show();
                return false;
            }
        }
        new MinecraftDownloader().start(
                this,
                mcVersion,
                normalizedVersionId,
                new ContextAwareDoneListener(this, normalizedVersionId)
        );
        return false;
    };
    private final TaskCountListener mDoubleLaunchPreventionListener = taskCount -> {
        // Hide the notification that starts the game if there are tasks executing.
        // Prevents the user from trying to launch the game with tasks ongoing.
        if(taskCount > 0) {
            Tools.runOnUiThread(() ->
                    mNotificationManager.cancel(NotificationUtils.NOTIFICATION_ID_GAME_START)
            );
        }
    };
    private ActivityResultLauncher<String> mRequestNotificationPermissionLauncher;
    private WeakReference<Runnable> mRequestNotificationPermissionRunnable;
    @Override
    protected boolean shouldIgnoreNotch() {
        return getResources().getConfiguration().orientation == ORIENTATION_PORTRAIT;
    }
    @Override
    public boolean setFullscreen() {
        return false;
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pojav_launcher);
        FragmentManager fragmentManager = getSupportFragmentManager();
        // If we don't have a back stack root yet...
        if(fragmentManager.getBackStackEntryCount() < 1) {
            fragmentManager.beginTransaction()
                    .setReorderingAllowed(true)
                    .addToBackStack("ROOT")
                    .add(R.id.container_fragment, LaunchFragment.class, null, "ROOT").commit();
        }
        IconCacheJanitor.runJanitor();
        mRequestNotificationPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isAllowed -> {
                    if(!isAllowed) handleNoNotificationPermission();
                    else {
                        Runnable runnable = Tools.getWeakReference(mRequestNotificationPermissionRunnable);
                        if(runnable != null) runnable.run();
                    }
                }
        );
        getWindow().setBackgroundDrawable(null);
        bindViews();
        bindDock();
        checkNotificationPermission();
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        ProgressKeeper.addTaskCountListener(mDoubleLaunchPreventionListener);
        ProgressKeeper.addTaskCountListener((mProgressServiceKeeper = new ProgressServiceKeeper(this)));
        ProgressKeeper.addTaskCountListener(mProgressLayout);
        ExtraCore.addExtraListener(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);
        new AsyncVersionList().getVersionList(versions -> ExtraCore.setValue(ExtraConstants.RELEASE_TABLE, versions), false);
        mInstallTracker = new ModloaderInstallTracker(this);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_MINECRAFT);
        mProgressLayout.observe(ProgressLayout.UNPACK_RUNTIME);
        mProgressLayout.observe(ProgressLayout.INSTALL_MODPACK);
        mProgressLayout.observe(ProgressLayout.AUTHENTICATE_MICROSOFT);
        mProgressLayout.observe(ProgressLayout.DOWNLOAD_VERSION_LIST);
    }
    @Override
    protected void onStart() {
        super.onStart();
        getSupportFragmentManager().registerFragmentLifecycleCallbacks(mFragmentCallbackListener, true);
        mClockHandler.post(mClockRunnable);
    }
    @Override
    protected void onStop() {
        super.onStop();
        mClockHandler.removeCallbacks(mClockRunnable);
    }
    @Override
    protected void onResume() {
        super.onResume();
        ContextExecutor.setActivity(this);
        mInstallTracker.attach();
    }
    @Override
    protected void onPause() {
        super.onPause();
        ContextExecutor.clearActivity();
        mInstallTracker.detach();
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mProgressLayout.cleanUpObservers();
        ProgressKeeper.removeTaskCountListener(mProgressLayout);
        ProgressKeeper.removeTaskCountListener(mProgressServiceKeeper);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.BACK_PREFERENCE, mBackPreferenceListener);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.SELECT_AUTH_METHOD, mSelectAuthMethod);
        ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, mLaunchGameListener);
        getSupportFragmentManager().unregisterFragmentLifecycleCallbacks(mFragmentCallbackListener);
    }
    /** Custom implementation to feel more natural when a backstack isn't present */
    @Override
    public void onBackPressed() {
        MicrosoftLoginFragment fragment = (MicrosoftLoginFragment) getVisibleFragment(MicrosoftLoginFragment.TAG);
        if(fragment != null){
            if(fragment.canGoBack()){
                fragment.goBack();
                return;
            }
        }
        // Check if we are at the root then
        if(getVisibleFragment("ROOT") != null){
            finish();
        }
        super.onBackPressed();
    }
    @Override
    public void onAttachedToWindow() {
        LauncherPreferences.computeNotchSize(this);
    }
    @SuppressWarnings("SameParameterValue")
    private Fragment getVisibleFragment(String tag){
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        if(fragment != null && fragment.isVisible()) {
            return fragment;
        }
        return null;
    }
    private void checkNotificationPermission() {
        if(LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK ||
            checkForNotificationPermission()) {
            return;
        }
        if(ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.POST_NOTIFICATIONS)) {
            showNotificationPermissionReasoning();
            return;
        }
        askForNotificationPermission(null);
    }
    private void showNotificationPermissionReasoning() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.notification_permission_dialog_title)
                .setMessage(R.string.notification_permission_dialog_text)
                .setPositiveButton(android.R.string.ok, (d, w) -> askForNotificationPermission(null))
                .setNegativeButton(android.R.string.cancel, (d, w)-> handleNoNotificationPermission())
                .show();
    }
    private void handleNoNotificationPermission() {
        LauncherPreferences.PREF_SKIP_NOTIFICATION_PERMISSION_CHECK = true;
        LauncherPreferences.DEFAULT_PREF.edit()
                .putBoolean(LauncherPreferences.PREF_KEY_SKIP_NOTIFICATION_CHECK, true)
                .apply();
        Toast.makeText(this, R.string.notification_permission_toast, Toast.LENGTH_LONG).show();
    }
    public boolean checkForNotificationPermission() {
        return Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_DENIED;
    }
    public void askForNotificationPermission(Runnable onSuccessRunnable) {
        if(Build.VERSION.SDK_INT < 33) return;
        if(onSuccessRunnable != null) {
            mRequestNotificationPermissionRunnable = new WeakReference<>(onSuccessRunnable);
        }
        mRequestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
    /** Stuff all the view boilerplate here */
    private void bindViews(){
        mFragmentView = findViewById(R.id.container_fragment);
        mProgressLayout = findViewById(R.id.progress_layout);
        mClockView = findViewById(R.id.dock_clock);
    }

    /** Wire the GNOME-style top dock navigation. */
    private void bindDock(){
        mDockItems[DOCK_LAUNCH] = findViewById(R.id.dock_launch);
        mDockItems[DOCK_VERSIONS] = findViewById(R.id.dock_versions);
        mDockItems[DOCK_DOWNLOADS] = findViewById(R.id.dock_downloads);
        mDockItems[DOCK_MODPACKS] = findViewById(R.id.dock_modpacks);
        mDockItems[DOCK_TERRACOTTA] = findViewById(R.id.dock_terracotta);
        mDockItems[DOCK_SETTINGS] = findViewById(R.id.dock_settings);
        mDockItems[DOCK_ACCOUNT] = findViewById(R.id.dock_account);

        mDockItems[DOCK_LAUNCH].setOnClickListener(v -> {
            Tools.backToMainMenu(this);
            setDockSelection(DOCK_LAUNCH);
        });
        mDockItems[DOCK_VERSIONS].setOnClickListener(v -> openDockPage(DOCK_VERSIONS));
        mDockItems[DOCK_DOWNLOADS].setOnClickListener(v -> openDockPage(DOCK_DOWNLOADS));
        mDockItems[DOCK_MODPACKS].setOnClickListener(v -> openDockPage(DOCK_MODPACKS));
        mDockItems[DOCK_TERRACOTTA].setOnClickListener(v -> openDockPage(DOCK_TERRACOTTA));
        mDockItems[DOCK_SETTINGS].setOnClickListener(v -> openDockPage(DOCK_SETTINGS));
        mDockItems[DOCK_ACCOUNT].setOnClickListener(v -> openDockPage(DOCK_ACCOUNT));

        // Dock label toggle ("icons only" mode)
        boolean showLabels = UiTheme.showDockLabels();
        for (TextView item : mDockItems) {
            if (!showLabels) item.setText("");
        }
        setDockSelection(DOCK_LAUNCH);
    }

    /** Open a dock page, replacing the current content fragment. */
    private void openDockPage(int dockIndex){
        Fragment current = getSupportFragmentManager().findFragmentById(mFragmentView.getId());
        Class<? extends Fragment> target;
        switch (dockIndex) {
            case DOCK_VERSIONS: target = VersionsFragment.class; break;
            case DOCK_DOWNLOADS: target = DownloadsFragment.class; break;
            case DOCK_MODPACKS: target = ModpacksFragment.class; break;
            case DOCK_TERRACOTTA: target = TerracottaFragment.class; break;
            case DOCK_SETTINGS: target = SettingsFragment.class; break;
            case DOCK_ACCOUNT: target = AccountFragment.class; break;
            default: return;
        }
        if (current != null && current.getClass() == target) {
            setDockSelection(dockIndex);
            return;
        }
        Tools.swapFragment(this, target, target.getName(), null);
        setDockSelection(dockIndex);
    }

    private void setDockSelection(int index){
        for (int i = 0; i < mDockItems.length; i++) {
            if (mDockItems[i] != null) UiTheme.styleDockButton(mDockItems[i], i == index);
        }
    }

    /** Map the resumed fragment back to a dock highlight. */
    private void syncDockFromFragment(Fragment f){
        int index = -1;
        if (f instanceof LaunchFragment) index = DOCK_LAUNCH;
        else if (f instanceof VersionsFragment) index = DOCK_VERSIONS;
        else if (f instanceof DownloadsFragment) index = DOCK_DOWNLOADS;
        else if (f instanceof ModpacksFragment) index = DOCK_MODPACKS;
        else if (f instanceof TerracottaFragment) index = DOCK_TERRACOTTA;
        else if (f instanceof SettingsFragment) index = DOCK_SETTINGS;
        else if (f instanceof AccountFragment) index = DOCK_ACCOUNT;
        if (index >= 0) setDockSelection(index);
    }

    private void updateClock(){
        if (mClockView == null) return;
        mClockView.setVisibility(UiTheme.showClock() ? View.VISIBLE : View.GONE);
        if (UiTheme.showClock()) {
            mClockView.setText(DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date()));
        }
    }
}
