package net.momirealms.sparrow.reflection.constructor;

import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.Objects;

public final class SparrowConstructor<T> {
    public final Constructor<T> constructor;

    public SparrowConstructor(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @NotNull
    public static <T> SparrowConstructor<T> of(@NotNull final Constructor<T> constructor) {
        Objects.requireNonNull(constructor, "constructor cannot be null");
        return new SparrowConstructor<>(constructor);
    }

    @Nullable
    public static <T> SparrowConstructor<T> ofNullable(@Nullable final Constructor<T> constructor) {
        return constructor == null ? null : new SparrowConstructor<T>(constructor);
    }

    public SConstructor asm() {
        try {
            return ConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor invoker", e);
        }
    }

    public SConstructor0 asm$0() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor0 invoker", e);
        }
    }

    public SConstructor1 asm$1() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor1 invoker", e);
        }
    }

    public SConstructor2 asm$2() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor2 invoker", e);
        }
    }

    public SConstructor3 asm$3() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor3 invoker", e);
        }
    }

    public SConstructor4 asm$4() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor4 invoker", e);
        }
    }

    public SConstructor5 asm$5() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor5 invoker", e);
        }
    }

    public SConstructor6 asm$6() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor6 invoker", e);
        }
    }

    public SConstructor7 asm$7() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor7 invoker", e);
        }
    }

    public SConstructor8 asm$8() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor8 invoker", e);
        }
    }

    public SConstructor9 asm$9() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor9 invoker", e);
        }
    }

    public SConstructor10 asm$10() {
        try {
            return OptimizedConstructorInvokerFactory.create(this.constructor);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SConstructor10 invoker", e);
        }
    }

    public SConstructor unsafe() {
        return new UnsafeConstructor(this.constructor);
    }
}
