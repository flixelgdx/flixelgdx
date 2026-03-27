/**
 * Graphics primitives for FlixelGDX.
 *
 * <p>This package contains the lightweight wrappers and helpers used for rendering sprites and
 * working with textures in a Flixel style.
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic} - Pooled wrapper around a texture
 *       asset key with reference counting and persistence policy.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.graphics.FlixelFrame} - Frame metadata wrapper around a
 *       {@code TextureRegion}, used for sprite sheets and atlas like behavior.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.graphics.FlixelGraphicSource} - Source object that provides
 *       consistent loading and wrapper access for one graphic key.</li>
 * </ul>
 *
 * <p>Textures are loaded through {@link me.stringdotjar.flixelgdx.Flixel#assets} and should be
 * preloaded in a loading state to avoid blocking the main thread, which is what the game loop runs on.
 */
package me.stringdotjar.flixelgdx.graphics;
