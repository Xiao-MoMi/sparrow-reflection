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
    public static final Unsafe UNSAFE;
    public static final MethodHandles.Lookup LOOKUP;
    private static final MethodHandle method$MethodHandleNatives$refKindIsSetter;
    private static final MethodHandle constructor$MemberName;
    private static final MethodHandle method$MemberName$getReferenceKind;
    private static final MethodHandle method$MethodHandles$Lookup$getDirectField;

    static {
        try {
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
            Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            Object base = UNSAFE.staticFieldBase(implLookup);
            long offset = UNSAFE.staticFieldOffset(implLookup);
            LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(base, offset); // 获取神权lookup
            Class<?> clazz$MethodHandleNatives = Class.forName("java.lang.invoke.MethodHandleNatives");
            Class<?> clazz$MemberName = Class.forName("java.lang.invoke.MemberName");
            method$MethodHandleNatives$refKindIsSetter = LOOKUP.unreflect(clazz$MethodHandleNatives.getDeclaredMethod("refKindIsSetter", byte.class));
            constructor$MemberName = LOOKUP.unreflectConstructor(clazz$MemberName.getDeclaredConstructor(Field.class, boolean.class));
            method$MemberName$getReferenceKind = LOOKUP.unreflect(clazz$MemberName.getDeclaredMethod("getReferenceKind"));
            method$MethodHandles$Lookup$getDirectField = LOOKUP.unreflect(MethodHandles.Lookup.class.getDeclaredMethod("getDirectField", byte.class, Class.class, clazz$MemberName));
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to init Reflection", e);
        }
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
            return (T) UNSAFE.allocateInstance(clazz);
        } catch (InstantiationException e) {
            return null;
        }
    }

    public static VarHandle unreflectVarHandle(@NotNull final Field field) {
        try {
            return LOOKUP.unreflectVarHandle(field);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandle unreflectSetter(@NotNull final Field field) {
        try {
            return LOOKUP.unreflectSetter(field);
        } catch (IllegalAccessException e) {
            try { // 绕过final限制获取方法句柄
                Object memberName = constructor$MemberName.invoke(field, true);
                Object refKind = method$MemberName$getReferenceKind.invoke(memberName);
                method$MethodHandleNatives$refKindIsSetter.invoke(refKind);
                return (MethodHandle) method$MethodHandles$Lookup$getDirectField.invoke(LOOKUP, refKind, field.getDeclaringClass(), memberName);
            } catch (Throwable ex) {
                return null;
            }
        }
    }

    public static MethodHandle unreflectGetter(@NotNull final Field field) {
        try {
            return LOOKUP.unreflectGetter(field);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandle unreflectConstructor(@NotNull final Constructor<?> constructor) {
        try {
            return LOOKUP.unreflectConstructor(constructor);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    public static MethodHandle unreflectMethod(@NotNull final Method method) {
        try {
            return LOOKUP.unreflect(method);
        } catch (IllegalAccessException e) {
            return null;
        }
    }
}
