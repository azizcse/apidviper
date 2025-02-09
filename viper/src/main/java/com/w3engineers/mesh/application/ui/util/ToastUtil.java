package com.w3engineers.mesh.application.ui.util;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.w3engineers.mesh.util.lib.mesh.HandlerUtil;

/**
 * Uses: For all type of toast showing
 * Created by : Monir Zzaman.
 */
public class ToastUtil {
    public static void showLong(Context context, String txt) {
        HandlerUtil.postForeground(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, txt, Toast.LENGTH_LONG).show();
            }
        });

    }

    public static void showShort(Context context, String txt) {
        HandlerUtil.postForeground(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, txt, Toast.LENGTH_SHORT).show();
            }
        });

    }

    public static void showLog(String tag, String message){
        Log.e(tag, " :: "+message);
    }
}