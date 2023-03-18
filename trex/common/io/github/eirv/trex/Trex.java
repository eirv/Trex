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

import static io.github.eirv.trex.TrexOption.COLOR_AT;
import static io.github.eirv.trex.TrexOption.COLOR_CAPTION;
import static io.github.eirv.trex.TrexOption.COLOR_CLASS_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_MESSAGE;
import static io.github.eirv.trex.TrexOption.COLOR_NUMBER;
import static io.github.eirv.trex.TrexOption.COLOR_PACKAGE_NAME;
import static io.github.eirv.trex.TrexOption.COLOR_PUNCTUATION;
import static io.github.eirv.trex.TrexOption.COLOR_TEXT;

import sun.misc.Unsafe;

import java.io.CharArrayWriter;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * @author Eirv
 * @date 2022/10/24 11:29
 */
public final class Trex {
    public static final Unsafe theUnsafe = Utils.UNSAFE;

    static final Map<VmMethodKey, StackFrame> sStackFrameCache = new WeakHashMap<>();
    static final Map<Class<?>, ExecutableNames> sExecutableNamesCache = new WeakHashMap<>();

    private static final Field sStackTraceField;
    private static Platform sPlatform;

    static {
        sStackTraceField = Utils.findFieldIfExists(Throwable.class, "stackTrace");
    }

    private Trex() {}

    public static StackFrame[] getStackFrame(Throwable throwable) {
        return getStackFrame(throwable, null);
    }

    public static StackFrame[] getStackFrame(Throwable throwable, TrexOption option) {
        Utils.requireNonNull(throwable, "throwable");
        Platform platform = ensurePlatformInitialized();
        option = platform.cloneOption0(Utils.select(option, TrexOption.getDefault()));

        StackFrame[] stackFrames =
                getStackFrame(
                        throwable,
                        option,
                        platform,
                        option.computeStackFrameOptionHashCode(),
                        new Throwable[1]);

        return stackFrames != null ? stackFrames.clone() : null;
    }

    private static StackFrame[] getStackFrame(
            Throwable throwable,
            TrexOption option,
            Platform platform,
            int stackFrameOptionHashCode,
            Throwable[] stubThrowable) {

        Object backTrace;
        FakeBackTrace fakeBackTrace = FakeBackTrace.from(platform, throwable);
        if (fakeBackTrace == null) return null;
        StackFrame[] stackFrames = fakeBackTrace.getStackFrame();

        int currentStackFrameOptionHashCode = fakeBackTrace.getStackFrameOptionHashCode();
        if (stackFrames != null
                && (currentStackFrameOptionHashCode == -1
                        || currentStackFrameOptionHashCode == stackFrameOptionHashCode)) {
            return stackFrames;
        } else {
            backTrace = fakeBackTrace.getBackTrace();
            if (backTrace == null) return null;
        }

        StackTraceElement[] stackTraces = throwable.getStackTrace();
        int len = stackTraces.length;
        stackFrames = new StackFrame[len];

        if (((Object) stackTraces[0]) instanceof StackFrame) {
            for (int i = 0; len > i; i++) {
                stackFrames[i] = (StackFrame) (Object) stackTraces[i];
            }
        } else {
            BackTraceParser parser =
                    platform.newBackTraceParser0(
                            stackTraces, backTrace, option, stackFrameOptionHashCode);
            int depth = parser.depth();

            if (len != depth) {
                Throwable stub = stubThrowable[0];
                if (stub == null || sStackTraceField == null) {
                    stubThrowable[0] = stub = new Throwable();
                } else {
                    Utils.setField(sStackTraceField, stub, null);
                }
                platform.setBackTrace0(stub, backTrace);
                platform.setDepth0(stub, depth);
                parser.setStackTrace(stub.getStackTrace());
                stackFrames = new StackFrame[depth];
            }

            for (int i = 0; depth > i; i++) {
                stackFrames[i] = parser.parse(i);
            }
        }

        fakeBackTrace.setStackFrame(stackFrames);
        fakeBackTrace.setStackFrameOptionHashCode(stackFrameOptionHashCode);
        return stackFrames;
    }

