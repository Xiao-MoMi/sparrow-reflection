package net.momirealms.sparrow.reflection.proxy;

// 将解析完成的代理定义写入为一个具体策略的隐藏类字节码.
interface ProxyClassWriter {

    ProxyClassBytes write(ProxyDefinition definition);
}
