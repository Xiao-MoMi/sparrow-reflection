package net.momirealms.sparrow.reflection.proxy;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.util.AsmUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class AsmProxyBuilder implements ProxyBuilder, Opcodes {
    private final Set<String> writtenSignatures = new HashSet<>();
    private final ClassWriter cw;
    private final String internalName;
    private final List<MethodHandle> finalFields;
    private int count = 0;

    AsmProxyBuilder(ClassWriter classWriter, String internalName) {
        this.cw = classWriter;
        this.internalName = internalName;
        this.finalFields = new ArrayList<>(4);
    }

    public List<MethodHandle> finalFields() {
        return this.finalFields;
    }

    private boolean checkDuplicate(Method method) {
        String sig = method.getName() + Type.getMethodDescriptor(method);
        return !this.writtenSignatures.add(sig);
    }

    @Override
    public void writeFieldGetter(Method method, Field field) {
        MethodVisitor mv = this.cw.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        Class<?> fieldType = field.getType();
        Class<?> returnType = method.getReturnType();

        String owner = Type.getInternalName(field.getDeclaringClass());
        String fieldDescriptor = Type.getDescriptor(fieldType);

        if (!Modifier.isStatic(field.getModifiers())) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, owner);
            mv.visitFieldInsn(GETFIELD, owner, field.getName(), fieldDescriptor);
        } else {
            mv.visitFieldInsn(GETSTATIC, owner, field.getName(), fieldDescriptor);
        }

        if (returnType.isPrimitive()) {
            if (!fieldType.isPrimitive()) {
                throw new IllegalArgumentException(String.format(
                        "Cannot unbox object field '%s' (%s) to primitive return type '%s' in method '%s'",
                        field.getName(), fieldType.getSimpleName(), returnType.getSimpleName(), method.getName()));
            } else if (fieldType != returnType) {
                throw new IllegalArgumentException(String.format(
                        "Incompatible primitive types in method '%s': cannot return field '%s' of type '%s' as '%s'",
                        method.getName(), field.getName(), fieldType.getSimpleName(), returnType.getSimpleName()));
            }
        } else {
            if (fieldType.isPrimitive()) {
                AsmUtils.box(mv, fieldDescriptor);
            } else {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(returnType));
            }
        }

        mv.visitInsn(Type.getType(returnType).getOpcode(IRETURN));

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    @Override
    public void writeFieldSetter(Method method, Field field) {
        if (Modifier.isFinal(field.getModifiers())) {
            writeFinalFieldSetter(method, field);
            return;
        }

        MethodVisitor mv = this.cw.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        Class<?> fieldType = field.getType();
        int valueParamIndex = Modifier.isStatic(field.getModifiers()) ? 1 : 2;

        String owner = Type.getInternalName(field.getDeclaringClass());
        String fieldDescriptor = Type.getDescriptor(fieldType);

        if (!Modifier.isStatic(field.getModifiers())) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, owner);
        }

        Class<?> proxyParamType = method.getParameterTypes()[valueParamIndex - 1];
        Type asmProxyParamType = Type.getType(proxyParamType);
        mv.visitVarInsn(asmProxyParamType.getOpcode(ILOAD), valueParamIndex);

        if (fieldType.isPrimitive()) {
            if (!proxyParamType.isPrimitive()) {
                AsmUtils.unboxAndCast(mv, fieldDescriptor);
            } else if (proxyParamType != field.getType()) {
                throw new IllegalArgumentException(String.format(
                        "Primitive type mismatch in method '%s': cannot pass '%s' to field '%s' of type '%s' without explicit conversion",
                        method.getName(),
                        proxyParamType.getSimpleName(),
                        field.getName(),
                        field.getType().getSimpleName()
                ));
            }
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(fieldType));
        }

        if (!Modifier.isStatic(field.getModifiers())) {
            mv.visitFieldInsn(PUTFIELD, owner, field.getName(), fieldDescriptor);
        } else {
            mv.visitFieldInsn(PUTSTATIC, owner, field.getName(), fieldDescriptor);
        }

        mv.visitInsn(RETURN);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    private void writeFinalFieldSetter(Method method, Field field) {
        String handleFieldName = "HANDLE_" + this.count++;
        this.cw.visitField(ACC_PRIVATE | ACC_STATIC, handleFieldName, "Ljava/lang/invoke/MethodHandle;", null, null);

        MethodVisitor mv = this.cw.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        mv.visitFieldInsn(GETSTATIC, this.internalName, handleFieldName, "Ljava/lang/invoke/MethodHandle;");

        boolean isStatic = Modifier.isStatic(field.getModifiers());
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getDeclaringClass()));
        }

        int valueParamIndex = isStatic ? 1 : 2;
        Class<?> proxyParamType = method.getParameterTypes()[valueParamIndex - 1];
        Type asmProxyParamType = Type.getType(proxyParamType);
        mv.visitVarInsn(asmProxyParamType.getOpcode(ILOAD), valueParamIndex);

        String fieldDescriptor = Type.getDescriptor(field.getType());
        if (field.getType().isPrimitive()) {
            if (!proxyParamType.isPrimitive()) {
                AsmUtils.unboxAndCast(mv, fieldDescriptor);
            } else if (proxyParamType != field.getType()) {
                throw new IllegalArgumentException(String.format(
                        "Primitive type mismatch in method '%s': cannot pass '%s' to field '%s' of type '%s' without explicit conversion",
                        method.getName(),
                        proxyParamType.getSimpleName(),
                        field.getName(),
                        field.getType().getSimpleName()
                ));
            }
        } else {
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(field.getType()));
        }

        String invokeDesc = isStatic ? "(" + fieldDescriptor + ")V" : "(" + Type.getDescriptor(field.getDeclaringClass()) + fieldDescriptor + ")V";

        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/invoke/MethodHandle", "invokeExact", invokeDesc, false);

        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        MethodHandle handle = SReflection.unreflectSetter(SReflection.setAccessible(field));
        assert handle != null;
        handle.asType(handle.type().changeParameterType(isStatic ? 0 : 1, method.getParameterTypes()[0]));
        this.finalFields.add(handle);
    }

    @Override
    public void writeMethod(Method proxyMethod, Method targetMethod) {
        if (checkDuplicate(proxyMethod)) return;
        MethodVisitor mv = this.cw.visitMethod(ACC_PUBLIC, proxyMethod.getName(), Type.getMethodDescriptor(proxyMethod), null, null);
        mv.visitCode();

        Class<?> owner = targetMethod.getDeclaringClass();
        Class<?>[] targetParamTypes = targetMethod.getParameterTypes();
        boolean isStaticTarget = Modifier.isStatic(targetMethod.getModifiers());

        if (!isStaticTarget) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        int currentSlot = isStaticTarget ? 1 : 2;

        for (Class<?> targetParamType : targetParamTypes) {
            Type asmType = Type.getType(targetParamType);
            mv.visitVarInsn(asmType.getOpcode(ILOAD), currentSlot);
            if (!targetParamType.isPrimitive()) {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(targetParamType));
            }
            currentSlot += asmType.getSize();
        }

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

    private void writeReturn(MethodVisitor mv, Method proxyMethod, Method targetMethod) {
        Class<?> targetRet = targetMethod.getReturnType();
        Class<?> proxyRet = proxyMethod.getReturnType();

        if (targetRet == void.class) {
            if (proxyRet != void.class) {
                mv.visitInsn(ACONST_NULL);
                mv.visitInsn(ARETURN);
            } else {
                mv.visitInsn(RETURN);
            }
            return;
        }

        if (targetRet.isPrimitive()) {
            if (!proxyRet.isPrimitive()) {
                // 基本 -> 包装 (装箱)
                AsmUtils.box(mv, Type.getDescriptor(targetRet));
                mv.visitInsn(ARETURN);
            } else {
                // 基本 -> 基本
                if (targetRet != proxyRet) {
                    throw new IllegalArgumentException("Primitive mismatch: " + targetRet + " to " + proxyRet);
                }
                mv.visitInsn(Type.getType(proxyRet).getOpcode(IRETURN));
            }
        }
        else {
            if (proxyRet.isPrimitive()) {
                // 对象 -> 基本 (拆箱)
                AsmUtils.unboxAndCast(mv, Type.getDescriptor(proxyRet));
                mv.visitInsn(Type.getType(proxyRet).getOpcode(IRETURN));
            } else {
                // 对象 -> 对象
                if (!proxyRet.isAssignableFrom(targetRet)) {
                    mv.visitTypeInsn(CHECKCAST, Type.getInternalName(proxyRet));
                }
                mv.visitInsn(ARETURN);
            }
        }
    }

    @Override
    public void writeConstructor(Method method, Constructor<?> constructor) {
        MethodVisitor mv = this.cw.visitMethod(ACC_PUBLIC, method.getName(), Type.getMethodDescriptor(method), null, null);
        mv.visitCode();

        Class<?> owner = constructor.getDeclaringClass();
        String internalName = Type.getInternalName(owner);
        Class<?>[] targetParamTypes = constructor.getParameterTypes();

        mv.visitTypeInsn(NEW, internalName);
        mv.visitInsn(DUP);

        int currentSlot = 1;

        for (Class<?> targetParamType : targetParamTypes) {
            Type asmType = Type.getType(targetParamType);
            mv.visitVarInsn(asmType.getOpcode(ILOAD), currentSlot);
            if (!targetParamType.isPrimitive()) {
                mv.visitTypeInsn(CHECKCAST, Type.getInternalName(targetParamType));
            }
            currentSlot += asmType.getSize();
        }

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
