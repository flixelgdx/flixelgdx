# Window and Platform Services

Beyond drawing and input, games talk to the world around them: the window, the clipboard, notifications, file
pickers, phone vibration, message boxes, and facts about the device. This guide covers `Flixel.window`,
`Flixel.host`, `Flixel.haptics`, `Flixel.alert`, and `Flixel.runtime`, and how to write code that degrades gracefully
when a platform cannot do something.

## Table of contents

1. [Asking before you use: the supports pattern](#asking-before-you-use-the-supports-pattern)
2. [The window](#the-window)
3. [Fullscreen and resolution options](#fullscreen-and-resolution-options)
4. [Catching the close button](#catching-the-close-button)
5. [Transparent and borderless windows](#transparent-and-borderless-windows)
6. [Monitors](#monitors)
7. [Clipboard](#clipboard)
8. [Notifications and getting attention](#notifications-and-getting-attention)
9. [Keeping the screen awake](#keeping-the-screen-awake)
10. [Opening and saving files with the system dialog](#opening-and-saving-files-with-the-system-dialog)
11. [Opening a web page](#opening-a-web-page)
12. [Phone vibration](#phone-vibration)
13. [Message boxes](#message-boxes)
14. [Facts about the device and program](#facts-about-the-device-and-program)
15. [Which platform am I on?](#which-platform-am-i-on)
16. [Check your understanding](#check-your-understanding)

## Asking before you use: the supports pattern

Platforms differ. A browser cannot move its window; a phone has no window title; a desktop computer cannot vibrate.
Instead of crashing, unsupported calls simply do nothing, and each service has `supports...()` methods so your game
can hide options that would not work:

```java
fullscreenToggle.visible = Flixel.window.supportsFullscreen();
copyButton.visible = Flixel.host.supportsClipboard();
rumbleOption.visible = Flixel.haptics.isSupported();
```

Think of a universal remote control: every button exists on the remote, but some TVs ignore some buttons. Checking
`supports...()` is like greying out the buttons your TV does not have.

## The window

`Flixel.window` controls the game's window (on the web, the canvas and the browser tab):

```java
Flixel.window.setTitle("My Game - Level 3");
Flixel.window.setSize(1280, 720);
Flixel.window.setPosition(100, 100);
Flixel.window.setResizable(false);
Flixel.window.setDecorated(false);       // No title bar or border.
Flixel.window.setFloating(true);         // Always on top.
Flixel.window.setOpacity(0.9f);
Flixel.window.bringToForeground();

int w = Flixel.window.getWidth();
boolean focused = Flixel.window.isFocused();
```

| Feature                     | Desktop | Web                         | Android |
|-----------------------------|---------|-----------------------------|---------|
| Title                       | Yes     | The tab title               | No      |
| Size                        | Yes     | The canvas follows the page | No      |
| Position, opacity, decorations, floating | Yes | No                  | No      |
| Fullscreen                  | Yes     | Yes (after a click or key press) | Always fullscreen |

Window size and render resolution are related: see
[Graphics and Shaders](graphics-and-shaders.md#three-sizes-design-render-resolution-render-scale).

## Fullscreen and resolution options

```java
Flixel.window.setFullscreen(true);
Flixel.window.toggleFullscreen();
Flixel.window.setWindowed(1280, 720);
boolean full = Flixel.window.isFullscreen();
```

A typical options menu combines fullscreen with a resolution list from `Flixel.graphics.getDisplayModes()` and
`Flixel.graphics.setRenderResolution(w, h)`. Save the player's choice with [`Flixel.save`](saving.md) and apply it in
your first state.

On the web, browsers only allow fullscreen in response to a click, tap, or key press. If you ask at another moment, the
framework waits and enters fullscreen on the player's next input.

## Catching the close button

Sometimes you want to ask "Save before quitting?" when the player clicks the window's close button. Absorbing close
requests stops the window from closing by itself:

```java
Flixel.window.setAbsorbCloseRequests(true);

// Later, when the player confirms:
Flixel.quit();
```

Use this carefully. Players expect the close button to quit, so always provide an obvious way out, and do not trap
them in the game. Check `supportsAbsorbCloseRequests()` first. On the web, `Flixel.host.setExitConfirmation(message)`
asks the browser to show its own "Leave site?" prompt instead.

## Transparent and borderless windows

Desktop pets, overlays, and widget-style games can draw with a see-through background. Request a transparent
framebuffer in the config, then turn transparency on at runtime:

```java
new FlixelConfig.Builder("Desktop Pet").transparentFramebuffer(true).build();

// In create():
Flixel.window.setDecorated(false);
Flixel.window.setTransparencyActive(true); // Empty areas show the desktop behind the window.
```

Transparent framebuffers cost a little performance on low-end machines, so only request one when you use it.

## Monitors

```java
FlixelList<FlixelMonitor> monitors = Flixel.host.getMonitors();
FlixelMonitor primary = Flixel.host.getPrimaryMonitor();
if (primary != null) {
  Flixel.info("{}: {}x{} at {} Hz", primary.getName(), primary.getWidth(), primary.getHeight(),
      primary.getRefreshRate());
}
```

Each monitor also lists its own display modes. Browsers may need permission to see more than one screen; ask with
`Flixel.host.requestMonitorPermission()`.

## Clipboard

Copying is immediate. Pasting is **asynchronous** (the result arrives later, especially in browsers), so listen to the
`onTextPasted()` signal first, then ask:

```java
Flixel.host.copyToClipboard(levelCode);

// Once:
Flixel.host.onTextPasted().add(text -> levelCodeInput.setText(text));

// When the player presses "Paste":
Flixel.host.pasteFromClipboard();
```

If the clipboard is empty or holds no text, the signal is not fired.

## Notifications and getting attention

```java
Flixel.host.requestNotificationPermission();    // Needed on the web and some systems.
Flixel.host.sendNotification("Match found!", "Your game is ready.");

Flixel.host.requestAttention();                 // Flash the taskbar entry, bounce the dock icon, or flash the tab title.
```

Check `supportsNotifications()` before relying on them.

## Keeping the screen awake

Phones and some browsers dim and lock the screen when nobody touches it. During a cutscene or a long video, keep it
on, and remember to turn it off afterward:

```java
Flixel.host.keepScreenAwake(true);
// ... cutscene ...
Flixel.host.keepScreenAwake(false);
```

## Opening and saving files with the system dialog

The host can show the operating system's file dialogs, which is the right way to import and export player-made content
(levels, replays, screenshots):

```java
// Let the player pick a level file.
Flixel.host.pickFile(files -> {
  if (files.length > 0) {
    loadCustomLevel(files[0].readString());
  }
}, "json");

// Let the player save an export.
Flixel.host.saveFile(levelJson, "my-level.json", saved -> Flixel.info("Saved: {}", saved));
```

`pickFiles(...)` allows choosing several files. The extensions after the listener filter what the dialog shows. On the
web, the browser only opens a file dialog in response to a click or key press, so call these from your button
handling code. Check `supportsFilePicker()` first.

## Opening a web page

```java
Flixel.host.openUrl("https://example.com/my-game/credits");
```

## Phone vibration

`Flixel.haptics` vibrates the device itself (for controller rumble, see [Input](input.md#rumble)):

```java
if (Flixel.haptics.isSupported()) {
  Flixel.haptics.vibrate(30);                            // 30 milliseconds: a light tap.
  Flixel.haptics.vibrate(new long[] {0, 50, 50, 50}, -1); // A pattern of waits and buzzes; -1 = do not repeat.
  Flixel.haptics.cancel();
}
```

Keep vibrations short and offer an option to turn them off.

## Message boxes

`Flixel.alert` shows a native message box. It is meant for rare, important messages, such as a startup error, not for
in-game dialog:

```java
Flixel.alert.info("Welcome", "Thanks for playing the demo!");
Flixel.alert.warn("Warning", "Your save file is from a newer version of the game.");
Flixel.alert.error("Save Failed", "Could not write to disk. Check your permissions.");
```

The framework's crash handler also uses it to show uncaught errors to the player.

## Facts about the device and program

`Flixel.runtime` answers questions about the running program:

```java
FlixelRuntimeMode mode = Flixel.runtime.getMode();         // RELEASE, DEBUG, or TEST.
long javaHeap = Flixel.runtime.getJavaHeap();              // Bytes of Java memory in use.
long nativeHeap = Flixel.runtime.getNativeHeap();
boolean packaged = Flixel.runtime.isRunningFromJar();
boolean inIde = Flixel.runtime.isRunningInIDE();
FlixelRunEnvironment env = Flixel.runtime.getEnvironment();
```

It also owns the game's **main-thread queue**. Work that finishes on another thread (a network reply, a background
loader, a clipboard paste) must not touch sprites, text, or the GPU directly, because the game may be using them at
that moment. Hand it to `Flixel.runtime.queueMainThread(...)` instead, and the framework runs it on the main thread at
the start of the next frame:

```java
new Thread(() -> {
  String scores = downloadHighScores(); // Slow, so it runs on its own thread.
  Flixel.runtime.queueMainThread(() -> scoresText.setText(scores));
}).start();
```

The queue lives on the runtime, not on `Flixel.graphics`, because it is about which thread runs your code, not about
drawing. On Android the main thread is FlixelGDX's render thread (`flixel-render`), not the Android UI thread.

Screen density is on the host: `Flixel.host.getDensity()` and `Flixel.host.getPpi()`, useful for sizing touch controls
on phones.

## Which platform am I on?

Most code should never need to ask: the whole point of the framework is that the same code works everywhere. When you
truly must (for example, to hide a "Quit" button where quitting is not allowed), ask the host:

```java
FlixelPlatform platform = Flixel.host.getPlatform();
quitButton.visible = platform == FlixelPlatform.DESKTOP;
```

The values are `DESKTOP`, `HTML`, `ANDROID`, `IOS`, and `UNKNOWN`. `FlixelPlatform` is an open type, so a custom
backend can define its own. Prefer the `supports...()` checks when they exist: they keep working if a platform gains a
feature later.

For a human-readable description of the machine (the OS and Java version on desktop, the phone model on Android, or the
browser on the web), useful in bug reports, ask the host too:

```java
String device = Flixel.host.getDeviceDescription();
```

## Check your understanding

1. Your "Paste level code" button never fills in the text box, but there are no errors. What is missing?
2. Why does `Flixel.window.setFullscreen(true)` sometimes wait until the next click in the browser?
3. How do you hide the Quit button on platforms where quitting does not make sense?

(Answers: a listener on `Flixel.host.onTextPasted()` registered before calling `pasteFromClipboard()`; browsers only
allow fullscreen in response to player input; check `Flixel.host.getPlatform() == FlixelPlatform.DESKTOP`.)

**Next:** [Logging](logging.md)
