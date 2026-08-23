/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.gradle.html5;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.logging.Logger;
import org.gradle.api.tasks.Copy;
import org.gradle.api.tasks.bundling.Zip;
import org.teavm.gradle.api.TeaVMExtension;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Gradle plugin that turns a TeaVM build of a FlixelGDX game into a browser-ready web app.
 *
 * <p>Apply it alongside {@code org.teavm} in the web module. TeaVM itself compiles the game to a
 * JavaScript bundle, a WebAssembly bundle, or both; this plugin fills in the rest of the web app
 * around those bundles: it copies the game's assets, copies any custom web resources, and generates
 * an {@code index.html} that boots the game. It also adds a {@code run} task with a small embedded
 * dev server so a game can be launched in a browser with one command.
 *
 * <h2>JavaScript and WebAssembly</h2>
 *
 * <p>The generated page prefers the WebAssembly build for speed and falls back to the JavaScript
 * build when the browser lacks WebAssembly GC support, so a single deployment runs everywhere.
 * Configure whichever targets you want in the {@code teavm} block; the plugin detects which are
 * added to the web app and writes the loader accordingly.
 *
 * <h2>Minimal usage</h2>
 *
 * <pre>{@code
 * plugins {
 *   id 'org.teavm' version '0.13.0'
 *   id 'org.flixelgdx.html5' version '<flixelgdx-version>
 * }
 *
 * teavm {
 *   all { mainClass = 'com.mygame.web.WebLauncher' }
 *   js { addedToWebApp = true; obfuscated = true }
 *   wasmGC { addedToWebApp = true; obfuscated = true }
 * }
 * }</pre>
 *
 * @see FlixelHtml5Extension
 */
public class FlixelHtml5Plugin implements Plugin<Project> {

  private static final String TASK_GROUP = "flixelgdx";
  private static final String RESOURCE_ROOT = "/org/flixelgdx/gradle/html5/";
  private static final String DEFAULT_INDEX_TEMPLATE = RESOURCE_ROOT + "default-index.html";
  private static final String FONT_RESOURCE = RESOURCE_ROOT + "vcr.ttf";
  private static final String LOGO_RESOURCE = RESOURCE_ROOT + "flixel-logo.png";
  private static final String LOADING_DIR = "flixelgdx-loading";
  private static final String DEFAULT_JS_BUNDLE = "classes.js";
  private static final String DEFAULT_WASM_BUNDLE = "classes.wasm";

  // TeaVM writes each target's bundle into its own subdirectory of the shared output directory, so
  // the generated page must reference the bundles through these prefixes rather than at the root.
  private static final String JS_DIR = "js/";
  private static final String WASM_DIR = "wasm-gc/";

  @Override
  public void apply(Project project) {
    FlixelHtml5Extension ext = project.getExtensions().create(FlixelHtml5Extension.NAME, FlixelHtml5Extension.class);

    ext.getCanvasId().convention(FlixelHtml5Extension.DEFAULT_CANVAS_ID);
    ext.getTitle().convention(FlixelHtml5Extension.DEFAULT_TITLE);
    ext.getWebappDir().convention(project.getLayout().getProjectDirectory().dir("src/main/webapp"));
    ext.getAssetsDir().convention(project.getRootProject().getLayout().getProjectDirectory().dir("assets"));
    ext.getGenerateDefaultIndexHtml().convention(true);
    ext.getDevServerPort().convention(8080);

    // Resolved to the TeaVM output directory in afterEvaluate; the fallback is only used when
    // org.teavm is missing entirely.
    DirectoryProperty webRoot = project.getObjects().directoryProperty();
    webRoot.convention(project.getLayout().getBuildDirectory().dir("generated/teavm"));

    // Resolved in afterEvaluate and read by the index generator at execution time.
    AtomicReference<WebBundle> bundle = new AtomicReference<>(WebBundle.DEFAULTS);

    registerCopyTasks(project, ext, webRoot);
    registerShaderTask(project, webRoot);
    registerManifestTask(project, webRoot);
    registerIndexTask(project, ext, webRoot, bundle);
    registerRunTask(project, ext, webRoot);
    registerPackageTask(project, webRoot);

    project.afterEvaluate(p -> wireTeaVm(p, webRoot, bundle));
  }

