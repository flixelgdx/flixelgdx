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
import org.gradle.api.provider.ListProperty;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * Prints a health report of the game's project and the computer it is built on.
 *
 * <p>Most "it does not build" problems come from the setup, not the game's code: a missing JDK, no Android SDK, or a
 * typo in {@code project.toml}. Running {@code ./gradlew doctor} checks those in one go and says how to fix each
 * problem it finds:
 *
 * <pre>{@code
 * $ ./gradlew doctor
 *
 * FlixelGDX project: My Game 0.3.0 (build 14)
 * Profile:  dev
 * Modules:  core, desktop, html5
 *
 * [ok] Gradle 9.2
 * [!!] Android SDK: not found. Install Android Studio, then set sdk.dir in local.properties or ANDROID_HOME.
 * ...
 * }</pre>
 *
 * <p>The report never fails the build, so it can always be pasted into a bug report. It is built when the task runs,
 * after every module has been configured.
 */
@UntrackedTask(because = "It only prints a report, so there is nothing to keep up to date.")
public abstract class DoctorTask extends DefaultTask {

  /** The name of the task. */
  public static final String NAME = "doctor";

  /** Prints the report. */
  @TaskAction
  public void print() {
    // Quiet is the level that is always shown, so the report is visible even in quiet builds.
    for (String line : getReport().get()) {
      getLogger().quiet(line);
    }
  }

  /**
   * The lines of the report.
   *
   * @return The report property.
   */
  @Internal
  public abstract ListProperty<String> getReport();
}
