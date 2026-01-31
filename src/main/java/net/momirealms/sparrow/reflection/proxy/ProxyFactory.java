package net.momirealms.sparrow.reflection.proxy;

import java.lang.reflect.Proxy;

public final class ProxyFactory {
    private final ClassLoader classLoader;

    public ProxyFactory(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public static ProxyFactory create(ClassLoader classLoader) {
        return new ProxyFactory(classLoader);
    }

    @SuppressWarnings("unchecked")
    public <T> T newJavaProxy(Class<T> clazz) {
        return (T) Proxy.newProxyInstance(
                this.classLoader,
                new Class<?>[]{clazz},
                new ReflectionProxyHandler<>(clazz)
        );
    }

    public <T> T newAsmProxy(Class<T> clazz) {
        throw new RuntimeException("Not implemented yet");
    }
}
