## 引入此依赖

```kotlin
repositories {
    maven("https://repo.momirealms.net/releases/")
}
```
```kotlin
dependencies {
    // 版本号可在 build.gradle.kts 查看
    implementation("net.momirealms:sparrow-reflection:{VERSION}")
    // 必要的 asm 依赖，保持最新版本
    implementation("org.ow2.asm:asm:9.9")
}
```

## 使用时的注意点

1. 请务必 relocate 此依赖，否则将抛出异常。
2. 对于绝大多数常规方法调用场景，推荐通过 `ASM` 动态生成对应参数数量的适配类，以提高调用效率与代码可维护性。
3. 若方法参数全部为基本类型，建议使用 `MethodHandle.invokeExact` 方法进行调用，以避免频繁的拆箱与装箱操作，从而提升性能。
4. 当需要修改 `record` 类型或声明为 `final` 的字段时，必须通过 `MethodHandle` 实现，这是目前唯一可行的方案。
5. 生成字段访问器时，优先使用例如 `asm$int` 的基础类型实现

## 使用示例

### 前提准备

```java
public static void init() {
    // 设置 asm 生成类的前缀
    SReflection.setAsmClassPrefix("MyPlugin");
    // (可选) 设置类映射器，以同时适配多种运行环境
    SReflection.setRemapper(Remapper.createFromPaperJar());
}
```

### 基础操作

```java
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.SparrowField;
import net.momirealms.sparrow.reflection.method.SMethod1;
import net.momirealms.sparrow.reflection.method.SparrowMethod;

// 引入这些静态方法以方便地使用各种 Matcher
// 构造器匹配器以c开头，字段匹配器以f开头，方法匹配器以m开头
import static net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatchers.*;
import static net.momirealms.sparrow.reflection.field.matcher.FieldMatchers.*;
import static net.momirealms.sparrow.reflection.method.matcher.MethodMatchers.*;

public final class Example {

    // 测试用类
    public static class TestClass {
        // 测试用私有字段
        private String field;
        // 测试用私有构造器
        private TestClass(String field) {
            this.field = field;
        }
        // 测试用私有方法
        private String someMethod(String a) {
            return this.field + " " + a;
        }
    }

    public static void main(String[] args) {
        SparrowClass<TestClass> sClass = SparrowClass.of(TestClass.class);
        
        // 获取任意第一个私有构造器
        SparrowConstructor<TestClass> constructor = sClass.getDeclaredSparrowConstructor(cAny(), 0); 
        SConstructor1 asmConstructor = constructor.asm$1(); // 生成 1 参数构造器
        TestClass testInstance = (TestClass) asmConstructor.newInstance("ByeBye");

        // 获取第一个类型为String的字段
        SparrowField field = sClass.getDeclaredSparrowField(fType(String.class)); 
        SField asmField = field.asm();
        asmField.set(testInstance, "Hello");

        // 获取名为 someMethod 且第一个参数和返回值都为 String 的方法
        SparrowMethod method = sClass.getDeclaredSparrowMethod(mNamed("someMethod").and(mTakeArgument(0, String.class).and(mReturnType(String.class))));
        SMethod1 asmMethod = method.asm$1(); // 生成 1 参数方法
        System.out.println(asmMethod.invoke(testInstance, "World"));
    }
}
```

### 反射代理

**目前存在的缺陷：**
- ProxyFactory.newJavaProxy 当前基于Java代理实现，底层使用ASM生成机制，但由于代理机制限制，每次调用相比原生方法需额外增加一次HashMap查找和数组参数转换开销，执行效率显著低于直接调用。
- ProxyFactory.newAsmProxy 尚未实现，其目标是直接生成高度优化的代理类，通过为每个方法生成最优化的字节码来彻底消除额外开销，实现接近原生的执行性能。

以下是一些测试用数据类

