#android ANR分析

1. 自用手机一般都没有root权限，无法adb push /data/anr下面的traces.txt文件。但是可以使用adb bugreport命令，把系统下的所有信息下载到当前目录下，解压后在FS/data/anr中就可以找到相应的traces.txt文件。

待研究：adb bugreport性能优化分析
https://blog.csdn.net/createchance/article/details/51954142

2. traces.txt分析

https://www.jianshu.com/p/ac3a7c28b830


案例1:

设置BroadcastReceiver广播中的onReceive()睡眠12s，发送多条广播，然后点击屏幕触发ANR。BroadcastTimeout(前台10s,后台60s):
```
public class MyReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        try {
            Thread.sleep(12000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
```

正常adb logcat -> log.txt拿到的log查找到的ANR信息如下：触摸事件处理超时
```
05-29 17:44:45.764  2246  2681 I ActivityManager: Done dumping
05-29 17:44:45.771  2246  2681 E ActivityManager: ANR in com.example.myapplication (com.example.myapplication/.MainActivity)
05-29 17:44:45.771  2246  2681 E ActivityManager: PID: 28340
05-29 17:44:45.771  2246  2681 E ActivityManager: Reason: Input dispatching timed out (Waiting to send non-key event because the touched window has not finished processing certain input events that were delivered to it over 500.0ms ago. waitqueue length = 10, head.seq = 5739424, Wait queue head age: 5518.4ms.)
05-29 17:44:45.771  2246  2681 E ActivityManager: Parent: com.example.myapplication/.MainActivity
05-29 17:44:45.771  2246  2681 E ActivityManager: Load: 0.0 / 0.0 / 0.0
05-29 17:44:45.771  2246  2681 E ActivityManager: CPU usage from 44373ms to 0ms ago (2021-05-29 17:43:57.333 to 2021-05-29 17:44:41.705):
05-29 17:44:45.771  2246  2681 E ActivityManager:   34% 7928/cmccwm.mobilemusic: 30% user + 3.2% kernel / faults: 2770 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   14% 2246/system_server: 9.9% user + 4.3% kernel / faults: 10327 minor 5 major
05-29 17:44:45.771  2246  2681 E ActivityManager:   11% 25092/adbd: 2% user + 9.6% kernel / faults: 189 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   10% 741/audioserver: 8.5% user + 1.5% kernel / faults: 37 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   8.2% 8983/com.android.bluetooth: 3.7% user + 4.4% kernel / faults: 122 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   6.7% 496/logd: 1.4% user + 5.3% kernel / faults: 8 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   6.6% 745/surfaceflinger: 3% user + 3.5% kernel / faults: 605 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   5% 17958/com.eg.android.AlipayGphone: 2.8% user + 2.1% kernel / faults: 2790 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   3.3% 718/vendor.huawei.hardware.audio@5.0-service: 0.7% user + 2.6% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   2.8% 719/vendor.huawei.hardware.bluetooth@1.1-service: 0.5% user + 2.2% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   2.5% 14404/kworker/u16:6: 0% user + 2.5% kernel / faults: 1 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   2.1% 6380/com.huawei.android.launcher: 1.5% user + 0.6% kernel / faults: 9469 minor 87 major
05-29 17:44:45.771  2246  2681 E ActivityManager:   2.1% 10702/kworker/u16:0: 0% user + 2.1% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.9% 27469/logcat: 0.6% user + 1.3% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.8% 4089/com.huawei.livewallpaper.paradise: 1.2% user + 0.6% kernel / faults: 6197 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.8% 27573/kworker/u16:3: 0% user + 1.8% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.7% 789/aptouch_daemon: 1.3% user + 0.3% kernel / faults: 1 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.6% 28035/logcat: 0.3% user + 1.3% kernel / faults: 3 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.5% 22115/kworker/u16:2: 0% user + 1.5% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.3% 20036/kworker/u16:7: 0% user + 1.3% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.3% 708/android.hardware.graphics.composer@2.2-service: 0.5% user + 0.7% kernel / faults: 1323 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.3% 4330/com.huawei.android.totemweather: 0.8% user + 0.4% kernel / faults: 12046 minor 4 major
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.3% 20121/com.eg.android.AlipayGphone:push: 0.1% user + 1.1% kernel / faults: 22 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.9% 11046/com.android.vending: 0.4% user + 0.4% kernel / faults: 2052 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   1.1% 4267/com.huawei.iaware: 0.6% user + 0.4% kernel / faults: 3021 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.8% 24794/kworker/u16:1: 0% user + 0.8% kernel / faults: 1 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.8% 1021/vendor.huawei.hardware.sensors@1.1-service: 0.2% user + 0.5% kernel / faults: 2 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.8% 3828/com.android.systemui: 0.4% user + 0.3% kernel / faults: 596 minor 3 major
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.8% 18683/kworker/u16:5: 0% user + 0.8% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.7% 724/vendor.huawei.hardware.hwdisplay.displayengine@1.2-service: 0.4% user + 0.3% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.7% 9087/cmccwm.mobilemusic:pushservice: 0.3% user + 0.4% kernel / faults: 14 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.7% 30924/com.huawei.hwid.core: 0.6% user + 0.1% kernel / faults: 1611 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.7% 14373/chargelogcat-c: 0% user + 0.7% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.7% 996/dubaid: 0.1% user + 0.5% kernel / faults: 294 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.6% 1018/hwpged: 0.1% user + 0.5% kernel / faults: 1 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.6% 1029/hiview: 0.1% user + 0.4% kernel / faults: 205 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.5% 9/rcu_preempt: 0% user + 0.5% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.5% 270/irq/187-thp: 0% user + 0.5% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.5% 1398/hisi_hcc: 0% user + 0.5% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.4% 4168/com.huawei.systemserver: 0.2% user + 0.2% kernel / faults: 91 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.4% 746/powerlogd: 0.3% user + 0.1% kernel / faults: 1 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.4% 4971/com.android.phone: 0.2% user + 0.1% kernel / faults: 200 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.4% 8/ksoftirqd/0: 0% user + 0.4% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.4% 8007/cmccwm.mobilemusic:AgentService: 0.1% user + 0.2% kernel / faults: 129 minor 6 major
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.4% 24952/com.tencent.mm:toolsmp: 0.2% user + 0.1% kernel / faults: 75 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.3% 4030/com.huawei.systemmanager:service: 0.2% user + 0.1% kernel / faults: 220 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.2% 7999/com.tencent.mm:push: 0.1% user + 0.1% kernel / faults: 148 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.3% 5687/sugov:0: 0% user + 0.3% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.2% 744/lmkd: 0% user + 0.2% kernel
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.2% 25041/com.tencent.mm:sandbox: 0% user + 0.1% kernel / faults: 60 minor
05-29 17:44:45.771  2246  2681 E ActivityManager:   0.2% 6946/com.huawei.hiai.
```

