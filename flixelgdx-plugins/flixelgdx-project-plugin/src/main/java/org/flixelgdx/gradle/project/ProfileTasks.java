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

import org.gradle.api.GradleException;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;
import org.gradle.api.tasks.TaskProvider;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Registers a task per profile for the usual run and package tasks, such as {@code :desktop:runDev}.
 *
 * <p>A profile is chosen once for a whole Gradle run, because it decides which modules are included and what the
 * shared {@code Project} class contains. So a profile task cannot switch profiles by itself. Instead, the settings
 * plugin looks at the task names that were asked for before anything else runs: when one of them is {@code runDev},
 * the {@code dev} profile is selected for the run, and {@code runDev} then simply runs {@code run}. Typing
 * {@code ./gradlew :desktop:runDev} is the same as {@code ./gradlew :desktop:run -Pflixel.profile=dev}.
 *
 * <p>Each profile task first runs a small check task. Gradle accepts shortened task names (such as {@code runD} for
 * {@code runDev}), and the settings plugin only sees the text that was typed, so the check fails the build before the
 * real task starts when the wrong profile is active.
 */
final class ProfileTasks {

  /** The group the profile tasks are listed under. */
  static final String GROUP = "profiles";

  private static final Logger LOGGER = Logging.getLogger(ProfileTasks.class);

  /** The tasks that get a profile version, per platform module. */
  private static final Map<String, List<Base>> BASES = Map.of(
      "desktop", List.of(new Base("run", "run"), new Base("debug", "debug"), new Base("packageAll", "packageAll")),
      "html5", List.of(new Base("run", "run"), new Base("debug", "debug"), new Base("package", "package")),
      "android", List.of(new Base("installDebug", "install"), new Base("assembleRelease", "assemble"),
          new Base("bundleRelease", "bundle")));

  private ProfileTasks() {}

  /**
   * Finds the profile that the requested task names ask for.
   *
   * @param taskNames The task names given to Gradle, such as {@code :desktop:runDev}.
   * @param profiles The profile names of the file.
   * @return The profile one of the names asks for, or {@code null} when none does.
   * @throws GradleException if the names ask for two different profiles.
   */
  @Nullable
  static String requested(List<String> taskNames, List<String> profiles) {
    Map<String, String> bySuffix = suffixes(profiles);
    String found = null;
    String foundTask = null;
    for (String path : taskNames) {
      String task = path.substring(path.lastIndexOf(':') + 1);
      for (Map.Entry<String, String> entry : bySuffix.entrySet()) {
        if (!isProfileTask(task, entry.getKey())) {
          continue;
        }
        if (found != null && !found.equals(entry.getValue())) {
          throw new GradleException("The tasks " + foundTask + " and " + path + " need different profiles ('"
              + found + "' and '" + entry.getValue() + "'), but one Gradle run can only use one profile. Run them "
              + "one after the other.");
        }
        found = entry.getValue();
        foundTask = path;
      }
    }
    return found;
  }

  /**
   * Fails with a clear message when a requested profile task belongs to a module the selected profile leaves out.
   *
   * <p>A profile can change {@code platforms}, so {@code :android:installDemo} exists in a normal run but not in a run
   * with the {@code demo} profile when that profile drops Android. Gradle would only say that the project was not
   * found.
   *
   * @param taskNames The task names given to Gradle.
   * @param profiles The profile names of the file.
   * @param profile The selected profile.
   * @param platforms The platforms of the selected profile.
   * @throws GradleException if a requested profile task belongs to a platform the selected profile leaves out.
   */
  static void checkModules(List<String> taskNames, List<String> profiles, String profile, List<String> platforms) {
    if (profile.isEmpty()) {
      return;
    }
    Map<String, String> bySuffix = suffixes(profiles);
    for (String path : taskNames) {
      int colon = path.lastIndexOf(':');
      if (colon <= 0) {
        continue;
      }
      String module = path.substring(path.startsWith(":") ? 1 : 0, colon);
      String task = path.substring(colon + 1);
      if (!BASES.containsKey(module) || platforms.contains(module)) {
        continue;
      }
      for (String suffix : bySuffix.keySet()) {
        if (isProfileTask(task, suffix)) {
          throw new GradleException("The task " + path + " belongs to the " + module + " module, but the '" + profile
              + "' profile does not include it (its platforms are " + String.join(", ", platforms) + "). Add \""
              + module
              + "\" to its platforms, or run a task of a module it includes.");
        }
      }
    }
  }

