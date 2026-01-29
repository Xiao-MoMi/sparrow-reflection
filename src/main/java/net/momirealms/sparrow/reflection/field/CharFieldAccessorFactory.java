package net.momirealms.sparrow.reflection.field;

import net.momirealms.sparrow.reflection.SReflection;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("DuplicatedCode")
final class CharFieldAccessorFactory implements Opcodes {
    private CharFieldAccessorFactory() {}
    private static final AtomicInteger ID = new AtomicInteger(0);
    private static final String ABSTRACT_CLASS_INTERNAL_NAME = Type.getInternalName(SCharField.class);

    static SCharField create(Field field) throws Exception {
        if (field.getType() != char.class) {
            throw new IllegalArgumentException("Field must be of type char");
        }
        Class<?> owner = field.getDeclaringClass();
        String fieldName = field.getName();
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "CharAccessor_" + fieldName + "_" + ID.getAndIncrement();
        byte[] bytes = generateByteCode(internalClassName, owner, fieldName, isStatic);
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (SCharField) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(String className, Class<?> owner, String fieldName, boolean isStatic) {
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

        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)C", null, null);
        mv.visitCode();
        if (isStatic) {
            mv.visitFieldInsn(GETSTATIC, ownerInternalName, fieldName, "C");
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerInternalName);
            mv.visitFieldInsn(GETFIELD, ownerInternalName, fieldName, "C");
        }
        mv.visitInsn(IRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;C)V", null, null);
        mv.visitCode();
        if (isStatic) {
            mv.visitVarInsn(ILOAD, 2);
            mv.visitFieldInsn(PUTSTATIC, ownerInternalName, fieldName, "C");
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerInternalName);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitFieldInsn(PUTFIELD, ownerInternalName, fieldName, "C");
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}