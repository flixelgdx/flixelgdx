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
package org.flixelgdx.backend.lwjgl3.video;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * Locates a working libvlc installation and prepares the process so libvlc can find
 * its plugin directory.
 *
 * <p>Candidates are tried in order, and each one is verified with a real
 * {@code libvlc_new} probe before it is accepted. A libvlc that loads but cannot
 * initialize (for example a distribution package with the plugin set missing) is
 * skipped instead of crashing the game, and the search falls through to the next
 * candidate.
 *
 * <p>Search order:
 *
 * <ol>
 *   <li>The {@code flixel.video.vlc.path} system property: a directory that contains
 *       the libvlc shared library and its {@code plugins} folder.</li>
 *   <li>A {@code vlc/} directory next to the game's working directory (for games that
 *       ship VLC alongside the executable).</li>
 *   <li>The natives bundled inside the {@code flixelgdx-video-lwjgl3} JAR under
 *       {@code org/flixelgdx/video/natives/}, extracted once to a per-user cache. On
 *       Windows and macOS these are the official self-contained VideoLAN builds and
 *       are preferred over a system install.</li>
 *   <li>A system-installed VLC (Program Files on Windows, {@code /Applications/VLC.app}
 *       on macOS, the system linker path on Linux). On Linux this is tried before the
 *       bundled copy, because distribution VLC plugins link against distribution codec
 *       libraries and the local install is the configuration that is guaranteed to
 *       match them.</li>
 * </ol>
 */
final class FlixelVlcDiscovery {

  /** System property naming a directory that contains libvlc and its plugins folder. */
  static final String PATH_PROPERTY = "flixel.video.vlc.path";

  private static final String RESOURCE_ROOT = "org/flixelgdx/video/natives/";

  private FlixelVlcDiscovery() {}

  /**
   * Finds a working libvlc, sets {@code VLC_PLUGIN_PATH} when needed, and loads it.
   *
   * @return The loaded libvlc native library, ready for {@link LibVlc#register}.
   * @throws IllegalStateException If no usable VLC installation could be found.
   */
  @NotNull
  static NativeLibrary load() {
    List<String> attempts = new ArrayList<>();

    String explicit = System.getProperty(PATH_PROPERTY);
    if (explicit != null && !explicit.isBlank()) {
      NativeLibrary lib = tryDirectory(new File(explicit), attempts);
      if (lib != null) {
        return lib;
      }
      throw new IllegalStateException(
          "No usable libvlc found in " + PATH_PROPERTY + "=" + explicit
              + " (attempts: " + String.join("; ", attempts) + ")");
    }

    NativeLibrary lib = tryDirectory(new File("vlc"), attempts);
    if (lib != null) {
      return lib;
    }

    boolean linux = isLinux();
    if (!linux) {
      lib = tryBundled(attempts);
      if (lib != null) {
        return lib;
      }
    }
    lib = trySystem(attempts);
    if (lib == null && linux) {
      lib = tryBundled(attempts);
    }
    if (lib != null) {
      return lib;
    }

    throw new IllegalStateException(
        "FlixelGDX video could not find a working libvlc. Attempts: ["
            + String.join("; ", attempts) + "]. Fixes: install VLC from "
            + "https://www.videolan.org (on Linux also install your distribution's VLC "
            + "plugin packages, e.g. vlc-plugin-base), ship a 'vlc' folder next to the "
            + "game, or point -D" + PATH_PROPERTY + " at a VLC directory.");
  }

  /**
   * Attempts to load and probe libvlc from one directory.
   *
   * @param dir Directory expected to contain the shared libraries and a plugins folder.
   * @param attempts Human-readable log of what was tried, for the final error message.
   * @return The loaded library, or {@code null} if this directory does not work.
   */
  @Nullable
  private static NativeLibrary tryDirectory(@NotNull File dir, @NotNull List<String> attempts) {
    if (!dir.isDirectory()) {
      return null;
    }
    File lib = findLibrary(dir, "libvlc");
    File core = findLibrary(dir, "libvlccore");
    if (lib == null && isMac()) {
      File macLib = new File(dir, "lib");
      if (macLib.isDirectory()) {
        lib = findLibrary(macLib, "libvlc");
        core = findLibrary(macLib, "libvlccore");
      }
    }
    if (lib == null) {
      attempts.add(dir.getAbsolutePath() + " (no libvlc library found)");
      return null;
    }

    File pluginDir = new File(dir, "plugins");
    if (!pluginDir.isDirectory()) {
      // Linux layouts keep plugins under vlc/plugins next to the libraries.
      File nested = new File(dir, "vlc/plugins");
      pluginDir = nested.isDirectory() ? nested : null;
    }
    setPluginPathEnv(pluginDir != null ? pluginDir.getAbsolutePath() : null);

    try {
      if (core != null) {
        // Loading libvlccore first lets the dynamic linker satisfy libvlc's dependency
        // even though the directory is not on the system library path.
        NativeLibrary.getInstance(core.getAbsolutePath());
      }
      NativeLibrary loaded = NativeLibrary.getInstance(lib.getAbsolutePath());
      if (!probe(lib.getAbsolutePath())) {
        attempts.add(lib.getAbsolutePath()
            + " (loaded, but libvlc_new failed; plugins missing or broken)");
        return null;
      }
      if (!codecPluginLoads(pluginDir)) {
        // libvlc_new succeeds even when individual plugins cannot load their own
        // dependencies; it just skips them and later fails to decode anything, which
        // shows up as a silent black video. Checking the main codec plugin up front
        // rejects such an installation so the search can fall through to a working one.
        attempts.add(lib.getAbsolutePath()
            + " (initialized, but the avcodec plugin cannot load; its codec libraries "
            + "are missing or from a different distribution)");
        return null;
      }
      return loaded;
    } catch (UnsatisfiedLinkError error) {
      attempts.add(lib.getAbsolutePath() + " (failed to load: " + error.getMessage() + ")");
      return null;
    }
  }

