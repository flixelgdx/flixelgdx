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
package org.flixelgdx.gradle.html5;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.Copy;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExcludesTest {

  @TempDir
  Path tmp;

  private void write(String rel) throws IOException {
    Path file = tmp.resolve("assets").resolve(rel);
    Files.createDirectories(file.getParent());
    Files.writeString(file, "x");
  }

  private Path copyWith(List<String> excludes) throws IOException {
    write("keep.png");
    write("debug/log.txt");
    write("art/source.psd");
    write("music/old_track.ogg");
    write("music/new_track.ogg");

    Project project = ProjectBuilder.builder().withProjectDir(tmp.toFile()).build();
    project.getPlugins().apply("org.flixelgdx.html5");
    Html5Extension ext = project.getExtensions().getByType(Html5Extension.class);
    ext.getAssetsDir().set(new File(tmp.toFile(), "assets"));
    ext.getExcludes().set(excludes);
    ((ProjectInternal) project).evaluate();

    Path out = tmp.resolve("out");
    Copy copy = (Copy) project.getTasks().getByName("copyAssets");
    copy.setDestinationDir(out.toFile());
    copy.getActions().forEach(a -> a.execute(copy));
    return out;
  }

  @Test
  void noExcludesCopiesEverything() throws IOException {
    Path out = copyWith(List.of());
    assertTrue(Files.exists(out.resolve("debug/log.txt")));
    assertTrue(Files.exists(out.resolve("art/source.psd")));
  }

  @Test
  void excludesFoldersExtensionsAndSingleFiles() throws IOException {
    Path out = copyWith(List.of("debug/**", "**/*.psd", "music/old_track.ogg"));
    assertTrue(Files.exists(out.resolve("keep.png")));
    assertTrue(Files.exists(out.resolve("music/new_track.ogg")));
    assertFalse(Files.exists(out.resolve("debug/log.txt")));
    assertFalse(Files.exists(out.resolve("art/source.psd")));
    assertFalse(Files.exists(out.resolve("music/old_track.ogg")));
  }
}
