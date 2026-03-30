# Contributing to FlixelGDX

We welcome contributions! Whether you're fixing bugs, adding new features, or improving documentation, your help is appreciated. To ensure a smooth process for everyone, please follow these guidelines.

> [!TIP]
> You can learn more about the project structure and build system in the [Project Structure](PROJECT.md) and [Compiling](COMPILING.md) documents.

## Java runtime (JDK 17, OpenJ9)

FlixelGDX targets **Java 17**. For **local development, running games, and Gradle**, use a JDK that ships **Eclipse OpenJ9** as the VM — specifically **[IBM Semeru Runtime](https://developer.ibm.com/languages/java/semeru-runtimes/downloads/)** (Java 17, OpenJ9 edition).

**Why:** OpenJ9 typically uses **much less heap and RSS** than HotSpot-class JVMs (Oracle JDK, Eclipse Temurin, Amazon Corretto, Azul Zulu HotSpot, and most default `openjdk-17` packages from Linux distros). That matters for games and iterative runs.

**Do not** use Oracle’s JDK or other **HotSpot** builds as your default for this project unless you have no alternative. If `java -version` output mentions **OpenJ9**, you are on the right VM; if it mentions **HotSpot** (or only “OpenJDK” without OpenJ9), switch to Semeru.

CI uses **IBM Semeru (OpenJ9)** via GitHub Actions so builds match that recommendation.

**Gradle toolchains:** The build is configured so **Java compile and test tasks** use a **Java 17 toolchain with the Eclipse OpenJ9 VM** (`JvmImplementation.J9`), not HotSpot. The [Foojay Toolchains Resolver](https://github.com/gradle/foojay-toolchains) (see `settings.gradle`) can auto-provision a matching JDK when possible. If you truly cannot use OpenJ9 for a one-off local build, pass **`-Pflixel.toolchain.requireOpenJ9=false`** or set **`flixel.toolchain.requireOpenJ9=false`** in `gradle.properties` (not recommended for day-to-day work).

Full install steps for Windows, macOS, and Linux are in [COMPILING.md](COMPILING.md) (Java section).

## Automated tests

Framework unit tests live in the **`flixelgdx-test`** Gradle module (`:flixelgdx-test:test`). Add new tests next to the area they cover (packages under `me.stringdotjar.flixelgdx`). They depend on **`flixelgdx-core`** and use JUnit 5; tests that need libGDX statics use the headless backend (see `GdxHeadlessExtension` in that module).

## Workflow

To maintain a stable code base, we follow a specific branching model:

1. **The Develop Branch**: All pull requests must be made against the `develop` branch. Pull requests targeting the `master` branch will not be merged.
2. **Build Checks**: Your PR **MUST** pass all automated build checks. If the build fails, **the PR will not be considered for merging until the issues are resolved.**
3. **Commit Messages**: Keep commit messages concise and descriptive. Use the imperative mood (e.g. "Add tween callback" not "Added tween callback"). If the change fixes an issue, reference it in the message (e.g. "Fix NPE when sprite has no graphic (#123)").
4. **Scope**: Prefer one logical change per PR. Large features can be split into smaller, reviewable PRs (e.g. API first, then implementation).
5. **Discussion**: For large or breaking changes, consider opening an issue first to discuss the approach. For bugs, feel free to open a PR directly if the fix is clear.

## Coding Standards

FlixelGDX aims to be a high-quality framework. Please follow these standards so the codebase stays consistent and maintainable.

### Formatting

We use [EditorConfig](https://editorconfig.org) to keep indentation, line endings, and basic whitespace consistent across editors. The project root contains a `.editorconfig` file that defines:

- **Charset**: UTF-8
- **Line endings**: LF
- **Final newline**: Insert at end of file
- **Trailing whitespace**: Trim (except in Markdown, where it is left for line breaks)
- **Indentation**: 2 spaces (no tabs) for Java, Gradle, Kotlin, etc.
- **Line length**: 120 characters for Java and similar (see `.editorconfig` for scope)

EditorConfig only controls how your editor behaves when you type or when it applies basic rules (e.g. indent on Tab, trim on save). It does **not** run a full code formatter. For “Format” or format-on-save, your editor uses its own formatter (e.g. IntelliJ’s code style or a VS Code formatter). To avoid clashes, enable EditorConfig in your editor so its behavior matches the project, and use the Project code style where applicable.

#### Editor setup (use EditorConfig in your editor)

- **IntelliJ IDEA**
  - Go to **Settings → Editor → Code Style** and enable **“Enable EditorConfig support”** (on some versions it is under **Settings → Editor → General**).
  - Use the **Project** code style scheme so EditorConfig takes precedence over personal/IDE defaults. The project’s `.editorconfig` also includes IntelliJ-specific options (e.g. `ij_java_*`) for brace style and imports.
- **VS Code / Cursor**
  - EditorConfig is not built in. Install the **“EditorConfig for VS Code”** extension (publisher: **EditorConfig**, extension ID: `EditorConfig.EditorConfig`).
  - After that, the editor will follow `.editorconfig` for indent, line endings, and trim/final newline when you edit. Any “Format” or format-on-save still uses whatever formatter you have selected; configure that formatter to match the project (e.g. 2 spaces, 120 chars) so formatting and EditorConfig stay in sync.

Once EditorConfig is enabled, new and edited files will respect the project’s indentation and whitespace rules automatically.

#### Style rules (match the codebase)

- **Indentation**: 2 spaces (no tabs). Apply to Java, Gradle, and Markdown (where applicable).
- **Line length**: Prefer staying under 120 characters. Break long lines at natural points (e.g. after a comma, before an operator); avoid breaking in the middle of a word or string when possible.
- **Braces**: Opening brace on the same line as the declaration (K&R style), with a space before it. Single-statement blocks may stay on one line when readable.
- **Whitespace**: Trim trailing whitespace from every line. End each file with a single newline. Use a single blank line between methods and between logical sections; do not add multiple blank lines in a row unless the style already exists in that file.
- **Imports**: Use single-class imports (no `import foo.bar.*`). Order imports as the rest of the project does (typically: Java, then third-party, then project packages, with blank lines between groups).

Match the existing style in the file you are editing. When in doubt, run `./gradlew classes` and rely on the build; we expect code to look like it was written by a single person.

### Naming and Style

- **Types**: PascalCase for classes, interfaces, and enums (e.g. `FlixelSprite`, `FlixelTweenManager`).
- **Methods and variables**: camelCase (e.g. `updateMotion`, `velocityX`). Use descriptive names; avoid single-letter names except for trivial loop indices or well-known math (e.g. `x`, `y`).
- **Constants**: UPPER_SNAKE_CASE for static final constants (e.g. `MAX_VELOCITY`).
- **Final**: Use `final` for parameters and local variables when the reference or value is not reassigned. This clarifies intent and helps avoid mistakes.

When replicating HaxeFlixel or Flixel APIs, follow the existing naming in this project. If the original uses an underscore (e.g. in Haxe), we use camelCase in Java (no underscores in identifiers).

### When to Add Comments and Javadoc

**Javadoc (block comments starting with `/**`):**

- **Public API**: Every public class, interface, and public or protected method that is part of the framework’s API should have Javadoc. This includes constructors, getters/setters that are part of the API, and overrides of public methods when the behavior differs or is non-obvious.
- **What to include**: A short summary of what the type or method does; for methods, `@param` for every parameter, `@return` when the return value is not void, `@throws` when the method can throw. Use `{@link ClassName}` or `{@link #methodName}` to reference other types or methods, and `{@code expression}` for code or constant names. Use `<p>` to separate paragraphs when the description is long.
- **Private and package-private**: Javadoc is optional. Add it when the method or class does something non-obvious, has non-trivial invariants, or is easy to misuse. Do not add Javadoc that only repeats the method name (e.g. "Gets the x" for `getX()`).

**Inline comments:**

- **Complex logic**: Use short comments to explain *why* something is done when the reason is not obvious from the code (e.g. a workaround, a non-obvious invariant, or a performance-sensitive choice). Prefer clear naming and small methods so that most code does not need comments.
- **Avoid noise**: Do not comment the obvious (e.g. "increment i" next to `i++`). Do not leave commented-out code in the final patch; remove it or explain in the PR why it must stay.
- **TODOs**: Use `// TODO: description` for temporary workarounds or follow-ups. Prefer opening an issue and referencing it in the TODO if the follow-up is non-trivial.

**Example - public API with Javadoc:**

```java
/**
 * Updates the position of this object using its current velocity and acceleration.
 *
 * <p>Called automatically each frame when {@link #moves} is {@code true}.
 *
 * @param elapsed Seconds elapsed since the last frame.
 */
public void updateMotion(float elapsed) { ... }
```

**Example - inline comment for non-obvious behavior:**

```java
// Iterate in reverse so finished tweens can be removed without skipping elements.
for (int i = activeTweens.size - 1; i >= 0; i--) { ... }
```

### Code Quality

- **Single responsibility**: Keep methods and classes focused. If a method does more than one thing, consider splitting it or extracting helpers.
- **Meaningful names**: Names should reveal intent. Avoid magic numbers; use named constants or variables (e.g. `float duration = 0.5f` or `private static final float FADE_DURATION = 0.5f`).
- **Consistency**: Match the existing project style exactly (indentation, brace placement, spacing, ordering of modifiers). The codebase should look like it was written by a single person.

### Performance and Memory

- **Avoid allocations in hot paths**: Do not create new objects inside `update()` or `draw()` (or other per-frame code) unless necessary. Allocations in hot paths cause GC pressure and can cause hitches.
- **Reuse and pooling**: Reuse objects (e.g. a `Vector2` or `Rectangle` stored in a field) where possible. For frequently created and destroyed objects (e.g. bullets, particles), implement `Poolable` and use a `Pool` so instances can be recycled instead of discarded.
- **Libraries**: Prefer libGDX types and idioms (e.g. `Array`, `ObjectMap`, `Pool`) so behavior and performance align with the rest of the ecosystem.

### Examples

**Good code (reuse, no per-frame allocation):**

```java
public class MyObject extends FlixelSprite {
  private final Vector2 tempVector = new Vector2(); // Reusable object

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    tempVector.set(velocityX, velocityY).scl(elapsed);
    changeX(tempVector.x);
    changeY(tempVector.y);
  }
}
```

**Bad code (allocation every frame):**

```java
public class MyObject extends FlixelSprite {
  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    Vector2 movement = new Vector2(velocityX, velocityY).scl(elapsed); // Bad: new allocation every frame
    changeX(movement.x);
    changeY(movement.y);
  }
}
```

**Good Javadoc (with good comments and formatting) on a public method:**

```java
/**
 * Adds a new animation to the animations list if it doesn't exist already.
 *
 * @param name The name of the animation. This is what you'll use every time you use {@code playAnimation()}.
 * @param frameIndices An array of integers used for animation frame indices.
 * @param frameDuration How long each frame lasts for in seconds.
 */
public void addAnimation(String name, int[] frameIndices, float frameDuration) {
  Array<TextureRegion> animFrames = new Array<>();

  // Convert 1D indices (0, 1, 2...) to 2D grid coordinates.
  int cols = frames[0].length;
  for (int index : frameIndices) {
    int row = index / cols;
    int col = index % cols;
    animFrames.add(frames[row][col]);
  }

  animations.put(name, new Animation<>(frameDuration, animFrames));
}
```

**Bad Javadoc (with bad comments and formatting) on a public method:**

```java
/**
 * uuhhhhhhhhhhh idk
 *
 * @param name its da name
 * @param frameIndices eh you can figure it out...
 * @param frameDuration what do you think it is ._.
 */
public 
void addAnimation(String name, int[] frameIndices, float frameDuration) {
  Array<TextureRegion> animFrames = new Array<>(); // create a new array to store the animation frames

  // convert     indices (0, 1, 2, wtv) to  grid coordinates   .
  int cols = 
    frames[0].length;
  for(int index : frameIndices) 
    { // loop through the indices and convert them to grid coordinates
    int row = index /   cols; // calculate the row number
       int col = index%cols; // calculate the column number
     animFrames.add(frames // add the frame to the array
     [row] // the row number
     [col] // the column number
     );
  }

  animations.put(name, new Animation<>(frameDuration, animFrames)); // add the animation to the animations map
}
```

## Creating a Pull Request

A good PR is easy to review and merge.

- **Title**: Descriptive and brief (e.g. "Add FlixelSpriteGroup support", "Fix NPE in FlixelTween when settings are null").
- **Description**: Explain *what* was changed and *why*. If the PR relates to an issue, reference it (e.g. "Fixes #123"). For features, describe the intended use and any breaking changes.
- **Self-review**: Before submitting, run `./gradlew classes` and `./gradlew :flixelgdx-test:test` (and any other relevant tasks) and fix build failures. Ensure formatting and style match the rest of the project; see [Editor setup (use EditorConfig in your editor)](#editor-setup-use-editorconfig-in-your-editor) above to enable EditorConfig in your editor.
- **Tests**: If you add a new feature, include tests or clear steps to verify the behavior. For bug fixes, describe how to reproduce the bug and confirm the fix.
- **Review feedback**: Address review comments in new commits or by amending. Keep the discussion focused and update the PR description if the scope changes.