  /**
   * Registers the profile tasks of one platform module, for every base task it has.
   *
   * @param project The platform module, already configured.
   * @param profiles The profile names of the file.
   * @param active The profile selected for this run, or an empty string.
   */
  static void register(Project project, List<String> profiles, String active) {
    List<Base> bases = BASES.get(project.getName());
    if (bases == null || !project.getPath().equals(":" + project.getName())) {
      return;
    }
    for (Map.Entry<String, String> entry : suffixes(profiles).entrySet()) {
      String suffix = entry.getKey();
      String profile = entry.getValue();
      TaskProvider<ProfileCheckTask> check = null;
      for (Base base : bases) {
        if (!project.getTasks().getNames().contains(base.task())) {
          continue;
        }
        String name = base.prefix() + suffix;
        if (project.getTasks().getNames().contains(name)) {
          LOGGER.info("The project {} already has a task named '{}', so the FlixelGDX one is not added.",
              project.getPath(), name);
          continue;
        }
        if (check == null) {
          check = project.getTasks().register("flixelCheck" + suffix + "Profile", ProfileCheckTask.class, task -> {
            task.setDescription("Checks that the '" + profile + "' profile is the one this run uses.");
            task.getProfile().set(profile);
            task.getActive().set(active);
            task.getExample().set(project.getPath() + ":" + base.prefix() + suffix);
          });
        }
        TaskProvider<ProfileCheckTask> checkTask = check;
        // The check runs first whenever both are part of a run, so the real task never starts with the wrong profile.
        project.getTasks().named(base.task()).configure(task -> task.mustRunAfter(checkTask));
        project.getTasks().register(name, task -> {
          task.setGroup(GROUP);
          task.setDescription("Runs " + base.task() + " with the '" + profile + "' profile from project.toml.");
          task.dependsOn(checkTask, base.task());
        });
      }
    }
  }

  /**
   * Turns profile names into task name suffixes, such as {@code dev} into {@code Dev}.
   *
   * <p>Two profiles that turn into the same suffix get no tasks, with a warning, because a task name could not tell
   * them apart.
   *
   * @param profiles The profile names.
   * @return Each suffix with its profile name, in the order of the file.
   */
  static Map<String, String> suffixes(List<String> profiles) {
    Map<String, String> out = new LinkedHashMap<>();
    List<String> clashing = new ArrayList<>();
    for (String profile : profiles) {
      String suffix = suffix(profile);
      if (suffix == null) {
        LOGGER.warn("The profile '{}' in {} has no letter or digit, so it gets no profile tasks. Select it with "
            + "-P{}={} instead.", profile, ProjectPlugin.FILE_NAME, ProjectPlugin.PROFILE_PROPERTY, profile);
        continue;
      }
      String other = out.put(suffix, profile);
      if (other != null) {
        clashing.add(suffix);
        LOGGER.warn("The profiles '{}' and '{}' in {} would both get tasks ending in '{}', so neither gets profile "
            + "tasks. Rename one of them, or select them with -P{}.", other, profile, ProjectPlugin.FILE_NAME,
            suffix, ProjectPlugin.PROFILE_PROPERTY);
      }
    }
    for (String suffix : clashing) {
      out.remove(suffix);
    }
    return out;
  }

  /**
   * Turns a profile name into a PascalCase task name suffix.
   *
   * @param profile The profile name, such as {@code steam-demo}.
   * @return The suffix, such as {@code SteamDemo}, or {@code null} if the name has no ASCII letter or digit.
   */
  @Nullable
  static String suffix(String profile) {
    StringBuilder out = new StringBuilder();
    boolean wordStart = true;
    for (int i = 0; i < profile.length(); i++) {
      char c = profile.charAt(i);
      boolean letter = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z';
      boolean digit = c >= '0' && c <= '9';
      if (!letter && !digit) {
        wordStart = true;
        continue;
      }
      out.append(wordStart ? Character.toUpperCase(c) : c);
      wordStart = false;
    }
    return out.isEmpty() ? null : out.toString();
  }

  private static boolean isProfileTask(String task, String suffix) {
    if (!task.endsWith(suffix)) {
      return false;
    }
    String prefix = task.substring(0, task.length() - suffix.length());
    for (List<Base> bases : BASES.values()) {
      for (Base base : bases) {
        if (base.prefix().equals(prefix)) {
          return true;
        }
      }
    }
    return false;
  }

  /** A task that gets a profile version, and the start of that version's name. */
  private record Base(String task, String prefix) {
  }
}
