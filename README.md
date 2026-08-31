# JByteMod Plugin API

The public plugin API for [JByteMod Remastered](https://github.com/jbytemod/JByteMod-Remastered). Plugins can inspect and modify the active ASM class tree, react to UI selections and archive loading, use JByteMod's decompilers, open and save files, and control an attached JVM.

JByteMod and plugins currently require JDK 21 or newer.

## Example plugins

- [Discord RPC](https://github.com/jbytemod/plugin-discord-rpc) is a compact example of lifecycle hooks, archive information, selection events, settings, Swing UI, and a shaded third-party dependency.
- [MCP Server](https://github.com/jbytemod/plugin-mcp) is a larger example covering archive inspection, direct bytecode editing, decompilation, file operations, JVM attachment, process controls, background services, and a modeless activity dashboard.

## Creating a plugin

Install the API in your local Maven repository:

```sh
git clone https://github.com/jbytemod/api.git
cd api
mvn install
```

Create a Java 21 Maven project and declare the API as a `provided` dependency. JByteMod supplies the API and ASM at runtime, so they must not be bundled into the plugin JAR.

```xml
<properties>
    <maven.compiler.release>21</maven.compiler.release>
    <jbytemod.version>2.11.0</jbytemod.version>
</properties>

<repositories>
    <repository>
        <id>ow2</id>
        <url>https://repository.ow2.org/nexus/content/repositories/snapshots/</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>brownie</groupId>
        <artifactId>jbytemod-api</artifactId>
        <version>${jbytemod.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>
```

Add one public, concrete `Plugin` subclass with a no-argument constructor:

```java
package example;

import de.xbrowniecodez.jbytemod.plugin.Plugin;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import java.util.Map;

public final class ExamplePlugin extends Plugin {
    public ExamplePlugin() {
        super("Example Plugin", "1.0.0", "your-name");
    }

    @Override
    public void init() {
        getContext().log("Example Plugin loaded");
    }

    @Override
    public void loadFile(Map<String, ClassNode> classes) {
        getContext().log("Loaded " + classes.size() + " classes");
    }

    @Override
    public void classSelected(ClassNode classNode) {
        getContext().log("Selected " + classNode.name);
    }

    @Override
    public void methodSelected(ClassNode classNode, MethodNode methodNode) {
        getContext().log("Selected " + classNode.name + "." + methodNode.name);
    }

    @Override
    public boolean isClickable() {
        return true;
    }

    @Override
    public void menuClick() {
        JOptionPane.showMessageDialog(
                SwingUtilities.getWindowAncestor(getMenu()),
                "Hello from JByteMod");
    }

    @Override
    public void shutdown() {
        getContext().log("Example Plugin stopped");
    }
}
```

JByteMod discovers concrete `Plugin` subclasses directly from each JAR. No service file or manifest entry is required. The constructor supplies the name, version, and author displayed in the Plugins menu and plugin manager.

## Lifecycle and events

`Plugin` provides these optional hooks:

| Hook | Called when |
| --- | --- |
| `init()` | The plugin has been attached to JByteMod and may use its context. |
| `loadFile(...)` | An archive, class, APK, JVM snapshot, or attached JVM has finished loading. |
| `loadProgress(...)` | Loading progress changes. |
| `classSelected(...)` | A class is selected in the tree. |
| `methodSelected(...)` | A method is selected in the tree. |
| `menuClick()` | The plugin's entry in the Plugins menu is clicked. |
| `shutdown()` | The plugin is disabled, reloaded, or JByteMod is closing it. |

Return `true` from `isClickable()` to enable the plugin's menu entry.

## Plugin class reference

Call `getContext()` only after `init()` begins. JByteMod attaches the context before invoking that hook.

| Method | Purpose |
| --- | --- |
| `getContext()` | Return the complete `PluginContext`. |
| `getCurrentFile()` | Convenience wrapper for the active class map. |
| `updateTree()` | Rebuild the class tree after changes. |
| `getMenu()` | Return JByteMod's main `JMenuBar`. |
| `getTree()` | Return the archive `JTree`. |
| `getSelectedNode()` | Return the currently selected class, or `null`. |
| `getSelectedMethod()` | Return the currently selected method, or `null`. |
| `getName()` / `setName(...)` | Read or change the displayed plugin name. |
| `getVersion()` / `setVersion(...)` | Read or change the displayed plugin version. |
| `getAuthor()` / `setAuthor(...)` | Read or change the displayed author. |

The context and convenience methods are protected, while metadata accessors are public.

## Complete context reference

### Application and archive state

| Method | Purpose |
| --- | --- |
| `getApplicationVersion()` | Return the running JByteMod version. |
| `getArchiveInfo()` | Return the current archive type, resource count, and source name or path. |
| `getCurrentFile()` | Return the live `Map<String, ClassNode>` used by the editor. The keys are JVM internal class names such as `java/lang/String`. |
| `getResourceNames()` | Return the sorted paths of all non-class entries in the current archive. |
| `getResource(String)` | Return a defensive copy of a resource, or `null` when it does not exist. |
| `putResource(String, byte[])` | Add or replace a non-class archive entry. |
| `removeResource(String)` | Remove an archive entry and report whether it existed. |
| `getClassBytes(ClassNode)` | Serialize an ASM class tree to class-file bytes. |
| `readClass(byte[])` | Parse class-file bytes into a `ClassNode`. |

`getCurrentFile()` returns an empty map when nothing is loaded. Its `ClassNode` values are the editor's live objects, so changes affect the in-memory archive.

Resource paths use forward slashes and are normalized by JByteMod. Empty paths, absolute paths, traversal segments, and `.class` paths are rejected. Resource methods are intended for JAR and APK archives; attached-JVM and single-class views do not contain editable resources. Byte arrays returned from and passed to the API are copied.

### Bytecode changes

| Method | Purpose |
| --- | --- |
| `methodModified(ClassNode, MethodNode)` | Clear cached decompiler output and refresh the selected method or tree after changing a method. |
| `replaceClass(ClassNode, ClassNode)` | Replace an entire class, including handling a changed class name and restoring the closest matching selection. |
| `updateTree()` | Rebuild the visible archive tree after broader map changes. |

After modifying instructions, try/catch blocks, local variables, annotations, or other method data directly, call `methodModified(...)`. Use `replaceClass(...)` when importing or generating a complete replacement class.

```java
ClassNode owner = getSelectedNode();
MethodNode method = getSelectedMethod();
method.instructions.insert(new org.objectweb.asm.tree.InsnNode(org.objectweb.asm.Opcodes.NOP));
getContext().methodModified(owner, method);
```

Class-file round trips are also supported:

```java
byte[] bytes = getContext().getClassBytes(original);
ClassNode replacement = getContext().readClass(bytes);
getContext().replaceClass(original, replacement);
```

### Decompilation

| Method | Purpose |
| --- | --- |
| `getDecompilerIds()` | Return every available decompiler ID. |
| `decompile(ClassNode, MethodNode, String)` | Decompile a class or a specific method with the selected decompiler. Pass `null` as the method for the whole class. |

The built-in IDs are `cfr`, `procyon`, `vineflower`, `jd-core`, `koffee`, and `asmifier`. Method-only output depends on the selected decompiler.

### File operations

| Method | Purpose |
| --- | --- |
| `openFile(String)` | Open a local `.jar`, `.class`, or `.apk` in the current JByteMod window and wait for loading to finish. |
| `saveFile(String)` | Save the active archive or JVM class snapshot and return the final output path. |

`saveFile(...)` creates missing parent directories and appends `.class` when a single loaded class is saved without that extension. Existing output files may be replaced.

### JVM attachment and control

| Method | Purpose |
| --- | --- |
| `listJvmProcesses()` | List attachable local JVMs except the current JByteMod process. |
| `attachToJvm(String)` | Attach by PID, inject the agent, load runtime classes, and switch the current window to the remote archive. |
| `refreshAttachedJvm()` | Reload classes from the target and discard unapplied in-memory edits. |
| `applyChangesToAttachedJvm()` | Redefine changed classes and return the number successfully sent to the target JVM. |
| `setAttachedJvmFrozen(boolean)` | Suspend or resume the entire attached JVM process. |
| `terminateAttachedJvm()` | Resume when necessary, terminate the target JVM, and keep its classes available as a local snapshot. |
| `getAttachedJvmRuntimeInfo()` | Return VM identity, uptime, processor, memory, class-loading, and thread counters. |
| `getAttachedJvmThreads(int)` | Return live threads and up to the requested number of stack frames per thread. Use `0` for no stacks. |
| `getAttachedJvmClassLoaders()` | Return the live class-loader hierarchy and loaded-class counts. |
| `getAttachedJvmSystemProperties()` | Return a snapshot of the target JVM's system properties. |
| `invokeAttachedJvmAgentExtension(String, String, Map<String, byte[]>, byte[])` | Load or reuse a plugin-supplied agent extension in the target JVM and exchange opaque request/response bytes. |

The attachment operations throw `IllegalStateException` when no remote JVM is attached. Freezing pauses the target's UI, application threads, and agent connection, so resume it before refreshing or applying changes.

Runtime inspection is served by JByteMod's injected agent and therefore inspects the same JVM and connection as the editor. The returned runtime, thread, loader, and property objects are snapshots and can be safely retained by a plugin.

Agent extensions keep feature-specific target code outside JByteMod core. The class-file map uses binary class names and must contain the entry class. That class must expose `public static byte[] invoke(byte[] request, Instrumentation instrumentation)`. Extension classes are cached by ID and content hash, must be self-contained apart from JDK classes, and run with the target JVM's permissions. Only invoke extensions supplied by trusted plugins.

JVM HotSwap prohibits most structural changes, including adding or removing fields, methods, superclasses, or interfaces. Method-body and constant changes are normally supported.

### Selection and Swing integration

| Method | Purpose |
| --- | --- |
| `getSelectedNode()` | Return the selected `ClassNode`, or `null`. |
| `getSelectedMethod()` | Return the selected `MethodNode`, or `null`. |
| `selectClass(ClassNode)` | Select and reveal a class in the JByteMod UI. |
| `selectMethod(ClassNode, MethodNode)` | Select and reveal a method in the JByteMod UI. |
| `getMenu()` | Return the main `JMenuBar` for window ownership or additional integration. |
| `getTree()` | Return the archive `JTree`. |

File, attachment, and selection methods marshal their JByteMod UI work onto Swing's event dispatch thread. Plugin callbacks should not assume they always run on that thread; use `SwingUtilities.invokeLater(...)` when directly changing Swing components.

### Logging

| Method | Purpose |
| --- | --- |
| `log(String)` | Write an informational message to the JByteMod log. |
| `logError(String, Throwable)` | Write an error message and optionally print its exception. |

## API data types

### `ArchiveInfo`

`ArchiveInfo` is a record containing:

| Component | Meaning |
| --- | --- |
| `type()` | One of the `ArchiveType` values below. |
| `resourceCount()` | Number of non-class output resources in the current archive. |
| `source()` | Current file name, path, JVM label, or `null` when unavailable. |

### `ArchiveType`

| Value | Meaning |
| --- | --- |
| `NONE` | Nothing is loaded. |
| `ARCHIVE` | A JAR or APK archive is loaded. |
| `CLASS` | A single class file is loaded. |
| `CURRENT_JVM` | Classes from JByteMod's own JVM are loaded. |
| `REMOTE_JVM` | JByteMod is attached to another JVM. |

### `JvmProcess`

`JvmProcess` contains the process `pid()` and its JVM-provided `displayName()`. Pass the PID to `attachToJvm(...)`.

## Packaging and installation

Build the plugin with:

```sh
mvn package
```

If the plugin uses third-party libraries, create a shaded JAR while keeping `jbytemod-api` provided. Do not shade JByteMod or ASM classes into the plugin because duplicate runtime classes can break plugin loading.

Copy the resulting JAR into JByteMod's `plugins` directory:

- Windows: `%APPDATA%\JByteMod-Remastered\plugins`
- macOS: `~/Library/Application Support/JByteMod-Remastered/plugins`
- Linux: `~/JByteMod-Remastered/plugins`

The plugin can then be enabled, disabled, or reloaded from **Plugins > Manage Plugins**.
