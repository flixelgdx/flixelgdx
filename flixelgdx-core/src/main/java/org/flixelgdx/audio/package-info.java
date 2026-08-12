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
 * <p>{@link org.flixelgdx.audio.FlixelSoundGroup FlixelSoundGroup} lets you control volume and
 * pause for a category of sounds (music or SFX) independently from the global volume. The
 * manager exposes a pre-built SFX group and music group; pass one during creation to assign a
 * sound to it:
 *
 * <pre>{@code
 * FlixelSound bgm = Flixel.sound.play(bgmSource, 0.6f, true, Flixel.sound.getMusicGroup());
 * }</pre>
 *
 * <h2>Audio effects</h2>
 * <p>Platform backends that support it expose DSP effects:
 * {@link org.flixelgdx.audio.FlixelReverbEffect FlixelReverbEffect},
 * {@link org.flixelgdx.audio.FlixelEchoEffect FlixelEchoEffect}, and
 * {@link org.flixelgdx.audio.FlixelLowPassEffect FlixelLowPassEffect}. Check the backend
 * documentation before depending on these in cross-platform builds.
 *
 * @see org.flixelgdx.audio.FlixelSoundManager
 * @see org.flixelgdx.audio.FlixelSound
 * @see org.flixelgdx.audio.FlixelSoundSource
 */
package org.flixelgdx.audio;
