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
package org.flixelgdx.gradle.project;

import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;
import org.gradle.api.tasks.options.Option;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds one to {@code app.build} in {@code project.toml} and raises {@code app.version} to match, editing only those
 * lines.
 *
 * <p>The build number has to go up with every release (Android refuses to install an older one over a newer one),
 * and the version people see usually changes with it. Forgetting either is easy, so running {@code ./gradlew bumpBuild}
 * before a release does both for you. By default the last number of the version goes up by one:
 *
 * <pre>{@code
 * $ ./gradlew bumpBuild
 * project.toml: app.build 14 -> 15
 * project.toml: app.version 0.3.0 -> 0.3.1
 * }</pre>
 *
 * <p>For a bigger step, pass the new version with {@code --new-version}:
 *
 * <pre>{@code
 * $ ./gradlew bumpBuild --new-version=1.0.0
 * project.toml: app.build 15 -> 16
 * project.toml: app.version 0.3.1 -> 1.0.0
 * }</pre>
 *
 * <p>Only the values change, so comments, spacing and the order of the file stay exactly as they were. When the file
 * has no {@code app.build} yet, the build number is {@code 1}, so the task adds {@code build = 2} to the {@code [app]}
 * table, right after {@code version} when it is there. A version that does not end in a number, such as
 * {@code 1.0-beta}, cannot be raised by one, so it is left alone unless {@code --new-version} is given.
 */
@UntrackedTask(because = "It edits project.toml in place, so there is no separate output to track.")
public abstract class BumpBuildTask extends DefaultTask {

  /** The name of the task. */
  public static final String NAME = "bumpBuild";

  // The value part only allows a plain decimal number (TOML allows '_' between digits), plus whatever follows it.
  // DOTALL lets the rest of the line keep the '\r' of a Windows line ending.
  private static final Pattern IN_TABLE =
      Pattern.compile("^(\\s*build\\s*=\\s*)([0-9][0-9_]*)(.*)$", Pattern.DOTALL);
  private static final Pattern DOTTED =
      Pattern.compile("^(\\s*app\\s*\\.\\s*build\\s*=\\s*)([0-9][0-9_]*)(.*)$", Pattern.DOTALL);
  private static final Pattern VERSION_LINE = Pattern.compile("^\\s*version\\s*=.*$", Pattern.DOTALL);
  // A version is a single-line string with no escapes, in either quote style. A multi-line string never matches,
  // because the value has to start right after the opening quote.
  private static final Pattern VERSION_IN_TABLE =
      Pattern.compile("^(\\s*version\\s*=\\s*)([\"'])([^\"'\\\\]+)\\2(.*)$", Pattern.DOTALL);
  private static final Pattern VERSION_DOTTED =
      Pattern.compile("^(\\s*app\\s*\\.\\s*version\\s*=\\s*)([\"'])([^\"'\\\\]+)\\2(.*)$", Pattern.DOTALL);
  // Only a version made of plain numbers separated by dots can be raised by one.
  private static final Pattern NUMERIC_VERSION = Pattern.compile("[0-9]+(\\.[0-9]+)*");

  /** Raises the build number and the version, and saves the file. */
  @TaskAction
  public void bump() {
    File file = getProjectFile().get().getAsFile();
    String text;
    try {
      text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GradleException("Could not read " + file + ".", e);
    }
    ProjectModel model = ProjectToml.parse(text);
    int before = model.build();
    String oldVersion = model.version();
    String newVersion = getNewVersion().getOrNull();
    if (newVersion != null) {
      ProjectValidator.checkVersion("--new-version", newVersion);
    } else {
      newVersion = nextVersion(oldVersion);
    }
    String updated = bump(text);
    if (newVersion != null) {
      updated = setVersion(updated, newVersion);
    }
    try {
      Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GradleException("Could not write " + file + ".", e);
    }
    // Quiet is the level that is always shown, so the new number is visible even in quiet builds.
    getLogger().quiet("{}: app.build {} -> {}", ProjectPlugin.FILE_NAME, before, before + 1);
    if (newVersion != null) {
      getLogger().quiet("{}: app.version {} -> {}", ProjectPlugin.FILE_NAME, oldVersion, newVersion);
    } else {
      getLogger().quiet("{}: app.version {} was left as it is, because it does not end in a number. Pass"
          + " --new-version=<version> to change it.", ProjectPlugin.FILE_NAME, oldVersion);
    }
  }

  /**
   * Returns a version with its last number raised by one, such as {@code 0.3.1} for {@code 0.3.0}.
   *
   * @param version The current version.
   * @return The next version, or {@code null} when the version is not made of plain numbers separated by dots (such
   *     as {@code 1.0-beta}), because there is no safe guess for what comes after it.
   */
  static @Nullable String nextVersion(String version) {
    if (!NUMERIC_VERSION.matcher(version).matches()) {
      return null;
    }
    int dot = version.lastIndexOf('.');
    String last = version.substring(dot + 1);
    // BigInteger keeps a very long number from overflowing.
    return version.substring(0, dot + 1) + new BigInteger(last).add(BigInteger.ONE);
  }

