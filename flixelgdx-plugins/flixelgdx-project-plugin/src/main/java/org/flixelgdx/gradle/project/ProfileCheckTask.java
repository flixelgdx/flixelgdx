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
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.UntrackedTask;

/**
 * Fails the build when a profile task runs with a different profile than its own, see {@link ProfileTasks}.
 *
 * <p>This happens when a profile task is started by a shortened name, such as {@code runD} for {@code runDev}. The
 * settings plugin picks the profile from the exact task names, so it cannot see that one.
 */
@UntrackedTask(because = "It only checks which profile is active, so there is nothing to keep up to date.")
public abstract class ProfileCheckTask extends DefaultTask {

  /** Checks the active profile. */
  @TaskAction
  public void check() {
    String profile = getProfile().get();
    String active = getActive().get();
    if (!profile.equals(active)) {
      throw new GradleException("This task needs the '" + profile + "' profile, but "
          + (active.isEmpty() ? "no profile" : "the '" + active + "' profile") + " is active. Run it by its full "
          + "name, such as ./gradlew " + getExample().get() + ", so the '" + profile + "' profile is selected.");
    }
  }

  /**
   * The profile the task belongs to.
   *
   * @return The profile property.
   */
  @Input
  public abstract Property<String> getProfile();

  /**
   * The profile this run uses.
   *
   * @return The active profile property, holding an empty string when none is selected.
   */
  @Input
  public abstract Property<String> getActive();

  /**
   * A full task name to suggest in the error message.
   *
   * @return The example property.
   */
  @Input
  public abstract Property<String> getExample();
}
