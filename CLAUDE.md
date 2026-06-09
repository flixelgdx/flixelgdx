# FlixelGDX (project instructions for Claude Code)

---

## Project context (FlixelGDX)

FlixelGDX is a general-purposed Java framework built on libGDX. It aims for strong performance and tooling, so game development stays modernized, approachable, and accessible.

The overarching goal is to bring HaxeFlixel-style features into Java's ecosystem while staying as memory-efficient as humanly possible without giving up features games need.

This repository is the **standalone framework**. Runtime verification happens in a **separate test project**, consuming the framework via `publishToMavenLocal`, a composite build, or JitPack from a GitHub branch/commit.

---

## Collaboration before implementation

Treat the interaction as teamwork, not robotic task execution. Prefer brainstorming when the user's direction is ambiguous.

Before implementing anything (planning or coding):

1. Ask yourself whether the requested design or refactor is actually good for FlixelGDX.
2. If it hurts the framework, breaks invariants, or there is clearly a better path, **stop before editing files or running commands**.
3. Explain why (pros and cons), suggest better alternatives, and ask whether the user still wants to proceed.
4. If they confirm after that discussion, proceed as requested.

---

## Explaining things for beginners and contributors

The framework welcomes new contributors learning open source.

When explaining code or introducing patterns:

- Explain **why** before **how** (motivation before mechanics).
- Use analogies for complex systems; if the user gave no analogy topic, ask for one they like.
- End **complex** explanations with a short check-in question so you can verify understanding.
- Stay encouraging and professional. Assume intelligence but not deep familiarity with Java, libGDX, or FlixelGDX quirks.

---

## Code quality (non-negotiables)

### Performance, Memory, and allocations

- **Do not allocate objects inside loops or in methods invoked every frame.** That rule is strict. Prefer reuse, pooling (FlixelGDX and libGDX), 
indexed `for` loops, and performance-oriented helpers such as FlixelGDX's `FlixelString` or libGDX's `ObjectMap`.
- **Always put fields in the correct order for each class**. This one is also an extremely important rule to follow, and it's
  vital you **do not forget this**. This rule is specifically for ensuring objects are as lean and small as possible and to keep
  alignment padding as tight as possible. Follow the order below:

  1. `long`s and `double`s
  2. `int`s, `float`s, and object references 
  3. `short`s and `char`s
  4. `boolean`s and `byte`s

### Coding style

**Always put fields, modifiers, types, and methods in the correct order**. This keeps the code readable and consistent.
Follow the orders below:

#### Modifiers

1. `public`
2. `protected`
3. default
4. `private`
5. `static`
6. `final`

#### Fields, Methods/Functions and Types

1. Fields (following the alignment padding rule!)
2. Constructors, with smallest to largest parameters top to bottom
3. Methods (if there are overloads, order them smallest to largest parameters top to bottom)
4. Simple Getter/Setter methods (below every other method)
5. Inner classes
6. Inner interfaces
7. Inner enums

#### Example

```java
/*
 * Copyright notice here...
 */
package org.flixelgdx.example;

/**
 * Rich and detailed Javadoc here.
 *
 * <p>Other details here.
 */
public class PerformanceObject {

  public static final long GLOBAL_LIMIT = 5000L;
  public static final int MAX_RETRIES = 5;
  public static String systemNodeName = "PRIMARY_NODE";
  public static char systemMode = 'V';

  protected static double internalWeight = 42.42;

  static final double VERSION_STABILITY = 1.0;
  static final float GRAVITY_CONSTANT = 9.81f;

  private static Object sharedLock = new Object();
  private static byte systemHeader = 0x1;

  public final long createdAt;
  private double sensorValue;

  public int instanceId;
  protected float velocity;
  private String displayName;
  private List<String> metadata;

  public short localFlags;
  private char categoryCode;

  public boolean isActive;
  private byte checksum;

  public PerformanceObject() {
    this(-1);
  }

  public PerformanceObject(int instanceId) {
    this(instanceId, "GENERIC_INSTANCE", 0.0f);
  }

  public PerformanceObject(int instanceId, String displayName, float velocity) {
    this(instanceId, displayName, velocity, false);
  }

  public PerformanceObject(int instanceId, String displayName, float velocity, boolean isActive) {
    this.createdAt = System.currentTimeMillis(); // Final field assigned here
    this.instanceId = instanceId;
    this.displayName = displayName;
    this.velocity = velocity;
    this.isActive = isActive;
  }

  public static void sayHello() {
    System.out.println("Hello, world!");
  }

  public void logMessage() {
    logMessage("Default system heartbeat check.");
  }

  public void logMessage(String msg) {
    logMessage(msg, 1);
  }

  public void logMessage(String msg, int level) {
    System.out.println("[" + level + "] " + msg);
  }

  protected void processData() {
    this.checksum = 0x00;
  }

  private float addNum(float f1, float f2) {
    return f1 + f2;
  }

  public String getDisplayName() {
    return displayName;
  }

  private static final class InternalConfig {
    private long timeout;
    private int bufferSize;
  }

  public interface StateListener {
    void onTransition(boolean success);
  }
  
  public enum Priority {
    LOW,
    MEDIUM,
    HIGH
  }
}
```

### Architecture and scope

- `flixelgdx-core` is the main surface most game code uses. Keep backend or platform quirks out of core; abstract with interfaces where behavior differs per platform.
- Keep changes minimal: avoid unrelated files unless needed for the stated task.

