package net.momirealms.sparrow.reflection.field;

import net.momirealms.sparrow.reflection.SReflection;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

@SuppressWarnings("DuplicatedCode")
final class BooleanFieldAccessorFactory implements Opcodes {
    private BooleanFieldAccessorFactory() {}

    static SBooleanField create(Field field) throws Exception {
        if (field.getType() != boolean.class) {
            throw new IllegalArgumentException("Field must be of type boolean");
        }

        Class<?> owner = field.getDeclaringClass();
        String fieldName = field.getName();
        boolean isStatic = Modifier.isStatic(field.getModifiers());
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "BooleanAccessor_" + fieldName;

        byte[] bytes = generateByteCode(internalClassName, owner, fieldName, isStatic);

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);

        return (SBooleanField) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(String className, Class<?> owner, String fieldName, boolean isStatic) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        String ownerInternalName = Type.getInternalName(owner);
        String interfaceInternalName = Type.getInternalName(SBooleanField.class);

        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{interfaceInternalName});

        // 默认构造函数
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 实现 boolean get(Object instance)
        mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;)Z", null, null);
        mv.visitCode();
        if (isStatic) {
            mv.visitFieldInsn(GETSTATIC, ownerInternalName, fieldName, "Z");
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerInternalName);
            mv.visitFieldInsn(GETFIELD, ownerInternalName, fieldName, "Z");
        }
        mv.visitInsn(IRETURN); // boolean 在操作数栈上按 int 处理
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 实现 void set(Object instance, boolean value)
        mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;Z)V", null, null);
        mv.visitCode();
        if (isStatic) {
            mv.visitVarInsn(ILOAD, 2); // 0/1 值的 int 加载
            mv.visitFieldInsn(PUTSTATIC, ownerInternalName, fieldName, "Z");
        } else {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, ownerInternalName);
            mv.visitVarInsn(ILOAD, 2);
            mv.visitFieldInsn(PUTFIELD, ownerInternalName, fieldName, "Z");
        }
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}