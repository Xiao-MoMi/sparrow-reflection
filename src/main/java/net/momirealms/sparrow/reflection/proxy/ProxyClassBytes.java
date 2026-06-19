package net.momirealms.sparrow.reflection.proxy;

import java.lang.invoke.MethodHandle;
import java.util.List;

/**
 * 使用 ProxyClassWriter 根据 ProxyDefinition 生成的产物,
 * 包含隐藏类 lookup host, 类字节码和需要回填到静态字段的 MethodHandle.
 */
record ProxyClassBytes(
        Class<?> lookupHost,
        byte[] bytecode,
        List<MethodHandle> methodHandleBindings
) {
}
