# FlixelGDX Shader Plugin

Write one shader. Run it everywhere.

This Gradle plugin lets a game author a single GLSL shader and have it work on every FlixelGDX
backend, with no per-platform authoring and no manual shader compilation. At build time it drives
bgfx's `shaderc` to cross-compile each shader into the OpenGL, Vulkan, Metal, and Direct3D bytecode
the runtime loads, then bundles the results into the game's resources.

## Why it exists

The FlixelGDX backends consume bgfx's compiled shader format, and the different graphics APIs each
need their own variant. Without this plugin a developer would have to write shaders in bgfx's
dialect and run `shaderc` by hand for every platform. The plugin removes that entirely: you write
ordinary GLSL, and the build produces every variant for you.

The plugin drives bgfx's real `shaderc` rather than synthesizing the binary format itself, because
`shaderc` is the same compiler that builds the framework's own sprite shader. Its output is
guaranteed to be the exact container `bgfx_create_shader` expects, down to the uniform reflection
table and the vertex/fragment signature hashes.

## Applying the plugin

In the game module's `build.gradle`:

```groovy
plugins {
  id 'java'
  id 'org.flixelgdx.shaders' version '0.1.0-beta'
}

flixelShaders {
  // Where the .glsl sources live (default: src/main/shaders).
  sourceDir = file('src/main/shaders')

  shader('crt') {
    fragment = 'crt.frag.glsl'
  }

  shader('wave') {
    fragment = 'wave.frag.glsl'
    vertex = 'wave.vert.glsl'   // optional
  }
}
```

The `compileFlixelShaders` task runs automatically before `processResources`, so a normal build
produces and bundles the compiled shaders.

## Authoring a shader

Write a plain GLSL fragment shader. The plugin injects the framework preamble, so you do not declare
the sampler or varyings yourself; you just use the names it provides:

| Name | Type | Meaning |
| --- | --- | --- |
| `u_texture` | `sampler2D` | The sprite or camera scene texture (bound at stage 0). |
| `v_texCoords` | `vec2` | The interpolated texture coordinate. |
| `v_color` | `vec4` | The interpolated vertex tint. |
| `flixel_texture(uv)` | function | Shorthand for `texture2D(u_texture, uv)`. |

Write the result to `gl_FragColor`. A minimal pass-through fragment shader:

```glsl
void main() {
  gl_FragColor = flixel_texture(v_texCoords) * v_color;
}
```

The vertex stage is optional. The built-in pass-through vertex shader is correct for almost every
sprite and camera effect, since the batch already transforms quad corners on the CPU. A custom
vertex shader may read `a_position` (`vec2`), `a_texcoord0` (`vec2`), and `a_color0` (`vec4`),
transform with `u_modelViewProj`, and must write `v_texCoords` and `v_color`.

## Loading at runtime

Load a compiled shader by the same name you declared in the build:

```java
FlixelShader crt = FlixelShader.load("crt");
Flixel.cameras.first().setShader(crt);
```

The framework selects the variant matching the active renderer. If a variant is missing (for
example, a Direct3D build produced on a non-Windows machine), the effect degrades to an unshaded
draw instead of crashing.

## Output layout

Each shader is written into the module's resources as:

```
shaders/<name>/glsl/{vs,fs}.bin      OpenGL (and the runtime fallback)
shaders/<name>/spirv/{vs,fs}.bin     Vulkan
shaders/<name>/metal/{vs,fs}.bin     Metal
shaders/<name>/dx11/{vs,fs}.bin      Direct3D 11 and 12
```

## Direct3D and the compiler

Every variant except Direct3D compiles on any host: the OpenGL, Vulkan, and Metal bytecode all
build fine on Linux, macOS, and Windows.

The Direct3D (`dx11`) variant is the one exception, because it is DXBC produced by Microsoft's FXC
compiler. FXC runs natively on Windows, and on other hosts through the `d3d4linux` Wine shim. When
no FXC compiler is found, the plugin skips just that one variant with a warning (the same way the
framework's own `scripts/build_shaders.sh` does) and still produces all the others.

So you do not need a Windows machine to develop shaders. Build the Direct3D variant wherever FXC is
available: a Windows CI runner (the recommended approach, see below) produces it automatically, or a
Linux/macOS host with Wine and the `d3d4linux` shim can produce it locally. A game that ships
without the `dx11` variant does not crash; the effect simply falls back to an unshaded draw on the
Direct3D renderer, and bgfx can also be pointed at the Vulkan or OpenGL renderer on Windows.

The `shaderc` binary is resolved in priority order: an explicit `shadercPath` in the
`flixelShaders` block, the binary bundled with the plugin for the current operating system, and
finally a `shaderc` found on the system `PATH`. See
[`tools/README.md`](src/main/resources/org/flixelgdx/gradle/shader/tools/README.md) for how the
bundled binaries are produced.
