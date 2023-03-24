# SMTShell-API

SMTShell-API is an Android library that provides methods for executing shell commands and loading shared objects **as the system user (UID 1000)** on Samsung devices that are running **SMTShell**.

## Including this in your project

**project build.gradle**
```gradle
allprojects {
    repositories {
        // other repos here
        maven { url "https://jitpack.io" }  // <--- add this
    }
}
```

**app/module build.gradle**
```gradle
dependencies {
    implementation 'com.github.BLuFeNiX:SMTShell-API:1.0'
}
```

## Get permissions

Declare one or both of these, and arrange for your user to grant them as per usual:

```
<uses-permission android:name="smtshell.permission.SYSTEM_COMMAND" />
<uses-permission android:name="smtshell.permission.LOAD_LIBRARY" />
```

## API Methods

### executeCommand

`executeCommand(Context context, String cmd)`
`executeCommand(Context context, String cmd, CommandCallback cb)`

```java
SMTShellAPI.executeCommand(context, "ls -la /data/data/android/", new SMTShellAPI.CommandCallback() {
    @Override
    public void onComplete(String stdout, String stderr, int exitCode) {
        Log.d(TAG, "stdout: " + stdout);
        Log.d(TAG, "stderr: " + stderr);
        Log.d(TAG, "exit code: " + exitCode);
    }
});
```

### loadLibrary

`loadLibrary(Context context, String path)`
`loadLibrary(Context context, String path, LoadLibraryCallback cb)`

Load a shared object as the system (UID 1000).

```java
SMTShellAPI.loadLibrary(this, getApplicationInfo().nativeLibraryDir + "/" + "libsmtshell.so", new SMTShellAPI.LoadLibraryCallback() {
    @Override
    public void onComplete(boolean success) {
        if (success) {
            // library loaded!
        } else {
            // something went wrong!
        }
    }
});
```
> Note: Callback will not fire if your shared object blocks the incoming thread (i.e., with an onload constructor).

### ping

`ping(Context context)`

Pings the API. You can capture the result with a dynamic broadcast receiver:

```java
BroadcastReceiver mApiReadyReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
        // API is ready!
    }
};

@Override
protected void onResume() {
    super.onResume();
    registerReceiver(mApiReadyReceiver, new IntentFilter(SMTShellAPI.ACTION_API_READY));
    SMTShellAPI.ping(this);
}

@Override
protected void onPause() {
    super.onPause();
    unregisterReceiver(mApiReadyReceiver);
}
```

You can also monitor the API going down (because the user killed it) via a recevier:

```java
registerReceiver(mApiDeathReceiver, new IntentFilter(SMTShellAPI.ACTION_API_DEATH_NOTICE));
```