### Language and style

- Target **Java 17**. Prefer modern features (records, lambdas, modern `switch`) over legacy patterns.
- **Import** every type you use. Do **not** use star imports (`*`).
- Do **not** use fully qualified class names inline when a normal import would read cleanly.
- Empty or void-returning methods: opening brace on the same line per `.editorconfig` (example: `public void hook() {}`).
- Prefer **short** field and method names. If shortening a method name hides its meaning, use a concise name plus Javadoc instead of a long identifier.

### Finishing work

Summarize edits in plain language: what changed, why, and how it fits the system.

Before considering a coding task finished, **run unit tests** and **Javadoc lint**; fix failures. **All** unit tests live
in the `flixelgdx-test` module, not scattered around multiple modules.

---

## Documentation, comments, and Javadoc

Documentation should read like a **beginner-friendly handbook**, not an expert-only manual.

### Mechanics

- Use correct grammar and punctuation everywhere (comments, `@param`, `@return`, `@throws`, and so on).
- Stick to **ASCII** in prose when practical; avoid decorative punctuation like en dash, em dash, fancy arrows, or emojis. This applies
  to both source comments and Javadoc. Use a plain hyphen (-) only for compound adjectives; never use it as a sentence separator or
  stand-in for an em dash. This rule also applies to inline comments in Groovy/Gradle files. This allows the docs to be read easily 
- on every device and requires you to use clarity over brevity. The Markdown docs are the only exception to this rule.
- Use **consistent capitalization and grammar** in prose and code.
- Every doc comment should always start with a single sentence, with detailed paragraphs following.
- Include the right Javadoc tags (`@param`, `@return`, `@throws`, …) wherever they apply.
- Use nullability annotations (`@Nullable`, `@NotNull`) where they help tooling.
- Keep `@link` references valid or fix broken links.
- Follow `.editorconfig` for formatting.
- Add comments where either complexity would otherwise be hard to follow, or where code requires import context.
- Skip Javadoc on trivial, self-explanatory methods (plain getters/setters or something like `calculateTotal()` unless there is subtle behavior).
- `.java` files should carry the project's standard copyright header (exceptions: `package-info.java`, `module-info.java`).
- Prefer **American English** in docs. (e.g., "behavior" instead of "behaviour")
- After code changes that affect public behavior or APIs, **update relevant Markdown docs** in the repo.
- Don't use section comments (like `// ---`). The code should be easily navigable simply by how it's organized; section comments are just noise.

### Comments versus Javadocs

- **In line comments**, do **not** use Markdown tricks (bold with `**`, backticks around snippets, etc.). Reserve richer formatting for Javadoc.
- When naming methods inside comments, include parentheses (`someMethod()`). If parameters exist, but you are not spelling them out, use `anotherMethod(...)`. Example class-qualified form: `SomeClass.someMethod(...)`.
- **In comments**, prefer `SomeClass.someMethod(...)` instead of Javadoc-style `SomeClass#someMethod(int)`.

### Heavily used or critical APIs

For widely used classes, fields, methods, or anything central to correctness, include a **small usage example** in Javadoc showing correct typical use.

---

## Working with Git and Pull Requests

- Prefer **small, focused commits** as you finish logical slices of work so history stays readable. For example, if the task
  involves a large refactor, **don't dump everything in one commit**; split each logical change into a separate commit.
- Use **one branch and one pull request** unless the user explicitly asks for more (for example, stacked features or dependent work).
- Follow `CONTRIBUTING.md`, `PULL_REQUEST_TEMPLATE.md`, and project PR or commit conventions.
- If the user renames a pull request, **do not rename it back**; respect their title.
- Your commit titles should be **short and descriptive**, not exceeding **72 characters**, and should not contain 
  **keywords in front (e.g. `fix`, `feat`, `refactor`, etc.)**. They should also be **present** tense. Examples:
    - "Update README with more descriptive content"
    - "Fix typos in documentation and refactor FlixelSprite"
    - "Fix rendering bug in FlixelCamera"
    - "Add missing Javadoc to FlixelCamera"
- If the current branch is not up to date with the remote, always pull the latest changes before changing any code.
- If the current branch is set to either `master`, `develop` or something else, **create a new branch off of the latest changes from `develop`**.
- When you're done with a task (and you haven't yet made one), **create a pull request**. Make sure it follows the [PR template](.github/PULL_REQUEST_TEMPLATE.md)
  exactly with all of your changes.
- Pull request titles should be read as **past tense**, in the format as if it was a new update to a game. Examples:
    - "Added experimental controller/gamepad support for games to be playable on more platforms such as console"
    - "Massively buffed the desktop/LWJGL3 backend with multiple new features, such as transparent window backgrounds, custom mouse icons, and more"
    - "Reworked the logging API and its stack trace system to be much more accurate using a custom logging plugin"
- All pull requests should target the **`develop`** branch, **never** the `master` branch unless it's absolutely necessary and explicity asked by the user.

---

## When research is needed

If the task needs external reference beyond this repo:

1. Start from these canonical sources:

   - `https://api.haxeflixel.com/flixel/index.html`
   - `https://libgdx.com/wiki/`
   - `https://github.com/HaxeFlixel/flixel`
   - `https://github.com/libgdx/libgdx`

2. If those are not enough or off-topic for the question, broaden the search thoughtfully.
