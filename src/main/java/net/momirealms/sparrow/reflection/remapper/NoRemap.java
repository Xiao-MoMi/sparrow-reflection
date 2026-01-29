package net.momirealms.sparrow.reflection.remapper;

public class NoRemap implements Remapper {
    public static final NoRemap INSTANCE = new NoRemap();

    @Override
    public String remapClassName(String className) {
        return className;
    }

    @Override
    public String remapFieldName(Class<?> clazz, String fieldName) {
        return fieldName;
    }

    @Override
    public String remapMethodName(Class<?> clazz, String methodName, Class<?>... parameterTypes) {
        return methodName;
    }
}
