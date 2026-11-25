# Building and Shipping: Packaging for Desktop, Web, and Android

Making a game is half the work; getting it to players is the other half. This guide covers the FlixelGDX Gradle
plugins that turn a project into something players can run: native desktop packages, a deployable web app, checked
and bundled shaders, and compressed Android textures. It also covers versioning and a pre-release checklist.

## Table of contents

1. [The plugins at a glance](#the-plugins-at-a-glance)
2. [Before you ship: release mode](#before-you-ship-release-mode)
3. [Desktop: native packages with packagr](#desktop-native-packages-with-packagr)
4. [Web: the HTML5 plugin](#web-the-html5-plugin)
5. [Shaders: the shader plugin](#shaders-the-shader-plugin)
6. [Asset paths: the assets plugin](#asset-paths-the-assets-plugin)
7. [Project info: the project plugin](#project-info-the-project-plugin)
8. [Android: building and compressing textures](#android-building-and-compressing-textures)
9. [Keeping files out of a build](#keeping-files-out-of-a-build)
10. [Versioning your game](#versioning-your-game)
11. [Settings and properties](#settings-and-properties)
12. [Release checklist](#release-checklist)
13. [Check your understanding](#check-your-understanding)

## The plugins at a glance

| Plugin id                | Applies to     | Does                                                                         |
|--------------------------|----------------|------------------------------------------------------------------------------|
| `org.flixelgdx.packagr`  | Desktop module | Builds a double-clickable package per operating system, with its own trimmed Java runtime. |
| `org.flixelgdx.html5`    | Web module     | Copies assets, generates the page, runs a dev server, and zips a deployable web app. |
| `org.flixelgdx.shaders`  | Core module    | Checks WGSL shaders at build time and bundles them into resources.           |
| `org.flixelgdx.assets`   | Core module    | Generates a type-safe `Assets` class with a path constant for every asset.   |
| `org.flixelgdx.project`  | Settings file  | Reads `project.toml` for the modules, title, version, window settings, constants and profiles, and adds the `bumpBuild` and `doctor` tasks. |
| `org.flixelgdx.basisu`   | Android module | Compresses PNGs into KTX2/Basis Universal textures at build time.            |

Projects created with the IntelliJ IDEA plugin come with the right plugins already applied.

## Before you ship: release mode

Release mode is the default, so a packaged or deployed game runs in release mode unless something explicitly asks for
debug mode. Debug tasks (`:desktop:debug`, `:html5:debug`) and debuggable Android builds are for development only.
There is nothing to remove from your code: debugger calls cost nothing in release mode (see [Debugging](debugging.md)).

## Desktop: native packages with packagr

A Java game normally needs players to install Java and run a command. packagr removes both: each package is a folder
with a native launcher (named after your game), a trimmed Java runtime, and your game's jar.

```groovy
// desktop/build.gradle
plugins {
  id 'java'
  id 'org.flixelgdx.packagr' version '<flixel-version>'
}

packagr {
  mainClass = 'com.mygame.DesktopLauncher'
  appName = 'MyGame'
  jvmArg('-Xmx1G')
  targets {
    linuxX64()
    linuxArm64()
    windowsX64()
    windowsArm64()
    macosArm64()
  }
}
```

```bash
./gradlew packageAll
```

```bash
./gradlew packageWindowsX64
```

Each target lands in `build/packagr/<target>/`, and a zip of it goes into the root project's `dist/` folder, ready to
upload to a store or website:

```
build/packagr/<target>/
  MyGame           The launcher (MyGame.exe on Windows).
  packagr.cfg      Launch settings the launcher reads.
  MyGame.jar       Your self-contained game jar.
  jre/             The trimmed Java runtime.
```

Good to know:

- **The launcher runs the game in its own process**, so it shows up under your game's name in the task manager.
- **The runtime is trimmed** with `jlink` from an Eclipse Temurin JDK, downloaded once and cached in
  `~/.flixelgdx/jdks`. Change the version with `jdkVersion` and the modules with `jlinkModules`.
- **Only matching natives are bundled**, so a Windows package carries no Linux or macOS libraries.
- **You can package every platform from one machine**, for example Windows packages from Linux.
- **macOS packages target Apple Silicon** (`macosArm64()`); there is no Intel macOS target.
- **The game jar must be self-contained** (dependencies, natives, and assets inside it). Generated projects already
  build it that way.

## Web: the HTML5 plugin

The web module applies TeaVM (which compiles Java to JavaScript or WebAssembly) and the FlixelGDX HTML5 plugin, which
does everything else a browser needs: the HTML page, asset copying, the asset preload list, a dev server with the right
file types, and packaging.

```groovy
// html5/build.gradle
plugins {
  id 'org.teavm' version '0.13.0'
  id 'org.flixelgdx.html5' version '<flixel-version>'
}

teavm {
  all {
    mainClass = 'com.mygame.web.WebLauncher'
  }
  js {
    addedToWebApp = true   // The JavaScript fallback.
    obfuscated = true
  }
  wasmGC {
    addedToWebApp = true   // The faster WebAssembly build, preferred when the browser supports it.
    obfuscated = true
  }
}

html5 {
  title = 'My Game'           // The browser tab title.
  favicon = file('src/main/webapp/favicon.ico')
  excludes = ['debug/**', '**/*.psd']
}
```

`org.teavm` must be applied before `org.flixelgdx.html5`.

| Task                    | Does                                                                         |
|-------------------------|------------------------------------------------------------------------------|
| `./gradlew :html5:run`  | Builds, serves at `http://localhost:8080`, and opens the browser.            |
| `./gradlew :html5:debug`| The same, with `?flixel.mode=debug` added to the address.                    |
| `./gradlew :html5:package` | Builds `dist/<name>-html5.zip`, ready for any static web host.            |

To deploy, unzip the package on any static host (itch.io, GitHub Pages, your own server). Make sure the host serves
`.wasm` files with the `application/wasm` type; most do.

Other options worth knowing:

| Option                     | Meaning                                                                    |
|----------------------------|----------------------------------------------------------------------------|
| `canvasId`                 | The id of the `<canvas>` element (default `flixelgdx-canvas`).             |
| `mode`                     | A runtime mode baked into the page as the default. The address parameter still overrides it. |
| `devServerPort`            | The dev server's port (default 8080).                                      |
| `assetsDir`                | Where assets come from (default: the root `assets/` folder).               |
| `webappDir`                | Extra web files copied as-is. An `index.html` here replaces the generated one. |
| `generateDefaultIndexHtml`, `indexHtml`, `injectHtmlTags` | Control the page. A custom page can use tags such as `{{TITLE}}` and `{{CANVAS_ID}}`, which the plugin fills in. |

## Shaders: the shader plugin

Without help, a typo in a shader only shows up when the game runs on a player's machine. The shader plugin compiles
every `.wgsl` file in `src/main/shaders` during the build, with the same WebGPU compiler the game uses, and fails the
build with `file:line:column` errors. It needs no GPU. It then copies the shaders into the game's resources, so
`FlixelShader.load(name)` finds them on every platform:

```groovy
// core/build.gradle
plugins {
  id 'java'
  id 'org.flixelgdx.shaders' version '<flixel-version>'
}
```

```java
FlixelShader tint = FlixelShader.load("tint");    // src/main/shaders/tint.wgsl
FlixelShader wave = FlixelShader.load("fx/wave"); // src/main/shaders/fx/wave.wgsl
```

Its `processShaders` task runs before resources are processed, so every normal build checks shaders. Run it alone with
`./gradlew processShaders`. See [Graphics and Shaders](graphics-and-shaders.md) for writing shaders.

## Asset paths: the assets plugin

Asset paths are plain strings, so a typo or a renamed file only shows up when the game fails to load it. The assets
plugin turns the `assets/` folder into a generated `Assets` class: each folder is a nested class in PascalCase and each
file is an UPPER_SNAKE_CASE constant holding its path. A missing file then becomes a compile error, and your IDE can
autocomplete every path.

```groovy
// core/build.gradle
plugins {
  id 'java'
  id 'org.flixelgdx.assets' version '<flixel-version>'
}

assetsClass {
  packageName = 'com.mygame'
}
```

```java
Flixel.assets.load(Assets.Shared.Sprites.PLAYER_PNG); // assets/shared/sprites/player.png
```

The class is generated before the code compiles and whenever the IDE reloads the Gradle project. See the
[plugin README](../flixelgdx-plugins/flixelgdx-assets-plugin/README.md) for the naming rules and settings.

## Project info: the project plugin

A game's name, version and window size tend to get typed into several build files and the game's code, and then
drift apart. The project plugin is a *settings* plugin that reads one file, `project.toml`, next to
`settings.gradle`. The file can be written by hand or by tools.

```groovy
// settings.gradle
plugins {
  id 'org.flixelgdx.project' version '<flixel-version>'
}
```

```toml
# project.toml
platforms = ["desktop", "html5", "android"]

[app]
title   = "My Game"
package = "com.example.mygame"
company = "My Studio"
version = "0.1.0"

[window]
width  = 640
height = 360
```

From that file the plugin includes `core` and the listed platform modules (`android` only when an Android SDK is
found, otherwise it warns and skips it), sets `group` and `version` on every project, and makes the HTML5 plugin's page
title and packagr's application name default to the title. It also generates a `Project` class in `core`, so the game
can start from the file's values:

```java
FlixelConfig config = Project.config().build();
```

An invalid title, company or version fails the build with a message naming the field and the character that is wrong.
Build scripts read the same values through the `game` extension. In the Android module, that keeps the app ID and
version in step with the file:

```kotlin
// android/build.gradle.kts
android {
  defaultConfig {
    applicationId = game.packageName
    versionCode = game.build
    versionName = game.version
  }
}
```

`versionCode` comes from `app.build`, the build number (see [Versioning your game](#versioning-your-game)).

### Your own constants

A `[defines]` table holds values of your own, such as a demo flag or the number of save slots. Each one becomes a
constant in `Project.Defines`:

```toml
[defines]
demo          = false
enable_cheats = false
max_saves     = 3
```

```java
if (Project.Defines.ENABLE_CHEATS && Flixel.keys.justPressed(FlixelKey.F1)) {
  player.health = 999;
}
```

They are compile-time constants, so code behind a flag that is `false` is removed by the compiler and never ships.
Build scripts read them too, as `game.defines["demo"]`. They end up in the game as plain text, so do not put passwords
or secret keys in them.

### Profiles

A profile changes a few values for one build, like a sticky note on a recipe. Write it as a `[profile.<name>]` table
and pick it with the `flixel.profile` property:

```toml
[profile.dev]
app.mode              = "debug"
window.fullscreen     = false
defines.enable_cheats = true

[profile.demo]
app.title    = "My Game Demo"
defines.demo = true
platforms    = ["desktop", "html5"]
```

```bash
./gradlew :desktop:playtest
./gradlew :desktop:packageAll -Pflixel.profile=demo
```

Every profile gets a task named after it in each platform module (`:desktop:playtest`, `:html5:playtest` and
`:android:playtest`), listed under **profiles** in your IDE's Gradle tool window. It starts the game with that profile,
the same as `./gradlew :desktop:run -Pflixel.profile=playtest`, and in the mode the profile's `app.mode` asks for. For
any other task, such as packaging, add `-Pflixel.profile`. Run profile tasks by their full name, and one profile per
Gradle run.

Without the property the file is used as written. A profile can change everything except `app.package`, and
`Project.PROFILE` holds the name of the selected one (an empty string when none is selected).

`app.mode` (`release`, `debug` or `test`) picks the mode the game starts in on every platform, and it can **only** be
set inside a profile. That way a build without a profile always starts in release mode, and a debug build cannot
reach players by accident. Think of it as a thermostat's default temperature: a launch setting such as
`-Dflixel.mode` or `?flixel.mode=` can still turn the dial for one run.

### Checking your setup

When something does not build and the reason is unclear, run:

```bash
./gradlew doctor
```

It checks Gradle, your JDK and Java toolchain, the Android SDK, the module folders, `project.toml` and every profile,
and says how to fix each problem. It even reports a broken `project.toml`, which otherwise stops every build before
it starts. The report ends with a link to the FlixelGDX
[bug report form](https://github.com/flixelgdx/flixelgdx/issues/new?template=bug_report.yml): if the problem turns out
to be a bug, paste the whole report into its "Logs or Screenshots" field.

See the [plugin README](../flixelgdx-plugins/flixelgdx-project-plugin/README.md) for every key and the naming rules.

## Android: building and compressing textures

Android builds use the standard Android Gradle plugin:

```bash
./gradlew :android:installDebug
```

```bash
./gradlew :android:assembleRelease
```

A release build must be signed with your own key before a store accepts it; follow the Android documentation for
creating and configuring a signing key. Remember the framework's requirements: API 26, Vulkan 1.1, and the 64-bit
ABIs `arm64-v8a` and `x86_64`.

### Compressed textures with Basis Universal

A 1024 by 1024 PNG that is 1 MB on disk becomes 4 MB of video memory once decoded. The Basis Universal plugin
compresses PNGs at build time into `.ktx2` files that stay compressed in GPU memory; at runtime the Android backend
picks the best format the device supports (ASTC or ETC2). Your code keeps asking for `.png` paths.

```groovy
// android/build.gradle
plugins {
  id 'com.android.application'
  id 'org.flixelgdx.basisu' version '<flixel-version>'
}

basisu {
  excludes = ['ui/icons/app-icon.png', 'fonts/**'] // Keep these as plain PNGs.
  // useUastc = true      // Higher quality, larger files, for art where exact color matters.
  // generateMipmaps = true
}
```

Compression is off by default. Turn it on for a build, or permanently in `gradle.properties`:

```bash
./gradlew :android:assembleRelease -PenableBasisuCompression=true
```

```properties
enableBasisuCompression=true
```

Notes: the default mode (ETC1S) is lossy, so colors can shift slightly; textures you change pixel by pixel at runtime
cannot be compressed and should be excluded; and desktop and web builds keep using the plain PNGs.

## Keeping files out of a build

Source art, debug data, and platform-specific files do not belong in every build:

- **Web:** `html5 { excludes = [...] }`. Excluded files are not copied or preloaded.
- **Desktop:** add an `exclude` to the `from(rootProject.file('assets'))` block of the desktop jar task.
- **Android:** use `ignoreAssetsPattern` in the `android` block, or the Basis Universal `excludes` for compression only.

## Versioning your game

A game has two version values in `project.toml`, and they answer different questions:

- **`version`** (such as `1.2.0`) is the name players see. Change it when the update deserves a new name.
- **`build`** (such as `14`) is a plain whole number that only ever goes up. Android and Google Play compare it to
  tell a newer build from an older one: Google Play rejects an upload whose number is not higher than the last one,
  and a phone refuses to install a build over one with a higher number.

Raise `build` for **every build you give to players**, not only for big releases:

| What you ship                                              | `version` | `build` |
|------------------------------------------------------------|-----------|---------|
| A major update (1.0.0 to 2.0.0)                            | changes   | +1      |
| A small fix (1.2.0 to 1.2.1)                               | changes   | +1      |
| The same version again, for example to fix a bad package   | same      | +1      |
| A test or beta upload to a store                           | either    | +1      |
| Builds you only run on your own computer                   | same      | no need |

Run `./gradlew bumpBuild` right before you build something that leaves your computer. It raises the number by one and
keeps the rest of the file as it was. Desktop and web do not check the number, but using one number for every
platform keeps save files and bug reports consistent.

Both values reach the game through the generated `Project` class, and `Project.config()` puts the version into the
config, so it is available at runtime (for a title screen, crash reports, or save migration):

```java
Flixel.info("Starting {} {} (build {})", Flixel.config.getTitle(), Flixel.config.getVersion(), Project.BUILD);
```

Store the build number in save files too, so a future update can recognize and convert old saves:

```java
int savedBuild = Flixel.save.getInt("build", Project.BUILD);
if (savedBuild < 10) {
  // Build 10 renamed "coins" to "gold", so older saves are fixed up once.
  Flixel.save.data.put("gold", Flixel.save.getInt("coins", 0));
  Flixel.save.data.remove("coins");
}
Flixel.save.data.put("build", Project.BUILD);
```

Note that the company and title also decide where saves are stored, so do not change them after release, or players'
saves will seem to disappear.

## Settings and properties

FlixelGDX reads a few named settings, all starting with `flixel.`, so they never clash with other tools. Some are read
by the Gradle plugins while the game is built, and some by the game itself while it runs.

### Build settings (Gradle properties)

Pass these to Gradle with `-P` (`./gradlew build -Pflixel.profile=dev`), or put them in a `gradle.properties` file.
The one next to `settings.gradle` is shared with everyone who builds the game; the one in your Gradle user home
(`~/.gradle/gradle.properties`) is only yours.

| Property                  | Values           | Does                                                                         |
|---------------------------|------------------|------------------------------------------------------------------------------|
| `flixel.profile`          | A profile name   | Applies that `[profile.<name>]` table of `project.toml`. A profile task such as `:desktop:playtest` selects its profile without it. See [Profiles](#profiles). |
| `enableBasisuCompression` | `true`, `false`  | Compresses Android textures with Basis Universal. See [Compressed textures](#compressed-textures-with-basis-universal). |

The project plugin also sets `flixel.project.title` and `flixel.project.version` on every Gradle project, so the
HTML5 and packagr plugins can default to the game's title and version without depending on it. Read them if a
build script of your own needs them, but set the values in `project.toml`, not here.

### Runtime settings

The game reads these when it starts. How to pass one depends on the platform:

| Setting                 | Platforms                | Values                                                               | Does |
|-------------------------|--------------------------|----------------------------------------------------------------------|------|
| `flixel.mode`           | Desktop, web, Android    | `release`, `debug`, `test`                                           | Picks the runtime mode for one launch. It wins over a profile's `app.mode`, which wins over the platform's choice (release, or debug for a debuggable Android build). See [Debugging](debugging.md). |
| `flixel.debugHost`      | Desktop, web, Android    | `host` or `host:port` (default `127.0.0.1:6510`)                     | Where the game looks for the remote debugger app in debug mode. |
| `flixel.render.backend` | Desktop                  | `auto` (default), `vulkan`, `metal`, `direct3d11`, `direct3d12`, `opengl`, `opengles`, `noop` | Forces the graphics API WebGPU uses. Useful when a machine's preferred driver misbehaves. An unknown value logs a warning and uses `auto`. |
| `flixel.render.stats`   | Desktop                  | `log` (or `true`)                                                    | Logs renderer statistics (frame times, memory, pipelines, textures) once a second. |
| `flixel.log.color`      | Desktop                  | `false`                                                              | Turns off colored console logs. Setting the `NO_COLOR` environment variable does the same. |

Ways to pass them:

| Where                          | How                                                                           |
|--------------------------------|-------------------------------------------------------------------------------|
| A packaged or plain desktop jar | `java -Dflixel.render.backend=opengl -jar mygame.jar`                        |
| Every packagr package          | `packagr { jvmArg("-Dflixel.render.backend=opengl") }` in the desktop build file |
| The `:desktop:run` task        | `tasks.named<JavaExec>("run") { systemProperty("flixel.render.stats", "log") }` in `desktop/build.gradle.kts` |
| The web                        | Add it to the page address: `?flixel.mode=debug&flixel.debugHost=192.168.1.20:6510` |
| Android                        | An intent extra: `adb shell am start -n <package>/<activity> -e flixel.mode debug` |

Note that `./gradlew :desktop:run -Dflixel.mode=debug` does **not** reach the game: `-D` on the Gradle command line
sets the property for Gradle itself, not for the game it starts. Use the `:desktop:debug` task or `systemProperty`
instead.

## Release checklist

- [ ] The game runs in release mode (no debug flags in the shipped launch command).
- [ ] The game was tested on every platform you ship, including a real mid-range phone for Android.
- [ ] The web build was tested in the browsers your players use, with WebGPU.
- [ ] Asset names are lowercase, with forward slashes.
- [ ] Progress is saved when it changes and on `gameSuspended`, not only on quit.
- [ ] Source art and debug files are excluded from builds.
- [ ] `project.toml` has the final title, company, and version.
- [ ] `./gradlew bumpBuild` raised the build number for this release.
- [ ] `./gradlew doctor` reports no problems on the machine that builds the release.
- [ ] The release is built without a development profile (no `flixel.profile=dev` left in `gradle.properties`).
- [ ] Desktop packages were built for every target, and each one was started at least once.
- [ ] Android release builds are signed; textures are compressed if memory matters.

## Check your understanding

1. Why do players not need Java installed to run a packagr package?
2. A shader typo breaks the game only on players' machines. Which plugin would have caught it, and when?
3. After an update, every player's save seems gone. What might you have changed?
4. You fixed one typo and want to upload the fix to Google Play. Do you need to raise `build`?

(Answers: each package bundles a trimmed Java runtime and a native launcher; the shader plugin, during the build; the
company or title in `FlixelConfig`, which decide where saves are stored; yes, every build given to players needs a
higher build number, however small the change.)

**Next:** [Architecture](architecture.md)
