/*
 * Copyright (C) 2023 Wanli Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.eirv.trex.demo;

import android.app.Activity;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import io.github.eirv.trex.Trex;
import io.github.eirv.trex.TrexAndroid;
import io.github.eirv.trex.TrexAndroidOption;
import io.github.eirv.trex.TrexOption;
import io.github.eirv.trex.TrexSpan;
import io.github.eirv.trex.TrexStyle;
import io.github.eirv.trex.test.Test;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;

public class MainActivity extends Activity implements OnSeekBarChangeListener, Runnable {
    private TextView mTextView;
    private Throwable e1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        SeekBar seekBar = findViewById(R.id.main_seek_bar);
        mTextView = findViewById(R.id.main_text);

        seekBar.setOnSeekBarChangeListener(this);
        mTextView.setTextIsSelectable(true);

        TrexAndroid.init();
        TrexAndroid.hookNativeFunc();
        TrexAndroidOption option = new TrexAndroidOption();
        option.setThrowableIdVisible(true);
        option.setProxyImplEnabled(true);
        option.applyBaseColorScheme();
        TrexOption.setDefault(option);

        Exception test = Test.test();
        e1 = new Exception("foo", new RuntimeException(new Error("bar", test)));
        e1.addSuppressed(new UnsupportedOperationException(new Throwable("xxx")));
        e1.addSuppressed(new IllegalArgumentException("bar"));
        e1.addSuppressed(test);

        runOnUiThread(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mTextView.setTextSize(progress / 4F);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {}

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {}

    @Override
    public void run() {
        Exception e = new Exception("a", new RuntimeException(new Error("foo")));
        e.addSuppressed(new UnsupportedOperationException(new Throwable("xxx")));
        e.addSuppressed(new IllegalArgumentException("bar"));
        e.addSuppressed(e1);
        Exception e2 = new Exception("suppressed", e1);
        e2.addSuppressed(new RuntimeException("test", new Error(e1)));
        e.addSuppressed(e2);
        e.addSuppressed(new Error("end", e2));

        String defStackFrame = Trex.getStackFrameString(e);
        String dumpedThrowable = TrexAndroid.dumpThrowableNative(e);
        TrexOption.getDefault().setStyle(TrexStyle.JNI).setTab("    ");
        TrexAndroid.replaceToProxyStackTrace(e);
        String stacktrace = Trex.getStackTraceString(e).replace("\t", "    ");

        SpannableStringBuilder ssb = new SpannableStringBuilder();
        ssb.append(TrexSpan.from(defStackFrame));
        ssb.append("\n\n");
        if (Method.class.getSuperclass() == AccessibleObject.class) { // Dalvik
            TrexAndroidOption option = new TrexAndroidOption();
            option.setDalvikAccelerateEnabled(true);
            option.setThrowableIdVisible(true);
            option.applyBaseColorScheme();
            ssb.append(TrexSpan.from(Trex.getStackFrameString(e, option)));
        } else { // Art
            ssb.append(TrexSpan.from(Trex.getStackFrameString(e)));
        }
        ssb.append("\n\n");
        ssb.append(dumpedThrowable);
        ssb.append("\n\n");
        ssb.append(TrexSpan.from(stacktrace));

        mTextView.setText(ssb);
    }
}
