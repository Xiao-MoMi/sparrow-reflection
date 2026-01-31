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

public final class SReflection {
    private static final String YOU_SHOULD_RELOCATE_THIS = "net{}momirealms{}sparrow{}reflection";
    private static final boolean TEST_ENVIRONMENT = true;
    public static final Unsafe UNSAFE;
    public static final MethodHandles.Lookup LOOKUP;
    private static final MethodHandle method$MethodHandleNatives$refKindIsSetter;
    private static final MethodHandle constructor$MemberName;
    private static final MethodHandle method$MemberName$getReferenceKind;
    private static final MethodHandle method$MethodHandles$Lookup$getDirectField;
    private static final MethodHandle methodHandle$ClassFile$readClassFile;
    private static final Object instance$defaultDumper;
    private static final MethodHandle constructor$ClassDefiner;
    private static final MethodHandle method$ClassDefiner$defineClass;

    static {
        try {
            if (!TEST_ENVIRONMENT) {
                checkRelocation();
            }
            Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            UNSAFE = (Unsafe) unsafeField.get(null);
            Field implLookup = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
            @SuppressWarnings("deprecation") Object base = UNSAFE.staticFieldBase(implLookup);
            @SuppressWarnings("deprecation") long offset = UNSAFE.staticFieldOffset(implLookup);
            LOOKUP = (MethodHandles.Lookup) UNSAFE.getObject(base, offset); // 获取神权lookup
            Class<?> clazz$MethodHandleNatives = Class.forName("java.lang.invoke.MethodHandleNatives");
            Class<?> clazz$MemberName = Class.forName("java.lang.invoke.MemberName");
            method$MethodHandleNatives$refKindIsSetter = LOOKUP.unreflect(clazz$MethodHandleNatives.getDeclaredMethod("refKindIsSetter", byte.class));
            constructor$MemberName = LOOKUP.unreflectConstructor(clazz$MemberName.getDeclaredConstructor(Field.class, boolean.class));
            method$MemberName$getReferenceKind = LOOKUP.unreflect(clazz$MemberName.getDeclaredMethod("getReferenceKind"));
            method$MethodHandles$Lookup$getDirectField = LOOKUP.unreflect(MethodHandles.Lookup.class.getDeclaredMethod("getDirectField", byte.class, Class.class, clazz$MemberName));
            Class<?> clazz$ClassFile = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassFile");
            methodHandle$ClassFile$readClassFile = Objects.requireNonNull(SReflection.unreflectMethod(clazz$ClassFile.getDeclaredMethod("readClassFile", byte[].class)));
            instance$defaultDumper = Objects.requireNonNull(SReflection.unreflectMethod(MethodHandles.Lookup.class.getDeclaredMethod("defaultDumper"))).invoke();
            Class<?> clazz$ClassDefiner = Class.forName("java.lang.invoke.MethodHandles$Lookup$ClassDefiner");
            constructor$ClassDefiner = Objects.requireNonNull(SReflection.unreflectConstructor(clazz$ClassDefiner.getDeclaredConstructors()[0]));
            method$ClassDefiner$defineClass = Objects.requireNonNull(SReflection.unreflectMethod(clazz$ClassDefiner.getDeclaredMethod("defineClass", boolean.class)));
        } catch (Throwable e) {
            throw new SparrowReflectionException("Failed to init Reflection", e);
        }
    }

    @SuppressWarnings("all")
    private static void checkRelocation() {
        if (SReflection.class.getName().startsWith(YOU_SHOULD_RELOCATE_THIS.replace("{}", "."))) {
            throw new SparrowReflectionException("You should relocate sparrow reflection");
        }
    }

    private static String PREFIX = "Sparrow";
    private static Remapper REMAPPER = Remapper.noOp();

    private SReflection() {}

    public static void setAsmClassPrefix(@NotNull String prefix) {
        SReflection.PREFIX = Objects.requireNonNull(prefix);
    }

    public static void setRemapper(@NotNull Remapper remapper) {
        SReflection.REMAPPER = Objects.requireNonNull(remapper);
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
    public static <T extends AccessibleObject> T setAccessible(@NotNull final T o) {
        o.setAccessible(true);
        return o;
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

    public static Class<?> defineClass(@NotNull final MethodHandles.Lookup lookup, final byte[] bytes) throws Throwable {
        Object classFile = methodHandle$ClassFile$readClassFile.invoke(bytes);
        Object classDefiner = constructor$ClassDefiner.invoke(lookup, classFile, /* STRONG_LOADER_LINK */ 4, instance$defaultDumper);
        return (Class<?>) method$ClassDefiner$defineClass.invoke(classDefiner, true);
    }
}
