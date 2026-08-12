/**
 * Miscellaneous utilities used throughout the FlixelGDX framework and game code.
 *
 * <p>This package groups small, broadly applicable helpers that do not belong to a more specific
 * sub-system. Higher-level entry points are exposed through {@link org.flixelgdx.Flixel Flixel}
 * and friends; the types here are the building blocks those APIs use internally.
 *
 * <h2>Key types</h2>
 * <ul>
 *   <li>{@link org.flixelgdx.util.FlixelColor FlixelColor} - a mutable RGBA color with float
 *       components and a library of named presets (WHITE, RED, CYAN, and more). Pass one
 *       anywhere a tint or background color is expected.</li>
 *   <li>{@link org.flixelgdx.util.FlixelString FlixelString} - a mutable, resizable character
 *       buffer that builds strings without heap allocation. Use it for score displays, HUD
 *       labels, or any string built every frame.</li>
 *   <li>{@link org.flixelgdx.util.FlixelBlendMode FlixelBlendMode} - enum of sprite blend modes
 *       (ADD, MULTIPLY, SCREEN, and so on). Note that switching blend mode mid-batch flushes
 *       the GPU, so group sprites by blend mode where possible.</li>
 *   <li>{@link org.flixelgdx.util.FlixelAlign FlixelAlign} - horizontal and vertical alignment
 *       constants used by text, bars, and layout helpers.</li>
 *   <li>{@link org.flixelgdx.util.FlixelDirectionFlags FlixelDirectionFlags} - bitfield constants
 *       for directional state (UP, DOWN, LEFT, RIGHT) used in collision response.</li>
 *   <li>{@link org.flixelgdx.util.FlixelAxes FlixelAxes} - axis flags (X, Y, XY) used by
 *       tweens, shake, and physics helpers.</li>
 *   <li>{@link org.flixelgdx.util.FlixelShader FlixelShader} - base class for custom GLSL
 *       shaders applied to sprites or render targets.</li>
 * </ul>
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@link org.flixelgdx.util.signal signal} - multicast event signals for framework hooks
 *       and game events.</li>
 *   <li>{@link org.flixelgdx.util.timer timer} - frame-based timers for delayed and repeating
 *       callbacks.</li>
 *   <li>{@link org.flixelgdx.util.save save} - JSON-backed persistent save data.</li>
 * </ul>
 *
 * @see org.flixelgdx.util.FlixelColor
 * @see org.flixelgdx.util.FlixelString
 * @see org.flixelgdx.util.signal.FlixelSignal
 * @see org.flixelgdx.util.timer.FlixelTimer
 * @see org.flixelgdx.util.save.FlixelSave
 */
package org.flixelgdx.util;
