package io.github.eirv.trex.demo;

import android.app.Activity;
import android.app.ActivityThread;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup.LayoutParams;
import android.widget.HorizontalScrollView;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.CharArrayWriter;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;

public class CrashHandler implements UncaughtExceptionHandler {
    private static final String KEY_CRASH_LOG = "crash_log";

    private static CrashHandler sInstance;

    private final Context mContext;
    private final UncaughtExceptionHandler mDefaultHandler;

    private CrashHandler(Context context, UncaughtExceptionHandler defaultHandler) {
        mContext = context;
        mDefaultHandler = defaultHandler;
    }

    public static CrashHandler getInstance() {
        return getInstance(null);
    }

    public static CrashHandler getInstance(Context context) {
        if (sInstance == null) {
            UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
            if (defaultHandler instanceof CrashHandler) {
                sInstance = (CrashHandler) defaultHandler;
            } else {
                if (context == null) {
                    context = ActivityThread.currentApplication();
                }
                sInstance = new CrashHandler(context, defaultHandler);
            }
        }
        return sInstance;
    }

    public void init() {
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable throwable) {
        CharArrayWriter writer = new CharArrayWriter(128 * 1024);
        PrintWriter out = new PrintWriter(writer);

        out.print("Time       : ");
        out.println(System.currentTimeMillis());
        out.print("Android SDK: ");
        out.println(Build.VERSION.SDK_INT);
        out.println();

        out.append("Exception in thread \"");
        out.append(thread.getName());
        out.println('\"');
        throwable.printStackTrace(out);

        String crashLog = writer.toString();

        Intent intent = new Intent(mContext, CrashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra(KEY_CRASH_LOG, crashLog);
        mContext.startActivity(intent);

        mDefaultHandler.uncaughtException(thread, throwable);
    }

    public static class CrashActivity extends Activity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            String crashLog = getIntent().getStringExtra(KEY_CRASH_LOG);

            ScrollView scrollView = new ScrollView(this);
            scrollView.setFillViewport(true);
            HorizontalScrollView horizontalScrollView = new HorizontalScrollView(this);
            horizontalScrollView.setFillViewport(true);
            scrollView.addView(
                    horizontalScrollView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            TextView textView = new TextView(this);
            textView.setText(crashLog);
            textView.setTextIsSelectable(true);
            horizontalScrollView.addView(
                    textView, LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
            setContentView(scrollView);
        }
    }
}
