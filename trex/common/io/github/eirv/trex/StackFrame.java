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

import java.lang.reflect.Member;

/**
 * @author Eirv
 * @date 2023/2/7 12:19
 */
public interface StackFrame {
    String getDescriptor();

    String getClassName();

    String getMethodName();

    String getFileName();

    int getLineNumber();

    boolean isNativeMethod();

    Class<?> getDeclaringClass();

    MethodTypeCompat getMethodType();

    String getModuleName();

    // 仅支持 jvm, 安卓永远返回 null
    String getModuleVersion();

    // 仅支持 jvm, 安卓永远返回 null
    String getClassLoaderName();

    // uint32_t, 仅支持安卓
    int getByteCodeIndex();

    StackTraceElement toStackTraceElement();

    <T extends Member> T toExecutable();

    StackFrame proxy();

    StackFrame proxy(boolean alwaysProxy);

    StackFrame clone(int lineNumber, int byteCodeIndex);

    String toString(TrexOption option);
}
