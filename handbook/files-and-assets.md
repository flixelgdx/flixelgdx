# Files and Assets: Loading, Caching, and Loading Screens

This guide explains how your game finds and reads files on every platform, the five file roots, where your assets
should live, how the asset manager loads and caches images, sounds, and data, how to build a loading screen, and how
reference counting decides when memory is freed.

## Table of contents

1. [Two layers: files and assets](#two-layers-files-and-assets)
2. [Where assets live](#where-assets-live)
3. [File handles](#file-handles)
4. [The five roots](#the-five-roots)
5. [Reading and writing files](#reading-and-writing-files)
6. [Listing a folder](#listing-a-folder)
7. [The asset manager](#the-asset-manager)
8. [Building a loading screen](#building-a-loading-screen)
9. [Getting a loaded asset](#getting-a-loaded-asset)
10. [Reference counting and unloading](#reference-counting-and-unloading)
11. [What the asset manager loads by default](#what-the-asset-manager-loads-by-default)
12. [Platform notes](#platform-notes)
13. [Naming rules that save headaches](#naming-rules-that-save-headaches)
14. [Check your understanding](#check-your-understanding)

## Two layers: files and assets

There are two layers, and it helps to know which one you are talking to.

- **Files** (`Flixel.files`, `FlixelFile`): "where is this file, and give me its bytes or text." Cheap, simple, no
  caching. Think of a library card catalog: it tells you where a book is, and lets you read it.
- **Assets** (`Flixel.assets`, `FlixelAsset`): "load this image or sound into memory once, decode it, and share it."
  Think of a librarian who fetches the book, keeps it on the front desk, and lends it to everyone who asks, keeping
  count of who still has it.

Most of the time you do not touch either directly: `sprite.loadGraphic(Flixel.files.internal("images/player.png"))`
and `Flixel.sound.play(...)` go through the asset manager for you. You reach for them when you read your own data files
or want to preload things on a loading screen.

## Where assets live

Generated projects have one `assets/` folder at the root, shared by every platform. Put images, sounds, fonts, and data
files there, in any folder structure you like:

```
assets/
  images/
    player.png
    tiles.png
  sounds/
    jump.ogg
  music/
    theme.ogg
  fonts/
    Nunito.ttf
  data/
    level1.json
```

Paths are always relative to `assets/` and always use forward slashes: `"images/player.png"`.

## File handles

`Flixel.files` hands out `FlixelFile` handles. A handle is just an address: creating one is cheap and does not touch
the disk until you read from it.

```java
FlixelFile levelFile = Flixel.files.internal("data/level1.json");
if (levelFile.exists()) {
  String json = levelFile.readString();
}
```

| Method                          | Returns                                              |
|---------------------------------|------------------------------------------------------|
| `exists()`                      | Whether the file is there.                           |
| `isDirectory()`                 | Whether it is a folder.                              |
| `getPath()` / `getName()`       | Its path, and its name without the folders.          |
| `getAbsolutePath()`             | The full path on the device, where that makes sense. |
| `length()`                      | Its size in bytes.                                   |
| `readString()` / `readBytes()`  | Its contents.                                        |
| `writeString(...)` / `writeBytes(...)` | Write contents, where the root is writable.   |
| `delete()`                      | Delete it, where the root is writable.               |
| `list()` / `list(suffix)`       | The files in a folder.                               |

## The five roots

The root you pick says where to look:

| Root                              | Location                                                         | Typical use                                 |
|-----------------------------------|------------------------------------------------------------------|---------------------------------------------|
| `Flixel.files.internal(path)`     | Your `assets/` folder, bundled with the game.                    | Images, sounds, fonts, data. **95% of the time.** |
| `Flixel.files.classpath(path)`    | Resources on the Java classpath (inside your jar).               | Files packaged with code, such as window icons and shaders. |
| `Flixel.files.external(path)`     | The player's home directory.                                     | Screenshots or exports the player should find. |
| `Flixel.files.local(path)`        | The folder the game was started from.                            | Developer tools and debug output.           |
| `Flixel.files.absolute(path)`     | A full operating system path.                                    | Rare. Paths the player chose themselves.    |

Internal files are read-only on every platform. For save data, do not write files by hand; use
[`Flixel.save`](saving.md), which picks the right private location on every platform (including the browser, which
has no file system).

## Reading and writing files

```java
// Read bundled data.
String json = Flixel.files.internal("data/level1.json").readString();
byte[] raw = Flixel.files.internal("data/map.bin").readBytes();

// Write a screenshot or an export (desktop).
FlixelFile out = Flixel.files.external("MyGame/export.txt");
boolean ok = out.writeString("Hello!");
```

Write methods return `false` when the platform or root does not allow writing, so check the result. Parsing JSON and
XML is covered in [JSON and XML](data-serialization.md).

## Listing a folder

```java
FlixelFile[] levels = Flixel.files.internal("data/levels").list(".json");
for (int i = 0; i < levels.length; i++) {
  Flixel.info("Found level {}", levels[i].getName());
}
```

Listing creates an array, so do it once (for example when a level select screen opens), not every frame.

## The asset manager

`Flixel.assets` loads files into memory once and shares them. Its job is to answer two questions: "is it loaded yet?"
and "who is still using it?"

Loading a big image or a long song the first time it is needed can cause a visible hitch in the middle of gameplay. The
asset manager lets you **preload** instead: queue everything at the start, load it a little at a time behind a loading
screen, then play without hitches. On platforms with threads, the slow part (reading and decoding) runs in the
background, and only the final step (uploading a texture to the GPU) happens on the main thread.

| Method                              | Purpose                                                            |
|-------------------------------------|--------------------------------------------------------------------|
| `load(path)`                        | Queue an asset for loading. Does not wait.                         |
| `update()`                          | Make progress on the queue. Returns `true` when everything is ready. |
| `update(millis)`                    | Make progress for at most `millis` milliseconds.                   |
| `getProgress()`                     | From 0 to 1, for a loading bar.                                    |
| `isLoaded(path)`                    | Whether one asset is ready.                                        |
| `finishLoading()`                   | Block until the whole queue is loaded.                             |
| `finishLoadingAsset(path)`          | Block until one asset is loaded.                                   |
| `get(path)`                         | The asset's handle (a `FlixelAsset`).                              |
| `unload(path)`                      | Remove an asset and free its memory.                               |
| `getDiagnostics()`                  | A text report of every cached asset, for debugging.                |

## Building a loading screen

```java
public class LoadingState extends FlixelState {

  private FlixelBar bar;

  @Override
  public void create() {
    super.create();
    Flixel.assets.load("images/player.png");
    Flixel.assets.load("images/tiles.png");
    Flixel.assets.load("music/theme.ogg");
    Flixel.assets.load("sounds/explosion.ogg");

    bar = new FlixelBar(170f, 170f, 300f, 12f).setRange(0f, 1f);
    bar.setTrack(Flixel.assets::getProgress);
    add(bar);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    // Spend at most 10 ms per frame loading, so the bar keeps animating.
    if (Flixel.assets.update(10)) {
      Flixel.switchState(MenuState::new);
    }
  }
}
```

Use `update(millis)` with a small budget so the loading screen itself stays smooth. Make the loading state your
game's first state, and every later `loadGraphic(...)` finds its image already in memory.

## Getting a loaded asset

`get(path)` returns a `FlixelAsset<T>` handle, where `T` depends on the file type: `FlixelGraphic` for images,
`FlixelSoundSource` for audio, and `String` for text files. Call `get()` on the handle for the content:

```java
FlixelAsset<FlixelGraphic> asset = Flixel.assets.get("images/player.png");
sprite.loadGraphic(asset.get());
```

Multiple calls with the same path return the same handle. If you ask for the wrong type
(`FlixelAsset<String>` for a PNG), you get a `ClassCastException` when you use it.

## Reference counting and unloading

Loaded assets stay in memory until you unload them. Nothing is thrown away automatically when you switch states, so you
stay in control of when memory is freed (and a level you return to does not have to load again).

To make unloading safe, every asset counts its users:

- `retain()` says "I am using this." The count goes up.
- `release()` says "I am done." The count goes down.

Sprites do this for you when they load a graphic and when they are destroyed. When you hold an asset yourself, follow
the same rule:

```java
private FlixelAsset<FlixelGraphic> bossArt;

@Override
public void create() {
  super.create();
  bossArt = Flixel.assets.get("images/boss.png");
  bossArt.retain();
}

@Override
public void destroy() {
  bossArt.release();
  super.destroy();
}
```

When you want the memory back (for example, after the last level that uses a huge background), call
`Flixel.assets.unload(path)`. It frees GPU memory immediately. If the count is still above zero, it logs a warning and
unloads anyway, so anything still using it will draw a destroyed texture; that warning is a sign of a missing
`release()`.

Custom file formats (a level format, a dialog script) can be taught to the asset manager with
`registerLoader(...)`. See [Extending the Framework](extending-the-framework.md).

## What the asset manager loads by default

| Extensions                                  | Handle content       |
|---------------------------------------------|----------------------|
| `.png`, `.jpg`, `.jpeg`, `.bmp`, `.tga`     | `FlixelGraphic` (a GPU texture) |
| `.mp3`, `.ogg`, `.wav`, `.flac`             | `FlixelSoundSource`  |
| `.txt`, `.xml`, `.json`                     | `String`             |
| `.ktx2` (Android only)                      | A compressed texture |

## Platform notes

- **Desktop:** internal files come from the `assets` folder (or from the jar in a packaged game), with the classpath as
  a fallback.
- **Web:** the build plugin writes a list of every asset (`assets/assets.txt`), and the browser downloads all of them
  before the game starts. File reads in game code are then instant memory lookups, never network requests. On the
  web, `readBytes()` on an image returns the encoded PNG or JPEG bytes, not decoded pixels; go through the asset
  manager (or `loadGraphic`) for images, so the browser's decoder runs. Use the HTML5 plugin's `excludes` option to keep
  files that only matter on other platforms out of the download.
- **Android:** with the Basis Universal plugin, PNGs are compressed at build time and loaded as `.ktx2` automatically;
  your code still asks for `.png`. See [Building and Shipping](building-and-shipping.md).

## Naming rules that save headaches

- **Use lowercase file names.** The web and Android are case-sensitive even when your desktop is not, so
  `Player.png` and `player.png` are different files there. A game that works on Windows and breaks in the browser is
  almost always this.
- **Use forward slashes**, never backslashes, on every platform.
- **Avoid spaces and special characters.** They work, but they cause trouble in URLs and build tools.

## Check your understanding

1. A sprite loads fine on Windows but is missing in the browser. What is the first thing to check?
2. Where should you save the player's progress: `Flixel.files.internal(...)` or something else?
3. Your loading bar freezes while loading, then jumps to 100%. What should you change?

(Answers: the capitalization of the file name and path; something else, `Flixel.save`, because internal files are
read-only; call `Flixel.assets.update(millis)` with a small budget every frame instead of `finishLoading()`.)

**Next:** [Saving Progress](saving.md)