    public static void setStackFrame(Throwable throwable, StackFrame[] stackFrames) {
        Utils.requireNonNull(throwable, "throwable");
        if (stackFrames != null) {
            stackFrames = stackFrames.clone();
            for (int i = 0, len = stackFrames.length; len > i; i++) {
                if (stackFrames[i] == null) {
                    throw new NullPointerException("stackFrames[" + i + ']');
                }
            }
        }

        Platform platform = ensurePlatformInitialized();
        Object backTrace = platform.getBackTrace0(throwable);
        if (backTrace == null) {
            Utils.requireNonNull(stackFrames, "stackFrames");
        }

        FakeBackTrace fakeBackTrace = FakeBackTrace.fromNullable(backTrace);
        fakeBackTrace.setStackFrame(stackFrames);
        fakeBackTrace.setStackFrameOptionHashCode(stackFrames != null ? -1 : 0);
    }

    public static String getStackTraceString(Throwable throwable) {
        if (throwable == null) return "";
        CharArrayWriter writer = new CharArrayWriter(128 * 1024);
        FastPrintWriter printer = new FastPrintWriter(writer);
        throwable.printStackTrace(printer);
        printer.flush();
        return writer.toString();
    }

    public static String getStackFrameString(Throwable throwable) {
        return getStackFrameString(throwable, null);
    }

    public static String getStackFrameString(Throwable throwable, TrexOption option) {
        if (throwable == null) return "";
        option = Utils.select(option, TrexOption.getDefault());

        StringPrinter printer =
                new StringPrinter(option.isColorSchemeEnabled() ? 512 * 1024 : 128 * 1024);
        printStackFrame(throwable, printer, option);
        return printer.toString();
    }

    public static void printStackFrame(Throwable throwable) {
        printStackFrame(throwable, (TrexOption) null);
    }

    public static void printStackFrame(Throwable throwable, TrexOption option) {
        printStackFrame(throwable, System.err, option);
    }

    public static void printStackFrame(Throwable throwable, PrintStream stream) {
        printStackFrame(throwable, stream, null);
    }

    public static void printStackFrame(Throwable throwable, PrintStream stream, TrexOption option) {
        Utils.requireNonNull(stream, "stream");
        FastPrintWriter printer =
                new FastPrintWriter(new OutputStreamWriter(stream), false, 16 * 1024);
        printer.initLock(stream);
        printStackFrame(throwable, (TrexPrinter) printer, option);
        printer.flush();
    }

    public static void printStackFrame(Throwable throwable, PrintWriter writer) {
        printStackFrame(throwable, writer, null);
    }

    public static void printStackFrame(
            Throwable throwable, final PrintWriter writer, TrexOption option) {
        Utils.requireNonNull(writer, "writer");
        TrexPrinter printer =
                new TrexPrinter.Base(writer) {
                    @Override
                    public void print(char x) {
                        writer.print(x);
                    }

                    @Override
                    public void print(String x) {
                        writer.print(x);
                    }
                };
        printStackFrame(throwable, printer, option);
    }

    public static void printStackFrame(Throwable throwable, TrexPrinter printer) {
        printStackFrame(throwable, printer, null);
    }

    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    public static void printStackFrame(
            Throwable throwable, TrexPrinter printer, TrexOption option) {
        Utils.requireNonNull(throwable, "throwable");
        Utils.requireNonNull(printer, "printer");
        Platform platform = ensurePlatformInitialized();
        option = platform.cloneOption0(Utils.select(option, TrexOption.getDefault()));

        printer.option(option);
        TrexStyle style = option.getStyle();
        StackTraceElement[] enclosingTraces =
                option.isFoldEnabled() ? new StackTraceElement[0] : null;
        String tab = option.getTab();
        String at = style.at();
        String atDuplicate = style.atDuplicate();
        Set<Throwable> dejaVu = Utils.newIdentityHashSet();
        List<IdentityObject<Throwable>> identityThrowables = new ArrayList<>();
        Throwable[] stubThrowable = new Throwable[1];
        int stackFrameOptionHashCode = option.computeStackFrameOptionHashCode();

        final Object lock = printer.lock();
        if (lock != null) {
            synchronized (lock) {
                printStackFrame(
                        throwable,
                        printer,
                        enclosingTraces,
                        "",
                        tab,
                        at,
                        atDuplicate,
                        dejaVu,
                        identityThrowables,
                        stubThrowable,
                        option,
                        style,
                        stackFrameOptionHashCode);
            }
        } else {
            printStackFrame(
                    throwable,
                    printer,
                    enclosingTraces,
                    "",
                    tab,
                    at,
                    atDuplicate,
                    dejaVu,
                    identityThrowables,
                    stubThrowable,
                    option,
                    style,
                    stackFrameOptionHashCode);
        }
    }

