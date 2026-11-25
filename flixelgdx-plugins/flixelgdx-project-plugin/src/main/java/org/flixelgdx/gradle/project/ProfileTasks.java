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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Registers the tasks each profile gets in each platform module, such as {@code :desktop:demo} and
 * {@code :desktop:demoPackage}.
 *
 * <p>The task named after the profile starts the game the way the module normally does ({@code run} on desktop and
 * the web, {@code installDebug} on Android), with that profile applied. The packaging tasks add {@code Package} (and
 * {@code Bundle} on Android) to the name and run the module's packaging task ({@code packageAll} on desktop,
 * {@code package} on the web, {@code assembleRelease} and {@code bundleRelease} on Android), for builds such as a
 * demo. The runtime mode comes only from the profile's
 * {@code app.mode}, never from the task, so {@code :desktop:playtest} starts in whatever mode the {@code playtest}
 * profile asks for.
 *
 * <p>A profile is chosen once for a whole Gradle run, because it decides which modules are included and what the
 * shared {@code Project} class contains. So a profile task cannot switch profiles by itself. Instead, the settings
 * plugin looks at the task names that were asked for before anything else runs: when one of them is {@code demo}, the
 * {@code demo} profile is selected for the run, and the {@code demo} task then simply runs {@code run}. Typing
 * {@code ./gradlew :desktop:demo} is the same as {@code ./gradlew :desktop:run -Pflixel.profile=demo}.
 *
 * <p>Each profile task first runs a small check task. Gradle accepts shortened task names (such as {@code dem} for
 * {@code demo}), and the settings plugin only sees the text that was typed, so the check fails the build before the
 * game starts when the wrong profile is active.
 */
final class ProfileTasks {

  /** The group the profile tasks are listed under. */
  static final String GROUP = "profiles";

  private static final Logger LOGGER = Logging.getLogger(ProfileTasks.class);

  /**
   * The tasks each platform module gets per profile: the one that starts the game (named after the profile) and the
   * ones that package it (named after the profile plus {@code Package} or {@code Bundle}).
   */
  private static final Map<String, List<Kind>> BASES = Map.of(
      "desktop", List.of(new Kind("run", "", false), new Kind("packageAll", "Package", true)),
      "html5", List.of(new Kind("run", "", false), new Kind("package", "Package", true)),
      "android", List.of(new Kind("installDebug", "", false), new Kind("assembleRelease", "Package", true),
          new Kind("bundleRelease", "Bundle", true)));

  /** The endings a packaging task adds to the profile name. */
  private static final List<String> ENDINGS = List.of("Package", "Bundle");

  /**
   * Names a profile task cannot have, because Gradle, the framework's plugins or most builds already use them, and a
   * profile with one of them would otherwise be selected by running that ordinary task.
   */
  private static final Set<String> RESERVED = Set.of(
      "assemble", "build", "check", "clean", "test", "run", "debug", "install", "package", "packageall", "jar",
      "javadoc", "classes", "help", "tasks", "projects", "properties", "dependencies", "dependencyinsight",
      "buildenvironment", "init", "wrapper", "components", "model", "outgoingvariants", "resolvableconfigurations",
      "javatoolchains", "doctor", "bumpbuild", "game", "lint");

  private ProfileTasks() {}

  /**
   * Finds the profile that the requested task names ask for.
   *
   * @param taskNames The task names given to Gradle, such as {@code :desktop:demo}.
   * @param profiles The profile names of the file.
   * @return The profile one of the names asks for, or {@code null} when none does.
   * @throws GradleException if the names ask for two different profiles.
   */
  @Nullable
  static String requested(List<String> taskNames, List<String> profiles) {
    List<String> names = taskProfiles(profiles, true);
    String found = null;
    String foundTask = null;
    for (String path : taskNames) {
      String profile = profileOf(path.substring(path.lastIndexOf(':') + 1), names);
      if (profile == null) {
        continue;
      }
      if (found != null && !found.equals(profile)) {
        throw new GradleException("The tasks " + foundTask + " and " + path + " need different profiles ('" + found
            + "' and '" + profile + "'), but one Gradle run can only use one profile. Run them one after the other.");
      }
      found = profile;
      foundTask = path;
    }
    return found;
  }

  /**
   * Fails with a clear message when a requested profile task belongs to a module the selected profile leaves out.
   *
   * <p>A profile can change {@code platforms}, so {@code :android:demo} exists in a normal run but not in a run with
   * the {@code demo} profile when that profile drops Android. Gradle would only say that the project was not found.
   *
   * @param taskNames The task names given to Gradle.
   * @param profile The selected profile.
   * @param platforms The platforms of the selected profile.
   * @throws GradleException if a requested profile task belongs to a platform the selected profile leaves out.
   */
  static void checkModules(List<String> taskNames, String profile, List<String> platforms) {
    if (profile.isEmpty()) {
      return;
    }
    for (String path : taskNames) {
      int colon = path.lastIndexOf(':');
      if (colon <= 0 || !profile.equals(profileOf(path.substring(colon + 1), List.of(profile)))) {
        continue;
      }
      String module = path.substring(path.startsWith(":") ? 1 : 0, colon);
      if (BASES.containsKey(module) && !platforms.contains(module)) {
        throw new GradleException("The task " + path + " belongs to the " + module + " module, but the '" + profile
            + "' profile does not include it (its platforms are " + String.join(", ", platforms) + "). Add \""
            + module + "\" to its platforms, or run the task of a module it includes.");
      }
    }
  }

