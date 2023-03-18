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
 * @date 2023/2/7 12:52
 */
class VmMethodKey {
    private final Object vmMethod;
    private final int stackFrameOptionHashCode;
    private final boolean incomplete;
    private final int hash;

    public VmMethodKey(Object vmMethod, int stackFrameOptionHashCode, boolean incomplete) {
        this.vmMethod = vmMethod;
        this.stackFrameOptionHashCode = stackFrameOptionHashCode;
        this.incomplete = incomplete;

        int hash = 17;
        hash = 31 * hash + vmMethod.hashCode();
        hash = 31 * hash + stackFrameOptionHashCode;
        hash = 31 * hash + Utils.hashCode(incomplete);
        this.hash = hash;
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof VmMethodKey)) return false;

        VmMethodKey peer = (VmMethodKey) obj;
        return vmMethod.equals(peer.vmMethod)
                && stackFrameOptionHashCode == peer.stackFrameOptionHashCode
                && incomplete == peer.incomplete;
    }
}