  /**
   * Returns the text of a {@code project.toml} with {@code app.version} set to a new value.
   *
   * @param text The current file contents.
   * @param version The new version, which must already be a valid version.
   * @return The new file contents, the same as {@code text} apart from the version.
   * @throws GradleException if the version is written in a way this task cannot edit safely (such as a quoted key or
   *     a multi-line string), in which case it should be changed by hand.
   */
  static String setVersion(String text, String version) {
    String[] lines = text.split("\n", -1);
    String table = "";
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String stripped = line.strip();
      if (stripped.startsWith("[")) {
        table = tableName(stripped);
        continue;
      }
      Matcher matcher = null;
      if (table.equals("app")) {
        matcher = VERSION_IN_TABLE.matcher(line);
      } else if (table.isEmpty()) {
        matcher = VERSION_DOTTED.matcher(line);
      }
      if (matcher != null && matcher.matches()) {
        String quote = matcher.group(2);
        lines[i] = matcher.group(1) + quote + version + quote + matcher.group(4);
        String updated = String.join("\n", lines);
        if (!ProjectToml.parse(updated).version().equals(version)) {
          throw cannotEdit("app.version");
        }
        return updated;
      }
    }
    throw cannotEdit("app.version");
  }

  /**
   * Returns the text of a {@code project.toml} with {@code app.build} raised by one.
   *
   * @param text The current file contents.
   * @return The new file contents, the same as {@code text} apart from the build number.
   * @throws GradleException if the file is invalid, the build number is already the highest allowed, or the number is
   *     written in a way this task cannot edit safely (such as a quoted key), in which case it should be changed by
   *     hand.
   */
  static String bump(String text) {
    int current = ProjectToml.parse(text).build();
    if (current >= ProjectToml.MAX_BUILD) {
      throw new GradleException("app.build is already " + current + ", the highest build number Android accepts.");
    }
    int next = current + 1;
    String[] lines = text.split("\n", -1);

    String table = "";
    int appHeader = -1;
    int versionLine = -1;
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i];
      String stripped = line.strip();
      if (stripped.startsWith("[")) {
        table = tableName(stripped);
        if (table.equals("app")) {
          appHeader = i;
        }
        continue;
      }
      Matcher matcher = null;
      if (table.equals("app")) {
        matcher = IN_TABLE.matcher(line);
        if (VERSION_LINE.matcher(line).matches()) {
          versionLine = i;
        }
      } else if (table.isEmpty()) {
        matcher = DOTTED.matcher(line);
      }
      if (matcher != null && matcher.matches()) {
        if (Integer.parseInt(matcher.group(2).replace("_", "")) != current) {
          throw cannotEdit("app.build");
        }
        lines[i] = matcher.group(1) + next + matcher.group(3);
        return check(String.join("\n", lines), next);
      }
    }

    // There was no build number yet, so one is added to the [app] table.
    if (appHeader < 0 || current != ProjectToml.DEFAULT_BUILD) {
      throw cannotEdit("app.build");
    }
    int after = versionLine >= 0 ? versionLine : appHeader;
    String indent = "";
    if (after == versionLine) {
      indent = lines[after].substring(0, lines[after].length() - lines[after].stripLeading().length());
    }
    // Every line but the last still ends with its own '\r' in a Windows file, so the new line gets one too.
    boolean crlf = text.contains("\r\n");
    String added = indent + "build = " + next;
    if (after == lines.length - 1) {
      if (crlf) {
        lines[after] += "\r";
      }
    } else if (crlf) {
      added += "\r";
    }
    String[] out = new String[lines.length + 1];
    System.arraycopy(lines, 0, out, 0, after + 1);
    out[after + 1] = added;
    System.arraycopy(lines, after + 1, out, after + 2, lines.length - after - 1);
    return check(String.join("\n", out), next);
  }

  private static String tableName(String header) {
    if (header.startsWith("[[")) {
      // An array of tables, such as [[installed]], is never the [app] table.
      return "[[";
    }
    int end = header.indexOf(']');
    return end < 0 ? "[" : header.substring(1, end).strip();
  }

  /** Makes sure the edit really changed the build number and nothing else broke. */
  private static String check(String text, int expected) {
    if (ProjectToml.parse(text).build() != expected) {
      throw cannotEdit("app.build");
    }
    return text;
  }

  private static GradleException cannotEdit(String key) {
    return new GradleException("Could not change " + key + " in " + ProjectPlugin.FILE_NAME + " automatically, because"
        + " it is written in a way this task does not edit (such as a quoted key or an inline table). Change it by"
        + " hand.");
  }

  /**
   * The {@code project.toml} to edit.
   *
   * @return The project file property.
   */
  @Internal
  public abstract RegularFileProperty getProjectFile();

  /**
   * The version to set instead of raising the last number of the current one, given on the command line as
   * {@code --new-version=1.0.0}.
   *
   * @return The new version property, empty unless it was given.
   */
  @Internal
  @Option(option = "new-version", description = "The new app.version, instead of raising its last number by one.")
  public abstract Property<String> getNewVersion();
}
