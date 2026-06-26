/**
 * Graphics primitives for FlixelGDX.
 *
 * <p>This package contains the lightweight wrappers and helpers used for rendering sprites and
 * working with textures in a Flixel style.
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} - Pooled wrapper around a texture
 *       asset key with reference counting and persistence policy.</li>
 *   <li>{@link org.flixelgdx.graphics.FlixelFrame FlixelFrame} - Frame metadata wrapper around a
 *       {@code TextureRegion}, used for sprite sheets and atlas like behavior.</li>
 *   <li>{@link org.flixelgdx.asset.FlixelAssetLoader FlixelAssetLoader} - Functional interface for
 *       registering custom asset loaders per file extension.</li>
 * </ul>
 *
 * <p>Textures are loaded through {@link org.flixelgdx.Flixel#assets Flixel.assets} and should be
 * preloaded in a loading state to avoid blocking the main thread, which is what the game loop runs on.
 */
package org.flixelgdx.graphics;
