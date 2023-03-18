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

import static io.github.eirv.trex.TrexOption.COLOR_CLASS_LOADER_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_ARROW;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_CLASS_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_CLASS_NAME_SYNTHETIC;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_L;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_METHOD_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_PACKAGE_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_PACKAGE_NAME_SYNTHETIC;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_PRIMITIVE;
import static io.github.eirv.trex.TrexOption.COLOR_DESCRIPTOR_SEMICOLON;
import static io.github.eirv.trex.TrexOption.COLOR_FILE_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_LINE_NUMBER;
import static io.github.eirv.trex.TrexOption.COLOR_MODULE_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_MODULE_VERSION;
import static io.github.eirv.trex.TrexOption.COLOR_NUMBER;
import static io.github.eirv.trex.TrexOption.COLOR_PUNCTUATION;
import static io.github.eirv.trex.TrexOption.COLOR_TEXT;

import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;

/**
 * @author Eirv
 * @date 2023/2/7 12:19
 */
public abstract class TrexStyle {
    protected static final int FLAG_BOOT_CLASS_LOADER = 1 << 0;
    protected static final int FLAG_CLASS_SYNTHETIC = 1 << 1;
    protected static final int FLAG_METHOD_SYNTHETIC = 1 << 2;
    protected static final int FLAG_EXECUTABLE_UNIQUE = 1 << 3;

