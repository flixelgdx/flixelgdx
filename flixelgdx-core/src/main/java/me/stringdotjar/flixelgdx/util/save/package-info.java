/**
 * Persistent preferences and JSON-friendly save data built on libGDX {@link com.badlogic.gdx.Preferences}.
 *
 * <p>The main entry point is {@link me.stringdotjar.flixelgdx.util.save.FlixelSave}, exposed as {@link me.stringdotjar.flixelgdx.Flixel#save}.
 * Call {@link me.stringdotjar.flixelgdx.util.save.FlixelSave#bind(String, String)} before writing keys, then use {@code data} and
 * {@link me.stringdotjar.flixelgdx.util.save.FlixelSave#flush()} to persist. Check {@link me.stringdotjar.flixelgdx.util.save.FlixelSave#isBound()}
 * and {@link me.stringdotjar.flixelgdx.util.save.FlixelSave#getStatus()} after load on a new install.
 *
 * <p>Use from the main libGDX thread only. This package is for small structured state, not large binary blobs.
 *
 * @see me.stringdotjar.flixelgdx.util.save.FlixelSave
 * @see me.stringdotjar.flixelgdx.util.save.FlixelSaveStatus
 */
package me.stringdotjar.flixelgdx.util.save;
