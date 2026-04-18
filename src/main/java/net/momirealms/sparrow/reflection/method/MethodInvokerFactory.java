package net.momirealms.sparrow.reflection.method;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.util.ASMUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@SuppressWarnings("DuplicatedCode")
final class MethodInvokerFactory implements Opcodes {
    private MethodInvokerFactory() {}
    private static final String SUPER_NAME = Type.getInternalName(SMethod.class);

    static SMethod create(Method method) throws Exception {
        Class<?> owner = method.getDeclaringClass();
        String methodName = method.getName();
        String methodDescriptor = Type.getMethodDescriptor(method);
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();

        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.getAsmClassPrefix() + "Method_" + methodName;

        byte[] bytes = generateByteCode(internalClassName, owner, methodName, methodDescriptor, isStatic, parameterTypes, returnType);

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.getLookup());
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (SMethod) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(String className, Class<?> owner, String methodName, String methodDescriptor, boolean isStatic, Class<?>[] params, Class<?> returnType) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, SUPER_NAME, null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUPER_NAME, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        for (int i = 0; i < params.length; i++) {
            mv.visitVarInsn(ALOAD, 2);
            ASMUtils.pushInt(mv, i);
            mv.visitInsn(AALOAD);
            ASMUtils.unboxAndCast(mv, Type.getDescriptor(params[i]));
        }

        int opcode = isStatic ? INVOKESTATIC : (owner.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
        mv.visitMethodInsn(opcode, Type.getInternalName(owner), methodName, methodDescriptor, owner.isInterface());

        if (returnType == void.class) {
            mv.visitInsn(ACONST_NULL);
        } else {
            ASMUtils.box(mv, Type.getDescriptor(returnType));
        }

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}