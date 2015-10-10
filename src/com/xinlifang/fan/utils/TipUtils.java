package com.xinlifang.fan.utils;

import android.content.Context;
import android.view.Gravity;
import android.widget.Toast;

public class TipUtils {

	public static void showToast(Context context, String tip) {
		Toast toast = Toast.makeText(context, tip, Toast.LENGTH_SHORT);
		toast.setGravity(Gravity.CENTER, 0, 0);
		toast.show();
	}

}
