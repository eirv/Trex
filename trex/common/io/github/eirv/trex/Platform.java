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
import java.util.List;

/**
 * @author Eirv
 * @date 2023/2/7 12:21
 */
interface Platform {
    boolean isInitialized0();

    Object getBackTrace0(Throwable throwable);

    void setBackTrace0(Throwable throwable, Object backTrace);

    void setDepth0(Throwable throwable, int depth);

    Object getBackTrace0(StackTraceElement stackTrace);

    BackTraceParser newBackTraceParser0(
            StackTraceElement[] stackTraces,
            Object backTraces,
            TrexOption option,
            int stackFrameOptionHashCode);

    Class<?> getDeclaringClass0(Object vmMethod);

    Member getExecutable0(Object vmMethod);

    String getModuleName0(Class<?> clazz);

    List<Class<?>> getCallerClasses0(Object backTrace);

    StackTraceElement newStackTraceElement0(
            String declaringClass,
            String methodName,
            String fileName,
            int lineNumber,
            String classLoaderName,
            String moduleName,
            String moduleVersion);

    void printStackTraceInfoPrefix0(
            TrexPrinter printer, StackTraceElement stackTrace, TrexOption option);

    TrexOption cloneOption0(TrexOption option);
}
