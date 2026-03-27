/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import java.util.jar.JarFile;

/**
 * Utility class for handling operation related to the runtime environment, including OS detection,
 * extracting runtime information, obtaining information from exceptions, and other related tasks.
 */
public final class FlixelRuntimeUtil {

  /**
   * Returns {@code true} when the application is running from the final packaged distribution JAR,
   * and {@code false} when it is running from the IDE (even if the IDE loads individual module JARs
   * on the classpath).
   *
   * <p>Gradle builds each module (e.g. {@code flixelgdx}) into its own module JAR inside
   * {@code build/libs/} and puts that on the classpath during IDE runs. Checking only whether the
   * code-source path ends with {@code .jar} therefore incorrectly returns {@code true} in the IDE.
   * Instead, this method opens the JAR that contains this class and inspects its manifest for a
   * {@code Main-Class} attribute. The only JAR in this project that carries that attribute is the
   * fat distribution JAR produced by the {@code lwjgl3:jar} task. Individual module JARs do not
   * have it.
   *
   * @return {@code true} if running from the distribution JAR, {@code false} otherwise.
   */
  public static boolean isRunningFromJar() {
    try {
      String path = getWorkingDirectory();

      if (path == null) {
        // Failed to get the working directory, so we can't determine if we're running from a JAR or not.
        return false;
      }

      if (!path.endsWith(".jar")) {
        // Exploded class-file directory, which we can safely assume is from the IDE.
        return false;
      }
      // The class is inside a JAR. Module JARs built during development have no Main-Class entry.
      // The fat distribution JAR always does (set by the lwjgl3:jar manifest block).
      try (JarFile jar = new JarFile(path)) {
        var manifest = jar.getManifest();
        return manifest != null && manifest.getMainAttributes().getValue("Main-Class") != null;
      }
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Returns {@code true} when the application is running inside an IDE (IntelliJ, Eclipse, Cursor,
   * VS Code, etc.), and {@code false} when running from the distribution JAR or plain classpath.
   *
   * <p>Detection uses IDE-specific system properties, classpath hints, and the code source path.
   * Gradle-based runs are detected via {@code build/classes} (exploded) or a JAR under
   * {@code build/libs/} that is not the distribution JAR (no {@code Main-Class} in manifest).
   *
   * @return {@code true} if running in an IDE, {@code false} otherwise.
   */
  public static boolean isRunningInIDE() {
    // IntelliJ (run/debug).
    if (System.getProperty("idea.launcher.port") != null) {
      return true;
    }
    // IntelliJ fallback: idea_rt.jar on classpath (e.g. debug).
    if (System.getProperty("java.class.path", "").contains("idea_rt.jar")) {
      return true;
    }
    // Eclipse.
    if (System.getProperty("eclipse.application") != null) {
      return true;
    }
    var path = getWorkingDirectory();
    if (path == null) {
      return false;
    }
    // IntelliJ default output.
    if (path.contains("out/production")) {
      return true;
    }
    // Eclipse default output.
    if (path.contains("bin/")) {
      return true;
    }
    // Gradle: exploded classes (e.g. build/classes/java/main).
    if (path.contains("build/classes")) {
      return true;
    }
    // Gradle: module JAR on classpath (build/libs/*.jar without Main-Class); distribution JAR has Main-Class.
    if (path.contains("build/libs") && path.endsWith(".jar") && !isRunningFromJar()) {
      return true;
    }
    return false;
  }

  /**
	 * Detects the current runtime environment.
	 *
	 * <p>This method is used to determine if the application is running in the IDE, from a JAR, or from the classpath.
	 *
	 * @return The detected environment.
	 */
	public static RunEnvironment detectEnvironment() {
		if (isRunningInIDE()) {
			return RunEnvironment.IDE;
		}
    if (isRunningFromJar()) {
			return RunEnvironment.JAR;
    }
    return RunEnvironment.CLASSPATH;
  }

  /**
   * Returns the working directory of the game (code source location: class output dir or JAR path).
   *
   * @return The working directory of the game. If an error occurs, {@code null} is returned.
   */
  public static String getWorkingDirectory() {
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

  /**
   * Returns the default directory path where log files should be stored, depending on the runtime.
   * <ul>
   *   <li>When running in an IDE: the project root directory, so logs go to {@code <project-root>/logs/}.</li>
   *   <li>When running from a JAR: the directory containing the JAR, so logs go to {@code <jar-dir>/logs/}.</li>
   *   <li>Otherwise (e.g. classpath): the current working directory, so logs go to {@code <user.dir>/logs/}.</li>
   * </ul>
   *
   * @return The absolute path to the logs folder (with no trailing separator), or {@code null} if it cannot be determined.
   */
  public static String getDefaultLogsFolderPath() {
    String path = getWorkingDirectory();
    if (path == null) {
      return null;
    }
    path = path.replaceAll("/$", "");
    if (isRunningInIDE()) {
      // Project root: strip IDE/build output segments so we get the project root.
      if (path.contains("build/classes")) {
        path = path.substring(0, path.indexOf("build/classes"));
      } else if (path.contains("out/production")) {
        path = path.substring(0, path.indexOf("out/production"));
      } else if (path.contains("/bin/")) {
        path = path.substring(0, path.indexOf("/bin/"));
      }
      path = path.replaceAll("/$", "");
      // In many libGDX projects, the working directory is the assets folder; if so,
      // trim the trailing /assets segment so logs go to the project root instead of assets/.
      if (path.endsWith("/assets")) {
        path = path.substring(0, path.length() - "/assets".length());
      }
      return path + "/logs";
    }
    if (isRunningFromJar()) {
      // Directory containing the JAR.
      int lastSlash = path.lastIndexOf('/');
      if (lastSlash > 0) {
        path = path.substring(0, lastSlash);
      }
      return path.replaceAll("/$", "") + "/logs";
    }
    // CLASSPATH or other: use current working directory.
    String cwd = System.getProperty("user.dir", "");
    String base = (cwd.isEmpty() ? path : cwd).replaceAll("/$", "");
    // If the working directory is the assets folder, move one level up so logs land in the
    // project root rather than under assets/.
    if (base.endsWith("/assets")) {
      base = base.substring(0, base.length() - "/assets".length());
    }
    return base + "/logs";
  }

  /**
   * Returns the root package name of the library. This is done just in case
   * (for whatever reason it may be) the root package changes.
   *
   * @return The root package name of the library.
   */
  public static String getLibraryRoot() {
    return FlixelRuntimeUtil.class.getPackageName().replaceAll("\\.[^.]+$", "");
  }

  /**
   * Obtains a string representation of where an exception was thrown from, including the class,
   * method, file, and line number.
   *
   * @param exception The exception to obtain the location from.
   * @return A string representation of where the exception was thrown from.
   */
  public static String getExceptionLocation(Throwable exception) {
    if (exception == null) {
      return "Unknown Location";
    }
    StackTraceElement[] stackTrace = exception.getStackTrace();
    if (stackTrace.length == 0) {
      return "Unknown Location";
    }
    StackTraceElement element = stackTrace[0];
    return "FILE="
      + element.getFileName()
      + ", CLASS="
      + element.getClassName()
      + ", METHOD="
      + element.getMethodName()
      + "(), LINE="
      + element.getLineNumber();
  }

  /**
   * Obtains a full detailed message from an exception, including its type, location, and stack trace.
   *
   * @param exception The exception to obtain the message from.
   * @return A full detailed message from the exception.
   */
  public static String getFullExceptionMessage(Throwable exception) {
    if (exception == null) {
      return "No exception provided.";
    }
    StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("Exception: ").append(exception).append("\n");
    messageBuilder.append("Location: ").append(getExceptionLocation(exception)).append("\n");
    messageBuilder.append("Stack Trace:\n");
    for (StackTraceElement element : exception.getStackTrace()) {
      messageBuilder.append("\tat ").append(element.toString()).append("\n");
    }
    return messageBuilder.toString();
  }

  /**
   * Enum representing the environment in which the game is running in.
   */
  public enum RunEnvironment {
    IDE,
    JAR,
    CLASSPATH
  }

  private FlixelRuntimeUtil() {}
}
