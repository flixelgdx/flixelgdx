# Logging: Messages, Levels, Tags, and Sinks

Logging is the simplest debugging tool: print a message, and read it back to find out what the game did. This guide
covers the four log levels, message placeholders, tagged loggers, display modes, where log output goes on each
platform, writing logs to files, and sending logs anywhere you like with custom sinks.

## Table of contents

1. [Why a logger instead of System.out](#why-a-logger-instead-of-systemout)
2. [The four levels](#the-four-levels)
3. [Placeholders and exceptions](#placeholders-and-exceptions)
4. [Tagged loggers](#tagged-loggers)
5. [Choosing how much to log](#choosing-how-much-to-log)
6. [Display modes: simple and detailed](#display-modes-simple-and-detailed)
7. [Where logs go on each platform](#where-logs-go-on-each-platform)
8. [Writing logs to files](#writing-logs-to-files)
9. [Custom sinks: sending logs anywhere](#custom-sinks-sending-logs-anywhere)
10. [Logs in the debugger](#logs-in-the-debugger)
11. [Logging habits that stay fast](#logging-habits-that-stay-fast)
12. [Check your understanding](#check-your-understanding)

## Why a logger instead of System.out

`System.out.println` works, but only on desktop: in a browser or on Android it goes nowhere useful. It also has no
levels (you cannot hide the chatty messages), no tags (you cannot tell which system printed what), and it builds a
string even when nobody reads it.

The framework's logger sends each message to the right place on every platform (the terminal, the browser console,
Android's logcat, a file, the debugger app), tells you which file and line it came from, and costs almost nothing for
messages that are filtered out.

## The four levels

| Level   | Shortcut           | Use it for                                                         |
|---------|--------------------|--------------------------------------------------------------------|
| `DEBUG` | `Flixel.debug(...)` | Detailed messages while developing: "jump pressed," "path found." |
| `INFO`  | `Flixel.info(...)`  | Normal milestones: "level 3 started," "save loaded."               |
| `WARN`  | `Flixel.warn(...)`  | Something odd but survivable: a missing optional file, low memory. |
| `ERROR` | `Flixel.error(...)` | Something broke: a save failed, a level file would not parse.      |

```java
Flixel.debug("Jump pressed.");
Flixel.info("Level {} started.", levelNumber);
Flixel.warn("Player health is low: {}", health);
Flixel.error("Could not load the level file!");
```

## Placeholders and exceptions

Each `{}` in a message is replaced by the next argument, in order:

```java
Flixel.info("Player at ({}, {}) with {} coins.", player.getX(), player.getY(), coins);
```

Prefer placeholders over string concatenation (`"Coins: " + coins`). The logger fills them in without building a new
string for every call, and skips the work entirely when that level is turned off.

A trailing exception that no `{}` used is attached to the entry, and its stack trace is printed:

```java
try {
  loadLevel(path);
} catch (Exception e) {
  Flixel.error("Could not load level {}", path, e); // path fills the {}, e is the attached exception.
}
```

## Tagged loggers

In a big game, "Loaded 12 items" is much more useful as "[Inventory] Loaded 12 items." A tagged logger stamps every
message with a tag. Keep one in a static field for each class or system that logs a lot:

```java
public class Pathfinder {

  private static final FlixelLogger LOG = Flixel.log.tagged("Pathfinding");
  private static final FlixelLogger GRID_LOG = Flixel.log.tagged("Pathfinding/Grid");

  public void find(int fromX, int fromY, int toX, int toY) {
    LOG.debug("Searching from ({}, {}) to ({}, {})", fromX, fromY, toX, toY);
  }
}
```

A slash in a tag has no special meaning, but it groups related messages so they are easy to filter. On Android, the tag
also becomes the logcat tag.

## Choosing how much to log

The logger throws away messages below its minimum level before doing any work for them. The default is `DEBUG`
(everything). A release build might only want warnings and errors:

```java
if (!Flixel.isDebugMode()) {
  Flixel.log.setLevel(FlixelLogLevel.WARN);
}
```

For a debug message that is expensive to *prepare* (not just to print), ask first:

```java
if (Flixel.log.isEnabled(FlixelLogLevel.DEBUG)) {
  Flixel.debug("Grid state:\n{}", grid.dumpToString()); // dumpToString() only runs when it will be printed.
}
```

## Display modes: simple and detailed

| Mode       | Shows                                                                      |
|------------|----------------------------------------------------------------------------|
| `SIMPLE`   | The location of the log call and the message. Familiar if you know Haxe's `trace()`. The default. |
| `DETAILED` | The time, class, method, and line of the call, plus the message.           |

```java
Flixel.log.setMode(FlixelLogMode.DETAILED);
```

The call site (file and line) is found lazily, only when something actually prints it. On desktop and Android the
framework reads it from the stack; in the browser, where there is no stack to read, a compiler plugin records it at
build time. Either way, you get correct file and line numbers without configuration.

## Where logs go on each platform

Each platform's launcher installs a console sink that suits it:

| Platform | Console output                                                                           |
|----------|------------------------------------------------------------------------------------------|
| Desktop  | The terminal, with colors.                                                               |
| Web      | The browser's developer tools console, using the matching method (`console.warn`, `console.error`, and so on). |
| Android  | Logcat. Read it with `adb logcat -s FlixelGDX:E` (or your own tag).                      |

Android debugging is covered in more depth in the repository's `ANDROID_DEBUGGING.md`.

## Writing logs to files

Players rarely run games from a terminal, so when something goes wrong in a shipped game, a log file is what you ask
them to send. On desktop, the launcher installs a file sink; start it once your game is running:

```java
Flixel.log.setMaxLogFiles(5);     // Keep the five newest files (the default is 10).
Flixel.log.startFileLogging();    // Writes to the platform's logs folder.
String folder = Flixel.log.getLogsFolder();
```

Turn it off with `stopFileLogging()`, or forbid it entirely with `setCanStoreLogs(false)`. Change the folder with
`setLogsFolder(absolutePath)`. There is no file sink in the browser.

## Custom sinks: sending logs anywhere

A **sink** is a destination for log entries. The logger formats each message once into a reused `FlixelLogEntry` and
hands it to every sink. Add your own to send logs anywhere: an in-game console, a crash reporter, a network service.

```java
public class ErrorCounterSink implements FlixelLogSink {

  public int errors;

  @Override
  public void write(FlixelLogEntry entry) {
    if (entry.getLevel() == FlixelLogLevel.ERROR) {
      errors++;
    }
  }
}

ErrorCounterSink counter = new ErrorCounterSink();
Flixel.log.addSink(counter);
// Later: Flixel.log.removeSink(counter);
```

A `FlixelLogEntry` carries `getLevel()`, `getTag()`, `getMessage()`, `getThrowable()`, `getTime()`, and `site()` (the
file, line, class, and method of the call). **The entry object is reused for every message**, so copy what you need
inside `write(...)` (for example `entry.getMessage().toString()`) instead of keeping the entry.

You can also replace the console sink (`setConsoleSink(...)`) or the file sink (`setFileSink(...)`) entirely. See
[Extending the Framework](extending-the-framework.md).

## Logs in the debugger

In debug mode, the debugger app's **Logs** panel shows the latest lines with level filters, a text filter, and the full
message and stack trace of the selected line. AI agents connected to the debugger can read the same log. See
[Debugging](debugging.md).

## Logging habits that stay fast

- **Do not log every frame** at `INFO` or above. A message per frame floods the console and slows the game, especially
  in the browser. If you need to watch a value continuously, use a debugger watch instead.
- **Use placeholders**, not concatenation.
- **Guard expensive preparation** with `isEnabled(...)`.
- **Keep tagged loggers in static final fields**, created once.

## Check your understanding

1. Why is `Flixel.debug("Position: " + x + ", " + y)` in `update(...)` worse than `Flixel.debug("Position: {}, {}", x, y)`?
2. A shipped game crashed on a player's computer. How could you get its log?
3. Your custom sink stores every `FlixelLogEntry` in a list, and they all show the same message. Why?

(Answers: the concatenated version builds a new string every frame even when debug messages are filtered out; turn on
`Flixel.log.startFileLogging()` in the game and ask the player for the file in the logs folder; the logger reuses one
entry object, so copy the message text inside `write(...)`.)

**Next:** [Debugging and the Debugger App](debugging.md)