    public static final TrexStyle DEFAULT =
            new TrexStyle() {
                private void printClassSignature(TrexPrinter p, Class<?> clazz) {
                    if (clazz.isPrimitive()) {
                        printPrimitiveClassSignature(p, clazz);
                    } else {
                        String name = clazz.getName().replace('.', '/');
                        if (clazz.isArray()) {
                            p.print(name);
                        } else {
                            p.print('L');
                            p.print(name);
                            p.print(';');
                        }
                    }
                }

                private void printColorfulClassSignature(TrexPrinter p, Class<?> clazz) {
                    if (clazz.isPrimitive()) {
                        p.color(COLOR_DESCRIPTOR_PRIMITIVE);
                        printPrimitiveClassSignature(p, clazz);
                    } else {
                        boolean synthetic = clazz.isSynthetic();
                        String name = clazz.getName();
                        if (clazz.isArray()) {
                            int index = name.lastIndexOf('[');
                            p.color(COLOR_PUNCTUATION);
                            p.print(name.substring(0, ++index));
                            if (name.charAt(index) == 'L') {
                                name = name.substring(index + 1, name.length() - 1);
                                printClassNameSignature(p, name, synthetic);
                            } else {
                                p.color(COLOR_DESCRIPTOR_PRIMITIVE);
                                p.print(name.charAt(index));
                            }
                        } else {
                            printClassNameSignature(p, name, synthetic);
                        }
                    }
                }

                private void printPrimitiveClassSignature(TrexPrinter p, Class<?> clazz) {
                    char ch;
                    if (clazz == Boolean.TYPE) ch = 'Z';
                    else if (clazz == Byte.TYPE) ch = 'B';
                    else if (clazz == Short.TYPE) ch = 'S';
                    else if (clazz == Character.TYPE) ch = 'C';
                    else if (clazz == Integer.TYPE) ch = 'I';
                    else if (clazz == Float.TYPE) ch = 'F';
                    else if (clazz == Long.TYPE) ch = 'J';
                    else if (clazz == Double.TYPE) ch = 'D';
                    else if (clazz == Void.TYPE) ch = 'V';
                    // WTF ???
                    else throw new AssertionError();
                    p.print(ch);
                }

                private void printClassNameSignature(
                        TrexPrinter p, String className, boolean synthetic) {
                    p.color(COLOR_DESCRIPTOR_L);
                    p.print('L');
                    String[] splits = Utils.splitClassName(className);
                    int last = splits.length - 1;
                    for (int i = 0; last > i; i++) {
                        p.color(
                                synthetic
                                        ? COLOR_DESCRIPTOR_PACKAGE_NAME_SYNTHETIC
                                        : COLOR_DESCRIPTOR_PACKAGE_NAME);
                        p.print(splits[i]);
                        p.color(COLOR_PUNCTUATION);
                        p.print('/');
                    }
                    p.color(
                            synthetic
                                    ? COLOR_DESCRIPTOR_CLASS_NAME_SYNTHETIC
                                    : COLOR_DESCRIPTOR_CLASS_NAME);
                    p.print(splits[last]);
                    p.color(COLOR_DESCRIPTOR_SEMICOLON);
                    p.print(';');
                }

                @Override
                protected String getDescriptor(Member executable, TrexOption option) {
                    Class<?> declaringClass = executable.getDeclaringClass();
                    Class<?> returnType;
                    String name;
                    Class<?>[] parameterTypes;
                    boolean bridge;

                    if (executable instanceof Method) {
                        Method method = (Method) executable;
                        returnType = method.getReturnType();
                        name = method.getName();
                        parameterTypes = method.getParameterTypes();
                        bridge = method.isBridge();
                    } else {
                        Constructor<?> constructor = (Constructor<?>) executable;
                        returnType = Void.TYPE;
                        name = "<init>";
                        parameterTypes = constructor.getParameterTypes();
                        bridge = false;
                    }

                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    if (option.isColorSchemeEnabled()) {
                        printColorfulClassSignature(p, declaringClass);
                        p.color(COLOR_DESCRIPTOR_ARROW);
                        p.print("->");
                        p.color(
                                executable.isSynthetic() || bridge
                                        ? COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC
                                        : COLOR_DESCRIPTOR_METHOD_NAME);
                        p.print(name);
                        p.color(COLOR_PUNCTUATION);
                        p.print('(');
                        for (Class<?> parameterType : parameterTypes) {
                            printColorfulClassSignature(p, parameterType);
                        }
                        p.color(COLOR_PUNCTUATION);
                        p.print(')');
                        printColorfulClassSignature(p, returnType);
                        p.color(COLOR_TEXT);
                    } else {
                        printClassSignature(p, declaringClass);
                        p.print("->");
                        p.print(name);
                        p.print('(');
                        for (Class<?> parameterType : parameterTypes) {
                            printClassSignature(p, parameterType);
                        }
                        p.print(')');
                        printClassSignature(p, returnType);
                    }
                    return p.toString();
                }

                @Override
                protected String getDescriptor(
                        StackTraceElement stackTrace, TrexOption option, int flags) {
                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    printDescriptor(p, stackTrace, option, flags);
                    return p.toString();
                }

                private void printDescriptor(
                        TrexPrinter p, StackTraceElement stackTrace, TrexOption option, int flags) {
                    String className = stackTrace.getClassName();
                    String methodName = stackTrace.getMethodName();
                    boolean syntheticClass = (flags & FLAG_CLASS_SYNTHETIC) != 0;
                    boolean syntheticMethod = (flags & FLAG_METHOD_SYNTHETIC) != 0;

                    if (option.isColorSchemeEnabled()) {
                        printClassNameSignature(p, className, syntheticClass);
                        p.color(COLOR_DESCRIPTOR_ARROW);
                        p.print("->");
                        p.color(
                                syntheticMethod
                                        ? COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC
                                        : COLOR_DESCRIPTOR_METHOD_NAME);
                        p.print(methodName);
                        p.color(COLOR_PUNCTUATION);
                        p.print("(?)?");
                        p.color(COLOR_TEXT);
                    } else {
                        p.print('L');
                        p.print(className.replace('.', '/'));
                        p.print(";->");
                        p.print(methodName);
                        p.print("(?)?");
                    }
                }

                @Override
                protected String toString(StackFrame stackFrame, TrexOption option) {
                    String descriptor = stackFrame.getDescriptor();
                    String fileName = stackFrame.getFileName();
                    int lineNumber = stackFrame.getLineNumber();
                    boolean nativeMethod = stackFrame.isNativeMethod();
                    String moduleName = stackFrame.getModuleName();
                    String moduleVersion = stackFrame.getModuleVersion();
                    String classLoaderName = stackFrame.getClassLoaderName();
                    int byteCodeIndex = stackFrame.getByteCodeIndex();

                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    p.print(descriptor);
                    if (option.isColorSchemeEnabled()) {
                        p.color(COLOR_PUNCTUATION);
                        p.print("  [");
                        printStackTraceInfoPrefix(
                                p, classLoaderName, moduleName, moduleVersion, option);
                        p.color(COLOR_FILE_NAME);
                        if (fileName != null) {
                            p.color(COLOR_FILE_NAME);
                            p.print(fileName);
                        } else {
                            p.color(COLOR_PUNCTUATION);
                            p.print("???");
                        }
                        if (nativeMethod) {
                            p.color(COLOR_PUNCTUATION);
                            p.print("::");
                            p.color(COLOR_TEXT);
                            p.print("native");
                        } else {
                            boolean lineNumberAvailable =
                                    lineNumber > 0 && lineNumber != byteCodeIndex;
                            boolean byteCodeIndexAvailable =
                                    option.isByteCodeIndexVisible()
                                            && byteCodeIndex != -1
                                            && byteCodeIndex != 0;

                            if (lineNumberAvailable || byteCodeIndexAvailable) {
                                p.color(COLOR_PUNCTUATION);
                                p.print(':');
                                if (lineNumberAvailable) {
                                    p.color(COLOR_LINE_NUMBER);
                                    p.print(lineNumber);
                                    if (byteCodeIndexAvailable) {
                                        p.color(COLOR_PUNCTUATION);
                                        p.print(':');
                                        p.color(COLOR_NUMBER);
                                        p.print(byteCodeIndex & 0xFFFFFFFFL);
                                    }
                                } else {
                                    p.print(':');
                                    p.color(COLOR_TEXT);
                                    p.print(Utils.ANDROID ? "PC" : "BCI");
                                    p.color(COLOR_PUNCTUATION);
                                    p.print('-');
                                    p.color(COLOR_NUMBER);
                                    p.print(byteCodeIndex & 0xFFFFFFFFL);
                                }
                            }
                        }
                        p.color(COLOR_PUNCTUATION);
                        p.print(']');
                        p.color(COLOR_TEXT);
                    } else {
                        p.print("  [");
                        printStackTraceInfoPrefix(
                                p, classLoaderName, moduleName, moduleVersion, option);
                        p.print(fileName != null ? fileName : "???");
                        if (nativeMethod) {
                            p.print("::native");
                        } else {
                            boolean lineNumberAvailable =
                                    lineNumber > 0 && lineNumber != byteCodeIndex;
                            boolean byteCodeIndexAvailable =
                                    option.isByteCodeIndexVisible()
                                            && byteCodeIndex != -1
                                            && byteCodeIndex != 0;

                            if (lineNumberAvailable || byteCodeIndexAvailable) {
                                p.print(':');
                                if (lineNumberAvailable) {
                                    p.print(lineNumber);
                                    if (byteCodeIndexAvailable) {
                                        p.print(':');
                                        p.print(byteCodeIndex & 0xFFFFFFFFL);
                                    }
                                } else {
                                    p.print(Utils.ANDROID ? ":PC-" : ":BCI-");
                                    p.print(byteCodeIndex & 0xFFFFFFFFL);
                                }
                            }
                        }
                        p.print(']');
                    }
                    return p.toString();
                }

                @Override
                protected String toString(StackTraceElement stackTrace, TrexOption option) {
                    Platform platform = Trex.ensurePlatformInitialized();
                    String fileName = stackTrace.getFileName();
                    int lineNumber = stackTrace.getLineNumber();
                    boolean nativeMethod = stackTrace.isNativeMethod();

                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    printDescriptor(p, stackTrace, option, 0);
                    if (option.isColorSchemeEnabled()) {
                        p.color(COLOR_PUNCTUATION);
                        p.print("  [");
                        platform.printStackTraceInfoPrefix0(p, stackTrace, option);
                        if (fileName != null) {
                            p.color(COLOR_FILE_NAME);
                            p.print(fileName);
                        } else {
                            p.color(COLOR_PUNCTUATION);
                            p.print("???");
                        }
                        if (nativeMethod) {
                            p.color(COLOR_PUNCTUATION);
                            p.print("::");
                            p.color(COLOR_TEXT);
                            p.print("native");
                        } else if (lineNumber > 0) {
                            p.color(COLOR_PUNCTUATION);
                            p.print(':');
                            p.color(COLOR_LINE_NUMBER);
                            p.print(lineNumber);
                        }
                        p.color(COLOR_PUNCTUATION);
                        p.print(']');
                        p.color(COLOR_TEXT);
                    } else {
                        p.print("  [");
                        platform.printStackTraceInfoPrefix0(p, stackTrace, option);
                        p.print(fileName != null ? fileName : "???");
                        if (nativeMethod) {
                            p.print("::native");
                        } else if (lineNumber > 0) {
                            p.print(':');
                            p.print(lineNumber);
                        }
                        p.print(']');
                    }
                    return p.toString();
                }

                @Override
                protected String tab() {
                    return "    ";
                }

                @Override
                protected String at() {
                    return "-> ";
                }

                @Override
                protected String atDuplicate() {
                    return "-> -- ";
                }
            };

