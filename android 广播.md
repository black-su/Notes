#android 广播


广播的使用：
https://www.cnblogs.com/naray/p/5348809.html

广播的原理：
https://skytoby.github.io/2019/BroadcastCast%E5%B9%BF%E6%92%AD%E6%9C%BA%E5%88%B6%E5%8E%9F%E7%90%86/


补充几点：

有序广播之间的数据传递：
```
    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.v(TAG,"broadcastReceiver====intent:"+intent+"  "+intent.getExtras().get("tag")+"  "+getResultData());
            setResultData("broadcastReceiver====onReceive");
        }
    };
```


静态注册广播后，单纯使用setAction()的方式无法正常发送广播。在Android 8.0以及更高版本上需要通过setAction()和setComponent()才能正常发送广播(单独setComponent()也可以启动)。动态注册可以单纯使用setAction()发送广播。


前台广播/后台广播
```
Intent intent = new Intent();
intent.setAction("com.android.xxxxx");
//intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);//前台广播（默认是后台广播）
sendBroadcast(intent);
```

只要是执行sendOrderedBroadcast()发送的有序广播，那么只要BroadcastReceiver.onReceive()超时，必然会引发广播超时ANR。
如果执行sendBroadcast()发送的普通广播，如果接收者是动态注册的，即使BroadcastReceiver.onReceive()超时，也不会引起广播超时ANR(在onReceive期间不点击屏幕，否则还是会引起触摸事件超时引发的ANR)

1.当发送串行广播（order= true）时
静态注册的广播接收者（receivers），采用串行处理
动态注册的广播接收者（registeredReceivers），采用串行处理

2.当发送并行广播（order= false）时
静态注册的广播接收者（receivers），采用串行处理
动态注册的广播接收者（registeredReceivers），采用并行处理