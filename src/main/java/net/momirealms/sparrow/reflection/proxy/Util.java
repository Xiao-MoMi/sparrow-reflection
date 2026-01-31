package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;

final class Util {
    private Util() {}

    public static List<Class<?>> getTopDownInterfaceHierarchy(Class<?> interfaceClass) {
        Set<Class<?>> hierarchy = new LinkedHashSet<>();
        hierarchy.add(interfaceClass);
        collectInterfaces(interfaceClass, hierarchy);
        // 父接口在前，子接口在后
        List<Class<?>> result = new ArrayList<>(hierarchy);
        Collections.reverse(result);
        return Collections.unmodifiableList(result);
    }

    private static void collectInterfaces(Class<?> clazz, Set<Class<?>> collector) {
        for (Class<?> parent : clazz.getInterfaces()) {
            collector.add(parent);
            collectInterfaces(parent, collector);
        }
    }

    public static Class<?> getProxiedClassOrNull(Class<?> clazz) {
        ReflectionProxy proxy = clazz.getDeclaredAnnotation(ReflectionProxy.class);
        if (proxy == null) {
            return clazz;
        }
        return getProxiedClass(clazz, proxy);
    }

    public static Class<?> getProxiedClass(Class<?> clazz) {
        ReflectionProxy proxy = clazz.getDeclaredAnnotation(ReflectionProxy.class);
        if (proxy == null) {
            throw new IllegalArgumentException("Class " + clazz + " has no @ReflectionProxy annotation");
        }
        return getProxiedClass(clazz, proxy);
    }

    @Nullable
    private static Class<?> getProxiedClass(Class<?> clazz, ReflectionProxy proxy) {
        if (proxy.clazz() == Object.class && proxy.name().isEmpty() && proxy.names().length == 0) {
            throw new IllegalArgumentException("ReflectionProxy doesn't have value or class name set for class " + clazz);
        }
        if (proxy.clazz() != Object.class) {
            return proxy.clazz();
        }
        if (!proxy.name().isEmpty()) {
            return SparrowClass.find(proxy.name());
        }
        return SparrowClass.find(proxy.names());
    }

    public static void checkArgumentCount(Method method, int expected) {
        int length = method.getParameterCount();
        if (length != expected) {
            throw new IllegalArgumentException("Method " + method.getName() + " has " + length + " parameters but expected " + expected);
        }
    }

    public static Class<?> getParameterClass(Parameter parameter) {
        Type type = parameter.getDeclaredAnnotation(Type.class);
        if (type == null) {
            return parameter.getType();
        }
        if (type.clazz() == Object.class && type.name().isEmpty() && type.names().length == 0) {
            throw new IllegalArgumentException("Type annotation doesn't have value or class name set for parameter " + parameter);
        }
        if (type.clazz() != Object.class) {
            return getProxiedClass(type.clazz());
        }
        if (!type.name().isEmpty()) {
            return SparrowClass.find(type.name());
        }
        return SparrowClass.find(type.names());
    }

    @SuppressWarnings("DuplicatedCode")
    public static FieldMatcher getFieldMatcher(FieldGetter fieldGetter) {
        FieldMatcher matcher;
        if (!fieldGetter.name().isEmpty()) {
            matcher = FieldMatcher.named(fieldGetter.name());
        } else if (fieldGetter.names().length != 0) {
            matcher = FieldMatcher.named(fieldGetter.names());
        } else {
            throw new IllegalArgumentException("FieldGetter doesn't have name or names set");
        }
        if (fieldGetter.isStatic()) {
            matcher.and(FieldMatcher.staticField());
        }
        return matcher;
    }

    @SuppressWarnings("DuplicatedCode")
    public static FieldMatcher getFieldMatcher(FieldSetter fieldSetter) {
        FieldMatcher matcher;
        if (!fieldSetter.name().isEmpty()) {
            matcher = FieldMatcher.named(fieldSetter.name());
        } else if (fieldSetter.names().length != 0) {
            matcher = FieldMatcher.named(fieldSetter.names());
        } else {
            throw new IllegalArgumentException("FieldSetter doesn't have name or names set");
        }
        if (fieldSetter.isStatic()) {
            matcher.and(FieldMatcher.staticField());
        }
        return matcher;
    }

    public static MethodMatcher createMethodMatcher(MethodInvoker invoker) {
        MethodMatcher matcher;
        if (!invoker.name().isEmpty()) {
            matcher = MethodMatcher.named(invoker.name());
        } else if (invoker.names().length != 0) {
            matcher = MethodMatcher.named(invoker.names());
        } else {
            throw new IllegalArgumentException("MethodInvoker doesn't have name or names set");
        }
        if (invoker.isStatic()) {
            matcher.and(MethodMatcher.staticMethod());
        }
        return matcher;
    }
}