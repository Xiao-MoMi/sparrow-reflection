## Gradle Configuration

```kotlin
repositories {
    mavenCentral() // asm
    maven("https://repo.momirealms.net/releases/") // sparrow reflection
}
```
```kotlin
dependencies {
    // Required ASM dependency (keep updated to latest version)
    implementation("org.ow2.asm:asm:9.9")
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

```java
@ReflectionProxy(name = "net.minecraft.server.level.ServerPlayer")
public interface ServerPlayerProxy extends PlayerProxy {
    ServerPlayerProxy INSTANCE = ASMProxyFactory.create(ServerPlayerProxy.class);

    @FieldGetter(name = "chunkLoader")
    Object chunkLoader(Object instance);

    @FieldSetter(name = "chunkLoader")
    void chunkLoader(Object instance, Object chunkLoader);

    @MethodInvoker(name = "nextContainerCounter")
    int nextContainerCounter(Object instance);

    @MethodInvoker(name = "initMenu")
    void initMenu(Object instance, @Type(clazz = AbstractContainerMenuProxy.class) Object menu);
}
```