  /** Extracts classpath-bundled natives (if present) to a per-user cache and probes them. */
  @Nullable
  private static NativeLibrary tryBundled(@NotNull List<String> attempts) {
    String platform = platformDirectory();
    String root = RESOURCE_ROOT + platform + "/";
    URL marker = FlixelVlcDiscovery.class.getClassLoader().getResource(root);
    if (marker == null) {
      attempts.add("bundled natives (none on classpath)");
      return null;
    }
    try {
      File cacheDir = cacheDirectory(platform);
      // The marker alone is not trusted: if the user (or a cleaner tool) removed the
      // extracted libraries but the marker survived, everything is extracted again.
      if (!new File(cacheDir, ".complete").exists() || findLibrary(cacheDir, "libvlc") == null) {
        extractResources(marker, root, cacheDir.toPath());
        Files.writeString(new File(cacheDir, ".complete").toPath(), "ok");
      }
      return tryDirectory(cacheDir, attempts);
    } catch (IOException e) {
      attempts.add("bundled natives (extraction failed: " + e.getMessage() + ")");
      return null;
    }
  }

  /** Tries well-known system installation locations for the current OS. */
  @Nullable
  private static NativeLibrary trySystem(@NotNull List<String> attempts) {
    if (isWindows()) {
      String[] roots = {
          System.getenv("ProgramFiles"),
          System.getenv("ProgramFiles(x86)")
      };
      for (String rootPath : roots) {
        if (rootPath == null) {
          continue;
        }
        NativeLibrary lib = tryDirectory(new File(rootPath, "VideoLAN/VLC"), attempts);
        if (lib != null) {
          return lib;
        }
      }
      return null;
    }
    if (isMac()) {
      return tryDirectory(new File("/Applications/VLC.app/Contents/MacOS"), attempts);
    }
    try {
      // A system-wide libvlc knows its own plugin directory, but a plugin path set by
      // an earlier candidate must not leak into this attempt.
      setPluginPathEnv(null);
      NativeLibrary loaded = NativeLibrary.getInstance("vlc");
      if (probe("vlc")) {
        return loaded;
      }
      attempts.add("system libvlc (loaded, but libvlc_new failed; install vlc-plugin-base)");
      return null;
    } catch (UnsatisfiedLinkError error) {
      attempts.add("system libvlc (not found on linker path)");
      return null;
    }
  }

  /**
   * Verifies that the candidate's avcodec plugin (the decoder used by virtually every
   * video format) can be loaded by the dynamic linker.
   *
   * <p>This catches plugin sets built against codec libraries the current system does
   * not have, for example Debian-built plugins that need a libavcodec version another
   * distribution no longer ships. When the plugin directory is unknown or has an
   * unexpected layout, the candidate is given the benefit of the doubt because the
   * {@code libvlc_new} probe already passed.
   */
  private static boolean codecPluginLoads(@Nullable File pluginDir) {
    if (pluginDir == null) {
      return true;
    }
    File plugin = findLibrary(new File(pluginDir, "codec"), "libavcodec_plugin");
    if (plugin == null) {
      return true;
    }
    try {
      NativeLibrary.getInstance(plugin.getAbsolutePath());
      return true;
    } catch (UnsatisfiedLinkError error) {
      return false;
    }
  }

  /**
   * Verifies a candidate by creating and immediately releasing a throwaway libvlc
   * instance. This is the only reliable test that the plugin set is actually usable.
   */
  private static boolean probe(@NotNull String libraryPathOrName) {
    try {
      ProbeLibrary probeLib = Native.load(libraryPathOrName, ProbeLibrary.class);
      Pointer instance = probeLib.libvlc_new(0, null);
      if (instance == null) {
        return false;
      }
      probeLib.libvlc_release(instance);
      return true;
    } catch (UnsatisfiedLinkError error) {
      return false;
    }
  }

