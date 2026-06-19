package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 同 ClassLoader 场景的 writer, 把生成类定义成目标类的 nestmate.
 *
 * <p>该策略依赖 hidden class 的 NESTMATE 权限直接访问目标类私有成员.
 * 它只在 proxy 接口和目标类由同一个 ClassLoader 加载时使用, 避免父 loader 无法解析子 loader proxy 接口.</p>
 */
final class NestmateProxyClassWriter extends AbstractProxyClassWriter {

    @Override
    protected Class<?> lookupHost(ProxyDefinition definition) {
        return definition.targetType();
    }

    @Override
    protected String internalName(ProxyDefinition definition) {
        return Type.getInternalName(definition.targetType()) + "$Proxy";
    }

    /**
     * 兼容目标字段基本类型装箱到代理方法返回 Object 的场景.
     */
    @Override
    protected void writeFieldGetter(EmissionContext ctx, Method method, Field field) {
        if (ctx.hasWritten(method)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        Class<?> fieldType = field.getType();
        Class<?> returnType = method.getReturnType();
        String owner = Type.getInternalName(field.getDeclaringClass());
        String fieldDescriptor = Type.getDescriptor(fieldType);

        // 实例字段需从第一个参数加载目标对象, 静态字段则直接 GETSTATIC.
        if (!Modifier.isStatic(field.getModifiers())) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, owner);
            mv.visitFieldInsn(GETFIELD, owner, field.getName(), fieldDescriptor);
        } else {
            mv.visitFieldInsn(GETSTATIC, owner, field.getName(), fieldDescriptor);
        }

        writeGetterReturnCast(mv, method, field, fieldType, returnType, fieldDescriptor);
        mv.visitInsn(Type.getType(returnType).getOpcode(IRETURN));

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 写入普通字段 setter, final 字段会切换到 MethodHandle 写入路径.
     */
    @Override
    protected void writeFieldSetter(EmissionContext ctx, Method method, Field field) {
        if (ctx.hasWritten(method)) {
            return;
        }
        if (Modifier.isFinal(field.getModifiers())) {
            writeFinalFieldSetter(ctx, method, field);
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        Class<?> fieldType = field.getType();
        String owner = Type.getInternalName(field.getDeclaringClass());
        String fieldDescriptor = Type.getDescriptor(fieldType);

        // 实例字段需要先压入 owner, 写入值位于第二个参数槽位.
        if (!Modifier.isStatic(field.getModifiers())) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, owner);
        }

        int valueParamIndex = Modifier.isStatic(field.getModifiers()) ? 1 : 2;
        loadAndCastSetterValue(mv, method, field, valueParamIndex);

        if (!Modifier.isStatic(field.getModifiers())) {
            mv.visitFieldInsn(PUTFIELD, owner, field.getName(), fieldDescriptor);
        } else {
            mv.visitFieldInsn(PUTSTATIC, owner, field.getName(), fieldDescriptor);
        }

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * final 字段通过静态 MethodHandle 槽位延迟绑定 setter.
     */
    private static void writeFinalFieldSetter(EmissionContext ctx, Method method, Field field) {
        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        String handleFieldName = addStaticHandle(ctx, SReflection.unreflectSetter(field));
        mv.visitFieldInsn(GETSTATIC, ctx.internalName, handleFieldName, "Ljava/lang/invoke/MethodHandle;");

        boolean isStatic = Modifier.isStatic(field.getModifiers());
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getDeclaringClass()));
        }

        int valueParamIndex = isStatic ? 1 : 2;
        loadAndCastSetterValue(mv, method, field, valueParamIndex);

        String fieldDescriptor = Type.getDescriptor(field.getType());
        String invokeDesc = isStatic
                ? "(" + fieldDescriptor + ")V"
                : "(" + Type.getDescriptor(field.getDeclaringClass()) + fieldDescriptor + ")V";
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", invokeDesc, false);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 写入目标方法调用, 实例方法约定 proxy 方法第一个参数为目标对象.
     */
    @Override
    protected void writeMethod(EmissionContext ctx, Method proxyMethod, Method targetMethod) {
        if (ctx.hasWritten(proxyMethod)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, proxyMethod.getName(), Type.getMethodDescriptor(proxyMethod), null, null);
        mv.visitCode();

        Class<?> owner = targetMethod.getDeclaringClass();
        Class<?>[] targetParamTypes = targetMethod.getParameterTypes();
        boolean isStaticTarget = Modifier.isStatic(targetMethod.getModifiers());

        // 实例调用先加载 receiver, 参数槽位从 2 开始; 静态调用没有 receiver, 从 1 开始.
        if (!isStaticTarget) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        int currentSlot = isStaticTarget ? 1 : 2;
        loadAndCastTargetArguments(mv, targetParamTypes, currentSlot);

        int opcode = isStaticTarget ? INVOKESTATIC : (owner.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
        mv.visitMethodInsn(
                opcode,
                Type.getInternalName(owner),
                targetMethod.getName(),
                Type.getMethodDescriptor(targetMethod),
                owner.isInterface()
        );

        writeReturn(mv, proxyMethod, targetMethod);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 写入目标构造器调用, 返回值始终是新建出的目标对象.
     */
    @Override
    protected void writeConstructor(EmissionContext ctx, Method method, Constructor<?> constructor) {
        if (ctx.hasWritten(method)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        Class<?> owner = constructor.getDeclaringClass();
        String internalName = Type.getInternalName(owner);
        Class<?>[] targetParamTypes = constructor.getParameterTypes();

        mv.visitTypeInsn(NEW, internalName);
        mv.visitInsn(DUP);
        loadAndCastTargetArguments(mv, targetParamTypes, 1);

        mv.visitMethodInsn(
                INVOKESPECIAL,
                internalName,
                "<init>",
                Type.getConstructorDescriptor(constructor),
                false
        );
        mv.visitInsn(ARETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