    public static final TrexStyle JNI =
            new TrexStyle() {
                private void printClassNameSignature(
                        TrexPrinter p, String className, boolean synthetic) {
                    String[] splits = Utils.splitClassName(className);
                    int last = splits.length - 1;
                    for (int i = 0; last > i; i++) {
                        p.color(
                                synthetic
                                        ? COLOR_DESCRIPTOR_PACKAGE_NAME_SYNTHETIC
                                        : COLOR_DESCRIPTOR_PACKAGE_NAME);
                        p.print(splits[i]);
                        p.color(COLOR_PUNCTUATION);
                        p.print('.');
                    }
                    p.color(
                            synthetic
                                    ? COLOR_DESCRIPTOR_CLASS_NAME_SYNTHETIC
                                    : COLOR_DESCRIPTOR_CLASS_NAME);
                    p.print(splits[last]);
                }

                private void printCanonicalName(TrexPrinter p, Class<?> clazz, TrexOption option) {
                    if (clazz.isArray()) {
                        StringPrinter suffix = new StringPrinter();
                        suffix.option(option);
                        suffix.color(COLOR_PUNCTUATION);
                        do {
                            suffix.print("[]");
                            clazz = clazz.getComponentType();
                            assert clazz != null;
                        } while (clazz.isArray());
                        if (clazz.isPrimitive()) {
                            p.color(COLOR_DESCRIPTOR_PRIMITIVE);
                        }
                        p.print(clazz.getName());
                        p.print(suffix.toString());
                    } else {
                        String name = clazz.getName();
                        if (option.isColorSchemeEnabled()) {
                            printClassNameSignature(p, name, clazz.isSynthetic());
                        } else {
                            p.print(name);
                        }
                    }
                }

                @Override
                protected String getDescriptor(Member executable, TrexOption option) {
                    Class<?> returnType;
                    Class<?> declaringClass = executable.getDeclaringClass();
                    String name;
                    Class<?>[] parameterTypes;
                    int parameterCount;
                    boolean bridge;

                    if (executable instanceof Method) {
                        Method method = (Method) executable;
                        returnType = method.getReturnType();
                        name = method.getName();
                        parameterTypes = method.getParameterTypes();
                        bridge = method.isBridge();
                    } else {
                        Constructor<?> constructor = (Constructor<?>) executable;
                        returnType = Void.TYPE;
                        name = "<init>";
                        parameterTypes = constructor.getParameterTypes();
                        bridge = false;
                    }
                    parameterCount = parameterTypes.length;

                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    if (option.isColorSchemeEnabled()) {
                        printCanonicalName(p, returnType, option);
                        p.print(' ');
                        printCanonicalName(p, declaringClass, option);
                        p.color(COLOR_DESCRIPTOR_ARROW);
                        p.print('.');
                        p.color(
                                executable.isSynthetic() || bridge
                                        ? COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC
                                        : COLOR_DESCRIPTOR_METHOD_NAME);
                        p.print(name);
                        p.color(COLOR_PUNCTUATION);
                        p.print('(');
                        if (parameterCount != 0) {
                            printCanonicalName(p, parameterTypes[0], option);
                            for (int i = 1; parameterCount > i; i++) {
                                p.color(COLOR_PUNCTUATION);
                                p.print(", ");
                                printCanonicalName(p, parameterTypes[i], option);
                            }
                        }
                        p.color(COLOR_PUNCTUATION);
                    } else {
                        printCanonicalName(p, returnType, option);
                        p.print(' ');
                        printCanonicalName(p, declaringClass, option);
                        p.print('.');
                        p.print(name);
                        p.print('(');
                        if (parameterCount != 0) {
                            printCanonicalName(p, parameterTypes[0], option);
                            for (int i = 1; parameterCount > i; i++) {
                                p.print(", ");
                                printCanonicalName(p, parameterTypes[i], option);
                            }
                        }
                    }
                    p.print(')');
                    return p.toString();
                }

                @Override
                protected String getDescriptor(
                        StackTraceElement stackTrace, TrexOption option, int flags) {
                    String className = stackTrace.getClassName();
                    String methodName = stackTrace.getMethodName();
                    boolean syntheticClass = (flags & FLAG_CLASS_SYNTHETIC) != 0;
                    boolean syntheticMethod = (flags & FLAG_METHOD_SYNTHETIC) != 0;

                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    if (option.isColorSchemeEnabled()) {
                        printClassNameSignature(p, className, syntheticClass);
                        p.color(COLOR_DESCRIPTOR_ARROW);
                        p.print('.');
                        p.color(
                                syntheticMethod
                                        ? COLOR_DESCRIPTOR_METHOD_NAME_SYNTHETIC
                                        : COLOR_DESCRIPTOR_METHOD_NAME);
                    } else {
                        p.print(className);
                        p.print('.');
                    }
                    p.print(methodName);
                    return p.toString();
                }

                @Override
                protected String toString(StackFrame stackFrame, TrexOption option) {
                    String descriptor = stackFrame.getDescriptor();
                    String fileName = stackFrame.getFileName();
                    int lineNumber = stackFrame.getLineNumber();
                    String classLoaderName = stackFrame.getClassLoaderName();
                    String moduleName = stackFrame.getModuleName();
                    String moduleVersion = stackFrame.getModuleVersion();

                    StringPrinter p = new StringPrinter();
                    p.option(option);
                    if (option.isColorSchemeEnabled()) {
                        p.print(descriptor);
                        p.color(COLOR_PUNCTUATION);
                        p.print(" (");
                        printStackTraceInfoPrefix(
                                p, classLoaderName, moduleName, moduleVersion, option);
                        if (fileName != null) {
                            p.color(COLOR_FILE_NAME);
                            p.print(fileName);
                        } else {
                            p.color(COLOR_PUNCTUATION);
                            p.print('(');
                            p.color(COLOR_FILE_NAME);
                            p.print("null");
                            p.color(COLOR_PUNCTUATION);
                            p.print(')');
                        }
                        p.color(COLOR_PUNCTUATION);
                        p.print(':');
                        p.color(COLOR_LINE_NUMBER);
                        p.print(lineNumber);
                        p.color(COLOR_PUNCTUATION);
                        p.print(')');
                        p.color(COLOR_TEXT);
                    } else {
                        p.print(descriptor);
                        p.print(" (");
                        printStackTraceInfoPrefix(
                                p, classLoaderName, moduleName, moduleVersion, option);
                        if (fileName != null) {
                            p.print(fileName);
                        } else {
                            p.print("(null)");
                        }
                        p.print(':');
                        p.print(lineNumber);
                        p.print(')');
                    }
                    return p.toString();
                }

                @Override
                protected String toString(StackTraceElement stackTrace, TrexOption option) {
                    return stackTrace.toString();
                }

                @Override
                protected String tab() {
                    return "\t";
                }

                @Override
                protected String at() {
                    return "at ";
                }

                @Override
                protected String atDuplicate() {
                    return "at -- ";
                }
            };

