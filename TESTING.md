# Compiling & Testing

Since FlixelGDX is a framework and not a standalone game, it cannot be run directly. To test your changes, you should use the framework as a local dependency in a test project.

## Compiling FlixelGDX

To build the framework and install it to your local Maven repository (usually located at `~/.m2/repository`), run the following command in the project root:

```bash
./gradlew publishToMavenLocal
```

This will make the framework available to other Gradle projects on your machine.

The published coordinates follow the pattern:

- **Group**: `me.stringdotjar.flixelgdx`
- **Artifact**: `core` (for the framework API your game code uses)
- **Version**: defined in this repository's Gradle configuration (see `gradle.properties`).

## Testing with a Test Project

You can test FlixelGDX using either a project created with **gdx-liftoff** or by manually setting up a test project in **IntelliJ IDEA**.

### Method 1: Using gdx-liftoff

[gdx-liftoff](https://github.com/libgdx/gdx-liftoff) is the recommended tool for creating new libGDX projects.

1.  **Create a New Project**: Use gdx-liftoff to generate a new libGDX project.
2.  **Add Maven Local**: In the root `build.gradle` of your new project, ensure `mavenLocal()` is included in the `repositories` block:

    ```gradle
    allprojects {
      repositories {
        mavenLocal()
        mavenCentral()
        // ... other repositories
      }
    }
    ```

3.  **Add FlixelGDX Dependency**: In the `core` module's `build.gradle`, add FlixelGDX as a dependency:

    ```gradle
    dependencies {
      implementation "me.stringdotjar.flixelgdx:core:1.0.0" // Replace with the current version.
      // ... other dependencies
    }
    ```

4.  **Refresh Gradle**: Refresh your project in your IDE. You can now start using FlixelGDX classes in your test project.

At this point your test project can:

- extend `FlixelGame` as your main game class,
- create one or more `FlixelState` subclasses (e.g. `MenuState`, `PlayState`),
- and wire them into your existing desktop/Android launchers just like any other libGDX `ApplicationListener` or `Screen`.

### Method 2: Using IntelliJ IDEA (Composite Builds)

IntelliJ IDEA's **Composite Builds** feature allows you to include FlixelGDX as a dependency without publishing it to Maven Local.

1.  **Open Your Test Project**: Open your libGDX test project in IntelliJ.
2.  **Link FlixelGDX**: Go to **File > Project Structure > Modules**.
3.  **Add Module**: Click the **+** icon and select **Import Module**.
4.  **Select FlixelGDX Root**: Navigate to the root directory of your FlixelGDX clone and select it.
5.  **Configure Dependency**: In your test project's `settings.gradle`, add the following:

    ```gradle
    includeBuild('/path/to/flixelgdx') {
      dependencySubstitution {
        substitute module('me.stringdotjar.flixelgdx:core') using project(':core')
      }
    }
    ```

This method is often faster for development as it avoids the need to run `publishToMavenLocal` every time you make a change.

With a composite build, your test game's `core` module can still declare:

```gradle
dependencies {
  implementation 'me.stringdotjar.flixelgdx:core'
}
```

and Gradle will substitute that module with the local `:core` project from your FlixelGDX checkout, so changes to the framework are picked up immediately when you re-run or debug the game.

## Verification

After adding the dependency, create a simple `FlixelGame` to verify that everything is working correctly:

```java
public class MyTestGame extends FlixelGame {

  @Override
  public void create() {
    super.create();
    Flixel.switchState(new MyTestState());
  }
}
```

If you can compile and run this in your test project, you're all set!

From here you can:

- add more `FlixelState` subclasses to cover different parts of your game,
- try out the tweening system (`FlixelTween`, `FlixelTweenSettings`) inside those states,
- and iterate on FlixelGDX itself while immediately seeing the effects inside your test game.
