package net.momirealms.sparrow.reflection.constructor;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.util.AsmUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("DuplicatedCode")
final class ConstructorInvokerFactory implements Opcodes {
    private static final AtomicInteger ID = new AtomicInteger(0);

    static SConstructor create(Constructor<?> constructor) throws Exception {
        Class<?> owner = constructor.getDeclaringClass();
        String constructorDescriptor = Type.getConstructorDescriptor(constructor);
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "SConstructor_" + ID.getAndIncrement();
        byte[] bytes = generateByteCode(internalClassName, owner, constructorDescriptor, parameterTypes);
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (SConstructor) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(String className, Class<?> owner, String descriptor, Class<?>[] params) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);

        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{Type.getInternalName(SConstructor.class)});

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC | ACC_VARARGS, "newInstance", "([Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        String ownerInternalName = Type.getInternalName(owner);
        mv.visitTypeInsn(NEW, ownerInternalName);
        mv.visitInsn(DUP);

        for (int i = 0; i < params.length; i++) {
            mv.visitVarInsn(ALOAD, 1);
            AsmUtils.pushInt(mv, i);
            mv.visitInsn(AALOAD);
            AsmUtils.unboxAndCast(mv, Type.getDescriptor(params[i]));
        }

        mv.visitMethodInsn(INVOKESPECIAL, ownerInternalName, "<init>", descriptor, false);

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}