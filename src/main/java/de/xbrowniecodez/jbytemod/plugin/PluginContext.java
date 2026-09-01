package de.xbrowniecodez.jbytemod.plugin;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.JMenuBar;
import javax.swing.JTree;
import java.util.List;
import java.util.Map;

public interface PluginContext {
    String getApplicationVersion();

    ArchiveInfo getArchiveInfo();

    Map<String, ClassNode> getCurrentFile();

    List<String> getResourceNames();

    byte[] getResource(String path);

    void putResource(String path, byte[] bytes);

    boolean removeResource(String path);

    byte[] getClassBytes(ClassNode classNode);

    ClassNode readClass(byte[] bytes);

    List<String> getDecompilerIds();

    void openFile(String path) throws Exception;

    String saveFile(String path) throws Exception;

    default String saveFile(String path, ApkSigningOptions signingOptions) throws Exception {
        if (!signingOptions.usesDebugKey()) {
            throw new UnsupportedOperationException("Custom APK signing is not supported by this JByteMod version");
        }
        return saveFile(path);
    }

    List<JvmProcess> listJvmProcesses();

    void attachToJvm(String pid) throws Exception;

    void refreshAttachedJvm() throws Exception;

    int applyChangesToAttachedJvm() throws Exception;

    void setAttachedJvmFrozen(boolean frozen) throws Exception;

    void detachFromAttachedJvm() throws Exception;

    void terminateAttachedJvm() throws Exception;

    JvmRuntimeInfo getAttachedJvmRuntimeInfo() throws Exception;

    List<JvmThreadInfo> getAttachedJvmThreads(int maxStackDepth) throws Exception;

    List<JvmClassLoaderInfo> getAttachedJvmClassLoaders() throws Exception;

    Map<String, String> getAttachedJvmSystemProperties() throws Exception;

    byte[] invokeAttachedJvmAgentExtension(String extensionId, String entryClassName,
                                           Map<String, byte[]> classFiles, byte[] request) throws Exception;

    String decompile(ClassNode classNode, MethodNode method, String decompilerId);

    void selectClass(ClassNode classNode);

    void selectMethod(ClassNode classNode, MethodNode method);

    void methodModified(ClassNode classNode, MethodNode method);

    void replaceClass(ClassNode previous, ClassNode replacement);

    void log(String message);

    void logError(String message, Throwable error);

    void updateTree();

    JMenuBar getMenu();

    JTree getTree();

    ClassNode getSelectedNode();

    MethodNode getSelectedMethod();
}