    protected TrexStyle() {}

    static void printStackTraceInfoPrefix(
            TrexPrinter p,
            String classLoaderName,
            String moduleName,
            String moduleVersion,
            TrexOption option) {

        if (option.isClassLoaderNameVisible() && Utils.isNotEmpty(classLoaderName)) {
            p.color(COLOR_CLASS_LOADER_NAME);
            p.print(classLoaderName);
            p.color(COLOR_PUNCTUATION);
            p.print('/');
        }
        if (option.isModuleNameVisible() && Utils.isNotEmpty(moduleName)) {
            p.color(COLOR_MODULE_NAME);
            p.print(moduleName);
            if (option.isModuleVersionVisible() && Utils.isNotEmpty(moduleVersion)) {
                p.color(COLOR_PUNCTUATION);
                p.print('@');
                p.color(COLOR_MODULE_VERSION);
                p.print(moduleVersion);
            }
            p.color(COLOR_PUNCTUATION);
            p.print('/');
        }
    }

    protected abstract String getDescriptor(Member executable, TrexOption option);

    protected abstract String getDescriptor(
            StackTraceElement stackTrace, TrexOption option, int flags);

    protected abstract String toString(StackFrame stackFrame, TrexOption option);

    protected abstract String toString(StackTraceElement stackTrace, TrexOption option);

    protected abstract String tab();

    protected abstract String at();

    protected abstract String atDuplicate();
}
