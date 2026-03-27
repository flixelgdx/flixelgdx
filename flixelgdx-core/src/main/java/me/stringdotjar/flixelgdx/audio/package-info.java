/**
 * Audio support for FlixelGDX.
 *
 * <p>This package provides Flixel style audio APIs on top of MiniAudio and libGDX integration.
 * It includes playback objects, cached sources, and managers for controlling sound and music.
 *
 * <p>Key types:
 * <ul>
 *   <li>{@link me.stringdotjar.flixelgdx.Flixel#sound} - Central audio manager used by game code.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.audio.FlixelSound} - A playback object with volume, pan,
 *       pitch, fades, and completion signals.</li>
 *   <li>{@link me.stringdotjar.flixelgdx.audio.FlixelSoundSource} - A cached sound asset that can
 *       spawn fresh {@link me.stringdotjar.flixelgdx.audio.FlixelSound} instances.</li>
 * </ul>
 *
 * <p>For loading, prefer {@link me.stringdotjar.flixelgdx.Flixel#assets} and the asset types in
 * {@link me.stringdotjar.flixelgdx.asset}.
 */
package me.stringdotjar.flixelgdx.audio;
