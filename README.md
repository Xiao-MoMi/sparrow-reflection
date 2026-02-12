## API Repository

```kotlin
repositories {
    mavenCentral() // for asm
    maven("https://repo.momirealms.net/releases/") // for sparrow reflection
}
```
```kotlin
dependencies {
    // Required ASM dependency (keep updated to latest version)
    implementation("org.ow2.asm:asm:9.9.1")
    // Check build.gradle.kts for the latest version
    implementation("net.momirealms:sparrow-reflection:{VERSION}")
}

tasks {
    // Important: Always relocate the package to avoid conflicts:
    shadowJar {
        relocate("net.momirealms.sparrow.reflection", "your.domain.libs.reflection")
    }
}
```

## Usage Example

### Prerequisite Preparation

```java
public static void init() {
    // Set prefix for ASM generated classes
    SReflection.setAsmClassPrefix("MyPlugin");
    // (Optional) Set up class mapper to adapt to multiple runtime environments
    SReflection.setRemapper(Remapper.createFromPaperJar());
    // (Optional) Set up custom active predicate to activate the activeIf option in annotations
    SReflection.setActivePredicate(new MinecraftVersionPredicate());
}
```

### Basic Operations

Basic operations allow you to more flexibly search for classes, methods, and fields based on Java reflection, and also enable you to generate corresponding ASM methods to reduce the overhead typically associated with reflection.

```java
package net.momirealms.sparrow.reflection;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.SIntField;
import net.momirealms.sparrow.reflection.field.SparrowField;
import net.momirealms.sparrow.reflection.method.SMethod1;
import net.momirealms.sparrow.reflection.method.SparrowMethod;

// Import these static methods for conveniently using various Matchers
// Constructor matchers start with c, field matchers start with f, method matchers start with m, Type matchers start with t
import static net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatchers.*;
import static net.momirealms.sparrow.reflection.field.matcher.FieldMatchers.*;
import static net.momirealms.sparrow.reflection.method.matcher.MethodMatchers.*;

public final class Example {

    public static class TestClass {
        private String stringField;
        private int intField;
        private TestClass(String field) { this.stringField = field; }
        private String someMethod(String a) { return this.stringField + " " + a; }
    }

    public static void main(String[] args) {
        SparrowClass<TestClass> sClass = SparrowClass.of(TestClass.class);

        // Get any first private constructor
        SparrowConstructor<TestClass> constructor = sClass.getDeclaredSparrowConstructor(cAny(), 0);
        // Generate a constructor with one parameter
        // By the same token, asm$2 would be the constructor with 2 parameters.
        SConstructor1 asmConstructor = constructor.asm$1();
        TestClass testInstance = (TestClass) asmConstructor.newInstance("ByeBye");

        // Get the first field of type String
        SparrowField stringField = sClass.getDeclaredSparrowField(fType(String.class), 0);
        SField asmField = stringField.asm();
        asmField.set(testInstance, "Hello");

        // Reduce performance overhead from boxing and unboxing through the use of primitive type ASM implementations
        SparrowField intField = sClass.getDeclaredSparrowField(fType(int.class), 0);
        SIntField asmIntField = intField.asm$int();
        asmIntField.set(testInstance, 12345);

        // Get the method named 'someMethod' with the first parameter and return value both of type String
        SparrowMethod method = sClass.getDeclaredSparrowMethod(mNamed("someMethod").and(mTakeArgument(0, String.class).and(mReturnType(String.class))));
        SMethod1 asmMethod = method.asm$1();
        System.out.println(asmMethod.invoke(testInstance, "World"));
    }
}
```

### Reflection Proxy

The core feature of Sparrow Reflection is Reflection Proxy, which enables the rapid generation of corresponding ASM-based implementation classes. This achieves performance levels close to native execution, while completely bypassing restrictions such as private, protected, and final modifiers.