以上模拟复现抓取的log显示很多都是触摸事件超时。通过adb bugreport拿到traces log，分析如下：
通过以上的log信息，得到当前发生ANR的进程是PID: 28340

```
"main" prio=5 tid=1 Sleeping
  | group="main" sCount=1 dsCount=0 flags=1 obj=0x739eb360 self=0x72a2210800
  | sysTid=28340 nice=-10 cgrp=default sched=1073741825/2 handle=0x73292350d0
  | state=S schedstat=( 303173420 25611469 360 ) utm=24 stm=5 core=4 HZ=100
  | stack=0x7fdb8bb000-0x7fdb8bd000 stackSize=8192KB
  | held mutexes=
  at java.lang.Thread.sleep(Native method)
  - sleeping on <0x02829f86> (a java.lang.Object)
  at java.lang.Thread.sleep(Thread.java:443)
  - locked <0x02829f86> (a java.lang.Object)
  at java.lang.Thread.sleep(Thread.java:359)
  at com.example.myapplication.MyReceiver.onReceive(MyReceiver.java:16)
  at android.app.ActivityThread.handleReceiver(ActivityThread.java:4469)
  at android.app.ActivityThread.access$2700(ActivityThread.java:251)
  at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2314)
  at android.os.Handler.dispatchMessage(Handler.java:110)
  at android.os.Looper.loop(Looper.java:219)
  at android.app.ActivityThread.main(ActivityThread.java:8393)
  at java.lang.reflect.Method.invoke(Native method)
  at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:513)
  at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1055)
```
traces log中可以清晰的看到哦main进程处于睡眠状态，根据调用栈信息定位到MyReceiver.onReceive()。
