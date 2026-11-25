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

[defines]                    # optional, your own constants
demo      = false
max_saves = 3
```

`platforms` is a top-level key, so it has to come before the first `[table]`. Tables and keys the plugin does not
know (such as an `[installed]` table that tools write) are ignored.

## The values

| Key                      | Required | Default                | Notes                                                                 |
|--------------------------|----------|------------------------|-----------------------------------------------------------------------|
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
   | `game.profile`     | The selected profile, or an empty string, see profiles below.          |
   | `game.defines`     | The `[defines]` table, looked up by key, such as `game.defines["demo"]`. |
   | `game.model`       | Every value, including the `[render]` ones.                            |

   The extension is also available as `flixelProject`, the name earlier versions used, so older build scripts keep
   working. New scripts should use `game`.

3. **Sets defaults for the other plugins.** The `org.flixelgdx.html5` plugin uses the title as its page title, and the
   `org.flixelgdx.packagr` plugin uses the title as its application name and (through `version`) the version.
   Anything set in a build file still wins.
4. **Adds a `bumpBuild` task** to the root project, see the build number below.
5. **Generates a `Project` class** for the `core` module when it has the `java` plugin, before the code compiles.

## The generated class

With the file above, the `core` module gets `com.example.mygame.Project`:

```java
public final class Project {
  public static final String TITLE = "My Game";
  public static final String COMPANY = "My Studio";
  public static final String VERSION = "0.1.0";
  public static final int BUILD = 1;
  public static final String PROFILE = "";   // the selected profile, see below
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
folder, `build/generated/sources/flixelProject/java/main`, is a Java source folder of the `main` source set, so
compiling (Java or Kotlin) always runs it first. The class is also written whenever an IDE reloads the Gradle
project, so `Project` resolves right after a sync without a build.

## Your own constants: `[defines]`

Most games have a few values that change between builds or that belong in one obvious place: a demo flag, a cheats
switch, a server address, the number of save slots. Put them in a `[defines]` table:

```toml
[defines]
demo          = false
enable_cheats = false
max_saves     = 3
gravity       = 980.0
leaderboard   = "https://scores.example.com/my-game"
```

and they become constants in a nested `Defines` class of `Project`:

```java
public static final class Defines {
  public static final boolean DEMO = false;
  public static final boolean ENABLE_CHEATS = false;
  public static final int MAX_SAVES = 3;
  public static final float GRAVITY = 980.0f;
  public static final String LEADERBOARD = "https://scores.example.com/my-game";
}
```

```java
import com.example.mygame.Project.Defines;

if (Defines.ENABLE_CHEATS && Flixel.keys.justPressed(FlixelKey.F1)) {
  player.health = 999;
}
```

Because they are compile-time constants, the compiler removes code behind a flag that is `false`. In the example, the
cheat code is not in the built game at all, so flags like these cost nothing at runtime.

| TOML value             | Java type                                      |
|------------------------|------------------------------------------------|
| `true` or `false`      | `boolean`                                      |
| A whole number         | `int`, or `long` when it does not fit an `int` |
| A decimal number       | `float`                                        |
| A string in quotes     | `String`                                       |

Arrays, nested tables and dates are not supported and fail the build with the key that is wrong. Keys turn into
UPPER_SNAKE_CASE with the same rules as the assets plugin: `max_saves`, `max-saves` and `maxSaves` all become
`MAX_SAVES`, and a key that starts with a digit gets a leading `_`. Two keys that turn into the same name fail the build.
The constants live in their own `Defines` class, so they can never clash with `Project`'s own constants.

Build scripts can read the same values, so a flag can change the build too:

```kotlin
// desktop/build.gradle.kts
if (game.defines["demo"] == true) {
  packagr { appName = "My Game Demo" }
}
```

Defines end up in the built game as plain text, so they are not a place for passwords or secret keys.

## Profiles

Often you want the same game with a few lines changed: windowed with cheats while you develop, or a demo with its own
title. A profile is a named set of overrides, written as a `[profile.<name>]` table. Think of the rest of the file as
a recipe and each profile as a sticky note that changes only the lines it mentions:

```toml
[app]
title = "My Game"

[window]
fullscreen = true

[defines]
demo          = false
enable_cheats = false

# Local development: windowed, cheats on.
[profile.dev]
window.fullscreen     = false
defines.enable_cheats = true

# The free demo: its own title, the demo flag on, and no Android build.
[profile.demo]
app.title    = "My Game Demo"
defines.demo = true
platforms    = ["desktop", "html5"]
```

Select one with the `flixel.profile` Gradle property:

```
./gradlew desktop:run -Pflixel.profile=dev
./gradlew desktop:packagr -Pflixel.profile=demo
./gradlew desktop:packagr                         # no profile: the file as written
```

To use one by default on your own computer, put `flixel.profile=dev` in a `gradle.properties` that is not committed,
such as the one in your Gradle user home (`~/.gradle/gradle.properties`). Every other Gradle build ignores it, because
of the `flixel.` prefix.

- A profile can change any value in this README except `app.package`, because the game's code imports the generated
  `Project` class from that package. For a separate Android app ID, use `applicationIdSuffix` in the Android module.
- A profile can change defines the file already has (they keep their place) and add new ones (they come after the
  file's own).
- `Project.PROFILE` holds the selected profile's name, or an empty string, so the game can show it:

  ```java
  if (!Project.PROFILE.isEmpty()) {
    add(new FlixelText(4, 4, 0, "[" + Project.PROFILE + "]"));
  }
  ```

- Selecting a profile the file does not have fails the build and lists the ones it has. A mistake inside a profile
  only fails the build when that profile is selected, and the message names the profile's line, such as
  `profile.demo.window.width`.
- One Gradle run uses one profile for every module, so building the demo and the full game takes two runs. Android's
  own build types and flavors still work as usual alongside profiles.

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

Before a release, raise it with:

```
$ ./gradlew bumpBuild
project.toml: app.build 14 -> 15
```

Only the number changes, so comments and spacing stay as they were. When the file has no `app.build` yet, the task
adds `build = 2` to the `[app]` table. If a build script already has its own task named `bumpBuild`, that one is kept
and the plugin does not add its own.
