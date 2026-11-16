# Graphics, Rendering, and Shaders

This guide explains how FlixelGDX draws a frame and what you can control: design size, render resolution, and render
scale; frame rate and vsync; blend modes and antialiasing; textures, images, and render targets; and custom WGSL
shaders on sprites, cameras, and the whole screen.

## Table of contents

1. [How a frame is drawn](#how-a-frame-is-drawn)
2. [Batching: why draw order affects speed](#batching-why-draw-order-affects-speed)
3. [Three sizes: design, render resolution, render scale](#three-sizes-design-render-resolution-render-scale)
4. [Crisp pixel art](#crisp-pixel-art)
5. [Frame rate and vsync](#frame-rate-and-vsync)
6. [Display modes and fullscreen](#display-modes-and-fullscreen)
7. [Blend modes](#blend-modes)
8. [Textures and images](#textures-and-images)
9. [Render targets: drawing off screen](#render-targets-drawing-off-screen)
10. [Shaders: what they are](#shaders-what-they-are)
11. [Writing a WGSL shader](#writing-a-wgsl-shader)
12. [Loading shaders and setting uniforms](#loading-shaders-and-setting-uniforms)
13. [Three places to put a shader](#three-places-to-put-a-shader)
14. [Animated shaders](#animated-shaders)
15. [Checking shaders at build time](#checking-shaders-at-build-time)
16. [Which GPU API is running?](#which-gpu-api-is-running)
17. [Render statistics](#render-statistics)
18. [Check your understanding](#check-your-understanding)

## How a frame is drawn

FlixelGDX draws everything with **WebGPU**, a modern graphics API that the framework uses on every platform. On
desktop, WebGPU in turn runs on Vulkan, Metal, or Direct3D 12, whichever your computer prefers; in the browser it uses
the browser's WebGPU; on Android it runs on Vulkan. One renderer, shared by every platform, means a shader or blend
mode looks the same everywhere.

You rarely touch the renderer directly. `Flixel.graphics` (a `FlixelGraphicsManager`) is the doorway when you need it,
and every sprite draws itself through a shared **batch** each frame. The exact order of a frame is described in
[The Game Loop](game-loop.md).

## Batching: why draw order affects speed

Sending work to the GPU has a fixed cost per "draw call," no matter how much is drawn in it. The batch collects
sprites into as few draw calls as possible: as long as consecutive sprites use the same texture, blend mode, and
shader, they go out together.

Think of mailing letters: sending 100 letters in one box is far cheaper than 100 separate trips to the post office.
Every time the texture, blend mode, or shader changes, the current box is sealed and sent, and a new one starts.

So, for speed:

- Put many small images on one sheet (an atlas) so sprites share a texture.
- Keep sprites that share a texture, blend mode, or shader next to each other in draw order (for example, in one group).
- Particle emitters already do this for their particles.

See [Optimization](optimization.md) and the render statistics at the end of this guide.

## Three sizes: design, render resolution, render scale

Three different "sizes" affect how your game looks. Picture a painting on a wall:

| Size                   | Analogy                    | What it controls                                                            | Set with |
|------------------------|----------------------------|-----------------------------------------------------------------------------|----------|
| **Design size**        | The picture                | How much of the world the camera shows, in game pixels. Your coordinates.   | `FlixelConfig.Builder.size(w, h)` |
| **Render resolution**  | The frame it hangs in      | The size the game is displayed at: the window size on desktop, or a fixed size stretched to fill fullscreen and the browser canvas. | `renderResolution(w, h)`, `Flixel.graphics.setRenderResolution(...)` |
| **Render scale**       | How finely it is painted   | How many pixels are drawn relative to the displayed size. `1` is one to one, `0.5` draws half as many and stretches (faster), above `1` supersamples (smoother). | `renderScale(s)`, `Flixel.graphics.setRenderScale(...)` |

By default the render resolution equals the design size, so a 640 by 360 game opens in a 640 by 360 window. To open a
bigger window for the same game, set a render resolution:

```java
new FlixelConfig.Builder("My Game")
    .size(640, 360)               // The world is 640 by 360 game pixels.
    .renderResolution(1280, 720)  // Displayed at 1280 by 720.
    .build();
```

On each platform, the render resolution behaves a little differently:

- **Desktop, windowed:** the window is that size.
- **Desktop, fullscreen:** the scene is drawn at that size and stretched to fill the monitor (with bars if the shape
  differs), like a monitor scaling a lower resolution.
- **Web:** the page decides the canvas size, so the scene is drawn at that size and stretched to fill the canvas.
- **Mobile:** ignored, since the screen is fixed. Use render scale instead.

Call `disableRenderResolution()` on the builder to draw fullscreen at the monitor's full native size instead.

Render scale is a performance and quality knob that works everywhere and never changes how much of the world is
visible. A "Graphics quality" option is often just this:

```java
Flixel.graphics.setRenderScale(0.75f); // Draw 75% of the pixels; good for weak phones.
```

Both can be changed at any time, for example from an options menu.

## Crisp pixel art

Pixel art looks best when every game pixel becomes a clean block of screen pixels. Three settings matter:

```java
new FlixelConfig.Builder("Pixel Game")
    .size(320, 180)
    .renderResolution(1280, 720, false) // false = nearest-neighbor when stretched, not blurry.
    .build();

// In create(), or globally:
Flixel.setAntialiasing(false);          // The default, but worth knowing.
Flixel.cameras.first().pixelPerfectRender = true; // Snap drawing to whole pixels.
```

Choose a render resolution that is a whole multiple of the design size (here, exactly 4 times) to avoid uneven
pixels.

## Frame rate and vsync

```java
new FlixelConfig.Builder("My Game").framerate(60).vsync(true).build();

Flixel.graphics.setTargetFps(144);   // Change the frame rate cap at runtime.
Flixel.graphics.setVSync(false);
int fps = Flixel.graphics.getFps();  // The measured frames per second.
```

**Vsync** waits for the monitor to finish showing one picture before sending the next. It prevents "tearing" (a
visible horizontal seam during fast motion) and saves power, at the cost of the frame rate following the monitor's
refresh rate. Because all motion uses `elapsed`, your game plays the same at 60, 120, or 144 frames per second.

## Display modes and fullscreen

```java
FlixelList<FlixelDisplayMode> modes = Flixel.graphics.getDisplayModes(); // For a resolution picker.
FlixelDisplayMode current = Flixel.graphics.getDisplayMode();

Flixel.window.setFullscreen(true);
Flixel.window.toggleFullscreen();
Flixel.window.setWindowed(1280, 720);
```

See [Window and Platform Services](window-and-platform-services.md) for everything the window can do.

## Blend modes

A blend mode decides how a sprite's colors combine with what is already drawn behind it:

| Mode       | Effect                                                    | Good for                                   |
|------------|-----------------------------------------------------------|--------------------------------------------|
| `NORMAL`   | Draws over, respecting transparency.                      | Almost everything.                         |
| `ADD`      | Adds light. Overlaps get brighter.                        | Fire, lasers, glows, magic, sparks.        |
| `MULTIPLY` | Darkens by multiplying colors.                            | Shadows, tinted glass, darkness overlays.  |
| `SCREEN`   | Lightens, the opposite of multiply.                       | Soft light, haze.                          |
| `SUBTRACT` | Subtracts color.                                          | Dark magic, negative effects.              |
| `LIGHTEN`  | Keeps the lighter of the two colors.                      | Highlights.                                |
| `DARKEN`   | Keeps the darker of the two colors.                       | Silhouettes.                               |

```java
glow.setBlendMode(FlixelBlendMode.ADD);
```

Every blend mode respects transparency on every platform, so soft-edged images blend correctly.

## Textures and images

Two layers are involved in every image:

- `FlixelTexture`: the raw image on the GPU, owned by the backend.
- `FlixelGraphic`: the shareable, reference-counted handle your code holds. Ten sprites loading the same file share one
  graphic, and the texture is freed when the last one lets go.

`loadGraphic(...)` and `makeGraphic(...)` manage all of this for you. For pictures you build pixel by pixel (a
procedural map, a minimap, a noise texture), fill a `FlixelImage` on the CPU and upload it:

```java
FlixelImage img = new FlixelImage(64, 64);
img.fill(0xFF0000FF);                              // RGBA8888: opaque red.
FlixelTexture texture = Flixel.graphics.createTexture(img);
img.destroy();                                     // The CPU copy is no longer needed.
```

## Render targets: drawing off screen

A **render target** is an invisible canvas. Instead of drawing to the screen, the batch draws into it, and the result
becomes a texture you can draw or run through a shader. It is how you make effects such as a reflection, a
picture-in-picture, a paint layer that accumulates, or a minimap.

```java
FlixelRenderTarget target = Flixel.graphics.createRenderTarget(320, 180);

target.begin();
// ... draw sprites, cameras, and so on ...
target.end();

FlixelTexture result = target.getTexture();
```

Most games never need one directly: cameras with shaders and global shaders use render targets for you.

## Shaders: what they are

A **shader** is a tiny program that runs on the GPU for every pixel of whatever is being drawn, deciding its final
color. It runs on thousands of pixels at once, which is why it can do things that would be far too slow on the CPU:
turning the screen gray, making water ripple, adding a CRT scanline effect, glowing outlines.

Think of a pair of tinted glasses: everything you look at passes through them on the way to your eyes. A shader is a
pair of glasses for the GPU.

FlixelGDX shaders are written in **WGSL**, WebGPU's shading language, and the same file runs on every platform.

## Writing a WGSL shader

A shader is one `.wgsl` file. You do not declare the camera matrix, the texture, or the sampler yourself: the
framework adds a "preamble" in front of every shader with these names already defined.

| Name                                      | What it is                                                      |
|-------------------------------------------|-----------------------------------------------------------------|
| `flixelTexture`                           | The texture of the sprite or picture being drawn.               |
| `flixelSampler`                           | How the texture is sampled (nearest or linear).                 |
| `flixel.proj`, `flixel.texSize`, `flixel.texelSize` | The projection matrix, the texture size, and one pixel's size in texture space. |
| `FlixelVertexOutput`                      | What reaches your fragment function: `uv` (texture position) and `color` (the sprite's tint). |
| Your own uniform struct                   | Optional settings at `@group(1) @binding(0)`, set from Java by name. |

Your file must contain a fragment entry point named `fs_main`. A vertex entry point `vs_main` is optional. Here is a
shader that fades the picture toward gray, with an `amount` setting Java can change:

```wgsl
// shaders/gray.wgsl
struct Params {
  amount: f32,
}
@group(1) @binding(0) var<uniform> params: Params;

@fragment
fn fs_main(in: FlixelVertexOutput) -> @location(0) vec4f {
  let c = textureSample(flixelTexture, flixelSampler, in.uv) * in.color;
  let g = dot(c.rgb, vec3f(0.299, 0.587, 0.114));
  return vec4f(mix(c.rgb, vec3f(g), params.amount), c.a);
}
```

Return ordinary (straight, not premultiplied) color, like the texture you sampled. The framework premultiplies the
result before blending, so every blend mode treats transparency correctly.

## Loading shaders and setting uniforms

`FlixelShader.load(name)` loads `shaders/<name>.wgsl` from your assets or resources and compiles it:

```java
FlixelShader gray = FlixelShader.load("gray");
gray.setUniform("amount", 0.8f);
```

`setUniform(...)` accepts floats, ints, 2 to 4 float components, `FlixelVector`, `FlixelColor`, and `FlixelMatrix`,
matched by the member name in your uniform struct. Values are uploaded at most once per change.

If the file is missing or does not compile, `isCompiled()` returns `false`, an error is logged, and drawing falls back
to no shader instead of crashing. You can also compile from a string with `new FlixelShader(wgslSource)`.

## Three places to put a shader

| Where              | How                                          | Affects                                    | Example                       |
|--------------------|----------------------------------------------|--------------------------------------------|-------------------------------|
| One sprite         | `sprite.setShader(shader)`                   | Only that sprite.                          | A glowing outline on a pickup. |
| One camera         | `camera.setShader(shader)`                   | Everything that camera shows.              | Underwater wobble on the world, not the HUD. |
| The whole screen   | `Flixel.graphics.addGlobalShader(shader)`    | The combined picture of every camera.      | A CRT filter, a color grade.  |

```java
sprite.setShader(outline);
Flixel.cameras.first().setShader(underwater);
Flixel.graphics.addGlobalShader(crt);
Flixel.graphics.removeGlobalShader(crt);
```

Global shaders **chain**: each one reads the previous one's output, so you can stack a color grade and a CRT effect.
The game's global overlay is drawn after the chain and is never affected by it. Remember that a per-sprite shader
breaks batching for that sprite, so prefer camera or global shaders for effects that cover many sprites.

## Animated shaders

Effects that move (ripples, scrolling noise, pulsing glows) need a time value that changes every frame. Subclass
`FlixelShader`, track time in `update(...)`, and upload it in `applyUniforms()`:

```java
public class WaveShader extends FlixelShader {

  private float time;

  public WaveShader(String wgsl) {
    super(wgsl);
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    time += elapsed;
  }

  @Override
  public void applyUniforms() {
    setUniform("time", time);
  }
}
```

`FlixelShader` is a `FlixelBasic`, so add it to your state (`add(wave)`) and the state updates it every frame and
destroys it with the state. A shader you create yourself and do not add should be destroyed with `destroy()` when you
are done, to free its GPU resources.

## Checking shaders at build time

Without help, a typo in a shader only shows up when the game runs. The FlixelGDX shader Gradle plugin compiles every
shader in `src/main/shaders` during the build, with the same compiler the game uses, and fails the build with
`file:line:column` messages. It then copies the shaders into your resources so `FlixelShader.load(...)` finds them on
every platform:

```groovy
plugins {
  id 'java'
  id 'org.flixelgdx.shaders' version '<flixel-version>'
}
```

```java
FlixelShader tint = FlixelShader.load("tint");    // src/main/shaders/tint.wgsl
FlixelShader wave = FlixelShader.load("fx/wave"); // src/main/shaders/fx/wave.wgsl
```

See [Building and Shipping](building-and-shipping.md).

## Which GPU API is running?

`Flixel.graphics.getApi()` returns a `FlixelGraphicsApi`, such as `WEBGPU` or `NOOP` (headless, during tests). On
desktop, WebGPU picks the best native API by itself; to force one for testing, pass `-Dflixel.render.backend=...` to
the JVM (for example `vulkan`). This is for troubleshooting driver problems, not something games normally set.

```java
if (Flixel.graphics.getApi() == FlixelGraphicsApi.NOOP) {
  return; // Running headless, such as in a unit test. Skip GPU work.
}
```

## Render statistics

`Flixel.graphics.getStats()` reports what the last frame cost: `draws` (draw calls), `triangles`, `vertices`,
`passes`, `submits`, `textureBytes` (GPU memory used by textures), `cpuMs`, and `presentMs`. The debugger's stats and
performance panels show these live (see [Debugging](debugging.md)). If `draws` is much higher than you expect, look
for texture, blend mode, or shader changes between sprites.

## Check your understanding

1. Your 320 by 180 pixel game looks blurry when fullscreen. Which two settings fix that?
2. You want a CRT effect on the game but not on a pause menu drawn by the global overlay. Where does the shader go?
3. Your shader's ripple never moves. What is missing?

(Answers: a render resolution with `smooth = false` (ideally a whole multiple of the design size) and antialiasing
off; a global shader, since the overlay is drawn after the global chain; the shader is not added to the state, so its
`update(...)` never runs, or `applyUniforms()` is not uploading the time.)

**Next:** [Window and Platform Services](window-and-platform-services.md)
