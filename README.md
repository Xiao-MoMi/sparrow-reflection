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