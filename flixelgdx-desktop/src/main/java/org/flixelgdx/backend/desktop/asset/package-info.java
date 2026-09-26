/**
 * JVM asset manager for the desktop backend.
 *
 * <p>{@link org.flixelgdx.backend.desktop.asset.FlixelJvmAssetManager FlixelJvmAssetManager}
 * extends {@link org.flixelgdx.asset.FlixelBaseAssetManager FlixelBaseAssetManager} with
 * stage-one loads (file reads, image and audio decoding) running on a background thread pool,
 * while the main thread finishes each asset (GPU uploads, cache registration) inside
 * {@code update()}.
 */
package org.flixelgdx.backend.desktop.asset;
