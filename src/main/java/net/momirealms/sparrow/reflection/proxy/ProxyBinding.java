package net.momirealms.sparrow.reflection.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 单个代理方法和目标类成员之间的绑定关系
 */
sealed interface ProxyBinding permits
        ProxyBinding.FieldGetter,
        ProxyBinding.FieldSetter,
        ProxyBinding.MethodInvoker,
        ProxyBinding.ConstructorInvoker
{
    Method proxyMethod();

    // 代理方法读取目标字段
    record FieldGetter(Method proxyMethod, Field targetField) implements ProxyBinding { }

    // 代理方法写入目标字段
    record FieldSetter(Method proxyMethod, Field targetField) implements ProxyBinding { }

    // 代理方法调用目标方法
    record MethodInvoker(Method proxyMethod, Method targetMethod) implements ProxyBinding { }

    // 代理方法调用目标构造器
    record ConstructorInvoker(Method proxyMethod, Constructor<?> targetConstructor) implements ProxyBinding { }
}
