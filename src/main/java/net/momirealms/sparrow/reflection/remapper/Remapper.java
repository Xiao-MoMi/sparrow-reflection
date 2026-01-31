package net.momirealms.sparrow.reflection.remapper;

import net.momirealms.sparrow.reflection.clazz.SparrowClass;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public interface Remapper {

    static Remapper noOp() {
        return NoRemap.INSTANCE;
    }

    static Remapper create(Map<String, ClassData> debof, Map<String, ClassData> obf) {
        return new RemapperImpl(debof, obf);
    }

    static Remapper create(Path mappingsFile, String fromNamespace, String toNamespace) throws IOException {
        try (InputStream is = Files.newInputStream(mappingsFile)) {
            return new RemapperImpl(is, fromNamespace, toNamespace);
        }
    }

    static Remapper create(InputStream mappingsStream, String fromNamespace, String toNamespace) throws IOException {
        return new RemapperImpl(mappingsStream, fromNamespace, toNamespace);
    }

    static Remapper createFromPaperJar() {
        // mojang mappings
        if (SparrowClass.existsNoRemap("net.neoforged.art.internal.RenamerImpl")) {
            return noOp();
        }
        Class<?> minecraftClass = SparrowClass.find(
                "net.minecraft.obfuscate.DontObfuscate",
                "net.minecraft.server.Main"
        );
        if (minecraftClass == null) {
            return noOp();
        }
        try (InputStream is = minecraftClass.getClassLoader().getResourceAsStream("META-INF/mappings/reobf.tiny")) {
            if (is == null) {
                throw new IllegalStateException("Failed to find META-INF/mappings/reobf.tiny");
            }
            InputStream bis = is instanceof BufferedInputStream ? is : new BufferedInputStream(is);
            if (Util.firstLine(bis).contains(MappingNamespaces.MOJANG_PLUS_YARN)) {
                return create(bis, MappingNamespaces.MOJANG_PLUS_YARN, MappingNamespaces.SPIGOT);
            }
            return create(bis, MappingNamespaces.MOJANG, MappingNamespaces.SPIGOT);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read META-INF/mappings/reobf.tiny", e);
        }
    }

    String remapClassName(String className);

    String remapFieldName(Class<?> clazz, String fieldName);

    String remapMethodName(Class<?> clazz, String methodName, Class<?>... parameterTypes);

    default String remapClassOrArrayName(final String clazzOrArrayName) {
        if (clazzOrArrayName.isEmpty()) {
            return clazzOrArrayName;
        }
        if (clazzOrArrayName.charAt(0) != '[') {
            return this.remapClassName(clazzOrArrayName);
        }
        final int lastBracketIndex = clazzOrArrayName.lastIndexOf('[');
        if (lastBracketIndex + 1 >= clazzOrArrayName.length()) {
            return clazzOrArrayName;
        }
        if (clazzOrArrayName.charAt(lastBracketIndex + 1) == 'L') {
            if (clazzOrArrayName.charAt(clazzOrArrayName.length() - 1) != ';') {
                return clazzOrArrayName;
            }
            final String className = clazzOrArrayName.substring(lastBracketIndex + 2, clazzOrArrayName.length() - 1);
            final String remappedClassName = this.remapClassName(className);

            return clazzOrArrayName.substring(0, lastBracketIndex + 2) + remappedClassName + ';';
        }
        // 处理基本类型数组 (int[], boolean[] 等)
        return clazzOrArrayName;
    }

    default boolean isNoOp() {
        return false;
    }
}
