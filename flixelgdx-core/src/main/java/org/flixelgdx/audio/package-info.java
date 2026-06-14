/**
 * Audio support for FlixelGDX.
 *
 * <p>This package provides simple audio APIs on top of a platform-agnostic sound backend.
 * It includes playback objects, cached sources, and managers for controlling sound and music.
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link org.flixelgdx.Flixel#sound Flixel.sound} - Central audio manager used by game code.</li>
 *   <li>{@link org.flixelgdx.audio.FlixelSound FlixelSound} - A playback object with volume, pan,
 *       pitch, fades, and completion signals.</li>
 *   <li>{@link org.flixelgdx.audio.FlixelSoundSource FlixelSoundSource} - A cached sound asset that can
 *       spawn fresh {@link org.flixelgdx.audio.FlixelSound FlixelSound} instances.</li>
 * </ul>
 *
 * <p>For loading, prefer {@link org.flixelgdx.Flixel#assets Flixel.assets} and the asset types in
 * {@link org.flixelgdx.asset asset}.
 */
package org.flixelgdx.audio;
