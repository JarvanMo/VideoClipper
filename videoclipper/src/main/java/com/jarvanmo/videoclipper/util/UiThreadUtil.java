package com.jarvanmo.videoclipper.util;

import android.os.Handler;
import android.os.Looper;

public class UiThreadUtil {

    public static void runOnUiThread(Runnable runnable){
        new Handler(Looper.getMainLooper()).post(runnable);
    }
}
