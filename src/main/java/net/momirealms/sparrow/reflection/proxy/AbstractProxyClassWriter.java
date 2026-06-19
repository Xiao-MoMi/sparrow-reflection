package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.util.ASMUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;

// 代理实现 writer 的共享模板, 负责类壳生成和通用的 ASM 类型转换.
abstract class AbstractProxyClassWriter implements ProxyClassWriter, Opcodes {

    /**
     * 生成一个实现 proxy 接口的隐藏类字节码, 并收集需要 loader 回填的静态 MethodHandle.
     */
    @Override
    public final ProxyClassBytes write(ProxyDefinition definition) {
        Objects.requireNonNull(definition, "Proxy definition must not be null");

        String internalName = internalName(definition);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        classWriter.visit(
                V17,
                ACC_PUBLIC | ACC_FINAL,
                internalName,
                null,
                "java/lang/Object",
                new String[]{Type.getInternalName(definition.proxyType())}
        );

        writeDefaultConstructor(classWriter);

        EmissionContext ctx = new EmissionContext(classWriter, internalName);
        for (ProxyBinding binding : definition.bindings()) {
            writeBinding(ctx, binding);
        }

        classWriter.visitEnd();
        return new ProxyClassBytes(
                lookupHost(definition),
                classWriter.toByteArray(),
                List.copyOf(ctx.staticHandleBindings)
        );
    }

    /**
     * 返回 defineHiddenClass 使用的 lookup host.
     */
    protected abstract Class<?> lookupHost(ProxyDefinition definition);

    /**
     * 返回生成类内部名. hidden class 要求内部名和 lookup host 位于同一包内.
     */
    protected abstract String internalName(ProxyDefinition definition);

    /**
     * 写入字段 getter 的具体访问策略.
     */
    protected abstract void writeFieldGetter(EmissionContext ctx, Method method, Field field);

    /**
     * 写入字段 setter 的具体访问策略.
     */
    protected abstract void writeFieldSetter(EmissionContext ctx, Method method, Field field);

    /**
     * 写入目标方法调用的具体访问策略.
     */
    protected abstract void writeMethod(EmissionContext ctx, Method proxyMethod, Method targetMethod);

    /**
     * 写入目标构造器调用的具体访问策略.
     */
    protected abstract void writeConstructor(EmissionContext ctx, Method method, Constructor<?> constructor);

    private static void writeDefaultConstructor(ClassWriter classWriter) {
        MethodVisitor mv = classWriter.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    private void writeBinding(EmissionContext ctx, ProxyBinding binding) {
        if (binding instanceof ProxyBinding.FieldGetter getField) {
            writeFieldGetter(ctx, getField.proxyMethod(), getField.targetField());
            return;
        }
        if (binding instanceof ProxyBinding.FieldSetter setField) {
            writeFieldSetter(ctx, setField.proxyMethod(), setField.targetField());
            return;
        }
        if (binding instanceof ProxyBinding.MethodInvoker invokeMethod) {
            writeMethod(ctx, invokeMethod.proxyMethod(), invokeMethod.targetMethod());
            return;
        }
        if (binding instanceof ProxyBinding.ConstructorInvoker invokeConstructor) {
            writeConstructor(ctx, invokeConstructor.proxyMethod(), invokeConstructor.targetConstructor());
        }
    }

    /**
     * 根据字段真实类型和 proxy 返回类型写入 getter 返回值转换.
     */
    protected static void writeGetterReturnCast(
            MethodVisitor mv,
            Method method,
            Field field,
            Class<?> fieldType,
            Class<?> returnType,
            String fieldDescriptor
    ) {
        if (returnType.isPrimitive()) {
            if (!fieldType.isPrimitive()) {
                throw new IllegalArgumentException(String.format(
                        "Cannot unbox object field '%s' (%s) to primitive return type '%s' in method '%s'",
                        field.getName(), fieldType.getSimpleName(), returnType.getSimpleName(), method.getName()
                ));
            }
            if (fieldType != returnType) {
                throw new IllegalArgumentException(String.format(
                        "Incompatible primitive types in method '%s': cannot return field '%s' of type '%s' as '%s'",
                        method.getName(), field.getName(), fieldType.getSimpleName(), returnType.getSimpleName()
                ));
            }
            return;
        }

        if (fieldType.isPrimitive()) {
            ASMUtils.box(mv, fieldDescriptor);
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
        }
    }

    /**
     * 从 proxy 方法参数槽加载 setter 值, 并转换成目标字段类型.
     */
    protected static void loadAndCastSetterValue(
            MethodVisitor mv,
            Method method,
            Field field,
            int valueParamIndex
    ) {
        Class<?> fieldType = field.getType();
        Class<?> proxyParamType = method.getParameterTypes()[valueParamIndex - 1];
        Type asmProxyParamType = Type.getType(proxyParamType);
        mv.visitVarInsn(asmProxyParamType.getOpcode(ILOAD), valueParamIndex);

        if (fieldType.isPrimitive()) {
            if (!proxyParamType.isPrimitive()) {
                ASMUtils.unboxAndCast(mv, Type.getDescriptor(fieldType));
                return;
            }
            if (proxyParamType != fieldType) {
                throw new IllegalArgumentException(String.format(
                        "Primitive type mismatch in method '%s': cannot pass '%s' to field '%s' of type '%s' without explicit conversion",
                        method.getName(),
                        proxyParamType.getSimpleName(),
                        field.getName(),
                        fieldType.getSimpleName()
                ));
            }
            return;
        }

        mv.visitTypeInsn(CHECKCAST, Type.getInternalName(fieldType));
    }

    /**
     * 按目标方法真实返回类型和 proxy 方法声明写入 return 指令.
     */
    protected static void writeReturn(MethodVisitor mv, Method proxyMethod, Method targetMethod) {
        Class<?> targetReturnType = targetMethod.getReturnType();
        Class<?> proxyReturnType = proxyMethod.getReturnType();

        if (targetReturnType == void.class) {
            if (proxyReturnType != void.class) {
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(ARETURN);
            } else {
                mv.visitInsn(RETURN);
            }
            return;
        }

        if (targetReturnType.isPrimitive()) {
            writePrimitiveReturn(mv, targetReturnType, proxyReturnType);
        } else {
            writeObjectReturn(mv, targetReturnType, proxyReturnType);
        }
    }

    private static void writePrimitiveReturn(
            MethodVisitor mv,
            Class<?> targetReturnType,
            Class<?> proxyReturnType
    ) {
        if (!proxyReturnType.isPrimitive()) {
            ASMUtils.box(mv, Type.getDescriptor(targetReturnType));
            mv.visitInsn(ARETURN);
            return;
        }
        if (targetReturnType != proxyReturnType) {
            throw new IllegalArgumentException("Primitive mismatch: " + targetReturnType + " to " + proxyReturnType);
        }
        mv.visitInsn(Type.getType(proxyReturnType).getOpcode(IRETURN));
    }

    private static void writeObjectReturn(
            MethodVisitor mv,
            Class<?> targetReturnType,
            Class<?> proxyReturnType
    ) {
        if (proxyReturnType.isPrimitive()) {
            ASMUtils.unboxAndCast(mv, Type.getDescriptor(proxyReturnType));
            mv.visitInsn(Type.getType(proxyReturnType).getOpcode(IRETURN));
            return;
        }
        if (!proxyReturnType.isAssignableFrom(targetReturnType)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(proxyReturnType));
        }
        mv.visitInsn(ARETURN);
    }

