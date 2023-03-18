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

package io.github.eirv.trex;

import java.util.Arrays;

/**
 * @author Eirv
 * @date 2023/2/7 12:19
 */
public class TrexOption implements Cloneable {
    public static final byte COLOR_TEXT;
    public static final byte COLOR_PACKAGE_NAME;
    public static final byte COLOR_CLASS_NAME;
    public static final byte COLOR_FILE_NAME;
    public static final byte COLOR_LINE_NUMBER;
    public static final byte COLOR_CLASS_LOADER_NAME;
    public static final byte COLOR_MODULE_NAME;
    public static final byte COLOR_MODULE_VERSION;
    public static final byte COLOR_NUMBER;
    public static final byte COLOR_MESSAGE;
    public static final byte COLOR_CAPTION;
    public static final byte COLOR_AT;
    public static final byte COLOR_PUNCTUATION;
    public static final byte COLOR_DESCRIPTOR_L;
    public static final byte COLOR_DESCRIPTOR_PACKAGE_NAME;
    public static final byte COLOR_DESCRIPTOR_PACKAGE_NAME_SYNTHETIC;
    public static final byte COLOR_DESCRIPTOR_CLASS_NAME;
    public static final byte COLOR_DESCRIPTOR_CLASS_NAME_SYNTHETIC;
    public static final byte COLOR_DESCRIPTOR_METHOD_NAME;
    public static final byte COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC;
    public static final byte COLOR_DESCRIPTOR_PRIMITIVE;
    public static final byte COLOR_DESCRIPTOR_SEMICOLON;
    public static final byte COLOR_DESCRIPTOR_ARROW;

    private static final byte COLOR_MIN = 0;
    private static final byte COLOR_MAX = 22;

    private static TrexOption sDefaultOption = new TrexOption();

    static {
        // 阻止 java 编译器内联
        COLOR_TEXT = 0;
        COLOR_PACKAGE_NAME = 1;
        COLOR_CLASS_NAME = 2;
        COLOR_FILE_NAME = 3;
        COLOR_LINE_NUMBER = 4;
        COLOR_CLASS_LOADER_NAME = 5;
        COLOR_MODULE_NAME = 6;
        COLOR_MODULE_VERSION = 7;
        COLOR_NUMBER = 8;
        COLOR_MESSAGE = 9;
        COLOR_CAPTION = 10;
        COLOR_AT = 11;
        COLOR_PUNCTUATION = 12;
        COLOR_DESCRIPTOR_L = 13;
        COLOR_DESCRIPTOR_PACKAGE_NAME = 14;
        COLOR_DESCRIPTOR_PACKAGE_NAME_SYNTHETIC = 15;
        COLOR_DESCRIPTOR_CLASS_NAME = 16;
        COLOR_DESCRIPTOR_CLASS_NAME_SYNTHETIC = 17;
        COLOR_DESCRIPTOR_METHOD_NAME = 18;
        COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC = 19;
        COLOR_DESCRIPTOR_PRIMITIVE = 20;
        COLOR_DESCRIPTOR_SEMICOLON = 21;
        COLOR_DESCRIPTOR_ARROW = 22;
    }

    private TrexStyle mStyle = TrexStyle.DEFAULT;
    private String mTab;
    private boolean mCacheEnabled = true;
    private boolean mFoldEnabled = true;
    private boolean mBootMethodTypeVisible;
    private boolean mSynthesizedMethodTypeVisible;
    private boolean mUniqueMethodTypeVisible = true;
    private boolean mThrowableIdVisible;
    private boolean mClassLoaderNameVisible;
    private boolean mModuleNameVisible = true;
    private boolean mModuleVersionVisible;
    private boolean mByteCodeIndexVisible = true;
    private boolean mOnlyCompareHashCodeEnabled = true;
    private boolean mCheckDuplicateTraceEnabled = true;
    private int mDuplicateTraceMaxSize = 8;
    private boolean mColorSchemeEnabled;
    private String[] mColorScheme;

    public TrexOption() {}

    protected TrexOption(TrexOption orig) {
        mStyle = orig.mStyle;
        mTab = orig.mTab;
        mCacheEnabled = orig.mCacheEnabled;
        mFoldEnabled = orig.mFoldEnabled;
        mBootMethodTypeVisible = orig.mBootMethodTypeVisible;
        mSynthesizedMethodTypeVisible = orig.mSynthesizedMethodTypeVisible;
        mUniqueMethodTypeVisible = orig.mUniqueMethodTypeVisible;
        mThrowableIdVisible = orig.mThrowableIdVisible;
        mClassLoaderNameVisible = orig.mClassLoaderNameVisible;
        mModuleNameVisible = orig.mModuleNameVisible;
        mModuleVersionVisible = orig.mModuleVersionVisible;
        mByteCodeIndexVisible = orig.mByteCodeIndexVisible;
        mOnlyCompareHashCodeEnabled = orig.mOnlyCompareHashCodeEnabled;
        mCheckDuplicateTraceEnabled = orig.mCheckDuplicateTraceEnabled;
        mDuplicateTraceMaxSize = orig.mDuplicateTraceMaxSize;
        mColorSchemeEnabled = orig.mColorSchemeEnabled;
        mColorScheme = orig.mColorScheme != null ? orig.mColorScheme.clone() : null;
    }

