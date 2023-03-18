package io.github.eirv.trex.demo;

import io.github.eirv.trex.Trex;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class Mirror {
    public static long getOffset(Class<?> clazz, String name) throws NoSuchFieldException {
        return Trex.theUnsafe.objectFieldOffset(clazz.getDeclaredField(name));
    }

    // Java.Lang Java8 Throwable
    public static class JLJ8_Throwable {
        private transient Object backtrace;
        private String detailMessage;
        private Throwable cause;
        private StackTraceElement[] stackTrace;
        private List<Throwable> suppressedExceptions;
    }

    public static class JLJ9_Throwable {
        private transient Object backtrace;
        private String detailMessage;
        private Throwable cause;
        private StackTraceElement[] stackTrace;
        private transient int depth;
        private List<Throwable> suppressedExceptions;
    }

    public abstract static class JLRJ8_AccessibleObject implements AnnotatedElement {
        boolean override;
        volatile Object securityCheckCache;
    }

    public abstract static class JLRJ8_Executable extends JLRJ8_AccessibleObject
            implements Member, GenericDeclaration {
        private transient volatile boolean hasRealParameterData;
        private transient volatile Parameter[] parameters;
        private transient Map<Class<? extends Annotation>, Annotation> declaredAnnotations;
    }

    public abstract static class JLRJ8_Constructor<T> extends JLRJ8_Executable {
        private Class<T> clazz;
        private int slot;
        private Class<?>[] parameterTypes;
        private Class<?>[] exceptionTypes;
        private int modifiers;
        private transient String signature;
        private transient Object genericInfo;
        private byte[] annotations;
        private byte[] parameterAnnotations;
        private volatile Object constructorAccessor;
        private Constructor<T> root;
    }

    public abstract static class JLRJ8_Method extends JLRJ8_Executable {
        private Class<?> clazz;
        private int slot;
        private String name;
        private Class<?> returnType;
        private Class<?>[] parameterTypes;
        private Class<?>[] exceptionTypes;
        private int modifiers;
        private transient String signature;
        private transient Object genericInfo;
        private byte[] annotations;
        private byte[] parameterAnnotations;
        private byte[] annotationDefault;
        private volatile Object methodAccessor;
        private Method root;
    }
}
