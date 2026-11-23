# Glossary

Plain-language definitions of the terms used throughout this handbook, in alphabetical order. Each entry links to the
guide that covers it.

## Table of contents

[A](#a) | [B](#b) | [C](#c) | [D](#d) | [E](#e) | [F](#f) | [G](#g) | [H](#h) | [I](#i) | [J](#j) | [K](#k) |
[L](#l) | [M](#m) | [N](#n) | [O](#o) | [P](#p) | [R](#r) | [S](#s) | [T](#t) | [U](#u) | [V](#v) | [W](#w)

## A

**Action.** A named control ("jump," "move") bound to any number of keys, buttons, sticks, and touch areas.
[Input Actions](input-actions.md)

**Allocation.** Creating a new object with `new`. Allocations in code that runs every frame create garbage.
[Optimization](optimization.md)

**Antialiasing.** Smoothing the edges of scaled or rotated images. Off by default, which keeps pixel art crisp.
[Graphics and Shaders](graphics-and-shaders.md)

**Asset.** A file the game loads into memory and shares: an image, sound, font, or data file.
[Files and Assets](files-and-assets.md)

**Atlas.** One image holding many smaller images, plus a description of where each one is. Sparrow atlases are a common
format. [Animation](animation.md)

## B

**Backend.** The platform-specific half of the framework (desktop, web, Android) that implements core's interfaces.
[Architecture](architecture.md)

**Batch.** A group of sprites sent to the GPU in one draw call. Changing texture, blend mode, or shader starts a new
batch. [Graphics and Shaders](graphics-and-shaders.md#batching-why-draw-order-affects-speed)

**Blend mode.** How a sprite's colors combine with what is behind it (normal, add, multiply, and so on).

**Boot callback.** Code registered with `Flixel.boot` that runs during startup, before or after the core managers are
created. [The Boot Sequence](boot-sequence.md)

**Boxing.** Wrapping a primitive (`int`, `float`) in an object (`Integer`, `Float`), which creates garbage.
[Collections and Pooling](collections-and-pooling.md)

## C

**Camera.** Decides which part of the world is visible and where on the window it is drawn. [Cameras](cameras.md)

**Clip.** A named sequence of animation frames, such as "walk." [Animation](animation.md)

**Collide.** Check whether objects touch, and push them apart if they do. Compare *overlap*.
[Physics and Collisions](physics-and-collisions.md)

**Config.** The `FlixelConfig` built in your game class: title, company, version, size, frame rate, and render
settings. [Getting Started](getting-started.md)

**Core.** `flixelgdx-core`, the platform-neutral framework your game depends on. [Architecture](architecture.md)

## D

**Dead zone (camera).** A box in which the camera's target can move without the camera following. [Cameras](cameras.md)

**Dead zone (gamepad).** Small stick values that are ignored, so a stick at rest does not cause drift. [Input](input.md)

**Debug mode.** A runtime mode with extra checks that connects to the debugger app. [Debugging](debugging.md)

**Design size.** The resolution you design for, in game pixels. Your coordinates never change, whatever the window
size. [Graphics and Shaders](graphics-and-shaders.md#three-sizes-design-render-resolution-render-scale)

**Destroy.** Free an object for good. Never use it afterward. Compare *kill*.
[Basics, Objects, and Sprites](sprites-and-objects.md)

**Drag.** Slowing an object that has no acceleration on that axis, like friction. [Physics and Collisions](physics-and-collisions.md)

**Draw call.** One command to the GPU to draw a batch.

## E

**Ease.** A function that shapes a tween's motion, such as `quadOut` or `bounceOut`. [Tweens and Timers](tweens-and-timers.md)

**Elapsed.** The seconds since the previous frame, passed to `update(elapsed)`. [The Game Loop](game-loop.md)

**Emitter.** An object that launches and manages many particles. [Particles](particles.md)

## F

**Feed.** A stream of game data in a fixed format, for programs such as debugger tools and AI agents.
[Extending the Debugger](debugger-extending.md#feeds-the-debugger-for-programs)

**Flush.** Write save data to storage. Nothing is saved until you flush. [Saving Progress](saving.md)

**Frame.** One pass through the game loop (update, then draw). Also one image of an animation.

**Free camera.** A debug camera you can fly around while the game is paused or running. [Debugging](debugging.md)

## G

**Garbage.** Objects no longer used, which Java must eventually clean up, sometimes causing stutter.
[Optimization](optimization.md#why-garbage-causes-stutter)

**Global overlay.** A group drawn on top of every camera that survives state switches. [The Game Loop](game-loop.md)

**Group.** A container of objects that is itself an object. [Groups and Recycling](groups-and-recycling.md)

## H

**Hitbox.** The rectangle used for collisions: an object's position and size. Separate from its picture.
[Basics, Objects, and Sprites](sprites-and-objects.md#the-hitbox-and-the-picture-are-separate)

**HUD.** Heads-up display: score, health, and other UI drawn over the game, usually on its own camera.
[Text, HUDs, and Bars](text-and-ui.md)

## I

**Immovable.** An object that collisions never push, such as a floor. [Physics and Collisions](physics-and-collisions.md)

**Inspector.** The debugger panel that shows and edits the selected object's properties. [Debugging](debugging.md)

## J

**`justPressed`.** True only on the frame a button goes down. [Input](input.md)

## K

**Kill.** Switch an object off without freeing it, so it can be revived and reused. Compare *destroy*.

## L

**Language tag.** A short name for a language and optional region, such as `en`, `es`, or `pt-BR`.
[Localization](localization.md)

**Launcher.** The one-line platform entry point that installs the backend and starts your game.
[The Boot Sequence](boot-sequence.md)

**Lerp.** Linear interpolation: a value part of the way between two others. [Math and Utilities](math-and-utilities.md)

**Localization.** Showing the game's text in the player's language, by looking strings up by key.
[Localization](localization.md)

## M

**MCP.** Model Context Protocol, how AI agents connect to the debugger app's tools.
[Debugging](debugging.md#letting-an-ai-agent-debug-your-game)

## N

**No-op.** "No operation": a placeholder implementation that does nothing, installed before a backend runs.
[Architecture](architecture.md#safe-defaults-the-no-op-implementations)

## O

**Offset.** How far a sprite's picture is shifted from its hitbox. [Basics, Objects, and Sprites](sprites-and-objects.md)

**Overlap.** Check whether objects touch, without moving them. Compare *collide*.

## P

**Panel.** A tab in the debugger app, built by the game from widgets. [Custom Debugger Panels](debugger-panels.md)

**Parallax.** Layers that scroll at different speeds to suggest depth. [Cameras](cameras.md#parallax-with-scroll-factors)

**Plural rule.** A small function that picks which plural form ("one", "few", "other", and so on) a language uses
for a count. [Localization](localization.md#plurals-1-coin-versus-2-coins)

**Polling.** Asking for input state each frame instead of waiting for events. [Input](input.md)

**Pool.** A store of reusable objects handed out and returned instead of created and destroyed.
[Collections and Pooling](collections-and-pooling.md#object-pools)

## R

**Recycle.** Revive a dead group member for reuse, creating a new one only if needed.
[Groups and Recycling](groups-and-recycling.md)

**Release mode.** The default runtime mode, the one players get.

**Render resolution.** The size the game is displayed at (the window size on desktop).
[Graphics and Shaders](graphics-and-shaders.md#three-sizes-design-render-resolution-render-scale)

**Render scale.** How many pixels are drawn relative to the displayed size; a quality and performance knob.

**Render target.** An off-screen canvas the batch can draw into. [Graphics and Shaders](graphics-and-shaders.md#render-targets-drawing-off-screen)

**Runner.** The platform's main loop, which creates the window and drives each frame. [The Boot Sequence](boot-sequence.md)

## S

**Scroll factor.** How much an object moves when the camera scrolls. `0` pins it to the screen. [Cameras](cameras.md)

**Seam.** An interface in core that each backend implements, such as `FlixelFiles` or `FlixelWindow`.
[Architecture](architecture.md#seams-the-interfaces-backends-fill-in)

**Seed.** The starting point of a random generator. The same seed always gives the same numbers.
[Math and Utilities](math-and-utilities.md#seeds-making-randomness-repeatable)

**Shader.** A small program that runs on the GPU and decides the color of each pixel. FlixelGDX shaders are written in
WGSL. [Graphics and Shaders](graphics-and-shaders.md)

**Signal.** A list of listeners notified together when something happens. [Signals](signals.md)

**Sink.** A destination for log entries, such as the console or a file. [Logging](logging.md)

**Slot (save).** A name that keeps several independent saves for one game. [Saving Progress](saving.md)

**Sprite.** An object with a picture. The class you use most. [Basics, Objects, and Sprites](sprites-and-objects.md)

**State.** One screen of your game, such as a menu or a level. [States and Substates](states-and-substates.md)

**Substate.** A smaller state opened on top of another, such as a pause menu.

## T

**Tag.** A label stamped on log messages from one logger. [Logging](logging.md#tagged-loggers)

**TeaVM.** The compiler that turns the game's Java bytecode into JavaScript or WebAssembly for the web.
[Platform Guide](platforms.md#web)

**Time scale.** A multiplier on `elapsed` for slow motion or fast-forward. [The Game Loop](game-loop.md)

**Tween.** A smooth change of a value over time. [Tweens and Timers](tweens-and-timers.md)

## U

**Uniform.** A setting passed from Java to a shader, such as a time or a color. [Graphics and Shaders](graphics-and-shaders.md)

## V

**Velocity.** Speed in pixels per second. [Physics and Collisions](physics-and-collisions.md)

**Viewport.** Maps a camera's view onto part of the window, with a scaling policy (fit, extend, or stretch).
[Cameras](cameras.md#fitting-any-window-viewports-and-scaling)

## W

**Watch.** A named value the debugger shows live. [Extending the Debugger](debugger-extending.md#watches-live-values)

**WebGPU.** The modern graphics API FlixelGDX draws with on every platform. [Graphics and Shaders](graphics-and-shaders.md)

**WGSL.** The WebGPU Shading Language, used for every FlixelGDX shader.

**Widget.** A building block of a debugger panel: a button, label, table, graph, and so on.
[Custom Debugger Panels](debugger-panels.md)

**World coordinates.** Positions in the game world, after undoing the camera. Compare screen coordinates.
[Input](input.md#screen-space-versus-world-space)

**Back to the [index](README.md).**
