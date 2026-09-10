/**
 * The graphics device, texture pipeline, and rendering primitives for FlixelGDX.
 *
 * <p>Game code reaches everything here through {@link org.flixelgdx.Flixel#graphics Flixel.graphics},
 * the central {@link org.flixelgdx.graphics.FlixelGraphicsManager FlixelGraphicsManager} backed by
 * the active GPU library (bgfx on native, WebGL in the browser). No backend type is ever named in
 * game code: you call this interface and the framework handles the rest. A safe no-op default
 * ({@link org.flixelgdx.graphics.FlixelNoopGraphicsManager FlixelNoopGraphicsManager}) is installed
 * before any backend starts, so {@code Flixel.graphics} is never {@code null}.
 *
 * <h2>Textures and graphics handles</h2>
 *
 * <p>Textures go through two layers. {@link org.flixelgdx.graphics.FlixelTexture FlixelTexture} is
 * the raw GPU handle, opaque and backend-owned; game code almost never creates one directly.
 * {@link org.flixelgdx.graphics.FlixelGraphic FlixelGraphic} is the reference-counted,
 * asset-manager-aware wrapper your game code holds. Multiple sprites loading the same path share one
 * {@code FlixelGraphic}; reference counting ensures the GPU texture is freed only when the last user
 * releases it.
 *
 * <p>The typical path is through {@link org.flixelgdx.FlixelSprite FlixelSprite}: call
 * {@link org.flixelgdx.FlixelSprite#loadGraphic(org.flixelgdx.file.FlixelFile) FlixelSprite.loadGraphic(FlixelFile)} to load a
 * path, or
 * {@link org.flixelgdx.FlixelSprite#makeGraphic(int, int, org.flixelgdx.util.FlixelColor) FlixelSprite.makeGraphic(...)}
 * to generate a solid-color rectangle on the fly. Both handle the graphic lifecycle automatically.
 *
 * <p>For cases where you need to build or decode pixels on the CPU first, use
 * {@link org.flixelgdx.graphics.FlixelImage FlixelImage} and upload the result to the GPU:
 *
 * <pre>{@code
 * // Build a 64x64 solid red texture from scratch:
 * FlixelImage img = new FlixelImage(64, 64);
 * img.fill(0xFF0000FF); // RGBA8888
 * FlixelTexture texture = Flixel.graphics.createTexture(img);
 * img.destroy();
 * }</pre>
 *
 * <p>{@link org.flixelgdx.graphics.FlixelFrame FlixelFrame} represents a rectangular sub-region of
 * a texture, carrying the extra trim metadata that packed Sparrow and atlas frames need. Frames are
 * what the animation system hands to the batch each time a sprite draws a clip frame.
 *
 * <h2>The sprite batch</h2>
 *
 * <p>{@link org.flixelgdx.graphics.FlixelBatch FlixelBatch} collects textured quads and submits
 * them to the GPU in as few draw calls as possible. Every drawable in the framework renders through
 * the shared batch from
 * {@link org.flixelgdx.graphics.FlixelGraphicsManager#getBatch() FlixelGraphicsManager.getBatch()}.
 * Game code rarely touches the batch directly;
 * {@link org.flixelgdx.FlixelSprite FlixelSprite},
 * {@link org.flixelgdx.FlixelCamera FlixelCamera}, and the scene graph drive it automatically
 * each frame.
 *
 * <h2>Render targets and post-processing</h2>
 *
 * <p>A {@link org.flixelgdx.graphics.FlixelRenderTarget FlixelRenderTarget} is an off-screen
 * surface: the batch draws into it instead of the screen, and the result can then be sampled as a
 * texture through a shader. Create one via
 * {@link org.flixelgdx.graphics.FlixelGraphicsManager#createRenderTarget(int, int) FlixelGraphicsManager.createRenderTarget(...)}
 * and wrap
 * {@link org.flixelgdx.graphics.FlixelRenderTarget#begin() FlixelRenderTarget.begin()} /
 * {@link org.flixelgdx.graphics.FlixelRenderTarget#end() FlixelRenderTarget.end()} around the
 * drawing you want to redirect:
 *
 * <pre>{@code
 * FlixelRenderTarget target = Flixel.graphics.createRenderTarget(320, 180);
 *
 * // In draw():
 * target.begin();
 * // ... draw sprites, cameras, etc. ...
 * target.end();
 * }</pre>
 *
 * <p>The easiest way to apply a post-processing effect to the entire scene is through
 * {@link org.flixelgdx.graphics.FlixelGlobalShaderPipeline FlixelGlobalShaderPipeline}, reached via
 * {@link org.flixelgdx.graphics.FlixelGraphicsManager#addGlobalShader(org.flixelgdx.util.FlixelShader) FlixelGraphicsManager.addGlobalShader(...)}.
 * Shaders added there chain automatically: each reads from the previous output and writes its
 * result forward.
 *
 * <pre>{@code
 * FlixelShader gray = FlixelShader.load("grayscale");
 * Flixel.graphics.addGlobalShader(gray);
 * }</pre>
 *
 * <h2>Shader programs</h2>
 *
 * <p>{@link org.flixelgdx.graphics.FlixelShaderProgram FlixelShaderProgram} is the low-level,
 * opaque handle to a compiled GPU shader program. It is created by the backend via
 * {@link org.flixelgdx.graphics.FlixelGraphicsManager#compileShaderProgram(String) FlixelGraphicsManager.compileShaderProgram(...)}
 * and is normally wrapped by a higher-level
 * {@link org.flixelgdx.util.FlixelShader FlixelShader} that manages uniforms and the destroy
 * lifecycle automatically. Prefer {@code FlixelShader} in game code; only reach for
 * {@code FlixelShaderProgram} when writing a custom renderer or backend.
 *
 * <h2>Viewports and display modes</h2>
 *
 * <p>{@link org.flixelgdx.graphics.FlixelViewport FlixelViewport} maps a world-space rectangle
 * onto a portion of the screen. Every {@link org.flixelgdx.FlixelCamera FlixelCamera} owns one.
 * Its {@link org.flixelgdx.graphics.FlixelViewport.Scaling Scaling} policy decides how mismatches
 * between the game's design resolution and the window are resolved:
 * {@link org.flixelgdx.graphics.FlixelViewport.Scaling#FIT FIT} letterboxes,
 * {@link org.flixelgdx.graphics.FlixelViewport.Scaling#EXTEND EXTEND} grows the visible world to
 * fill the screen, and {@link org.flixelgdx.graphics.FlixelViewport.Scaling#STRETCH STRETCH}
 * distorts.
 *
 * <p>{@link org.flixelgdx.graphics.FlixelDisplayMode FlixelDisplayMode} describes a single monitor
 * video mode (resolution, refresh rate, and color depth). Query the available modes through
 * {@link org.flixelgdx.graphics.FlixelGraphicsManager#getDisplayModes() FlixelGraphicsManager.getDisplayModes()}
 * to build a resolution picker in a settings screen, then hand the chosen mode to the window to
 * enter full-screen.
 *
 * <h2>Identifying the backend</h2>
 *
 * <p>{@link org.flixelgdx.graphics.FlixelGraphicsApi FlixelGraphicsApi} is an open, extensible
 * string identity for the active GPU backend. It is intentionally not an enum so that custom
 * renderers can mint their own IDs. Use it when you need to detect the backend at runtime, for
 * example to skip a feature unsupported in a specific environment:
 *
 * <pre>{@code
 * if (Flixel.graphics.getApi() == FlixelGraphicsApi.Noop) {
 *   // Running headless or in a test environment. Skip GPU work.
 * }
 * }</pre>
 *
 * @see org.flixelgdx.graphics.FlixelGraphicsManager
 * @see org.flixelgdx.graphics.FlixelGraphic
 * @see org.flixelgdx.graphics.FlixelBatch
 * @see org.flixelgdx.graphics.FlixelRenderTarget
 */
package org.flixelgdx.graphics;
