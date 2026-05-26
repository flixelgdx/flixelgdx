# FlixelGDX — project instructions for Claude Code

---

## Project context (FlixelGDX)

FlixelGDX is a general purpose Java framework built on libGDX. It aims for strong performance and tooling so game development stays modernized, approachable, and accessible.

The overarching goal is to bring HaxeFlixel-style features into Java's ecosystem while staying as memory-efficient as humanly possible without giving up features games need.

This repository is the **standalone framework**. Runtime verification happens in a **separate test project**, consuming the framework via `publishToMavenLocal`, a composite build, or JitPack from a GitHub branch or commit.

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

### Performance and allocations

**Do not allocate objects inside loops or in methods invoked every frame.** That rule is strict. Prefer reuse, pooling (FlixelGDX and libGDX), indexed `for` loops, and performance-oriented helpers such as FlixelGDX's `FlixelString` or libGDX's `ObjectMap`.

### Architecture and scope

- `flixelgdx-core` is the main surface most game code uses. Keep backend or platform quirks out of core; abstract with interfaces where behavior differs per platform.
- Keep changes minimal: avoid unrelated files unless needed for the stated task.

### Language and style

- Target **Java 17**. Prefer modern features (records, lambdas, modern `switch`) over legacy patterns.
- **Import** every type you use. Do **not** use star imports (`*`).
- Do **not** use fully qualified class names inline when a normal import would read cleanly.
- Empty or void-returning methods: opening brace on the same line per `.editorconfig` (example: `public void hook() {}`).
- Prefer **short** field and method names. If shortening a method name hides meaning, use a concise name plus Javadoc instead of a long identifier.

### Finishing work

Summarize edits in plain language: what changed, why, and how it fits the system.

Before considering a coding task finished, **run unit tests** and **Javadoc lint**; fix failures.

---

## Documentation, comments, and Javadoc

Documentation should read like a **beginner-friendly handbook**, not an expert-only manual.

### Mechanics

- Use correct grammar and punctuation everywhere (comments, `@param`, `@return`, `@throws`, and so on).
- Stick to **ASCII** in prose when practical; avoid decorative punctuation like en dash, em dash, fancy arrows or emojis. This applies
  to both source comments and Javadoc. Use a plain hyphen (-) only for compound adjectives; never use it as a sentence separator or
  stand-in for an em dash. This rule also applies to inline comments in Groovy/Gradle files.
  This allows the docs to be read easily on every device and requires you to use clarity over brevity.
- Include the right Javadoc tags (`@param`, `@return`, `@throws`, …) wherever they apply.
- Use nullability annotations (`@Nullable`, `@NotNull`) where they help tooling.
- Keep `@link` references valid or fix broken links.
- Follow `.editorconfig` for formatting.
- Add comments where either complexity would otherwise be hard to follow, or where code requires import context.
- Skip Javadoc on trivial, self-explanatory methods (plain getters/setters or something like `calculateTotal()` unless there is subtle behavior).
- `.java` files should carry the project's standard copyright header (exceptions: `package-info.java`, `module-info.java`).
- Prefer **American English** in docs.
- After code changes that affect public behavior or APIs, **update relevant Markdown docs** in the repo.

### Comments versus Javadocs

- **In line comments**, do **not** use Markdown tricks (bold with `**`, backticks around snippets, etc.). Reserve richer formatting for Javadoc.
- When naming methods inside comments, include parentheses (`someMethod()`). If parameters exist but you are not spelling them out, use `anotherMethod(...)`. Example class-qualified form: `SomeClass.someMethod(...)`.
- **In comments**, prefer `SomeClass.someMethod(...)` instead of Javadoc-style `SomeClass#someMethod(int)`.

### Heavily used or critical APIs

For widely used classes, fields, or methods — or anything central to correctness — include a **small usage example** in Javadoc showing correct typical use.

Refer to `.cursor/rules/documentation.mdc` in the repo for full good versus bad examples if you need longer samples while editing.

---

## Working with Git and Pull Requests

- Prefer **small, focused commits** as you finish logical slices of work so history stays readable.
- Use **one branch and one pull request** unless the user explicitly asks for more (for example stacked features or dependent work).
- Follow `CONTRIBUTING.md`, `PULL_REQUEST_TEMPLATE.md`, and project PR or commit conventions.
- If the user renames a pull request, **do not rename it back**; respect their title.

---

## When research is needed

If the task needs external reference beyond this repo:

1. Start from these canonical sources:

   - `https://api.haxeflixel.com/flixel/index.html`
   - `https://libgdx.com/wiki/`
   - `https://github.com/HaxeFlixel/flixel`
   - `https://github.com/libgdx/libgdx`

2. If those are insufficient or off-topic for the question, broaden the search thoughtfully.

---

## Source of truth

The living Cursor rule files remain under `.cursor/rules/*.mdc`. If you need to know more about a rule,
you can always consult those files, as they go into much more detail than this file.
