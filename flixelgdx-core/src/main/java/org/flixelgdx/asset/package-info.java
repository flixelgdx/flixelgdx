/**
 * Asset loading and lifecycle for FlixelGDX.
 *
 * <p><b>{@link FlixelAssetManager}</b>: Centralized asset system. It wraps a libGDX
 * {@link com.badlogic.gdx.assets.AssetManager} as a loading engine and maintains a single
 * {@code ObjectMap<String, FlixelAsset<?>>} cache as the source of truth at runtime.
 * Game code accesses it via {@link org.flixelgdx.Flixel#assets Flixel.assets}.
 *
 * <p><b>{@link FlixelAsset}</b>: Unified handle for any asset with reference counting and
 * lifecycle policy. {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} implements
 * {@code FlixelAsset<FlixelGraphic>} directly; other types (text, audio sources) use the
 * generic {@link FlixelDefaultAsset}.
 *
 * <p><b>{@link FlixelAssetLoader}</b>: Functional interface for registering custom asset types.
 * One loader per file extension; register with
 * {@link FlixelAssetManager#registerLoader(String, Class, FlixelAssetLoader)}.
 *
 * <p><b>Basic workflow:</b>
 *
 * <pre>{@code
 * // Loading state
 * Flixel.assets.load("images/player.png");
 * while (!Flixel.assets.update()) { ... }
 *
 * // Game state
 * FlixelAsset<FlixelGraphic> asset = Flixel.assets.get("images/player.png");
 * asset.retain();
 * sprite.loadGraphic(asset.get());
 *
 * // destroy()
 * asset.release();
 * }</pre>
 *
 * <p><b>Custom asset types:</b>
 *
 * <pre>{@code
 * Flixel.assets.registerLoader(".cfg", String.class,
 *     (assets, path) -> new FlixelDefaultAsset<>(assets, path, String.class));
 * }</pre>
 *
 * <p><b>Experts:</b> Use {@link FlixelAssetManager#getManager()} only when you need raw
 * libGDX behavior (custom loaders, {@link com.badlogic.gdx.assets.AssetDescriptor}s, etc.).
 *
 * @see org.flixelgdx.asset.FlixelAssetManager
 * @see org.flixelgdx.asset.FlixelDefaultAssetManager
 * @see org.flixelgdx.Flixel#assets
 */
package org.flixelgdx.asset;
