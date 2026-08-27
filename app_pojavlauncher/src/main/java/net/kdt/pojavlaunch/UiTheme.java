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
        int onAccent = ctx.getResources().getColor(R.color.ui_on_accent);
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

    /** Build a full MD3 tonal scheme for the selected accent + current dark/light mode.
     *  Order: { primary, onPrimary, primaryContainer, onPrimaryContainer,
     *           secondaryContainer, onSecondaryContainer }
     *  This lets every role of the Material 3 color system follow the accent. */
    public static int[] buildColorScheme(Context ctx, int accent) {
        boolean dark = isDark();
        int primary, primaryContainer, onPrimaryContainer, secondaryContainer, onSecondaryContainer;
        if (dark) {
            primary = ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.22f);
            primaryContainer = ColorUtils.blendARGB(accent, 0xFF000000, 0.35f);
            onPrimaryContainer = ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.60f);
            secondaryContainer = ColorUtils.blendARGB(accent, 0xFF000000, 0.60f);
            onSecondaryContainer = ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.50f);
        } else {
            primary = ColorUtils.blendARGB(accent, 0xFF000000, 0.15f);
            primaryContainer = ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.70f);
            onPrimaryContainer = ColorUtils.blendARGB(accent, 0xFF000000, 0.50f);
            secondaryContainer = ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.85f);
            onSecondaryContainer = ColorUtils.blendARGB(accent, 0xFF000000, 0.55f);
        }
        int onPrimary = (ColorUtils.calculateLuminance(primary) > 0.45f) ? 0xFF201A00 : 0xFFFFFFFF;
        return new int[]{
                primary, onPrimary, primaryContainer, onPrimaryContainer,
                secondaryContainer, onSecondaryContainer
        };
    }
}