    /**
     * 为当前生成类分配一个 HANDLE_n 静态字段, loader 阶段会按相同顺序回填 MethodHandle.
     */
    protected static String addStaticHandle(EmissionContext ctx, MethodHandle handle) {
        if (handle == null) {
            throw new IllegalArgumentException("Cannot create MethodHandle for proxy method");
        }

        String handleFieldName = "HANDLE_" + ctx.staticHandleBindings.size();
        ctx.classWriter
                .visitField(ACC_PRIVATE | ACC_STATIC, handleFieldName, "Ljava/lang/invoke/MethodHandle;", null, null)
                .visitEnd();
        ctx.staticHandleBindings.add(handle);
        return handleFieldName;
    }

    /**
     * 依次加载目标成员参数, 非基本类型参数按目标签名做 CHECKCAST.
     */
    protected static void loadAndCastTargetArguments(
            MethodVisitor mv,
            Class<?>[] targetParamTypes,
            int currentSlot
    ) {
        for (Class<?> targetParamType : targetParamTypes) {
            Type asmType = Type.getType(targetParamType);
            mv.visitVarInsn(asmType.getOpcode(ILOAD), currentSlot);
            if (!targetParamType.isPrimitive()) {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(targetParamType));
            }
            currentSlot += asmType.getSize();
        }
    }

    /**
     * 构造 MethodHandle.invokeExact 的描述符.
     *
     * <p>invokeExact 的 descriptor 必须和句柄 MethodType 完全一致, 不能只使用 proxy 方法描述符.
     * 实例方法和实例字段需要显式把 receiver 类型放进 descriptor 的第一个参数.</p>
     */
    protected static String createMethodHandleDescriptor(
            Class<?> receiverType,
            Class<?>[] argumentTypes,
            Class<?> returnType
    ) {
        StringBuilder descriptor = new StringBuilder("(");
        if (receiverType != null) {
            descriptor.append(Type.getDescriptor(receiverType));
        }
        for (Class<?> argumentType : argumentTypes) {
            descriptor.append(Type.getDescriptor(argumentType));
        }
        descriptor.append(')').append(Type.getDescriptor(returnType));
        return descriptor.toString();
    }

    /**
     * 处理构造器 MethodHandle 的返回值, 使 fallback 构造器和 direct NEW 路径保持一致.
     */
    protected static void writeConstructorReturn(MethodVisitor mv, Method method, Class<?> owner) {
        Class<?> proxyReturnType = method.getReturnType();
        if (proxyReturnType == void.class) {
            mv.visitInsn(POP);
            mv.visitInsn(RETURN);
            return;
        }
        if (proxyReturnType.isPrimitive()) {
            ASMUtils.unboxAndCast(mv, Type.getDescriptor(proxyReturnType));
            mv.visitInsn(Type.getType(proxyReturnType).getOpcode(IRETURN));
            return;
        }
        if (!proxyReturnType.isAssignableFrom(owner)) {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(proxyReturnType));
        }
        mv.visitInsn(ARETURN);
    }

    /**
     * 单次 class emission 的共享状态.
     */
    protected static final class EmissionContext {
        final ClassWriter classWriter; // 当前代理类的 ClassWriter.
        final String internalName; // 当前代理类内部名, 用于访问自身静态字段.
        final Set<String> writtenSignatures = new HashSet<>(); // 避免父子接口重复写入同一签名.
        final List<MethodHandle> staticHandleBindings = new ArrayList<>(4); // HANDLE_n 静态槽位绑定值.

        private EmissionContext(ClassWriter classWriter, String internalName) {
            this.classWriter = classWriter;
            this.internalName = internalName;
        }

        boolean hasWritten(Method method) {
            String signature = method.getName() + Type.getMethodDescriptor(method);
            return !this.writtenSignatures.add(signature);
        }
    }
}
