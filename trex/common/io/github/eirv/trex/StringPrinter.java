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
 * @date 2022/2/8 10:12
 */
public class StringPrinter extends TrexPrinter.Base {
    // Optimized for Android only
    private char[] text;
    private int count;

    public StringPrinter() {
        this(8 * 1024);
    }

    public StringPrinter(int capacity) {
        text = new char[capacity];
    }

    @Override
    public void print(char x) {
        grow(1);
        text[count++] = x;
    }

    @Override
    public void print(String x) {
        if (x == null) {
            grow(4);
            char[] t = text;
            int c = count;
            t[c++] = 'n';
            t[c++] = 'u';
            t[c++] = 'l';
            t[c++] = 'l';
            count = c;
        } else {
            int len = x.length();
            if (len == 0) return;
            grow(len);
            x.getChars(0, len, text, count);
            count += len;
        }
    }

    @Override
    public String toString() {
        return new String(text, 0, count);
    }

    private void grow(int size) {
        final int minSize = 4 * 1024;
        ensureCapacity(count + (size > minSize ? size : minSize));
    }

    private void ensureCapacity(int minimumCapacity) {
        char[] t = text;
        int len = t.length;

        if (minimumCapacity - len > 0) {
            int newCapacity = (len << 1) + 2;
            if (newCapacity - minimumCapacity < 0) {
                newCapacity = minimumCapacity;
            }

            char[] newText = new char[newCapacity];
            System.arraycopy(t, 0, newText, 0, len);
            text = newText;
        }
    }
}
