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

import com.badlogic.gdx.graphics.g2d.Batch;

/**
 * Extension of the libGDX {@link Batch} interface that adds render-call tracking.
 *
 * <p>All FlixelGDX batch implementations must satisfy this interface so the debug overlay can
 * aggregate render-call counts across the framework's own batch and any additional user-registered
 * batches without depending on a concrete class such as libGDX's {@code SpriteBatch}.
 *
 * <p>{@link #getRenderCalls()} reports how many {@code glDrawElements} calls the batch issued
 * since the most recent {@link #begin()}, mirroring {@code SpriteBatch.renderCalls}.
 * {@link #getTotalRenderCalls()} is the cumulative count since the batch was constructed,
 * mirroring {@code SpriteBatch.totalRenderCalls}. Both values are used by
 * {@link org.flixelgdx.debug.FlixelDebugManager} and
 * {@link org.flixelgdx.debug.FlixelDebugOverlay} to display stats in the debug panel.
 *
 * <p>The built-in implementation is {@link FlixelSpriteBatch}. Custom implementations that wrap
 * a third-party renderer only need to forward these two values from whatever the underlying
 * renderer tracks.
 */
public interface FlixelBatch extends Batch {

  /**
   * Returns the number of draw calls ({@code glDrawElements} invocations) issued by this batch
   * since the last {@link #begin()} call. This counter resets to zero at the start of each
   * frame when {@link #begin()} is invoked.
   *
   * @return Per-frame render call count.
   */
  int getRenderCalls();

  /**
   * Returns the cumulative number of draw calls issued by this batch since it was created.
   * Unlike {@link #getRenderCalls()}, this value never resets.
   *
   * @return Total render call count since construction.
   */
  int getTotalRenderCalls();
}
