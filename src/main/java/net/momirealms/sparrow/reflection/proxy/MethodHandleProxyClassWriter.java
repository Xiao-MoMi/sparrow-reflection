package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * 跨 ClassLoader 场景的 writer, 把生成类定义到 proxy 接口所在 loader 和 package 下.
 *
 * <p>所有目标成员访问都会先由反射解析为 MethodHandle,再通过静态 HANDLE_n 槽位回填到 hidden class 中.</p>
 */
final class MethodHandleProxyClassWriter extends AbstractProxyClassWriter {

    @Override
    protected Class<?> lookupHost(ProxyDefinition definition) {
        return definition.proxyType();
    }

    @Override
    protected String internalName(ProxyDefinition definition) {
        return Type.getInternalName(definition.proxyType()) + "$HandleProxy";
    }

    /**
     * 从静态 HANDLE_n 调用 getter 句柄.
     */
    @Override
    protected void writeFieldGetter(EmissionContext ctx, Method method, Field field) {
        if (ctx.hasWritten(method)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        String handleFieldName = addStaticHandle(ctx, SReflection.unreflectGetter(field));
        mv.visitFieldInsn(GETSTATIC, ctx.internalName, handleFieldName, "Ljava/lang/invoke/MethodHandle;");

        // 实例字段的 MethodHandle 形状是 (owner)fieldType, 静态字段则是 ()fieldType.
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getDeclaringClass()));
        }

        Class<?> fieldType = field.getType();
        Class<?> returnType = method.getReturnType();
        String fieldDescriptor = Type.getDescriptor(fieldType);
        String invokeDesc = isStatic
                ? "()" + fieldDescriptor
                : "(" + Type.getDescriptor(field.getDeclaringClass()) + ")" + fieldDescriptor;
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", invokeDesc, false);

        writeGetterReturnCast(mv, method, field, fieldType, returnType, fieldDescriptor);
        mv.visitInsn(Type.getType(returnType).getOpcode(IRETURN));

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 所有字段写入都走 MethodHandle, 包括普通字段和 final 字段.
     */
    @Override
    protected void writeFieldSetter(EmissionContext ctx, Method method, Field field) {
        if (ctx.hasWritten(method)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        String handleFieldName = addStaticHandle(ctx, SReflection.unreflectSetter(field));
        mv.visitFieldInsn(GETSTATIC, ctx.internalName, handleFieldName, "Ljava/lang/invoke/MethodHandle;");

        // setter 句柄形状随 static 变化: (value)V 或 (owner,value)V.
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
     * 通过静态 MethodHandle 槽保持调用方 proxy 接口 classloader 与目标成员 classloader 解耦.
     */
    @Override
    protected void writeMethod(EmissionContext ctx, Method proxyMethod, Method targetMethod) {
        if (ctx.hasWritten(proxyMethod)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, proxyMethod.getName(), Type.getMethodDescriptor(proxyMethod), null, null);
        mv.visitCode();

        String handleFieldName = addStaticHandle(ctx, SReflection.unreflectMethod(targetMethod));
        mv.visitFieldInsn(GETSTATIC, ctx.internalName, handleFieldName, "Ljava/lang/invoke/MethodHandle;");

        Class<?> owner = targetMethod.getDeclaringClass();
        Class<?>[] targetParamTypes = targetMethod.getParameterTypes();
        boolean isStaticTarget = Modifier.isStatic(targetMethod.getModifiers());

        // 实例方法的句柄第一个参数是 owner receiver, proxy 方法第一个参数也约定为目标对象.
        if (!isStaticTarget) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        int currentSlot = isStaticTarget ? 1 : 2;
        loadAndCastTargetArguments(mv, targetParamTypes, currentSlot);

        String invokeDesc = createMethodHandleDescriptor(
                isStaticTarget ? null : owner,
                targetParamTypes,
                targetMethod.getReturnType()
        );
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", invokeDesc, false);

        writeReturn(mv, proxyMethod, targetMethod);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    /**
     * 构造器 MethodHandle 返回新建目标实例, 再按 proxy 方法声明转换返回值.
     */
    @Override
    protected void writeConstructor(EmissionContext ctx, Method method, Constructor<?> constructor) {
        if (ctx.hasWritten(method)) {
            return;
        }

        MethodVisitor mv = ctx.classWriter.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        String handleFieldName = addStaticHandle(ctx, SReflection.unreflectConstructor(constructor));
        mv.visitFieldInsn(GETSTATIC, ctx.internalName, handleFieldName, "Ljava/lang/invoke/MethodHandle;");

        Class<?> owner = constructor.getDeclaringClass();
        Class<?>[] targetParamTypes = constructor.getParameterTypes();
        loadAndCastTargetArguments(mv, targetParamTypes, 1);

        String invokeDesc = createMethodHandleDescriptor(null, targetParamTypes, owner);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", invokeDesc, false);
        writeConstructorReturn(mv, method, owner);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }
}
