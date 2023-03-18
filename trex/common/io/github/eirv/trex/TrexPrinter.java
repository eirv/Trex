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
 * @date 2023/2/7 12:20
 */
public interface TrexPrinter {
    void print(char x);

    void print(int x);

    void print(long x);

    void print(String x);

    void println(String x);

    void println();

    Object lock();

    void option(TrexOption option);

    void color(byte colorId);

    void resetLastColor();

    abstract class Base implements TrexPrinter {
        protected final Object lock;
        protected TrexOption option;
        protected boolean colorSchemeEnabled;
        protected int lastColorHash;

        public Base() {
            this(null);
        }

        public Base(Object lock) {
            this.lock = lock;
        }

        @Override
        public void print(int x) {
            print(Integer.toString(x));
        }

        @Override
        public void print(long x) {
            print(Long.toString(x));
        }

        @Override
        public void println(String x) {
            print(x);
            println();
        }

        @Override
        public void println() {
            print(Utils.LINE_SEPARATOR);
        }

        @Override
        public Object lock() {
            return lock;
        }

        @Override
        public void option(TrexOption option) {
            this.option = option;
            colorSchemeEnabled = option.isColorSchemeEnabled();
        }

        @Override
        public void color(byte colorId) {
            if (colorSchemeEnabled) {
                String colorScheme = option.getColor(colorId);
                int colorSchemeHash = colorScheme.hashCode();
                if (lastColorHash != colorSchemeHash) {
                    print(colorScheme);
                    lastColorHash = colorSchemeHash;
                }
            }
        }

        @Override
        public void resetLastColor() {
            lastColorHash = -1;
        }
    }
}
