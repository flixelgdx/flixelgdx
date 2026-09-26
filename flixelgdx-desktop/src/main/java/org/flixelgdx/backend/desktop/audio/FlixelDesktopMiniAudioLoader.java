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
package org.flixelgdx.backend.desktop.audio;

import org.flixelgdx.Flixel;
import org.flixelgdx.backend.miniaudio.FlixelLibraryLoader;
import org.flixelgdx.backend.miniaudio.FlixelMiniAudioFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Extracts the bundled miniaudio native library to a temp file and loads it.
 *
 * <p>The desktop module ships prebuilt platform binaries as classpath resources under
 * {@code org/flixelgdx/natives/}. This loader picks the right subdirectory for the
 * running OS and CPU, copies the binary to a temp file, and calls {@code System.load}
 * so the JVM can find and bind the JNI symbols at runtime.
 *
 * <p>Register this loader once before calling {@link FlixelMiniAudioFactory#create()}:
 * <pre>{@code
 * FlixelMiniAudio.setLoader(new FlixelDesktopMiniAudioLoader());
 * }</pre>
 */
public class FlixelDesktopMiniAudioLoader implements FlixelLibraryLoader {

  /** Resource root holding the platform-specific subdirectories. */
  private static final String NATIVES_ROOT = "/org/flixelgdx/natives/";

  /** {@inheritDoc} */
  @Override
  public void load() throws IOException {
    String os = System.getProperty("os.name", "").toLowerCase();
    String arch = System.getProperty("os.arch", "").toLowerCase();
    String subdir;
    String libName;
    if (os.contains("win")) {
      boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
      subdir = arm64 ? "windows-arm64" : "windows-x86_64";
      libName = "flixel_miniaudio.dll";
    } else if (os.contains("mac") || os.contains("darwin")) {
      subdir = "macos";
      libName = "libflixel_miniaudio.dylib";
    } else {
      boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
      subdir = arm64 ? "linux-arm64" : "linux-x86_64";
      libName = "libflixel_miniaudio.so";
    }
    String resource = NATIVES_ROOT + subdir + "/" + libName;
    try (InputStream in = FlixelDesktopMiniAudioLoader.class.getResourceAsStream(resource)) {
      if (in == null) {
        throw new IOException("Bundled miniaudio native not found at '" + resource + "'.");
      }
      String suffix = libName.substring(libName.lastIndexOf('.'));
      Path temp = Files.createTempFile("flixel_miniaudio", suffix);
      temp.toFile().deleteOnExit();
      Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
      // Load by absolute path: System.load, not System.loadLibrary (the latter resolves a bare
      // library name against java.library.path and would never find the extracted temp file).
      System.load(temp.toAbsolutePath().toString());
      Flixel.info("Audio", "Loaded miniaudio native from '" + resource + "'.");
    }
  }
}
