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
package org.flixelgdx.audio;

import org.flixelgdx.collections.FlixelArray;

/**
 * Global registry that maps audio effect node type names to stable integer IDs.
 *
 * <p>The framework's built-in effect types are pre-registered at class load time, so
 * {@link #REVERB}, {@link #LOW_PASS}, and the rest are simply constants that were registered
 * first. Third-party code (plugins, game-specific effects) can extend the registry by calling
 * {@link #register(String)} once at startup:
 *
 * <pre>{@code
 * // In a plugin's init method:
 * int MY_CHORUS = FlixelAudioNodeRegistry.register("my_chorus");
 *
 * // Later, attach it to a sound:
 * FlixelSoundEffect chorus = sound.addNode(MY_CHORUS, rate, depth, mix);
 * chorus.setParam(0, 2.5f);  // rate
 * }</pre>
 *
 * <p>Backends that do not recognize a type ID from {@link FlixelSound#addNode} return
 * {@link FlixelSoundEffect#NOOP} rather than throwing, so an effect unsupported by the
 * current platform is silently ignored.
 *
 * <p>All IDs are assigned sequentially from 0. Registration is not thread-safe; call
 * {@link #register(String)} from the main thread before starting the game loop.
 */
public final class FlixelAudioNodeRegistry {

  /** Built-in ID for a low-pass filter. */
  public static final int LOW_PASS;

  /** Built-in ID for a high-pass filter. */
  public static final int HIGH_PASS;

  /** Built-in ID for a band-pass filter. */
  public static final int BAND_PASS;

  /** Built-in ID for a reverb effect. */
  public static final int REVERB;

  /** Built-in ID for a delay/echo effect. */
  public static final int DELAY;

  private static final FlixelArray<String> names = new FlixelArray<>(8);

  static {
    LOW_PASS = register("low_pass");
    HIGH_PASS = register("high_pass");
    BAND_PASS = register("band_pass");
    REVERB = register("reverb");
    DELAY = register("delay");
  }

  private FlixelAudioNodeRegistry() {}

  /**
   * Registers a new node type and returns its stable ID.
   *
   * <p>If the name was already registered, the existing ID is returned without creating a
   * duplicate. IDs are assigned sequentially starting at 0, with the framework's built-ins
   * occupying the first few slots.
   *
   * @param name A unique, lowercase identifier for the node type (e.g. {@code "my_chorus"}).
   * @return The stable integer ID for this node type.
   */
  public static int register(String name) {
    for (int i = 0; i < names.getSize(); i++) {
      if (names.get(i).equals(name)) {
        return i;
      }
    }
    names.add(name);
    return names.getSize() - 1;
  }

  /**
   * Returns the name registered for the given ID, or {@code null} when the ID is unknown.
   *
   * @param typeId The node type ID.
   * @return The registered name, or {@code null}.
   */
  public static String getName(int typeId) {
    if (typeId < 0 || typeId >= names.getSize()) {
      return null;
    }
    return names.get(typeId);
  }

  /**
   * Returns {@code true} when the given ID was registered through this registry.
   *
   * @param typeId The node type ID to check.
   * @return {@code true} if the ID is known.
   */
  public static boolean isRegistered(int typeId) {
    return typeId >= 0 && typeId < names.getSize();
  }
}
