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

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * The thin JNI bridge to the bundled miniaudio native library.
 *
 * <p>miniaudio is a full audio engine (decoding, mixing, effects, spatialization) in a single C
 * header. This class holds the {@code native} methods our C wrapper implements, and loads the
 * matching platform library from the desktop module's {@code org/flixelgdx/natives} resources.
 * Libraries live in per-platform subdirectories ({@code linux-x86_64}, {@code linux-arm64},
 * {@code windows-x86_64}, {@code macos}), letting the framework carry binaries for every
 * supported target at once. The native is extracted to a temp file once and loaded, so packaged
 * games need no extra setup.
 *
 * <p>The bridge is deliberately low-level and stateless: every handle is a {@code long} pointer
 * into native memory. {@link FlixelMiniAudioFactory}, {@link FlixelMiniAudioSound}, and
 * {@link FlixelMiniAudioGroup} wrap these calls behind the framework's audio interfaces.
 *
 * <p>All calls must run on one thread; the framework only ever calls audio from the main thread.
 */
public class FlixelMiniAudio {

  /** {@code true} once {@link #ensureLoaded()} has successfully loaded the native library. */
  private static boolean loaded;

  private FlixelMiniAudio() {}

  /**
   * Extracts and loads the platform native library on first use.
   *
   * @return {@code true} when the native library is loaded and usable.
   */
  static synchronized boolean ensureLoaded() {
    if (loaded) {
      return true;
    }
    String os = System.getProperty("os.name", "").toLowerCase();
    String arch = System.getProperty("os.arch", "").toLowerCase();
    String subdir;
    String libName;
    if (os.contains("win")) {
      subdir = "windows-x86_64";
      libName = "flixel_miniaudio.dll";
    } else if (os.contains("mac") || os.contains("darwin")) {
      subdir = "macos";
      libName = "libflixel_miniaudio.dylib";
    } else {
      boolean arm64 = arch.equals("aarch64") || arch.equals("arm64");
      subdir = arm64 ? "linux-arm64" : "linux-x86_64";
      libName = "libflixel_miniaudio.so";
    }
    String resource = "/org/flixelgdx/natives/" + subdir + "/" + libName;
    try (InputStream in = FlixelMiniAudio.class.getResourceAsStream(resource)) {
      if (in == null) {
        Flixel.warn("Audio", "Bundled miniaudio native not found at '" + resource + "'; audio is disabled.");
        return false;
      }
      Path temp = Files.createTempFile("flixel_miniaudio", libName.substring(libName.lastIndexOf('.')));
      temp.toFile().deleteOnExit();
      Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
      // Load by absolute path: System.load, not System.loadLibrary (the latter resolves a bare
      // library name against java.library.path and would never find the extracted temp file).
      System.load(temp.toAbsolutePath().toString());
      loaded = true;
      return true;
    } catch (IOException | UnsatisfiedLinkError e) {
      Flixel.error("Audio", "Could not load the miniaudio native library.", e);
      return false;
    }
  }

  /**
   * Initializes the audio engine.
   *
   * @return A native engine handle, or {@code 0} on failure.
   */
  static native long engineInit();

  /**
   * Shuts the engine down and frees its resources.
   *
   * @param engine The engine handle.
   */
  static native void engineUninit(long engine);

  /**
   * Sets the engine master volume.
   *
   * @param engine The engine handle.
   * @param volume Volume in {@code [0, 1]}.
   */
  static native void engineSetVolume(long engine, float volume);

  /**
   * Creates a sound group on the engine.
   *
   * @param engine The engine handle.
   * @return A native group handle, or {@code 0} on failure.
   */
  static native long groupInit(long engine);

  /**
   * Frees a sound group.
   *
   * @param group The group handle.
   */
  static native void groupUninit(long group);

  /**
   * Pauses (stops) every sound in a group.
   *
   * @param group The group handle.
   */
  static native void groupStop(long group);

  /**
   * Resumes (starts) every sound in a group.
   *
   * @param group The group handle.
   */
  static native void groupStart(long group);

  /**
   * Returns the current volume multiplier for a sound group.
   *
   * @param group The group handle.
   * @return The current volume, where 1 is normal and 0 is silent.
   */
  static native float groupGetVolume(long group);

  /**
   * Sets the volume multiplier for a sound group.
   *
   * @param group The group handle.
   * @param volume Volume multiplier (0 = silent, 1 = normal; values above 1 amplify).
   */
  static native void groupSetVolume(long group, float volume);

  /**
   * Decodes and loads a sound from an in-memory encoded buffer.
   *
   * @param engine The engine handle.
   * @param data The encoded audio bytes (WAV, MP3, OGG, FLAC).
   * @param length The number of valid bytes in {@code data}.
   * @param group The group handle to attach to, or {@code 0} for the engine default.
   * @return A native sound handle, or {@code 0} on failure.
   */
  static native long soundLoad(long engine, byte[] data, int length, long group);

  /**
   * Frees a sound's native resources.
   *
   * @param sound The sound handle.
   */
  static native void soundUninit(long sound);

  /**
   * Starts or resumes playback.
   *
   * @param sound The sound handle.
   */
  static native void soundStart(long sound);

  /**
   * Pauses playback, keeping the cursor position.
   *
   * @param sound The sound handle.
   */
  static native void soundStop(long sound);

  /**
   * Returns {@code true} when the sound is actively playing.
   *
   * @param sound The sound handle.
   */
  static native boolean soundIsPlaying(long sound);

  /**
   * Returns {@code true} when the cursor reached the end.
   *
   * @param sound The sound handle.
   */
  static native boolean soundIsAtEnd(long sound);

  /**
   * Returns the current volume in {@code [0, 1]} (or higher).
   *
   * @param sound The sound handle.
   */
  static native float soundGetVolume(long sound);

  /**
   * Sets the sound volume.
   *
   * @param sound The sound handle.
   * @param volume Volume in {@code [0, 1]} (or higher).
   */
  static native void soundSetVolume(long sound, float volume);

  /**
   * Sets the pitch multiplier.
   *
   * @param sound The sound handle.
   * @param pitch Pitch factor; {@code 1} is unchanged.
   */
  static native void soundSetPitch(long sound, float pitch);

  /**
   * Sets the stereo pan.
   *
   * @param sound The sound handle.
   * @param pan Pan in {@code [-1, 1]}.
   */
  static native void soundSetPan(long sound, float pan);

  /**
   * Returns the current cursor position in seconds.
   *
   * @param sound The sound handle.
   */
  static native float soundGetCursor(long sound);

  /**
   * Seeks to a position.
   *
   * @param sound The sound handle.
   * @param seconds The target position in seconds.
   */
  static native void soundSeek(long sound, float seconds);

  /**
   * Returns the sound length in seconds, or {@code 0} when unknown.
   *
   * @param sound The sound handle.
   */
  static native float soundGetLength(long sound);

  /**
   * Returns {@code true} when looping is enabled.
   *
   * @param sound The sound handle.
   */
  static native boolean soundIsLooping(long sound);

  /**
   * Enables or disables looping.
   *
   * @param sound The sound handle.
   * @param looping {@code true} to loop.
   */
  static native void soundSetLooping(long sound, boolean looping);

  /**
   * Sets a spatial position for 3D audio.
   *
   * @param sound The sound handle.
   * @param x X position.
   * @param y Y position.
   * @param z Z position.
   */
  static native void soundSetPosition(long sound, float x, float y, float z);
}