    public static TrexOption getDefault() {
        return sDefaultOption;
    }

    public static void setDefault(TrexOption option) {
        Utils.requireNonNull(option, "option");
        sDefaultOption = option;
    }

    protected TrexStyle getStyle() {
        return mStyle;
    }

    public TrexOption setStyle(TrexStyle style) {
        Utils.requireNonNull(style, "style");
        mStyle = style;
        return this;
    }

    protected String getTab() {
        if (mTab != null) {
            return mTab;
        }
        return mStyle.tab();
    }

    public TrexOption setTab(String tab) {
        mTab = tab;
        return this;
    }

    protected boolean isCacheEnabled() {
        return mCacheEnabled;
    }

    public TrexOption setCacheEnabled(boolean cacheEnabled) {
        mCacheEnabled = cacheEnabled;
        return this;
    }

    protected boolean isFoldEnabled() {
        return mFoldEnabled;
    }

    public TrexOption setFoldEnabled(boolean foldEnabled) {
        mFoldEnabled = foldEnabled;
        return this;
    }

    protected boolean isBootMethodTypeVisible() {
        return mBootMethodTypeVisible;
    }

    public TrexOption setBootMethodTypeVisible(boolean bootMethodTypeVisible) {
        mBootMethodTypeVisible = bootMethodTypeVisible;
        return this;
    }

    protected boolean isSynthesizedMethodTypeVisible() {
        return mSynthesizedMethodTypeVisible;
    }

    public TrexOption setSynthesizedMethodTypeVisible(boolean synthesizedMethodTypeVisible) {
        mSynthesizedMethodTypeVisible = synthesizedMethodTypeVisible;
        return this;
    }

    protected boolean isUniqueMethodTypeVisible() {
        return mUniqueMethodTypeVisible;
    }

    public TrexOption setUniqueMethodTypeVisible(boolean uniqueMethodTypeVisible) {
        mUniqueMethodTypeVisible = uniqueMethodTypeVisible;
        return this;
    }

    protected boolean isThrowableIdVisible() {
        return mThrowableIdVisible;
    }

    public TrexOption setThrowableIdVisible(boolean throwableIdVisible) {
        mThrowableIdVisible = throwableIdVisible;
        return this;
    }

    protected boolean isClassLoaderNameVisible() {
        return mClassLoaderNameVisible;
    }

    public TrexOption setClassLoaderNameVisible(boolean classLoaderNameVisible) {
        mClassLoaderNameVisible = classLoaderNameVisible;
        return this;
    }

    protected boolean isModuleNameVisible() {
        return mModuleNameVisible;
    }

    public TrexOption setModuleNameVisible(boolean moduleNameVisible) {
        mModuleNameVisible = moduleNameVisible;
        return this;
    }

    protected boolean isModuleVersionVisible() {
        return mModuleVersionVisible;
    }

    public TrexOption setModuleVersionVisible(boolean moduleVersionVisible) {
        mModuleVersionVisible = moduleVersionVisible;
        return this;
    }

    protected boolean isByteCodeIndexVisible() {
        return mByteCodeIndexVisible;
    }

    public TrexOption setByteCodeIndexVisible(boolean byteCodeIndexVisible) {
        mByteCodeIndexVisible = byteCodeIndexVisible;
        return this;
    }

    protected boolean isOnlyCompareHashCodeEnabled() {
        return mOnlyCompareHashCodeEnabled;
    }

    public TrexOption setOnlyCompareHashCodeEnabled(boolean onlyCompareHashCodeEnabled) {
        mOnlyCompareHashCodeEnabled = onlyCompareHashCodeEnabled;
        return this;
    }

    protected boolean isCheckDuplicateTraceEnabled() {
        return mCheckDuplicateTraceEnabled;
    }

    public TrexOption setCheckDuplicateTraceEnabled(boolean checkDuplicateTraceEnabled) {
        mCheckDuplicateTraceEnabled = checkDuplicateTraceEnabled;
        return this;
    }

    protected int getDuplicateTraceMaxSize() {
        return mDuplicateTraceMaxSize;
    }

    public TrexOption setDuplicateTraceMaxSize(int duplicateTraceMaxSize) {
        if (duplicateTraceMaxSize <= 0) {
            throw new IllegalArgumentException("size <= 0");
        }
        mDuplicateTraceMaxSize = duplicateTraceMaxSize;
        return this;
    }

    protected boolean isColorSchemeEnabled() {
        return mColorSchemeEnabled;
    }

