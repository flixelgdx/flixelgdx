# FlixelGDX packagr Plugin

Turn a game into a double-clickable app for every platform, from one machine.

This Gradle plugin packages a FlixelGDX game into a self-contained folder a player can run without
installing Java: a native launcher, a trimmed Java runtime, and only the code that platform needs.
It is the FlixelGDX-native replacement for libGDX-specific packaging tools, built to know how the
framework ships its per-platform native libraries and to keep packaging fast.

## Why it exists

A packaged game should start like any other native application and carry only what it needs. The
plugin does three things that make that practical:

- **A single-process native launcher.** Each package runs as one executable under its own name, not
  as a wrapper that spawns a separate `java` process. The launcher starts the JVM in-process from a
  small config file, so the game appears as itself in the task manager.
- **A trimmed runtime.** Instead of bundling a whole JDK, `jlink` assembles a runtime from just the
  modules a game needs, which keeps package size down.
- **Platform-matched contents.** The framework ships its native libraries (windowing, rendering,
  audio) as one jar per platform. The plugin bundles only the ones matching the target being built,
  so a Windows package never carries Linux or macOS native code.

Two long-standing packaging annoyances are fixed by design: a downloaded JDK is cached and reused,
so it is never re-downloaded on later builds, and each platform is a separate, incremental task.

## Applying the plugin

In the game module's `build.gradle`:

```groovy
plugins {
  id 'java'
  id 'org.flixelgdx.packagr' version '<flixel-version>'
}

packagr {
  mainClass = 'com.mygame.DesktopLauncher'

  targets {
    linuxX64()
    windowsX64()
  }
}
```

Run `./gradlew packageAll` to build every target, or a single target's task such as
`./gradlew packageLinuxX64`. Every package lands under `build/packagr/<target>/`. All of the
plugin's tasks live in the `packagr` group.

## Choosing targets

The `targets` block offers a one-liner for each common platform, plus a way to describe any other:

```groovy
packagr {
  mainClass = 'com.mygame.DesktopLauncher'

  targets {
    linuxX64()
    linuxArm64()
    windowsX64()
    macosX64()
    macosArm64()

    // Anything the convenience methods do not cover, described directly.
    register('steamDeck') {
      os = OperatingSystem.LINUX
      arch = Architecture.AARCH64
    }
  }
}
```

Each target adds a `package<Name>` task (for example `packageLinuxX64` or `packageSteamDeck`) and is
included by `packageAll`.

A target only produces a package when a native launcher is bundled for its platform. Linux x86_64
and Windows x86_64 ship with the plugin; other platforms need their launcher built and committed
first (see the launcher [`README`](src/main/resources/org/flixelgdx/gradle/packagr/stub/README.md)).

## Configuration reference

Everything below is optional except `mainClass`.

| Option         | Default                     | Meaning                                                    |
|----------------|-----------------------------|------------------------------------------------------------|
| `mainClass`    | (required)                  | Fully qualified class whose `main` starts the game.        |
| `appName`      | project name                | Name of the launcher executable and output folder.         |
| `appVersion`   | project version             | Version recorded for the package.                          |
| `jvmArg(...)`  | none                        | An argument passed to the bundled JVM at startup.          |
| `jdkVendor`    | `temurin`                   | JDK vendor to download and trim a runtime from.            |
| `jdkVersion`   | `17`                        | JDK feature version to bundle.                             |
| `jdkCacheDir`  | `~/.flixelgdx/jdks`         | Shared cache; a given JDK is downloaded only once.         |
| `jlinkModules` | a desktop-game module set   | Java modules included in the trimmed runtime.              |
| `assetsDir`    | none                        | A folder copied next to the launcher in every package.     |

Example using several of these:

```groovy
packagr {
  mainClass = 'com.mygame.DesktopLauncher'
  appName = 'MyGame'

  jvmArg('-Xmx1G')
  jdkVersion = 21

  assetsDir = file('assets')

  targets {
    windowsX64()
  }
}
```

## Output layout

Each target produces a folder a player can copy and run:

```
build/packagr/<target>/
  MyGame            launcher executable (MyGame.exe on Windows)
  packagr.cfg       launch settings the launcher reads
  jre/              the trimmed Java runtime
  lib/              the game jar and its platform-matched dependency jars
  assets/           present only when assetsDir is set
```

The `packagr.cfg` is a small text file listing the main class, the runtime and library folders, and
each JVM argument. The launcher reads it at startup; there is nothing platform-specific to edit.

## The bundled JDK

The runtime is trimmed from an Eclipse Temurin JDK downloaded from the Adoptium API. The first build
for a given version and platform downloads and verifies it, then caches it under `~/.flixelgdx/jdks`;
every later build reuses the cache with no download. Only Temurin is downloaded automatically today.

Because `jlink` cannot build a runtime from a newer JDK than the one running the build, the machine
running packaging needs a JDK at least as new as `jdkVersion`. Packaging one operating system from
another (for example a Windows package from Linux) works: the host's `jlink` assembles the target
runtime from the downloaded target JDK's modules.