> <details open>
>     <summary>Example target classes</summary>
> 
> ```java
> package net.momirealms.sparrow.reflection;
> 
> import java.util.concurrent.ThreadLocalRandom;
> 
> public abstract class Player {
>     public void sayHello() { System.out.println("Hello World!"); }
>     public static String getRandomString(int length) {
>         return String.valueOf(ThreadLocalRandom.current().nextInt(0, length));
>     }
> }
> ```
> 
> ```java
> package net.momirealms.sparrow.reflection;
> 
> import java.util.UUID;
> 
> public class ServerPlayer extends Player {
>     private final UUID uuid;
>     private String name;
>     private ServerLevel level;
> 
>     public ServerPlayer(UUID uuid, String name, ServerLevel level) {
>         this.uuid = uuid;
>         this.name = name;
>         this.level = level;
>     }
> 
>     public UUID uuid() { return this.uuid; }
>     public String name() { return this.name; }
>     public ServerLevel level() { return level; }
> }
> ```
> 
> ```java
> package net.momirealms.sparrow.reflection;
> 
> public class ServerLevel {
>     private int time;
> 
>     public ServerLevel(int time) {
>         this.time = time;
>     }
> 
>     public int time() { return time; }
>     public int setTime(int time) {
>         int prev = this.time;
>         this.time = time;
>         return prev;
>     }
> }
> ```
> </details>

> <details open>
>     <summary>Example proxy interfaces</summary>
> 
> ```java
> package net.momirealms.sparrow.reflection;
> 
> import net.momirealms.sparrow.reflection.proxy.ASMProxyFactory;
> import net.momirealms.sparrow.reflection.proxy.annotation.*;
> 
> import java.util.UUID;
> 
> public class ProxyExample {
> 
>     public static void main(String[] args) {
>         // Assume an existing object exists
>         ServerLevel serverLevel = new ServerLevel(666);
>         // Create ServerLevelProxy
>         ServerLevelProxy serverLevelProxy = ASMProxyFactory.create(ServerLevelProxy.class);
>         // Set world time via proxy interface
>         int previous = serverLevelProxy.setTime(serverLevel, 100);
> 
>         // Create ServerPlayerProxy
>         ServerPlayerProxy serverPlayerProxy = ASMProxyFactory.create(ServerPlayerProxy.class);
>         // Call its constructor
>         Object serverPlayer = serverPlayerProxy.newInstance(UUID.randomUUID(), "XiaoMoMi", serverLevel);
>         // Call its method
>         UUID uuid = serverPlayerProxy.getUUID(serverPlayer);
>         // Call parent class method
>         serverPlayerProxy.sayHello(serverPlayer);
>         // Call parent class static method
>         serverPlayerProxy.getRandomString(100);
>         // Ignore final and modify private fields
>         serverPlayerProxy.setUUID(serverPlayer, UUID.randomUUID());
>     }
> 
>     @ReflectionProxy(clazz = Player.class)
>     public interface PlayerProxy {
> 
>         @MethodInvoker(name = "sayHello")
>         void sayHello(Object player);
> 
>         @MethodInvoker(name = "getRandomString", isStatic = true /* Static methods or static fields require setting this parameter */)
>         String getRandomString(int length);
>     }
> 
>     // If you set up a remapper, this class name will be automatically mapped to the new class name in the corresponding environment
>     @ReflectionProxy(name = "net.momirealms.sparrow.reflection.ServerPlayer")
>     public interface ServerPlayerProxy extends PlayerProxy {
> 
>         @ConstructorInvoker
>         Object newInstance(UUID uuid, 
>                            String name,
>                            /* Use the Type annotation for inaccessible classes */
>                            @Type(clazz = ServerLevelProxy.class) Object level);
> 
>         @FieldSetter(name = "uuid")
>         void setUUID(Object player, UUID uuid);
> 
>         @FieldGetter(name = "uuid")
>         UUID getUUID(Object player);
>     }
> 
>     // Support different names for different versions
>     @ReflectionProxy(name = {"net.momirealms.sparrow.reflection.ServerLevel", "another.version.ServerLevel"})
>     public interface ServerLevelProxy {
> 
>         // The activeIf option takes effect only after activePredicate is set
>         @MethodInvoker(name = "setTime", activeIf = ">1.20.1") 
>         int setTime(Object serverLevel, int time);
>     }
> 
>     // For classes that may have their location changed due to relocation,
>     // mark ignoreRelocation = true and use '{}' in place of '.'
>     @ReflectionProxy(name = "net{}kyori{}adventure{}text{}Component", ignoreRelocation = true)
>     public interface ComponentProxy {
>     }
> }
> ```
> 
> </details>
