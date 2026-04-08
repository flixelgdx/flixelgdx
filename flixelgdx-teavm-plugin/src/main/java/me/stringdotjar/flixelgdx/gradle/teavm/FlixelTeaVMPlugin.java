/*********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 *********************************************************************************/

package me.stringdotjar.flixelgdx.gradle.teavm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Copy;
import org.jspecify.annotations.NonNull;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Gradle plugin that automates the web assembly steps required to run a FlixelGDX game via
 * TeaVM in a browser.
 *
 * <p>Apply alongside {@code org.teavm} in the web module's {@code build.gradle}. The plugin
 * registers five helper tasks and wires them as dependencies of the TeaVM build tasks so that
 * every {@code generateJavaScript} or {@code javaScriptDevServer} run produces a complete,
 * browser-ready webapp directory.
 *
 * <h2>What the plugin provides</h2>
 *
 * <ul>
 *   <li>{@code copyAssets} - copies the game's asset directory into
 *       {@code <outputDir>/assets/}.</li>
 *   <li>{@code copyWebApp} - copies everything in {@code src/main/webapp/} into
 *       {@code <outputDir>/} (skipped if the directory does not exist), except
 *       {@code startup-logo.png}, which is placed under {@code assets/} by {@code copyDefaultStartupLogo}.</li>
 *   <li>{@code generateIndexHtml} - writes a default {@code index.html} into
 *       {@code <outputDir>/} when no {@code index.html} is present in the webapp directory.
 *       The generated page uses the canvas ID configured via {@link FlixelTeaVMExtension}.</li>
 *   <li>{@code extractNativeScripts} - extracts native JavaScript files (e.g. {@code gdx.wasm.js},
 *       {@code howler.js}) from gdx-teavm dependency JARs into {@code <outputDir>/scripts/}.
 *       These are required at runtime by the gdx-teavm backend.</li>
 *   <li>{@code generatePreloadFile} - scans the assets directory and writes a {@code preload.txt}
 *       manifest into {@code <outputDir>/assets/}. This file is required by gdx-teavm's asset
 *       preloader to discover and download game assets at startup.</li>
 *   <li>{@code copyDefaultStartupLogo} - writes {@code startup-logo.png} into
 *       {@code <outputDir>/assets/} for gdx-teavm's preload screen.</li>
 * </ul>
 *
 * <p>All six helper tasks above are added as dependencies of {@code generateJavaScript},
 * {@code javaScriptDevServer}, and {@code run} (when those tasks exist on the project).
 *
 * <h2>Minimal usage</h2>
 *
 * <p>In the web module's {@code build.gradle}:
 *
 * <pre>{@code
 * plugins {
 *   id 'org.teavm' version '0.13.0'
 *   id 'flixelgdx.teavm' version '0.1.0-beta'
 * }
 *
 * teavm {
 *   all {
 *     mainClass = 'com.mygame.teavm.MyTeaVMLauncher'
 *   }
 *   js {
 *     addedToWebApp = true
 *     targetFileName = 'teavm.js'
 *     outputDir = file("$buildDir/dist/webapp")
 *   }
 * }
 *
 * dependencies {
 *   implementation project(':core')
 * }
 * }</pre>
 *
 * <p>In the root {@code settings.gradle}, add the FlixelGDX plugin repository to
 * {@code pluginManagement} so Gradle can resolve the plugin:
 *
 * <pre>{@code
 * pluginManagement {
 *   repositories {
 *     mavenLocal()
 *     maven { url 'https://jitpack.io' }
 *     gradlePluginPortal()
 *   }
 * }
 * }</pre>
 *
 * <h2>Optional customization</h2>
 *
 * <pre>{@code
 * flixelgdx {
 *   title = 'My Game Title'                                        // default: My FlixelGDX Game
 *   canvasId = 'my-canvas'                                         // default: 'flixelgdx-canvas'
 *   outputDir = file("$buildDir/dist/webapp")                      // default: same value
 *   devServerPort = 1234                                           // default: 8080
 *   assetsDir = file('../assets')                                  // default: rootProject/assets/
 *   webappDir = file('src/main/webapp')                            // default: same value
 *   generateDefaultIndexHtml = true                                // default: true
 *   generateDefaultStartupLogo = true                              // default: true
 *   customIndexHtml = file('src/main/webapp/index.html')           // default: flixelgdx's resource index.html
 *   customStartupLogo = file('src/main/webapp/startup-logo.png')   // default: flixelgdx's resource startup-logo.png
 *   customFavicon = file('src/main/webapp/favicon.ico')            // default: none
 * }
 * }</pre>
 *
 * @see FlixelTeaVMExtension
 */
