/**
 * Asset loading and lifecycle for FlixelGDX.
 *
 * <p><b>{@link org.flixelgdx.asset.FlixelAssetManager FlixelAssetManager}</b>: Centralized asset
 * system, defined as a pure interface so each platform installs its own implementation (the
 * shared JVM one on desktop and Android, a browser-based one on web). It maintains a single
 * handle cache as the source of truth at runtime and loads everything through the
 * {@link org.flixelgdx.file.FlixelFiles FlixelFiles} seam. Game code accesses it via
 * {@link org.flixelgdx.Flixel#assets Flixel.assets}.
 *
 * <p><b>{@link org.flixelgdx.asset.FlixelAsset FlixelAsset}</b>: Unified handle for any asset with
 * reference counting and lifecycle policy. {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic}
 * implements {@code FlixelAsset<FlixelGraphic>} directly; other types (text, audio sources) use the
 * generic {@link org.flixelgdx.asset.FlixelDefaultAsset FlixelDefaultAsset}.
 *
 * <p><b>{@link org.flixelgdx.asset.FlixelAssetLoader FlixelAssetLoader}</b>: The two-stage loader
 * interface for registering custom asset types. One loader per file extension; register with
 * {@link org.flixelgdx.asset.FlixelAssetManager#registerLoader(String, FlixelAssetLoader) FlixelAssetManager.registerLoader(...)}.
 * Stage one may run on a worker thread on platforms that support multithreading.
 *
 * <p><b>Basic workflow:</b>
 *
 * <pre>{@code
 * // Loading state
 * Flixel.assets.load("images/player.png");
 * while (!Flixel.assets.update()) { ... }
 *
 * // Game state.
 * FlixelAsset<FlixelGraphic> asset = Flixel.assets.get("images/player.png");
 * asset.retain();
 * sprite.loadGraphic(asset.get());
 *
 * // destroy().
 * asset.release();
 * }</pre>
 *
 * @see org.flixelgdx.asset.FlixelAssetManager
 * @see org.flixelgdx.asset.FlixelBaseAssetManager
 * @see org.flixelgdx.Flixel#assets
 */
package org.flixelgdx.asset;
