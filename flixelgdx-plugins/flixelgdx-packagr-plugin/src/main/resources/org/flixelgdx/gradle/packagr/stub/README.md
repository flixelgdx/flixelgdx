# packagr launcher stubs

This folder holds the native launcher that every packaged game runs as. The launcher is a tiny
program that contains no game code: at startup it reads the `packagr.cfg` next to it, loads the
bundled runtime's `libjvm`, creates a JVM in its own process, and calls the game's main class.
Running the JVM in-process (rather than spawning a child `java`) is what makes a packaged game show
up as a single process under its own name, like a native app.

The single source file is [`src/launcher.c`](src/launcher.c). One binary is committed per platform,
in a folder named `<os>-<arch>` matching the classifier the plugin looks up at package time:

```
linux-x86_64/launcher
linux-aarch64/launcher
windows-x86_64/launcher.exe
windows-aarch64/launcher.exe
macos-aarch64/launcher
```

All of these are committed and produced by the `build-launcher-stubs` GitHub workflow. There is no
`macos-x86_64` (Intel) launcher on purpose, since Apple has deprecated Intel Macs; packaging a macOS
x86_64 target fails on purpose with a clear message.

When a game is packaged for a platform that has no committed launcher, packaging stops with a clear
message instead of producing something that cannot start. To support a new platform, build its
launcher with the matching command below and commit the result here.

## Why the binaries are committed

The launcher depends only on the system C runtime and resolves `JNI_CreateJavaVM` at runtime, so a
single binary works with any JDK version and rarely needs rebuilding. Committing the finished
binaries keeps packaging fast and free of a C toolchain requirement, the same approach the shader
plugin takes with its bundled `shaderc` binaries. Only rebuild when `launcher.c` changes.

## Building

Every command needs the JNI headers (`jni.h` and its platform `jni_md.h`), which ship inside any
JDK under `include/`. The launcher does not link against `libjvm`, so only the headers are needed,
not a specific JDK to link with.

### Linux x86_64

Built with the system GCC and the host JDK's headers:

```sh
gcc -O2 -s -static-libgcc \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" \
  src/launcher.c -o linux-x86_64/launcher -ldl
```

### Windows x86_64

Built either with MinGW-w64 (works when cross-compiling from Linux) or with MSVC on Windows. The
`-mwindows` flag makes it a windowless GUI application, so no console window flashes when a player
double-clicks the game; startup errors are shown in a message box instead.

MinGW-w64 (the win32 `jni_md.h` is a few typedefs; copy it from a Windows JDK's `include/win32`):

```sh
x86_64-w64-mingw32-gcc -O2 -s -mwindows \
  -I"<win-jni-include>" \
  src/launcher.c -o windows-x86_64/launcher.exe -static -static-libgcc
```

MSVC (from a Developer Command Prompt):

```bat
cl /O2 /I "%JAVA_HOME%\include" /I "%JAVA_HOME%\include\win32" ^
  src\launcher.c /Fe:windows-x86_64\launcher.exe ^
  /link /SUBSYSTEM:WINDOWS /ENTRY:mainCRTStartup user32.lib
```

### macOS (aarch64)

Build on an Apple Silicon Mac with the Xcode command-line tools. Intel (x86_64) is deliberately not
built or supported.

```sh
clang -O2 \
  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" \
  src/launcher.c -o macos-aarch64/launcher
```

Note for macOS: windowing libraries such as SDL and GLFW must run on the process's first thread. The
stock `java` command runs your `main` on a *new* thread by default and reserves the first thread for
the Cocoa event loop; the `-XstartOnFirstThread` option flips that so `main` runs on the first
thread. That option is handled by the `java` launcher, not the JVM, so it is not a valid argument to
`JNI_CreateJavaVM`.

This launcher sidesteps the whole issue: it creates the JVM and calls the game's `main` directly on
the process's first thread, so `main` (and therefore `glfwInit`) already runs where macOS requires
it. LWJGL's first-thread check (`Configuration.GLFW_CHECK_THREAD0`) passes for the same reason, and
`-XstartOnFirstThread` is neither needed nor accepted here. If a game's config happens to carry that
argument, the launcher drops it rather than failing to start. Still, test a windowed game on macOS
after building the stub to confirm the window opens.

### Linux aarch64

Build natively on an ARM machine, or cross-compile with an aarch64 toolchain:

```sh
aarch64-linux-gnu-gcc -O2 -s -static-libgcc \
  -I"<aarch64-jni-include>" -I"<aarch64-jni-include>/linux" \
  src/launcher.c -o linux-aarch64/launcher -ldl
```

## Testing a stub

To confirm a freshly built stub works end to end, assemble a minimal package by hand: a `jre/` from
`jlink --add-modules java.base`, a `lib/` holding one jar with a `main` method, a `packagr.cfg`
pointing `main` at that class, and the launcher next to them. Running the launcher should start the
JVM and reach `main`.
