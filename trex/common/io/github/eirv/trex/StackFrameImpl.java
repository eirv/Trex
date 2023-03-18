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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * @author Eirv
 * @date 2023/2/7 12:21
 */
final class StackFrameImpl implements StackFrame, Externalizable {
    private static final long serialVersionUID = -3958024750613849257L;

    String descriptor;
    String className;
    String methodName;
    String fileName;
    int lineNumber;
    String moduleName;
    String moduleVersion;
    String classLoaderName;
    int byteCodeIndex;

    transient Object vmMethod;
    transient Member executable;
    private transient StackTraceElement stackTrace;
    private transient int hash = -1;

    public StackFrameImpl() {}

    public StackFrameImpl(
            StackTraceElement stackTrace,
            String descriptor,
            String moduleName,
            int byteCodeIndex,
            Object vmMethod) {
        this(
                descriptor,
                stackTrace.getClassName(),
                stackTrace.getMethodName(),
                stackTrace.getFileName(),
                stackTrace.getLineNumber(),
                moduleName,
                byteCodeIndex,
                vmMethod);
    }

    public StackFrameImpl(
            String descriptor,
            String className,
            String methodName,
            String fileName,
            int lineNumber,
            String moduleName,
            int byteCodeIndex,
            Object vmMethod) {
        this.descriptor = descriptor;
        this.className = className;
        this.methodName = methodName;
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.moduleName = moduleName;
        this.byteCodeIndex = byteCodeIndex;
        this.vmMethod = vmMethod;
    }

    private StackFrameImpl(StackFrameImpl orig, int lineNumber, int byteCodeIndex) {
        descriptor = orig.descriptor;
        className = orig.className;
        methodName = orig.methodName;
        fileName = orig.fileName;
        moduleName = orig.moduleName;
        moduleVersion = orig.moduleVersion;
        classLoaderName = orig.classLoaderName;
        vmMethod = orig.vmMethod;
        executable = orig.executable;
        stackTrace = orig.stackTrace;
        this.lineNumber = lineNumber;
        this.byteCodeIndex = byteCodeIndex;
    }

    static Class<?> getDeclaringClass(StackFrame stackFrame, Object vmMethod) {
        Platform platform = Trex.ensurePlatformInitialized();
        Class<?> declaringClass = platform.getDeclaringClass0(vmMethod);
        if (declaringClass == null) {
            Member executable = stackFrame.toExecutable();
            if (executable != null) {
                declaringClass = executable.getDeclaringClass();
            }
        }
        return declaringClass;
    }

    static MethodTypeCompat getMethodType(StackFrame stackFrame) {
        Member executable = stackFrame.toExecutable();
        if (executable == null) return null;
        Class<?> returnType;
        Class<?>[] parameterTypes;
        if (executable instanceof Method) {
            Method method = (Method) executable;
            returnType = method.getReturnType();
            parameterTypes = method.getParameterTypes();
        } else {
            Constructor<?> constructor = (Constructor<?>) executable;
            returnType = Void.TYPE;
            parameterTypes = constructor.getParameterTypes();
        }
        return new MethodTypeCompat(returnType, parameterTypes);
    }

    static Member toExecutable(Object vmMethod) {
        return Trex.ensurePlatformInitialized().getExecutable0(vmMethod);
    }

    static boolean equals(StackFrame a, Object b) {
        if (!(b instanceof StackFrame)) return false;
        StackFrame peer = (StackFrame) b;

        return a.getLineNumber() == peer.getLineNumber()
                && a.getByteCodeIndex() == peer.getByteCodeIndex()
                && a.getDescriptor().equals(peer.getDescriptor())
                && Utils.equals(a.getFileName(), peer.getFileName());
    }

    @Override
    public String getDescriptor() {
        return descriptor;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public String getMethodName() {
        return methodName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public int getLineNumber() {
        return lineNumber;
    }

    @Override
    public boolean isNativeMethod() {
        return lineNumber == -2;
    }

    @Override
    public Class<?> getDeclaringClass() {
        return getDeclaringClass(this, vmMethod);
    }

    @Override
    public MethodTypeCompat getMethodType() {
        return getMethodType(this);
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getModuleVersion() {
        return moduleVersion;
    }

    @Override
    public String getClassLoaderName() {
        return classLoaderName;
    }

    @Override
    public int getByteCodeIndex() {
        return byteCodeIndex;
    }

    @Override
    public StackTraceElement toStackTraceElement() {
        if (stackTrace == null) {
            stackTrace =
                    Trex.ensurePlatformInitialized()
                            .newStackTraceElement0(
                                    className,
                                    methodName,
                                    fileName,
                                    lineNumber,
                                    classLoaderName,
                                    moduleName,
                                    moduleVersion);
            if (stackTrace == null) {
                stackTrace = new StackTraceElement(className, methodName, fileName, lineNumber);
            }
        }
        return stackTrace;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Member> T toExecutable() {
        if (executable == null) {
            executable = toExecutable(vmMethod);
        }
        return (T) executable;
    }

    @Override
    public StackFrame proxy() {
        return this;
    }

    @Override
    public StackFrame proxy(boolean alwaysProxy) {
        return this;
    }

    @Override
    public StackFrame clone(int lineNumber, int byteCodeIndex) {
        return new StackFrameImpl(this, lineNumber, byteCodeIndex);
    }

    @Override
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        descriptor = in.readUTF();
        className = in.readUTF();
        methodName = in.readUTF();
        fileName = (String) in.readObject();
        lineNumber = in.readInt();
        moduleName = (String) in.readObject();
        moduleVersion = (String) in.readObject();
        classLoaderName = (String) in.readObject();
        byteCodeIndex = in.readInt();
    }

    @Override
    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeUTF(descriptor);
        out.writeUTF(className);
        out.writeUTF(methodName);
        out.writeObject(fileName);
        out.writeInt(lineNumber);
        out.writeObject(moduleName);
        out.writeObject(moduleVersion);
        out.writeObject(classLoaderName);
        out.writeInt(byteCodeIndex);
    }

    @Override
    public int hashCode() {
        int result = hash;
        if (result == -1) {
            result = 17;
            result = 31 * result + descriptor.hashCode();
            result = 31 * result + Utils.hashCode(fileName);
            result = 31 * result + lineNumber;
            result = 31 * result + byteCodeIndex;
            hash = result;
        }
        return result;
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        return equals(this, obj);
    }

    @Override
    public String toString() {
        return toString(null);
    }

    @Override
    public String toString(TrexOption option) {
        option = Utils.select(option, TrexOption.getDefault());
        return option.getStyle().toString(this, option);
    }
}
