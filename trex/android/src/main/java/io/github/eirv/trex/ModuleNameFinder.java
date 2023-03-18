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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * @author Eirv
 * @date 2023/2/7 12:51
 */
class ModuleNameFinder {
    private static final Item[] items;
    private static final Map<String, String> cache = new WeakHashMap<>();
    private static final Map<Class<?>, String> customClasses = new IdentityHashMap<>();
    private static final Map<ClassLoader, Set<Item>> customClassLoaderItems =
            new IdentityHashMap<>();
    private static final Set<Item> customItems = new HashSet<>();
    private static final Map<String, String> customCache = new WeakHashMap<>();

    static {
        List<Item> list = new ArrayList<>();
        addItem(
                list,
                "android.art",
                "dalvik.system.AnnotatedStackTraceElement",
                "dalvik.system.ClassExt",
                "dalvik.system.TransactionAbortError",
                "dalvik.system.VMRuntime",
                "dalvik.system.VMStack",
                "java.lang.AndroidHardcodedSystemProperties",
                "java.lang.CaseMapper",
                "java.lang.Daemons",
                "java.lang.DexCache",
                "java.lang.StringFactory",
                "java.lang.VMClassLoader",
                "java.lang.invoke.ArrayElementVarHandle",
                "java.lang.invoke.ByteArrayViewVarHandle",
                "java.lang.invoke.ByteBufferViewVarHandle",
                "java.lang.invoke.FieldVarHandle",
                "java.lang.ref.FinalizerReference",
                "java.net.AddressCache",
                "java.net.DefaultFileNameMap",
                "java.nio.NIOAccess",
                "java.nio.NioUtils");
        addItem(list, "android.core", "android.system.*", "libcore.*");
        addItem(list, "android.dalvik", "dalvik.*", "org.apache.harmony.dalvik.*");
        addItem(list, "android.internal", "com.android.internal.*");
        addItem(list, "android.server", "com.android.server.*");
        addItem(list, "android.base", "android.*");
        addItem(list, "java.desktop", "java.awt.font.*", "java.beans.*");
        addItem(list, "java.prefs", "java.util.prefs.*");
        addItem(list, "java.sql", "java.sql.*", "javax.sql.*");
        addItem(list, "java.xml", "javax.xml.*", "org.w3c.dom.*", "org.xml.sax.*");
        addItem(list, "jdk.net", "jdk.net.*");
        addItem(list, "jdk.unsupported", "sun.misc.Unsafe");
        addItem(
                list,
                "java.base",
                "java.io.*",
                "java.lang.*",
                "java.math.*",
                "java.net.*",
                "java.nio.*",
                "java.security.*",
                "java.text.*",
                "java.time.*",
                "java.util.*",
                "javax.crypto.*",
                "javax.net.*",
                "javax.security.*",
                "jdk.internal.*",
                "sun.*");
        addItem(list, "org.json", "org.json.*");
        items = list.toArray(new Item[list.size()]);
    }

    public static void addModuleName(String name, Class<?> clazz) {
        customClasses.put(clazz, name);
    }

    public static void addModuleName(String name, String[] rules) {
        customItems.add(new Item(name, rules));
    }

    public static void addModuleName(String name, ClassLoader classLoader, String... rules) {
        Set<Item> items = customClassLoaderItems.get(classLoader);
        if (items != null) {
            items.add(new Item(name, rules));
        } else {
            items = new HashSet<>();
            items.add(new Item(name, rules));
            customClassLoaderItems.put(classLoader, items);
        }
    }

    public static void removeModuleName(String name) {
        customItems.remove(new Item(name));
    }

    public static void removeModuleName(String name, ClassLoader classLoader) {
        Set<Item> items = customClassLoaderItems.get(classLoader);
        if (items != null) {
            items.remove(new Item(name));
        }
    }

    public static String findCustom(Class<?> clazz) {
        if (customClasses.containsKey(clazz)) {
            return customClasses.get(clazz);
        }

        ClassLoader classLoader = clazz.getClassLoader();
        String className = getClassName(clazz.getName());

        if (customClassLoaderItems.containsKey(classLoader)) {
            Set<Item> items = customClassLoaderItems.get(classLoader);
            return findCustom(items, null, className);
        }
        return findCustom(customItems, customCache, className);
    }

    private static String findCustom(Set<Item> items, Map<String, String> cache, String className) {
        if (cache != null && cache.containsKey(className)) {
            return cache.get(className);
        }
        for (Item item : items) {
            if (item.matches(className)) {
                assert cache != null;
                cache.put(className, item.name);
                return item.name;
            }
        }
        return null;
    }

    public static String find(String className) {
        className = getClassName(className);
        if (cache.containsKey(className)) {
            return cache.get(className);
        }
        for (Item item : items) {
            if (item.matches(className)) {
                cache.put(className, item.name);
                return item.name;
            }
        }
        return null;
    }

    private static String getClassName(String className) {
        int index = className.indexOf('$');
        if (index == -1) {
            return className;
        } else {
            return className.substring(0, index);
        }
    }

    private static void addItem(List<Item> list, String name, String... rules) {
        list.add(new Item(name, rules));
    }

    private static class Item {
        private static final PatternItem[] EMPTY_PATTERN_ITEM = new PatternItem[0];

        public final String name;
        private final PatternItem[] patterns;

        public Item(String name) {
            this.name = name;
            this.patterns = EMPTY_PATTERN_ITEM;
        }

        public Item(String name, String[] rules) {
            this.name = name;
            int len = rules.length;
            PatternItem[] patterns = new PatternItem[len];
            for (int i = 0; len > i; i++) {
                patterns[i] = new PatternItem(rules[i]);
            }
            this.patterns = patterns;
        }

        public boolean matches(String input) {
            for (PatternItem pattern : patterns) {
                if (pattern.matches(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof Item)) return false;

            Item peer = (Item) obj;
            return name.equals(peer.name);
        }
    }

    private static class PatternItem {
        private final Pattern pattern;
        private final String rule;

        public PatternItem(String rule) {
            if (rule.indexOf('*') == -1) {
                this.rule = rule;
                pattern = null;
            } else {
                rule = rule.replace(".", "\\.");
                rule = rule.replace("*", ".*");
                pattern = Pattern.compile(rule);
                this.rule = null;
            }
        }

        public boolean matches(String input) {
            return pattern != null ? pattern.matcher(input).matches() : rule.equals(input);
        }
    }
}
