package net.momirealms.sparrow.reflection;

import net.momirealms.sparrow.reflection.exception.SparrowReflectionException;
import net.momirealms.sparrow.reflection.remapper.Remapper;
import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.Predicate;

public final class SReflection {
    private static Unsafe UNSAFE;
    private static MethodHandles.Lookup LOOKUP;
    private static MethodHandle constructor$MemberName;
    private static MethodHandle method$MemberName$getReferenceKind;
    private static MethodHandle method$MethodHandles$Lookup$getDirectField;

    public static void init(MethodHandles.Lookup lookup) {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
            if (lookup == null) {
                LOOKUP = getLookup(UNSAFE);
            } else {
                LOOKUP = lookup;
            }
            Class<?> clazz$MemberName = Class.forName("java.lang.invoke.MemberName");
            constructor$MemberName = LOOKUP.unreflectConstructor(clazz$MemberName.getDeclaredConstructor(Field.class, boolean.class));
            method$MemberName$getReferenceKind = LOOKUP.unreflect(clazz$MemberName.getDeclaredMethod("getReferenceKind"));
            method$MethodHandles$Lookup$getDirectField = LOOKUP.unreflect(MethodHandles.Lookup.class.getDeclaredMethod("getDirectField", byte.class, Class.class, clazz$MemberName));
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to init Reflection", e);
        }
    }

    // 获取神权lookup
    private static MethodHandles.Lookup getLookup(Unsafe unsafe) throws NoSuchFieldException {
        Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        long offset = unsafe.staticFieldOffset(implLookup);
        return (MethodHandles.Lookup) unsafe.getObject(MethodHandles.Lookup.class, offset);
    }

    private static String PREFIX = "Sparrow";
    private static Remapper REMAPPER = Remapper.noOp();
    private static Predicate<String> ACTIVE_PREDICATE = s -> true;

    private SReflection() {}

    public static void setAsmClassPrefix(@NotNull String prefix) {
        SReflection.PREFIX = Objects.requireNonNull(prefix);
    }

    public static void setRemapper(@NotNull Remapper remapper) {
        SReflection.REMAPPER = Objects.requireNonNull(remapper);
    }

    public static void setActivePredicate(@NotNull Predicate<String> predicate) {
        SReflection.ACTIVE_PREDICATE = Objects.requireNonNull(predicate);
    }

    @NotNull
    public static Remapper getRemapper() {
        return SReflection.REMAPPER;
    }

    @NotNull
    public static String getAsmClassPrefix() {
        return SReflection.PREFIX;
    }

    @NotNull
    public static Predicate<String> getFilter() {
        return SReflection.ACTIVE_PREDICATE;
    }

    @NotNull
    public static <T extends AccessibleObject> T setAccessible(@NotNull final T o) {
        o.setAccessible(true);
        return o;
    }

    @SuppressWarnings("unchecked")
    public static <T> T allocateInstance(Class<T> clazz) {
        try {
            return (T) getUnsafe().allocateInstance(clazz);
        } catch (InstantiationException e) {
            return null;
        }
    }

    public static VarHandle unreflectVarHandle(@NotNull final Field field) {
        try {
            return getLookup().unreflectVarHandle(field);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandle unreflectSetter(@NotNull final Field field) {
        try {
            return getLookup().unreflectSetter(field);
        } catch (IllegalAccessException e) {
            try { // 绕过final限制获取方法句柄
                Object memberName = constructor$MemberName.invoke(field, /*makeSetter*/ true);
                Object refKind = method$MemberName$getReferenceKind.invoke(memberName);
                return (MethodHandle) method$MethodHandles$Lookup$getDirectField.invoke(LOOKUP, refKind, field.getDeclaringClass(), memberName);
            } catch (Throwable ex) { // 这里为了确保不是 jdk 差异导致的失败还是抛出错误好点
                throw new SparrowReflectionException("Failed to unreflect field " + field, ex);
            }
        }
    }

    public static MethodHandle unreflectGetter(@NotNull final Field field) {
        try {
            return getLookup().unreflectGetter(field);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandle unreflectConstructor(@NotNull final Constructor<?> constructor) {
        try {
            return getLookup().unreflectConstructor(constructor);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandle unreflectMethod(@NotNull final Method method) {
        try {
            return getLookup().unreflect(method);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandles.Lookup getLookup() {
        if (LOOKUP == null) init(null);
        return LOOKUP;
    }

    public static Unsafe getUnsafe() {
        if (UNSAFE == null) init(null);
        return UNSAFE;
    }
}
