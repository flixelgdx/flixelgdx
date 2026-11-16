/**
 * Audio playback, sound grouping, and effects for FlixelGDX.
 *
 * <p>Game code almost always goes through {@link org.flixelgdx.Flixel#sound Flixel.sound}, the
 * central {@link org.flixelgdx.audio.FlixelSoundManager FlixelSoundManager} that controls
 * global volume, pausing, and muting. Individual sounds are represented by
 * {@link org.flixelgdx.audio.FlixelSound FlixelSound} objects that carry their own volume, pan,
 * pitch, looping, and completion signals. The raw audio data they play from is managed by
 * {@link org.flixelgdx.audio.FlixelSoundSource FlixelSoundSource}, which is loaded once and
 * shared across many playback instances.
 *
 * <h2>Playing a sound</h2>
 * <p>Pass a file handle to {@link org.flixelgdx.Flixel#sound Flixel.sound} to play it right away:
 *
 * <pre>{@code
 * FlixelSound s = Flixel.sound.play(Flixel.files.internal("sounds/explosion.ogg"), 0.8f);
 *
 * // React when the sound finishes. The signal carries no data, so the parameter is unnamed.
 * s.onComplete.add(_ -> Flixel.info("boom finished"));
 * }</pre>
 *
 * <h2>Looping music</h2>
 * <pre>{@code
 * FlixelSound music = Flixel.sound.playMusic(Flixel.files.internal("music/theme.ogg"), 0.6f, true); // true = loop
 * music.fadeIn(2f); // Fade in over 2 seconds.
 * }</pre>
 *
 * <h2>Sound groups</h2>
 * <p>{@link org.flixelgdx.audio.FlixelSoundGroup FlixelSoundGroup} lets you pause and resume a
 * category of sounds (such as SFX) independently from the global volume. The manager exposes a
 * pre-built SFX group; pass one during creation to assign a sound to it:
 *
 * <pre>{@code
 * FlixelSound sfx = Flixel.sound.play(Flixel.files.internal("sounds/explosion.ogg"), 0.8f, false, Flixel.sound.getSfxGroup());
 * }</pre>
 *
 * @see org.flixelgdx.audio.FlixelSoundManager
 * @see org.flixelgdx.audio.FlixelSound
 * @see org.flixelgdx.audio.FlixelSoundSource
 */
package org.flixelgdx.audio;
