package com.kdt.mcgui;

import android.content.*;
import android.graphics.*;
import android.util.*;
import android.widget.EditText;

import com.horizon.launcher.R;
import com.horizon.launcher.UiTheme;

public class MineEditText extends androidx.appcompat.widget.AppCompatEditText {
	public MineEditText(Context ctx) {
		super(ctx);
		init();
	}

	public MineEditText(Context ctx, AttributeSet attrs) {
		super(ctx, attrs);
		init();
	}

	public void init() {
		// Theme-adaptive: use the MD3 input surface instead of the legacy hardcoded
		// #131313 so text stays legible in both dark and light mode.
		setBackgroundColor(getContext().getResources().getColor(R.color.ui_input));
		setTextColor(UiTheme.getTextPrimaryColor(getContext()));
		setHintTextColor(getContext().getResources().getColor(R.color.ui_text_secondary));
		setPadding(10, 5, 10, 5);
	}
}
