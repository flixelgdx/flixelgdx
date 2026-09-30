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
package org.flixelgdx.gradle.shader;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A command-line compiler that the plugin ships inside its own JAR and runs at build time.
 *
 * <p>The plugin cannot assume a game developer has any shader tooling installed, so it bundles each
 * compiler for every supported operating system under {@code tools/<classifier>/}. The first time a
 * tool is needed, the binary for the current host is copied out of the JAR into the plugin's scratch
 * directory and marked executable. Because this only happens at build time, the size of these
 * binaries never affects a shipped game.
 *
 * <p>A tool is resolved in priority order:
 * <ol>
 *   <li>An explicit path the developer configured in the {@code shaders} block.</li>
 *   <li>The binary bundled in this plugin for the current operating system.</li>
 *   <li>A binary with the same name found on the system {@code PATH}.</li>
 * </ol>
 *
 * <p>Think of it like a toolbox that travels with the plugin: the build opens the box, takes out the
 * tool that fits this machine, and puts it on the workbench before using it.
 */
public final class BundledTool {

  private static final String TOOLS_ROOT = "/org/flixelgdx/gradle/shader/tools/";

  @NotNull
  private final File executable;

  private BundledTool(@NotNull File executable) {
    this.executable = executable;
  }

  /**
   * Resolves a runnable copy of a bundled tool, extracting it into {@code workDir/bin} when needed.
   *
   * @param workDir A writable directory the plugin owns, used to cache the extracted binary.
   * @param override An explicit path configured on the extension, or {@code null} to auto-resolve.
   * @param name The tool's file name without any {@code .exe} suffix, such as {@code glslang}.
   * @param optionName The {@code shaders} block option that overrides this tool, for error messages.
   * @return The resolved tool.
   * @throws IOException When no binary can be found or the bundled one cannot be extracted.
   */
  @NotNull
  public static BundledTool locate(@NotNull File workDir, @Nullable File override, @NotNull String name,
      @NotNull String optionName) throws IOException {
    if (override != null) {
      if (!override.isFile()) {
        throw new IOException("Configured " + optionName + " does not exist: " + override.getAbsolutePath());
      }
      return new BundledTool(override);
    }

    String exeName = isWindows() ? name + ".exe" : name;
    String classifier = hostClassifier();
    String resource = TOOLS_ROOT + classifier + "/" + exeName;
    if (BundledTool.class.getResource(resource) != null) {
      File binDir = new File(workDir, "bin");
      Files.createDirectories(binDir.toPath());
      File dest = new File(binDir, exeName);
      extractResource(resource, dest);
      if (!isWindows()) {
        dest.setExecutable(true, false);
      }
      return new BundledTool(dest);
    }

    File onPath = findOnPath(exeName);
    if (onPath != null) {
      return new BundledTool(onPath);
    }

    throw new IOException(
        "No " + name + " is bundled for this platform (" + classifier + ") and none was found on PATH. "
            + "Set its location with the '" + optionName + "' option in the shaders block, or add a "
            + "bundled binary for this platform to the plugin.");
  }

  /**
   * Runs the tool with the given arguments and waits for it to finish.
   *
   * @param args The command-line arguments, not including the executable itself.
   * @return The process result, including combined output for diagnostics.
   * @throws IOException When the process cannot be started.
   */
  @NotNull
  public ToolResult run(@NotNull List<String> args) throws IOException {
    List<String> cmd = new ArrayList<>(args.size() + 1);
    cmd.add(executable.getAbsolutePath());
    cmd.addAll(args);

    Process process = new ProcessBuilder(cmd).redirectErrorStream(true).start();
    String log;
    try (InputStream in = process.getInputStream()) {
      log = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    int code;
    try {
      code = process.waitFor();
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while waiting for " + executable.getName() + ".", e);
    }
    return new ToolResult(code == 0, log.strip());
  }

  /**
   * Copies a resource from the plugin JAR to a file, replacing any existing copy.
   *
   * @param resource The absolute resource path inside the plugin JAR.
   * @param dest The file to write.
   * @throws IOException When the resource is missing or the file cannot be written.
   */
  static void extractResource(@NotNull String resource, @NotNull File dest) throws IOException {
    try (InputStream in = BundledTool.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Plugin resource missing: " + resource);
      }
      Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * Returns whether the build is running on Windows.
   *
   * @return {@code true} on a Windows host.
   */
  static boolean isWindows() {
    return osName().contains("win");
  }

  @Nullable
  private static File findOnPath(@NotNull String exeName) {
    String path = System.getenv("PATH");
    if (path == null) {
      return null;
    }
    for (String entry : path.split(File.pathSeparator)) {
      File candidate = new File(entry, exeName);
      if (candidate.isFile() && candidate.canExecute()) {
        return candidate;
      }
    }
    return null;
  }

  @NotNull
  private static String hostClassifier() {
    String os = osName();
    String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
    boolean arm = arch.contains("aarch64") || arch.contains("arm64");
    if (os.contains("win")) {
      return arm ? "windows-aarch64" : "windows-x86_64";
    }
    if (os.contains("mac") || os.contains("darwin")) {
      return arm ? "macos-aarch64" : "macos-x86_64";
    }
    return arm ? "linux-aarch64" : "linux-x86_64";
  }

  @NotNull
  private static String osName() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  }

  @NotNull
  public File getExecutable() {
    return executable;
  }
}
