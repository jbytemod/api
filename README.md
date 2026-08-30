# JByteMod Plugin API

The public plugin API for [JByteMod Remastered](https://github.com/apkreader/JByteMod-Remastered). Plugins can inspect and modify the active ASM class tree, react to UI selections and archive loading, use JByteMod's decompilers, open and save files, and control an attached JVM.

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

## Plugin context

Call `getContext()` after `init()` begins to access JByteMod. The context provides:

- Application and archive information through `getApplicationVersion()` and `getArchiveInfo()`.
- The active `Map<String, ClassNode>` and helpers for reading or writing class-file bytes.
- Class replacement, method-modification notification, and tree refreshing.
- All installed decompiler IDs and class or method decompilation.
- Current class and method selection plus programmatic UI selection.
- Opening and saving JAR, class, and APK files.
- Listing JVM processes, attaching, refreshing classes, applying changes, freezing, resuming, and terminating the attached JVM.
- JByteMod logging and access to the main menu bar and class tree for Swing integrations.

Changes made directly to a `MethodNode` should be followed by `getContext().methodModified(classNode, methodNode)`. Use `replaceClass(previous, replacement)` for whole-class replacement. JVM HotSwap still prohibits most structural changes, including adding or removing fields, methods, superclasses, or interfaces.

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
