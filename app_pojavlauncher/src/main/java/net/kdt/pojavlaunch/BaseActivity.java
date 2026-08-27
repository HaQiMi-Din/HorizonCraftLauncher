package net.kdt.pojavlaunch;

import android.content.*;
import android.content.res.Resources;
import android.os.*;
import androidx.appcompat.app.*;
import net.kdt.pojavlaunch.utils.*;

import static net.kdt.pojavlaunch.prefs.LauncherPreferences.PREF_IGNORE_NOTCH;

public abstract class BaseActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        // LocaleUtils.setLocale also initializes LauncherPreferences.DEFAULT_PREF,
        // so UiTheme can be safely queried right after.
        Context localized = LocaleUtils.setLocale(newBase);
        super.attachBaseContext(new AccentContextWrapper(localized));
    }

    /** Wraps the context so every Resources lookup resolves the MD3 role
     *  colors (ui_accent and friends) from the user-selected accent. */
    private static final class AccentContextWrapper extends ContextWrapper {
        private final Resources mAccentResources;

        AccentContextWrapper(Context base) {
            super(base);
            int accent = UiTheme.getAccentColor(base);
            int[] scheme = UiTheme.buildColorScheme(base, accent);
            mAccentResources = new AccentResources(base.getResources(), scheme);
        }

        @Override
        public Resources getResources() {
            return mAccentResources;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LocaleUtils.setLocale(this);
        Tools.setFullscreen(this, setFullscreen());
        Tools.updateWindowSize(this);
    }

    /** @return Whether the activity should be set as a fullscreen one */
    public boolean setFullscreen(){
        return true;
    }


    @Override
    public void startActivity(Intent i) {
        super.startActivity(i);
        //new Throwable("StartActivity").printStackTrace();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Tools.checkStorageInteractive(this);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        Tools.setFullscreen(this, setFullscreen());
        Tools.ignoreNotch(shouldIgnoreNotch(),this);
    }

    /** @return Whether or not the notch should be ignored */
    protected boolean shouldIgnoreNotch(){
        return PREF_IGNORE_NOTCH;
    }
}