```java
package net.momirealms.sparrow.reflection;

import java.util.concurrent.ThreadLocalRandom;

public class Player {

    public void sayHello() {
        System.out.println("Hello World!");
    }

    public static String getRandomString(int length) {
        return String.valueOf(ThreadLocalRandom.current().nextInt(0, length));
    }
}
```
```java
package net.momirealms.sparrow.reflection;

import java.util.UUID;

public class ServerPlayer extends Player {
    private final UUID uuid;
    private final String name;
    private final ServerLevel level;

    public ServerPlayer(UUID uuid, String name, ServerLevel level) {
        this.uuid = uuid;
        this.name = name;
        this.level = level;
    }

    public UUID uuid() {
        return this.uuid;
    }

    public String name() {
        return this.name;
    }

    public ServerLevel level() {
        return level;
    }
}
```
```java
package net.momirealms.sparrow.reflection;

public class ServerLevel {
    private int time;

    public ServerLevel(int time) {
        this.time = time;
    }

    public int time() {
        return time;
    }

    public int setTime(int time) {
        int prev = this.time;
        this.time = time;
        return prev;
    }
}
```

以下是反射代理使用方法。

```java
package net.momirealms.sparrow.reflection;

import net.momirealms.sparrow.reflection.proxy.ProxyFactory;
import net.momirealms.sparrow.reflection.proxy.Strategy;
import net.momirealms.sparrow.reflection.proxy.annotation.*;

import java.util.UUID;

public class ProxyExample {

    public static void main(String[] args) {
        // 创建代理工厂
        ProxyFactory proxyFactory = ProxyFactory.create(ProxyExample.class.getClassLoader());
        // 假设存在已有对象
        ServerLevel serverLevel = new ServerLevel(666);
        
        // 创建 ServerLevelProxy 并通过代理接口设置世界时间
        ServerLevelProxy serverLevelProxy = proxyFactory.newJavaProxy(ServerLevelProxy.class);
        int previous = serverLevelProxy.setTime(serverLevel, 100);
        System.out.println(previous);

        // 创建 ServerPlayerProxy 并调用其构造器
        ServerPlayerProxy serverPlayerProxy = proxyFactory.newJavaProxy(ServerPlayerProxy.class);
        Object serverPlayer = serverPlayerProxy.newInstance(UUID.randomUUID(), "XiaoMoMi", serverLevel);
        // 调用父类方法
        serverPlayerProxy.sayHello(serverPlayer);
        // 调用父类静态方法
        serverPlayerProxy.getRandomString(100);
        // 调用方法
        System.out.println(serverPlayerProxy.getUUID(serverPlayer));
        // 修改私有字段
        serverPlayerProxy.setUUID(serverPlayer, UUID.randomUUID());
        System.out.println(serverPlayerProxy.getUUID(serverPlayer));
    }

    @ReflectionProxy(clazz = Player.class)
    public interface PlayerProxy {

        @MethodInvoker(name = "sayHello")
        void sayHello(Object player);

        @MethodInvoker(name = "getRandomString", isStatic = true /* 静态方法或者静态字段需要设置此参数 */)
        String getRandomString(int length);
    }

    // 如果你设置了 remapper, 此类名会在相应环境下自动映射为新类名
    @ReflectionProxy(name = "net.momirealms.sparrow.reflection.ServerPlayer")
    public interface ServerPlayerProxy extends PlayerProxy {

        @ConstructorInvoker
        Object newInstance(UUID uuid, String name, @Type(clazz = ServerLevelProxy.class) Object level /* 对于不可访问的类使用 Type 注解 */);

        @FieldSetter(name = "uuid", strategy = Strategy.MH)
        void setUUID(Object player, UUID uuid);

        @FieldGetter(name = "uuid")
        UUID getUUID(Object player);
    }

    // 支持不同版本不同命名
    @ReflectionProxy(names = {"net.momirealms.sparrow.reflection.ServerLevel", "another.version.ServerLevel"})
    public interface ServerLevelProxy {

        @MethodInvoker(names = {"setTime", "setTimeNow"})
        int setTime(Object serverLevel, int time);
    }
}
```