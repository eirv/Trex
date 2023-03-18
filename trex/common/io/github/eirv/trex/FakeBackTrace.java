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
 * @date 2023/2/7 12:22
 */
class FakeBackTrace {
    private static final int IDX_VOID = 0;
    private static final int IDX_BACK_TRACE = 1;
    private static final int IDX_STACK_FRAME = 2;
    private static final int IDX_STACK_FRAME_OPTION_HASH_CODE = 3;
    private static final int LEN_BACK_TRACE_ARR = 4;

    private final Object[] mBackTraceArr;

    public FakeBackTrace(Object[] backTraceArr) {
        mBackTraceArr = backTraceArr;
    }

    public static FakeBackTrace from(Platform tp, Throwable throwable) {
        Object backTrace = tp.getBackTrace0(throwable);
        StackTraceElement[] stackTraces = throwable.getStackTrace();

        if (backTrace == null) {
            int len = stackTraces.length;
            if (len > 0) {
                backTrace = tp.getBackTrace0(stackTraces[0]);
            }
            if (backTrace == null) return null;
        }

        FakeBackTrace fakeBackTrace = from(backTrace);
        if (fakeBackTrace == null) return null;

        tp.setBackTrace0(throwable, fakeBackTrace.mBackTraceArr);
        return fakeBackTrace;
    }

    public static FakeBackTrace fromNullable(Object backTrace) {
        return from(backTrace != null ? backTrace : Void.TYPE);
    }

    public static FakeBackTrace from(Object backTrace) {
        Object[] backTraceArr;
        if (backTrace instanceof Object[]) {
            backTraceArr = (Object[]) backTrace;
            int len = backTraceArr.length;
            if (len == 0) return null;
            if (backTraceArr[IDX_VOID] == Void.TYPE) {
                assert len == LEN_BACK_TRACE_ARR;
                return new FakeBackTrace(backTraceArr);
            }
        }
        backTraceArr = new Object[LEN_BACK_TRACE_ARR];
        backTraceArr[IDX_VOID] = Void.TYPE;
        backTraceArr[IDX_BACK_TRACE] = backTrace != Void.TYPE ? backTrace : null;
        return new FakeBackTrace(backTraceArr);
    }

    public Object getBackTrace() {
        return mBackTraceArr[IDX_BACK_TRACE];
    }

    public StackFrame[] getStackFrame() {
        Object element = mBackTraceArr[IDX_STACK_FRAME];
        if (element instanceof StackFrame[]) {
            return (StackFrame[]) element;
        }
        return null;
    }

    public void setStackFrame(StackFrame[] stackFrame) {
        mBackTraceArr[IDX_STACK_FRAME] = stackFrame;
    }

    public int getStackFrameOptionHashCode() {
        Object element = mBackTraceArr[IDX_STACK_FRAME_OPTION_HASH_CODE];
        if (element instanceof Integer) {
            return (int) element;
        }
        return -1;
    }

    public void setStackFrameOptionHashCode(int stackFrameOptionHashCode) {
        mBackTraceArr[IDX_STACK_FRAME_OPTION_HASH_CODE] = stackFrameOptionHashCode;
    }
}
