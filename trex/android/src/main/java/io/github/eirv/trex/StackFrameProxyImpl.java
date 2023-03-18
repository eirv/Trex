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
import java.lang.reflect.Member;

/**
 * @author Eirv
 * @date 2023/2/7 12:51
 */
final class StackFrameProxyImpl extends StackTraceElement implements StackFrame, Serializable {
    private static final long serialVersionUID = 8379546280965435804L;

    private final String descriptor;
    private final String moduleName;
    private final int byteCodeIndex;
    private final transient Object vmMethod;
    private transient Member executable;
    private transient int proxy;
    private transient int hash = -1;

    public StackFrameProxyImpl(
            String descriptor,
            String className,
            String methodName,
            String fileName,
            int lineNumber,
            String moduleName,
            int byteCodeIndex,
            Object vmMethod) {
        super(className, methodName, fileName, lineNumber);
        this.descriptor = descriptor;
        this.moduleName = moduleName;
        this.byteCodeIndex = byteCodeIndex;
        this.vmMethod = vmMethod;
    }

    public StackFrameProxyImpl(StackFrameImpl peer) {
        this(
                peer.descriptor,
                peer.className,
                peer.methodName,
                peer.fileName,
                peer.lineNumber,
                peer.moduleName,
                peer.byteCodeIndex,
                peer.vmMethod);
        executable = peer.executable;
    }

    private StackFrameProxyImpl(StackFrameProxyImpl orig, int lineNumber, int byteCodeIndex) {
        this(
                orig.descriptor,
                orig.getClassName(),
                orig.getMethodName(),
                orig.getFileName(),
                lineNumber,
                orig.moduleName,
                byteCodeIndex,
                orig.vmMethod);
        executable = orig.executable;
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return StackFrameImpl.getDeclaringClass(this, vmMethod);
    }

    @Override
    public MethodTypeCompat getMethodType() {
        return StackFrameImpl.getMethodType(this);
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getModuleVersion() {
        return null;
    }

    @Override
    public String getClassLoaderName() {
        return null;
    }

    @Override
    public int getByteCodeIndex() {
        return byteCodeIndex;
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Member> T toExecutable() {
        if (executable == null) {
            executable = StackFrameImpl.toExecutable(vmMethod);
        }
        return (T) executable;
    }

    @Override
    public StackFrame proxy() {
        if (proxy != -1) proxy++;
        return this;
    }

    @Override
    public StackFrame proxy(boolean alwaysProxy) {
        proxy = alwaysProxy ? -1 : 0;
        return this;
    }

    @Override
    public StackFrameProxyImpl clone(int lineNumber, int byteCodeIndex) {
        return new StackFrameProxyImpl(this, lineNumber, byteCodeIndex);
    }

    @Override
    public int hashCode() {
        if (!TrexAndroidImpl.isStackTraceProxyEnabled()) {
            if (proxy == 0) {
                return super.hashCode();
            } else if (proxy > 0) proxy--;
        }

        int result = hash;
        if (result == -1) {
            result = 17;
            result = 31 * result + descriptor.hashCode();
            result = 31 * result + Utils.hashCode(getFileName());
            result = 31 * result + getLineNumber();
            result = 31 * result + byteCodeIndex;
            hash = result;
        }
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;

        if (!TrexAndroidImpl.isStackTraceProxyEnabled()) {
            if (proxy == 0) {
                if (!(obj instanceof StackTraceElement)) return false;
                StackTraceElement peer = (StackTraceElement) obj;

                return getClassName().equals(peer.getClassName())
                        && getMethodName().equals(peer.getMethodName())
                        && Utils.equals(getFileName(), peer.getFileName())
                        && getLineNumber() == peer.getLineNumber();
            } else if (proxy > 0) proxy--;
        }

        return StackFrameImpl.equals(this, obj);
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public String toString(TrexOption option) {
        if (!TrexAndroidImpl.isStackTraceProxyEnabled()) {
            if (proxy == 0) {
                return super.toString();
            } else if (proxy > 0) proxy--;
        }

        option = Utils.select(option, TrexOption.getDefault());
        return option.getStyle().toString((StackFrame) this, option);
    }
}