    private static void printStackFrame(
            Throwable throwable,
            TrexPrinter p,
            Object[] enclosingTraces,
            String prefix,
            String tab,
            String at,
            String atDuplicate,
            Set<Throwable> dejaVu,
            List<IdentityObject<Throwable>> identityThrowables,
            Throwable[] stubThrowable,
            TrexOption option,
            TrexStyle style,
            int stackFrameOptionHashCode) {

        if (dejaVu.contains(throwable)) {
            if (option.isColorSchemeEnabled()) {
                p.color(COLOR_PUNCTUATION);
                p.print('[');
                p.color(COLOR_CAPTION);
                p.print("CIRCULAR REFERENCE");
                p.color(COLOR_PUNCTUATION);
                p.print(": ");

                if (printString(throwable, p, option, prefix, identityThrowables)) {
                    p.print(' ');
                } else {
                    p.println();
                    p.print(prefix);
                }

                p.color(COLOR_PUNCTUATION);
                p.print(']');
                p.color(COLOR_TEXT);
            } else {
                p.print("[CIRCULAR REFERENCE: ");
                if (printString(throwable, p, option, prefix, identityThrowables)) {
                    p.print(' ');
                } else {
                    p.println();
                    p.print(prefix);
                }
                p.print(']');
            }
            p.println();
        } else {
            dejaVu.add(throwable);
            if (option.isThrowableIdVisible()) {
                IdentityObject<Throwable> identityThrowable = new IdentityObject<>(throwable);
                if (!identityThrowables.contains(identityThrowable)) {
                    identityThrowables.add(identityThrowable);
                }
            }

            StackFrame[] stackFrames =
                    getStackFrame(
                            throwable, option, sPlatform, stackFrameOptionHashCode, stubThrowable);
            StackTraceElement[] stackTraces = throwable.getStackTrace();
            Object[] traces = Utils.select(stackFrames, stackTraces);
            int length = traces.length;

            int m = length - 1;
            int framesInCommon = 0;
            if (enclosingTraces != null) {
                int n = enclosingTraces.length - 1;
                while (m >= 0 && n >= 0 && isSimilar(traces[m], enclosingTraces[n])) {
                    m--;
                    n--;
                }
                framesInCommon = length - 1 - m;
            }
            m++;

            printString(throwable, p, option, prefix, identityThrowables);
            p.println();

            boolean hasDuplicateTrace = option.isCheckDuplicateTraceEnabled();
            List<DuplicateItem> duplicateItems = null;
            int duplicateItemSize = 0;

            if (hasDuplicateTrace) {
                duplicateItems = new ArrayList<>();
                int duplicateTraceMaxSize = option.getDuplicateTraceMaxSize();

                boolean onlyCompareHashCodeEnabled = option.isOnlyCompareHashCodeEnabled();
                int[] hashCodes = null;
                Object[] targetTraces = null;

                if (onlyCompareHashCodeEnabled) {
                    hashCodes = new int[m];
                    for (int i = 0; m > i; i++) {
                        hashCodes[i] = traces[i].hashCode();
                    }
                } else {
                    if (stackFrames != null) {
                        targetTraces = stackFrames;
                    } else {
                        targetTraces = new HashObject[length];
                        for (int i = 0; length > i; i++) {
                            targetTraces[i] = new HashObject(stackTraces[i]);
                        }
                    }
                }

                for (int i = 0, len = m - duplicateTraceMaxSize; len > i; i++) {
                    for (int size = 1; duplicateTraceMaxSize >= size; size++) {
                        if (onlyCompareHashCodeEnabled
                                ? hashCodes[i] == hashCodes[i + size]
                                : HashObject.equals(targetTraces[i], targetTraces[i + size])) {
                            int count = 1;
                            count:
                            for (; ; ) {
                                int next = i + (size * count);
                                for (int j = 0; size > j; j++) {
                                    if (onlyCompareHashCodeEnabled
                                            ? hashCodes[j + i] != hashCodes[j + next]
                                            : !HashObject.equals(
                                                    targetTraces[j + i], targetTraces[j + next]))
                                        break count;
                                }
                                count++;
                            }
                            if (count == 1) break;

                            duplicateItems.add(new DuplicateItem(i - 1, size, count + 1));

                            i += size * (count - 1);
                            break;
                        }
                    }
                }

                duplicateItemSize = duplicateItems.size();
                hasDuplicateTrace = duplicateItemSize > 0;
            }

            for (int i = 0, j = 0; m > i; i++) {
                DuplicateItem duplicateItem;
                if (hasDuplicateTrace && i == (duplicateItem = duplicateItems.get(j)).index) {
                    int size = duplicateItem.size;
                    int count = duplicateItem.count;

                    for (int k = 0; size > k; k++) {
                        printStackTraceLine(
                                p,
                                option,
                                style,
                                stackFrames,
                                stackTraces,
                                prefix,
                                tab,
                                atDuplicate,
                                i + k);
                    }

                    p.print(prefix);
                    p.print(tab);
                    p.color(COLOR_AT);
                    p.print(atDuplicate);
                    p.color(COLOR_PUNCTUATION);
                    p.print("... ");
                    p.color(COLOR_NUMBER);
                    p.print(count - 1);
                    p.color(COLOR_TEXT);
                    p.println(" more");

                    i += size * count - 1;
                    if (++j == duplicateItemSize) hasDuplicateTrace = false;
                } else {
                    printStackTraceLine(
                            p, option, style, stackFrames, stackTraces, prefix, tab, at, i);
                }
            }

            if (framesInCommon != 0) {
                p.print(prefix);
                p.print(tab);
                p.color(COLOR_PUNCTUATION);
                p.print("... ");
                p.color(COLOR_NUMBER);
                p.print(framesInCommon);
                p.color(COLOR_TEXT);
                p.println(" more");
            }

            enclosingTraces = enclosingTraces != null ? traces : null;

            String sePrefix = prefix.concat(tab);
            for (Throwable se : throwable.getSuppressed()) {
                p.print(sePrefix);
                p.color(COLOR_CAPTION);
                p.print("Suppressed");
                p.color(COLOR_PUNCTUATION);
                p.print(": ");

                printStackFrame(
                        se,
                        p,
                        enclosingTraces,
                        sePrefix,
                        tab,
                        at,
                        atDuplicate,
                        dejaVu,
                        identityThrowables,
                        stubThrowable,
                        option,
                        style,
                        stackFrameOptionHashCode);
            }

            Throwable cause = throwable.getCause();
            if (cause != null) {
                p.print(prefix);
                p.color(COLOR_CAPTION);
                p.print("Caused by");
                p.color(COLOR_PUNCTUATION);
                p.print(": ");

                printStackFrame(
                        cause,
                        p,
                        enclosingTraces,
                        prefix,
                        tab,
                        at,
                        atDuplicate,
                        dejaVu,
                        identityThrowables,
                        stubThrowable,
                        option,
                        style,
                        stackFrameOptionHashCode);
            }
        }
    }

