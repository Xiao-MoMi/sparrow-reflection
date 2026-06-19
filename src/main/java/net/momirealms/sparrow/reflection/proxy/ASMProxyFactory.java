package net.momirealms.sparrow.reflection.proxy;

public final class ASMProxyFactory {
    private ASMProxyFactory() {
    }

    public static <T> T create(final Class<T> proxy) {
        return ProxyGenerator.create(proxy);
    }
}
