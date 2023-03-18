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

import java.io.Serializable;
import java.util.concurrent.Callable;

/**
 * @author Eirv
 * @date 2023/2/7 12:54
 */
class StackTraceElementExt extends StackTraceElement implements Callable<Object>, Serializable {
    private static final long serialVersionUID = 2671308546913276480L;

    private final transient Object backTrace;

    public StackTraceElementExt(StackTraceElement stackTrace, Object backTrace) {
        super(
                stackTrace.getClassName(),
                stackTrace.getMethodName(),
                stackTrace.getFileName(),
                stackTrace.getLineNumber());
        this.backTrace = backTrace;
    }

    @Override
    public Object call() {
        return backTrace;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof StackTraceElement)) return false;
        StackTraceElement peer = (StackTraceElement) obj;

        return getLineNumber() == getLineNumber()
                && getClassName().equals(peer.getClassName())
                && getMethodName().equals(peer.getMethodName())
                && Utils.equals(getFileName(), peer.getFileName());
    }
}
