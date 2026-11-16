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
7. [Android: building and compressing textures](#android-building-and-compressing-textures)
8. [Keeping files out of a build](#keeping-files-out-of-a-build)
9. [Versioning your game](#versioning-your-game)
10. [Release checklist](#release-checklist)
11. [Check your understanding](#check-your-understanding)

## The plugins at a glance

| Plugin id                | Applies to     | Does                                                                         |
|--------------------------|----------------|------------------------------------------------------------------------------|
| `org.flixelgdx.packagr`  | Desktop module | Builds a double-clickable package per operating system, with its own trimmed Java runtime. |
| `org.flixelgdx.html5`    | Web module     | Copies assets, generates the page, runs a dev server, and zips a deployable web app. |
| `org.flixelgdx.shaders`  | Core module    | Checks WGSL shaders at build time and bundles them into resources.           |
| `org.flixelgdx.assets`   | Core module    | Generates a type-safe `Assets` class with a path constant for every asset.   |
| `org.flixelgdx.basisu`   | Android module | Compresses PNGs into KTX2/Basis Universal textures at build time.            |

Projects from the project generator come with the right plugins already applied.

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
  `~/.flixelgdx/jdks`. Change the version with `jreVersion` and the modules with `jlinkModules`.
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

Set the version in your config, so it is available at runtime (for a title screen, crash reports, or save migration):

```java
new FlixelConfig.Builder("My Game").company("My Studio").version("1.2.0").build();

Flixel.info("Starting {} {}", Flixel.config.getTitle(), Flixel.config.getVersion());
```

Store a version number in save files too, so a future update can recognize and convert old saves. Note that the
company and title also decide where saves are stored, so do not change them after release, or players' saves will
seem to disappear.

## Release checklist

- [ ] The game runs in release mode (no debug flags in the shipped launch command).
- [ ] The game was tested on every platform you ship, including a real mid-range phone for Android.
- [ ] The web build was tested in the browsers your players use, with WebGPU.
- [ ] Asset names are lowercase, with forward slashes.
- [ ] Progress is saved when it changes and on `gameSuspended`, not only on quit.
- [ ] Source art and debug files are excluded from builds.
- [ ] The config has the final title, company, and version.
- [ ] Desktop packages were built for every target, and each one was started at least once.
- [ ] Android release builds are signed; textures are compressed if memory matters.

## Check your understanding

1. Why do players not need Java installed to run a packagr package?
2. A shader typo breaks the game only on players' machines. Which plugin would have caught it, and when?
3. After an update, every player's save seems gone. What might you have changed?

(Answers: each package bundles a trimmed Java runtime and a native launcher; the shader plugin, during the build; the
company or title in `FlixelConfig`, which decide where saves are stored.)

**Next:** [Architecture](architecture.md)
