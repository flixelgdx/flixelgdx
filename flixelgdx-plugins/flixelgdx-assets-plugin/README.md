# FlixelGDX Assets Plugin

Turn your `assets/` folder into Java code, so a typo in an asset path breaks the build instead of the game.

Asset paths are usually plain strings. If you misspell one, or rename a file and forget a line of code, nothing
complains until the game runs and fails to load it. This plugin generates a class called `Assets` with a constant for
every file, grouped into nested classes by folder. Your IDE can autocomplete the paths, and a missing file becomes a
compile error.

## Usage

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

With this assets folder:

```
assets/
  music/
    theme.ogg
  shared/
    sprites/
      player.png
      playerIdle.png
```

the game can write:

```java
import com.mygame.Assets;

Flixel.assets.load(Assets.Shared.Sprites.PLAYER_PNG);              // "shared/sprites/player.png"
Flixel.sound.play(Flixel.files.internal(Assets.Music.THEME_OGG)); // "music/theme.ogg"
```

Every constant holds the file's path relative to `assets/`, which is exactly what `Flixel.assets` and
`Flixel.files.internal(...)` expect.

## Naming rules

| On disk                    | In code                        |
|----------------------------|--------------------------------|
| Folder `shared`            | Class `Shared`                 |
| Folder `ui_icons`          | Class `UiIcons`                |
| Folder `HUDIcons`          | Class `HudIcons`               |
| File `player.png`          | Constant `PLAYER_PNG`          |
| File `playerIdle.png`      | Constant `PLAYER_IDLE_PNG`     |
| File `boss-theme.ogg`      | Constant `BOSS_THEME_OGG`      |
| File `8bit.ogg`            | Constant `_8BIT_OGG`           |

- Folders become PascalCase classes and files become UPPER_SNAKE_CASE constants (the extension is part of the name).
- Spaces, dashes, dots and other symbols separate words, and so does a change from lower to upper case.
- A name that would start with a digit gets a leading underscore.
- Hidden files and folders (names starting with a dot, such as `.DS_Store`) are skipped.

The build fails with a clear message when:

- two files (or two folders) in the same folder turn into the same name, such as `my-song.ogg` and `my_song.ogg`;
- a folder would get the same name as a class it sits in, such as `sprites/sprites/` (Java does not allow
  `Sprites.Sprites`);
- a name has no letters or digits at all.

Rename the file, or leave it out with `excludes`.

## When the class is generated

- **Before the code compiles.** The `generateAssetsClass` task writes the class into
  `build/generated/sources/flixelgdx/assets/java/main`, which is a source folder of the `main` source set, so
  `compileJava` always runs it first. Run it alone with `./gradlew generateAssetsClass`.
- **When your IDE reloads the Gradle project.** The class is written while the project loads (IntelliJ IDEA, Android
  Studio, Eclipse and VS Code), so code completion knows about new files right after a reload. A naming problem only
  logs a warning during a reload; the next build reports it as an error.

## Settings

All settings are optional except the package when the project has no `group`.

```groovy
assetsClass {
  // The folder that is scanned (default: the assets folder at the root of the project).
  assetsDir = rootProject.layout.projectDirectory.dir('assets')

  // The package of the class (default: the project's group).
  packageName = 'com.mygame'

  // The name of the class (default: Assets).
  className = 'Assets'

  // Ant-style patterns, relative to assetsDir, for files that should not get a constant.
  excludes = ['source-art/**', '**/*.psd']
}
```
