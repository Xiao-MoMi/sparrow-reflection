package net.momirealms.sparrow.reflection.field;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Objects;

final class MethodHandleFieldAccessor extends SField {
    private final boolean isStatic;
    private final MethodHandle rawGetter;
    private final MethodHandle rawSetter;
    private final MethodHandle genericGetter;
    private final MethodHandle genericSetter;

    MethodHandleFieldAccessor(@NotNull Field field) {
        this.isStatic = Modifier.isStatic(field.getModifiers());
        MethodHandle getter = Objects.requireNonNull(SReflection.unreflectGetter(field));
        MethodHandle setter = Objects.requireNonNull(SReflection.unreflectSetter(field));
        if (this.isStatic) {
            this.rawGetter = getter;
            this.rawSetter = setter;
        } else {
            this.rawGetter = getter.asType(getter.type().changeParameterType(0, Object.class));
            this.rawSetter = setter.asType(setter.type().changeParameterType(0, Object.class));
        }
        if (this.isStatic) {
            this.genericGetter = this.rawGetter.asType(MethodType.methodType(Object.class));
            this.genericSetter = this.rawSetter.asType(MethodType.methodType(void.class, Object.class));
        } else {
            this.genericGetter = this.rawGetter.asType(MethodType.methodType(Object.class, Object.class));
            this.genericSetter = this.rawSetter.asType(MethodType.methodType(void.class, Object.class, Object.class));
        }
    }

    @Override
    public int getInt(Object instance) {
        try {
            return this.isStatic ? (int) this.rawGetter.invokeExact() : (int) this.rawGetter.invokeExact(instance);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public long getLong(Object instance) {
        try {
            return this.isStatic ? (long) this.rawGetter.invokeExact() : (long) this.rawGetter.invokeExact(instance);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public double getDouble(Object instance) {
        try {
            return this.isStatic ? (double) this.rawGetter.invokeExact() : (double) this.rawGetter.invokeExact(instance);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public boolean getBoolean(Object instance) {
        try {
            return this.isStatic ? (boolean) this.rawGetter.invokeExact() : (boolean) this.rawGetter.invokeExact(instance);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public float getFloat(Object instance) {
        try {
            return this.isStatic ? (float) this.rawGetter.invokeExact() : (float) this.rawGetter.invokeExact(instance);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public void setInt(Object instance, int value) {
        try {
            if (this.isStatic) this.rawSetter.invokeExact(value);
            else this.rawSetter.invokeExact(instance, value);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public void setLong(Object instance, long value) {
        try {
            if (this.isStatic) this.rawSetter.invokeExact(value);
            else this.rawSetter.invokeExact(instance, value);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public void setDouble(Object instance, double value) {
        try {
            if (this.isStatic) this.rawSetter.invokeExact(value);
            else this.rawSetter.invokeExact(instance, value);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public void setFloat(Object instance, float value) {
        try {
            if (this.isStatic) this.rawSetter.invokeExact(value);
            else this.rawSetter.invokeExact(instance, value);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public void setBoolean(Object instance, boolean value) {
        try {
            if (this.isStatic) this.rawSetter.invokeExact(value);
            else this.rawSetter.invokeExact(instance, value);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    @Nullable
    public Object get(Object instance) {
        try {
            return this.isStatic ? this.genericGetter.invokeExact() : this.genericGetter.invokeExact(instance);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    @Override
    public void set(Object instance, @Nullable Object value) {
        try {
            if (this.isStatic) this.genericSetter.invokeExact(value);
            else this.genericSetter.invokeExact(instance, value);
        } catch (Throwable e) {
            throw new SparrowReflectionException(e);
        }
    }

    public boolean isStatic() {
        return this.isStatic;
    }
}