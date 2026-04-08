# Compiling & Testing

FlixelGDX is a framework, not a standalone game, so it cannot be run by itself. To test your changes, you use the framework as a local dependency (or composite build) in a separate test project. This guide walks you through every step from a clean machine to running and testing the framework—including prerequisites, IDE setup on all major platforms, and how to avoid common mistakes.

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Getting the source](#getting-the-source)
3. [IDE setup](#ide-setup)
4. [Compiling FlixelGDX](#compiling-flixelgdx)
5. [Testing with a test project](#testing-with-a-test-project)
6. [How to test the framework properly](#how-to-test-the-framework-properly)
7. [Web (TeaVM) setup and configuration](#web-teavm-setup-and-configuration)
8. [Setting up the Android SDK (for contributing to the Android platform)](#setting-up-the-android-sdk-for-contributing-to-the-android-platform)
9. [Setting up an iOS development environment (for contributing to the iOS platform)](#setting-up-an-ios-development-environment-for-contributing-to-the-ios-platform)
10. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### 1. GitHub account and Git

- **GitHub account**: If you only need to build and test locally, you can clone the repository without an account. To contribute (fork, open PRs), create a free account at [github.com](https://github.com).
- **Git**: You need Git to clone the repository and switch branches.

| Platform | How to install Git |
|----------|--------------------|
| **Windows** | Download and run the installer from [git-scm.com](https://git-scm.com/download/win). Use the default options; “Git from the command line and also from 3rd-party software” is recommended so `git` works in Command Prompt and PowerShell. |
| **macOS** | Either install Xcode Command Line Tools (`xcode-select --install`) or install via Homebrew: `brew install git`. |
| **Linux (Ubuntu / Debian)** | `sudo apt update && sudo apt install git` |
| **Linux (Fedora / RHEL)** | `sudo dnf install git` |
| **Linux (Arch)** | `sudo pacman -S git` |
| **Linux (openSUSE)** | `sudo zypper install git` |

Verify: open a new terminal and run `git --version`.

### 2. Java (JDK 17 with OpenJ9)

FlixelGDX requires **Java 17** (LTS). The build uses the Gradle wrapper (Gradle 9.x), which runs on JDK 17+. Your IDE and command line must both use JDK 17.

**Use the OpenJ9 VM:** install **[IBM Semeru Runtime](https://developer.ibm.com/languages/java/semeru-runtimes/downloads/)** — **17 (LTS), OpenJ9** — not Oracle JDK, not Eclipse Temurin, and not the default `openjdk-17` packages on most Linux distros (those are almost always **HotSpot** and use a lot more RAM). For games, OpenJ9 is the recommended default.

After install, `java -version` should mention **Eclipse OpenJ9** (or **OpenJ9**). If you see **OpenJDK HotSpot** or **Temurin** with no OpenJ9, install Semeru instead.

#### Windows

- Download the **Windows x64** JDK 17 **with OpenJ9** from the [IBM Semeru Runtimes downloads](https://developer.ibm.com/languages/java/semeru-runtimes/downloads/) page (MSI or ZIP).  
- Run the installer. Enable “Set JAVA_HOME” / “Add to PATH” if offered.  
- **Set JAVA_HOME (if not set by installer)**  
  - **Settings → System → About → Advanced system settings → Environment Variables**.  
  - **System variables → New**: `JAVA_HOME` = Semeru install folder (e.g. `C:\Program Files\Semeru\jdk-17.x.x`).  
  - **Path** → **New** → `%JAVA_HOME%\bin`.  
  - Restart the terminal.
- **Verify**  
  - `java -version` — should show **17** and **OpenJ9**.  
  - `javac -version` — **17**.

#### macOS

- **Option A (installer)**  
  - Download **macOS** JDK 17 **OpenJ9** from [IBM Semeru Runtimes downloads](https://developer.ibm.com/languages/java/semeru-runtimes/downloads/) and install the `.pkg`.
- **Option B (Homebrew)**  
  - `brew install --cask semeru-jdk-open@17` — see [semeru-jdk-open@17](https://formulae.brew.sh/cask/semeru-jdk-open@17).  
  - Point `JAVA_HOME` at the Semeru install (e.g. under `/Library/Java/JavaVirtualMachines/`).
- **Verify**  
  - `java -version` and `javac -version` show 17 with **OpenJ9**.

#### Linux

Prefer a **Semeru 17 OpenJ9** build from [IBM Semeru Runtimes downloads](https://developer.ibm.com/languages/java/semeru-runtimes/downloads/) (`.tar.gz`), unpack to e.g. `/opt/semeru-java17`, then:

```bash
export JAVA_HOME=/opt/semeru-java17
export PATH="$JAVA_HOME/bin:$PATH"
```

Add those lines to `~/.bashrc` or `~/.profile` (or `~/.zshrc` on Zsh).

Some distributions package OpenJ9 under other names; if in doubt, use the official Semeru tarball so the VM is **OpenJ9**, not the distro’s HotSpot `openjdk-17-jdk`.

- **Verify**  
  - `java -version`, `javac -version`, and `echo $JAVA_HOME`.

---

## Getting the source

1. **Clone the repository** (replace with the actual repo URL if you use a fork):
  ```bash
   git clone https://github.com/stringdotjar/flixelgdx.git
   cd flixelgdx
  ```
2. **If you are contributing**: Fork the repo on GitHub, then clone your fork and add the upstream remote:
  ```bash
   git remote add upstream https://github.com/stringdotjar/flixelgdx.git
   git fetch upstream
   git checkout develop
  ```
3. **Use the `develop` branch** for development and PRs (see [CONTRIBUTING.md](CONTRIBUTING.md)).

---

## IDE setup

Configure your editor so it uses JDK 17 and the project’s Gradle build. Enabling EditorConfig (see [CONTRIBUTING.md](CONTRIBUTING.md)) keeps indentation and line endings consistent.

### IntelliJ IDEA

- **Install**  
  - [jetbrains.com/idea](https://www.jetbrains.com/idea/) — **IntelliJ IDEA Community** is sufficient.  
  - Windows: run the .exe installer.  
  - macOS: download .dmg, drag IntelliJ to Applications.  
  - Linux: unpack the .tar.gz or use Toolbox / Snap.
- **Open the project**  
  - **File → Open** → select the **flixelgdx** root folder (the one that contains `build.gradle` and `settings.gradle`).  
  - Choose **Open as Project**.  
  - When asked “Load Gradle project?”, choose **Load Gradle Project** and use the **Gradle wrapper** (default). Wait for indexing and dependency resolution to finish.
- **Set project JDK to 17 (OpenJ9)**  
  - **File → Project Structure → Project**: set **Project SDK** to **17**. Prefer **IBM Semeru** (OpenJ9) — add it via **Add SDK → Download JDK** and pick a **Semeru 17** build, or **Add SDK → JDK** and point to your Semeru install folder.  
  - Set **Project language level** to **17**.
- **EditorConfig**  
  - **Settings → Editor → Code Style** → enable **“Enable EditorConfig support”**.  
  - Use the **Project** code style scheme so the project’s `.editorconfig` is applied.
- **Build from IDE**  
  - **View → Tool Windows → Gradle**. Under **flixelgdx → Tasks → publishing**, run **publishToMavenLocal**.  
  - Or use the terminal inside IntelliJ: `./gradlew publishToMavenLocal` (on Windows: `gradlew.bat publishToMavenLocal`).

### VS Code / Cursor

- **Install**  
  - [code.visualstudio.com](https://code.visualstudio.com/) or [cursor.com](https://cursor.com).  
  - Windows/macOS: run the installer.  
  - Linux: .deb / .rpm from the site, or install via Snap/Flatpak.

- **Extensions**  
  - Install **“Extension Pack for Java”** (Microsoft) — it includes language support and Gradle.  
  - Optionally **“Gradle for Java”** (Microsoft) for Gradle tasks in the sidebar.  
  - For consistent style: **“EditorConfig for VS Code”** (EditorConfig.EditorConfig).


- **Open the project**  
  - **File → Open Folder** → select the **flixelgdx** root directory.
- **Select JDK 17**  
  - **Ctrl+Shift+P** (or **Cmd+Shift+P** on macOS) → **“Java: Configure Java Runtime”**.  
  - Point to JDK 17 (e.g. the path you used for `JAVA_HOME`). If needed, add a JDK 17 download from the same screen.

- **Build**  
  - Open the integrated terminal (**Ctrl+`** / **Cmd+`**) and run:  
  `./gradlew publishToMavenLocal`  
  - On Windows use `.\gradlew.bat publishToMavenLocal` if `./gradlew` is not available.

### Eclipse

- **Install**  
  - [eclipse.org/downloads](https://www.eclipse.org/downloads/) — **Eclipse IDE for Java Developers** or **Eclipse IDE for Java and DSL Developers**.  
  - Windows: unpack the zip or run the installer.  
  - macOS/Linux: unpack the tar.gz or use the installer.
- **Import as Gradle project**  
  - **File → Import → Gradle → Existing Gradle Project → Next**.  
  - **Project root directory**: browse to the **flixelgdx** root folder.  
  - Click **Finish**. Wait for Gradle to sync and build.
- **Use JDK 17**  
  - **Window → Preferences → Java → Installed JREs**: add your JDK 17 (Add → Standard VM → Directory → select JDK home). Set it as default or ensure the project uses it.  
  - **Project → Properties → Java Build Path → Libraries**: ensure the JRE is JDK 17.
- **Build**  
  - Right-click the root project → **Gradle → Refresh Gradle Project**.  
  - To publish locally: right-click project → **Run As → Gradle Build**; in **Gradle Tasks** choose **publishToMavenLocal**, or run in a terminal: `./gradlew publishToMavenLocal`.

---

## Compiling FlixelGDX

Build the framework and install it into your local Maven repository (e.g. `~/.m2/repository` on Unix or `%USERPROFILE%\.m2\repository` on Windows):

```bash
./gradlew publishToMavenLocal
```

On **Windows** in Command Prompt or PowerShell, use:

```cmd
gradlew.bat publishToMavenLocal
```

The first run may take longer while Gradle downloads the wrapper and dependencies. After it succeeds, the framework is available to other Gradle projects on your machine. The default build does **not** include the Android module and does **not** require an Android SDK; see [Setting up the Android SDK](#setting-up-the-android-sdk-for-contributing-to-the-android-platform) if you need to build the Android module.

**Published coordinates:**

- **Group**: `me.stringdotjar.flixelgdx`
- **Artifact**: `flixelgdx-core` (the library your game’s `core` module depends on)
- **Version**: from this repo’s `gradle.properties` (e.g. `projectVersion=1.0.0`)

So the dependency is: `me.stringdotjar.flixelgdx:flixelgdx-core:<version>`.

---

## Testing with a test project

You need a separate libGDX application that depends on FlixelGDX. Two ways to do that:

- **Method 1**: Create a project with **gdx-liftoff** and depend on the published artifact from `mavenLocal()`.
- **Method 2**: Use **Gradle composite build** so your test project uses the FlixelGDX source directly (no need to run `publishToMavenLocal` after every change).

### Method 1: Using gdx-liftoff

[gdx-liftoff](https://github.com/libgdx/gdx-liftoff) is the recommended way to create new libGDX projects. It is a desktop app that generates a Gradle-based project with the platforms and options you choose. The steps below describe how to create a **minimal** project (desktop-only, no extensions), then how to integrate FlixelGDX and what to select if you want to test on Android, iOS, or web as well.

#### Getting and running gdx-liftoff

1. Download the latest **gdx-liftoff-*.jar** from [gdx-liftoff Releases](https://github.com/libgdx/gdx-liftoff/releases) (e.g. `gdx-liftoff-1.14.0.7.jar`). Use the cross-platform JAR if you have a JDK installed.
2. Run it (Java 17 highly recommended):
  ```bash
   java -jar gdx-liftoff-1.14.0.7.jar
  ```
   Replace the filename with the JAR you downloaded. A setup window opens.

#### Creating a minimal project (step-by-step)

Work through the gdx-liftoff screens in order. For a **minimal** test project used only to run and test FlixelGDX on desktop, use these choices:

1. **Basic project information (first screen)**
  - **Project name**: e.g. `FlixelTest` or `MyFlixelGame`.  
  - **Package**: e.g. `com.example.flixeltest`.  
  - **Main class**: e.g. `FlixelTestGame` — this will be the class that will later extend `FlixelGame`.  
  - Click **Next** or **Project Options** to open the Add-Ons / configuration screens.
2. **Platforms (Add-Ons → Platforms)**
  - For a **minimal desktop-only** project:  
    - Check **Core** (required).  
    - Check **LWJGL3** (desktop).  
    - Leave **Android**, **iOS**, and **HTML** unchecked.
  - If you want to test FlixelGDX on other platforms later, you can add them here (or regenerate a new project with them). See [Platform options for testing](#platform-options-for-testing) below.
3. **Languages**
  - Leave **Java** selected (FlixelGDX uses Java).
4. **Extensions**
  - For a minimal project, leave all extensions **unchecked** (no Ashley, FreeType, etc.).
5. **Template**
  - For quick testing, choose **Classic** or **ApplicationAdapter** — this generates a single main class that implements `ApplicationListener`. You will replace its logic with `FlixelGame` and states.  
  - Alternatively, **Game** gives you a `Game` plus `Screen` structure; you can still make the main class extend `FlixelGame` and use `FlixelState` instead of `Screen`.
6. **Third-party libraries**
  - Leave the list empty for a minimal project (you will add FlixelGDX manually).
7. **Final Settings**
  - Set **Java version** to **17** (required for FlixelGDX and the generated build).  
  - Set **Project path** to the folder where the project should be created (e.g. `C:\dev\FlixelTest` or `~/projects/FlixelTest`).  
  - Click **Generate**. gdx-liftoff will create the project and open the folder when done.

You now have a Gradle project with at least a **core** module and an **lwjgl3** module (and optionally **android**, **ios**, **html**, etc., if you selected them).

#### Platform options for testing

If you want to run your FlixelGDX test game on more than desktop, select the following in the **Platforms** step **before** generating:

| Platform | What to select in gdx-liftoff | What you get |
|----------|-------------------------------|--------------|
| **Desktop (LWJGL3)** | **Core** + **LWJGL3** | `lwjgl3` module; run the `lwjgl3` run configuration or `./gradlew lwjgl3:run`. |
| **Android** | **Core** + **LWJGL3** + **Android** | `android` module; run the `android` configuration or connect a device/emulator and run the Android app. Requires Android SDK. |
| **iOS** | **Core** + **LWJGL3** + **iOS** with **MobiVM** | `ios` module; run the iOS launcher from a Mac. You **must** select **MobiVM** (or the RoboVM option that uses the [MobiVM](https://github.com/MobiVM/robovm) fork) as the iOS backend in gdx-liftoff. MobiVM is the maintained fork of RoboVM used by libGDX for iOS. |
| **Web (TeaVM)** | **Core** + **LWJGL3** + **HTML** with **TeaVM** | In Secondary Platforms, check **HTML(TeaVM)** (not GWT). Generates an `html` module that compiles Java to JavaScript via TeaVM. Run the `html` run configuration or the Gradle task that starts the TeaVM server / builds the web output. |

Many backend modules already depend on `flixelgdx-core`; you typically depend on **`flixelgdx-core`** and **one backend module** plus any extra modules you use directly. For example, you would place the following in your **core** module (where all of your game's main logic goes):

```gradle
dependencies {
  implementation 'me.stringdotjar.flixelgdx:flixelgdx-core:1.0.0'
}
```

And then you would place the following in your **lwjgl3** (desktop) module:

```gradle
dependencies {
  implementation 'me.stringdotjar.flixelgdx:flixelgdx-lwjgl3:1.0.0'
}
```

#### Integrating FlixelGDX into the generated project

After the project is generated, add FlixelGDX and wire it in:

1. **Publish FlixelGDX to Maven Local** (if you have not already). From the FlixelGDX repo root:
  ```bash
   ./gradlew publishToMavenLocal
   ```

2. **Add Maven Local to the test project.**  
   Open the **root** `build.gradle` of the liftoff-generated project (the one that contains `allprojects { ... }`). In the `repositories` block, ensure `mavenLocal()` is present, usually before `mavenCentral()`:
   ```gradle
   allprojects {
     repositories {
       mavenLocal()
       mavenCentral()
       // ... other repositories
     }
   }
   ```
   If `mavenLocal()` is already there, skip this step.

3. **Add the FlixelGDX dependency to the core module.**  
   Open the **core** module’s `build.gradle` (e.g. `core/build.gradle`). In the `dependencies { }` block, add:
   ```gradle
   dependencies {
     implementation "me.stringdotjar.flixelgdx:flixelgdx-core:1.0.0"  // Use the version from FlixelGDX’s gradle.properties
     // ... existing dependencies (e.g. libgdx)
   }
   ```

4. **Replace the main class with a FlixelGame.**  
   Open the main class in `core` (the one you named in gdx-liftoff, e.g. `FlixelTestGame.java`). Change it so that it extends `FlixelGame` instead of `ApplicationAdapter` or `Game`, and start a state in `create()`:
   ```java
   package com.example.flixeltest;

   import me.stringdotjar.flixelgdx.Flixel;
   import me.stringdotjar.flixelgdx.FlixelGame;
   import me.stringdotjar.flixelgdx.FlixelState;

   public class FlixelTestGame extends FlixelGame {
     
     // Make sure to create a constructor so you can use it in a launcher!
     public FlixelTestGame(String title, int width, int height, FlixelState initialScreen) {
       super(title, width, height, initialScreen);
     }
   }
   ```

5. **Create a simple state.**  
   Add a new class (e.g. `MyTestState.java`) in the same package or in a `states` package, extending `FlixelState`, and override what you need (e.g. `create()` to add sprites or text):
   ```java
   package com.example.flixeltest;

   import me.stringdotjar.flixelgdx.FlixelState;

   public class MyTestState extends FlixelState {

     @Override
     public void create() {
       super.create();
       // Add your test content (sprites, tweens, etc.)
     }
   }
   ```

6. **Use a dedicated built-in launcher.**  
   FlixelGDX provides many built-in launchers for each platform. In this example, we'll use the LWJGL3 launcher.

   - **Add the FlixelGDX LWJGL3 dependency** to the **lwjgl3** module’s `build.gradle` (e.g. `lwjgl3/build.gradle`), not the root or core module:
     ```gradle
     dependencies {
       implementation "me.stringdotjar.flixelgdx:flixelgdx-lwjgl3:1.0.0"  // same version as flixelgdx-core
       // ... existing dependencies (e.g. libGDX lwjgl3 backend, core project)
     }
     ```
     The lwjgl3 module already depends on your `core` project, so it will have access to `FlixelTestGame` and `MyTestState`.

   - **Use (or create) a launcher class** in the lwjgl3 module. Typically, gdx-liftoff will generate a launcher class for you, but you can customize it if you want.
     ```java
     package com.example.flixeltest;

    import com.example.flixeltest.MyTestState;
    import com.example.flixeltest.FlixelTestGame;
    import me.stringdotjar.flixelgdx.backend.lwjgl3.FlixelLwjgl3Launcher;

    /** Launches the desktop (LWJGL3) application. */
    public class Lwjgl3Launcher {

      public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) return; // This handles macOS support and helps on Windows.

        // Create your game and pass the initial screen.
        FlixelTestGame game = new FlixelTestGame(
          "Flixel Test", 
          800, 
          600, 
          new MyTestState()
        );

        // Launch the game using the FlixelGDX LWJGL3 launcher.
        FlixelLwjgl3Launcher.launch(game);
      }
    }
    ```
7. **Refresh Gradle** in your IDE (e.g. “Reload All Gradle Projects” or Gradle sync). Resolve any import errors so that `FlixelGame`, `FlixelState`, and `Flixel` are found from `flixelgdx-core`, and `FlixelLwjgl3Launcher` from `flixelgdx-lwjgl3`.
8. **Run the desktop launcher.**
  Run the **lwjgl3** run configuration with the main class set to your launcher (e.g. `FlixelTestLauncher`). You should see your state. If you added Android/iOS/HTML, run the corresponding launcher or Gradle task for that platform to test there as well.

### Method 2: Composite build (IntelliJ or any Gradle-based IDE)

Composite build lets the test project use your local FlixelGDX source so changes are picked up without republishing.

1. **Open your test project** (the libGDX app) in your IDE.
2. **Add the composite in the test project’s `settings.gradle`** (at the top level, same file that has `rootProject.name`):
  ```gradle
   rootProject.name = 'my-test-game'

   includeBuild('/path/to/flixelgdx') {
     dependencySubstitution {
       substitute module('me.stringdotjar.flixelgdx:flixelgdx-core') using project(':flixelgdx-core')
     }
   }
  ```
   Replace `/path/to/flixelgdx` with the **absolute path** to your FlixelGDX clone.  
  - **Windows**: use forward slashes or escaped backslashes, e.g. `C:/Users/You/flixelgdx` or `C:\\Users\\You\\flixelgdx`.  
  - **macOS/Linux**: e.g. `/home/you/projects/flixelgdx`.
3. **Declare the dependency in the test project’s core `build.gradle`** (no version needed; the composite supplies the project):
  ```gradle
   dependencies {
     implementation 'me.stringdotjar.flixelgdx:flixelgdx-core'
     // ... other dependencies
   }
  ```
4. **Refresh Gradle** (e.g. “Reload All Gradle Projects” or run a Gradle sync). The test project will compile against your FlixelGDX source; re-run or debug the game to see framework changes immediately.

---

## How to test the framework properly

1. **Run the project’s unit tests**

   From the FlixelGDX repo root:

   ```bash
   ./gradlew test
   ```

   Fix any failing tests before submitting changes.
2. **Use a real test game**
  - In your test project (liftoff or composite), create a minimal `FlixelGame` and at least one `FlixelState`.
  - Switch states, create sprites, use `FlixelTween` / `FlixelTweenSettings`, and hit the code paths you changed.
  - Run the **desktop** launcher first; then Android or other platforms if your change affects them.
3. **Verify no regressions**
  After modifying FlixelGDX, run `./gradlew test` again and run your test game (state switches, tweens, sprites, etc.) to ensure nothing else breaks.
4. **Example verification**
  In the test project, a minimal check that the framework is wired correctly:
   If this compiles and runs (e.g. shows your state), the dependency and setup are correct. Then add more states and use the APIs you’re changing to test the framework properly.

---

## Web (TeaVM) setup and configuration

FlixelGDX supports web builds through the **TeaVM** backend (`flixelgdx-teavm`). TeaVM transpiles
Java bytecode into JavaScript so your game can run in a browser.

> [!IMPORTANT]
> **Do not use the TeaVM module generated by gdx-liftoff.** Liftoff generates a module using the
> old `backend-teavm` + `TeaVMBuilder.java` approach, which is incompatible with FlixelGDX's
> `backend-web` + `org.teavm` Gradle plugin approach. Delete `TeaVMBuilder.java` and replace the
> entire `build.gradle` with the template below.

### Recommended: use the FlixelGDX TeaVM Gradle plugin

`flixelgdx-teavm-plugin` is a companion Gradle plugin that automates the three steps that
otherwise require manual setup:

| Without plugin | With plugin |
|---|---|
| Write `src/main/webapp/index.html` by hand | Auto-generated with the correct canvas ID |
| Add a `copyAssets` Gradle task | Handled by `copyAssets` |
| Wire copy tasks to `generateJavaScript` | Handled automatically |

#### 1. Add plugin resolution to `settings.gradle`

The plugin is published to `mavenLocal` (dev) and JitPack (distribution), so add those
repositories to the `pluginManagement` block at the top of your root `settings.gradle`:

```gradle
pluginManagement {
  repositories {
    mavenLocal()
    maven { url 'https://jitpack.io' }
    gradlePluginPortal()
  }
}
```

#### 2. Create the web module

Add a `teavm/` directory (or any name you prefer) to your project. A minimal
`teavm/build.gradle`:

```gradle
plugins {
  id 'org.teavm' version '0.13.0'
  id 'me.stringdotjar.flixelgdx.teavm' version '0.1.0-beta'
}

teavm {
  all { mainClass = 'com.mygame.teavm.MyTeaVMLauncher' }
  js {
    addedToWebApp = true
    targetFileName = 'teavm.js'
    outputDir = file("$buildDir/dist/webapp")
  }
}

dependencies {
  implementation 'me.stringdotjar.flixelgdx:flixelgdx-teavm:0.1.0-beta'
  implementation project(':core')
}
```

Include the module in your root `settings.gradle`:

```gradle
include 'core', 'lwjgl3', 'teavm'
```

#### 3. Create the launcher class

```java
public class MyTeaVMLauncher {

  public static void main(String[] args) {
    FlixelTeaVMLauncher.launch(new MyGame("My Game", 800, 600, new PlayState()));
  }
}
```

That is the entire setup. The plugin automatically:

- Copies `<rootProject>/assets/` to `<outputDir>/assets/` before each build.
- Generates a default `index.html` (with the correct canvas ID and script path) if you do not
  provide one in the `flixelgdx {}` extension block (refer to the [plugin module](flixelgdx-teavm-plugin/) and its Javadoc for more details).
- Wires everything to `generateJavaScript` and `javaScriptDevServer`.

#### 4. Run the game in the browser

Running the game in the browser is as simple as running the `run` task:

```bash
./gradlew :teavm:run
```

The output in `teavm/build/dist/webapp/` is a self-contained folder you can serve with any HTTP server.

### Optional plugin customization

Use the `flixelgdx {}` block to override defaults:

```gradle
flixelgdx {
  // Title of the game (default: "My FlixelGDX Game").
  title = 'My Game Title'

  // Canvas element ID (default: "flixelgdx-canvas").
  // Must match WebApplicationConfiguration.canvasID in your launcher.
  canvasId = 'my-canvas'

  // Where the assembled web app goes (default: "$buildDir/dist/webapp").
  // Must match teavm.js.outputDir.
  outputDir = file("$buildDir/dist/webapp")

  // Custom startup logo (optional).
  customStartupLogo = file('src/main/webapp/startup-logo.png')

  // Custom favicon (optional).
  customFavicon = file('src/main/webapp/favicon.ico')

  // Port for the `run` dev server task (default: 8080).
  devServerPort = 8080

  // Game assets to copy (default: rootProject/assets/).
  assetsDir = file('../assets')

  // User-provided web resources directory (default: src/main/webapp/).
  // If this directory contains an index.html the auto-generation is skipped.
  webappDir = file('src/main/webapp')

  // Set to false to disable index.html auto-generation entirely (default: true).
  generateDefaultIndexHtml = true
}
```

To provide a completely custom `index.html`, place it in `src/main/webapp/index.html`. The
plugin detects it and skips generation automatically. The canvas ID in your custom HTML must
match `FlixelTeaVMLauncher`'s default (`flixelgdx-canvas`) or the value you pass to
`WebApplicationConfiguration.canvasID`.

### Web configuration customization

The launcher accepts an optional `Consumer<WebApplicationConfiguration>` to override canvas ID,
dimensions, or other web-specific settings:

```java
FlixelTeaVMLauncher.launch(
  new MyGame("My Game", 800, 600, new InitialState()),
  FlixelRuntimeMode.RELEASE,
  config -> {
    config.canvasID = "my-canvas";
    config.antialiasing = true;
  }
);
```

### Reflection metadata

FlixelGDX's TeaVM build auto-generates a `teavm.json` reflection metadata file during the
`processResources` phase. This file preserves class/field/method information that TeaVM's
ahead-of-time compiler would otherwise strip.

The reflection profile is controlled by `flixelReflectionProfile` in `gradle.properties`:

| Profile    | What is preserved |
|------------|-------------------|
| `SIMPLE`   | FlixelGDX classes only. |
| `STANDARD` | FlixelGDX + libGDX classes (recommended). |
| `ALL`      | FlixelGDX + libGDX + visible dependencies (anim8, miniaudio). |

To include your own game packages in the metadata, set `flixelReflectionExtraPackages` in
`gradle.properties`:

```properties
flixelReflectionExtraPackages=com.mygame,org.example.tools
```

### Platform limitations on web

The web backend intentionally omits several features that are unavailable in a browser
environment:

- **File logging** is disabled. There is no host filesystem, so `Flixel.startFileLogging()` is
  a safe no-op. Console output (`System.out.println`) maps to `console.log` in the browser.
- **Jansi / ANSI colors** are not installed. Terminal color codes are irrelevant in a browser
  console.
- **`FlixelVarTween`** relies on runtime reflection and may exhibit slower performance on
  TeaVM. Prefer `FlixelPropertyTween` (getter/setter lambdas) for web-targeted games.
- **`FlixelGitUtil`** and other `ProcessBuilder`-based utilities are unavailable (no subprocess
  support in browsers).
- **`FlixelDefaultAssetManager.extractAssetPath()`** uses `java.io.File` for temp file
  extraction. On web, audio assets load through the browser's network stack and do not need
  filesystem materialization.

### Validation checklist for web readiness

Before shipping a web build, verify the following:

1. The game boots and the initial state renders in the browser.
2. No `ClassNotFoundException` or `NoSuchMethodException` in the browser console (indicates
   missing reflection metadata; widen the profile or add extra packages).
3. Tweens animate correctly (prefer `FlixelPropertyTween` over `FlixelVarTween`).
4. Audio plays in the browser (uses the Web Audio API via libGDX's backend).
5. Input (keyboard, mouse/touch) responds as expected.

---

## Setting up the Android SDK (for contributing to the Android platform)

If you want to contribute to the **flixelgdx-android** module or run and test FlixelGDX on Android (in this repo or in a test project), you need the Android SDK and a way to run an Android app (emulator or physical device). This section covers installation, configuration, and the limitations and workarounds you may hit depending on your OS.

### The Android module is optional (no SDK required by default)

The framework repo **does not require an Android SDK** to compile. By default, the **flixelgdx-android** module is **not included** in the build, so you can clone, build core/desktop/TeaVM, and contribute without installing the SDK. Testing on Android is done in a **separate test project** (see [Testing with a test project](#testing-with-a-test-project)); that test project has its own Android app and its own SDK—install the SDK there when you need to run on device or emulator.

To **build the Android module in this repo** (e.g. to work on Android-specific code or to publish the AAR), you must enable it in one of two ways (do **not** add this to the committed `gradle.properties`—that would require everyone to have the SDK):

- **CI or one-off builds**: pass the property on the command line:  
`./gradlew -PincludeAndroid=true :flixelgdx-android:assembleRelease`
- **Local development (recommended if you often work on Android)**: add a line to **local.properties** (this file is gitignored, so you never commit it):
  ```properties
  includeAndroid=true
  ```
  Then any normal `./gradlew ...` run will include the Android module. You still need the Android SDK and the setup steps below when the module is enabled.

### Why you need the Android SDK

- **Building the flixelgdx-android module in this repo**: after enabling it (see above), `./gradlew :flixelgdx-android:assemble` (or `assembleRelease`) requires the Android SDK and build tools.
- **Building a test project that includes an Android module**: requires the Android SDK in that project (and `sdk.dir` in that project’s `local.properties`).
- **Running on an emulator or device**: to verify behavior and debug, you need either an Android Virtual Device (AVD) or a physical Android device.

### Installing the Android SDK

**Option A: Android Studio (recommended)**  
Android Studio installs the SDK, SDK Manager, and emulator support in one place. It is the simplest option on all platforms.

1. Download [Android Studio](https://developer.android.com/studio) for your OS.
2. Run the installer. During setup, ensure the **Android SDK** and **Android Virtual Device** components are selected.
3. After installation, open **Android Studio → Settings** (or **Preferences** on macOS) → **Languages & Frameworks → Android SDK**. Note the **Android SDK Location** (e.g. `C:\Users\You\AppData\Local\Android\Sdk` on Windows, `~/Library/Android/sdk` on macOS, `~/Android/Sdk` on Linux).

**Option B: Command-line tools only**  
If you prefer not to use Android Studio, install the [command-line tools](https://developer.android.com/studio#command-tools) and use `sdkmanager` to install packages (platform-tools, build-tools, a platform like `android-34`, and optionally emulator and system images).

**Option C: Using the project's setup scripts**  
The FlixelGDX repo includes helper scripts in the **`scripts/`** directory that download and install the Android command-line tools and a minimal set of packages:

- **Windows**: Run `scripts\android_setup_windows.bat` from the repo root (e.g. from Command Prompt). The script installs to `%USERPROFILE%\android-sdk` with the same packages.
- **macOS / Linux**: Run `./scripts/android_setup_macos_linux.sh` (or `bash scripts/android_setup_macos_linux.sh`) from the repo root. The script installs the SDK to `$HOME/android-sdk` and installs platform-tools, `platforms;android-34`, and `build-tools;34.0.0`.

After running a script, add `ANDROID_HOME` and the script’s suggested `PATH` entries to your environment (as printed at the end of the run), then set `sdk.dir` in `local.properties` as described in [Configuring the SDK for FlixelGDX](#configuring-the-sdk-for-flixelgdx).

**Warnings when using the scripts:**

- **Command-line tools only**: The scripts do **not** install Android Studio or emulator/AVD components. To run an emulator, you must install emulator and system-image packages yourself (e.g. via `sdkmanager`) or use Android Studio.
- **Fixed install path**: The scripts always install to `$HOME/android-sdk` (mac/Linux) or `%USERPROFILE%\android-sdk` (Windows). If you already have Android Studio, it usually uses a different path (e.g. `~/Library/Android/sdk` on macOS). You may then have two SDK installations; set `ANDROID_HOME` and `sdk.dir` to the one you want Gradle to use.
- **Windows**: License acceptance is done by sending a fixed number of “y” responses. If the script fails with license-related errors, run `sdkmanager --sdk_root="%ANDROID_HOME%" --licenses` manually (with `ANDROID_HOME` set to your SDK path), accept all required licenses, then install any missing packages with `sdkmanager` if needed.
- **macOS / Linux**: The script downloads a zip into the **current working directory** before extracting it (then deletes the zip). Run the script from the repo root or from `scripts/` to avoid leaving temporary files in other directories.

### Configuring the SDK for FlixelGDX

1. **Set ANDROID_HOME (or ANDROID_SDK_ROOT)**
  Point this environment variable to your SDK location. Use the path from Android Studio’s SDK location, or the root of your command-line SDK install.

  | Platform    | Typical SDK path                                              |
  | ----------- | ------------------------------------------------------------- |
  | **Windows** | `C:\Users\<You>\AppData\Local\Android\Sdk`                    |
  | **macOS**   | `~/Library/Android/sdk` or `/Users/<You>/Library/Android/sdk` |
  | **Linux**   | `~/Android/Sdk` or `/home/<You>/Android/Sdk`                  |

   Add to your shell profile (e.g. `~/.bashrc`, `~/.zshrc`) or, on Windows, System Environment Variables:
   ```bash
   export ANDROID_HOME=~/Library/Android/sdk   # adjust for your path
   export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"
   ```
   On Windows, add `%ANDROID_HOME%\platform-tools` and, if using cmdline-tools, `%ANDROID_HOME%\cmdline-tools\latest\bin` to `Path`.

2. **Point Gradle at the SDK**  
   In the **root** of the FlixelGDX repo (or your test project), create or edit `local.properties` and set:
   ```properties
   sdk.dir=C\:\\Users\\You\\AppData\\Local\\Android\\Sdk
   ```
   Use the path for your machine; on Windows use double backslashes. This file is usually gitignored so each developer uses their own path. If you are building the Android module in the FlixelGDX repo, you can also add `includeAndroid=true` to this file so the Android module is included without passing `-PincludeAndroid=true` each time.

3. **Accept SDK licenses**  
   From a terminal:
   ```bash
   sdkmanager --licenses
   ```
   Accept all required licenses. If `sdkmanager` is not in `PATH`, use the full path (e.g. `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses`).

### Running on emulator or device

- **Emulator**: In Android Studio, **Tools → Device Manager** (or AVD Manager) to create an AVD (e.g. Pixel 6, API 34). Start the AVD, then run your test project’s Android run configuration or `./gradlew :android:installDebug` (or the equivalent for your test app). Enable hardware acceleration (see below) for better performance.
- **Physical device**: Enable **Developer options** and **USB debugging** on the device, connect via USB, and run the same run configuration or Gradle install task.

### Limitations and workarounds by platform

| Platform | Limitations | Workarounds |
|----------|-------------|-------------|
| **Windows** | Emulator can be slow without acceleration; Hyper-V and Android Emulator can conflict. | Use **HAXM** (Intel) or **Windows Hypervisor Platform** for x86/x86_64 emulator images if available; or use a physical device. For FlixelGDX you can still **build** the Android module (`./gradlew -PincludeAndroid=true :flixelgdx-android:assemble`) and contribute code without running the app; CI or maintainers can run on device/emulator. |
| **macOS** | None specific to Android, although Apple Silicon (M1/M2) doesn't support x86 emulation very well. | Choose an AVD with an ARM64 image (e.g. “Apple M1” or “ARM 64” in AVD Manager). On Apple Silicon (M1/M2), use **ARM64** (e.g. `arm64-v8a`) system images for the emulator to avoid slow x86 emulation. |
| **Linux** | Some older SDK tools expect 32-bit libs; modern installs are 64-bit. Emulator needs KVM for acceleration. | Install KVM and ensure your user is in the `kvm` group for hardware-accelerated emulator. If you cannot run an emulator, build the module and use a physical device or rely on CI. |

**Contributing without running on a device/emulator**  
You can still edit and build the **flixelgdx-android** module. Enable the module (e.g. add `includeAndroid=true` to `local.properties` or use `-PincludeAndroid=true`), then run `./gradlew :flixelgdx-android:assemble` to confirm it compiles. For runtime behavior, rely on a maintainer or CI that has an Android environment, or use a cloud/VM with the SDK and an emulator.

---

## Setting up an iOS development environment (for contributing to the iOS platform)

Contributing to the **flixelgdx-ios** module or testing FlixelGDX on iOS requires a **Mac** and **Xcode**. Apple does not allow building or running iOS apps on Windows or Linux. This section describes the environment, the development process, and practical limitations and workarounds.

### Why you need a Mac and Xcode

- **flixelgdx-ios** (and any libGDX iOS backend, including MobiVM) compiles and links native iOS code. The toolchain (Xcode, clang, iOS SDK, simulators) runs only on macOS.
- To run your game in the simulator or on a device, you need Xcode and, for a physical device, an Apple Developer account (free for simulator, paid for device distribution).

### Installing Xcode and the command-line tools

1. **Install Xcode** from the Mac App Store (free). This includes the iOS SDK, simulators, and the full Xcode IDE.
2. **Install the Xcode Command Line Tools** (needed for Gradle and command-line builds):
  ```bash
   xcode-select --install
  ```
   If you use the full Xcode app, open it once and accept the license. Point the active developer directory to Xcode if you have multiple versions:
  ```bash
  sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
  ```
3. **MobiVM**  
   FlixelGDX uses **MobiVM** for iOS (the maintained [RoboVM fork](https://github.com/MobiVM/robovm)). When you create a test project with gdx-liftoff and add the iOS platform, select **MobiVM** (or the RoboVM option that uses MobiVM) as the backend. The generated project will pull the MobiVM/RoboVM Gradle plugin and dependencies; no separate installer is usually needed. Ensure your test project uses the MobiVM backend as described in the [platform options table](#platform-options-for-testing) earlier in this document.

### Development process for iOS

1. **Build from Gradle**
  In the FlixelGDX repo or your test project, the iOS app is built by Gradle (MobiVM/RoboVM plugin), not by opening a separate Xcode project by default. From the project root:
  - Build the iOS module: `./gradlew :flixelgdx-ios:build` (for the framework) or the equivalent for your test project’s `ios` module (e.g. `./gradlew ios:createIpa` or the task your liftoff project defines).
  - Run on the **simulator**: use the Gradle task provided by the MobiVM/liftoff setup (e.g. `ios:launchIPhoneSimulator` or similar; name may vary). That task builds and starts the simulator with your app.
2. **Running on a physical device**
  You need an **Apple Developer account** (free account allows simulator; paid $99/year for device and App Store). In Xcode, you may need to open the generated or exported Xcode project/workspace for signing: set your Team and provisioning profile, then build and run from Xcode, or configure signing in the Gradle/MobiVM build so that the IPA can be installed on a device.
3. **Debugging**
  Run the app from the simulator or device via Gradle or Xcode. Use Xcode’s debugger for native crashes; for Java-level debugging, use your IDE’s remote debugger attached to the MobiVM runtime if supported by the MobiVM/RoboVM version you use.

### Limitations and workarounds by platform

| Situation | Limitation | Workaround |
|-----------|------------|------------|
| **No Mac (Windows or Linux)** | You cannot build or run an iOS app locally. There is no official iOS simulator or toolchain for Windows/Linux. | You can still **contribute code** to **flixelgdx-ios**: edit Java and build the module if it compiles (e.g. `./gradlew :flixelgdx-ios:build` may run non-iOS parts of the build). For running on simulator/device, rely on **CI** (e.g. GitHub Actions with a Mac runner) or a **maintainer with a Mac**. For occasional testing, consider a **Mac in the cloud** (e.g. MacStadium, AWS EC2 Mac, or similar) and connect to it for builds and simulator runs. |
| **Mac, no paid Apple Developer account** | You cannot install or run on a **physical device** or distribute via the App Store. | Use the **simulator only** (free). Create and run an iPhone/iPad simulator from Xcode → **Window → Devices and Simulators**. The simulator is enough for many contributions and bug reproductions. |
| **Mac, with paid account** | None for basic testing. | Use simulator for quick iteration; use a device when you need to test device-only behavior (performance, sensors, etc.). |
| **MobiVM / RoboVM version** | MobiVM is actively maintained; libGDX and gdx-liftoff pin specific versions. | Follow [MobiVM releases](https://github.com/MobiVM/robovm/releases) and gdx-liftoff release notes. Use the Java and dependency versions recommended by the FlixelGDX and liftoff docs. |

**Summary**  

- **Android**: Install the Android SDK (Android Studio or command-line), set `ANDROID_HOME` and `local.properties`, accept licenses. You can build the Android module on any OS; running the app requires an emulator (with acceleration where possible) or a physical device. On Windows/Linux you cannot do iOS; on Mac prefer ARM64 emulator images on Apple Silicon.
- **iOS**: You must have a Mac and Xcode. Use MobiVM for the iOS backend. Build and run via Gradle and the simulator; use a paid Apple Developer account for device testing. Without a Mac, you can still contribute to flixelgdx-ios code and rely on CI or a Mac-in-the-cloud for running and testing.

---

## Troubleshooting

### JAVA_HOME not set or wrong

- **Symptom**: Gradle or the IDE reports “JAVA_HOME is not set” or uses the wrong Java version.
- **Fix**: Set `JAVA_HOME` to the **JDK 17** installation directory (see [Java (JDK 17)](#2-java-jdk-17) for your OS). Use a **new** terminal/IDE restart after changing environment variables.

### Wrong Java version (8, 11, 21, etc.)

- **Symptom**: Build fails with “invalid target release” or “class file version” errors, or `java -version` is not 17.
- **Fix**:  
  - From the command line: ensure `JAVA_HOME` and `PATH` point to JDK 17; run `java -version` and `javac -version`.  
  - In the IDE: set the **project SDK / JRE** to JDK 17 (see [IDE setup](#ide-setup)).

### Gradle wrapper not executable (Linux / macOS)

- **Symptom**: `./gradlew publishToMavenLocal` fails with “Permission denied”.
- **Fix**:  
`chmod +x gradlew`  
Then run `./gradlew publishToMavenLocal` again.

### Path with spaces or special characters

- **Symptom**: Gradle or scripts fail when the project path contains spaces (e.g. `C:\Users\My Name\flixelgdx`).
- **Fix**: Prefer a path without spaces (e.g. `C:\dev\flixelgdx`). If you must use spaces, quote the path in scripts and in composite build: `includeBuild('C:/Users/My Name/flixelgdx') { ... }`.

### Dependency not found: `me.stringdotjar.flixelgdx:flixelgdx-core`

- **Symptom**: The test project fails to resolve the FlixelGDX dependency.
- **Fix**:  
  - If using **Maven Local**: run `./gradlew publishToMavenLocal` in the FlixelGDX repo, and ensure the test project’s root `build.gradle` has `mavenLocal()` in `repositories`.  
  - If using **composite build**: check that `includeBuild(...)` in the test project’s `settings.gradle` uses the correct **absolute** path to the FlixelGDX directory and that the module name is `:flixelgdx-core`.

### Composite build path wrong (Windows)

- **Symptom**: “Project directory does not exist” or path not found in `includeBuild`.
- **Fix**: Use an absolute path with forward slashes, e.g. `C:/Users/You/flixelgdx`, or escaped backslashes. Avoid trailing backslash.

### Version mismatch (test project vs published artifact)

- **Symptom**: Test project depends on `flixelgdx-core:1.0.0` but you have not published that version, or you changed the version locally.
- **Fix**: Either run `publishToMavenLocal` so the version in `gradle.properties` is installed, or use a **composite build** so the test project ignores the version and uses the local project.

### Android: SDK not found or licenses not accepted

- **Symptom**: Building or running the Android part of FlixelGDX (or a test app with Android) fails with “SDK location not found” or license errors.
- **Fix**:  
  - Set `ANDROID_HOME` (or `ANDROID_SDK_ROOT`) to your Android SDK path.
  - Create a `local.properties` file in the root of the project and add the following:
    ```properties
    sdk.dir = /path/to/android/sdk
    ```
  - Run `sdkmanager --licenses` (or accept licenses in Android Studio) and accept the required licenses.

### Line endings (CRLF vs LF) on Windows

- **Symptom**: `./gradlew` fails with “bad interpreter” or similar (often in Git Bash or WSL).
- **Fix**: Ensure `gradlew` uses Unix line endings (LF). In Git: `git config core.autocrlf input` and re-checkout, or run `dos2unix gradlew` if available. The repo should keep `gradlew` as LF.

### IDE does not pick up Gradle or JDK after install

- **Fix**:  
  - **IntelliJ**: File → Invalidate Caches / Restart; and re-import the project or “Reload All Gradle Projects”.  
  - **VS Code/Cursor**: Run “Java: Clean Java Language Server Workspace” from the Command Palette, then reload.  
  - **Eclipse**: Project → Clean; and Gradle → Refresh Gradle Project.  
  - In all cases, confirm the **project** is using JDK 17 in its settings.

### Gradle daemon or port issues

- **Symptom**: “Address already in use” or daemon-related errors.
- **Fix**: Stop Gradle daemons: `./gradlew --stop`. Then run your build again. This project sets `org.gradle.daemon=false` in `gradle.properties`, so the daemon may already be disabled.

If you hit an error not listed here, open an issue with your OS, Java version (`java -version`), Gradle version (`./gradlew --version`), and the full error message so maintainers can help.