    public TrexOption setColorSchemeEnabled(boolean colorSchemeEnabled) {
        if (colorSchemeEnabled && mColorScheme == null) {
            applyBaseColorScheme();
        }
        mColorSchemeEnabled = colorSchemeEnabled;
        return this;
    }

    private void ensureColorSchemeEnabled() {
        if (!mColorSchemeEnabled) {
            mColorScheme = new String[COLOR_MAX + 1];
            mColorSchemeEnabled = true;
        }
    }

    public TrexOption applyBaseColorScheme() {
        ensureColorSchemeEnabled();
        setColorUncheck(COLOR_TEXT, 0, 0, 0);
        setColorUncheck(COLOR_PACKAGE_NAME, 38, 5, 208);
        setColorUncheck(COLOR_CLASS_NAME, 38, 5, 196);
        setColorUncheck(COLOR_FILE_NAME, 38, 5, 105);
        setColorUncheck(COLOR_LINE_NUMBER, 0, 0, 0);
        setColorUncheck(COLOR_CLASS_LOADER_NAME, 38, 5, 246);
        setColorUncheck(COLOR_MODULE_NAME, 38, 5, 246);
        setColorUncheck(COLOR_MODULE_VERSION, 38, 5, 246);
        setColorUncheck(COLOR_NUMBER, 38, 5, 250);
        setColorUncheck(COLOR_MESSAGE, 38, 5, 208);
        setColorUncheck(COLOR_CAPTION, 0, 0, 0);
        setColorUncheck(COLOR_AT, 38, 5, 208);
        setColorUncheck(COLOR_PUNCTUATION, 38, 5, 252);
        setColorUncheck(COLOR_DESCRIPTOR_L, 38, 5, 252);
        setColorUncheck(COLOR_DESCRIPTOR_PACKAGE_NAME, 38, 5, 246);
        setColorUncheck(COLOR_DESCRIPTOR_PACKAGE_NAME_SYNTHETIC, 38, 5, 246);
        setColorUncheck(COLOR_DESCRIPTOR_CLASS_NAME, 38, 5, 39);
        setColorUncheck(COLOR_DESCRIPTOR_CLASS_NAME_SYNTHETIC, 38, 5, 252);
        setColorUncheck(COLOR_DESCRIPTOR_METHOD_NAME, 38, 5, 208);
        setColorUncheck(COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC, 38, 5, 252);
        setColorUncheck(COLOR_DESCRIPTOR_PRIMITIVE, 38, 5, 39);
        setColorUncheck(COLOR_DESCRIPTOR_SEMICOLON, 38, 5, 252);
        setColorUncheck(COLOR_DESCRIPTOR_ARROW, 38, 5, 196);
        return this;
    }

    public TrexOption setColor(byte colorId, int z) {
        return setColor(colorId, 38, 5, z);
    }

    public TrexOption setColor(byte colorId, int x, int y, int z) {
        checkColorId(colorId);
        if (0 > x || x > 255 || 0 > y || y > 255 || 0 > z || z > 255) {
            throw new IllegalArgumentException("Invalid color: [" + x + ',' + y + ',' + z + ']');
        }
        setColorUncheck(colorId, x, y, z);
        return this;
    }

    private void checkColorId(byte colorId) {
        if (!mColorSchemeEnabled) {
            throw new IllegalStateException("Color scheme disabled");
        }
        if (COLOR_MIN > colorId || colorId > COLOR_MAX) {
            throw new IllegalArgumentException("Invalid color id: " + colorId);
        }
    }

    private void setColorUncheck(byte colorId, int x, int y, int z) {
        String color;
        if (x == 0 && y == 0 && z == 0) {
            color = "\u001b[0m";
        } else {
            color = "\u001b[" + x + ';' + y + ';' + z + 'm';
        }
        mColorScheme[colorId] = color;
    }

    public TrexOption setColor(byte colorId, String color) {
        Utils.requireNonNull(color, "color");
        checkColorId(colorId);
        mColorScheme[colorId] = color;
        return this;
    }

    protected String getColor(byte colorId) {
        if (mColorSchemeEnabled) {
            assert COLOR_MIN <= colorId && colorId <= COLOR_MAX;
            return mColorScheme[colorId];
        }
        return "";
    }

    protected int computeStackFrameOptionHashCode() {
        int hash = 17;
        hash = 31 * hash + mStyle.hashCode();
        hash = 31 * hash + Utils.hashCode(mTab);
        hash = 31 * hash + Utils.hashCode(mBootMethodTypeVisible);
        hash = 31 * hash + Utils.hashCode(mSynthesizedMethodTypeVisible);
        hash = 31 * hash + Utils.hashCode(mUniqueMethodTypeVisible);
        hash = 31 * hash + (mColorSchemeEnabled ? Arrays.hashCode(mColorScheme) : 0);
        return hash;
    }

    @Override
    public TrexOption clone() {
        return new TrexOption(this);
    }
}
