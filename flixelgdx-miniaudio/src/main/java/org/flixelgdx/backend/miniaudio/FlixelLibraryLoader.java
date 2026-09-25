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
package org.flixelgdx.backend.miniaudio;

/**
 * Loads the miniaudio native library for the current platform.
 *
 * <p>Each platform backend supplies its own implementation and registers it once via
 * {@link FlixelMiniAudio#setLoader}. The desktop backend extracts the bundled library
 * to a temp file and calls {@code System.load}; the Android backend calls
 * {@code System.loadLibrary} against the packaged .so.
 *
 * <p>Implementations must be idempotent. Calling {@link #load} more than once is
 * safe, though {@link FlixelMiniAudio} will only call it once.
 *
 * <p>Example usage:
 * <pre>{@code
 * FlixelMiniAudio.setLoader(() -> System.loadLibrary("flixel_miniaudio"));
 * FlixelSoundManager.defaultFactory = FlixelMiniAudioFactory.create();
 * }</pre>
 */
@FunctionalInterface
public interface FlixelLibraryLoader {

  /**
   * Loads the native library.
   *
   * @throws Exception if the library cannot be loaded; the audio backend falls back to silent.
   */
  void load() throws Exception;
}
