package net.blufenix.smtshell.api;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.concurrent.atomic.AtomicInteger;

public class SMTShellAPI {

    // requests
    public static final String ACTION_SHELL_COMMAND = "smtshell.intent.action.SHELL_COMMAND";
    public static final String EXTRA_COMMAND = "smtshell.intent.extra.COMMAND";
    public static final String EXTRA_REQUEST_ID = "smtshell.intent.extra.REQUEST_ID";
    public static final String EXTRA_CALLBACK_PKG = "smtshell.intent.extra.CALLBACK_PKG"; // IntentSender
    public static final String PKG_NAME_SMT = "com.samsung.SMT"; // send requests to this pkg

    public static final String ACTION_LOAD_LIBRARY = "smtshell.intent.action.ACTION_LOAD_LIBRARY";
    public static final String EXTRA_LIBRARY_PATH = "smtshell.intent.extra.EXTRA_LIBRARY_PATH";

    public static final String ACTION_API_PING = "smtshell.intent.action.API_PING";

    // results
    public static final String ACTION_SHELL_RESULT = "smtshell.intent.action.SHELL_RESULT";
    public static final String EXTRA_STDOUT = "smtshell.intent.extra.STDOUT";
    public static final String EXTRA_STDERR = "smtshell.intent.extra.STDERR";
    public static final String EXTRA_EXIT_CODE = "smtshell.intent.extra.EXIT_CODE";

    public static final String ACTION_LOAD_LIBRARY_RESULT = "smtshell.intent.action.LOAD_LIBRARY_RESULT";
    public static final String EXTRA_LOAD_SUCCESS = "smtshell.intent.extra.LOAD_SUCCESS";

    public static final String ACTION_API_READY = "smtshell.intent.action.API_READY";
    public static final String ACTION_API_DEATH_NOTICE = "smtshell.intent.action.API_DEATH_NOTICE";

    // permissions
    public static final String PERMISSION_SYSTEM_COMMAND = "smtshell.permission.SYSTEM_COMMAND";
    public static final String PERMISSION_LOAD_LIBRARY = "smtshell.permission.LOAD_LIBRARY";
    public static final String PERMISSION_RECEIVER_GUARD = "android.permission.REBOOT";

    // start at a number a user is unlikely to use for themselves,
    //  in case they manually call the API without the wrapper
    private static final AtomicInteger REQUEST_ID = new AtomicInteger(1000000);
    static int nextId() {
        return REQUEST_ID.getAndIncrement();
    }

    public static void executeCommand(Context context, String cmd) {
        executeCommand(context, cmd, null);
    }

    public static void executeCommand(Context context, String cmd, CommandCallback cb) {
        // setup intent
        int requestId = nextId();
        Intent intent = createIntent(ACTION_SHELL_COMMAND, requestId);
        intent.putExtra(EXTRA_COMMAND, cmd);

        if (cb != null) {
            // specify that the request has a sender
            setSender(context, intent);
            // setup receiver
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (requestId == intent.getIntExtra(EXTRA_REQUEST_ID, -1)) {
                        context.unregisterReceiver(this);
                        cb.onComplete(
                                intent.getStringExtra(EXTRA_STDOUT),
                                intent.getStringExtra(EXTRA_STDERR),
                                intent.getIntExtra(EXTRA_EXIT_CODE, -1)
                        );
                    }
                }
            }, new IntentFilter(ACTION_SHELL_RESULT), PERMISSION_RECEIVER_GUARD, null);
        }

        // send command
        context.sendBroadcast(intent);
    }

    public static void loadLibrary(Context context, String path) {
        loadLibrary(context, path, null);
    }

    public static void loadLibrary(Context context, String path, LoadLibraryCallback cb) {
        // setup intent
        int requestId = nextId();
        Intent intent = createIntent(ACTION_LOAD_LIBRARY, requestId);
        intent.putExtra(EXTRA_LIBRARY_PATH, path);

        if (cb != null) {
            // specify that the request has a sender
            setSender(context, intent);
            // setup receiver
            context.registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (requestId == intent.getIntExtra(EXTRA_REQUEST_ID, -1)) {
                        context.unregisterReceiver(this);
                        cb.onComplete(intent.getBooleanExtra(EXTRA_LOAD_SUCCESS, false));
                    }
                }
            }, new IntentFilter(ACTION_LOAD_LIBRARY_RESULT), PERMISSION_RECEIVER_GUARD, null);
        }

        // send the command
        context.sendBroadcast(intent);
    }

    public static void ping(Context context) {
        Intent intent = createIntent(ACTION_API_PING, -1);
        setSender(context, intent);
        context.sendBroadcast(intent);
    }

    static Intent createIntent(String action, int requestId) {
        Intent intent = new Intent(action);
        intent.setPackage(PKG_NAME_SMT);
        intent.putExtra(EXTRA_REQUEST_ID, requestId);
        return intent;
    }

    // used to prove where the intent came from, so other packages can't request things on our behalf
    static void setSender(Context context, Intent intent) {
        int flags = PendingIntent.FLAG_IMMUTABLE;
        PendingIntent self = PendingIntent.getBroadcast(context, 0, new Intent(), flags);
        intent.putExtra(EXTRA_CALLBACK_PKG, self.getIntentSender());
    }

    public interface CommandCallback {
        void onComplete(String stdout, String stderr, int exitCode);
    }

    public interface LoadLibraryCallback {
        void onComplete(boolean success);
    }

}