  /** Registers the asset and web-resource copy tasks. */
  private void registerCopyTasks(Project project, FlixelHtml5Extension ext, DirectoryProperty webRoot) {
    project.getTasks().register("copyAssets", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Copies game assets from the assets directory into the web output directory.");
      task.from(ext.getAssetsDir());
      task.into(webRoot.dir("assets"));
    });

    project.getTasks().register("copyWebApp", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription(
          "Copies user-provided web resources (e.g. a custom index.html) into the web output directory.");
      task.onlyIf(t -> ext.getWebappDir().get().getAsFile().exists());
      task.from(ext.getWebappDir());
      task.into(webRoot);
    });
  }

  /**
   * Registers the web shader copy task. The shader plugin compiles each shader to a raw ESSL variant
   * on the classpath ({@code shaders/<name>/essl/*.glsl}); a browser cannot read classpath resources,
   * so those files are copied into the web assets tree where they preload like any other asset and
   * the web backend can read them from its warm cache when a shader is loaded by name.
   */
  private void registerShaderTask(Project project, DirectoryProperty webRoot) {
    project.getTasks().register("copyShaders", Copy.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Copies compiled web (ESSL) shader variants into the web assets so they preload.");
      Configuration runtimeClasspath = project.getConfigurations().findByName("runtimeClasspath");
      if (runtimeClasspath != null) {
        // The ESSL variants live on the runtime classpath, either as loose resource directories
        // (project dependencies) or inside dependency jars, so both cases are expanded here.
        task.from(project.provider(() -> runtimeClasspath.getFiles().stream()
            .map(file -> file.isDirectory() ? file : project.zipTree(file))
            .collect(Collectors.toList())));
      }
      task.include("shaders/**/essl/**");
      task.into(webRoot.dir("assets"));
    });
  }

  /**
   * Registers the asset manifest generator. It walks the copied assets and writes an
   * {@code assets/assets.txt} listing every file, which the web backend reads at startup to preload
   * everything before the game runs.
   */
  private void registerManifestTask(Project project, DirectoryProperty webRoot) {
    project.getTasks().register("generateAssetManifest", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Writes assets/assets.txt listing every bundled asset for the web preloader.");
      task.dependsOn(project.getTasks().named("copyAssets"), project.getTasks().named("copyShaders"));
      task.doLast(t -> writeAssetManifest(new File(webRoot.get().getAsFile(), "assets")));
    });
  }

  /**
   * Writes {@code assets.txt} into the assets directory, one relative asset path per line. The file
   * is always written (empty when there are no assets) so the backend can rely on its presence.
   *
   * @param assetsDir The output assets directory to scan.
   */
  private void writeAssetManifest(File assetsDir) {
    assetsDir.mkdirs();
    Path root = assetsDir.toPath();
    List<String> paths = new ArrayList<>();
    try (Stream<Path> files = Files.walk(root)) {
      files.filter(Files::isRegularFile).forEach(file -> {
        String rel = root.relativize(file).toString().replace('\\', '/');
        if (!rel.equals("assets.txt")) {
          paths.add(rel);
        }
      });
    } catch (IOException e) {
      throw new RuntimeException("FlixelGDX: failed to scan assets for the manifest.", e);
    }
    Collections.sort(paths);
    try {
      Files.writeString(new File(assetsDir, "assets.txt").toPath(),
          paths.isEmpty() ? "" : String.join("\n", paths) + "\n", StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("FlixelGDX: failed to write assets.txt.", e);
    }
  }

  /** Registers the {@code index.html} generator. */
  private void registerIndexTask(Project project, FlixelHtml5Extension ext, DirectoryProperty webRoot,
      AtomicReference<WebBundle> bundle) {
    project.getTasks().register("generateIndexHtml", task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Writes index.html into the output directory, booting the WebAssembly or JavaScript bundle.");
      task.onlyIf(t -> {
        // A configured custom file must always be deployed, even when auto-generation is off.
        if (ext.getCustomIndexHtml().isPresent() && ext.getCustomIndexHtml().getAsFile().get().exists()) {
          return true;
        }
        if (!ext.getGenerateDefaultIndexHtml().get()) {
          return false;
        }
        // Skip when the webapp source directory already contains an index.html (copyWebApp handles it).
        return !new File(ext.getWebappDir().get().getAsFile(), "index.html").exists();
      });
      task.doLast(t -> writeIndexHtml(project, ext, webRoot.get().getAsFile(), bundle.get()));
    });
  }

  /**
   * Writes {@code index.html}, either copying a developer-supplied file or filling in the built-in
   * template with the resolved bundle names.
   */
  private void writeIndexHtml(Project project, FlixelHtml5Extension ext, File outputDir, WebBundle bundle) {
    outputDir.mkdirs();

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

    String faviconLink = copyFavicon(project, ext, outputDir);
    String modeDefault = resolveModeDefault(ext);
    copyLoadingAssets(outputDir);

    try {
      String template;
      try (InputStream in = FlixelHtml5Plugin.class.getResourceAsStream(DEFAULT_INDEX_TEMPLATE)) {
        if (in == null) {
          throw new IOException("default-index.html template not found in plugin JAR at " + DEFAULT_INDEX_TEMPLATE);
        }
        template = new String(in.readAllBytes(), StandardCharsets.UTF_8);
      }
      String html = template
          .replace("{{TITLE}}", ext.getTitle().get())
          .replace("{{CANVAS_ID}}", ext.getCanvasId().get())
          .replace("{{FAVICON}}", faviconLink)
          .replace("{{MODE_DEFAULT}}", modeDefault)
          .replace("{{WASM_ENABLED}}", Boolean.toString(bundle.wasmEnabled()))
          .replace("{{JS_BUNDLE}}", bundle.jsBundle())
          .replace("{{WASM_BUNDLE}}", bundle.wasmBundle())
          .replace("{{WASM_RUNTIME}}", bundle.wasmRuntime());
      Files.writeString(new File(outputDir, "index.html").toPath(), html, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new RuntimeException("FlixelGDX: failed to generate default index.html.", e);
    }
  }

  /**
   * Copies the loading screen's font and logo into a {@code flixelgdx-loading} directory beside the
   * generated {@code index.html}, so the overlay shown while the game downloads can display the
   * FlixelGDX logo and render its progress text in the bundled VCR font before any game code runs.
   *
   * @param outputDir The web root the {@code index.html} is written into.
   */
  private void copyLoadingAssets(File outputDir) {
    File dir = new File(outputDir, LOADING_DIR);
    dir.mkdirs();
    copyResource(FONT_RESOURCE, new File(dir, "vcr.ttf"));
    copyResource(LOGO_RESOURCE, new File(dir, "flixel-logo.png"));
  }

  /** Copies a plugin classpath resource to a target file, replacing any existing one. */
  private void copyResource(String resource, File target) {
    try (InputStream in = FlixelHtml5Plugin.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("bundled plugin resource not found: " + resource);
      }
      Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new RuntimeException("FlixelGDX: failed to copy loading resource " + resource + ".", e);
    }
  }

  /** Copies a configured favicon into the output and returns the {@code <link>} tag, or an empty string. */
  private String copyFavicon(Project project, FlixelHtml5Extension ext, File outputDir) {
    if (!ext.getCustomFavicon().isPresent()) {
      return "";
    }
    File favicon = ext.getCustomFavicon().getAsFile().get();
    if (!favicon.exists()) {
      return "";
    }
    try {
      Files.copy(favicon.toPath(), new File(outputDir, favicon.getName()).toPath(),
          StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      project.getLogger().warn("[FlixelGDX] Could not copy favicon: {}", e.getMessage());
    }
    return "  <link rel=\"icon\" href=\"" + favicon.getName() + "\">";
  }

  /** Resolves the baked-in runtime mode default as a JavaScript literal ({@code 'debug'} or {@code null}). */
  private static String resolveModeDefault(FlixelHtml5Extension ext) {
    if (!ext.getMode().isPresent()) {
      return "null";
    }
    return switch (ext.getMode().get()) {
      case DEBUG -> "'debug'";
      case TEST -> "'test'";
      case RELEASE -> "'release'";
    };
  }

  /**
   * Registers the {@code package} task, which zips the entire web output into a distributable
   * archive under {@code <rootProject>/dist/}. Running {@code ./gradlew :web:package} after a build
   * produces {@code dist/<projectName>-web.zip}, ready to upload to a web host.
   */
  private void registerPackageTask(Project project, DirectoryProperty webRoot) {
    project.getTasks().register("package", Zip.class, task -> {
      task.setGroup(TASK_GROUP);
      task.setDescription("Packages the web output into a zip archive in the dist/ directory at the project root.");
      task.from(webRoot);
      task.getArchiveBaseName().convention(project.getRootProject().getName());
      task.getArchiveClassifier().convention("html5");
      task.getArchiveVersion().convention(project.provider(() -> {
        Object version = project.getVersion();
        String v = version != null ? version.toString() : "";
        return v.equals("unspecified") ? "" : v;
      }));
      task.getDestinationDirectory().convention(
          project.getRootProject().getLayout().getProjectDirectory().dir("dist"));
    });
  }

  /** Registers the {@code run} and {@code debug} tasks. */
  private void registerRunTask(Project project, FlixelHtml5Extension ext, DirectoryProperty webRoot) {
    project.getTasks().register("run", task -> {
      task.setGroup("application");
      task.setDescription("Builds the web app and starts a local HTTP dev server. Press Ctrl+C to stop.");
      task.doLast(t -> serve(project, webRoot.get().getAsFile(), ext.getDevServerPort().get(), false));
    });
    project.getTasks().register("debug", task -> {
      task.setGroup("application");
      task.setDescription(
          "Builds the web app and starts a local HTTP dev server in debug mode (?flixel.mode=debug). "
              + "Press Ctrl+C to stop.");
      task.doLast(t -> serve(project, webRoot.get().getAsFile(), ext.getDevServerPort().get(), true));
    });
  }

  /** Starts the embedded HTTP dev server and blocks until the build is interrupted. */
  private void serve(Project project, File webRoot, int port, boolean debug) {
    HttpServer server;
    try {
      server = HttpServer.create(new InetSocketAddress(port), 0);
    } catch (IOException e) {
      throw new RuntimeException("[FlixelGDX] Could not start dev server on port " + port + ": " + e.getMessage(), e);
    }

    Map<String, String> mimeTypes = Map.of(
        "html", "text/html; charset=utf-8",
        "js", "application/javascript",
        "css", "text/css",
        "png", "image/png",
        "jpg", "image/jpeg",
        "jpeg", "image/jpeg",
        "gif", "image/gif",
        "txt", "text/plain",
        "ttf", "font/ttf",
        "wasm", "application/wasm");

    server.createContext("/", (HttpExchange exchange) -> {
      String urlPath = exchange.getRequestURI().getPath();
      if (urlPath.equals("/") || urlPath.isEmpty()) {
        urlPath = "/index.html";
      }
      File file = new File(webRoot, urlPath);
      if (!file.exists() || file.isDirectory()) {
        byte[] body = ("404 Not Found: " + urlPath).getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(404, body.length);
        try (OutputStream out = exchange.getResponseBody()) {
          out.write(body);
        }
        return;
      }
      String suffix = "";
      int dot = file.getName().lastIndexOf('.');
      if (dot >= 0) {
        suffix = file.getName().substring(dot + 1).toLowerCase();
      }
      byte[] bytes = Files.readAllBytes(file.toPath());
      exchange.getResponseHeaders().set("Content-Type", mimeTypes.getOrDefault(suffix, "application/octet-stream"));
      exchange.sendResponseHeaders(200, bytes.length);
      try (OutputStream out = exchange.getResponseBody()) {
        out.write(bytes);
      }
    });

    server.setExecutor(null);
    server.start();

    String url = "http://localhost:" + port + (debug ? "?flixel.mode=debug" : "");
    Logger logger = project.getLogger();
    logger.quiet("");
    logger.quiet("[FlixelGDX] Dev server running at " + url);
    logger.quiet("[FlixelGDX] Serving: " + webRoot.getAbsolutePath());
    logger.quiet("[FlixelGDX] Press Ctrl+C to stop.");
    logger.quiet("");

    openBrowser(logger, url);

    try {
      Thread.currentThread().join();
    } catch (InterruptedException e) {
      server.stop(0);
      Thread.currentThread().interrupt();
    }
  }

  /** Attempts to open the default browser at the dev server URL, printing the URL if that fails. */
  private static void openBrowser(Logger logger, String url) {
    String os = System.getProperty("os.name", "").toLowerCase();
    try {
      if (os.contains("linux")) {
        Runtime.getRuntime().exec(new String[] { "xdg-open", url });
      } else if (os.contains("mac")) {
        Runtime.getRuntime().exec(new String[] { "open", url });
      } else {
        Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Desktop.Action.BROWSE)) {
          desktop.browse(new URI(url));
        }
      }
    } catch (Exception ignored) {
      logger.quiet("Please navigate to " + url + " in your browser; it could not be opened automatically.");
    }
  }

  /**
   * Resolves the TeaVM output directory and bundle names, then wires the helper tasks as
   * dependencies of the TeaVM build tasks so a web build always produces a complete app.
   */
  private void wireTeaVm(Project project, DirectoryProperty webRoot, AtomicReference<WebBundle> bundle) {
    TeaVMExtension teavm = project.getExtensions().findByType(TeaVMExtension.class);
    if (teavm == null) {
      project.getLogger().warn(
          "[FlixelGDX] org.teavm extension not found. Apply org.teavm before this plugin so the web root and "
              + "build tasks can be resolved.");
      return;
    }

    boolean jsEnabled = teavm.getJs().getAddedToWebApp().getOrElse(false);
    boolean wasmEnabled = teavm.getWasmGC().getAddedToWebApp().getOrElse(false);
    String jsBundle = teavm.getJs().getTargetFileName().getOrElse(DEFAULT_JS_BUNDLE);
    String wasmBundle = teavm.getWasmGC().getTargetFileName().getOrElse(DEFAULT_WASM_BUNDLE);

    // TeaVM's per-target output directory is the shared parent (build/generated/teavm) and it writes
    // each bundle into a target subdirectory beneath it (js/, wasm-gc/). The web root is therefore
    // that shared parent, and the bundle references carry the subdirectory prefix so the page loads
    // them from the right place.
    webRoot.set(jsEnabled ? teavm.getJs().getOutputDir() : teavm.getWasmGC().getOutputDir());
    bundle.set(new WebBundle(jsEnabled, wasmEnabled, JS_DIR + jsBundle, WASM_DIR + wasmBundle,
        WASM_DIR + deriveWasmRuntime(wasmBundle)));

    wireBuildTask(project, "generateJavaScript");
    wireBuildTask(project, "generateWasmGC");
    dependOn(project, "run", "generateJavaScript");
    dependOn(project, "run", "generateWasmGC");
    dependOn(project, "debug", "generateJavaScript");
    dependOn(project, "debug", "generateWasmGC");
    dependOn(project, "package", "generateJavaScript");
    dependOn(project, "package", "generateWasmGC");

    // TeaVM emits the WebAssembly bundle from generateWasmGC but copies its JavaScript loader runtime
    // (teavm.wasm-runtime.js, the classic TeaVM.wasmGC.load bootstrap the generated page uses) from a
    // separate copyWasmGCRuntime task. Without wiring it in, a WebAssembly-enabled build ships the
    // .wasm but not its loader, so the page falls back to JavaScript. Tie the two together so enabling
    // the WebAssembly target is all a developer has to do.
    if (wasmEnabled) {
      Task generateWasm = project.getTasks().findByName("generateWasmGC");
      Task copyRuntime = project.getTasks().findByName("copyWasmGCRuntime");
      if (generateWasm != null && copyRuntime != null) {
        generateWasm.finalizedBy(copyRuntime);
      }
      dependOn(project, "run", "copyWasmGCRuntime");
      dependOn(project, "debug", "copyWasmGCRuntime");
      dependOn(project, "package", "copyWasmGCRuntime");
    }
  }

  /**
   * Wires the copy tasks and index generation around a TeaVM build task, if that task exists (a
   * target the user did not enable will not have registered its task).
   */
  private void wireBuildTask(Project project, String taskName) {
    Task build = project.getTasks().findByName(taskName);
    if (build == null) {
      return;
    }
    build.dependsOn(project.getTasks().named("copyAssets"), project.getTasks().named("copyWebApp"),
        project.getTasks().named("copyShaders"), project.getTasks().named("generateAssetManifest"));
    Task index = project.getTasks().findByName("generateIndexHtml");
    Task copyWebApp = project.getTasks().findByName("copyWebApp");
    if (index != null) {
      build.finalizedBy(index);
      if (copyWebApp != null) {
        index.mustRunAfter(copyWebApp);
      }
    }
  }

  /** Adds a dependency from one task to another when both exist. */
  private void dependOn(Project project, String taskName, String dependencyName) {
    Task task = project.getTasks().findByName(taskName);
    Task dependency = project.getTasks().findByName(dependencyName);
    if (task != null && dependency != null) {
      task.dependsOn(dependency);
    }
  }

  /**
   * Derives the WebAssembly GC runtime loader file name from the bundle name. TeaVM emits the
   * runtime next to the bundle as {@code <name>.wasm-runtime.js}.
   *
   * @param wasmBundle The WebAssembly bundle file name (for example {@code classes.wasm}).
   * @return The runtime loader file name.
   */
  private static String deriveWasmRuntime(String wasmBundle) {
    String base = wasmBundle.endsWith(".wasm") ? wasmBundle.substring(0, wasmBundle.length() - ".wasm".length())
        : wasmBundle;
    return base + ".wasm-runtime.js";
  }

  /**
   * The resolved bundle layout the index generator needs: which targets are enabled and the file
   * names the loader should reference.
   *
   * @param jsEnabled Whether the JavaScript target is part of the web app.
   * @param wasmEnabled Whether the WebAssembly GC target is part of the web app.
   * @param jsBundle The JavaScript bundle file name.
   * @param wasmBundle The WebAssembly bundle file name.
   * @param wasmRuntime The WebAssembly runtime loader file name.
   */
  private record WebBundle(boolean jsEnabled, boolean wasmEnabled, String jsBundle, String wasmBundle,
      String wasmRuntime) {

    /** Conservative defaults used before the TeaVM extension has been resolved. */
    static final WebBundle DEFAULTS = new WebBundle(true, false, JS_DIR + DEFAULT_JS_BUNDLE,
        WASM_DIR + DEFAULT_WASM_BUNDLE, WASM_DIR + "classes.wasm-runtime.js");
  }
}
