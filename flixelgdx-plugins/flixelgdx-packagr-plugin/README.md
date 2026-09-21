# FlixelGDX packagr Plugin

Turn a game into a double-clickable app for every desktop platform, from one machine.

This Gradle plugin packages a FlixelGDX game into a self-contained folder a player can run without
installing Java: a native launcher, a trimmed Java runtime, and only the code that platform needs.

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

A downloaded JDK is cached and reused, so it is never re-downloaded on later builds, and each 
platform is a separate, incremental task.

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
`./gradlew packageLinuxX64`. Every package lands under `build/packagr/<target>/`. All the
plugin's tasks live in the `packagr` group.

## Choosing targets

The `targets` block offers a one-liner for each common platform:

```groovy
packagr {
  mainClass = 'com.mygame.DesktopLauncher'

  targets {
    linuxX64()
    linuxArm64()
    windowsX64()
    windowsArm64()
    macosArm64()
  }
}
```

Each target adds a `package<Name>` task (for example `packageLinuxX64` or `packageSteamDeck`) and is
included by `packageAll`. There is no macOS x86_64 (Intel) target, since Apple has deprecated it;
target Apple Silicon with `macosArm64()`.

A target only produces a package when a native launcher is bundled for its platform. Linux (x86_64
and ARM64), Windows (x86_64 and ARM64), and macOS (ARM64) all ship with the plugin; any other
platform needs its launcher built and committed first (see the launcher
[`README`](src/main/resources/org/flixelgdx/gradle/packagr/stub/README.md)).

## Configuration reference

Everything below is optional except `mainClass`.

| Option         | Default                     | Meaning                                                    |
|----------------|-----------------------------|------------------------------------------------------------|
| `mainClass`    | (required)                  | Fully qualified class whose `main` starts the game.        |
| `appName`      | project name                | Name of the launcher executable, output folder, and zip.   |
| `appVersion`   | project version             | Version recorded for the package.                          |
| `jvmArg(...)`  | none                        | An argument passed to the bundled JVM at startup.          |
| `jreVersion`   | `17`                        | Feature version of the Java runtime to bundle.             |
| `jreCacheDir`  | `~/.flixelgdx/jdks`         | Shared cache; a given runtime is downloaded only once.     |
| `jlinkModules` | a desktop-game module set   | Java modules included in the trimmed runtime.              |

Example using several of these:

```groovy
packagr {
  mainClass = 'com.mygame.DesktopLauncher'
  appName = 'MyGame'

  jvmArg('-Xmx1G')
  jreVersion = 21

  targets {
    windowsX64()
  }
}
```

## Output layout

Each target produces a folder a player can copy and run, plus a zip of it in the root project's
`dist` folder for sharing:

```
build/packagr/<target>/
  MyGame            launcher executable (MyGame.exe on Windows)
  packagr.cfg       launch settings the launcher reads
  jre/              the trimmed Java runtime
  lib/              the game jar and its platform-matched dependency jars

dist/
  MyGame-<target>.zip   the folder above, zipped under one top-level folder
```

The `packagr.cfg` is a small text file listing the main class, the runtime and library folders, and
each JVM argument. The launcher reads it at startup; there is nothing platform-specific to edit.

Assets are not copied into the package: a game's assets already ship inside its jar (which lands in
`lib/`), so there is no separate assets step.

## The bundled runtime

The runtime is trimmed from an Eclipse Temurin JDK downloaded from the Adoptium API; Temurin is the
only vendor, matching the framework's recommendation. The first build for a given version and
platform downloads and verifies it, then caches it under `~/.flixelgdx/jdks`; every later build
reuses the cache with no download.

`jlink` is version-strict (its version must match the runtime it builds), so a runtime of the target
version is always used to assemble the image rather than the JDK running the build. Packaging one
operating system from another (for example a Windows package from Linux) works: a host-platform JDK
of the target version supplies a `jlink` that runs locally, and it links the target JDK's modules.
