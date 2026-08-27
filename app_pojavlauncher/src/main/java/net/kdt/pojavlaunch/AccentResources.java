package net.kdt.pojavlaunch;

import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.Theme;
import android.util.DisplayMetrics;

import androidx.annotation.NonNull;

/**
 * A {@link Resources} whose Material Design 3 role colors follow the
 * user-selected accent color instead of the static values baked in
 * ui_colors.xml.
 *
 * <p>Layouts/drawables reference the roles through theme attributes
 * ({@code ?attr/colorPrimary} etc.); the theme maps those attributes to
 * the {@code ui_*} color resources, and resolving them calls
 * {@link #getColor(int, Theme)}, which this class intercepts to return
 * the dynamically computed tonal scheme. Everything else is delegated to
 * the base resources.</p>
 */
public final class AccentResources extends Resources {
    private final Resources mBase;
    private final int mPrimary, mOnPrimary, mPrimaryContainer, mOnPrimaryContainer,
            mSecondaryContainer, mOnSecondaryContainer;

    public AccentResources(@NonNull Resources base, @NonNull int[] scheme) {
        super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
        mBase = base;
        mPrimary = scheme[0];
        mOnPrimary = scheme[1];
        mPrimaryContainer = scheme[2];
        mOnPrimaryContainer = scheme[3];
        mSecondaryContainer = scheme[4];
        mOnSecondaryContainer = scheme[5];
    }

    @Override
    public int getColor(int id) throws NotFoundException {
        int c = resolveRole(id);
        return c != Integer.MIN_VALUE ? c : mBase.getColor(id);
    }

    @Override
    public int getColor(int id, Theme theme) throws NotFoundException {
        int c = resolveRole(id);
        return c != Integer.MIN_VALUE ? c : mBase.getColor(id, theme);
    }

    private int resolveRole(int id) {
        if (id == R.color.ui_accent) return mPrimary;
        if (id == R.color.ui_on_accent) return mOnPrimary;
        if (id == R.color.ui_primary_container) return mPrimaryContainer;
        if (id == R.color.ui_on_primary_container) return mOnPrimaryContainer;
        if (id == R.color.ui_secondary_container) return mSecondaryContainer;
        if (id == R.color.ui_on_secondary_container) return mOnSecondaryContainer;
        return Integer.MIN_VALUE;
    }

    @Override
    public Configuration getConfiguration() {
        return mBase.getConfiguration();
    }

    @Override
    public DisplayMetrics getDisplayMetrics() {
        return mBase.getDisplayMetrics();
    }

    @Override
    public AssetManager getAssets() {
        return mBase.getAssets();
    }

    @Override
    public void updateConfiguration(Configuration config, DisplayMetrics metrics) {
        super.updateConfiguration(config, metrics);
    }
}
