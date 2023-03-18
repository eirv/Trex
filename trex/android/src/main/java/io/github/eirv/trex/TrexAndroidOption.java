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

/**
 * @author Eirv
 * @date 2023/2/7 12:51
 */
public class TrexAndroidOption extends TrexOption {
    private boolean mModuleNameFinderEnabled = true;
    private boolean mProxyImplEnabled;
    private boolean mDalvikAccelerateEnabled;

    public TrexAndroidOption() {}

    protected TrexAndroidOption(TrexAndroidOption orig) {
        super(orig);
        mModuleNameFinderEnabled = orig.mModuleNameFinderEnabled;
        mProxyImplEnabled = orig.mProxyImplEnabled;
        mDalvikAccelerateEnabled = orig.mDalvikAccelerateEnabled;
    }

    private TrexAndroidOption(TrexOption option) {
        super(option);
    }

    static TrexAndroidOption clone(TrexOption option) {
        return new TrexAndroidOption(option);
    }

    protected boolean isModuleNameFinderEnabled() {
        return mModuleNameFinderEnabled;
    }

    public TrexOption setModuleNameFinderEnabled(boolean moduleNameFinderEnabled) {
        mModuleNameFinderEnabled = moduleNameFinderEnabled;
        return this;
    }

    protected boolean isProxyImplEnabled() {
        return mProxyImplEnabled;
    }

    public TrexAndroidOption setProxyImplEnabled(boolean proxyImplEnabled) {
        mProxyImplEnabled = proxyImplEnabled;
        return this;
    }

    protected boolean isDalvikAccelerateEnabled() {
        return mDalvikAccelerateEnabled;
    }

    public TrexAndroidOption setDalvikAccelerateEnabled(boolean dalvikAccelerateEnabled) {
        if (dalvikAccelerateEnabled) {
            setStyle(TrexStyle.DEFAULT);
        }
        mDalvikAccelerateEnabled = dalvikAccelerateEnabled;
        return this;
    }

    @Override
    public TrexOption setStyle(TrexStyle style) {
        if (mDalvikAccelerateEnabled && style != TrexStyle.DEFAULT) return this;
        return super.setStyle(style);
    }

    @Override
    protected int computeStackFrameOptionHashCode() {
        int hash = super.computeStackFrameOptionHashCode();
        hash = 31 * hash + Utils.hashCode(mModuleNameFinderEnabled);
        return hash;
    }

    @Override
    public TrexAndroidOption clone() {
        return new TrexAndroidOption(this);
    }
}
