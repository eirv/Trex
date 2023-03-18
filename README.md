# Trex

![](https://img.shields.io/badge/VM-art%20%2F%20dalvik%20%2F%20jvm-red.svg)
![](https://img.shields.io/badge/Android-4.0.1%20~%2013-brightgreen)
![](https://img.shields.io/badge/Java-1.8%20~%2019-orange.svg)

获取更详细的Java堆栈信息,即使清除调试信息并混淆也能知道那个方法报错

## 开始
### 引入
> 远程仓库
```gradle
repositories {
    maven { url 'https://github.com/eirv/Trex/raw/main/maven' }
}
```
```gradle
dependencies {
    // android
    implementation 'io.github.eirv.trex:android:1.0.0-alpha'

    // jvm
    implementation 'io.github.eirv.trex:jvm:1.0.0-alpha'
}
```
> [GitHub Release](https://github.com/eirv/Trex/releases)
### 简单使用
> 注意:
> 因为安卓 P 的反射限制, 目前采用的是写死偏移访问隐藏 API, 可能会出现一些问题, 建议在初始化之前先过掉隐藏 API 限制, 详情可见 [MyApplication.java](https://github.com/eirv/Trex/blob/master/demo/android/src/main/java/io/github/eirv/trex/demo/MyApplication.java#L26)
```java
// android
TrexAndroid.initJava();

// jvm
TrexJvm.init();
```
```java
try {
    Integer.parseInt(null);
} catch (Exception e) {
    Trex.printStackFrame(e);
}
```

## 预览图

### Trex

> 默认样式

<img src='https://github.com/eirv/Trex/raw/main/imgs/preview_trex_default.png'>

> 安卓 JNI 样式

<img src='https://github.com/eirv/Trex/raw/main/imgs/preview_trex_jni.png'>

### 安卓自带

> 安卓 Art 虚拟机异常转储

```java
java.lang.Exception: a
  at void io.github.eirv.trex.demo.MainActivity.run() (MainActivity.java:19)
  at void android.app.Activity.runOnUiThread(java.lang.Runnable) (Activity.java:7184)
  at void io.github.eirv.trex.demo.MainActivity.onCreate(android.os.Bundle) (MainActivity.java:298)
  at void android.app.Activity.performCreate(android.os.Bundle, android.os.PersistableBundle) (Activity.java:8142)
  at void android.app.Activity.performCreate(android.os.Bundle) (Activity.java:8114)
  at void android.app.Instrumentation.callActivityOnCreate(android.app.Activity, android.os.Bundle) (Instrumentation.java:1309)
  at android.app.Activity android.app.ActivityThread.performLaunchActivity(android.app.ActivityThread$ActivityClientRecord, android.content.Intent) (ActivityThread.java:3549)
  at android.app.Activity android.app.ActivityThread.handleLaunchActivity(android.app.ActivityThread$ActivityClientRecord, android.app.servertransaction.PendingTransactionActions, android.content.Intent) (ActivityThread.java:3748)
  at void android.app.servertransaction.LaunchActivityItem.execute(android.app.ClientTransactionHandler, android.os.IBinder, android.app.servertransaction.PendingTransactionActions) (LaunchActivityItem.java:85)
  at void android.app.servertransaction.TransactionExecutor.executeCallbacks(android.app.servertransaction.ClientTransaction) (TransactionExecutor.java:135)
  at void android.app.servertransaction.TransactionExecutor.execute(android.app.servertransaction.ClientTransaction) (TransactionExecutor.java:95)
  at void android.app.ActivityThread$H.handleMessage(android.os.Message) (ActivityThread.java:2187)
  at void android.os.Handler.dispatchMessage(android.os.Message) (Handler.java:106)
  at void android.os.Looper.loop() (Looper.java:236)
  at void android.app.ActivityThread.main(java.lang.String[]) (ActivityThread.java:8057)
  at java.lang.Object java.lang.reflect.Method.invoke(java.lang.Object, java.lang.Object[]) (Method.java:-2)
  at void com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run() (RuntimeInit.java:620)
  at void com.android.internal.os.ZygoteInit.main(java.lang.String[]) (ZygoteInit.java:1011)
Caused by: java.lang.RuntimeException: java.lang.Error: foo
  at void io.github.eirv.trex.demo.MainActivity.run() (MainActivity.java:14)
  at void android.app.Activity.runOnUiThread(java.lang.Runnable) (Activity.java:7184)
  at void io.github.eirv.trex.demo.MainActivity.onCreate(android.os.Bundle) (MainActivity.java:298)
  at void android.app.Activity.performCreate(android.os.Bundle, android.os.PersistableBundle) (Activity.java:8142)
  at void android.app.Activity.performCreate(android.os.Bundle) (Activity.java:8114)
  at void android.app.Instrumentation.callActivityOnCreate(android.app.Activity, android.os.Bundle) (Instrumentation.java:1309)
  at android.app.Activity android.app.ActivityThread.performLaunchActivity(android.app.ActivityThread$ActivityClientRecord, android.content.Intent) (ActivityThread.java:3549)
  at android.app.Activity android.app.ActivityThread.handleLaunchActivity(android.app.ActivityThread$ActivityClientRecord, android.app.servertransaction.PendingTransactionActions, android.content.Intent) (ActivityThread.java:3748)
  at void android.app.servertransaction.LaunchActivityItem.execute(android.app.ClientTransactionHandler, android.os.IBinder, android.app.servertransaction.PendingTransactionActions) (LaunchActivityItem.java:85)
  at void android.app.servertransaction.TransactionExecutor.executeCallbacks(android.app.servertransaction.ClientTransaction) (TransactionExecutor.java:135)
  at void android.app.servertransaction.TransactionExecutor.execute(android.app.servertransaction.ClientTransaction) (TransactionExecutor.java:95)
  at void android.app.ActivityThread$H.handleMessage(android.os.Message) (ActivityThread.java:2187)
  at void android.os.Handler.dispatchMessage(android.os.Message) (Handler.java:106)
  at void android.os.Looper.loop() (Looper.java:236)
  at void android.app.ActivityThread.main(java.lang.String[]) (ActivityThread.java:8057)
  at java.lang.Object java.lang.reflect.Method.invoke(java.lang.Object, java.lang.Object[]) (Method.java:-2)
  at void com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run() (RuntimeInit.java:620)
  at void com.android.internal.os.ZygoteInit.main(java.lang.String[]) (ZygoteInit.java:1011)
Caused by: java.lang.Error: foo
  at void io.github.eirv.trex.demo.MainActivity.run() (MainActivity.java:11)
  at void android.app.Activity.runOnUiThread(java.lang.Runnable) (Activity.java:7184)
  at void io.github.eirv.trex.demo.MainActivity.onCreate(android.os.Bundle) (MainActivity.java:298)
  at void android.app.Activity.performCreate(android.os.Bundle, android.os.PersistableBundle) (Activity.java:8142)
  at void android.app.Activity.performCreate(android.os.Bundle) (Activity.java:8114)
  at void android.app.Instrumentation.callActivityOnCreate(android.app.Activity, android.os.Bundle) (Instrumentation.java:1309)
  at android.app.Activity android.app.ActivityThread.performLaunchActivity(android.app.ActivityThread$ActivityClientRecord, android.content.Intent) (ActivityThread.java:3549)
  at android.app.Activity android.app.ActivityThread.handleLaunchActivity(android.app.ActivityThread$ActivityClientRecord, android.app.servertransaction.PendingTransactionActions, android.content.Intent) (ActivityThread.java:3748)
  at void android.app.servertransaction.LaunchActivityItem.execute(android.app.ClientTransactionHandler, android.os.IBinder, android.app.servertransaction.PendingTransactionActions) (LaunchActivityItem.java:85)
  at void android.app.servertransaction.TransactionExecutor.executeCallbacks(android.app.servertransaction.ClientTransaction) (TransactionExecutor.java:135)
  at void android.app.servertransaction.TransactionExecutor.execute(android.app.servertransaction.ClientTransaction) (TransactionExecutor.java:95)
  at void android.app.ActivityThread$H.handleMessage(android.os.Message) (ActivityThread.java:2187)
  at void android.os.Handler.dispatchMessage(android.os.Message) (Handler.java:106)
  at void android.os.Looper.loop() (Looper.java:236)
  at void android.app.ActivityThread.main(java.lang.String[]) (ActivityThread.java:8057)
  at java.lang.Object java.lang.reflect.Method.invoke(java.lang.Object, java.lang.Object[]) (Method.java:-2)
  at void com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run() (RuntimeInit.java:620)
  at void com.android.internal.os.ZygoteInit.main(java.lang.String[]) (ZygoteInit.java:1011)
```

> 安卓默认

```java
java.lang.Exception: a
    at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:19)
    at android.app.Activity.runOnUiThread(Activity.java:7184)
    at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:298)
    at android.app.Activity.performCreate(Activity.java:8142)
    at android.app.Activity.performCreate(Activity.java:8114)
    at android.app.Instrumentation.callActivityOnCreate(Instrumentation.java:1309)
    at android.app.ActivityThread.performLaunchActivity(ActivityThread.java:3549)
    at android.app.ActivityThread.handleLaunchActivity(ActivityThread.java:3748)
    at android.app.servertransaction.LaunchActivityItem.execute(LaunchActivityItem.java:85)
    at android.app.servertransaction.TransactionExecutor.executeCallbacks(TransactionExecutor.java:135)
    at android.app.servertransaction.TransactionExecutor.execute(TransactionExecutor.java:95)
    at android.app.ActivityThread$H.handleMessage(ActivityThread.java:2187)
    at android.os.Handler.dispatchMessage(Handler.java:106)
    at android.os.Looper.loop(Looper.java:236)
    at android.app.ActivityThread.main(ActivityThread.java:8057)
    at java.lang.reflect.Method.invoke(Native Method)
    at com.android.internal.os.RuntimeInit$MethodAndArgsCaller.run(RuntimeInit.java:620)
    at com.android.internal.os.ZygoteInit.main(ZygoteInit.java:1011)
    Suppressed: java.lang.UnsupportedOperationException: java.lang.Throwable: xxx
        at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:31)
        ... 17 more
    Caused by: java.lang.Throwable: xxx
        at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:28)
        ... 17 more
    Suppressed: java.lang.IllegalArgumentException: bar
        at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:59)
        ... 17 more
    Suppressed: java.lang.Exception: foo
        at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:215)
        ... 15 more
        Suppressed: java.lang.UnsupportedOperationException: java.lang.Throwable: xxx
            at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:229)
            ... 15 more
        Caused by: java.lang.Throwable: xxx
            at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:226)
            ... 15 more
        Suppressed: java.lang.IllegalArgumentException: bar
            at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:255)
            ... 15 more
        Suppressed: java.lang.RuntimeException: hello
            at io.github.eirv.trex.test.Test$1.run(Test.java:5)
            at io.github.eirv.trex.test.NoSourceFileNoLineNumber.test(Unknown Source:0)
            at io.github.eirv.trex.test.NoSourceFileLineNumber.test(Unknown Source:1)
            at io.github.eirv.trex.test.SourceFileNoLineNumber.test(Unknown Source:0)
            at io.github.eirv.trex.test.SourceFileLineNumber.test(SourceFileLineNumber.java:1)
            at io.github.eirv.trex.test.SyntheticClassTest.bridge(SyntheticClassTest.java:1)
            at io.github.eirv.trex.test.SyntheticClassTest.synthetic(SyntheticClassTest.java:1)
            at io.github.eirv.trex.test.SyntheticClassTest.test(SyntheticClassTest.java:1)
            at io.github.eirv.trex.test.ClassTest.bridge(ClassTest.java:1)
            at io.github.eirv.trex.test.ClassTest.synthetic(ClassTest.java:1)
            at io.github.eirv.trex.test.ClassTest.test(ClassTest.java:1)
            at io.github.eirv.trex.test.Test.testJ(Test.java:6)
            at io.github.eirv.trex.test.Test.testI(Test.java:10)
            at io.github.eirv.trex.test.Test.testH(Test.java:12)
            at io.github.eirv.trex.test.Test.testG(Test.java:15)
            at io.github.eirv.trex.test.Test.testF(Test.java:1)
            at io.github.eirv.trex.test.Test.testG(Test.java:10)
            at io.github.eirv.trex.test.Test.testF(Test.java:1)
            at io.github.eirv.trex.test.Test.testG(Test.java:10)
            at io.github.eirv.trex.test.Test.testF(Test.java:1)
            at io.github.eirv.trex.test.Test.testG(Test.java:10)
            at io.github.eirv.trex.test.Test.testF(Test.java:1)
            at io.github.eirv.trex.test.Test.testG(Test.java:10)
            at io.github.eirv.trex.test.Test.testF(Test.java:1)
            at io.github.eirv.trex.test.Test.testE(Test.java:1)
            at io.github.eirv.trex.test.Test.testD(Test.java:1)
            at io.github.eirv.trex.test.Test.testC(Test.java:15)
            at io.github.eirv.trex.test.Test.testB(Test.java:1)
            at io.github.eirv.trex.test.Test.testA(Test.java:1)
            at io.github.eirv.trex.test.Test.testC(Test.java:10)
            at io.github.eirv.trex.test.Test.testB(Test.java:1)
            at io.github.eirv.trex.test.Test.testA(Test.java:1)
            at io.github.eirv.trex.test.Test.testC(Test.java:10)
            at io.github.eirv.trex.test.Test.testB(Test.java:1)
            at io.github.eirv.trex.test.Test.testA(Test.java:1)
            at io.github.eirv.trex.test.Test.test(Test.java:6)
            at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:195)
            ... 15 more
    Caused by: java.lang.RuntimeException: java.lang.Error: bar
        at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:210)
        ... 15 more
    Caused by: java.lang.Error: bar
        at io.github.eirv.trex.demo.MainActivity.onCreate(MainActivity.java:207)
        ... 15 more
    [CIRCULAR REFERENCE:java.lang.RuntimeException: hello]
    Suppressed: java.lang.Exception: suppressed
        at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:108)
        ... 17 more
        Suppressed: java.lang.RuntimeException: test
            at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:122)
            ... 17 more
        Caused by: java.lang.Error: java.lang.Exception: foo
            at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:117)
            ... 17 more
    [CIRCULAR REFERENCE:java.lang.Exception: foo]
    [CIRCULAR REFERENCE:java.lang.Exception: foo]
    Suppressed: java.lang.Error: end
        at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:167)
        ... 17 more
    [CIRCULAR REFERENCE:java.lang.Exception: suppressed]
Caused by: java.lang.RuntimeException: java.lang.Error: foo
    at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:14)
    ... 17 more
Caused by: java.lang.Error: foo
    at io.github.eirv.trex.demo.MainActivity.run(MainActivity.java:11)
    ... 17 more
```
