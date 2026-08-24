# FlixelGDX Logging Plugin

Get accurate file and line numbers in every log call without touching game code.

This Gradle plugin post-processes compiled bytecode after each compile task, rewriting
`FlixelLogger` and `Flixel` log calls to site-aware overloads that record the exact source file and
line number at the call site. The result is that every log message carries precise location metadata,
even on platforms where stack walking is unavailable or unreliable, such as TeaVM-compiled web builds.

## Why it exists

On the JVM, a logger can call `Thread.currentThread().getStackTrace()` to find out where it was
called from. That works on desktop, but it is slow, and it is simply unavailable on some
platforms: TeaVM does not support stack walking in its JavaScript or WebAssembly output. A log
message from the web build could only say "somewhere in the game" rather than "PlayerState.java:47".

The approach here is different. The weaver runs after `javac` (and `kotlinc`) and inspects every
compiled class file. When it finds a `FlixelLogger.warn(...)` or `Flixel.error(...)` call it
rewrites the bytecode to call `warnWithSite(...)` instead, inserting the source file name and line
number as constant arguments. Those values come from the `.class` file's debug info, which `javac`
emits by default. The result is zero-overhead location metadata: the strings are baked in as
constants, no stack walk happens at runtime, and it works identically on every platform.

## Applying the plugin

Apply it in each module that uses `FlixelLogger` or the `Flixel` static logging helpers:

```groovy
plugins {
  id 'org.flixelgdx.logging' version '<flixel-version>'
}
```

No further configuration is required. Weaving is enabled by default and runs transparently after
every `compileJava` and `compileKotlin` task.

## Configuring the extension

```groovy
flixelgdxLogging {
  // Whether the weaver runs at all (default: true).
  enabled = true

  // Print each class file path that is rewritten, at lifecycle log level (default: false).
  verbose = false

  // Also weave dependency JARs on runtimeClasspath (default: true). See below.
  weaveDependencies = true
}
```

## What gets rewritten

Two categories of call sites are rewritten:

**`FlixelLogger` instance calls.** An `INVOKEVIRTUAL FlixelLogger.warn(tag, msg)` in game code
becomes `INVOKEVIRTUAL FlixelLogger.warnWithSite(tag, msg, sourceFile, line, className, methodName)`,
where the four extra arguments are inserted as constants at compile time.

**`Flixel` static helpers.** An `INVOKESTATIC Flixel.warn(tag, msg)` in game code becomes
`INVOKESTATIC FlixelLoggingBytecodeHooks.bcWarn1(tag, msg, sourceFile, line, className, methodName)`,
routing through a generated dispatch hook that passes the metadata along to the active logger.

The following `FlixelLogger` levels are covered:

| Method                         | Rewritten to                              |
|--------------------------------|-------------------------------------------|
| `debug(msg)`                   | `debugWithSite(msg, ...)`                 |
| `debug(tag, msg)`              | `debugWithSite(tag, msg, ...)`            |
| `info(msg)`                    | `infoWithSite(msg, ...)`                  |
| `info(tag, msg)`               | `infoWithSite(tag, msg, ...)`             |
| `warn(msg)`                    | `warnWithSite(msg, ...)`                  |
| `warn(tag, msg)`               | `warnWithSite(tag, msg, ...)`             |
| `error(msg)`                   | `errorWithSite(msg, ...)`                 |
| `error(msg, throwable)`        | `errorWithSite(msg, throwable, ...)`      |
| `error(tag, msg)`              | `errorWithSite(tag, msg, ...)`            |
| `error(tag, msg, throwable)`   | `errorWithSite(tag, msg, throwable, ...)` |

## Dependency weaving

When `weaveDependencies` is `true` (the default), the plugin registers a Gradle artifact transform
that runs the same weaver over every JAR on `runtimeClasspath`. This covers log calls that originate
inside third-party libraries: if a dependency calls `FlixelLogger`, the rewritten version in the
transformed JAR carries the library's own source location instead of a generic fallback.

To disable dependency weaving while keeping source weaving active:

```groovy
flixelgdxLogging {
  weaveDependencies = false
}
```

This is useful if a dependency is sensitive to bytecode modification, or if you want to limit
rewriting strictly to your own compiled sources.

## Java and Kotlin support

Both `javac` compile tasks (`AbstractCompile`) and `kotlinc` compile tasks (KGP 2.x
`KotlinCompile`) are hooked. Kotlin compile tasks no longer extend `AbstractCompile` as of KGP
2.x, so they are detected by class hierarchy rather than type, and the weaver runs on their output
directory after each compilation.

## HTML5 support

When an HTML5 build task is present on the classpath, the plugin also prepends the woven view of
`runtimeClasspath` to its classpath before the TeaVM compilation step runs. TeaVM compiles from
bytecode, so it sees the already-rewritten calls and emits the site-aware variants into the JavaScript
or WebAssembly output. This is the only way to get accurate log locations in the web build, since
TeaVM's output has no runtime stack-walking capability.
