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
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.jvm.toolchain.JavaLauncher;
import org.gradle.jvm.toolchain.JavaToolchainService;
import org.gradle.jvm.toolchain.JavaToolchainSpec;
import org.jetbrains.annotations.Nullable;
import org.tomlj.Toml;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Builds the lines of the {@code doctor} report, see {@link DoctorTask}.
 *
 * <p>Each check adds one line that starts with {@code [ok]} when it passed, {@code [!!]} when it found a problem (with
 * how to fix it), or {@code [--]} for something worth knowing that is not a problem.
 */
final class ProjectDoctor {

  private ProjectDoctor() {}

  /**
   * Builds the report.
   *
   * @param facts What the settings plugin found while it read the file and included the modules.
   * @param root The root project, used to find the Gradle version and the {@code core} module's Java toolchain.
   * @return The lines of the report, ending with a summary line.
   */
  static List<String> report(Facts facts, Project root) {
    List<String> lines = new ArrayList<>();
    ProjectModel model = facts.model();
    lines.add("");
    if (model == null) {
      lines.add("FlixelGDX project: " + ProjectPlugin.FILE_NAME + " could not be read");
    } else {
      lines.add("FlixelGDX project: " + model.title() + " " + model.version() + " (build " + model.build() + ")");
      lines.add("Profile:  " + (model.profile().isEmpty() ? "(none)" : model.profile()));
      lines.add("Modules:  " + String.join(", ", facts.modules()));
    }
    lines.add("");

    lines.add("[ok] Gradle " + root.getGradle().getGradleVersion());
    lines.add("[ok] Gradle runs on Java " + System.getProperty("java.version") + " ("
        + System.getProperty("java.vendor") + ")");
    toolchain(root, lines);
    if (model != null && model.platforms().contains("android")) {
      lines.add(facts.androidSdk() != null ? "[ok] Android SDK at " + facts.androidSdk()
          : "[!!] Android SDK: not found, so the android module was skipped. Install Android Studio, then set sdk.dir "
              + "in local.properties next to the settings file, or set the ANDROID_HOME environment variable.");
    }
    lines.add(new File(facts.dir(), "assets").isDirectory() ? "[ok] assets folder found"
        : "[--] No assets folder next to the settings file. That is fine if the game has no files to load.");
    lines.addAll(facts.notes());
    projectFile(facts, lines);

    int problems = 0;
    for (String line : lines) {
      if (line.startsWith("[!!]")) {
        problems++;
      }
    }
    lines.add("");
    lines.add(problems == 0 ? "Everything looks good."
        : problems + (problems == 1 ? " problem" : " problems") + " found.");
    return lines;
  }

  private static void toolchain(Project root, List<String> lines) {
    Project core = root.findProject(":core");
    JavaPluginExtension java = core == null ? null : core.getExtensions().findByType(JavaPluginExtension.class);
    JavaToolchainService service = core == null ? null : core.getExtensions().findByType(JavaToolchainService.class);
    if (java == null || service == null || !java.getToolchain().getLanguageVersion().isPresent()) {
      return;
    }
    JavaToolchainSpec spec = java.getToolchain();
    String version = spec.getLanguageVersion().get().toString();
    try {
      JavaLauncher launcher = service.launcherFor(spec).get();
      lines.add("[ok] Java " + version + " toolchain at " + launcher.getMetadata().getInstallationPath());
    } catch (RuntimeException e) {
      // Gradle throws when no installed JDK matches and it is not allowed to download one.
      lines.add("[!!] Java " + version + " toolchain: not found. Install JDK " + version + " (for example Eclipse "
          + "Temurin), or let Gradle download one by adding the org.gradle.toolchains.foojay-resolver-convention "
          + "plugin to the settings file.");
    }
  }

  private static void projectFile(Facts facts, List<String> lines) {
    if (facts.problem() != null) {
      lines.add("[!!] " + ProjectPlugin.FILE_NAME + ": " + facts.problem());
      return;
    }
    ProjectModel model = facts.model();
    List<String> profiles = ProjectToml.profileNames(Toml.parse(facts.text()));
    String summary = model.defines().size() + (model.defines().size() == 1 ? " define" : " defines") + ", "
        + profiles.size() + (profiles.size() == 1 ? " profile" : " profiles")
        + (profiles.isEmpty() ? "" : " (" + String.join(", ", profiles) + ")");
    lines.add("[ok] " + ProjectPlugin.FILE_NAME + ": " + summary);
    // A mistake inside a profile only fails a build that selects it, so every profile is checked here.
    for (String profile : profiles) {
      try {
        ProjectToml.parse(facts.text(), profile);
      } catch (GradleException e) {
        lines.add("[!!] Profile '" + profile + "': " + e.getMessage());
      }
    }
  }

  /**
   * What the settings plugin found, handed to the report.
   *
   * @param dir The folder of the settings file.
   * @param text The contents of {@code project.toml}, or {@code null} when it is missing.
   * @param model The checked values, or {@code null} when the file could not be read.
   * @param problem Why the file could not be read, or {@code null} when it was read.
   * @param modules The modules that were included.
   * @param notes Report lines about modules that could not be included.
   * @param androidSdk The Android SDK folder that was found, or {@code null}.
   */
  record Facts(File dir, @Nullable String text, @Nullable ProjectModel model, @Nullable String problem,
      List<String> modules, List<String> notes, @Nullable String androidSdk) {
  }
}
