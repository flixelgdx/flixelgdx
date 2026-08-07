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
package org.flixelgdx.graphics;

import org.flixelgdx.backend.FlixelWindow;

/**
 * A single video mode a monitor can present: a resolution, refresh rate, and color depth.
 *
 * <p>Use these to build a resolution picker in a settings menu. Query the list from
 * {@link FlixelGraphicsManager#getDisplayModes() Flixel.graphics.getDisplayModes()}, let the
 * player choose one, then hand it to {@link FlixelWindow#setFullscreen(FlixelDisplayMode)}.
 *
 * <p>Display modes are a desktop concept. On platforms that do not expose them (web, mobile), the
 * mode list is empty and the current mode simply reflects the surface the game is drawn to.
 *
 * <p>Example:
 *
 * <pre>{@code
 * for (FlixelDisplayMode mode : Flixel.graphics.getDisplayModes()) {
 *   Flixel.info(mode.width() + "x" + mode.height() + " @ " + mode.refreshRate() + "Hz");
 * }
 * }</pre>
 *
 * @param width Horizontal resolution in physical pixels.
 * @param height Vertical resolution in physical pixels.
 * @param refreshRate Refresh rate in hertz, or {@code 0} when the backend cannot report it.
 * @param bitsPerPixel Color depth in bits per pixel, or {@code 0} when unknown.
 * @see FlixelGraphicsManager#getDisplayModes()
 */
public record FlixelDisplayMode(int width, int height, int refreshRate, int bitsPerPixel) {
}