public class FlixelTeaVMPlugin implements Plugin<Project> {

  private static final String TASK_GROUP = "flixelgdx";
  private static final String DEFAULT_INDEX_TEMPLATE = "/me/stringdotjar/flixelgdx/gradle/teavm/default-index.html";
  /**
   * Must start with {@code /} so {@link Class#getResourceAsStream(String)} resolves from the classpath
   * root. Without a leading slash, the path is treated as relative to this class's package and becomes
   * {@code .../teavm/me/stringdotjar/.../default-startup-logo.png}, which does not exist in the JAR.
   */
  private static final String DEFAULT_STARTUP_LOGO = "/me/stringdotjar/flixelgdx/gradle/teavm/default-startup-logo.png";

  @Override
  public void apply(Project project) {
    FlixelTeaVMExtension ext = project.getExtensions().create(FlixelTeaVMExtension.NAME, FlixelTeaVMExtension.class);

    ext.getCanvasId().convention(FlixelTeaVMExtension.DEFAULT_CANVAS_ID);
    ext.getTitle().convention(FlixelTeaVMExtension.DEFAULT_TITLE);
    ext.getOutputDir().convention(project.getLayout().getBuildDirectory().dir("dist/webapp"));
    ext.getWebappDir().convention(project.getLayout().getProjectDirectory().dir("src/main/webapp"));
    ext.getAssetsDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("assets"));
    ext.getGenerateDefaultIndexHtml().convention(true);
    ext.getGenerateDefaultStartupLogo().convention(true);
    ext.getDevServerPort().convention(8080);

