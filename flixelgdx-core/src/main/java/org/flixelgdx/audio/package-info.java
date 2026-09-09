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
 * <p>Load a source through the asset manager, then play it through
 * {@link org.flixelgdx.Flixel#sound Flixel.sound}:
 *
 * <pre>{@code
 * // Load once (at state creation or via the asset manager):
 * FlixelSoundSource boom = Flixel.assets.get("sounds/explosion.ogg", FlixelSoundSource.class);
 *
 * // Play on demand:
 * FlixelSound s = Flixel.sound.play(boom, 0.8f);
 *
 * // React when the sound finishes:
 * s.onComplete.add(() -> Flixel.info("boom finished"));
 * }</pre>
 *
 * <h2>Looping music</h2>
 * <pre>{@code
 * FlixelSound music = Flixel.sound.play(bgmSource, 0.6f, true); // true = loop
 * music.fadeIn(2f); // fade in over 2 seconds
 * }</pre>
 *
 * <h2>Sound groups</h2>
 * <p>{@link org.flixelgdx.audio.FlixelSoundGroup FlixelSoundGroup} lets you pause and resume a
 * category of sounds (such as SFX) independently from the global volume. The manager exposes a
 * pre-built SFX group; pass one during creation to assign a sound to it:
 *
 * <pre>{@code
 * FlixelSound sfx = Flixel.sound.play(boomSource, 0.8f, false, Flixel.sound.getSfxGroup());
 * }</pre>
 *
 * @see org.flixelgdx.audio.FlixelSoundManager
 * @see org.flixelgdx.audio.FlixelSound
 * @see org.flixelgdx.audio.FlixelSoundSource
 */
package org.flixelgdx.audio;
