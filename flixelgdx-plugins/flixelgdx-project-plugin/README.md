# FlixelGDX Project Plugin

Describe your game once, in a `project.toml` file, and let the build read it everywhere.

Without this plugin, the same facts (the title, the version, the window size) are typed into several build files and
the game's code, and they slowly drift apart. This plugin makes `project.toml` the single source of truth: it picks
the modules to build, sets the group and version, and generates a `Project` class so the game's `FlixelConfig` is
filled in from the file. The file can be written by hand or by tools.

## Usage

Apply the plugin in `settings.gradle` (it is a settings plugin, not a project plugin):

```groovy
// settings.gradle
plugins {
  id 'org.flixelgdx.project' version '<flixel-version>'
}
```

Then put a `project.toml` next to the settings file:

```toml
platforms = ["desktop", "html5", "android"]

[app]
title    = "My Game"
package  = "com.example.mygame"
company  = "My Studio"
version  = "0.1.0"
build    = 1                 # optional, raise it with every release
language = "java"            # or "kotlin"

[window]
width      = 640
height     = 360
framerate  = 60
vsync      = true
fullscreen = false

[render]                     # optional
resolution  = [0, 0]         # 0 means use the window size
scale       = 1.0
smooth      = true
transparent = false
```

`platforms` is a top-level key, so it has to come before the first `[table]`. Tables and keys the plugin does not
know (such as an `[installed]` table that tools write) are ignored.

## The values

| Key                      | Required | Default                | Notes                                                                 |
|--------------------------|----------|------------------------|-------------------------------------------------------------------------------------------|
| `app.title`              | yes      |                        | Printable ASCII, see the naming rules below.                          |
| `app.company`            | yes      |                        | Same rules as the title.                                              |
| `app.version`            | yes      |                        | Letters, digits, `.`, `+` and `-`, and it starts with a digit.        |
| `app.package`            | yes      |                        | A lowercase Java package such as `com.example.mygame`.                |
| `app.build`              | no       | `1`                    | A whole number from 1 to 2100000000, see the build number below.      |
| `app.language`           | no       | `java`                 | `java` or `kotlin`.                                                   |
| `window.width`           | no       | `640`                  | Greater than 0.                                                       |
| `window.height`          | no       | `360`                  | Greater than 0.                                                       |
| `window.framerate`       | no       | `60`                   | Greater than 0.                                                       |
| `window.vsync`           | no       | `true`                 |                                                                       |
| `window.fullscreen`      | no       | `false`                |                                                                       |
| `render.resolution`      | no       | `[0, 0]`               | Two numbers, 0 or more. 0 means the window size. `false` turns the render resolution off. |
| `render.scale`           | no       | `1.0`                  | Greater than 0.                                                       |
| `render.smooth`          | no       | `true`                 | `false` for crisp nearest-neighbor stretching (pixel art).            |
| `render.transparent`     | no       | `false`                | Requests an alpha-capable framebuffer.                                |
| `platforms`              | no       | `["desktop"]`          | Any of `desktop`, `html5` and `android`. `ios` is not supported yet.  |

The defaults are the ones of `FlixelConfig.Builder`, so leaving a value out behaves like never setting it.

### Naming rules for the title and company

These end up in folder names, executables and save paths on every operating system, so they must:

- use printable ASCII only (space through `~`),
- not be blank, and not start or end with a space or a dot,
- not contain any of `< > : " / \ | ? *`,
- not be a reserved Windows name (`CON`, `PRN`, `AUX`, `NUL`, `COM1` to `COM9`, `LPT1` to `LPT9`), in any letter case.

A value that breaks a rule fails the build with a message that names the field and the offending character.

## What the plugin does

1. **Includes the modules.** `core` is always included, plus every listed platform. Each module's folder must exist,
   or the build fails and says which one is missing. `android` is only included when an Android SDK is found
   (`sdk.dir` in `local.properties`, or the `ANDROID_HOME` or `ANDROID_SDK_ROOT` environment variable pointing to a
   folder). Otherwise it is skipped with a warning that explains how to point to one.
