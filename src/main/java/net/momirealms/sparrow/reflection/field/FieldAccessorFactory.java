package net.momirealms.sparrow.reflection.field;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.util.AsmUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("DuplicatedCode")
final class FieldAccessorFactory implements Opcodes {
    private FieldAccessorFactory() {}
    private static final AtomicInteger ID = new AtomicInteger(0);
    private static final String ABSTRACT_CLASS_INTERNAL_NAME = Type.getInternalName(SField.class);

    static SField create(java.lang.reflect.Field field) throws Exception {
        Class<?> owner = field.getDeclaringClass();
        String fieldName = field.getName();
        String fieldDescriptor = Type.getDescriptor(field.getType());
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "Accessor_" + fieldName + "_" + ID.getAndIncrement();
        byte[] bytes = generateByteCode(internalClassName, owner, fieldName, fieldDescriptor, isStatic);
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (SField) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(String className, Class<?> owner, String fieldName, String fieldDescriptor, boolean isStatic) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String ownerInternalName = Type.getInternalName(owner);

        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, ABSTRACT_CLASS_INTERNAL_NAME, null);

        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, ABSTRACT_CLASS_INTERNAL_NAME, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        if (isStatic) {
            mv.visitFieldInsn(GETSTATIC, ownerInternalName, fieldName, fieldDescriptor);
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerInternalName);
            mv.visitFieldInsn(GETFIELD, ownerInternalName, fieldName, fieldDescriptor);
        }
        AsmUtils.box(mv, fieldDescriptor);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        if (isStatic) {
            mv.visitVarInsn(ALOAD, 2);
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerInternalName);
            mv.visitVarInsn(ALOAD, 2);
        }
        AsmUtils.unboxAndCast(mv, fieldDescriptor);
        mv.visitFieldInsn(isStatic ? PUTSTATIC : PUTFIELD, ownerInternalName, fieldName, fieldDescriptor);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}