package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatcher;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.SparrowField;
import net.momirealms.sparrow.reflection.method.SMethod;
import net.momirealms.sparrow.reflection.method.SparrowMethod;
import net.momirealms.sparrow.reflection.method.matcher.MethodMatcher;
import net.momirealms.sparrow.reflection.proxy.annotation.ConstructorInvoker;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldGetter;
import net.momirealms.sparrow.reflection.proxy.annotation.FieldSetter;
import net.momirealms.sparrow.reflection.proxy.annotation.MethodInvoker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

final class ReflectionProxyHandler<I> implements InvocationHandler {
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[]{};
    private final Class<I> proxyClass;
    private final Map<Method, ArgumentsConsumer> consumers;

    public ReflectionProxyHandler(Class<I> proxyClass) {
        this.proxyClass = proxyClass;
        this.consumers = new HashMap<>();
        this.analyse();
    }

    private void analyse() {
        Class<?> previousTarget = null;
        Class<?> previousProxy = null;
        // 获取全部父接口的方法
        for (Class<?> proxyClazz : Util.getTopDownInterfaceHierarchy(this.proxyClass)) {
            Class<?> targetClass = Util.getProxiedClass(proxyClazz);
            if (targetClass == null) {
                throw new IllegalArgumentException("Cannot find proxied class for " + proxyClazz);
            }
            if (previousTarget != null && !previousTarget.isAssignableFrom(targetClass)) {
                throw new IllegalArgumentException("Incompatible class found. " +
                        "Proxied class: " + targetClass + " and " + previousTarget + "; " +
                        "Proxy class: " + proxyClazz + " and " + previousProxy);
            }
            this.analyse(proxyClazz, targetClass);
            previousProxy = proxyClazz;
            previousTarget = targetClass;
        }
    }

    private void analyse(Class<?> proxyClass, Class<?> targetClass) {
        SparrowClass<?> spaClass = new SparrowClass<>(targetClass);
        for (Method method : proxyClass.getDeclaredMethods()) {

            // 构造器
            ConstructorInvoker constructorInvoker = method.getAnnotation(ConstructorInvoker.class);
            if (constructorInvoker != null) {
                // 构造器只需要 参数 即可确定
                Class<?>[] parameterTypes = Arrays.stream(method.getParameters())
                        .map(Util::getParameterClass)
                        .toArray(Class<?>[]::new);
                SparrowConstructor<?> spaConstructor = spaClass.getDeclaredSparrowConstructor(ConstructorMatcher.takeArguments(parameterTypes));
                Objects.requireNonNull(spaConstructor, "Constructor not found for proxy class " + proxyClass + "#" + method.getName());
                this.consumers.put(method, new ConstructorInvokerConsumer(
                        constructorInvoker.strategy() == Strategy.ASM ? spaConstructor.asm() : spaConstructor.unsafe()
                ));
                continue;
            }

            // 字段获取
            FieldGetter fieldGetter = method.getAnnotation(FieldGetter.class);
            if (fieldGetter != null) {
                // 字段只需 名称 即可确定
                SparrowField spaField = spaClass.getDeclaredSparrowField(Util.getFieldMatcher(fieldGetter));
                Objects.requireNonNull(spaField, "Field not found for proxy class " + proxyClass + "#" + method.getName());
                if (fieldGetter.isStatic()) {
                    Util.checkArgumentCount(method, 0);
                    this.consumers.put(method, new StaticFieldGetterConsumer(fieldGetter.strategy() == Strategy.ASM ? spaField.asm() : spaField.mh()));
                } else {
                    Util.checkArgumentCount(method, 1);
                    this.consumers.put(method, new FieldGetterConsumer(fieldGetter.strategy() == Strategy.ASM ? spaField.asm() : spaField.mh()));
                }
                continue;
            }

            // 字段设置
            FieldSetter fieldSetter = method.getAnnotation(FieldSetter.class);
            if (fieldSetter != null) {
                SparrowField spaField = spaClass.getDeclaredSparrowField(Util.getFieldMatcher(fieldSetter));
                Objects.requireNonNull(spaField, "Field not found for proxy class " + proxyClass + "#" + method.getName());
                if (fieldSetter.isStatic()) {
                    Util.checkArgumentCount(method, 1);
                    this.consumers.put(method, new StaticFieldSetterConsumer(fieldSetter.strategy() == Strategy.ASM ? spaField.asm() : spaField.mh()));
                } else {
                    Util.checkArgumentCount(method, 2);
                    this.consumers.put(method, new FieldSetterConsumer(fieldSetter.strategy() == Strategy.ASM ? spaField.asm() : spaField.mh()));
                }
                continue;
            }

            // 方法调用
            MethodInvoker methodInvoker = method.getAnnotation(MethodInvoker.class);
            if (methodInvoker != null) {
                Class<?>[] parameterTypes = Arrays.stream(method.getParameters())
                            .skip(methodInvoker.isStatic() ? 0 : 1)
                            .map(Util::getParameterClass)
                            .toArray(Class<?>[]::new);
                // 方块只需要 参数 + 名称，即可确定唯一方法
                SparrowMethod spaMethod = spaClass.getDeclaredSparrowMethod(Util.createMethodMatcher(methodInvoker).and(MethodMatcher.takeArguments(parameterTypes)));
                Objects.requireNonNull(spaMethod, "Method not found for proxy class " + proxyClass + "#" + method.getName());
                if (methodInvoker.isStatic()) {
                    this.consumers.put(method, new StaticMethodInvokerConsumer(spaMethod.asm()));
                } else {
                    if (method.getParameterCount() < 1) {
                        throw new IllegalArgumentException("Non-static method must have at least one argument");
                    }
                    this.consumers.put(method, new MethodInvokerConsumer(spaMethod.asm()));
                }
            }
        }
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        ArgumentsConsumer argumentsConsumer = this.consumers.get(method);
        if (argumentsConsumer != null) {
            if (args == null) {
                args = EMPTY_OBJECT_ARRAY;
            }
            return argumentsConsumer.apply(args);
        }
        throw new RuntimeException("Unsupported method: " + method);
    }

    interface ArgumentsConsumer extends Function<Object[], Object> {
    }

    private record StaticFieldSetterConsumer(SField sField) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            this.sField.set(null, args[0]);
            return null;
        }
    }

    private record FieldSetterConsumer(SField sField) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            this.sField.set(args[0], args[1]);
            return null;
        }
    }

    private record StaticFieldGetterConsumer(SField sField) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            return this.sField.get(null);
        }
    }

    private record FieldGetterConsumer(SField sField) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            return this.sField.get(args[0]);
        }
    }

    private record StaticMethodInvokerConsumer(SMethod sMethod) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            return this.sMethod.invoke(null, args);
        }
    }

    private record MethodInvokerConsumer(SMethod sMethod) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            Object[] realArgs = new Object[args.length - 1];
            System.arraycopy(args, 1, realArgs, 0, realArgs.length);
            return this.sMethod.invoke(args[0], realArgs);
        }
    }

    private record ConstructorInvokerConsumer(SConstructor sConstructor) implements ArgumentsConsumer {

        @Override
        public Object apply(Object[] args) {
            return this.sConstructor.newInstance(args);
        }
    }
}
