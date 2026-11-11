---
name: teavm-check
description: Check that Java code reachable on the web (HTML5 / TeaVM) only uses JDK classes and methods TeaVM's class library supports, and has no JavaScript- or WebAssembly-specific code outside the html5 backend. Use after changing core or html5 code, or when a web build fails with a missing class or method.
argument-hint: "[path or PR number] (defaults to Java files changed on the current branch)"
---

# TeaVM compatibility check

Check the Java files in `$ARGUMENTS` (or those changed in `git diff master...HEAD`) for web compatibility.

## Which code is in scope

Code that ends up in a web build: `flixelgdx-core`, `flixelgdx-html5`, and anything else the html5 module depends on. Desktop-only, Android-only, and Gradle plugin code is out of scope.

## How to check JDK usage

TeaVM reimplements the JDK in its class library. A JDK class is supported only if TeaVM ships a matching class, named with a `T` prefix: `java.util.Objects` becomes `org.teavm.classlib.java.util.TObjects`.

1. List every JDK type the files use: imports from `java.*` plus `java.lang` types used without imports.
2. Find the TeaVM version the build uses (`gradle/libs.versions.toml` or the html5 build files; it is pinned on purpose, so do not suggest upgrading it).
3. Look each type up in that version's class library. The JARs are in the Gradle cache:
   `find ~/.gradle/caches/modules-2 -name "teavm-classlib-<version>*.jar"`
   Then check with `unzip -l <jar> | grep "classlib/java/util/TObjects"`. For a method, read the class in the `-sources.jar` and confirm the method exists with the same parameters.
4. Pay extra attention to areas TeaVM supports only partly: threads and concurrency, `java.nio` beyond buffers, `java.time` formatting, regular expressions, `String.format`, reflection (banned anyway), `java.net`, files, and locales.

## Other web rules

- No JavaScript- or WebAssembly-specific code outside `flixelgdx-html5`, and none inside it either unless it works on both of TeaVM's targets.
- No reflection, and no APIs that need it under the hood.

## Report

For each problem give `file:line`, the class or method, why TeaVM cannot compile or run it (missing class, missing method, or partial support), and a replacement that also respects `AGENTS.md`. If everything is supported, say which types you checked.
