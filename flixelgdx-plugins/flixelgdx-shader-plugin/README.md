# FlixelGDX Shader Plugin

Catch your shader mistakes at build time, not when the game opens.

FlixelGDX shaders are written in **WGSL**, the WebGPU shading language, and nothing else. Every platform compiles
the same `.wgsl` text at run time, so the plugin has no cross-compiling to do. Its job is to be a proofreader: at
build time it compiles each shader with **Dawn** (the exact compiler the game uses), fails the build with
`file:line:column` messages when something is wrong, and copies the shaders into the game's resources so
`FlixelShader.load(name)` finds them on every platform.

## Usage

```groovy
plugins {
  id 'java'
  id 'org.flixelgdx.shaders' version '0.1.0-beta'
}
```

Put shaders in `src/main/shaders` (subfolders are kept) and load them by path without the extension:

```java
FlixelShader tint = FlixelShader.load("tint");     // src/main/shaders/tint.wgsl
FlixelShader wave = FlixelShader.load("fx/wave");  // src/main/shaders/fx/wave.wgsl
```

The `processShaders` task runs before `processResources`, so a normal `build` or `run` validates and bundles every
shader. Run `./gradlew processShaders` to check shaders on their own.

## The WGSL contract

A shader file does not declare the camera matrix, the texture or the sampler. The framework puts a *preamble* in
front of every shader before compiling it. The preamble is the file
`flixelgdx-core/src/main/resources/org/flixelgdx/shaders/flixel_preamble.wgsl` (the single source of truth, and the
plugin bundles that very file at its own build time), so these names are always available:

| What | Where | Notes |
|------|-------|-------|
| `flixel` (`FlixelGlobals`) | `@group(0) @binding(0)` | `proj` (`mat4x4f`), `texSize` and `texelSize` (`vec2f`). |
| `flixelTexture` | `@group(0) @binding(1)` | `texture_2d<f32>` being drawn. |
| `flixelSampler` | `@group(0) @binding(2)` | Nearest or linear sampler. |
| Your uniform struct | `@group(1) @binding(0)` | Optional. Set its members from Java with `setUniform(...)`. |
| `FlixelVertexInput`, `FlixelVertexOutput` | | The vertex and the vertex-to-fragment structs. |
| Entry points | | `fs_main` is required. `vs_main` is optional (the default `flixel_vs` is used otherwise). |

A complete shader that mixes the sprite toward a color, driven by a group 1 uniform struct:

```wgsl
struct Params {
  tint: vec4f,
  amount: f32,
}
@group(1) @binding(0) var<uniform> params: Params;

@fragment
fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
  let c = textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
  return vec4f(mix(c.rgb, params.tint.rgb, params.amount), c.a);
}
```

```java
FlixelShader tint = FlixelShader.load("tint");
tint.setUniform("tint", FlixelColor.RED);
tint.setUniform("amount", 0.5f);
sprite.setShader(tint);
```

## What is checked

For every shader the plugin does what the renderer does when it creates a shader:

1. Joins the preamble and your file exactly like the framework does, and compiles the result as one module.
2. Builds a trial render pipeline with the framework's own bind group layouts (group 0: globals, texture and
   sampler; group 1: one uniform buffer), the framework's vertex layout and the entry points `fs_main` and
   `vs_main` (or `flixel_vs`). This catches what compiling alone cannot: a missing `fs_main`, or a group 1 binding
   that is not a uniform buffer.

Line numbers are mapped back to **your file** (the preamble's lines are subtracted), so editors and terminals can
jump straight to the problem. A failing build looks like this:

```
> Task :processShaders FAILED
* What went wrong:
Execution failed for task ':processShaders'.
> Found 2 shader errors:
  /game/src/main/shaders/broken.wgsl:5:3: error: expected ';' for variable declaration
    return vec4f(x);
    ^
  /game/src/main/shaders/nofs.wgsl: error: Entry point "fs_main" doesn't exist in the shader module.
```

Findings about a whole shader (such as a missing entry point) have no line and read
`<file>: error: <message>`. Warnings are printed and do not fail the build. The uniform struct's members are not
type-checked against `setUniform(...)` calls here; the renderer lays the struct out when the shader is created.

## How validation runs

The check calls Dawn through Java's foreign function API, so it runs in a **forked Java 25 process** (Gradle's
worker API with process isolation, started with `--enable-native-access=ALL-UNNAMED`), using the Java 25 toolchain
of the build. The process creates one Dawn device on the **Null backend**, a fake GPU that compiles and validates
but never draws, so **no graphics card or display is needed** and it works on CI. One device is shared by all
shaders of a run.

The jwebgpu libraries it needs (`jwebgpu-api`, `jwebgpu-ffm` and the `jwebgpu-natives-<os>-<arch>` jar of the
machine running the build) come from a dedicated `shaderValidator` configuration of the plugin, so they never reach
your game's own class path. While jwebgpu is consumed as a Gradle composite build (see COMPILING.md in the
framework), `includeBuild` it in your `settings.gradle` and leave `jwebgpuVersion` unset.

## Settings

All optional:

```groovy
shaders {
  sourceDir = layout.projectDirectory.dir('assets/shaders')  // default: src/main/shaders
  validate = true                                            // false only copies the files
  jwebgpuVersion = '0.1.0'                                   // default: unset (composite build)
}
```

## Output contract

Other plugins may rely on this.

- The task is `processShaders` (type `ProcessShadersTask`, group `shaders`).
- Its output directory, `ProcessShadersTask.getOutputDir()`, defaults to `build/generated/flixelgdx/shaders`. It is a
  **resource root**: a source file `<sourceDir>/<name>.wgsl` is written to
  `build/generated/flixelgdx/shaders/shaders/<name>.wgsl`, with subfolders kept.
- The directory is added to the `main` source set's resources, so the files end up on the class path as
  `shaders/<name>.wgsl`, which is where `FlixelShader.load(name)` looks.
- To serve the shaders as files (for example the web build), take the task's `outputDir` (or the files under its
  `shaders` folder) as an input of your task instead of hard-coding the path. That also wires the task dependency.
- The output only ever holds shaders that passed validation: it is emptied at the start of each run.
- The task is cacheable and incremental: it is up to date while the shaders, the settings and the plugin are
  unchanged, and skipped (`NO-SOURCE`) when there are no `.wgsl` files.

## Developing the plugin

The plugin's build copies `flixel_preamble.wgsl` and turns the constants of `FlixelWgsl` (entry point names, bind
group numbers, the globals size) into a small properties file, both bundled as plugin resources. This keeps one source
of truth: change the contract in `flixelgdx-core` and the plugin follows on its next build. If a constant is renamed
there, the plugin's `generateShaderContract` task fails and says so.

`./gradlew :flixelgdx-plugins:flixelgdx-shader-plugin:test` runs the helper tests and the Gradle TestKit builds. The
TestKit builds need the jwebgpu checkout and its desktop library; they are skipped when those are missing.
