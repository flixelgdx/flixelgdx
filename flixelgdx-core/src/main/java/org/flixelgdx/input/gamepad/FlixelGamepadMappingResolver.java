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
package org.flixelgdx.input.gamepad;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Supplies a {@link FlixelGamepadMapping} for a connected gamepad, or declines by returning
 * {@code null}.
 *
 * <p>Resolvers are registered on {@link FlixelGamepadInputManager} and consulted in priority order
 * whenever a new gamepad connects. The first resolver that returns a non-null mapping wins; the
 * rest are skipped. Returning {@code null} passes the gamepad on to the next resolver in the chain.
 * If no resolver matches, the manager installs a safe empty mapping that reports every input as
 * unavailable.
 *
 * <p>A resolver can inspect anything it needs through the {@link FlixelGamepad} it receives:
 * {@link FlixelGamepad#getVendorId()} and {@link FlixelGamepad#getProductId()} are the most
 * reliable identifiers (stable across drivers and OS versions), while {@link FlixelGamepad#getName()}
 * is a readable fallback and {@link FlixelGamepad#getNativeHandle()} provides a full escape hatch.
 *
 * <p>This interface is the extension point for both the framework and user code. The framework
 * registers its own resolvers (for example a resolver backed by the SDL3 community controller
 * database in Phase 3). User code adds resolvers for exotic or custom hardware without the
 * framework needing to know they exist:
 *
 * <pre>{@code
 * public class NesAdapterResolver implements FlixelGamepadMappingResolver {
 *   private static final int VENDOR  = 0x0079;
 *   private static final int PRODUCT = 0x0011;
 *
 *   @Override
 *   public FlixelGamepadMapping resolve(FlixelGamepad gamepad) {
 *     if (gamepad.getVendorId() == VENDOR && gamepad.getProductId() == PRODUCT) {
 *       return NES_MAPPING;
 *     }
 *     return null;
 *   }
 * }
 *
 * Flixel.gamepads.addMappingResolver(new NesAdapterResolver());
 * }</pre>
 *
 * @see FlixelGamepadInputManager#addMappingResolver(FlixelGamepadMappingResolver)
 * @see FlixelGamepadMapping
 */
public interface FlixelGamepadMappingResolver {

  /**
   * Returns a mapping for the given gamepad, or {@code null} to pass it to the next resolver.
   *
   * <p>This is called once when a gamepad connects, not every frame. The returned mapping is
   * stored on the gamepad slot for the lifetime of the connection.
   *
   * @param gamepad The gamepad that just connected; never {@code null}.
   * @return A populated {@link FlixelGamepadMapping}, or {@code null} to decline.
   */
  @Nullable
  FlixelGamepadMapping resolve(@NotNull FlixelGamepad gamepad);
}
