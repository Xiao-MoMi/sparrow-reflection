package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.proxy.annotation.ReflectionProxy;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;

import java.lang.reflect.Array;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Objects;

// 解析 proxy 注解中的类型引用, 并封装 remap, nullable, activeIf 和数组类型规则
final class ProxyTypeResolver {
    private ProxyTypeResolver() {}

    // 解析代理接口上的 @ReflectionProxy 为真实目标类型, inactive 或 nullable 缺失时允许返回 null.
    static Class<?> resolveProxyTarget(Class<?> clazz) {
        if (clazz.isArray()) {
            Class<?> proxiedComponent = resolveProxyTarget(clazz.getComponentType());
            if (proxiedComponent == null) {
                return null;
            }
            return Array.newInstance(proxiedComponent, 0).getClass();
        }

        ReflectionProxy proxy = clazz.getDeclaredAnnotation(ReflectionProxy.class);
        if (proxy == null) {
            throw new IllegalArgumentException("Class " + clazz + " has no @ReflectionProxy annotation");
        }
        if (!SReflection.getFilter().test(proxy.activeIf())) {
            return null;
        }

        Class<?> proxiedClass = resolveProxyAnnotation(clazz, proxy, clazz.getClassLoader());
        if (proxy.nullable()) {
            return proxiedClass;
        }
        return Objects.requireNonNull(proxiedClass, "Cannot find proxied class for " + clazz);
    }

    private static Class<?> resolveProxyAnnotation(
            Class<?> clazz,
            ReflectionProxy proxy,
            ClassLoader classLoader
    ) {
        if (proxy.clazz() == Object.class && proxy.name().length == 0) {
            throw new IllegalArgumentException("ReflectionProxy doesn't have value or class name set for class " + clazz);
        }
        if (proxy.clazz() != Object.class) {
            return proxy.clazz();
        }
        return findByNames(classLoader, proxy.name(), proxy.ignoreRelocation());
    }

    /**
     * 解析代理方法参数上的 @Type, 未声明时直接使用 Java 参数类型.
     */
    static Class<?> resolveParameterType(Parameter parameter, ClassLoader classLoader) {
        Type type = parameter.getDeclaredAnnotation(Type.class);
        if (type == null) {
            return parameter.getType();
        }
        if (type.clazz() == Object.class && type.name().length == 0) {
            throw new IllegalArgumentException("Type annotation doesn't have value or class name set for parameter " + parameter);
        }
        if (type.clazz() != Object.class) {
            return resolveProxyTarget(type.clazz());
        }
        return findByNames(classLoader, type.name(), type.ignoreRelocation());
    }

    private static Class<?> findByNames(
            ClassLoader classLoader,
            String[] names,
            boolean ignoreRelocation
    ) {
        if (!ignoreRelocation) {
            return SparrowClass.find(false, classLoader, names);
        }

        String[] normalizedNames = Arrays.stream(names)
                .map(it -> it.replace("{}", "."))
                .toArray(String[]::new);
        return SparrowClass.find(false, classLoader, normalizedNames);
    }
}
