/**
 * Persistent preferences and JSON-friendly save data built on libGDX {@link com.badlogic.gdx.Preferences}.
 *
 * <p>The main entry point is {@link org.flixelgdx.util.save.FlixelSave}, exposed as {@link org.flixelgdx.Flixel#save}.
 * Call {@link org.flixelgdx.util.save.FlixelSave#bind(String, String)} before writing keys, then use {@code data} and
 * {@link org.flixelgdx.util.save.FlixelSave#flush()} to persist. Check {@link org.flixelgdx.util.save.FlixelSave#isBound()}
 * and {@link org.flixelgdx.util.save.FlixelSave#getStatus()} after load on a new install.
 *
 * <p>Use from the main libGDX thread only. This package is for small structured state, not large binary blobs.
 *
 * @see org.flixelgdx.util.save.FlixelSave
 * @see org.flixelgdx.util.save.FlixelSaveStatus
 */
package org.flixelgdx.util.save;
