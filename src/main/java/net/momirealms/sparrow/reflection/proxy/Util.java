package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.matcher.FieldMatcher;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import net.momirealms.sparrow.reflection.proxy.annotation.*;
import net.momirealms.sparrow.reflection.proxy.annotation.Type;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;

final class Util implements Opcodes {
    private Util() {}

    @SuppressWarnings("unchecked")
    public static <T> T createAsmProxy(final Class<T> proxy) {
        List<Class<?>> interfaces = Util.getChildFirstHierarchy(proxy);
        Class<?> targetClass = Util.getProxiedClass(proxy);
        if (targetClass == null) return null;
        String internalClassName = org.objectweb.asm.Type.getInternalName(targetClass) + "$Proxy";
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, internalClassName, null, "java/lang/Object", new String[]{org.objectweb.asm.Type.getInternalName(proxy)});

        // 添加构造方法
        {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }

        // 写入接口方法
        ASMProxyBuilder builder = new ASMProxyBuilder(cw, internalClassName);
        Util.analyseAndApply(builder, interfaces);

        // 完成类
        cw.visitEnd();

        try {
            MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(targetClass, SReflection.LOOKUP);
            MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(cw.toByteArray(), true, MethodHandles.Lookup.ClassOption.NESTMATE);
            Class<?> proxyClass = hiddenLookup.lookupClass();
            int i = 0;
            for (MethodHandle finalFieldHandle : builder.finalFields()) {
                Field handleField = proxyClass.getDeclaredField("HANDLE_" + i++);
                SReflection.setAccessible(handleField).set(null, finalFieldHandle);
            }
            return (T) proxyClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create proxy class " + proxy, e);
        }
    }

    public static List<Class<?>> getChildFirstHierarchy(Class<?> interfaceClass) {
        Objects.requireNonNull(interfaceClass, "Interface class must not be null");
        if (!interfaceClass.isInterface()) {
            throw new IllegalArgumentException("Class must be an interface: " + interfaceClass);
        }

        Set<Class<?>> allInterfaces = new HashSet<>(8);
        collectInterfaces(interfaceClass, allInterfaces);

        Map<Class<?>, Integer> inDegree = new HashMap<>(8);
        Map<Class<?>, List<Class<?>>> parentsMap = new HashMap<>(8);

        for (Class<?> clazz : allInterfaces) {
            inDegree.putIfAbsent(clazz, 0);

            Class<?>[] parents = clazz.getInterfaces();
            for (Class<?> parent : parents) {
                inDegree.put(parent, inDegree.getOrDefault(parent, 0) + 1);
                parentsMap.computeIfAbsent(clazz, k -> new ArrayList<>()).add(parent);
            }
        }

        Queue<Class<?>> queue = new LinkedList<>();
        inDegree.forEach((clazz, degree) -> {
            if (degree == 0) queue.offer(clazz);
        });

        List<Class<?>> result = new ArrayList<>();
        while (!queue.isEmpty()) {
            Class<?> current = queue.poll();
            result.add(current);

            List<Class<?>> parents = parentsMap.get(current);
            if (parents != null) {
                for (Class<?> parent : parents) {
                    int remainingDegree = inDegree.get(parent) - 1;
                    inDegree.put(parent, remainingDegree);
                    if (remainingDegree == 0) {
                        queue.offer(parent);
                    }
                }
            }
        }

        return Collections.unmodifiableList(result);
    }

    private static void collectInterfaces(Class<?> clazz, Set<Class<?>> visited) {
        if (visited.add(clazz)) {
            for (Class<?> parent : clazz.getInterfaces()) {
                collectInterfaces(parent, visited);
            }
        }
    }

    public static Class<?> getProxiedClass(Class<?> clazz) {
        ReflectionProxy proxy = clazz.getDeclaredAnnotation(ReflectionProxy.class);
        if (proxy == null) {
            throw new IllegalArgumentException("Class " + clazz + " has no @ReflectionProxy annotation");
        }
        if (SReflection.getFilter().test(proxy.activeIf())) {
            Class<?> proxiedClass = getProxiedClass(clazz, proxy);
            if (proxy.nullable()) {
                return proxiedClass;
            }
            return Objects.requireNonNull(proxiedClass, "Cannot find proxied class for " + clazz);
        } else {
            return null;
        }
    }

    @SuppressWarnings("DuplicatedCode")
    @Nullable
    private static Class<?> getProxiedClass(Class<?> clazz, ReflectionProxy proxy) {
        if (proxy.clazz() == Object.class && proxy.name().length == 0) {
            throw new IllegalArgumentException("ReflectionProxy doesn't have value or class name set for class " + clazz);
        }
        if (proxy.clazz() != Object.class) {
            return proxy.clazz();
        }
        if (proxy.ignoreRelocation()) {
            String[] name = proxy.name();
            if (name.length == 1) {
                return SparrowClass.find(name[0].replace("{}", "."));
            } else {
                return SparrowClass.find(Arrays.stream(name).map(it -> it.replace("{}", ".")).toArray(String[]::new));
            }
        } else {
            return SparrowClass.find(proxy.name());
        }
    }

    public static void checkArgumentCount(Method method, int expected) {
        int length = method.getParameterCount();
        if (length != expected) {
            throw new IllegalArgumentException("Method " + method.getName() + " has " + length + " parameters but expected " + expected);
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static Class<?> getParameterClass(Parameter parameter) {
        Type type = parameter.getDeclaredAnnotation(Type.class);
        if (type == null) {
            return parameter.getType();
        }
        if (type.clazz() == Object.class && type.name().length == 0) {
            throw new IllegalArgumentException("Type annotation doesn't have value or class name set for parameter " + parameter);
        }
        if (type.clazz() != Object.class) {
            return getProxiedClass(type.clazz());
        }
        if (type.ignoreRelocation()) {
            String[] name = type.name();
            if (name.length == 1) {
                return SparrowClass.find(name[0].replace("{}", "."));
            } else {
                return SparrowClass.find(Arrays.stream(name).map(it -> it.replace("{}", ".")).toArray(String[]::new));
            }
        } else {
            return SparrowClass.find(type.name());
        }
    }

    @SuppressWarnings("DuplicatedCode")
    public static FieldMatcher getFieldMatcher(FieldGetter fieldGetter) {
        FieldMatcher matcher;
        if (fieldGetter.name().length != 0) {
            matcher = FieldMatcher.named(fieldGetter.name());
        } else {
            throw new IllegalArgumentException("FieldGetter doesn't have name or names set");
        }
        if (fieldGetter.isStatic()) {
            matcher.and(FieldMatcher.staticField());
        } else {
            matcher.and(FieldMatcher.instanceField());
        }
        return matcher;
    }

    @SuppressWarnings("DuplicatedCode")
    public static FieldMatcher getFieldMatcher(FieldSetter fieldSetter) {
        FieldMatcher matcher;
        if (fieldSetter.name().length != 0) {
            matcher = FieldMatcher.named(fieldSetter.name());
        } else {
            throw new IllegalArgumentException("FieldSetter doesn't have name or names set");
        }
        if (fieldSetter.isStatic()) {
            matcher.and(FieldMatcher.staticField());
        } else {
            matcher.and(FieldMatcher.instanceField());
        }
        return matcher;
    }

    public static MethodMatcher createMethodMatcher(MethodInvoker invoker) {
        MethodMatcher matcher;
        if (invoker.name().length != 0) {
            matcher = MethodMatcher.named(invoker.name());
        } else {
            throw new IllegalArgumentException("MethodInvoker doesn't have name or names set");
        }
        if (invoker.isStatic()) {
            matcher.and(MethodMatcher.staticMethod());
        } else {
            matcher.and(MethodMatcher.instanceMethod());
        }
        return matcher;
    }

    public static void analyseAndApply(ProxyBuilder builder, List<Class<?>> interfaces) {
        for (Class<?> proxyClass : interfaces) {
            Class<?> targetClazz = Util.getProxiedClass(proxyClass);
            // 版本原因找不到类
            if (targetClazz == null) {
                continue;
            }
            analyseAndApply(proxyClass, targetClazz, builder);
        }
    }

    private static void analyseAndApply(Class<?> proxyClass, Class<?> targetClass, ProxyBuilder builder) {
        SparrowClass<?> spaClass = new SparrowClass<>(targetClass);
        for (Method method : proxyClass.getDeclaredMethods()) {

            // 字段获取
            FieldGetter fieldGetter = method.getAnnotation(FieldGetter.class);
            if (fieldGetter != null && SReflection.getFilter().test(fieldGetter.activeIf())) {
                // 字段只需 名称 即可确定
                Field field = spaClass.getDeclaredField(Util.getFieldMatcher(fieldGetter));
                Objects.requireNonNull(field, "Field not found for proxy " + proxyClass + "#" + method.getName());
                Util.checkArgumentCount(method, Modifier.isStatic(field.getModifiers()) ? 0 : 1);
                builder.writeFieldGetter(method, field);
                continue;
            }

            // 字段设置
            FieldSetter fieldSetter = method.getAnnotation(FieldSetter.class);
            if (fieldSetter != null && SReflection.getFilter().test(fieldSetter.activeIf())) {
                Field field = spaClass.getDeclaredField(Util.getFieldMatcher(fieldSetter));
                Objects.requireNonNull(field, "Field not found for proxy " + proxyClass + "#" + method.getName());
                Util.checkArgumentCount(method, Modifier.isStatic(field.getModifiers()) ? 1 : 2);
                builder.writeFieldSetter(method, field);
                continue;
            }

            // 方法调用
            MethodInvoker methodInvoker = method.getAnnotation(MethodInvoker.class);
            if (methodInvoker != null && SReflection.getFilter().test(methodInvoker.activeIf())) {
                Class<?>[] parameterTypes = Arrays.stream(method.getParameters())
                        .skip(methodInvoker.isStatic() ? 0 : 1)
                        .map(Util::getParameterClass)
                        .toArray(Class<?>[]::new);
                // 方法只需要 参数 + 名称，即可确定唯一方法
                Method targetMethod = spaClass.getDeclaredMethod(Util.createMethodMatcher(methodInvoker).and(MethodMatcher.takeArguments(parameterTypes)));
                Objects.requireNonNull(targetMethod, "Method not found for proxy " + proxyClass + "#" + method.getName());
                if (!Modifier.isStatic(targetMethod.getModifiers())) {
                    if (method.getParameterCount() < 1) {
                        throw new IllegalArgumentException("Non-static method must have at least one argument");
                    }
                }
                builder.writeMethod(method, targetMethod);
                continue;
            }

            // 构造器
            ConstructorInvoker constructorInvoker = method.getAnnotation(ConstructorInvoker.class);
            if (constructorInvoker != null && SReflection.getFilter().test(constructorInvoker.activeIf())) {
                // 构造器只需要 参数 即可确定
                Class<?>[] parameterTypes = Arrays.stream(method.getParameters())
                        .map(Util::getParameterClass)
                        .toArray(Class<?>[]::new);
                Constructor<?> constructor = spaClass.getDeclaredConstructor(ConstructorMatcher.takeArguments(parameterTypes));
                Objects.requireNonNull(constructor, "Constructor not found for proxy " + proxyClass + "#" + method.getName());
                builder.writeConstructor(method, constructor);
                continue;
            }
        }
    }
}