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
final class OptimizedMethodInvokerFactory implements Opcodes {
    private static final AtomicInteger ID = new AtomicInteger(0);
    private static final Class<?>[] INTERFACES = new Class<?>[]{
            SMethod0.class, SMethod1.class, SMethod2.class, SMethod3.class,
            SMethod4.class, SMethod5.class, SMethod6.class, SMethod7.class,
            SMethod8.class, SMethod9.class, SMethod10.class
    };

    @SuppressWarnings("unchecked")
    static <T> T create(Method method) throws Exception {
        Class<?> owner = method.getDeclaringClass();
        String methodName = method.getName();
        String methodDescriptor = Type.getMethodDescriptor(method);
        boolean isStatic = Modifier.isStatic(method.getModifiers());
        Class<?>[] parameterTypes = method.getParameterTypes();
        Class<?> returnType = method.getReturnType();

        Class<?> targetInterface = INTERFACES[parameterTypes.length];
        String suffix = targetInterface.getSimpleName().replace("SMethod", "");
        String internalClassName = Type.getInternalName(owner) + "$" + SReflection.PREFIX + "Invoker" + suffix + "_" + methodName + "_" + ID.getAndIncrement();

        byte[] bytes = generateByteCode(
                internalClassName,
                owner,
                methodName,
                methodDescriptor,
                isStatic,
                parameterTypes,
                returnType,
                targetInterface
        );

        MethodHandles.Lookup lookup = MethodHandles.privateLookupIn(owner, SReflection.LOOKUP);
        MethodHandles.Lookup hiddenLookup = lookup.defineHiddenClass(bytes, true, MethodHandles.Lookup.ClassOption.NESTMATE);
        return (T) hiddenLookup.lookupClass().getDeclaredConstructor().newInstance();
    }

    private static byte[] generateByteCode(
            String className,
            Class<?> owner,
            String methodName,
            String methodDescriptor,
            boolean isStatic,
            Class<?>[] params,
            Class<?> returnType,
            Class<?> interfaceClass) {

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
        cw.visit(V17, ACC_PUBLIC | ACC_FINAL, className, null, "java/lang/Object", new String[]{Type.getInternalName(interfaceClass)});

        // 构造函数
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        // 动态生成接口方法实现
        // 接口方法签名始终是 (Object instance, Object arg0, ...) -> Object
        String desc = "(Ljava/lang/Object;" + "Ljava/lang/Object;".repeat(params.length) +
                ")Ljava/lang/Object;";

        mv = cw.visitMethod(ACC_PUBLIC, "invoke", desc, null, null);
        mv.visitCode();

        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(owner));
        }

        for (int i = 0; i < params.length; i++) {
            mv.visitVarInsn(ALOAD, i + 2);
            AsmUtils.unboxAndCast(mv, Type.getDescriptor(params[i]));
        }

        int opcode = isStatic ? INVOKESTATIC : (owner.isInterface() ? INVOKEINTERFACE : INVOKEVIRTUAL);
        mv.visitMethodInsn(opcode, Type.getInternalName(owner), methodName, methodDescriptor, owner.isInterface());

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