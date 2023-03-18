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

package io.github.eirv.trex.demo;

import android.os.Build;

import java.lang.reflect.Method;

import dalvik.system.VMRuntime;
import sun.misc.Unsafe;

public class HiddenApi {
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                Unsafe unsafe = (Unsafe) Unsafe.class.getMethod("getUnsafe").invoke(null);
                Method[] stubs = Compiler.class.getDeclaredMethods();
                Method first = stubs[0];
                Method second = stubs[1];
                long size = unsafe.getLong(second, 24) - unsafe.getLong(first, 24);
                int addrSize = unsafe.addressSize();
                long methods = unsafe.getLong(VMRuntime.class, 48);
                long count = addrSize == 8 ? unsafe.getLong(methods) : unsafe.getInt(methods);
                methods += addrSize;
                for (long j = 0, done = 0; count > j; j++) {
                    long method = j * size + methods;
                    unsafe.putLong(first, 24, method);
                    String name = first.getName();
                    if (!"getRuntime".equals(name) && !"setHiddenApiExemptions".equals(name))
                        continue;
                    long addr = method + 4;
                    int acc = unsafe.getInt(addr);
                    unsafe.putInt(addr, acc | 0x10000000);
                    if (++done == 2) break;
                }
            } catch (Exception e) {
                throw new ExceptionInInitializerError(e);
            }
        }
    }

    public static void setExemptions(String... signaturePrefixes) {
        try {
            VMRuntime.getRuntime().setHiddenApiExemptions(signaturePrefixes);
        } catch (NoSuchMethodError ignored) {
        }
    }
}
