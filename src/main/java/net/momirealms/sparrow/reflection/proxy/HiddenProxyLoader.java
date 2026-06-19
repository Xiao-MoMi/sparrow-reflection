package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.List;

final class HiddenProxyLoader {

    // 将 ProxyClassBytes 记录的代理实现类字节码正式加载和实例化.
    @SuppressWarnings("unchecked")
    <T> T load(Class<?> proxyType, ProxyClassBytes bytes) {
        try {
            // hidden class 的包和 loader 由 lookupHost 决定.
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(bytes.lookupHost(), SReflection.getLookup());
            MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(
                    bytes.bytecode(),
                    true,
                    MethodHandles.Lookup.ClassOption.NESTMATE
            );
            Class<?> proxyClass = hiddenLookup.lookupClass();

            // 回填成员访问句柄到代理实现类的静态字段.
            injectMethodHandles(proxyClass, bytes.methodHandleBindings());
            return (T) proxyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy class " + proxyType, e);
        }
    }

    // 将 writer 收集到的 MethodHandle 按 HANDLE_n 顺序写回生成类的静态字段.
    private static void injectMethodHandles(
            Class<?> proxyClass,
            List<MethodHandle> staticHandleBindings
    ) throws ReflectiveOperationException {
        for (int i = 0; i < staticHandleBindings.size(); i++) {
            Field handleField = proxyClass.getDeclaredField("HANDLE_" + i);
            SReflection.setAccessible(handleField).set(null, staticHandleBindings.get(i));
        }
    }
}
