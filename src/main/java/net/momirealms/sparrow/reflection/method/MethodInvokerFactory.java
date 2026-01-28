package net.momirealms.sparrow.reflection.method;

import net.momirealms.sparrow.reflection.SReflection;
import net.momirealms.sparrow.reflection.util.AsmUtils;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("DuplicatedCode")
final class MethodInvokerFactory implements Opcodes {
    private static final AtomicInteger ID = new AtomicInteger(0);

    static SMethod create(Method method) throws Exception {
        Class<?> owner = method.getDeclaringClass();
        String methodName = method.getName();
        String methodDescriptor = Type.getMethodDescriptor(method);
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "Invoker_" + methodName + "_" + ID.getAndIncrement(); // 确保后缀唯一，避免类里相同方法重名
        byte[] bytes = generateByteCode(internalClassName, owner, methodName, methodDescriptor, isStatic, parameterTypes, returnType);
        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (SMethod) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(String className, Class<?> owner, String methodName, String methodDescriptor, boolean isStatic, Class<?>[] params, Class<?> returnType) {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{Type.getInternalName(SMethod.class)});

        // 构造函数
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 实现接口 invoke(Object instance, Object[] args)
        mv = cw.visitMethod(ACC_PUBLIC, "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        // 加载调用对象
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        // 加载并处理参数数组
        for (int i = 0; i < params.length; i++) {
            mv.visitVarInsn(ALOAD, 2); // 加载 Object[] args
            AsmUtils.pushInt(mv, i);                    // 加载索引 i
            mv.visitInsn(AALOAD);              // 从数组取出 Object 参数
            // 转换为目标类型 (拆箱或强转)
            AsmUtils.unboxAndCast(mv, Type.getDescriptor(params[i]));
        }

        // 执行调用
        int opcode = isStatic ? INVOKESTATIC : (owner.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
        mv.visitMethodInsn(opcode, Type.getInternalName(owner), methodName, methodDescriptor, owner.isInterface());

        // 处理返回值 (装箱或返回 null)
        if (returnType == void.class) {
            mv.visitInsn(ACONST_NULL);
        } else {
            AsmUtils.box(mv, Type.getDescriptor(returnType));
        }

        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }
}