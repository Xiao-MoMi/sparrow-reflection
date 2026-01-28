package net.momirealms.sparrow.reflection.method;

import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Objects;

public final class SparrowMethod {
    public final Method method;

    public SparrowMethod(Method method) {
        this.method = method;
    }

    public static SparrowMethod of(@NotNull final Method method) {
        Objects.requireNonNull(method, "method cannot be null");
        return new SparrowMethod(method);
    }

    public static SparrowMethod ofNullable(@Nullable final Method method) {
        return method == null ? null : new SparrowMethod(method);
    }

    public SMethod asm() {
        try {
            return MethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod invoker", e);
        }
    }

    public SMethod0 asm$0() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod0 invoker", e);
        }
    }

    public SMethod1 asm$1() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod1 invoker", e);
        }
    }

    public SMethod2 asm$2() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod2 invoker", e);
        }
    }

    public SMethod3 asm$3() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod3 invoker", e);
        }
    }

    public SMethod4 asm$4() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod4 invoker", e);
        }
    }

    public SMethod5 asm$5() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod5 invoker", e);
        }
    }

    public SMethod6 asm$6() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod6 invoker", e);
        }
    }

    public SMethod7 asm$7() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod7 invoker", e);
        }
    }

    public SMethod8 asm$8() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod8 invoker", e);
        }
    }

    public SMethod9 asm$9() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod9 invoker", e);
        }
    }

    public SMethod10 asm$10() {
        try {
            return OptimizedMethodInvokerFactory.create(this.method);
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to create SMethod10 invoker", e);
        }
    }
}
