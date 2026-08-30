package de.xbrowniecodez.jbytemod.plugin;

public record JvmClassLoaderInfo(
        String id,
        String name,
        String className,
        String parentId,
        int loadedClassCount,
        boolean bootstrap) {
}
