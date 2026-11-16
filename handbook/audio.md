# Audio: Sound Effects, Music, and Volume

This guide covers playing sound effects and music, controlling a sound after it starts (volume, pitch, pan, fades,
seeking), reacting when a sound ends, keeping music playing across states, organizing sounds into groups for options
menus, preloading audio, and the differences between platforms.

## Table of contents

1. [How audio is organized](#how-audio-is-organized)
2. [Playing a sound effect](#playing-a-sound-effect)
3. [Playing music](#playing-music)
4. [Controlling a sound](#controlling-a-sound)
5. [Fading in and out](#fading-in-and-out)
6. [Knowing when a sound ends](#knowing-when-a-sound-ends)
7. [Sounds and state switches](#sounds-and-state-switches)
8. [Reusing sounds you play often](#reusing-sounds-you-play-often)
9. [Master volume, pausing, and muting](#master-volume-pausing-and-muting)
10. [Sound groups for options menus](#sound-groups-for-options-menus)
11. [Preloading audio](#preloading-audio)
12. [File formats and platforms](#file-formats-and-platforms)
13. [Check your understanding](#check-your-understanding)

## How audio is organized

Think of a mixing desk at a concert. Each instrument has its own channel with a volume slider (a `FlixelSound`).
Channels are grouped into buses, such as "drums" or "vocals," each with its own slider (a `FlixelSoundGroup`). And a
master fader controls everything at once (`Flixel.sound`'s master volume).

| Piece               | Role                                                                                      |
|---------------------|-------------------------------------------------------------------------------------------|
| `Flixel.sound`      | The `FlixelSoundManager`: plays sounds, owns the music slot, master volume, pause and resume. |
| `FlixelSound`       | One playing (or ready-to-play) sound with its own volume, pitch, pan, and loop setting.   |
| `FlixelSoundGroup`  | A category of sounds you can pause, resume, stop, and set the volume of together.         |
| `FlixelSoundSource` | The decoded audio data, loaded once and shared by every sound that plays it.              |

## Playing a sound effect

```java
// A one-shot sound effect from the assets folder.
Flixel.sound.play(Flixel.files.internal("sounds/jump.ogg"));

// At 50% volume.
Flixel.sound.play(Flixel.files.internal("sounds/coin.ogg"), 0.5f);

// Looping, until you stop it.
FlixelSound engine = Flixel.sound.play(Flixel.files.internal("sounds/engine.ogg"), 0.8f, true);
```

The first time a file is played, it is loaded and decoded; after that, the decoded data is cached and shared. To
avoid the small hitch of that first load in the middle of gameplay, preload sounds (see below).

## Playing music

Music gets its own slot. Only one music track plays at a time, and starting a new one stops the old one:

```java
Flixel.sound.playMusic(Flixel.files.internal("music/theme.ogg"));            // Loops, full volume.
Flixel.sound.playMusic(Flixel.files.internal("music/boss.ogg"), 0.8f);       // Loops, 80% volume.
Flixel.sound.playMusic(Flixel.files.internal("music/jingle.ogg"), 1f, false); // Plays once.

Flixel.sound.music.fadeOut(1f); // The current track is always on Flixel.sound.music.
```

## Controlling a sound

`play(...)` returns the `FlixelSound`, which you can keep to control it later. Most setters return the sound, so they
chain:

```java
FlixelSound s = Flixel.sound.play(Flixel.files.internal("sounds/alarm.ogg"));

s.setVolume(0.5f);        // 0 to 1.
s.setPitch(1.2f);         // 1 is normal; higher is faster and higher-pitched.
s.setPan(-1f);            // -1 is fully left, 0 is centered, 1 is fully right.
s.setLooped(true);

s.pause();
s.resume();
s.stop();                 // Pause and rewind to the start.
s.play();                 // Start again from the beginning.
s.play(false);            // Continue from where it is instead of restarting.
s.play(true, 1500f);      // Restart from 1.5 seconds in.

float position = s.getTime();   // Milliseconds.
float length = s.getLength();   // Milliseconds.
s.setTime(30000f);              // Jump to 30 seconds.
s.setEndTime(45000f);           // Stop at 45 seconds (or null for the real end).
boolean playing = s.isPlaying();
```

A small random pitch makes repeated effects (footsteps, gunshots) sound less mechanical:

```java
s.setPitch(Flixel.random.nextFloat(0.9f, 1.1f));
```

## Fading in and out

```java
music.fadeIn(2f);              // From silent to full volume over 2 seconds.
music.fadeIn(2f, 0f, 0.6f);    // From 0 to 60%.
music.fadeOut(1f);             // To silent over 1 second.
music.fadeOut(1f, 0.2f);       // Down to 20%, for ducking under dialog.
```

Fades are tweens under the hood. A sound's fade tween is reachable through `getFadeTween()` if you need to cancel it.

## Knowing when a sound ends

Every sound has an `onComplete` signal that fires when it finishes (for a non-looping sound) or reaches its end time:

```java
FlixelSound line = Flixel.sound.play(Flixel.files.internal("voice/intro.ogg"));
line.onComplete.add(data -> showNextDialogLine());
```

The signal carries no data (its type is `Void`), so the parameter is always `null`.

## Sounds and state switches

When you switch states, every sound that is not marked **persistent** is stopped and released, **including the
music**. That is usually what you want: the level's sounds should not leak into the menu. For music that should keep
playing across screens (a menu theme shared by the title, options, and level select screens), mark it persistent:

```java
FlixelSound theme = Flixel.sound.playMusic(Flixel.files.internal("music/menu.ogg"));
theme.setPersist(true);
```

When you later want it gone, fade it out and `destroy()` it, or start new music with `playMusic(...)`, which replaces
it.

## Reusing sounds you play often

Each call to `Flixel.sound.play(...)` creates a new `FlixelSound` object, and by default that object is kept until the
next state switch, even after it finishes. For a sound you trigger a few times that is fine. For a sound you trigger
constantly (a machine gun, footsteps), pick one of these:

**Create once, replay many times.** `create(...)` builds a sound without playing it. `play()` restarts it from the
beginning each time:

```java
private FlixelSound shot;

@Override
public void create() {
  super.create();
  shot = Flixel.sound.create(Flixel.files.internal("sounds/shot.ogg"));
}

private void fire() {
  shot.play(); // Restarts the same sound. No new object.
}
```

This cuts off the previous shot if it is still playing, which is often exactly right for rapid fire. If you need
overlapping copies, keep a small array of sounds and cycle through it.

**Let one-shots clean themselves up.** If you do want a new sound each time, have it release itself when it ends:

```java
Flixel.sound.play(Flixel.files.internal("sounds/explosion.ogg")).setAutoDestroy(true);
```

## Master volume, pausing, and muting

```java
Flixel.sound.setMasterVolume(0.7f);       // Everything at 70%.
Flixel.sound.changeMasterVolume(-0.1f);   // A bit quieter.
float master = Flixel.sound.getMasterVolume();

Flixel.sound.pause();   // Pause every sound.
Flixel.sound.resume();  // Resume them.
```

A mute toggle is a master volume of `0` that remembers the old value. Store the player's choice with
[`Flixel.save`](saving.md).

When the window loses focus, audio pauses automatically and resumes when focus returns, as long as `Flixel.autoPause`
is `true` (the default). The debugger's pause (F3) also pauses audio.

## Sound groups for options menus

Groups let you control a whole category of sounds at once, which is how "Music" and "Effects" sliders in an options
menu work. Every sound goes into the manager's default SFX group unless you choose otherwise. Make your own groups
through the platform's factory:

```java
// Once, at startup (for example on your game class).
public static FlixelSoundGroup musicGroup;
public static FlixelSoundGroup sfxGroup;

musicGroup = Flixel.sound.getFactory().createGroup();
sfxGroup = Flixel.sound.getSfxGroup();

// Play into a group with the four-argument play(...) or create(...).
Flixel.sound.play(Flixel.files.internal("music/theme.ogg"), 1f, true, musicGroup);

// Options menu sliders:
musicGroup.setVolume(musicSlider);
sfxGroup.setVolume(effectsSlider);

// Pause only the effects, for example during a cutscene.
sfxGroup.pause();
sfxGroup.resume();
```

A group has `pause()`, `resume()`, `stop()`, `getVolume()`, `setVolume(...)`, `add(sound)`, and `remove(sound)`. Note
that `playMusic(...)` always uses the default SFX group; to put music in your own group, play it with
`play(file, volume, true, musicGroup)` and keep the returned sound yourself.

## Preloading audio

Decoding a long music file can take a moment. On a loading screen, ask the asset manager to load audio ahead of time;
later `play(...)` calls find it already decoded:

```java
Flixel.assets.load("music/theme.ogg");
Flixel.assets.load("sounds/explosion.ogg");
// ... call Flixel.assets.update() each frame until it returns true ...
```

See [Files and Assets](files-and-assets.md) for loading screens.

## File formats and platforms

| Format | Supported |
|--------|-----------|
| `.ogg` | Yes. A good default for both music and effects.            |
| `.mp3` | Yes.                                                       |
| `.wav` | Yes. Uncompressed, so best for very short effects.         |
| `.flac`| Yes. Lossless and large; usually more than a game needs.   |

| Platform | Audio engine     | Notes                                                                                       |
|----------|------------------|---------------------------------------------------------------------------------------------|
| Desktop  | miniaudio        |                                                                                             |
| Web      | Web Audio API    | Browsers do not allow sound until the player interacts with the page. The framework starts audio on the first click, tap, or key press by itself. A title screen with "click to start" hides this nicely. |
| Android  | miniaudio        | With `Flixel.autoPause = false`, audio keeps playing when the app goes to the background.   |

Keep file names lowercase and use forward slashes: the web and Android are case-sensitive.

## Check your understanding

1. Your level's music stops when the pause substate opens. Is a state switch to blame?
2. Your menu music cuts out when moving from the title screen to the options screen. What is the fix?
3. A machine gun fires ten shots per second for minutes at a time. How should its sound be played?

(Answers: no, opening a substate is not a switch; check whether the parent paused it, or whether `onFocusLost` and
auto-pause did; call `setPersist(true)` on the music; create the sound once and call `play()` on it for each shot, or
mark each one-shot with `setAutoDestroy(true)`.)

**Next:** [Tweens and Timers](tweens-and-timers.md)
