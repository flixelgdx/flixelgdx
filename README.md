# FlixelGDX

<img src="docs/readme/flxgdx ii.png" width="400">

[![CI](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml/badge.svg)](https://github.com/flixelgdx/flixelgdx/actions/workflows/ci_build.yml)
[![JitPack](https://jitpack.io/v/flixelgdx/flixelgdx.svg)](https://jitpack.io/#flixelgdx/flixelgdx)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)

🎮 **FlixelGDX is a Java-based featherweight powerhouse of a game framework built on top of [libGDX](https://libgdx.com/).** It chases the same beginner-friendly, productive vibe as [HaxeFlixel](https://haxeflixel.com/) (and the classic ActionScript [Flixel](http://www.flixel.org/)) while having incredibly low and fast performance for devices of all kinds..

If you want states, sprites, tweens, and groups without giving up libGDX, you are in the right place!

> [!NOTE]
> FlixelGDX is an independent project and is not officially affiliated with HaxeFlixel or libGDX.

> [!IMPORTANT]
> FlixelGDX is still relatively new, and as of right now, it only supports desktop and web. Mobile support is coming soon!

---

## What makes FlixelGDX special ✨

- 🤝 **Familiar Flixel-shaped building blocks** such as `FlixelGame`, `FlixelState`, `FlixelSprite`, groups, and lifecycle hooks that feel like home if you already know Flixel.
- ⚙️ **Powerful and easily changeable API** .
- 🌐 **Ship your game seamlessly** on Desktop (**LWJGL3**), and **web** (TeaVM).
- 🧰 **Batteries included** with everything you need for game development, such as tweens, timers, animation, bitmap text, audio helpers, saves, input action sets, debug helpers, and more!
- 🧱 **Clean module split** for your games logic, and backend modules to launch your game on different platforms using the best and most powerful libraries.
- ☕ **Powered by Java 17 + Gradle** to include a codebase that is meant to grow with your project.

### At a glance 🎯

- 🗂️ **States and scenes** — `FlixelGame` + `FlixelState` keep screens organized.
- 🧩 **Sprites and groups** — `FlixelSprite`, `FlixelGroup`, sprite groups, and collision-friendly objects.
- ✨ **Motion and juice** — Tweens (including motion helpers), timers, camera shake, flicker, and color tweens.
- 🎬 **Animation and art pipelines** — Spritemaps, animation controllers, and Adobe Animate-style rigs via `FlixelAnimateSprite`.
- 🔊 **Audio and assets** — Sound helpers and Flixel-flavored asset paths on top of libGDX loading.
- 🎮 **Input** — Easy-to-use input managers for keyboard, mouse, and controllers (mobile coming soon!).
- 🐞 **Debug** — Overlays and utilities so you can see what the game is doing while you iterate.

---

## Add it with JitPack 📦

[JitPack](https://jitpack.io/#flixelgdx/flixelgdx) builds this repo and publishes every module it installs to Maven local. Published versions include git **tags** (for example `0.2.1`), **commits**, or branch snapshots. Pick a green build from the JitPack page for your project.

**1. Register the repository** (root `build.gradle` or `settings.gradle`, depending on your Gradle setup):

```gradle
repositories {
  mavenCentral()
  maven { url 'https://jitpack.io' }
}
```

**2. Depend on core from your libGDX `core` module.** JitPack exposes artifacts under the group **`com.github.flixelgdx.flixelgdx`**.

```gradle
dependencies {
  implementation 'com.github.flixelgdx.flixelgdx:flixelgdx-core:<version>'
}
```

Replace `0.2.1` with the tag, commit, or branch snapshot you want from [JitPack](https://jitpack.io/#flixelgdx/flixelgdx).

**3. Add the backend module on each launcher.** Example for desktop (LWJGL3):

```gradle
dependencies {
  implementation 'com.github.flixelgdx.flixelgdx:flixelgdx-lwjgl3:<version>'
}
```

Other published modules include `flixelgdx-android`, `flixelgdx-ios`, `flixelgdx-teavm`, and tooling like `flixelgdx-teavm-plugin` when you target the browser. Match versions across modules.

For full wiring (launchers, composite builds, TeaVM notes), see **[COMPILING.md](COMPILING.md)**.

---

# Project Navigation 📚

- **[Contributing Guide](CONTRIBUTING.md)**: Learn how to contribute to the project, coding standards, and PR requirements.
- **[Project Structure](PROJECT.md)**: Understand the multi-module layout and how Gradle is used.
- **[Compiling & Testing](COMPILING.md)**: How to build the framework and test it as a dependency in your own projects.
- **API docs (Javadoc)**: Built from **`master`** via GitHub Actions and published to GitHub Pages — browse **`https://flixelgdx.github.io/flixelgdx/`** (landing page links into each module). Run `./gradlew javadocAll` locally to generate HTML under each module’s `build/docs/javadoc`.
