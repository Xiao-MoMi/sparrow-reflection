package net.momirealms.sparrow.reflection.proxy;

// 把公开的代理接口转换成可实例化的 ASM 隐藏类
final class ProxyGenerator {
    private static final ProxyBinder BINDER = new ProxyBinder(); // 注解解析与成员绑定
    private static final ProxyClassWriter NESTMATE_WRITER = new NestmateProxyClassWriter(); // 同 loader 直接访问 writer
    private static final ProxyClassWriter METHOD_HANDLE_WRITER = new MethodHandleProxyClassWriter(); // 跨 loader fallback writer
    private static final HiddenProxyLoader LOADER = new HiddenProxyLoader(); // 隐藏类加载与实例化

    private ProxyGenerator() {}

    /**
     * 按 bind -> write -> load 三个阶段创建代理实例
     */
    static <T> T create(Class<T> proxyType) {
        // 解析 proxy 类, 然后将解析的代理方法对象和目标类的成员进行绑定
        ProxyDefinition definition = BINDER.bind(proxyType);
        if (definition == null) {
            return null;
        }

        // 只有 proxy 接口和目标类处于同一个 ClassLoader 时, 才能把实现类定义成目标类 nestmate.
        ProxyClassWriter writer = definition.proxyType().getClassLoader() == definition.targetType().getClassLoader()
                ? NESTMATE_WRITER
                : METHOD_HANDLE_WRITER;
        ProxyClassBytes bytes = writer.write(definition);

        // 使用加载器加载生成的类.
        return LOADER.load(definition.proxyType(), bytes);
    }
}
