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
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileTasksTest {

  private static final List<String> PROFILES = List.of("dev", "demo", "steam-demo");

  @Test
  void profileNamesBecomePascalCaseSuffixes() {
    assertEquals("Dev", ProfileTasks.suffix("dev"));
    assertEquals("SteamDemo", ProfileTasks.suffix("steam-demo"));
    assertEquals("SteamDemo", ProfileTasks.suffix("steamDemo"));
    assertEquals("V2", ProfileTasks.suffix("v2"));
    assertNull(ProfileTasks.suffix("--"));
  }

  @Test
  void profilesWithTheSameSuffixGetNoTasks() {
    Map<String, String> suffixes = ProfileTasks.suffixes(List.of("dev", "steam-demo", "steam_demo"));

    assertEquals(Map.of("Dev", "dev"), suffixes);
  }

  @Test
  void theRequestedTaskPicksTheProfile() {
    assertEquals("dev", ProfileTasks.requested(List.of(":desktop:runDev"), PROFILES));
    assertEquals("demo", ProfileTasks.requested(List.of("packageAllDemo"), PROFILES));
    assertEquals("steam-demo", ProfileTasks.requested(List.of(":android:installSteamDemo"), PROFILES));
    assertEquals("dev", ProfileTasks.requested(List.of("clean", ":html5:debugDev", ":desktop:runDev"), PROFILES));
  }

  @Test
  void otherTasksPickNoProfile() {
    assertNull(ProfileTasks.requested(List.of(":desktop:run", "build", "--tests", "FooDev"), PROFILES));
    assertNull(ProfileTasks.requested(List.of("compileDev"), PROFILES));
    assertNull(ProfileTasks.requested(List.of(), PROFILES));
  }

  @Test
  void twoProfilesInOneRunFail() {
    String message = assertThrows(GradleException.class,
        () -> ProfileTasks.requested(List.of(":desktop:runDev", ":desktop:runDemo"), PROFILES)).getMessage();

    assertTrue(message.contains("'dev'") && message.contains("'demo'"), message);
  }

  @Test
  void aTaskOfALeftOutPlatformSaysSo() {
    String message = assertThrows(GradleException.class, () -> ProfileTasks.checkModules(
        List.of(":android:installDemo"), PROFILES, "demo", List.of("desktop", "html5"))).getMessage();

    assertTrue(message.contains(":android:installDemo") && message.contains("'demo'"), message);

    ProfileTasks.checkModules(List.of(":desktop:runDemo"), PROFILES, "demo", List.of("desktop", "html5"));
  }
}
