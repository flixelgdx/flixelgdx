/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.backend.jvm.runtime;

import java.util.ArrayList;
import java.util.jar.JarFile;

import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil;
import me.stringdotjar.flixelgdx.util.FlixelRuntimeUtil.RunEnvironment;
import org.jetbrains.annotations.Nullable;

/**
 * JVM desktop implementation of {@link FlixelRuntimeUtil.RuntimeProbe}: classpath and JAR checks,
 * IDE detection, and default log directory resolution. Not used on TeaVM or other non-JVM targets.
 */
public final class FlixelJvmRuntimeProbe implements FlixelRuntimeUtil.RuntimeProbe {

  @Override
  public RunEnvironment detectEnvironment() {
    if (isRunningInIDE()) {
      return RunEnvironment.IDE;
    }
    if (isRunningFromJar()) {
      return RunEnvironment.JAR;
    }
    return RunEnvironment.CLASSPATH;
  }

  @Override
  public boolean isRunningFromJar() {
    try {
      String path = getWorkingDirectory();
      if (path == null) {
        return false;
      }
      if (!path.endsWith(".jar")) {
        return false;
      }
      try (JarFile jar = new JarFile(path)) {
        var manifest = jar.getManifest();
        return manifest != null && manifest.getMainAttributes().getValue("Main-Class") != null;
      }
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean isRunningInIDE() {
    if (System.getProperty("idea.launcher.port") != null) {
      return true;
    }
    if (System.getProperty("idea.paths.selector") != null) {
      return true;
    }
    if (System.getProperty("java.class.path", "").contains("idea_rt.jar")) {
      return true;
    }
    if (System.getProperty("eclipse.application") != null) {
      return true;
    }
    if (classpathContainsIdeStyleOutput()) {
      return true;
    }
    String path = getWorkingDirectory();
    if (path == null) {
      return false;
    }
    if (path.contains("out/production")) {
      return true;
    }
    if (path.contains("bin/")) {
      return true;
    }
    if (path.contains("build/classes")) {
      return true;
    }
    return path.contains("build/libs") && path.endsWith(".jar") && !isRunningFromJar();
  }

  @Override
  @Nullable
  public String getWorkingDirectory() {
    try {
      return FlixelRuntimeUtil.class
        .getProtectionDomain()
        .getCodeSource()
        .getLocation()
        .toURI()
        .getPath();
    } catch (Exception e) {
      return null;
    }
  }

  @Override
  @Nullable
  public String getDefaultLogsFolderPath() {
    String path = getWorkingDirectory();
    if (path == null) {
      path = "";
    }
    path = path.replaceAll("/$", "");
    if (isRunningInIDE()) {
      String projectRoot = inferIdeProjectRootDirectory();
      if (projectRoot == null || projectRoot.isEmpty()) {
        projectRoot = stripIdeOutputSegments(path);
      }
      if (projectRoot == null || projectRoot.isEmpty()) {
        projectRoot = System.getProperty("user.dir", "");
      }
      projectRoot = projectRoot.replaceAll("/$", "");
      if (projectRoot.endsWith("/assets")) {
        projectRoot = projectRoot.substring(0, projectRoot.length() - "/assets".length());
      }
      return projectRoot.replaceAll("/$", "") + "/logs";
    }
    if (isRunningFromJar()) {
      int lastSlash = path.lastIndexOf('/');
      if (lastSlash > 0) {
        path = path.substring(0, lastSlash);
      }
      return path.replaceAll("/$", "") + "/logs";
    }
    String cwd = System.getProperty("user.dir", "");
    String base = (cwd.isEmpty() ? path : cwd).replaceAll("/$", "");
    if (base.endsWith("/assets")) {
      base = base.substring(0, base.length() - "/assets".length());
    }
    return base + "/logs";
  }

  private static boolean classpathContainsIdeStyleOutput() {
    String cp = System.getProperty("java.class.path", "");
    if (cp.isEmpty()) {
      return false;
    }
    String normalized = cp.replace('\\', '/');
    return normalized.contains("build/classes")
      || normalized.contains("out/production")
      || normalized.contains("/bin/");
  }

  @Nullable
  private static String moduleRootFromClasspathEntry(@Nullable String entry) {
    if (entry == null || entry.isEmpty()) {
      return null;
    }
    if (entry.charAt(0) == '"' && entry.length() >= 2 && entry.charAt(entry.length() - 1) == '"') {
      entry = entry.substring(1, entry.length() - 1);
    }
    String forward = entry.replace('\\', '/');
    int idx = forward.indexOf("build/classes");
    if (idx >= 0) {
      return trimTrailingPathSeparators(entry.substring(0, idx));
    }
    idx = forward.indexOf("out/production");
    if (idx >= 0) {
      return trimTrailingPathSeparators(entry.substring(0, idx));
    }
    idx = forward.indexOf("/bin/");
    if (idx >= 0) {
      return trimTrailingPathSeparators(entry.substring(0, idx));
    }
    return null;
  }

  private static String trimTrailingPathSeparators(String path) {
    int end = path.length();
    while (end > 0) {
      char c = path.charAt(end - 1);
      if (c == '/' || c == '\\') {
        end--;
      } else {
        break;
      }
    }
    return end == path.length() ? path : path.substring(0, end);
  }

  @Nullable
  private static String inferIdeProjectRootDirectory() {
    ArrayList<String> roots = collectNormalizedModuleRootsFromClasspath();
    if (roots.isEmpty()) {
      return null;
    }
    if (roots.size() == 1) {
      return roots.get(0);
    }
    String common = longestCommonDirectoryPrefix(roots);
    if (common != null && !common.isEmpty()) {
      return common;
    }
    return longestModuleRootMatchingUserDir(roots, System.getProperty("user.dir", ""));
  }

  private static char classpathSeparatorChar() {
    String ps = System.getProperty("path.separator");
    if (ps != null && !ps.isEmpty()) {
      return ps.charAt(0);
    }
    return ':';
  }

  private static ArrayList<String> collectNormalizedModuleRootsFromClasspath() {
    ArrayList<String> roots = new ArrayList<>(8);
    String cp = System.getProperty("java.class.path", "");
    char sep = classpathSeparatorChar();
    int start = 0;
    while (start <= cp.length()) {
      int next = cp.indexOf(sep, start);
      String entry = next < 0 ? cp.substring(start) : cp.substring(start, next);
      start = next + 1;
      String moduleRoot = moduleRootFromClasspathEntry(entry);
      if (moduleRoot != null) {
        String normalized = normalizePathForCompare(moduleRoot);
        if (!normalized.isEmpty() && !roots.contains(normalized)) {
          roots.add(normalized);
        }
      }
      if (next < 0) {
        break;
      }
    }
    return roots;
  }

  private static String normalizePathForCompare(String path) {
    return trimTrailingPathSeparators(path.replace('\\', '/'));
  }

  private static String longestModuleRootMatchingUserDir(ArrayList<String> roots, String userDir) {
    if (userDir == null || userDir.isEmpty()) {
      return roots.get(0);
    }
    String ud = normalizePathForCompare(userDir);
    String best = null;
    int bestLen = -1;
    for (int i = 0, n = roots.size(); i < n; i++) {
      String r = roots.get(i);
      if (pathPrefixMatches(ud, r) && r.length() > bestLen) {
        bestLen = r.length();
        best = r;
      }
    }
    return best != null ? best : roots.get(0);
  }

  private static boolean pathPrefixMatches(String path, String prefix) {
    if (!path.startsWith(prefix)) {
      return false;
    }
    return path.length() == prefix.length() || path.charAt(prefix.length()) == '/';
  }

  @Nullable
  private static String longestCommonDirectoryPrefix(ArrayList<String> paths) {
    String first = paths.get(0);
    int end = first.length();
    for (int i = 1, n = paths.size(); i < n; i++) {
      end = sharedPrefixLength(first, paths.get(i), end);
      if (end == 0) {
        return null;
      }
    }
    return directoryPrefix(first, end);
  }

  private static int sharedPrefixLength(String a, String b, int maxLen) {
    int n = Math.min(Math.min(a.length(), b.length()), maxLen);
    int i = 0;
    for (; i < n; i++) {
      char ca = normalizeSlashForCompare(a.charAt(i));
      char cb = normalizeSlashForCompare(b.charAt(i));
      if (ca != cb) {
        break;
      }
    }
    return i;
  }

  private static char normalizeSlashForCompare(char c) {
    return c == '\\' ? '/' : c;
  }

  @Nullable
  private static String directoryPrefix(String path, int endExclusive) {
    if (endExclusive <= 0) {
      return null;
    }
    int e = endExclusive;
    while (e > 0 && path.charAt(e - 1) != '/' && path.charAt(e - 1) != '\\') {
      e--;
    }
    while (e > 0 && (path.charAt(e - 1) == '/' || path.charAt(e - 1) == '\\')) {
      e--;
    }
    return e > 0 ? path.substring(0, e) : null;
  }

  private static String stripIdeOutputSegments(String path) {
    if (path.isEmpty()) {
      return "";
    }
    String forward = path.replace('\\', '/');
    int idx = forward.indexOf("build/classes");
    if (idx >= 0) {
      return trimTrailingPathSeparators(path.substring(0, idx));
    }
    idx = forward.indexOf("out/production");
    if (idx >= 0) {
      return trimTrailingPathSeparators(path.substring(0, idx));
    }
    idx = forward.indexOf("/bin/");
    if (idx >= 0) {
      return trimTrailingPathSeparators(path.substring(0, idx));
    }
    return "";
  }
}
