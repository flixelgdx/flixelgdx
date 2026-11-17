# Getting Started: Your First Project and Your First Game

This guide takes you from nothing to a running game: setting up the tools, creating a project, understanding what
each folder is for, and writing a tiny game where you move a square with the arrow keys.

## Table of contents

1. [What you need](#what-you-need)
2. [Creating a project](#creating-a-project)
3. [A tour of the project](#a-tour-of-the-project)
4. [Reading the handbook inside your project](#reading-the-handbook-inside-your-project)
5. [Running the game](#running-the-game)
6. [Your first game in two classes](#your-first-game-in-two-classes)
7. [Understanding the coordinate system](#understanding-the-coordinate-system)
8. [Runtime modes: release, debug, and test](#runtime-modes-release-debug-and-test)
9. [Adding FlixelGDX to an existing project](#adding-flixelgdx-to-an-existing-project)
10. [Common first-day problems](#common-first-day-problems)
11. [Where to go next](#where-to-go-next)

## What you need

- **A Java 25 JDK.** [Eclipse Temurin](https://adoptium.net/temurin/releases?version=25) is the recommended
  distribution. You usually do not need to install it yourself: the new project asks Gradle to download a
  matching JDK the first time you build.
- **An IDE.** IntelliJ IDEA, Eclipse, or VS Code with the Java extensions. Anything that understands Gradle projects
  works.
- **(Optional) The Android SDK**, only if you want to build for Android.
- **The `flxgdx` command line tool**, which creates and manages projects. See the next section.

You do not need to install Gradle. Every project ships with the Gradle wrapper (`gradlew`), which downloads the right
version of Gradle by itself.

## Creating a project

The recommended way to start is `flxgdx`, the FlixelGDX command line tool
([source and issues](https://github.com/flixelgdx/flixelgdx-cli)). Think of it as a cake mix: all the boring
ingredients (Gradle files, launchers, platform settings) are measured out for you, and you only add the fun part.

### Installing flxgdx

For now, `flxgdx` is installed by building it from source, which needs [Go](https://go.dev/dl/). Either let Go do it
for you:

```bash
go install github.com/flixelgdx/flixelgdx-cli/cmd/flxgdx@latest
```

or clone the repository and build it yourself:

```bash
git clone https://github.com/flixelgdx/flixelgdx-cli
cd flixelgdx-cli
go build ./cmd/flxgdx
```

`go install` puts the program in `~/go/bin` (or `$GOBIN`), so make sure that folder is on your `PATH`. Check it works
with `flxgdx version`. Installing through package managers is planned for later.

If you use IntelliJ IDEA, there is also a [plugin](https://github.com/flixelgdx/flixelgdx-intellij) with a New Project
wizard that uses `flxgdx` for you. It is not published on the JetBrains Marketplace yet, so you build it from its
repository.

### Making the project

```bash
flxgdx new my-game --title "My Game" --company "My Studio" --app-version 0.1.0
```

The title, company, and app version are required. When one is missing and you are at a terminal, `flxgdx` asks for
it. A few useful extras:

| Flag                          | What it does                                                          |
|-------------------------------|-----------------------------------------------------------------------|
| `--package com.example.mygame` | The Java package. By default it is made from the company and title.  |
| `--platforms desktop,html5,android` | Which platforms to create. The default is `desktop,html5`.      |
| `--language kotlin`           | Use Kotlin instead of Java.                                           |
| `--width 640 --height 360`    | The window size.                                                      |
| `--handbook`                  | Also copy the framework's handbook into the project.                  |

Run `flxgdx new --help` to see every flag. Then open the new folder in your IDE.

## A tour of the project

```
my-game/
  project.toml        The single source of truth: app info, window, render settings, and platforms.
  assets/             Images, sounds, fonts, and data files shared by every platform.
  core/               Your actual game code. This is where you spend almost all your time.
  desktop/            A tiny launcher that starts the game in a desktop window.
  html5/              A tiny launcher that starts the game in a web browser.
  android/            A tiny launcher that starts the game on Android (if you chose it).
  buildSrc/           Shared Gradle build logic used by every module.
  gradle/
    libs.versions.toml  The version catalog: every library and plugin version, grouped into bundles.
  settings.gradle.kts Applies the project plugin and lists the modules.
  gradlew             The Gradle wrapper (gradlew.bat on Windows).
  handbooks/          Offline docs, only if you installed them (see below).
```

The important idea is the split between `core` and the launchers. Your game is written once, in `core`, and it only
depends on `flixelgdx-core`. Each platform module is a thin plug that connects that shared game to one kind of
device. Think of `core` as a game cartridge and each launcher as a different console it slots into.

### project.toml

`project.toml` describes your game once, so the build and your code never disagree. It holds the title, company,
version, and package, the window size and frame rate, the render settings, and the list of platforms. The
`org.flixelgdx.project` Gradle settings plugin reads it, includes the right modules, and generates a small `Project`
class in `core` that carries those values into your code. To change the window size, edit `project.toml`, not Java
code:

```toml
platforms = ["desktop", "html5"]

[app]
title   = "My Game"
company = "My Studio"
version = "0.1.0"

[window]
width  = 640
height = 360
```

The [project plugin README](../flixelgdx-plugins/flixelgdx-project-plugin/README.md) lists every key.

### Dependencies

Libraries and Gradle plugins live in `gradle/libs.versions.toml`, grouped into bundles that the module build files
use. You rarely edit it by hand. Let `flxgdx` do it:

```bash
flxgdx add library --repo flixelgdx/flixelgdx-ui   # add a library from a GitHub repository
flxgdx add plugin --repo owner/repo                # add a Gradle plugin
flxgdx add platform android                        # add a platform to an existing project
flxgdx remove library --repo flixelgdx/flixelgdx-ui
flxgdx upgrade                                     # move installed packages to their latest release
```

A library tells `flxgdx` how to install it with a small `flxgdx.yml` file in its repository. If you write libraries,
see the [manifest format](https://github.com/flixelgdx/flixelgdx-cli/blob/master/MANIFEST.md).

### The launchers

Each platform has a one-line launcher. It installs everything that platform needs (window, graphics, audio, input,
files, saving, logging) and then starts your game. You never start the framework by hand.

```java
// desktop/src/main/java/com/example/mygame/DesktopLauncher.java
public final class DesktopLauncher {
  public static void main(String[] args) {
    FlixelDesktopLauncher.launch(new MyGame());
  }

  private DesktopLauncher() {}
}
```

```java
// html5/src/main/java/com/example/mygame/WebLauncher.java
public final class WebLauncher {
  public static void main(String[] args) {
    FlixelHtml5Launcher.launch(new MyGame());
  }

  private WebLauncher() {}
}
```

```java
// android/src/main/java/com/example/mygame/GameActivity.java
public final class GameActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    FlixelAndroidLauncher.launch(this, new MyGame());
  }
}
```

The desktop launcher can also take window icons from your resources folder, listed from smallest to largest:

```java
FlixelDesktopLauncher.launch(new MyGame(), "icons/icon16.png", "icons/icon32.png", "icons/icon256.png");
```

What actually happens inside that one line is described in [The Boot Sequence](boot-sequence.md).

## Reading the handbook inside your project

A library can ship its own documentation. `flxgdx install handbook` copies it into your project, so you can read it
offline, next to the code, and so can your editor and tools:

```bash
flxgdx install handbook --repo flixelgdx/flixelgdx
```

The docs go to `handbooks/<repo-name>/` (here, `handbooks/flixelgdx/`). They match the version of that library that
is installed in your project, so they never describe a different release than the one you are using. Any library
whose `flxgdx.yml` has a `handbook` entry works the same way. Run the command again after `flxgdx upgrade` to refresh
the docs, and use `flxgdx uninstall handbook --repo flixelgdx/flixelgdx` to remove them. You can also pass
`--handbook` to `flxgdx new` to get them from the start.

## Running the game

Open a terminal in the project folder and run one of these:

```bash
flxgdx run desktop
flxgdx run html5
flxgdx run android
```

`flxgdx run` runs the Gradle wrapper of your project for you, so these are the same as the Gradle tasks:

```bash
./gradlew :desktop:run
./gradlew :html5:run
./gradlew :android:installDebug
```

Add `--debug` to `flxgdx run desktop` or `flxgdx run html5` to start in debug mode. On Windows, use `gradlew.bat`
instead of `./gradlew`. The first build takes a little longer because Gradle downloads
the JDK and the framework. After that you should see an empty game window, which means everything is wired
correctly. The web task builds the game, starts a small local server, and opens your browser.

## Your first game in two classes

A FlixelGDX game needs exactly two classes of your own: a game class and a first state. Let's build a white square
you can move with the arrow keys.

### Step 1: the game class

Subclass `FlixelGame` and hand its constructor two things: a `FlixelConfig` describing the window, and a "recipe"
for the first state to show. A project made by `flxgdx` already has this class. The window values come from
`project.toml` through the generated `Project` class, so there is nothing to type twice:

```java
package com.example.mygame;

import org.flixelgdx.FlixelGame;

public class MyGame extends FlixelGame {

  public MyGame() {
    super(Project.config().build(), PlayState::new); // PlayState::new is the first state to show.
  }
}
```

`Project.config()` returns a `FlixelConfig.Builder` that is already filled in from `project.toml`, so you can still
tweak a value in code before building, for example `Project.config().framerate(144).build()`.

If your project does not use `project.toml`, build the config by hand with `FlixelConfig.Builder` instead. It works
the same way:

```java
super(
    new FlixelConfig.Builder("My Game")
        .company("My Studio")    // Used to pick the save folder. Required for saving.
        .version("1.0.0")
        .size(640, 360)          // The design resolution, in game pixels.
        .framerate(60)
        .vsync(true)
        .build(),
    PlayState::new);             // The first state to show.
```

Two details are worth understanding right away:

- **The size is a design resolution.** You write every position as if the screen is always 640 by 360 pixels. When
  the real window is bigger or smaller, the framework scales the picture for you, so your coordinates never change.
  Ask for the design size at runtime with `Flixel.getDesignWidth()` and `Flixel.getDesignHeight()`.
- **`PlayState::new` is a recipe, not a state.** The framework asks for a `Supplier<FlixelState>`, a small function
  that builds a fresh state when called. That way the framework decides *when* the state is created (after the window
  and graphics are ready), and it can build a fresh copy later, for example when you call `Flixel.resetState()`.

If you do not need a company name or version yet, there are shorter constructors:
`super("My Game", PlayState::new)` and `super("My Game", 640, 360, PlayState::new)`. Note that saving needs a company
name, so most real games use the builder.

### Step 2: the first state

Subclass `FlixelState`. Build your scene in `create()` and run your game logic in `update(float)`.

```java
package com.example.mygame;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelSprite;
import org.flixelgdx.FlixelState;
import org.flixelgdx.input.keyboard.FlixelKey;
import org.flixelgdx.util.FlixelColor;

public class PlayState extends FlixelState {

  private FlixelSprite player;

  @Override
  public void create() {
    super.create();

    player = new FlixelSprite();
    player.makeGraphic(16, 16, FlixelColor.WHITE); // A plain white square.
    player.screenCenter();
    add(player); // Without add(), the sprite is never updated or drawn.
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed); // Updates every member that was add()-ed. Do not forget it!

    float speed = 200f * elapsed; // 200 pixels per second.
    if (Flixel.keys.pressed(FlixelKey.LEFT)) {
      player.changeX(-speed);
    }
    if (Flixel.keys.pressed(FlixelKey.RIGHT)) {
      player.changeX(speed);
    }
    if (Flixel.keys.pressed(FlixelKey.UP)) {
      player.changeY(-speed);
    }
    if (Flixel.keys.pressed(FlixelKey.DOWN)) {
      player.changeY(speed);
    }
  }
}
```

### Step 3: run it

Make sure your launcher creates `new MyGame()` and run `flxgdx run desktop`. You now have a white square you can
move with the arrow keys. Everything else in this handbook builds on these two classes.

### Step 4: make it yours

Try these small changes to get a feel for the framework:

```java
// In create(): a dark blue background.
setBgColor(new FlixelColor(0.1f, 0.1f, 0.2f, 1f));

// In create(): load a real image from assets/images/player.png instead of a square.
player.loadGraphic(Flixel.files.internal("images/player.png"));

// In update(...): restart the state with R.
if (Flixel.keys.justPressed(FlixelKey.R)) {
  Flixel.resetState();
}
```

## Understanding the coordinate system

The origin `(0, 0)` is the **top-left** corner of the screen. X grows to the right and Y grows **downward**, the same
as most 2D drawing tools.

```
(0, 0) ----------------------> x
  |
  |      (100, 50) is 100 right, 50 down.
  |
  v
  y
```

So "move up" means making Y smaller, and gravity is a positive Y acceleration. A sprite's `(x, y)` is its own
top-left corner, not its center. Use `getMidpointX()` and `getMidpointY()` when you want the center.

## Runtime modes: release, debug, and test

A game runs in one of three modes. Release is the default and what players get. Debug turns on extra checks and
connects the game to the [debugger app](debugging.md). Test is for automated tests. You pick the mode without
rebuilding:

| Platform | How to pick debug mode                                                              |
|----------|-------------------------------------------------------------------------------------|
| Desktop  | Pass `-Dflixel.mode=debug` to the JVM (new projects have a `:desktop:debug` task). |
| Web      | Add `?flixel.mode=debug` (or just `?debug`) to the page address, or run `:html5:debug`. |
| Android  | Any debuggable build runs in debug mode automatically.                              |

Your code can ask which mode it is in with `Flixel.isDebugMode()`. Because the mode comes from how the game is
launched, there is nothing to remember to remove before you publish: a shipped build simply has no flag and runs in
release mode.

## Adding FlixelGDX to an existing project

If your project was not created with `flxgdx`, the pieces are published to Maven Central under the group `org.flixelgdx`. Your
shared game module depends on `flixelgdx-core`, and each platform module depends on its backend:

```groovy
// core/build.gradle
dependencies {
  api "org.flixelgdx:flixelgdx-core:$flixelVersion"
}

// desktop/build.gradle
dependencies {
  implementation project(':core')
  implementation "org.flixelgdx:flixelgdx-desktop:$flixelVersion"
}
```

The web module also needs the TeaVM and FlixelGDX HTML5 Gradle plugins, and the Android module needs the Android
Gradle plugin. Copying these from a project made with `flxgdx new` is far easier than writing them by hand, which is why
`flxgdx` is the recommended path. The `flxgdx add`, `run`, and `upgrade` commands only work in projects that have a
`project.toml`. See [Building and Shipping](building-and-shipping.md) for the plugins.

## Common first-day problems

| Symptom                                           | Likely cause and fix                                                                                   |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------|
| A sprite never appears.                           | You forgot `add(sprite)`, or it is off screen. Try `sprite.screenCenter()`.                            |
| Sprites stop moving or animating.                 | Your `update(...)` override does not call `super.update(elapsed)`.                                     |
| `NullPointerException` on `Flixel.keys` or similar. | You used a service in a static initializer or your game's constructor. Use it in `create()` or later. |
| An image loads on desktop but not on the web.     | File names are case-sensitive on the web and Android. Keep asset names lowercase and use forward slashes. |
| Saving does nothing and logs an error.            | Set `company` in `project.toml` (or `.company(...)` in your `FlixelConfig.Builder`).                   |
| The game is faster on one computer than another.  | You moved things by a fixed amount per frame. Multiply speeds by `elapsed`.                            |
| Desktop warns about native access.                | Start the JVM with `--enable-native-access=ALL-UNNAMED`. The project's run tasks and packaged games already do. |
| Something fails to build or run and the cause is unclear. | Run `flxgdx doctor`. It checks your JDK, Git, Android SDK, and project files, and prints a fix for each problem. Use `flxgdx doctor --fix` to repair what is safe to repair. |

## Where to go next

- [The Game Loop](game-loop.md) explains what `elapsed` is and when each method runs.
- [States and Substates](states-and-substates.md) shows how to build menus, levels, and pause screens.
- [Basics, Objects, and Sprites](sprites-and-objects.md) covers everything a sprite can do.

**Next:** [The Game Loop](game-loop.md)
