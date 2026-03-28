/**
 * Asset loading and lifecycle for FlixelGDX.
 *
 * <p><b>{@link FlixelAssetManager}</b>: Centralized asset system used by FlixelGDX.
 * It wraps a libGDX {@link com.badlogic.gdx.assets.AssetManager} and provides consistent helpers
 * for loading ({@link FlixelAssetManager#load(FlixelSource)}, {@link FlixelAssetManager#load(String)},
 * {@code update}), strict access ({@code require}), and lifecycle policy
 * (wrapper pools, reference counts, and clearing non persistent assets on state switches).
 *
 * <p>Prefer {@link FlixelAssetManager#load(FlixelSource)} for explicit asset types. {@link FlixelAssetManager#load(String)}
 * resolves the path by file extension using a per-manager registry ({@link FlixelAssetManager#registerExtension}).
 *
 * <p>Game code typically uses {@link me.stringdotjar.flixelgdx.Flixel#assets} rather than constructing
 * a manager directly, but the manager is instantiable and extendable for advanced use cases.
 *
 * <p><b>{@link FlixelAsset}</b>: Public contract for typed handles (refcount, {@code persist}, load/require).
 * Pooled handles come from {@link FlixelAssetManager#obtainTypedAsset(String, Class)} as {@link FlixelTypedAsset}
 * (implementation detail). {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic} and
 * {@link me.stringdotjar.flixelgdx.audio.FlixelSound} also implement {@code FlixelAsset} where applicable.
 *
 * <p><b>{@link FlixelSource}</b>: Small interface that lets built in and user defined source objects
 * expose a consistent {@code (assetKey, type)} contract for loading and requiring through the manager.
 *
 * <p><b>{@link FlixelWrapperSource}</b>: Extends {@link FlixelSource} with a pooled wrapper type
 * (e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}) resolved via
 * {@link FlixelAssetManager#obtainWrapper(String, Class)}. Registering <em>new</em> wrapper types uses
 * {@link FlixelAssetManager#registerWrapperFactory(FlixelWrapperFactory)}; caller-owned wrappers use
 * {@link FlixelAssetManager#allocateSyntheticWrapperKey()} and {@link FlixelAssetManager#registerWrapper(FlixelPooledWrapper)}.
 *
 * <p><b>When to use sources vs wrappers</b>
 *
 * <table border="1">
 *   <caption>Choosing between extension loading and the wrapper pool</caption>
 *   <tr><th>Need</th><th>Use</th><th>Wrapper factory?</th></tr>
 *   <tr>
 *     <td>Load a file as a libGDX type (texture, sound data, etc.)</td>
 *     <td>{@link FlixelAssetManager#registerExtension} + {@link FlixelSource} + {@code load} / {@code get} / {@code source.require(assets)}</td>
 *     <td>No, as it's not a pooled facade.</td>
 *   </tr>
 *   <tr>
 *     <td>Refcount / persist / clear policy on a {@code (key, Class)} handle</td>
 *     <td>{@link FlixelAssetManager#obtainTypedAsset} -> {@link FlixelAsset}</td>
 *     <td>No, as it's a typed handle, not the pooled wrapper system.</td>
 *   </tr>
 *   <tr>
 *     <td>A second pooled object around the asset (policy, sharing), e.g. {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphic}</td>
 *     <td>{@link FlixelAssetManager#obtainWrapper}; built-in {@link me.stringdotjar.flixelgdx.graphics.FlixelGraphicWrapperFactory};
 *         {@link FlixelAssetManager#registerWrapperFactory} only for new wrapper <em>classes</em>.</td>
 *     <td>Only if you introduce a new wrapper type beyond framework defaults.</td>
 *   </tr>
 * </table>
 *
 * <p><b>Experts:</b> Use {@link FlixelAssetManager#getManager()} only when you need raw libGDX behavior.
 *
 * @see me.stringdotjar.flixelgdx.asset.FlixelAssetManager
 * @see me.stringdotjar.flixelgdx.asset.FlixelDefaultAssetManager
 * @see me.stringdotjar.flixelgdx.Flixel#assets
 */
package me.stringdotjar.flixelgdx.asset;
