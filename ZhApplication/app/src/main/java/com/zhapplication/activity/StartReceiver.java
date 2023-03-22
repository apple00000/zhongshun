package com.zhapplication.activity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class StartReceiver extends BroadcastReceiver {
    private final String ACTION_BOOT = "android.intent.action.BOOT_COMPLETED";
    private final String ACTION_MEDIA_MOUNTED = "android.intent.action.MEDIA_MOUNTED";
    private final String ACTION_MEDIA_UNMOUNTED = "android.intent.action.MEDIA_UNMOUNTED";
    private final String ACTION_MEDIA_EJECT = "android.intent.action.MEDIA_EJECT";
    private final String ACTION_MEDIA_REMOVED = "android.intent.action.MEDIA_REMOVED";
    @Override
    public void onReceive(Context context, Intent intent) {
        // 判断是否是系统开启启动的消息，如果是，则启动APP
        if (    ACTION_BOOT.equals(intent.getAction()) ||
                ACTION_MEDIA_MOUNTED.equals(intent.getAction()) ||
                ACTION_MEDIA_UNMOUNTED.equals(intent.getAction()) ||
                ACTION_MEDIA_EJECT.equals(intent.getAction()) ||
                ACTION_MEDIA_REMOVED.equals(intent.getAction())
        ) {
            Intent intentMainActivity = new Intent(context, MainActivity.class);
            intentMainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentMainActivity);
        }
    }
}