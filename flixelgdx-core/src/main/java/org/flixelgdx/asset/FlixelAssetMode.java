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
package org.flixelgdx.asset;

/**
 * Controls when the asset manager reclaims memory for non-persistent assets.
 *
 * <p>The active mode is read by {@link org.flixelgdx.Flixel#switchState Flixel.switchState} and by individual asset
 * handles on every {@link FlixelAsset#release()} call, so changing the mode mid-session takes
 * effect immediately without any extra steps.
 *
 * <p>Set the mode on {@link FlixelAssetManager} via {@link FlixelAssetManager#setAssetMode(FlixelAssetMode)}.
 *
 * <h2>Choosing a mode</h2>
 * <ul>
 *   <li>{@link #LAZY} - simplest; assets accumulate. Use when memory is not a concern or when you
 *     preload everything up front and rarely switch states.</li>
 *   <li>{@link #STANDARD} - safe default; mirrors how most 2D frameworks manage assets. The right
 *     choice for most games.</li>
 *   <li>{@link #AGGRESSIVE} - tightest footprint. Use when you have many short-lived states and
 *     memory pressure is real. Read the {@link #AGGRESSIVE} docs carefully before opting in.</li>
 * </ul>
 */
public enum FlixelAssetMode {

  /**
   * Assets are never automatically unloaded. Every asset loaded into the manager stays in memory
   * for the entire session, regardless of state switches or reference counts.
   *
   * <p>Equivalent to setting {@code persist = true} globally. {@link FlixelAssetManager#clearNonPersist()}
   * is skipped entirely on state switches, so no teardown cost is paid at all.
   *
   * <p>Best for games that preload everything up front or have very few, long-lived states.
   */
  LAZY,

  /**
   * Non-persistent assets with a zero reference count are unloaded when
   * {@link org.flixelgdx.Flixel#switchState Flixel.switchState} runs. This is the default.
   *
   * <p>Persistent assets (see {@link FlixelAsset#isPersist()}) and any asset still held by a
   * live object (reference count greater than zero) are kept across the switch.
   */
  STANDARD,

  /**
   * Non-persistent assets are unloaded immediately the moment their reference count reaches zero,
   * without waiting for a state switch.
   *
   * <p>This uses O(1) work per {@link FlixelAsset#release()} call - there is no periodic scan.
   * The asset handle that just dropped to zero triggers the unload inline, then the handle is
   * evicted from the manager cache so subsequent {@code ensureTypedAsset} or {@code ensureWrapper}
   * calls start fresh.
   *
   * <p><b>Important caveats:</b>
   * <ul>
   *   <li>{@link FlixelAsset#release()} must be called on the GL/render thread. Calling it from
   *     a background thread while libGDX may be uploading or disposing GPU resources is not safe
   *     in AGGRESSIVE mode (it is fine in LAZY and STANDARD since those only unload at state
   *     switch boundaries, which is always on the GL thread).</li>
   *   <li>After an aggressive eviction, the underlying libGDX asset is disposed. Any code that
   *     still holds a raw reference to the same {@link com.badlogic.gdx.graphics.Texture} (or
   *     other resource) object will encounter a disposed object. The automated sprite pipeline
   *     ({@code loadGraphic} and {@code destroy}) handles this correctly; direct raw-asset usage
   *     outside that pipeline must ensure no other references remain before releasing.</li>
   *   <li>Re-requesting an aggressively evicted asset requires calling
   *     {@link FlixelAssetManager#load(String)} again and awaiting the async load cycle.</li>
   * </ul>
   *
   * <p>{@link org.flixelgdx.Flixel#switchState Flixel.switchState} still calls {@link FlixelAssetManager#clearNonPersist()}
   * as a safety net for assets that were loaded but never retained.
   */
  AGGRESSIVE
}
