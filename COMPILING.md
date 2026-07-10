# Compiling & Testing

FlixelGDX is a framework, not a standalone game, so it cannot be run by itself. To test your changes, you use the framework as a local dependency (or composite build) in a separate test project. This guide walks you through every step from a clean machine to running and testing the framework, including prerequisites, IDE setup on all major platforms, and how to avoid common mistakes.

---

## Table of contents

1. [Prerequisites](#prerequisites)
2. [Getting the source](#getting-the-source)
3. [IDE setup](#ide-setup)
4. [Testing with a test project](#testing-with-a-test-project)
5. [How to run unit tests](#how-to-run-unit-tests)
6. [Web (TeaVM) setup and configuration](#web-teavm-setup-and-configuration)
7. [Setting up the Android SDK (for contributing to the Android platform)](#setting-up-the-android-sdk-for-contributing-to-the-android-platform)
8. [Troubleshooting](#troubleshooting)

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

### 2. Java (JDK 17 with Eclipse Temurin)

FlixelGDX requires **Java 17** (LTS). The build uses the Gradle wrapper (Gradle 9.x), which runs on JDK 17+. Your IDE and command line must both use JDK 17.

**Use Eclipse Temurin:** install **[Eclipse Temurin 17 (Adoptium)](https://adoptium.net/temurin/releases/?package=jdk&version=17)** - the battle-tested, open-source HotSpot JDK used across the Java game development ecosystem. It gets timely security updates through Adoptium and integrates seamlessly with Gradle toolchains.

After install, `java -version` should show **17** and mention `OpenJDK`.

#### Windows

- Download the **Windows x64** JDK 17 MSI from the [Adoptium Temurin downloads](https://adoptium.net/temurin/releases/?package=jdk&version=17) page.
- Run the installer. Enable “Set JAVA_HOME” / “Add to PATH” if offered.
- **Set JAVA_HOME (if not set by installer)**
  - **Settings -> System -> About -> Advanced system settings -> Environment Variables**.
  - **System variables -> New**: `JAVA_HOME` = Temurin install folder (e.g. `C:\Program Files\Eclipse Adoptium\jdk-17.x.x`).
  - **Path** -> **New** -> `%JAVA_HOME%\bin`.
  - Restart the terminal.
- **Verify**
  - `java -version` - should show **17**.
  - `javac -version` - **17**.

#### macOS

- **Option A (Homebrew)**
  - `brew install --cask temurin@17`
  - Point `JAVA_HOME` at the Temurin install (e.g. under `/Library/Java/JavaVirtualMachines/`).
- **Option B (installer)**
  - Download **macOS** JDK 17 from [Adoptium Temurin downloads](https://adoptium.net/temurin/releases/?package=jdk&version=17) and install the `.pkg`.
- **Verify**
  - `java -version` and `javac -version` show 17.

#### Linux

Adoptium maintains official package repositories for major distributions.

**Ubuntu / Debian:**

```bash
wget -qO- https://packages.adoptium.net/artifactory/api/gpg/key/public \
  | sudo gpg --dearmor -o /usr/share/keyrings/adoptium.gpg
echo “deb [signed-by=/usr/share/keyrings/adoptium.gpg] \
  https://packages.adoptium.net/artifactory/deb $(lsb_release -cs) main” \
  | sudo tee /etc/apt/sources.list.d/adoptium.list
sudo apt update && sudo apt install -y temurin-17-jdk
```

**Fedora / RHEL:**

```bash
sudo dnf install https://packages.adoptium.net/artifactory/rpm/fedora/$(rpm -E %fedora)/adoptium.repo
sudo dnf install -y temurin-17-jdk
```

**Arch:**

```bash
sudo pacman -S jdk17-openjdk
```

(Arch ships HotSpot OpenJDK, which is binary-compatible with Temurin.)

- **Verify**
  - `java -version`, `javac -version`, and `echo $JAVA_HOME`.

---

## Getting the source

1. **Clone the repository** (replace with the actual repo URL if you use a fork):
  ```bash
   git clone https://github.com/flixelgdx/flixelgdx.git
   cd flixelgdx
  ```
2. **If you are contributing**: Fork the repo on GitHub, then clone your fork, and add the upstream remote:
  ```bash
   git remote add upstream https://github.com/flixelgdx/flixelgdx.git
   git fetch upstream
   git checkout develop
  ```
3. **Use the `develop` branch** for development and PRs (see [CONTRIBUTING.md](CONTRIBUTING.md)).

---

## IDE setup

Configure your editor so it uses JDK 17 and the project’s Gradle build. Enabling EditorConfig (see [CONTRIBUTING.md](CONTRIBUTING.md)) keeps indentation and line endings consistent.

### IntelliJ IDEA

- **Install**  
  - [jetbrains.com/idea](https://www.jetbrains.com/idea/)
  - Windows: run the .exe installer.  
  - macOS: download .dmg, drag IntelliJ to Applications.  
  - Linux: unpack the .tar.gz or use Toolbox / Snap.
- **Open the project**  
  - **File -> Open** -> select the **flixelgdx** root folder (the one that contains `build.gradle` and `settings.gradle`).  
  - Choose **Open as Project**.  
  - When asked “Load Gradle project?”, choose **Load Gradle Project** and use the **Gradle wrapper** (default). Wait for indexing and dependency resolution to finish.
- **Set project JDK to 17 (Eclipse Temurin)**
  - **File -> Project Structure -> Project**: set **Project SDK** to **17**. Prefer **Eclipse Temurin** (Adoptium) - add it via **Add SDK -> Download JDK** and pick a **Temurin 17** build, or **Add SDK -> JDK** and point to your Temurin install folder.
  - Set **Project language level** to **17**.
- **EditorConfig**  
  - **Settings -> Editor -> Code Style** -> enable **“Enable EditorConfig support”**.  
  - Use the **Project** code style scheme so the project’s `.editorconfig` is applied.
- **Build from IDE**  
  - **View -> Tool Windows -> Gradle**. Under **flixelgdx -> Tasks -> publishing**, run **publishToMavenLocal**.  
  - Or use the terminal inside IntelliJ: `./gradlew publishToMavenLocal` (on Windows: `gradlew.bat publishToMavenLocal`).

### Eclipse

- **Install**  
  - [eclipse.org/downloads](https://www.eclipse.org/downloads/): **Eclipse IDE for Java Developers** or **Eclipse IDE for Java and DSL Developers**.  
  - Windows: unpack the zip or run the installer.  
  - macOS/Linux: unpack the tar.gz or use the installer.
- **Import as Gradle project**  
  - **File -> Import -> Gradle -> Existing Gradle Project -> Next**.  
  - **Project root directory**: browse to the **flixelgdx** root folder.  
  - Click **Finish**. Wait for Gradle to sync and build.
- **Use JDK 17**  
  - **Window -> Preferences -> Java -> Installed JREs**: add your JDK 17 (Add -> Standard VM -> Directory -> select JDK home). Set it as default or ensure the project uses it.  
  - **Project -> Properties -> Java Build Path -> Libraries**: ensure the JRE is JDK 17.
- **Build**  
  - Right-click the root project -> **Gradle -> Refresh Gradle Project**.  
  - To publish locally: right-click project -> **Run As -> Gradle Build**; in **Gradle Tasks** choose **publishToMavenLocal**, or run in a terminal: `./gradlew publishToMavenLocal`.

---

## Testing with a test project

You need a separate test project that depends on FlixelGDX.

### Generating the project

Use the **Getting Started** page on the [FlixelGDX website](https://flixelgdx.org/getting-started) to generate a test project. It creates a fully wired, ready-to-run Gradle multi-module project:

```
my-game/
  assets/               -- game assets shared across all platforms
  core/                 -- your game’s shared logic (depends on flixelgdx-core)
  lwjgl3/               -- desktop launcher (depends on flixelgdx-lwjgl3)
  teavm/                -- web launcher (depends on flixelgdx-teavm)
  build.gradle          -- root build file
  gradlew / gradlew.bat -- Gradle wrapper script(s)
  settings.gradle       -- Project settings
```

1. Fill in your project details:
    - **Game name**: e.g. `FlixelTest`.
    - **Game id**: e.g. `flixel-test`.
    - **Package name**: e.g. `com.example.flixeltest`.
    - **Language**: **Java** (or Kotlin).
    - **JDK vendor**: **Eclipse Temurin** (recommended).
    - **Platforms**: at minimum **Desktop (LWJGL3)**. Add **Web (TeaVM)** if you want to test the web backend.
    - **Template**: **Blank play state** for an empty starting point.
2. Click **Download project** and unzip the result to a folder of your choice.
3. Open a terminal in the unzipped folder and verify the project boots:
   ```bash
   ./gradlew :lwjgl3:run
   ```
   (Use `gradlew.bat :lwjgl3:run` on Windows.)

Gradle auto-downloads a matching JDK toolchain on the first build. After a brief first-time download you should see the empty game window.

> [!TIP]
> If you toggle on Expert mode, some new options will be available to you, including JitPack setup and an option to provide a composite build path.

The template pre-creates a `FlixelGame` subclass and a `FlixelState` in `core/src/main/java/...` so you can start adding game logic immediately. Open those files and replace the state’s `create()` method with your test code.

**Platform run commands:**

| Platform | Generator option | Run command |
|----------|-----------------|---|
| **Desktop (LWJGL3)** | Check **Desktop (LWJGL3)** | `./gradlew :lwjgl3:run` |
| **Web (TeaVM)** | Check **Web (TeaVM)** | `./gradlew :teavm:run` |
| **Android** | Coming soon | `./gradlew :android:installDebug` |
| **iOS** | Coming soon | - |

Now that you have the project downloaded, you need to actually test your local changes, rather than from Maven Central. There are two methods of doing this:

- **Method 1**: Publish the framework to your local Maven repository and pull it in via `mavenLocal()`.
- **Method 2 (recommended)**: Use a **Gradle composite build** so your test project compiles directly against the FlixelGDX source, meaning no republishing needed after every change.

### Method 1: Using `mavenLocal()`

The generated project targets Maven Central by default, pulling the last stable release of FlixelGDX rather than your local clone. To test your local changes:

1. In the FlixelGDX repo, publish to your local Maven repository:
   ```bash
   ./gradlew publishToMavenLocal
   ```
2. In the test project’s root `build.gradle`, add `mavenLocal()` to the `repositories` block before `mavenCentral()`.

> [!NOTE]
> You must re-run `publishToMavenLocal` each time you change the framework and want the test project to pick up those changes. Use [Method 2](#method-2-composite-build-intellij-or-any-gradle-based-ide) to avoid this.

If you launch the desktop module with your own LWJGL3 configuration instance, declare it as `FlixelLwjgl3ApplicationConfiguration` rather than raw `Lwjgl3ApplicationConfiguration` so Flixel can wrap any `Lwjgl3WindowListener` you install without relying on reflection (important for tools such as GraalVM Native Image).

### Method 2: Composite build (IntelliJ or any Gradle-based IDE)

Composite build lets the test project use your local FlixelGDX source so changes are picked up without republishing.

1. **Open your test project** in your IDE.
2. **Add the composite to the test project’s `settings.gradle`**, below the existing `rootProject.name` line:
  ```gradle
   includeBuild ‘/path/to/flixelgdx’
  ```
   Replace `/path/to/flixelgdx` with the path to your FlixelGDX clone.
  - **Windows**: use forward slashes, e.g. `C:/Users/You/flixelgdx`.
  - **macOS/Linux**: e.g. `/home/you/projects/flixelgdx`.
3. **Your existing dependency declaration stays as-is**; the composite build substitutes it automatically:
  ```gradle
   dependencies {
     implementation ‘org.flixelgdx:flixelgdx-core:<version>’
     // ... other dependencies
   }
  ```
4. **Refresh Gradle** (e.g. “Reload All Gradle Projects” or run a Gradle sync). The test project will compile against your FlixelGDX source; re-run or debug the game to see framework changes immediately.

---

## How to run unit tests

All unit tests live in a separate Gradle module, inside of `flixelgdx-test`. You can run the framework's unit tests with the following command:

```bash
./gradlew :flixelgdx-test:test
```

---

## Web (TeaVM) setup and configuration

FlixelGDX supports web builds through the **TeaVM** backend (`flixelgdx-teavm`). TeaVM transpiles
Java bytecode into JavaScript so your game can run in a browser.

> [!NOTE]
> Projects generated by the **FlixelGDX project generator** already use the correct TeaVM setup
> described below and no extra steps are needed. If you have an older project originally created
> with gdx-liftoff, note that Liftoff generates a module using the old `backend-teavm` +
> `TeaVMBuilder.java` approach, which is incompatible with FlixelGDX's `backend-web` +
> `org.teavm` Gradle plugin approach. Delete `TeaVMBuilder.java` and replace the entire
> `build.gradle` with the template below.

### Run the game in the browser

Running a FlixelGDX web game in your browser is as simple as executing the `run` task:

```bash
./gradlew :teavm:run
```

The output under `teavm/build/generated/teavm/` (or whatever you set as `teavm.all.outputDir`) is a self-contained folder you can serve with any HTTP server. 
Thanks to the framework's [TeaVM plugin](./flixelgdx-teavm-plugin), running this simple task will automatically:

- Copy `<rootProject>/assets/` to `<outputDir>/assets/` before each build.
- Generate a default `index.html` (with the correct canvas ID and script path) if you do not
  provide one in the `flixelgdx {}` extension block (refer to the [plugin module](flixelgdx-teavm-plugin/) and its Javadoc for more details).
- Wires everything to `generateJavaScript`.
- Opens a local port to test the game immediately.

### Optional plugin customization

Use the `flixelgdx {}` block to override defaults:

```gradle
flixelgdx {
  // Title of the game (default: "My FlixelGDX Game").
  title = 'My Game Title'

  // Canvas element ID (default: "flixelgdx-canvas").
  // Must match WebApplicationConfiguration.canvasID in your launcher.
  canvasId = 'my-canvas'

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
  }
);
```

### Platform limitations on web

The web backend intentionally omits several features that are unavailable in a browser
environment:

- **File logging** is disabled. There is no host filesystem, so `Flixel.startFileLogging()` is
  a safe no-op. Console output (`System.out.println`) maps to `console.log` in the browser.
- **Jansi / ANSI colors** are not installed. Terminal color codes are irrelevant in a browser
  console.
- **`FlixelGitUtil`** and other `ProcessBuilder`-based utilities are unavailable (no subprocess
  support in browsers).
- **`FlixelDefaultAssetManager.extractAssetPath()`** uses `java.io.File` for temp file
  extraction. On web, audio assets load through the browser's network stack and do not need
  filesystem materialization.

---

## Setting up the Android SDK (for contributing to the Android platform)

If you want to contribute to the **flixelgdx-android** module or run and test FlixelGDX on Android (in this repo or in a test project), you need the Android SDK and a way to run an Android app (emulator or physical device). This section covers installation, configuration, and the limitations and workarounds you may hit depending on your OS.

### The Android module is optional (no SDK required by default)

The framework repo **does not require an Android SDK** to compile. By default, the **flixelgdx-android** module is **not included** in the build, so you can clone, build core/desktop/TeaVM, and contribute without installing the SDK. Testing on Android is done in a **separate test project** (see [Testing with a test project](#testing-with-a-test-project)); that test project has its own Android app and its own SDK. Install the SDK there when you need to run on device or emulator.

To **build the Android module in this repo** (e.g. to work on Android-specific code or to publish the AAR), you must enable it in one of two ways (do **not** add this to the committed `gradle.properties`, as that would require everyone to have the SDK):

- **CI or one-off builds**: pass the property on the command line:
  ```bash
  ./gradlew -PincludeAndroid=true :flixelgdx-android:assembleRelease
  ```
- **Local development (recommended if you often work on Android)**: add a line to **local.properties** (this file is gitignored, so you never commit it):
  ```properties
  includeAndroid=true
  ```
  Then any normal `./gradlew ...` run will include the Android module. You still need the Android SDK and the setup steps below when the module is enabled.

### Why you need the Android SDK

- **Building the flixelgdx-android module in this repo**: after enabling it (see above), `./gradlew :flixelgdx-android:assemble` (or `assembleRelease`) requires the Android SDK and build tools.
- **Building a test project that includes an Android module**: requires the Android SDK in that project (and `sdk.dir` in that project’s `local.properties`).
- **Running on an emulator or device**: to verify behavior and debug, you need either an Android Virtual Device (AVD) or a physical Android device.

### Installing the Android SDK via Android Studio

Android Studio is the recommended way to install and manage the Android SDK. It provides the SDK, SDK Manager, AVD Manager, and emulator support in one installer, with a graphical UI for managing SDK versions.

1. Download [Android Studio](https://developer.android.com/studio) for your OS and run the installer.
2. During the setup wizard, keep the default selections - **Android SDK**, **Android SDK Platform**, and **Android Virtual Device** should all be checked.
3. Once installation completes, open Android Studio. On the welcome screen, go to **More Actions -> SDK Manager** (or once a project is open, **Settings -> Languages & Frameworks -> Android SDK**).
4. Under the **SDK Platforms** tab, make sure **Android API 36** is installed.
5. Under the **SDK Tools** tab, make sure **Android SDK Build-Tools 36** and **Android SDK Platform-Tools** are installed.
6. Note the **Android SDK Location** shown at the top of the SDK Manager window - you will need this path in the next step.

> [!TIP]
> The typical SDK location is `C:\Users\<You>\AppData\Local\Android\Sdk` on Windows, `~/Library/Android/sdk` on macOS, and `~/Android/Sdk` on Linux.

#### Command-line tools only (advanced)

If you prefer not to install Android Studio, download the [command-line tools](https://developer.android.com/studio#command-tools) and use `sdkmanager` to install the required packages:

```bash
sdkmanager “platform-tools” “platforms;android-36” “build-tools;36.0.0”
```

### Configuring the SDK for FlixelGDX

After installing the SDK, point Gradle at it by creating or editing `local.properties` in the **root** of the FlixelGDX repo (or your test project):

```properties
sdk.dir=/path/to/your/android/sdk
```

Use the SDK location path you noted from the Android Studio SDK Manager. On Windows, use double backslashes:

```properties
sdk.dir=C\:\\Users\\You\\AppData\\Local\\Android\\Sdk
```

If you are building the Android module in this repo, you can also add `includeAndroid=true` to the same file to avoid passing `-PincludeAndroid=true` on every command.

Next, accept the required SDK licenses. Open a terminal and run:

```bash
sdkmanager --licenses
```

If `sdkmanager` is not on your `PATH`, use the full path: `$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses`. You can also accept licenses directly in Android Studio via **SDK Manager -> SDK Tools -> Accept licenses**.

### Running on emulator or device

- **Emulator**: In Android Studio, open **Tools -> Device Manager** and create an AVD (e.g. a Pixel 9 with an API 36 system image). Start the AVD, then run your test project’s Android run configuration or `./gradlew :android:installDebug`. Enable hardware acceleration (see the platform notes below) for good performance.
- **Physical device**: Enable **Developer options** and **USB debugging** on the device (**Settings -> About phone -> tap Build number 7 times**), connect via USB, and run the same Gradle install task.

### Limitations and workarounds by platform

| Platform | Limitations | Workarounds |
|----------|-------------|-------------|
| **Windows** | Emulator can be slow without acceleration; Hyper-V and the Android Emulator can conflict. | Use **Windows Hypervisor Platform** (enable in Windows Features) for hardware-accelerated x86/x86_64 emulator images, or use a physical device. You can still **build** the module (`./gradlew -PincludeAndroid=true :flixelgdx-android:assemble`) and contribute code without running the app. |
| **macOS** | Apple Silicon (M-series) does not support x86 emulation well. | In AVD Manager, select an **ARM64** system image (labeled “Apple Silicon” or “arm64-v8a”) to run the emulator natively on M-series chips. |
| **Linux** | The emulator requires KVM for hardware acceleration. | Install KVM and add your user to the `kvm` group (`sudo usermod -aG kvm $USER`, then log out and back in). If you cannot run an emulator, build the module and test with a physical device or rely on CI. |

**Contributing without running on a device/emulator**

You can still edit and build the **flixelgdx-android** module without a device or emulator. Enable the module (add `includeAndroid=true` to `local.properties` or use `-PincludeAndroid=true`), then run `./gradlew :flixelgdx-android:assemble` to confirm it compiles. For runtime behavior, rely on a maintainer or CI that has an Android environment, or test with a physical device.

---

## Troubleshooting

### JAVA_HOME not set or wrong

- **Symptom**: Gradle or the IDE reports “JAVA_HOME is not set” or uses the wrong Java version.
- **Fix**: Set `JAVA_HOME` to the **JDK 17** installation directory (see [Java (JDK 17 with Eclipse Temurin)](#2-java-jdk-17-with-eclipse-temurin) for your OS). Use a **new** terminal/IDE restart after changing environment variables.

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

### Dependency not found: `org.flixelgdx:flixelgdx-core`

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
    sdk.dir=/path/to/android/sdk
    ```
  - Run `sdkmanager --licenses` (or accept licenses in Android Studio) and accept the required licenses.

### Line endings (CRLF vs LF) on Windows

- **Symptom**: `./gradlew` fails with “bad interpreter” or similar (often in Git Bash or WSL).
- **Fix**: Ensure `gradlew` uses Unix line endings (LF). In Git: `git config core.autocrlf input` and re-checkout, or run `dos2unix gradlew` if available. The repo should keep `gradlew` as LF.

### IDE does not pick up Gradle or JDK after install

- **Fix**:  
  - **IntelliJ**: File -> Invalidate Caches / Restart; and re-import the project or “Reload All Gradle Projects”.  
  - **VS Code/Cursor**: Run “Java: Clean Java Language Server Workspace” from the Command Palette, then reload.  
  - **Eclipse**: Project -> Clean; and Gradle -> Refresh Gradle Project.  
  - In all cases, confirm the **project** is using JDK 17 in its settings.

### Gradle daemon or port issues

- **Symptom**: “Address already in use” or daemon-related errors.
- **Fix**: Stop Gradle daemons: `./gradlew --stop`. Then run your build again. This project sets `org.gradle.daemon=false` in `gradle.properties`, so the daemon may already be disabled.

If you hit an error not listed here, open an issue with your OS, Java version (`java -version`), Gradle version (`./gradlew --version`), and the full error message so maintainers can help.
