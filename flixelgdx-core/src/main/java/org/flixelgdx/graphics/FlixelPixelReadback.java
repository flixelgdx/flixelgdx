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

import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;

/**
 * Receives the pixels of a texture that were copied back from the GPU.
 *
 * <p>Think of asking a photo lab for a print of a picture that is stored in a vault: you hand in the request, and
 * some time later the lab calls you back with the print. Reading a GPU texture works the same way. The request
 * returns at once, and this callback runs later (on the game's main thread, from inside a frame) when the data is
 * ready. On the web the answer always arrives asynchronously, and on desktop and Android it arrives within a few
 * frames.
 *
 * <p>The buffer is only valid <em>during</em> the call. Copy what you need before returning. Use
 * {@link ByteBuffer#get(int)} with absolute indexes rather than relative reads, and do not change its position or
 * limit.
 *
 * @see FlixelGraphicsManager#readPixels(FlixelTexture, int, int, int, int, FlixelPixelReadback)
 */
@FunctionalInterface
public interface FlixelPixelReadback {

  /**
   * Called once when the copy finished or failed.
   *
   * @param rgba The pixels as 8-bit RGBA, or {@code null} when the pixels could not be read (the backend cannot
   *     read this texture, it was destroyed, or the GPU reported an error).
   * @param width The width of the copied area in pixels. Meaningless when {@code rgba} is {@code null}.
   * @param height The height of the copied area in pixels. Meaningless when {@code rgba} is {@code null}.
   * @param rowBytes The distance in bytes from the start of one row to the start of the next. It is at least
   *     {@code width * 4} and can be larger, because GPUs pad rows. Meaningless when {@code rgba} is {@code null}.
   */
  void onPixels(@Nullable ByteBuffer rgba, int width, int height, int rowBytes);
}
