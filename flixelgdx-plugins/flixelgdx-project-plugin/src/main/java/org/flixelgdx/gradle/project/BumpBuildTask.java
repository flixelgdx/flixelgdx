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
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adds one to {@code app.build} in {@code project.toml}, editing only that line.
 *
 * <p>The build number has to go up with every release (Android refuses to install an older one over a newer one),
 * and forgetting to raise it is easy. Running {@code ./gradlew bumpBuild} before a release does it for you:
 *
 * <pre>{@code
 * $ ./gradlew bumpBuild
 * project.toml: app.build 14 -> 15
 * }</pre>
 *
 * <p>Only the number changes, so comments, spacing and the order of the file stay exactly as they were. When the file
 * has no {@code app.build} yet, the build number is {@code 1}, so the task adds {@code build = 2} to the {@code [app]}
 * table, right after {@code version} when it is there.
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

  /** Raises the build number and saves the file. */
  @TaskAction
  public void bump() {
    File file = getProjectFile().get().getAsFile();
    String text;
    try {
      text = Files.readString(file.toPath(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GradleException("Could not read " + file + ".", e);
    }
    int before = ProjectToml.parse(text).build();
    String updated = bump(text);
    try {
      Files.writeString(file.toPath(), updated, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new GradleException("Could not write " + file + ".", e);
    }
    // Quiet is the level that is always shown, so the new number is visible even in quiet builds.
    getLogger().quiet("{}: app.build {} -> {}", ProjectPlugin.FILE_NAME, before, before + 1);
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
          throw cannotEdit();
        }
        lines[i] = matcher.group(1) + next + matcher.group(3);
        return check(String.join("\n", lines), next);
      }
    }

    // There was no build number yet, so one is added to the [app] table.
    if (appHeader < 0 || current != ProjectToml.DEFAULT_BUILD) {
      throw cannotEdit();
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
      throw cannotEdit();
    }
    return text;
  }

  private static GradleException cannotEdit() {
    return new GradleException("Could not raise app.build in " + ProjectPlugin.FILE_NAME + " automatically, because it"
        + " is written in a way this task does not edit (such as a quoted key or an inline table). Change it by hand.");
  }

  /**
   * The {@code project.toml} to edit.
   *
   * @return The project file property.
   */
  @Internal
  public abstract RegularFileProperty getProjectFile();
}
