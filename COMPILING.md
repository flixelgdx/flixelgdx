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
7. [Setting up the Android SDK (for contributing to the Android platform)](#setting-up-the-android-sdk-for-contributing-to-the-android-platform)
8. [Setting up an iOS development environment (for contributing to the iOS platform)](#setting-up-an-ios-development-environment-for-contributing-to-the-ios-platform)
9. [Troubleshooting](#troubleshooting)

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

### 2. Java (JDK 17)

FlixelGDX requires **Java 17** (LTS). The build uses the Gradle wrapper (Gradle 9.x), which runs on JDK 17+. Your IDE and command line must both use JDK 17.

#### Windows

- **Option A (recommended)**  
  - Download **Oracle JDK 17** from [oracle.com/java/technologies/downloads](https://www.oracle.com/java/technologies/downloads/#java17) (choose Windows x64 installer).  
  - Run the installer. Check “Set JAVA_HOME variable” and “Add to PATH” if offered.  
- **Option B**  
  - Oracle JDK is also available via winget: `winget install Oracle.JDK.17`  
- **Option C**  
  - Eclipse Temurin (Adoptium) 17 or Amazon Corretto 17 from their official sites.

- **Set JAVA_HOME (if not set by installer)**  
  - Open **Settings → System → About → Advanced system settings → Environment Variables**.  
  - Under **System variables**, click **New**: Variable name `JAVA_HOME`, value = installation folder (e.g. `C:\Program Files\Java\jdk-17`).  
  - Edit **Path**, add **New** → `%JAVA_HOME%\bin`.  
  - Close and reopen Command Prompt or PowerShell.

- **Verify**  
  - `java -version` and `javac -version` should show version 17.

#### macOS

- **Option A (Homebrew)**  
  - `brew install openjdk@17`  
  - Then add to `~/.zshrc` (or `~/.bash_profile`):  
    `export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"`  
    and optionally `export JAVA_HOME="/opt/homebrew/opt/openjdk@17"`.  
  - On Intel Macs the path may be `/usr/local/opt/openjdk@17`.  
  - Run `source ~/.zshrc` or open a new terminal.

- **Option B**  
  - Use Apple’s built-in mechanism: `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` in your shell profile (requires a JDK 17 installed, e.g. from Oracle’s .pkg).

- **Verify**  
  - `java -version` and `javac -version` show 17.

#### Linux

| Distro | Install command | Typical JAVA_HOME |
|--------|-----------------|--------------------|
| **Ubuntu / Debian** | `sudo apt update && sudo apt install openjdk-17-jdk` | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64` (or `java-17-openjdk`) |
| **Fedora / RHEL** | `sudo dnf install java-17-openjdk-devel` | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk` |
| **Arch** | `sudo pacman -S jdk17-openjdk` | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk` |
| **openSUSE** | `sudo zypper install java-17-openjdk-devel` | `export JAVA_HOME=/usr/lib/jvm/java-17-openjdk` |

Add the `export JAVA_HOME=...` line to `~/.bashrc` or `~/.profile` (or `~/.zshrc` on Zsh). Then run `source ~/.bashrc` or open a new terminal.

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

- **Set project JDK to 17**  
  - **File → Project Structure → Project**: set **Project SDK** to **17**. If 17 is not listed, click **Add SDK → Download JDK**, choose **Version 17** (e.g. Oracle OpenJDK), then **Download**.  
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

The first run may take longer while Gradle downloads the wrapper and dependencies. After it succeeds, the framework is available to other Gradle projects on your machine.

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
   - Leave **Java** selected (FlixelGDX is Java; Kotlin/Scala/Groovy are optional if your test project uses them).

4. **Extensions**  
   - For a minimal project, leave all extensions **unchecked** (no Box2D, Ashley, FreeType, etc.).

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

You only need **one** dependency on FlixelGDX: add `me.stringdotjar.flixelgdx:flixelgdx-core` to your **core** module. The desktop, Android, iOS, and HTML launchers that gdx-liftoff generates already reference your core main class (your `FlixelGame` subclass); they do not need a separate FlixelGDX backend dependency. Your game runs as the shared `ApplicationListener` on every platform.

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

   ```java
   public class MyTestGame extends FlixelGame {

     @Override
     public void create() {
       super.create();
       Flixel.switchState(new MyTestState());
     }
   }
   ```

   If this compiles and runs (e.g. shows your state), the dependency and setup are correct. Then add more states and use the APIs you’re changing to test the framework properly.

---

## Setting up the Android SDK (for contributing to the Android platform)

If you want to contribute to the **flixelgdx-android** module or run and test FlixelGDX on Android (in this repo or in a test project), you need the Android SDK and a way to run an Android app (emulator or physical device). This section covers installation, configuration, and the limitations and workarounds you may hit depending on your OS.

### Why you need the Android SDK

- Building the **flixelgdx-android** module: `./gradlew :flixelgdx-android:assemble` (or building a test project that includes an Android module) requires the Android SDK and build tools.
- Running on an emulator or device: to verify behavior and debug, you need either an Android Virtual Device (AVD) or a physical Android device.

### Installing the Android SDK

**Option A: Android Studio (recommended)**  
Android Studio installs the SDK, SDK Manager, and emulator support in one place. It is the simplest option on all platforms.

1. Download [Android Studio](https://developer.android.com/studio) for your OS.
2. Run the installer. During setup, ensure the **Android SDK** and **Android Virtual Device** components are selected.
3. After installation, open **Android Studio → Settings** (or **Preferences** on macOS) → **Languages & Frameworks → Android SDK**. Note the **Android SDK Location** (e.g. `C:\Users\You\AppData\Local\Android\Sdk` on Windows, `~/Library/Android/sdk` on macOS, `~/Android/Sdk` on Linux).

**Option B: Command-line tools only**  
If you prefer not to use Android Studio, install the [command-line tools](https://developer.android.com/studio#command-tools) and use `sdkmanager` to install packages (platform-tools, build-tools, a platform like `android-34`, and optionally emulator and system images).

### Configuring the SDK for FlixelGDX

1. **Set ANDROID_HOME (or ANDROID_SDK_ROOT)**  
   Point this environment variable to your SDK location. Use the path from Android Studio’s SDK location, or the root of your command-line SDK install.

   | Platform | Typical SDK path |
   |----------|-------------------|
   | **Windows** | `C:\Users\<You>\AppData\Local\Android\Sdk` |
   | **macOS** | `~/Library/Android/sdk` or `/Users/<You>/Library/Android/sdk` |
   | **Linux** | `~/Android/Sdk` or `/home/<You>/Android/Sdk` |

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
   Use the path for your machine; on Windows use double backslashes. This file is usually gitignored so each developer uses their own path.

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
| **Windows** | Emulator can be slow without acceleration; Hyper-V and Android Emulator can conflict. | Use **HAXM** (Intel) or **Windows Hypervisor Platform** for x86/x86_64 emulator images if available; or use a physical device. For FlixelGDX you can still **build** the Android module (`./gradlew :flixelgdx-android:assemble`) and contribute code without running the app; CI or maintainers can run on device/emulator. |
| **macOS** | None specific to Android, although Apple Silicon (M1/M2) doesn't support x86 emulation very well. | Choose an AVD with an ARM64 image (e.g. “Apple M1” or “ARM 64” in AVD Manager). On Apple Silicon (M1/M2), use **ARM64** (e.g. `arm64-v8a`) system images for the emulator to avoid slow x86 emulation. |
| **Linux** | Some older SDK tools expect 32-bit libs; modern installs are 64-bit. Emulator needs KVM for acceleration. | Install KVM and ensure your user is in the `kvm` group for hardware-accelerated emulator. If you cannot run an emulator, build the module and use a physical device or rely on CI. |

**Contributing without running on a device/emulator**  
You can still edit and build the **flixelgdx-android** module. Run `./gradlew :flixelgdx-android:assemble` to confirm it compiles. For runtime behavior, rely on a maintainer or CI that has an Android environment, or use a cloud/VM with the SDK and an emulator.

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
