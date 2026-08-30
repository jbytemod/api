package de.xbrowniecodez.jbytemod.plugin;

import java.util.List;

public record JvmThreadInfo(
        long id,
        String name,
        String state,
        boolean daemon,
        int priority,
        String lockName,
        String lockOwnerName,
        List<String> stackTrace) {

    public JvmThreadInfo {
        stackTrace = List.copyOf(stackTrace);
    }
}