    private static boolean isSimilar(Object traceA, Object traceB) {
        if (traceA == null || traceB == null) return false;
        if ((traceA instanceof StackFrame && traceB instanceof StackFrame)
                || (traceA instanceof StackTraceElement && traceB instanceof StackTraceElement)) {
            return traceA.equals(traceB);
        }

        StackFrame stackFrame;
        StackTraceElement stackTrace;

        if (traceA instanceof StackFrame) {
            stackFrame = (StackFrame) traceA;
            stackTrace = (StackTraceElement) traceB;
        } else {
            stackFrame = (StackFrame) traceB;
            assert traceA instanceof StackTraceElement;
            stackTrace = (StackTraceElement) traceA;
        }

        return stackFrame.getClassName().equals(stackTrace.getClassName())
                && stackFrame.getMethodName().equals(stackTrace.getMethodName())
                && Utils.equals(stackFrame.getFileName(), stackTrace.getFileName())
                && stackFrame.getLineNumber() == stackTrace.getLineNumber();
    }

    private static void printStackTraceLine(
            TrexPrinter p,
            TrexOption option,
            TrexStyle style,
            StackFrame[] stackFrames,
            StackTraceElement[] stackTraces,
            String prefix,
            String tab,
            String at,
            int i) {

        p.print(prefix);
        p.print(tab);
        p.color(COLOR_AT);
        p.print(at);
        if (stackFrames != null) {
            p.print(stackFrames[i].proxy().toString(option));
        } else {
            p.print(style.toString(stackTraces[i], option));
        }
        p.resetLastColor();
        p.println();
    }

    public static String toString(Throwable throwable) {
        return toString(throwable, null);
    }

    public static String toString(Throwable throwable, TrexOption option) {
        Utils.requireNonNull(throwable, "throwable");
        option = Utils.select(option, TrexOption.getDefault()).clone();
        StringPrinter p = new StringPrinter();
        p.option(option);
        printString(throwable, p, option, "", Collections.<IdentityObject<Throwable>>emptyList());
        return p.toString();
    }