  /**
   * Registers the profile tasks of one platform module.
   *
   * @param project The platform module, already configured.
   * @param profiles The profile names of the file.
   * @param active The profile selected for this run, or an empty string.
   * @param mode The runtime mode the selected profile starts the game in, or an empty string.
   */
  static void register(Project project, List<String> profiles, String active, String mode) {
    List<Kind> kinds = BASES.get(project.getName());
    if (kinds == null || !project.getPath().equals(":" + project.getName())) {
      return;
    }
    for (String profile : taskProfiles(profiles, false)) {
      for (Kind kind : kinds) {
        if (!project.getTasks().getNames().contains(kind.task())) {
          continue;
        }
        String name = profile + kind.ending();
        if (project.getTasks().getNames().contains(name)) {
          LOGGER.warn("The project {} already has a task named '{}', so the '{}' profile gets no such task there. "
              + "Run {} with -P{}={} instead.", project.getPath(), name, profile, kind.task(),
              ProjectPlugin.PROFILE_PROPERTY, profile);
          continue;
        }
        String example = project.getPath() + ":" + name;
        TaskProvider<ProfileCheckTask> check = project.getTasks().register("flixelProfileCheck_" + name,
            ProfileCheckTask.class, task -> {
              task.setDescription("Checks that the '" + profile + "' profile is the one this run uses.");
              task.getProfile().set(profile);
              task.getActive().set(active);
              task.getExample().set(example);
              task.getMode().set(mode);
              task.getPackaging().set(kind.packaging());
            });
        // The check runs first whenever both are part of a run, so the task never starts with the wrong profile.
        project.getTasks().named(kind.task()).configure(task -> task.mustRunAfter(check));
        project.getTasks().register(name, task -> {
          task.setGroup(GROUP);
          task.setDescription((kind.packaging() ? "Packages" : "Starts") + " the game (" + kind.task()
              + ") with the '" + profile + "' profile from project.toml.");
          task.dependsOn(check, kind.task());
        });
      }
    }
  }

  /**
   * Finds the profile a task name belongs to: the profile itself (such as {@code demo}) or one of its packaging tasks
   * (such as {@code demoPackage}).
   *
   * @param task The task name, without a module path.
   * @param names The profiles that get tasks.
   * @return The profile, or {@code null} when the name is not a profile task. A profile whose own name matches wins
   *     over one whose packaging task does.
   */
  @Nullable
  static String profileOf(String task, List<String> names) {
    if (names.contains(task)) {
      return task;
    }
    for (String ending : ENDINGS) {
      if (task.endsWith(ending) && names.contains(task.substring(0, task.length() - ending.length()))) {
        return task.substring(0, task.length() - ending.length());
      }
    }
    return null;
  }

  /**
   * Picks the profiles that can have a task: their name has to be a plain task name and not one that is already
   * taken, such as {@code build}.
   *
   * @param profiles The profile names.
   * @param warn Whether to log why a profile gets no task. The settings plugin asks once per build, so the warning
   *     is not repeated for every module.
   * @return The profiles that get a task, in the order of the file.
   */
  static List<String> taskProfiles(List<String> profiles, boolean warn) {
    List<String> out = new ArrayList<>();
    for (String profile : profiles) {
      if (!isPlainName(profile)) {
        if (warn) {
          LOGGER.warn("The profile '{}' in {} gets no task, because a task name can only use letters, digits, '_' and "
              + "'-'. Select it with -P{}={} instead.", profile, ProjectPlugin.FILE_NAME,
              ProjectPlugin.PROFILE_PROPERTY, profile);
        }
        continue;
      }
      if (RESERVED.contains(profile.toLowerCase(Locale.ROOT))) {
        if (warn) {
          LOGGER.warn("The profile '{}' in {} gets no task, because '{}' is already the name of a common task. Rename "
              + "the profile, or select it with -P{}={}.", profile, ProjectPlugin.FILE_NAME, profile,
              ProjectPlugin.PROFILE_PROPERTY, profile);
        }
        continue;
      }
      out.add(profile);
    }
    return out;
  }

  private static boolean isPlainName(String name) {
    if (name.isEmpty()) {
      return false;
    }
    for (int i = 0; i < name.length(); i++) {
      char c = name.charAt(i);
      boolean ok = c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_' || c == '-';
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  /**
   * A task a platform module gets per profile.
   *
   * @param task The module's own task the profile task runs.
   * @param ending What the profile task adds to the profile name, empty for the task that starts the game.
   * @param packaging Whether the task packages the game, which warns when the profile is not in release mode.
   */
  private record Kind(String task, String ending, boolean packaging) {
  }
}
