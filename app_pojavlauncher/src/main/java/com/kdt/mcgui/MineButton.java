package com.kdt.mcgui;
import android.content.*;
import android.graphics.drawable.*;
import android.util.*;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.ColorUtils;
import com.horizon.launcher.R;
import com.horizon.launcher.UiTheme;
public class MineButton extends androidx.appcompat.widget.AppCompatButton {
	
	public MineButton(Context ctx) {
		this(ctx, null);
	}
	
	public MineButton(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}
	public void init() {
		setTypeface(ResourcesCompat.getFont(getContext(), R.font.noto_sans_bold));
		setTextSize(TypedValue.COMPLEX_UNIT_PX, getResources().getDimensionPixelSize(R.dimen._13ssp));
		// Follow the user-selected MD3 accent instead of the legacy green 9-patch
		int accent = UiTheme.getAccentColor(getContext());
		int onAccent = UiTheme.getOnAccentColor(accent);
		setTextColor(onAccent);
		setBackground(buildAccentBackground(getContext(), accent));
	}

	private static Drawable buildAccentBackground(Context ctx, int accent) {
		int radius = UiTheme.dp(ctx, 12);
		GradientDrawable normal = new GradientDrawable();
		normal.setColor(accent);
		normal.setCornerRadius(radius);
		GradientDrawable pressed = new GradientDrawable();
		pressed.setColor(ColorUtils.blendARGB(accent, 0xFF000000, 0.30f));
		pressed.setCornerRadius(radius);
		GradientDrawable disabled = new GradientDrawable();
		disabled.setColor(ColorUtils.blendARGB(accent, 0xFFFFFFFF, 0.45f));
		disabled.setCornerRadius(radius);
		StateListDrawable bg = new StateListDrawable();
		bg.addState(new int[]{android.R.attr.state_pressed}, pressed);
		bg.addState(new int[]{-android.R.attr.state_enabled}, disabled);
		bg.addState(new int[]{}, normal);
		return bg;
	}
}