    private static boolean printString(
            Throwable throwable,
            TrexPrinter p,
            TrexOption option,
            String prefix,
            List<IdentityObject<Throwable>> identityThrowables) {

        String tab = option.getTab();
        boolean throwableIdVisible = option.isThrowableIdVisible();

        IntArray throwableIds = throwableIdVisible ? new IntArray() : null;
        List<String> throwableStrings = new ArrayList<>();
        List<String> classNames = new ArrayList<>();
        List<String> messages = new ArrayList<>();

        String rootThrowableString = throwable.toString();
        throwableStrings.add(rootThrowableString);

        for (; ; ) {
            if (throwableIdVisible) {
                IdentityObject<Throwable> identityThrowable = new IdentityObject<>(throwable);
                int id = identityThrowables.indexOf(identityThrowable);
                if (id == -1) {
                    identityThrowables.add(identityThrowable);
                    id = identityThrowables.size() - 1;
                }
                throwableIds.add(id);
            }

            classNames.add(throwable.getClass().getName());
            messages.add(throwable.getLocalizedMessage());

            throwable = throwable.getCause();
            if (throwable == null) break;

            String causeString = throwable.toString();
            if (rootThrowableString.contains(causeString)) {
                throwableStrings.add(causeString);
            } else break;
        }

        int size = classNames.size();
        if (size > 1) {
            for (int i = size - 2; i >= 0; i--) {
                String message = messages.get(i);
                if (message == null) continue;
                if (!message.equals(throwableStrings.get(i + 1))) continue;
                messages.set(i, null);
            }
        }

        if (option.isColorSchemeEnabled()) {
            for (int i = 0; size > i; i++) {
                String[] classNameSplits = Utils.splitClassName(classNames.get(i));
                int classNameSplitsLast = classNameSplits.length - 1;
                String message = messages.get(i);

                if (i != 0) {
                    p.print(prefix);
                    p.print(tab);
                }

                for (int j = 0; classNameSplitsLast > j; j++) {
                    p.color(COLOR_PACKAGE_NAME);
                    p.print(classNameSplits[j]);
                    p.color(COLOR_PUNCTUATION);
                    p.print('.');
                }
                p.color(COLOR_CLASS_NAME);
                p.print(classNameSplits[classNameSplitsLast]);

                if (throwableIdVisible) {
                    p.color(COLOR_PUNCTUATION);
                    p.print('<');
                    p.color(COLOR_NUMBER);
                    p.print(throwableIds.get(i));
                    p.color(COLOR_PUNCTUATION);
                    p.print('>');
                }

                if (message != null) {
                    p.color(COLOR_PUNCTUATION);
                    p.print(": ");
                    p.color(COLOR_MESSAGE);
                    p.print(message);
                }

                if (i + 1 != size) {
                    p.println();
                }
            }
        } else {
            for (int i = 0; size > i; i++) {
                String className = classNames.get(i);
                String message = messages.get(i);

                if (i != 0) {
                    p.print(prefix);
                    p.print(tab);
                }
                p.print(className);

                if (throwableIdVisible) {
                    p.print('<');
                    p.print(throwableIds.get(i));
                    p.print('>');
                }

                if (message != null) {
                    p.print(": ");
                    p.print(message);
                }

                if (i + 1 != size) {
                    p.println();
                }
            }
        }
        return size == 1;
    }

    public static Class<?> getCallerClass() {
        return getCallerClasses()[0];
    }

    public static Class<?>[] getCallerClasses() {
        Platform platform = ensurePlatformInitialized();
        Throwable throwable = new Throwable();
        Object backTrace = platform.getBackTrace0(throwable);
        Utils.requireNonNull(backTrace, "backtrace");
        List<Class<?>> callerClasses = platform.getCallerClasses0(backTrace);
        while (true) {
            if (callerClasses.remove(0) != Trex.class) break;
        }
        return callerClasses.toArray(new Class[callerClasses.size()]);
    }

    public static RuntimeException rethrow(Throwable throwable) {
        sneakyThrow(throwable);
        return new RuntimeException(throwable);
    }

    @SuppressWarnings("unchecked")
    private static <X extends Throwable> void sneakyThrow(Throwable throwable) throws X {
        throw (X) throwable;
    }

    static void setPlatform(Platform platform) {
        sPlatform = platform;
    }

    static Platform ensurePlatformInitialized() {
        if (sPlatform == null || !sPlatform.isInitialized0()) {
            throw new IllegalStateException("Trex is not initialized");
        }
        return sPlatform;
    }
}