    // Create a task to automatically copy the assets folder into the build output.
    project.getTasks().register("copyAssets", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Copies game assets from the assets directory into the web output directory.");
      task.from(ext.getAssetsDir());
      task.into(ext.getOutputDir().dir("assets"));
    });

    // Copy any user-made web config files into the build output.
    project.getTasks().register("copyWebApp", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Copies user-provided web resources (e.g. a custom index.html) into the web output directory.");
      task.onlyIf(t -> ext.getWebappDir().get().getAsFile().exists());
      task.from(ext.getWebappDir());
      task.into(ext.getOutputDir());
      task.exclude("startup-logo.png"); // If we copied the default logo here, it would sit next to index.html and the preloader would 404.
    });

    // Create a task that automatically runs if no index.html file is detected.
    project.getTasks().register("generateIndexHtml", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Writes index.html to the output directory. Copies a custom file if provided, otherwise generates a default from the built-in template.");
      task.onlyIf(t -> {
        if (!ext.getGenerateDefaultIndexHtml().get()) {
          return false;
        }
        // Always run when a custom file is explicitly provided.
        if (ext.getCustomIndexHtml().isPresent() && ext.getCustomIndexHtml().getAsFile().get().exists()) {
          return true;
        }
        // Skip if the webapp source directory already contains an index.html (copyWebApp handles it).
        File userIndex = new File(ext.getWebappDir().get().getAsFile(), "index.html");
        return !userIndex.exists();
      });
      task.doLast(t -> {
        File outputDir = ext.getOutputDir().get().getAsFile();
        outputDir.mkdirs();

        // If the developer supplied their own index.html, copy it verbatim.
        if (ext.getCustomIndexHtml().isPresent()) {
          File custom = ext.getCustomIndexHtml().getAsFile().get();
          if (custom.exists()) {
            try {
              Files.copy(custom.toPath(), new File(outputDir, "index.html").toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              throw new RuntimeException("FlixelGDX: failed to copy custom index.html.", e);
            }
            return;
          }
        }

        // Also copy the favicon to the output dir and build the <link> tag for the template.
        String faviconLink = "";
        if (ext.getCustomFavicon().isPresent()) {
          File favicon = ext.getCustomFavicon().getAsFile().get();
          if (favicon.exists()) {
            try {
              Files.copy(favicon.toPath(), new File(outputDir, favicon.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              project.getLogger().warn("[FlixelGDX] Could not copy favicon: {}", e.getMessage());
            }
            faviconLink = "  <link rel=\"icon\" href=\"" + favicon.getName() + "\">";
          }
        }

        // Generate from the default built-in template.
        try {
          String template;
          try (InputStream in = FlixelTeaVMPlugin.class.getResourceAsStream(DEFAULT_INDEX_TEMPLATE)) {
            if (in == null) {
              throw new IOException("default-index.html template not found in plugin JAR at " + DEFAULT_INDEX_TEMPLATE);
            }
            template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
          }
          String html = template
            .replace("{{TITLE}}", ext.getTitle().get())
            .replace("{{CANVAS_ID}}", ext.getCanvasId().get())
            .replace("{{FAVICON}}", faviconLink);
          Files.writeString(new File(outputDir, "index.html").toPath(), html, StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new RuntimeException("FlixelGDX: failed to generate default index.html.", e);
        }
      });
    });

    // Extract native JS files (gdx.wasm.js, howler.js, freetype.js, etc.) from gdx-teavm
    // dependency JARs into <outputDir>/scripts/ where the runtime expects them.
    project.getTasks().register("extractNativeScripts", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Extracts native JavaScript files from gdx-teavm dependency JARs into the scripts directory.");
      task.doLast(t -> {
        File scriptsDir = new File(ext.getOutputDir().get().getAsFile(), "scripts");
        scriptsDir.mkdirs();

        Configuration classpath = project.getConfigurations().getByName("runtimeClasspath");
        for (File file : classpath.resolve()) {
          if (!file.getName().endsWith(".jar")) {
            continue;
          }
          try (JarFile jar = new JarFile(file)) {
            if (jar.getEntry("META-INF/gdx-teavm.properties") == null) {
              continue;
            }
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
              JarEntry entry = entries.nextElement();
              String name = entry.getName();
              if (!entry.isDirectory() && name.endsWith(".js") && !name.contains("/")) {
                try (InputStream in = jar.getInputStream(entry)) {
                  Files.copy(in, new File(scriptsDir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
              }
            }
          } catch (IOException e) {
            project.getLogger().warn("[FlixelGDX] Could not read JAR: {}", file.getName(), e);
          }
        }
      });
    });

    // Copy startup-logo.png into <outputDir>/assets/. gdx-teavm requires this file at assets/startup-logo.png
    // for the preload sequence. Uses a custom file if provided, otherwise falls back to the built-in placeholder.
    project.getTasks().register("copyDefaultStartupLogo", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Copies startup-logo.png into the assets output directory. Uses a custom file if provided, otherwise falls back to the built-in placeholder.");
      task.mustRunAfter(project.getTasks().named("copyAssets"));
      task.onlyIf(t -> {
        if (ext.getCustomStartupLogo().isPresent() && ext.getCustomStartupLogo().getAsFile().get().exists()) {
          return true;
        }
        File webappLogo = new File(ext.getWebappDir().get().getAsFile(), "startup-logo.png");
        if (webappLogo.exists()) {
          return true;
        }
        return ext.getGenerateDefaultStartupLogo().get();
      });
      task.doLast(t -> {
        File assetsDir = new File(ext.getOutputDir().get().getAsFile(), "assets");
        assetsDir.mkdirs();
        File dest = new File(assetsDir, "startup-logo.png");

        // Use the custom logo if one was provided.
        if (ext.getCustomStartupLogo().isPresent()) {
          File custom = ext.getCustomStartupLogo().getAsFile().get();
          if (custom.exists()) {
            try {
              Files.copy(custom.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
              throw new RuntimeException("FlixelGDX: failed to copy custom startup-logo.png.", e);
            }
            return;
          }
        }

        // Fall back to the built-in placeholder (copied as raw bytes to avoid corrupting binary PNG data).
        File webappLogo = new File(ext.getWebappDir().get().getAsFile(), "startup-logo.png");
        if (webappLogo.exists()) {
          try {
            Files.copy(webappLogo.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            throw new RuntimeException("FlixelGDX: failed to copy webapp startup-logo.png.", e);
          }
          return;
        }

        if (!ext.getGenerateDefaultStartupLogo().get()) {
          return;
        }
        try (InputStream in = FlixelTeaVMPlugin.class.getResourceAsStream(DEFAULT_STARTUP_LOGO)) {
          if (in == null) {
            throw new IOException("Built-in startup-logo.png not found in plugin JAR at " + DEFAULT_STARTUP_LOGO);
          }
          Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          throw new RuntimeException("FlixelGDX: failed to copy startup-logo.png.", e);
        }
      });
    });

    // Generate preload.txt in the format gdx-teavm expects (fileType:assetType:path:size:overwrite).
    // This must run AFTER copyAssets and copyDefaultStartupLogo so assets/ includes startup-logo.png.
    project.getTasks().register("generatePreloadFile", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Generates preload.txt asset manifest required by gdx-teavm's runtime asset loader.");
      task.mustRunAfter(project.getTasks().named("copyAssets"));
      task.mustRunAfter(project.getTasks().named("copyDefaultStartupLogo"));
      task.doLast(t -> {
        File assetsOutputDir = new File(ext.getOutputDir().get().getAsFile(), "assets");
        if (!assetsOutputDir.isDirectory()) {
          return;
        }
        try {
          Path root = assetsOutputDir.toPath();
          List<String> lines = new ArrayList<>();
          Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            @NonNull
            public FileVisitResult preVisitDirectory(@NonNull Path dir, @NonNull BasicFileAttributes attrs) {
              if (!dir.equals(root)) {
                String rel = root.relativize(dir).toString().replace('\\', '/');
                lines.add("i:d:" + rel + ":0:0");
              }
              return FileVisitResult.CONTINUE;
            }

            @Override
            @NonNull
            public FileVisitResult visitFile(@NonNull Path file, @NonNull BasicFileAttributes attrs) {
              String rel = root.relativize(file).toString().replace('\\', '/');
              long size = attrs.size();
              lines.add("i:b:" + rel + ":" + size + ":0");
              return FileVisitResult.CONTINUE;
            }
          });
          Files.writeString(
            new File(assetsOutputDir, "preload.txt").toPath(),
            String.join("\n", lines) + "\n",
            StandardCharsets.UTF_8);
        } catch (IOException e) {
          throw new RuntimeException("FlixelGDX: failed to generate preload.txt.", e);
        }
      });
    });

    // Builds the full web app then starts an embedded HTTP dev server that serves the output directory.
    // The server keeps running until the developer presses Ctrl+C.
    project.getTasks().register("run", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Builds the web app and starts a local HTTP dev server. Press Ctrl+C to stop.");
      task.doLast(t -> {
        File webRoot = ext.getOutputDir().get().getAsFile();
        int port = ext.getDevServerPort().get();

        HttpServer server;
        try {
          server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
          throw new RuntimeException("[FlixelGDX] Could not start dev server on port " + port + ": " + e.getMessage(), e);
        }

        // MIME type map for common web assets.
        Map<String, String> mimeTypes = Map.of(
            "html", "text/html; charset=utf-8",
            "js",   "application/javascript",
            "css",  "text/css",
            "png",  "image/png",
            "jpg",  "image/jpeg",
            "jpeg", "image/jpeg",
            "gif",  "image/gif",
            "txt",  "text/plain",
            "wasm", "application/wasm"
        );

        server.createContext("/", (HttpExchange exchange) -> {
          URI requestUri = exchange.getRequestURI();
          String urlPath = requestUri.getPath();
          if (urlPath.equals("/") || urlPath.isEmpty()) {
            urlPath = "/index.html";
          }

          File file = new File(webRoot, urlPath);
          if (!file.exists() || file.isDirectory()) {
            File index = new File(file, "index.html");
            if (file.isDirectory() && index.exists()) {
              file = index;
            } else {
              byte[] body = ("404 Not Found: " + urlPath).getBytes(StandardCharsets.UTF_8);
              exchange.sendResponseHeaders(404, body.length);
              try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
              }
              return;
            }
          }

          String ext2 = "";
          int dot = file.getName().lastIndexOf('.');
          if (dot >= 0) {
            ext2 = file.getName().substring(dot + 1).toLowerCase();
          }
          String mime = mimeTypes.getOrDefault(ext2, "application/octet-stream");

          byte[] bytes = Files.readAllBytes(file.toPath());
          exchange.getResponseHeaders().set("Content-Type", mime);
          exchange.sendResponseHeaders(200, bytes.length);
          try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
          }
        });

        server.setExecutor(null);
        server.start();

        String url = "http://localhost:" + port;
        Logger logger = project.getLogger();
        logger.quiet("");
        logger.quiet("[FlixelGDX] Dev server running at " + url);
        logger.quiet("[FlixelGDX] Serving: " + webRoot.getAbsolutePath());
        logger.quiet("[FlixelGDX] Press Ctrl+C to stop.");
        logger.quiet("");

        // Attempt to open the browser. Desktop.browse() is unreliable in headless JVMs (e.g. the
        // Gradle daemon on Linux), so we try OS-native commands first and fall back to AWT last.
        String os = System.getProperty("os.name", "").toLowerCase();
        try {
          if (os.contains("linux")) {
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
          } else if (os.contains("mac")) {
            Runtime.getRuntime().exec(new String[]{"open", url});
          } else {
            Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.BROWSE)) {
              desktop.browse(new URI(url));
            }
          }
        } catch (Exception ignored) {
          // If all else fails the developer can just navigate to the URL printed above.
          logger.quiet("Please navigate to " + url + " in your browser to test your game, as it has failed to open automatically.");
        }

        // Block indefinitely until the build daemon is killed (Ctrl+C).
        try {
          Thread.currentThread().join();
        } catch (InterruptedException e) {
          server.stop(0);
          Thread.currentThread().interrupt();
        }
      });
    });

    // Connect the new tasks to automatically run when either the Java bytecode is being converted
    // to JavaScript or when a local developer server is created to test the game using the plugin.
    project.afterEvaluate(p -> {
      wireTo(p, "generateJavaScript");
      wireTo(p, "javaScriptDevServer");
      wireTo(p, "run");

      // The run task must also trigger the full TeaVM compilation so that teavm.js exists
      // before the dev server starts serving files.
      Task runTask = p.getTasks().findByName("run");
      Task generateJs = p.getTasks().findByName("generateJavaScript");
      if (runTask != null && generateJs != null) {
        runTask.dependsOn(generateJs);
      }
    });
  }

  private void wireTo(Project project, String taskName) {
    Task task = project.getTasks().findByName(taskName);
    if (task != null) {
      task.dependsOn(
          project.getTasks().named("copyAssets"),
          project.getTasks().named("copyWebApp"),
          project.getTasks().named("generateIndexHtml"),
          project.getTasks().named("extractNativeScripts"),
          project.getTasks().named("generatePreloadFile"),
          project.getTasks().named("copyDefaultStartupLogo"));
    } else {
      project.getLogger().warn("[FlixelGDX] Task '{}' not found. Make sure 'org.teavm' is applied before 'flixelgdx.teavm'.", taskName);
    }
  }
}
