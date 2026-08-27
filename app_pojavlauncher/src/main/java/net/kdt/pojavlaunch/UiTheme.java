package net.kdt.pojavlaunch;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.DrawableCompat;

import net.kdt.pojavlaunch.prefs.LauncherPreferences;

/**
 * Central helper for the highly-customizable Material Design 3 UI:
 * theme (dark/light), accent color, dynamic color, dock layout options.
 */
public final class UiTheme {
    public static final String PREF_ACCENT = "ui_accent";
    public static final String PREF_DARK = "ui_dark";
    public static final String PREF_DOCK_LABELS = "ui_dock_labels";
    public static final String PREF_CLOCK = "ui_clock";
    public static final String PREF_DYNAMIC = "ui_dynamic_color";

    public static final String ACCENT_ORANGE = "orange";
    public static final String ACCENT_BLUE = "blue";
    public static final String ACCENT_PURPLE = "purple";
    public static final String ACCENT_GREEN = "green";
    public static final String ACCENT_PINK = "pink";
    public static final String ACCENT_TEAL = "teal";

    public static final String[] ACCENTS = {
            ACCENT_ORANGE, ACCENT_BLUE, ACCENT_PURPLE, ACCENT_GREEN, ACCENT_PINK, ACCENT_TEAL
    };
    public static final int[] ACCENT_COLORS = {
            0xFFFF8A3D, 0xFF4C8DFF, 0xFF9B7BFF, 0xFF3ECF8E, 0xFFFF6B9D, 0xFF2EC4B6
    };

    private UiTheme() {}

    public static boolean isDark() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_DARK, true);
    }

    public static String getAccentName() {
        return LauncherPreferences.DEFAULT_PREF.getString(PREF_ACCENT, ACCENT_ORANGE);
    }

    /** Resolve the currently selected accent color (custom colors win over resource default). */
    public static int getAccentColor(Context ctx) {
        String name = getAccentName();
        for (int i = 0; i < ACCENTS.length; i++) {
            if (ACCENTS[i].equals(name)) return ACCENT_COLORS[i];
        }
        return ctx.getResources().getColor(R.color.ui_accent);
    }

    /** The theme-overlay style that re-maps the MD3 role attributes to the selected accent. */
    public static int getAccentOverlayResId() {
        return getAccentOverlayResId(getAccentName());
    }

    /** Map an accent name to its per-accent theme overlay style resource. */
    public static int getAccentOverlayResId(String name) {
        if (ACCENT_BLUE.equals(name)) return R.style.ThemeOverlay_Horizon_Blue;
        if (ACCENT_PURPLE.equals(name)) return R.style.ThemeOverlay_Horizon_Purple;
        if (ACCENT_GREEN.equals(name)) return R.style.ThemeOverlay_Horizon_Green;
        if (ACCENT_PINK.equals(name)) return R.style.ThemeOverlay_Horizon_Pink;
        if (ACCENT_TEAL.equals(name)) return R.style.ThemeOverlay_Horizon_Teal;
        return R.style.ThemeOverlay_Horizon_Orange;
    }

    /** The text color that should sit on top of the given accent (onPrimary role). */
    public static int getOnAccentColor(int accent) {
        return (ColorUtils.calculateLuminance(accent) > 0.45f) ? 0xFF201A00 : 0xFFFFFFFF;
    }

    public static void setAccent(String name) {
        LauncherPreferences.DEFAULT_PREF.edit().putString(PREF_ACCENT, name).apply();
    }

    public static boolean showDockLabels() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_DOCK_LABELS, true);
    }

    public static boolean showClock() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_CLOCK, true);
    }
    /** Material You: use the wallpaper-derived dynamic color scheme on Android 12+. */
    public static boolean useDynamicColor() {
        return LauncherPreferences.DEFAULT_PREF.getBoolean(PREF_DYNAMIC, false);
    }

    /** Apply the saved dark/light preference. Must be called before activities are created. */
    public static void applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
                isDark() ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    /** Tint any view that uses an accent-filled background with the current accent color. */
    public static void applyAccentTint(View view) {
        if (view == null) return;
        view.setBackgroundTintList(ColorStateList.valueOf(getAccentColor(view.getContext())));
    }

    /** Style a dock navigation button: selected -> accent pill; unselected -> transparent. */
    public static void styleDockButton(TextView button, boolean selected) {
        Context ctx = button.getContext();
        int accent = getAccentColor(ctx);
        int onAccent = getOnAccentColor(accent);
        int secondary = ctx.getResources().getColor(R.color.ui_text_secondary);

        if (selected) {
            GradientDrawable pill = new GradientDrawable();
            pill.setColor(accent);
            pill.setCornerRadius(dp(ctx, 16));
            button.setBackground(pill);
            button.setTextColor(onAccent);
            tintCompoundDrawables(button, onAccent);
        } else {
            button.setBackground(null);
            button.setTextColor(secondary);
            tintCompoundDrawables(button, secondary);
        }
    }

    private static void tintCompoundDrawables(TextView view, int color) {
        Drawable[] drawables = view.getCompoundDrawables();
        for (int i = 0; i < drawables.length; i++) {
            if (drawables[i] != null) {
                Drawable wrapped = DrawableCompat.wrap(drawables[i]).mutate();
                DrawableCompat.setTint(wrapped, color);
                drawables[i] = wrapped;
            }
        }
        view.setCompoundDrawables(drawables[0], drawables[1], drawables[2], drawables[3]);
    }

    /** Build a circular accent dot (used by the accent color picker). */
    public static GradientDrawable makeAccentDot(Context ctx, int color, boolean selected, int strokeColor) {
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        circle.setColor(color);
        if (selected) circle.setStroke(dp(ctx, 3), strokeColor);
        return circle;
    }

    public static int dp(Context ctx, float value) {
        return Math.round(value * ctx.getResources().getDisplayMetrics().density);
    }

    /** Theme accent colors, exposed as plain ints for pickers. */
    public static int accentColorByName(String name) {
        for (int i = 0; i < ACCENTS.length; i++) {
            if (ACCENTS[i].equals(name)) return ACCENT_COLORS[i];
        }
        return ACCENT_COLORS[0];
    }