2. **Sets the project info.** Every project gets `group` set to the package and `version` set to the version, and a
   read-only `game` extension with the values:

   ```kotlin
   // android/build.gradle.kts
   android {
     defaultConfig {
       applicationId = game.packageName
       versionName = game.version
     }
   }
   ```

   | Property           | Value                                                                  |
   |--------------------|------------------------------------------------------------------------|
   | `game.title`       | `app.title`                                                            |
   | `game.packageName` | `app.package`                                                          |
   | `game.company`     | `app.company`                                                          |
   | `game.version`     | `app.version`                                                          |
   | `game.build`       | `app.build`                                                            |
   | `game.language`    | `app.language`                                                         |
   | `game.width`, `game.height`, `game.framerate` | The `[window]` numbers.                        |
   | `game.isVsync`, `game.isFullscreen` | The `[window]` switches (`game.vsync` and `game.fullscreen` in Groovy). |
   | `game.platforms`   | `platforms`                                                            |
   | `game.model`       | Every value, including the `[render]` ones.                            |

   The extension is also available as `flixelProject`, the name earlier versions used, so older build scripts keep
   working. New scripts should use `game`.

3. **Sets defaults for the other plugins.** The `org.flixelgdx.html5` plugin uses the title as its page title, and the
   `org.flixelgdx.packagr` plugin uses the title as its application name and (through `version`) the version.
   Anything set in a build file still wins.
4. **Adds a `bumpBuild` task** to the root project, which raises the build number and the version, see the build
   number below.
5. **Generates a `Project` class** for the `core` module when it has the `java` plugin, before the code compiles.

## The generated class

With the file above, the `core` module gets `com.example.mygame.Project`:

```java
public final class Project {
  public static final String TITLE = "My Game";
  public static final String COMPANY = "My Studio";
  public static final String VERSION = "0.1.0";
  public static final int BUILD = 1;
  public static final int WIDTH = 640;
  // HEIGHT, FRAMERATE, VSYNC and FULLSCREEN, plus RENDER_* values when there is a [render] table

  public static FlixelConfig.Builder config() { ... }
}
```

`config()` returns a builder that is already filled in, so a game can start with the file's values and tweak what it
needs:

```java
FlixelConfig config = Project.config().framerate(144).build();
```

The task is called `generateProjectClass`. It is cacheable and reruns only when `project.toml` changes. Its output
folder, `build/generated/flixelgdx/project`, is a Java source folder of the `main` source set, so
compiling (Java or Kotlin) always runs it first. The class is also written whenever an IDE reloads the Gradle
project, so `Project` resolves right after a sync without a build.

## The build number

`app.version` is a label for people, such as `0.3.0`. Nothing compares it. `app.build` is a plain whole number that
goes up by one with every release, so it can be compared:

- **Android** uses it as the `versionCode`. Google Play rejects an upload whose code is not higher than the last one,
  and a phone refuses to install an APK over one with a higher code.
- **Game code** gets it as `Project.BUILD`, which is handy for upgrading old save files and for bug reports:

  ```java
  int savedBuild = Flixel.save.getInt("build", Project.BUILD);
  if (savedBuild < 10) {
    // Build 10 renamed "coins" to "gold", so older saves are fixed up once.
    Flixel.save.data.put("gold", Flixel.save.getInt("coins", 0));
    Flixel.save.data.remove("coins");
  }
  Flixel.save.data.put("build", Project.BUILD);
  ```

Read it in the Android module so it never drifts from the file:

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

Before a release, raise it with `bumpBuild`. The version people see usually changes along with it, so the task
also raises the last number of `app.version` by one:

```
$ ./gradlew bumpBuild
project.toml: app.build 14 -> 15
project.toml: app.version 0.3.0 -> 0.3.1
```

For a bigger step, such as a new minor or major version, give the new version with `--new-version` (Gradle already
uses `--version` for its own version):

```
$ ./gradlew bumpBuild --new-version=1.0.0
project.toml: app.build 15 -> 16
project.toml: app.version 0.3.1 -> 1.0.0
```

Only those two values change, so comments and spacing stay as they were. When the file has no `app.build` yet, the
task adds `build = 2` to the `[app]` table. A version that does not end in a number, such as `1.0-beta`, has no
obvious next value, so it is left alone unless `--new-version` is given. If a build script already has its own task named `bumpBuild`, that one is kept
and the plugin does not add its own.
