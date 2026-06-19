package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * 扫描代理接口和父接口, 将注解声明解释成一份 ProxyDefinition
 */
final class ProxyBinder {

    // 代理接口 inactive 时返回 null, 父接口 inactive 时跳过对应方法
    ProxyDefinition bind(Class<?> proxyType) {
        Objects.requireNonNull(proxyType, "Proxy type must not be null");
        if (!proxyType.isInterface()) {
            throw new IllegalArgumentException("Class must be an interface: " + proxyType);
        }

        Class<?> targetType = ProxyTypeResolver.resolveProxyTarget(proxyType);
        if (targetType == null) {
            return null;
        }

        List<Class<?>> interfaces = getChildFirstHierarchy(proxyType);
        List<ProxyBinding> bindings = new ArrayList<>();
        for (int i = 0; i < interfaces.size(); i++) {
            Class<?> proxyInterface = interfaces.get(i);
            Class<?> interfaceTarget = ProxyTypeResolver.resolveProxyTarget(proxyInterface);
            if (interfaceTarget == null) {
                continue;
            }
            collectBindings(proxyInterface, interfaceTarget, bindings);
        }

        return new ProxyDefinition(proxyType, targetType, List.copyOf(bindings));
    }

    private static List<Class<?>> getChildFirstHierarchy(Class<?> interfaceClass) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        collectInterfaces(interfaceClass, interfaces);
        return List.copyOf(interfaces);
    }

    private static void collectInterfaces(Class<?> clazz, Set<Class<?>> visited) {
        if (!visited.add(clazz)) {
            return;
        }

        Class<?>[] parents = clazz.getInterfaces();
        for (Class<?> parent : parents) {
            collectInterfaces(parent, visited);
        }
    }

    private static void collectBindings(
            Class<?> proxyInterface,
            Class<?> targetType,
            List<ProxyBinding> bindings
    ) {
        SparrowClass<?> targetClass = new SparrowClass<>(targetType);
        Method[] methods = proxyInterface.getDeclaredMethods();
        for (Method method : methods) {
            ProxyBinding binding = resolveBinding(proxyInterface, targetClass, method);
            if (binding != null) {
                bindings.add(binding);
            }
        }
    }

    private static ProxyBinding resolveBinding(
            Class<?> proxyInterface,
            SparrowClass<?> targetClass,
            Method method
    ) {
        ProxyBinding fieldGetter = resolveFieldGetter(proxyInterface, targetClass, method);
        if (fieldGetter != null) {
            return fieldGetter;
        }

        ProxyBinding fieldSetter = resolveFieldSetter(proxyInterface, targetClass, method);
        if (fieldSetter != null) {
            return fieldSetter;
        }

        ProxyBinding methodInvoker = resolveMethodInvoker(proxyInterface, targetClass, method);
        if (methodInvoker != null) {
            return methodInvoker;
        }

        return resolveConstructorInvoker(proxyInterface, targetClass, method);
    }

    private static ProxyBinding resolveFieldGetter(
            Class<?> proxyInterface,
            SparrowClass<?> targetClass,
            Method method
    ) {
        FieldGetter annotation = method.getAnnotation(FieldGetter.class);
        if (annotation == null || !SReflection.getFilter().test(annotation.activeIf())) {
            return null;
        }

        Field field = targetClass.getDeclaredField(createFieldMatcher(annotation));
        if (!annotation.optional()) {
            Objects.requireNonNull(field, "Field not found for proxy " + proxyInterface + "#" + method.getName());
        }
        if (field == null) {
            return null;
        }

        checkArgumentCount(method, Modifier.isStatic(field.getModifiers()) ? 0 : 1);
        return new ProxyBinding.FieldGetter(method, field);
    }

    private static ProxyBinding resolveFieldSetter(
            Class<?> proxyInterface,
            SparrowClass<?> targetClass,
            Method method
    ) {
        FieldSetter annotation = method.getAnnotation(FieldSetter.class);
        if (annotation == null || !SReflection.getFilter().test(annotation.activeIf())) {
            return null;
        }

        Field field = targetClass.getDeclaredField(createFieldMatcher(annotation));
        if (!annotation.optional()) {
            Objects.requireNonNull(field, "Field not found for proxy " + proxyInterface + "#" + method.getName());
        }
        if (field == null) {
            return null;
        }

        checkArgumentCount(method, Modifier.isStatic(field.getModifiers()) ? 1 : 2);
        return new ProxyBinding.FieldSetter(method, field);
    }

    private static ProxyBinding resolveMethodInvoker(
            Class<?> proxyInterface,
            SparrowClass<?> targetClass,
            Method method
    ) {
        MethodInvoker annotation = method.getAnnotation(MethodInvoker.class);
        if (annotation == null || !SReflection.getFilter().test(annotation.activeIf())) {
            return null;
        }

        Class<?>[] parameterTypes = resolveTargetParameterTypes(method, annotation.isStatic());
        Method targetMethod = targetClass.getDeclaredMethod(createMethodMatcher(annotation).and(MethodMatcher.takeArguments(parameterTypes)));
        if (!annotation.optional()) {
            Objects.requireNonNull(targetMethod, "Method not found for proxy " + proxyInterface + "#" + method.getName());
        }
        if (targetMethod == null) {
            return null;
        }
        if (!Modifier.isStatic(targetMethod.getModifiers()) && method.getParameterCount() < 1) {
            throw new IllegalArgumentException("Non-static method must have at least one argument");
        }

        return new ProxyBinding.MethodInvoker(method, targetMethod);
    }

    private static ProxyBinding resolveConstructorInvoker(
            Class<?> proxyInterface,
            SparrowClass<?> targetClass,
            Method method
    ) {
        ConstructorInvoker annotation = method.getAnnotation(ConstructorInvoker.class);
        if (annotation == null || !SReflection.getFilter().test(annotation.activeIf())) {
            return null;
        }

        Class<?>[] parameterTypes = resolveConstructorParameterTypes(method);
        Constructor<?> constructor = targetClass.getDeclaredConstructor(ConstructorMatcher.takeArguments(parameterTypes));
        if (!annotation.optional()) {
            Objects.requireNonNull(constructor, "Constructor not found for proxy " + proxyInterface + "#" + method.getName());
        }
        if (constructor == null) {
            return null;
        }

        return new ProxyBinding.ConstructorInvoker(method, constructor);
    }

    private static FieldMatcher createFieldMatcher(FieldGetter annotation) {
        if (annotation.name().length == 0) {
            throw new IllegalArgumentException("FieldGetter doesn't have name or names set");
        }

        FieldMatcher matcher = FieldMatcher.named(annotation.name());
        return annotation.isStatic() ? matcher.and(FieldMatcher.staticField()) : matcher.and(FieldMatcher.instanceField());
    }

    private static FieldMatcher createFieldMatcher(FieldSetter annotation) {
        if (annotation.name().length == 0) {
            throw new IllegalArgumentException("FieldSetter doesn't have name or names set");
        }

        FieldMatcher matcher = FieldMatcher.named(annotation.name());
        return annotation.isStatic() ? matcher.and(FieldMatcher.staticField()) : matcher.and(FieldMatcher.instanceField());
    }

    private static MethodMatcher createMethodMatcher(MethodInvoker annotation) {
        if (annotation.name().length == 0) {
            throw new IllegalArgumentException("MethodInvoker doesn't have name or names set");
        }

        MethodMatcher matcher = MethodMatcher.named(annotation.name());
        return annotation.isStatic() ? matcher.and(MethodMatcher.staticMethod()) : matcher.and(MethodMatcher.instanceMethod());
    }

    private static Class<?>[] resolveTargetParameterTypes(Method method, boolean staticMethod) {
        return Arrays.stream(method.getParameters())
                .skip(staticMethod ? 0 : 1)
                .map(parameter -> ProxyTypeResolver.resolveParameterType(parameter, method.getDeclaringClass().getClassLoader()))
                .toArray(Class<?>[]::new);
    }

    private static Class<?>[] resolveConstructorParameterTypes(Method method) {
        return Arrays.stream(method.getParameters())
                .map(parameter -> ProxyTypeResolver.resolveParameterType(parameter, method.getDeclaringClass().getClassLoader()))
                .toArray(Class<?>[]::new);
    }

    private static void checkArgumentCount(Method method, int expected) {
        int length = method.getParameterCount();
        if (length != expected) {
            throw new IllegalArgumentException("Method " + method.getName() + " has " + length + " parameters but expected " + expected);
        }
    }
}
