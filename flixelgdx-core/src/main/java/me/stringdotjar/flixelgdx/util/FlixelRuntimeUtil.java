/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.util;

import org.jetbrains.annotations.Nullable;

/**
 * Utility class for handling operation related to the runtime environment, including OS detection,
 * extracting runtime information, obtaining information from exceptions, and other related tasks.
 *
 * <p>Behavior that depends on the JVM, classpath, or JAR layout is supplied by a pluggable
 * {@link RuntimeProbe}. Desktop JVM builds register {@code FlixelJvmRuntimeProbe} from
 * {@code flixelgdx-jvm} at startup. TeaVM and other targets keep the built-in default probe, which
 * reports {@link RunEnvironment#UNKNOWN} for {@link #detectEnvironment()} (no {@code JarFile},
 * no classpath heuristics).
 */
public final class FlixelRuntimeUtil {

  private static volatile RuntimeProbe instance;

  private static final RuntimeProbe DEFAULT_PROBE = new RuntimeProbe() {
    @Override
    public boolean isRunningFromJar() {
      return false;
    }

    @Override
    public boolean isRunningInIDE() {
      return false;
    }

    @Override
    @Nullable
    public String getWorkingDirectory() {
      return defaultCodeSourcePath();
    }

    @Override
    @Nullable
    public String getDefaultLogsFolderPath() {
      return defaultLogsFolderPathForClasspathLikeRuntime();
    }
  };

  /**
   * Installs the probe used for {@link #isRunningFromJar()}, {@link #isRunningInIDE()},
   * {@link #getWorkingDirectory()}, and {@link #getDefaultLogsFolderPath()}. Pass {@code null} to
   * restore the default TeaVM-safe implementation.
   *
   * @param probe The implementation, or {@code null} for the default.
   */
  public static void setRuntimeProbe(@Nullable RuntimeProbe probe) {
    instance = probe;
  }

  /**
   * Returns the active runtime probe, or the default implementation when none was installed.
   *
   * @return The effective probe, never {@code null}.
   */
  public static RuntimeProbe getRuntimeProbe() {
    return probe();
  }

  /**
   * Returns {@code true} when the application is running from a packaged distribution JAR.
   *
   * <p>When using the JVM probe from {@code flixelgdx-jvm}, Gradle builds each module (e.g.
   * {@code flixelgdx-core}) into its own module JAR inside {@code build/libs/} and puts that on the
   * classpath during IDE runs. Checking only whether the code-source path ends with {@code .jar}
   * therefore incorrectly returns {@code true} in the IDE. Instead, the probe opens the JAR that
   * contains this class and inspects its manifest for a {@code Main-Class} attribute. The only JAR
   * in this project that carries that attribute is the fat distribution JAR produced by the
   * {@code lwjgl3:jar} task. Individual module JARs do not have it.
   *
   * @return {@code true} if running from the distribution JAR, {@code false} otherwise.
   */
  public static boolean isRunningFromJar() {
    return probe().isRunningFromJar();
  }

  /**
   * Returns {@code true} when the application is running inside an IDE (IntelliJ, Eclipse, Cursor,
   * VS Code, etc.), and {@code false} when running from the distribution JAR or plain classpath.
   *
   * @return {@code true} if running in an IDE, {@code false} otherwise.
   */
  public static boolean isRunningInIDE() {
    return probe().isRunningInIDE();
  }

  /**
   * Detects the current runtime environment.
   *
   * <p>Uses {@link RuntimeProbe#detectEnvironment()}. The default probe (TeaVM and other non-JVM
   * targets without a registered JVM probe) returns {@link RunEnvironment#UNKNOWN}.
   *
   * @return The detected environment.
   */
  public static RunEnvironment detectEnvironment() {
    return probe().detectEnvironment();
  }

  /**
   * Returns the working directory of the game (code source location: class output dir or JAR path).
   *
   * @return The working directory of the game. If an error occurs, {@code null} is returned.
   */
  @Nullable
  public static String getWorkingDirectory() {
    return probe().getWorkingDirectory();
  }

  /**
   * Returns the default directory path where log files should be stored, depending on the runtime.
   * <ul>
   *   <li>When running in an IDE: the project root directory (inferred from classpath when needed),
   *       so logs go to {@code <project-root>/logs/}.</li>
   *   <li>When running from a JAR: the directory containing the JAR, so logs go to {@code <jar-dir>/logs/}.</li>
   *   <li>Otherwise (e.g. classpath): the current working directory, so logs go to {@code <user.dir>/logs/}.</li>
   * </ul>
   *
   * @return The absolute path to the logs folder (with no trailing separator), or {@code null} if it cannot be determined.
   */
  @Nullable
  public static String getDefaultLogsFolderPath() {
    return probe().getDefaultLogsFolderPath();
  }

  /**
   * Returns the root package name of the library. This is done just in case
   * (for whatever reason it may be) the root package changes.
   *
   * <p>The package is derived from the fully qualified class name rather than
   * {@code Class.getPackageName()}, which is not available on TeaVM.
   *
   * @return The root package name of the library.
   */
  public static String getLibraryRoot() {
    String className = FlixelRuntimeUtil.class.getName();
    int lastDot = className.lastIndexOf('.');
    String packageName = (lastDot > 0) ? className.substring(0, lastDot) : "";
    int rootEnd = packageName.lastIndexOf('.');
    return (rootEnd > 0) ? packageName.substring(0, rootEnd) : packageName;
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

  private static String defaultCodeSourcePath() {
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
   * Logs next to {@code user.dir} when IDE and JAR detection are unavailable (browser / TeaVM).
   */
  private static String defaultLogsFolderPathForClasspathLikeRuntime() {
    String path = defaultCodeSourcePath();
    if (path == null) {
      path = "";
    }
    path = path.replaceAll("/$", "");
    String cwd = System.getProperty("user.dir", "");
    String base = (cwd.isEmpty() ? path : cwd).replaceAll("/$", "");
    if (base.endsWith("/assets")) {
      base = base.substring(0, base.length() - "/assets".length());
    }
    return base + "/logs";
  }

  private static RuntimeProbe probe() {
    RuntimeProbe p = instance;
    return p != null ? p : DEFAULT_PROBE;
  }

  /**
   * Supplies environment detection for the current platform. Desktop JVM games should install an
   * implementation from {@code flixelgdx-jvm} (see {@code FlixelJvmRuntimeProbe}) at startup.
   */
  public interface RuntimeProbe {
    boolean isRunningFromJar();

    boolean isRunningInIDE();

    @Nullable
    String getWorkingDirectory();

    @Nullable
    String getDefaultLogsFolderPath();

    /**
     * Returns IDE, JAR, or classpath when the probe can classify the JVM layout; otherwise
     * {@link RunEnvironment#UNKNOWN} (default implementation for non-desktop probes).
     *
     * @return The detected environment for this probe.
     */
    default RunEnvironment detectEnvironment() {
      return RunEnvironment.UNKNOWN;
    }
  }

  /**
   * High-level runtime classification for logging and tooling. {@link #UNKNOWN} is used when the
   * platform does not support JVM-style detection (default {@link RuntimeProbe}).
   */
  public enum RunEnvironment {
    IDE,
    JAR,
    CLASSPATH,
    UNKNOWN
  }

  private FlixelRuntimeUtil() {}
}