  @Nullable
  private static File findLibrary(@NotNull File dir, @NotNull String baseName) {
    String[] candidates;
    if (isWindows()) {
      candidates = new String[] { baseName + ".dll", baseName.substring(3) + ".dll" };
    } else if (isMac()) {
      candidates = new String[] { baseName + ".dylib", baseName + ".5.dylib", baseName + ".9.dylib" };
    } else {
      candidates = new String[] {
          baseName + ".so", baseName + ".so.5", baseName + ".so.9",
          baseName + ".so.5.6.1", baseName + ".so.9.0.1"
      };
    }
    for (String name : candidates) {
      File f = new File(dir, name);
      if (f.isFile()) {
        return f;
      }
    }
    return null;
  }

  /**
   * Publishes {@code VLC_PLUGIN_PATH} into the C runtime environment so libvlccore's
   * module loader (which reads it with {@code getenv}) can see it. Plain
   * {@code System.setProperty} cannot do this, hence the tiny libc binding. Passing
   * {@code null} clears the variable so a value set for an earlier candidate cannot
   * poison a later one.
   */
  private static void setPluginPathEnv(@Nullable String pluginPath) {
    try {
      if (isWindows()) {
        WindowsCRuntime.INSTANCE._putenv_s("VLC_PLUGIN_PATH", pluginPath != null ? pluginPath : "");
      } else if (pluginPath != null) {
        PosixCRuntime.INSTANCE.setenv("VLC_PLUGIN_PATH", pluginPath, 1);
      } else {
        PosixCRuntime.INSTANCE.unsetenv("VLC_PLUGIN_PATH");
      }
    } catch (LinkageError error) {
      // Without the environment variable libvlc falls back to its built-in search,
      // which still succeeds for system installations.
    }
  }

  private static void extractResources(@NotNull URL rootUrl, @NotNull String root,
      @NotNull Path targetDir) throws IOException {
    Files.createDirectories(targetDir);
    if ("jar".equals(rootUrl.getProtocol())) {
      JarURLConnection connection = (JarURLConnection) rootUrl.openConnection();
      connection.setUseCaches(false);
      try (JarFile jar = connection.getJarFile()) {
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
          JarEntry entry = entries.nextElement();
          if (entry.isDirectory() || !entry.getName().startsWith(root)) {
            continue;
          }
          Path out = targetDir.resolve(entry.getName().substring(root.length()));
          Files.createDirectories(out.getParent());
          try (InputStream in = jar.getInputStream(entry)) {
            Files.copy(in, out, StandardCopyOption.REPLACE_EXISTING);
          }
        }
      }
    } else if ("file".equals(rootUrl.getProtocol())) {
      Path source = new File(rootUrl.getPath()).toPath();
      try (var stream = Files.walk(source)) {
        for (Path path : (Iterable<Path>) stream::iterator) {
          if (Files.isDirectory(path)) {
            continue;
          }
          Path out = targetDir.resolve(source.relativize(path).toString());
          Files.createDirectories(out.getParent());
          Files.copy(path, out, StandardCopyOption.REPLACE_EXISTING);
        }
      }
    }
  }

  @NotNull
  private static File cacheDirectory(@NotNull String platform) throws IOException {
    String home = System.getProperty("user.home", ".");
    File base;
    if (isWindows()) {
      String localAppData = System.getenv("LOCALAPPDATA");
      base = localAppData != null ? new File(localAppData) : new File(home, "AppData/Local");
    } else if (isMac()) {
      base = new File(home, "Library/Caches");
    } else {
      String xdg = System.getenv("XDG_CACHE_HOME");
      base = xdg != null ? new File(xdg) : new File(home, ".cache");
    }
    File dir = new File(base, "flixelgdx/vlc/" + platform);
    Files.createDirectories(dir.toPath());
    return dir;
  }

  @NotNull
  private static String platformDirectory() {
    if (isWindows()) {
      return "windows-amd64";
    }
    if (isMac()) {
      return "macos-universal";
    }
    return "linux-amd64";
  }

  private static boolean isWindows() {
    return osName().contains("windows");
  }

  private static boolean isMac() {
    return osName().contains("mac");
  }

  private static boolean isLinux() {
    return osName().contains("linux");
  }

  @NotNull
  private static String osName() {
    return System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
  }

  /** Minimal interface-mapped binding used only to probe candidates before committing. */
  private interface ProbeLibrary extends Library {

    Pointer libvlc_new(int argc, Pointer argv);

    void libvlc_release(Pointer instance);
  }

  /** Binding for POSIX setenv so VLC_PLUGIN_PATH reaches libvlccore's getenv. */
  private interface PosixCRuntime extends Library {

    PosixCRuntime INSTANCE = Native.load("c", PosixCRuntime.class);

    int setenv(String name, String value, int overwrite);

    int unsetenv(String name);
  }

  /** Binding for the Windows C runtime equivalent of setenv. */
  private interface WindowsCRuntime extends Library {

    WindowsCRuntime INSTANCE = Native.load("msvcrt", WindowsCRuntime.class);

    int _putenv_s(String name, String value);
  }
}
