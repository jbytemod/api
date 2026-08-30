package de.xbrowniecodez.jbytemod.plugin;

public record JvmRuntimeInfo(
        String pid,
        String vmName,
        String vmVendor,
        String vmVersion,
        String runtimeName,
        String runtimeVersion,
        long startTimeMillis,
        long uptimeMillis,
        int availableProcessors,
        long heapUsedBytes,
        long heapCommittedBytes,
        long heapMaxBytes,
        long nonHeapUsedBytes,
        long nonHeapCommittedBytes,
        long nonHeapMaxBytes,
        int loadedClassCount,
        long totalLoadedClassCount,
        long unloadedClassCount,
        int threadCount,
        int peakThreadCount,
        int daemonThreadCount,
        long totalStartedThreadCount,
        boolean frozen) {
}
