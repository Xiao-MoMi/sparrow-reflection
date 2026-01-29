package net.momirealms.sparrow.reflection;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;
import net.momirealms.sparrow.reflection.constructor.SConstructor1;
import net.momirealms.sparrow.reflection.constructor.SparrowConstructor;
import net.momirealms.sparrow.reflection.field.SField;
import net.momirealms.sparrow.reflection.field.SparrowField;
import net.momirealms.sparrow.reflection.method.SMethod1;
import net.momirealms.sparrow.reflection.method.SparrowMethod;
import net.momirealms.sparrow.reflection.remapper.Remapper;

import static net.momirealms.sparrow.reflection.constructor.matcher.ConstructorMatchers.*;
import static net.momirealms.sparrow.reflection.field.matcher.FieldMatchers.*;
import static net.momirealms.sparrow.reflection.method.matcher.MethodMatchers.*;

public class Example {

    public static class TestClass {
        private String field;

        private TestClass(String field) {
            this.field = field;
        }

        private String someMethod(String a) {
            return this.field + " " + a;
        }
    }

    public static void main(String[] args) {
        SparrowClass<TestClass> sClass = SparrowClass.of(TestClass.class);
        SparrowConstructor<TestClass> constructor = sClass.getDeclaredSparrowConstructor(cAny(), 0);
        SConstructor1 asmConstructor = constructor.asm$1();
        TestClass testInstance = (TestClass) asmConstructor.newInstance("ByeBye");

        SparrowField field = sClass.getDeclaredSparrowField(fType(String.class));
        SField asmField = field.asm();
        asmField.set(testInstance, "Hello");

        SparrowMethod method = sClass.getDeclaredSparrowMethod(mNamed("someMethod").and(mTakeArgument(0, String.class).and(mReturnType(String.class))));
        SMethod1 asmMethod = method.asm$1();
        System.out.println(asmMethod.invoke(testInstance, "World"));
    }

    public static void init() {
        // 设置 asm 生成类名的前缀
        SReflection.setAsmClassPrefix("MyPlugin");
        // (可选) 设置类映射器，以同时适配多种运行环境
        SReflection.setRemapper(Remapper.createFromPaperJar());
    }
}
