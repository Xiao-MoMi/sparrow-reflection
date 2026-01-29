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
final class OptimizedConstructorInvokerFactory implements Opcodes {
    private OptimizedConstructorInvokerFactory() {}
    private static final AtomicInteger ID = new AtomicInteger(0);
    private static final Class<?>[] ABSTRACT_CLASSES = new Class<?>[]{
            SConstructor0.class, SConstructor1.class, SConstructor2.class, SConstructor3.class,
            SConstructor4.class, SConstructor5.class, SConstructor6.class, SConstructor7.class,
            SConstructor8.class, SConstructor9.class, SConstructor10.class
    };

    @SuppressWarnings("unchecked")
    static <T> T create(Constructor<?> constructor) throws Exception {
        Class<?> owner = constructor.getDeclaringClass();
        String constructorDescriptor = Type.getConstructorDescriptor(constructor);
        Class<?>[] parameterTypes = constructor.getParameterTypes();

        Class<?> targetAbstractClass = ABSTRACT_CLASSES[parameterTypes.length];
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "ConstructorInvoker" + parameterTypes.length + "_" + ID.getAndIncrement();

        byte[] bytes = generateByteCode(
                internalClassName,
                owner,
                constructorDescriptor,
                parameterTypes,
                targetAbstractClass
        );

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (T) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(
            String className,
            Class<?> owner,
            String desc,
            Class<?>[] params,
            Class<?> abstractClass) {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String superName = Type.getInternalName(abstractClass);

        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, superName, null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        StringBuilder interfaceDesc = new StringBuilder("(");
        for (int i = 0; i < params.length; i++) {
            interfaceDesc.append("Ljava/lang/Object;");
        }
        interfaceDesc.append(")Ljava/lang/Object;");

        mv = cw.visitMethod(ACC_PUBLIC, "newInstance", interfaceDesc.toString(), null, null);
        mv.visitCode();

        String ownerInternalName = Type.getInternalName(owner);
        mv.visitTypeInsn(NEW, ownerInternalName);
        mv.visitInsn(DUP);

        for (int i = 0; i < params.length; i++) {
            mv.visitVarInsn(ALOAD, i + 1);
            AsmUtils.unboxAndCast(mv, Type.getDescriptor(params[i]));
        }

        mv.visitMethodInsn(INVOKESPECIAL, ownerInternalName, "<init>", desc, false);

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}