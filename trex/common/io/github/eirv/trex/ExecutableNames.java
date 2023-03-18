package io.github.eirv.trex;

import java.lang.reflect.Method;
import java.util.HashSet;

class ExecutableNames {
    private final boolean mOnlyOneConstructor;
    private final String[] mMethodNames;

    public ExecutableNames(Class<?> clazz) {
        mOnlyOneConstructor = clazz.getDeclaredConstructors().length <= 1;
        Method[] methods = clazz.getDeclaredMethods();
        int count = methods.length;
        HashSet<String> methodNames = new HashSet<>();
        for (int i = 0; count > i; i++) {
            methodNames.add(methods[i].getName());
        }
        mMethodNames = methodNames.toArray(new String[methodNames.size()]);
    }

    public boolean isOnlyOneConstructor() {
        return mOnlyOneConstructor;
    }

    public boolean contains(String name) {
        for (String methodName : mMethodNames) {
            if (methodName.hashCode() != name.hashCode()) continue;
            if (methodName.equals(name)) return true;
        }
        return false;
    }
}
