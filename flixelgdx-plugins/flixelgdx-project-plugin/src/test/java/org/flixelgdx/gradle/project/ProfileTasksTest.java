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
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileTasksTest {

  private static final List<String> PROFILES = List.of("playtest", "demo", "steam-demo");

  @Test
  void onlyPlainUnusedNamesGetATask() {
    List<String> names = ProfileTasks.taskProfiles(
        List.of("playtest", "steam-demo", "v2_beta", "build", "Clean", "with space", "a.b", "doctor"), false);

    assertEquals(List.of("playtest", "steam-demo", "v2_beta"), names);
  }

  @Test
  void theRequestedTaskPicksTheProfile() {
    assertEquals("playtest", ProfileTasks.requested(List.of(":desktop:playtest"), PROFILES));
    assertEquals("demo", ProfileTasks.requested(List.of("demo"), PROFILES));
    assertEquals("steam-demo", ProfileTasks.requested(List.of(":android:steam-demo"), PROFILES));
    assertEquals("demo", ProfileTasks.requested(List.of("clean", ":html5:demo", ":desktop:demo"), PROFILES));
  }

  @Test
  void otherTasksPickNoProfile() {
    assertNull(ProfileTasks.requested(List.of(":desktop:run", "build", "--tests", "demo2"), PROFILES));
    assertNull(ProfileTasks.requested(List.of("runDemo", "demoRun"), PROFILES));
    assertNull(ProfileTasks.requested(List.of(), PROFILES));
    assertNull(ProfileTasks.requested(List.of("build"), List.of("build")));
  }

  @Test
  void twoProfilesInOneRunFail() {
    String message = assertThrows(GradleException.class,
        () -> ProfileTasks.requested(List.of(":desktop:playtest", ":desktop:demo"), PROFILES)).getMessage();

    assertTrue(message.contains("'playtest'") && message.contains("'demo'"), message);
  }

  @Test
  void aTaskOfALeftOutPlatformSaysSo() {
    String message = assertThrows(GradleException.class, () -> ProfileTasks.checkModules(
        List.of(":android:demo"), "demo", List.of("desktop", "html5"))).getMessage();

    assertTrue(message.contains(":android:demo") && message.contains("'demo'"), message);

    ProfileTasks.checkModules(List.of(":desktop:demo", ":android:installDebug"), "demo", List.of("desktop"));
  }
}
