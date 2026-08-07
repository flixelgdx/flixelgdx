# FlixelGDX Migration: Off libGDX

> **Status:** Planning. No migration code has been written yet.  
> **Owner:** stringdotjar  
> **Started:** 2026-08-03  
> **This is a living document.** Every big decision made during the migration should be  
> recorded here in the Decision Log so future contributors (and future AI sessions) never  
> lose the thread.  
>
> **Execution model:** No one depends on the framework yet, so **breaking changes are fully  
> acceptable** - we optimize for the cleanest end design, not backward compatibility. Once this  
> plan is finalized, the actual migration will be carried out in a focused, roughly one-to-two  
> week sprint using Opus and Fable, converting and testing subsystems one at a time.  

---

## Table of contents

**Part I - Reference (the "why" and "what", condensed)**

- [1. Why we are doing this](#1-why-we-are-doing-this)
- [2. What we depend on today (inventory)](#2-what-we-depend-on-today-inventory)
- [3. Key insight and guiding principles](#3-key-insight-and-guiding-principles)
- [4. Target architecture (sketch)](#4-target-architecture-sketch)
- [5. Strategic decisions (open questions)](#5-strategic-decisions-open-questions)
- [6. Decision log](#6-decision-log)

**Part II - The plan (step by step)**

- [7. Roadmap at a glance](#7-roadmap-at-a-glance)
- [8. Phase 0 - Spikes and strategic decisions](#8-phase-0---spikes-and-strategic-decisions)
- [9. Phase 1 - Own the utilities (the neutral layer)](#9-phase-1---own-the-utilities-the-neutral-layer)
- [10. Phase 2 - Introduce the abstraction seam](#10-phase-2---introduce-the-abstraction-seam)
- [11. Phase 3 - Stand up the new backend (desktop first)](#11-phase-3---stand-up-the-new-backend-desktop-first)
- [12. Phase 4 - Remove libGDX from desktop](#12-phase-4---remove-libgdx-from-desktop)
- [13. Phase 5 - Other platforms](#13-phase-5---other-platforms)
- [14. Phase 6 - Cleanup and docs](#14-phase-6---cleanup-and-docs)

**Part III - Appendices**

- [15. Risks and mitigations](#15-risks-and-mitigations)
- [16. Per-platform notes](#16-per-platform-notes)
- [17. Status checklist](#17-status-checklist)

---

# Part I - Reference

> This part is the condensed background: why we are migrating, what we depend on, and the
> principles and decisions that shape the work. The actual step-by-step plan lives in Part II.

## 1. Why we are doing this

FlixelGDX is currently built on top of [libGDX](https://libgdx.com/). libGDX has served the
framework well, but it carries a structural problem we can no longer ignore:

- **libGDX is tightly coupled to OpenGL.** Its entire rendering model, backends, and public
  API assume an OpenGL (or GLES) context.
- **OpenGL is a dead end on modern platforms.** Apple deprecated OpenGL on macOS and iOS in
  favor of Metal. The wider industry has moved to Vulkan, Direct3D 12, and Metal, with WebGPU
  emerging on the web. OpenGL is legacy.
- **We want FlixelGDX to age gracefully.** A framework meant to last cannot be anchored to an
  API that the platform vendors are actively walking away from.

The goal is to remove the hard dependency on libGDX and, over time, replace it with a rendering
and platform layer we own, built on a modern, forward-looking GPU abstraction, with
FlixelGDX-style, well-documented replacements for the libGDX utilities we lean on today.

> **Reality check:** This is the single biggest change in the framework's history. It is a
> multi-phase, long-horizon effort. This document exists so we plan it deliberately instead of
> rushing it.

## 2. What we depend on today (inventory)

From a scan of `flixelgdx-core/src`: 183 Java files, **96 of them import `com.badlogic.gdx`**
directly (roughly half the core surface).

### 2.1 Core-level coupling (by libGDX subpackage)

| Area | libGDX package | Key types we use | Difficulty |
|---|---|---|---|
| Collections / utils | `com.badlogic.gdx.utils` | `Array`, `ObjectMap`, `SnapshotArray`, `Pool`, `IntArray`, `CharArray`, `ObjectSet`, `Disposable`, `Json`/`JsonReader`/`JsonValue`, `XmlReader` | Low - mostly mechanical (Phase 1). |
| App lifecycle / platform | `com.badlogic.gdx` | `Gdx` statics, `Application`, `Graphics`, `Files`, `Input`, `InputProcessor`, `InputMultiplexer`, `Screen`, `Preferences` | High - backends differ per platform. |
| Graphics primitives | `com.badlogic.gdx.graphics` | `Texture`, `Color`, `Pixmap`, `GL20`, `VertexAttributes` | High - heart of the coupling. |
| Math | `com.badlogic.gdx.math` | `MathUtils`, `Vector2`, `Rectangle`, `Affine2`, `Matrix4` | Low/medium - self-contained. |
| 2D rendering | `com.badlogic.gdx.graphics.g2d` | `SpriteBatch`, `Batch`, `TextureRegion`, `BitmapFont`, `Animation`, FreeType font gen | High - batcher must be rebuilt. |
| Controllers | `com.badlogic.gdx.controllers` | `Controller`, `ControllerMapping`, `ControllerListener` | Medium - SDL/GLFW gamepad. |
| Assets | `com.badlogic.gdx.assets` | `AssetManager`, `AssetDescriptor`, loaders | Medium - async pipeline. |
| Files | `com.badlogic.gdx.files` | `FileHandle` | Medium - platform file abstraction. |
| Viewports | `com.badlogic.gdx.utils.viewport` | `ExtendViewport`, `FitViewport` | Low. |
| GL utils | `com.badlogic.gdx.graphics.glutils` | `ShaderProgram`, `FrameBuffer` | High - GL-specific. |
| Scene utils | `com.badlogic.gdx.scenes.scene2d.utils` | `ScissorStack` | Low. |

### 2.2 Platform backends (the modules)

Two modules are being renamed for clarity: `flixelgdx-lwjgl3` -> `flixelgdx-desktop` and
`flixelgdx-teavm` -> `flixelgdx-web` (the rename lands during the backend rework, Phases 4-5).

| Module (post-rename) | Today's backend (libGDX) | Target backend (post-migration) |
|---|---|---|
| `flixelgdx-desktop` (was `-lwjgl3`) | LWJGL3: GLFW + OpenGL (+ gdx-freetype, gdx-controllers, basisu, **miniaudio** audio, imgui) | **bgfx** via LWJGL |
| `flixelgdx-android` | libGDX Android: GLES (+ miniaudio audio) | **bgfx** |
| `flixelgdx-ios` | libGDX MobiVM/RoboVM: GLES. Not supported yet. | **bgfx** (Metal) |
| `flixelgdx-web` (was `-teavm`) | gdx-teavm: WebGL via TeaVM | **WebGPU** via our own TeaVM bindings (+ WebGL fallback) |
| `flixelgdx-jvm` | Pure-JVM helpers (no GPU) | unchanged |

### 2.3 Already (partly) decoupled - lowers the cost

- **Audio does NOT use libGDX audio.** Desktop and Android use `miniaudio`
  (`games.rednblack.miniaudio`). Web is the open question.
- **Texture compression** is a separable Basis Universal plugin.
- **Logging** is already a custom system (`FlixelLogger` + logging plugin).

## 3. Key insight and guiding principles

### 3.1 The key insight

libGDX bundles two very different things:

1. A **neutral low-level layer**: collections, math, file handles, native bindings. Not tied to
   OpenGL.
2. An **OpenGL-coupled layer**: the rendering pipeline, `Gdx.gl*`, the g2d batcher, the
   windowing/backend integration.

**Only layer 2 is the actual problem.** The *binding* mechanism underneath is not: LWJGL (which
we already use on desktop) is a neutral binding provider that already exposes Vulkan, bgfx,
GLFW, OpenAL, and stb, not just OpenGL. So "write our own binds for everything" is the wrong
framing. The right framing:

> Pick a modern GPU abstraction, build our own rendering + platform layer on top of it, and keep
> a neutral binding provider (LWJGL/JNI) underneath.

### 3.2 Guiding principles (non-negotiables)

1. **The web target constrains everything.** Web (TeaVM) can only use WebGL or WebGPU, never
   Vulkan/Metal/D3D. The abstraction we choose *must* degrade to the GL/WebGPU family on web (or
   we explicitly defer web). This eliminates most "raw Vulkan/Metal/DX" plans.
2. **Do not hand-write three native backends.** Separate raw Vulkan, Metal, and D3D backends are
   a job for a funded team, not a framework. Prefer an existing cross-platform GPU abstraction.
3. **Migrate incrementally behind a seam.** Wrap libGDX behind our own interfaces first, then
   swap the implementation. No big-bang rewrite where nothing compiles for months.
4. **Optimize for the cleanest final API, not backward compatibility.** No one depends on the
   framework yet, so breaking changes are fine. Prefer the right long-term design over preserving
   current signatures. The only reason to avoid gratuitous churn is to keep the migration itself
   testable one subsystem at a time, not to protect existing callers.
5. **Honor the framework's existing rules.** Field/modifier ordering, no per-frame allocations,
   uniform Java 17 baseline across all modules (see 5.1), beginner-friendly Javadoc, ASCII prose.
6. **Every replacement must be documented better than the thing it replaces.**

## 4. Target architecture (sketch)

A layered design so the swap happens in one place. Names are provisional.

```
Game code (FlixelSprite, FlixelState, ...)   <- ideally unchanged
        |
FlixelGDX public API (core)
        |
+-------------------------------------------+
|  FlixelGDX abstraction seam (interfaces)  |
|  - FlixelGraphicsManager (Flixel.graphics)|
|  - FlixelWindow / app lifecycle           |
|  - FlixelInput                            |
|  - FlixelFiles                            |
|  - FlixelAudio (already ~miniaudio)       |
|  - FlixelMath (FlixelPoint, matrices...)  |
|  - FlixelCollections (FlixelArray, ...)   |
|  - FlixelAssets                           |
+-------------------------------------------+
        |                         |
  [libGDX impl]            [new impl: bgfx (native) / WebGPU (web)]
  (transitional)           (the destination)
```

Strategy: build the seam, wrap libGDX behind it first (nothing breaks), then stand up the new
implementation behind the same seam and switch over per subsystem.

### 4.1 Graphics API structure (decided)

**One public surface, one internal seam.** Game code touches only `Flixel.graphics`; the
GPU-library backend is strictly internal and never exposed (no `Flixel.opengl` / `Flixel.vulkan`,
and no public device/RHI tier - that would be over-abstraction for a 2D framework).

```
Flixel.graphics  ->  FlixelGraphicsManager     the ONLY public surface: cameras, the sprite batch,
                                              blend modes, render targets, draw API. Backend-
                                              agnostic; ~all game code lives here.
                         |
                    FlixelGraphicsBackend      INTERNAL interface (textures, buffers, pipelines,
                         |                      draw submission) - the bgfx/WebGPU swap point.
                                                Never referenced by game code.
                    BgfxBackend (native) / WebGpuBackend (web)   the real backend, selected at
                                              startup.
```

- **`FlixelGraphicsManager`** (at `Flixel.graphics`) owns the active backend, the sprite batch, and
  cameras, and exposes the drawing API. It is the single public entry point.
- **`FlixelGraphicsBackend`** is *internal*; `BgfxBackend` / `WebGpuBackend` implement it. It is the
  seam that keeps bgfx and WebGPU interchangeable - the manager talks to it, never to a GPU library
  directly. Keeping it internal (not a public tier) avoids over-abstraction while preserving the
  swap point.
- **Texture source of truth:** `FlixelGraphic` (the existing ref-counted texture-resource handle)
  stays the cross-platform representation of a loaded image; post-migration it holds an opaque
  backend texture instead of a libGDX `Texture`. `FlixelFrame` and `FlixelSpriteBatch` sit on top.
- **Introspection + escape hatch:** `Flixel.graphics.backendType()` returns an enum for "what was
  injected at startup"; a documented, explicitly-unsafe native-handle accessor covers the rare
  power user. No per-backend method surfaces (a no-op-if-not-selected API would be a silent-failure
  footgun).
- **Naming:** there is deliberately no public class named `FlixelGraphics` - that avoids a collision
  with `FlixelGraphic` (one letter apart, opposite meaning: the renderer vs. a single texture).
- **Why this shape:** keeps libGDX-style coupling out of the public API (backend stays swappable
  behind our own interface) with one simple public surface. See decision 5.2.

## 5. Strategic decisions (open questions)

The forks in the road. Each has a lean, but the final call is the maintainer's. Settled ones
move to the [Decision log](#6-decision-log).

### 5.1 Java baseline (resolved: uniform Java 17 across all modules)

**Decision:** every module targets **Java 17** - one baseline, for consistency across the whole
framework. No per-module version bump, and Project Panama is not used (bgfx binds via LWJGL/JNI,
which works fine on 17).

The Foreign Function & Memory API (Project Panama) is finalized in **Java 22**, but FFM is a
**JVM-only** feature - it does not exist on TeaVM, Android, or MobiVM. Verified toolchain ceilings
(checked 2026-08 against our pinned versions):

| Target | Toolchain (pinned) | Max Java level accepted | Panama/FFM |
|---|---|---|---|
| Web | TeaVM 0.13.0 | up to **Java 25** bytecode - not a constraint | no (no native on web) |
| Android | AGP 8.7.3 | **Java 17** language level (API 34); Java 21 features unsupported | no |
| iOS | MobiVM 2.3.22 | **~Java 8** class library; modern invokedynamic-based features fragile | no |
| Desktop | HotSpot JVM | anything (**22+** fine) | yes |

- **Android is the binding constraint at Java 17**; TeaVM far exceeds it; MobiVM is the lowest but
  iOS is already a deferred target. Java 17 is the highest level all current-gen targets accept, so
  it becomes the single uniform baseline.
- Because we chose **bgfx** on native (LWJGL/JNI) rather than WebGPU-native, Panama never enters the
  picture and no module needs Java 22. Uniform 17 stands.
- **Standalone liability:** MobiVM's ~Java 8 ceiling is restrictive and fragile; it is an
  independent reason the iOS path may be reworked rather than kept on MobiVM. It must not drag the
  baseline below 17.

### 5.2 GPU abstraction - keystone (resolved)

**Decided:** own a single backend-agnostic abstraction (`FlixelGraphicsDevice`, see 4.1) and back
it with two internal backends:

- **Native (desktop, Android, iOS): bgfx**, bound via LWJGL. bgfx is the more proven, reliable
  option, already LWJGL-bound (so it works on Java 17 with no Panama), and its own shader toolchain
  cross-compiles one shader source to every native backend (Vulkan/Metal/D3D/GLES).
- **Web: WebGPU**, through our own **TeaVM JS-interop bindings**. bgfx's web path goes through
  emscripten/WASM, which clashes with TeaVM; since TeaVM already binds Java to JS/WASM, calling the
  browser's native WebGPU directly is the natural fit.

We do NOT hand-write raw Vulkan/Metal/D3D backends, and we do NOT expose per-backend public APIs.
Raw backends remain an *optional future* implementation of the same interface if a real need ever
arises. **Rejected:** SDL3 GPU (no native web-GPU path) and raw multi-backend (scope multiplies
into three-plus native codebases plus a shader toolchain).

**Consequence to plan for:** two backends means two shader dialects - bgfx's shader language
(compiled once for all native platforms via `shaderc`) and **WGSL** for the web/WebGPU path. Each
effect is authored for both, or authored once and cross-compiled. Manageable, but a real cost to
track (see Section 15). The Phase 0 spike (Section 8) now just *validates* each path rather than
choosing between them.

### 5.3 Windowing / input / audio (resolved)

**Decision:** **SDL3** for windowing + input + gamepad, **miniaudio** for audio.

- **SDL3** is bound via **LWJGL** (`org.lwjgl.sdl`, LWJGL 3.4.x) - the same JNI mechanism as bgfx,
  so it works on **Java 17** with no Panama. SDL3 owns the window and input; we hand its native
  window handle to bgfx for rendering. SDL is ~25 years old, Valve-backed, and stable (3.2.0, Jan
  2025) - a robust, lasting platform layer, with stronger gamepad/touch handling than GLFW and, on
  native, broader platform reach.
- **Audio stays on miniaudio, deliberately.** SDL3's core audio is low-level plumbing
  (`SDL_AudioStream`: device I/O, conversion, resampling, basic mixing) that only decodes WAV; MP3/
  OGG/FLAC and effects need the separate `SDL3_mixer`. That is the OpenAL-tier level we already left
  behind. miniaudio is a full engine (decoding, mixing, effects, node graph, optional 3D) in one
  file and already works across desktop and mobile. So we cherry-pick SDL3's strengths, not its
  audio.
- **Scope note:** LWJGL is desktop-only, so "SDL3 via LWJGL" is the *desktop* platform layer.
  Android uses SDL3 through its own native integration (JNI/NDK); web uses the browser (canvas + DOM
  events) via TeaVM, not SDL.

### 5.6 Resolved platform stack (summary)

Everything targets **Java 17**; core uses FlixelGDX's own utilities (no libGDX). iOS is deferred.

| Concern | Desktop (`-desktop`) | Android | Web (`-web`) | iOS (deferred) |
|---|---|---|---|---|
| Rendering | bgfx | bgfx | WebGPU (own TeaVM bindings) + WebGL fallback | bgfx (Metal) |
| Window / input / gamepad | SDL3 | SDL3 | browser (canvas + DOM) | SDL3 / UIKit |
| Audio | miniaudio | miniaudio | Web Audio API (TeaVM) | miniaudio |
| Bindings / toolchain | LWJGL / JNI | JNI / NDK | TeaVM (Java to JS/WASM) | MobiVM (~Java 8 caveat) |
| Shaders | bgfx shaderc | bgfx shaderc | WGSL | bgfx shaderc |

### 5.4 Web strategy (resolved: WebGPU via our own TeaVM bindings)

**Decision:** the `flixelgdx-web` module (renamed from `-teavm`) implements `FlixelGraphicsDevice`
by calling the browser's native **WebGPU** through **our own TeaVM JS-interop bindings**. This is
the natural fit - TeaVM already bridges Java to JS/WASM, so we bind `navigator.gpu` directly rather
than fighting bgfx's emscripten path.
- **WebGL fallback: yes.** We ship a **WebGL fallback** for browsers/devices without WebGPU so web
  reach stays wide. Built in Phase 5, behind the same `FlixelGraphicsDevice` web backend.
- Web may still temporarily regress or lag desktop during the migration; that is acceptable.

### 5.5 Branching / release strategy (resolved)

**Decision:** **each phase gets its own branch.** A phase is developed on its own branch (off the
previous completed phase); if it breaks or goes wrong, that branch can be discarded and restarted
without losing earlier phases. Completed phases merge forward in order and ultimately land as the
`1.0` line. No parallel `0.x` libGDX release line is maintained (no users, breaking changes fine).

## 6. Decision log

| Date | Decision | Rationale |
|---|---|---|
| 2026-08-03 | Start with Phase 1 (utilities) ahead of the GPU decision | Utilities are the neutral, non-OpenGL layer; owning them now shrinks coupling and does not depend on 5.2. |
| 2026-08-03 | Utility naming: HaxeFlixel-idiomatic (`FlixelPoint`, `FlixelRect`, `FlixelArray`, `FlixelMap`, `FlixelPool`, ...) | Matches HaxeFlixel and the existing `FlixelColor`/`FlixelString` precedent; presents a clean owned surface instead of leaking libGDX collection names. |
| 2026-08-03 | **Clean-room reimplement** the utilities (do not copy libGDX source); bake in improvements (see 9.2) | Algorithms/designs are not copyrightable; writing our own code avoids the Apache `NOTICE` obligation (courtesy credit only) and lets us add ergonomics libGDX lacks. |
| 2026-08-03 | Utility packages: `org.flixelgdx.math` (math value types + `FlixelMathUtil` + `FlixelRandom`) and `org.flixelgdx.collections` (collections + pooling) | Clear, discoverable layout; trivial helpers stay in `org.flixelgdx.util`. |
| 2026-08-03 | `FlixelRandom` is instance-based with a default exposed at `Flixel.random`; `FlixelArray` gets a built-in snapshot mode (no separate `FlixelSnapshotArray`) | Familiar global for veterans (like HaxeFlixel `FlxG.random`) plus seedable determinism; one array type instead of two. |
| 2026-08-03 | Breaking changes are acceptable; no backward-compatibility constraint | No one depends on the framework yet. Optimize for the cleanest final design. |
| 2026-08-03 | Execute the migration as a focused ~1-2 week sprint via Opus + Fable, testing subsystems one by one | Small dedicated sprint keeps momentum and lets each converted subsystem be verified in isolation. |
| 2026-08-03 | Graphics: one PUBLIC surface `FlixelGraphicsManager` at `Flixel.graphics`; the bgfx/WebGPU backend sits behind an INTERNAL `FlixelGraphicsBackend` seam (no public device/RHI tier, no per-backend APIs; `backendType()` enum + unsafe native-handle escape hatch) | Simpler for a 2D framework and avoids the `FlixelGraphic`/`FlixelGraphics` name collision, while the internal seam keeps bgfx/WebGPU swappable (see 4.1). |
| 2026-08-03 | `FlixelGraphic` stays the cross-platform texture-resource handle (holds an opaque backend texture post-migration) | One source of truth for a loaded image across all platforms. |
| 2026-08-03 | JSON: own a reflection-free layer - `FlixelJsonObject`/`FlixelJsonArray` DOM in `org.flixelgdx.json`, a `Flixel.json` (`FlixelJson`) facade, and **annotation-processor codegen** for automatic object mapping | Reflection-free removes TeaVM reflection-config pain; codegen gives the best DX (automatic + reflection-free); prefixed names + dedicated package avoid collisions. Lands as a later slice, not Phase 1. |
| 2026-08-03 | Back the graphics device with existing cross-platform libs, not hand-written raw backends; raw stays an optional future impl of the same interface | The libs absorb the multi-codebase renderer + shader-toolchain burden and do not lock us in behind our own seam (WebGPU is a W3C standard). Native-vs-web split resolved (next rows). |
| 2026-08-03 | GPU backends: **bgfx** for native (desktop/Android/iOS), **WebGPU via our own TeaVM bindings** for web | bgfx is proven and LWJGL-bound (Java 17, no Panama); bgfx's emscripten web path clashes with TeaVM, so browser-native WebGPU is the natural web fit. |
| 2026-08-03 | Web strategy: WebGPU through our own TeaVM JS-interop bindings (`flixelgdx-web`), **with a WebGL fallback** for browsers without WebGPU | TeaVM already bridges Java to JS/WASM, so we bind `navigator.gpu` directly; WebGL fallback keeps web reach wide. |
| 2026-08-03 | Java baseline: **uniform Java 17 across all modules** (no per-module bump; Panama unused) | Android/AGP 8.7 caps at 17 and FFM is JVM-only anyway; bgfx binds via LWJGL/JNI on 17. One baseline keeps the framework consistent. Verified: TeaVM 0.13 up to Java 25, MobiVM ~Java 8. |
| 2026-08-03 | Rename modules: `flixelgdx-lwjgl3` -> `flixelgdx-desktop`, `flixelgdx-teavm` -> `flixelgdx-web` | Names should say what the module is for without digging through docs. Lands during the backend rework (Phases 4-5). |
| 2026-08-03 | Windowing/input/gamepad = **SDL3** (via LWJGL, Java 17/JNI); audio stays **miniaudio** | SDL3 is a robust, lasting platform layer already bound by LWJGL; its core audio is WAV-only low-level plumbing (OpenAL-tier), so miniaudio's full engine is kept. |
| 2026-08-03 | Branching: **each phase gets its own branch**, merged forward in order; a broken phase can be discarded and restarted | Keeps completed phases safe; no parallel `0.x` line kept. |

---

# Part II - The plan

> Each phase is sequential in intent but can overlap. **Every phase must end in a buildable,
> testable state.** Each phase is developed on **its own branch** (5.5), branched from the previous
> completed phase, so a broken phase can be discarded and restarted without losing earlier work.
> Check the boxes as steps land.

## 7. Roadmap at a glance

| Phase | Goal | Gate before it can start |
|---|---|---|
| 0 | Spikes + settle strategic decisions | none |
| 1 | Own the utilities (collections, math, pooling) | none (breaking changes are OK) |
| 2 | Introduce the abstraction seam over libGDX | Phase 1 mostly done |
| 3 | Stand up the bgfx backend on desktop | Phase 2 |
| 4 | Remove libGDX from desktop | Phase 3 at parity |
| 5 | Bring other platforms onto the new backend | Phase 4 |
| 6 | Cleanup, docs, ship 1.0 | Phase 5 |

## 8. Phase 0 - Spikes and strategic decisions

*No production code. Prototypes live in a throwaway sandbox, not the framework.*

- [ ] **0a.** Spike a triangle + a textured quad on **bgfx** (via LWJGL) on desktop.
- [ ] **0b.** Spike a textured quad on **WebGPU via TeaVM** in a browser - validate that the
  JS-interop binding path actually draws a sprite.
- [ ] **0c.** Rough-benchmark the bgfx spike against the current gdx `SpriteBatch`.
- [ ] **0d.** Settle the remaining decisions (5.3, 5.5) and record them in the
  [Decision log](#6-decision-log).

> These are now *validation* spikes (the backends are chosen), not a bake-off.

> Phase 0 can run in parallel with Phase 1, since Phase 1 does not depend on the GPU choice.

## 9. Phase 1 - Own the utilities (the neutral layer)

**Why first:** the utilities are OpenGL-independent, so we can own them now, and doing so shrinks
the coupling before we touch anything hard.

**Framing:** these utilities leak into FlixelGDX's *public API*, so replacing them is a **breaking
change** rather than a silent internal swap. That is acceptable (no users yet), so we optimize for
the cleanest surface. Examples in core today:

- `FlixelGroup.getMembers()` / `FlixelSpriteGroup.getMembers()` return `SnapshotArray<T>`.
- `Flixel.cameras` / `FlixelGame.getCameras()` are `Array<FlixelCamera>`.
- `FlixelCamera.deadzone` is a `public Rectangle`; `Flixel.getSize()` returns `Vector2`.
- `FlixelSave.data` is a `public final ObjectMap<String, Object>`.
- `FlixelTween` and `FlixelTimer` `implements Pool.Poolable`.

Breaking game code is fine here (no users yet), so we take the chance to improve the surface: today
we leak libGDX's collection API (callers must learn `SnapshotArray.begin()/end()`); owning these
types gives a clean, documented, HaxeFlixel-idiomatic surface.

### 9.1 Naming map and packages (decided: HaxeFlixel-idiomatic)

**Packages (decided):** math value types go in a new **`org.flixelgdx.math`**; collections and
pooling go in a new **`org.flixelgdx.collections`**. The existing `FlixelMathUtil` (today in
`org.flixelgdx.util`) moves into `org.flixelgdx.math`. The trivial helpers (`FlixelAlign`,
`FlixelArraySupplier`, `FlixelDestroyable`) stay under `org.flixelgdx.util`.

| libGDX type | FlixelGDX type | Package | Notes |
|---|---|---|---|
| `math.Vector2` | `FlixelPoint` | `math` | Mirrors HaxeFlixel `FlxPoint`; poolable (see 9.2). |
| `math.Rectangle` | `FlixelRect` | `math` | Mirrors HaxeFlixel `FlxRect`; poolable. |
| `math.MathUtils` | `FlixelMathUtil` (moved + extended) | `math` | Absorb sin/cos, clamp, lerp, etc., plus game helpers (9.2). Randomness split out into `FlixelRandom`. |
| (new) | `FlixelRandom` | `math` | **Instance-based**, seedable RNG. A default instance is exposed at **`Flixel.random`** so veterans get a familiar global (like HaxeFlixel `FlxG.random`). |
| `utils.Array` | `FlixelArray<T>` | `collections` | Keeps `.items`/`.size` for zero-alloc iteration; **built-in snapshot mode** (9.2), so no separate snapshot class. |
| `utils.SnapshotArray` | (folded into `FlixelArray`) | `collections` | Replaced by `FlixelArray`'s snapshot mode; no standalone type. |
| `utils.ObjectMap` | `FlixelMap` | `collections` | |
| `utils.ObjectSet` | `FlixelSet` | `collections` | |
| `utils.IntArray` / `FloatArray` / `CharArray` | `FlixelIntArray` / `FlixelFloatArray` / `FlixelCharArray` | `collections` | Primitive, no-boxing. |
| `utils.IntMap` / `IntSet` / `IdentityMap` | `FlixelIntMap` / `FlixelIntSet` / `FlixelIdentityMap` | `collections` | |
| `utils.Pool` / `Pool.Poolable` | `FlixelPool` / `FlixelPoolable` | `collections` | `Poolable` is implemented by public classes; coordinate the `implements` change. |
| `utils.Disposable` | consolidate into existing `FlixelDestroyable` | `util` | Verify the concepts match before adding a new type. |
| `utils.Align` | `FlixelAlign` constants | `util` | Trivial bit constants. |
| `utils.ArraySupplier` | `FlixelArraySupplier` | `util` | Trivial functional interface. |

**Deferred out of Phase 1:**
- *Render/native-coupled (die with libGDX, not before):* `Affine2`, `Matrix4`, `BufferUtils`,
  `ScreenUtils`.
- *Own later as its own slice:* `Json`/`JsonReader`/`JsonValue` -> a reflection-free JSON layer
  (`FlixelJsonObject`/`FlixelJsonArray` DOM in `org.flixelgdx.json`, a `Flixel.json` /
  `FlixelJson` facade, and annotation-processor codegen for automatic mapping). `XmlReader`
  similarly deferred.
- *g2d that leaks but is not a utility:* e.g. `Animation<FlixelFrame>` - tracked with the render
  migration.

### 9.2 Improvements over libGDX (what our versions add)

We are reimplementing, not copying, so we bake in ergonomics and safety libGDX lacks.

**Cross-cutting (all types):** beginner-friendly Javadoc with examples; `@Nullable`/`@NotNull`
throughout; zero-alloc iteration as the default; optional dev-mode safety (bounds checks, misuse
asserts) that compiles out in release; consistent HaxeFlixel-style naming.

**Per type:**
- **`FlixelPoint` / `FlixelRect`** - poolable via `FlixelPoint.get(...)` / `.put()` plus a `weak()`
  auto-recycling variant (kills per-frame vector allocations). Game helpers: `distanceTo`,
  `angleTo`, `rotate`, `copyFrom`; union/intersection for rects.
- **`FlixelRandom`** - instance-based, seedable (deterministic replays / seeded procgen), default
  instance at `Flixel.random`. Helpers: `int(min,max)`, `float(min,max)`, `bool(chance)`, `sign()`,
  `pick(array)`, `weightedPick(...)`, `shuffle(...)`, `color()`.
- **`FlixelMathUtil`** - game math helpers libGDX lacks: `approach`, `wrap`, `remap`, `lerpAngle`,
  `snap`, alongside the standard clamp/lerp/sin-cos.
- **`FlixelPool`** - leak detection + stats (outstanding/peak counts) in dev mode; optional
  auto-`reset()` on free; `freeAll`.
- **`FlixelArray`** - built-in snapshot mode (safe add/remove mid-iteration) instead of a separate
  class; zero-alloc indexed `forEach`; `getRandom`, `first`/`last`, `swapRemove`; GC-friendly
  `clear()`.
- **`FlixelMap` / `FlixelSet`** - zero-alloc entry iteration; `getOrDefault`; optional
  insertion-order preservation (so no separate ordered variant is needed).
- **`FlixelDestroyable`** - idempotent `destroy()`, an `isDestroyed()` flag, dev-mode tracking of
  never-destroyed objects.

### 9.3 Methodology - how to do it correctly

We **clean-room reimplement** every utility. We may study how libGDX/HaxeFlixel solved a problem -
algorithms and API designs are not copyrightable - but we write our own code rather than copying
theirs. That means **no Apache `NOTICE` obligation**; a courtesy credit to libGDX/HaxeFlixel in our
docs is enough.

- **Reimplement from the algorithm**, then add the 9.2 improvements. For the tricky ones
  (`FlixelArray` growth, `FlixelMap` open-addressing/hashing, the `FlixelMathUtil` sin/cos table),
  implement from the well-known technique - do not transcribe libGDX line by line.
- **Defer**: everything in the deferred list above.

Correctness discipline (non-negotiable):
1. **Match proven semantics via differential tests** - especially snapshot-mode iteration, `Array`
   ordered-vs-unordered removal, and `FlixelRandom` determinism. Add tests in `flixelgdx-test` that
   run our type and the libGDX type side by side and assert identical behavior during the
   transition (test *behavior*, not copied code).
2. **Keep the `.items`/`.size` public-field pattern** on arrays - it enables zero-alloc indexed
   iteration, which is exactly our no-per-frame-allocation rule.
3. **Migrate file-by-file behind a one-way seam** - introduce the Flixel type, swap references
   per file, drop the gdx import, keep the build green throughout.
4. **Finish each slice properly** - run `flixelgdx-test`, `spotless apply`, and Javadoc lint
   before moving on.

### 9.4 Steps

- [x] **1a - Groundwork.** Create the `org.flixelgdx.math` and `org.flixelgdx.collections` packages;
  move the existing `FlixelMathUtil` from `org.flixelgdx.util` into `org.flixelgdx.math`; add a
  courtesy credit to libGDX/HaxeFlixel in the docs (clean-room, so no `NOTICE` needed).
- [x] **1b - Tier 1 (trivial, render-neutral).** `FlixelPool`/`FlixelPoolable` (with dev-mode leak
  stats), `FlixelAlign`, `FlixelArraySupplier`. (`Disposable` -> `FlixelDestroyable` consolidation
  is deferred to the 1e sweep, where the callers are actually swapped.)
- [x] **1c - Tier 2 (math).** `FlixelMathUtil` (+ game helpers), `FlixelPoint` / `FlixelRect`
  (poolable), and `FlixelRandom` (instance-based, wired to `Flixel.random`).
- [x] **1d - Tier 3 (collections).** The big ones first - `FlixelArray` (with snapshot mode),
  `FlixelMap`, `FlixelSet` - then the specialized ones (`FlixelIntArray`, `FlixelFloatArray`,
  `FlixelCharArray`, `FlixelIntMap`, `FlixelIntSet`, `FlixelIdentityMap`).
- [x] **1d.1 - Collection code generation.** Replaced the hand-duplicated primitive collections
  with a template-driven generator (`scripts/generate_primitive_collections.py` plus templates
  under `scripts/templates`). One template now drives every primitive array, primitive-keyed map,
  primitive-keyed set, and object-keyed primitive-value map, along with their unit tests. The
  generated Java files are still committed so IDEs and Javadoc treat them as normal source. This
  sweep also dropped the unused prim-to-prim maps (`FlixelIntIntMap`, `FlixelIntFloatMap`,
  `FlixelBoolMap`, `FlixelCharMap`) and added `FlixelLongSet`.
- [ ] **1e - Sweep.** Confirm no core file imports `com.badlogic.gdx.utils.*` or
  `com.badlogic.gdx.math.{Vector2,Rectangle,MathUtils}` anymore; update Markdown docs.

> **Progress note (build vs. sweep).** Slices 1a-1d - building and fully unit-testing every
> replacement type - are complete on the `phase-1-utilities` branch (all new types have tests,
> including differential tests vs. `java.util` for the hash collections and vs. proven snapshot
> semantics for `FlixelArray`). The primitive collections have also been consolidated behind a
> template-driven generator (1d.1), so the whole family is authored from one template per shape
> instead of hand-copied files. What remains is 1e: the file-by-file sweep that swaps ~56 core
> files off the gdx utility imports and onto the new types. That step is intentionally separate
> because it is where the *breaking public-API changes* land (for example `FlixelGroup.getMembers()`
> switching from `SnapshotArray<T>` to `FlixelArray<T>`).
>
> **Deferred/adjusted during 1a-1d (revisit in 1e or later):**
> - `FlixelRandom.color()` is not implemented yet: `FlixelColor` still wraps a libGDX `Color`, so
>   adding it now would pull gdx back into the clean `math` package. Add it once color is decoupled.
> - RNG helpers use Java-idiomatic names (`nextInt`, `nextFloat`, `nextBool`) rather than the
>   literal `int()` / `float()` from 9.2, since those are reserved words in Java.
> - `FlixelMap` insertion-order preservation (the "optional ordered mode" from 9.2) is not built
>   yet; the current map is unordered. Add it if/when an ordered use case appears.

## 10. Phase 2 - Introduce the abstraction seam

*Define our own interfaces for the OpenGL-coupled layer and route core through them, still backed
by libGDX. After this phase, core no longer imports `com.badlogic.gdx` directly - only the
transitional libGDX backend does.*

- [ ] **2a.** Design the seam interfaces: renderer/graphics, window/app lifecycle, input, files,
  assets (see [Section 4](#4-target-architecture-sketch)).
- [ ] **2b.** Implement each interface with a **transitional libGDX backend** that simply delegates
  to `Gdx.*`, so behavior is unchanged.
- [ ] **2c.** Route all core code through the seam; remove direct `com.badlogic.gdx` imports from
  core.
- [ ] **2d.** Green build + full test pass on the libGDX-backed seam (proves the seam is faithful).

## 11. Phase 3 - Stand up the new backend (desktop first)

*Implement `FlixelGraphicsDevice` (4.1) with the **bgfx** backend (5.2) for desktop, wired up
through `FlixelGraphicsManager`.*

- [ ] **3a.** Window + input + main loop on the new stack (GLFW/SDL3 per 5.3).
- [ ] **3b.** Implement the bgfx `FlixelGraphicsDevice` backend: texture upload + a 2D sprite
  batcher; reach visual parity with the gdx `SpriteBatch`.
- [ ] **3c.** Render targets / framebuffers, shaders, blend modes, scissor/clipping.
- [ ] **3d.** Font rendering (replace FreeType path as needed).
- [ ] **3e.** Benchmark against the libGDX backend; fix regressions.
- [ ] **3f.** Make the new backend the desktop default behind a flag, then unconditionally.

## 12. Phase 4 - Remove libGDX from desktop

- [ ] **4a.** Delete the transitional libGDX backend for desktop.
- [ ] **4b.** Rename `flixelgdx-lwjgl3` -> `flixelgdx-desktop` and update it to the bgfx stack;
  drop gdx desktop deps.
- [ ] **4c.** Full desktop test + example pass with zero libGDX on the classpath.

## 13. Phase 5 - Other platforms

*Hardest targets go last.*

- [ ] **5a.** Android onto the **bgfx** backend (Vulkan/GLES; NDK/native packaging).
- [ ] **5b.** Rename `flixelgdx-teavm` -> `flixelgdx-web` and implement **WebGPU via our own TeaVM
  bindings** (5.4); decide the WebGL fallback; accept temporary regression if needed.
- [ ] **5c.** iOS onto **bgfx** (Metal) - potentially more viable than the current MobiVM+GLES path
  (mind the MobiVM ~Java 8 ceiling, 5.1).

## 14. Phase 6 - Cleanup and docs

- [ ] **6a.** Remove all remaining gdx dependencies across every module.
- [ ] **6b.** Update PROJECT.md, README, and all Markdown docs.

---

# Part III - Appendices

## 15. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Scope is enormous; migration stalls half-done | Strict phasing; every phase ends buildable; libGDX stays a working fallback until each subsystem reaches parity. |
| Web target has no modern-GPU path | Decide web strategy explicitly (5.4); accept temporary regression rather than blocking desktop progress. |
| Reimplementing well-tested libGDX code introduces bugs | Clean-room from known algorithms; differential tests in `flixelgdx-test` assert identical behavior vs. the libGDX type during the transition. |
| Chosen GPU library becomes unmaintained | bgfx has broad adoption; WebGPU is a vendor-backed standard; the seam keeps either backend swappable. |
| Two shader dialects (bgfx `shaderc` for native, WGSL for web) drift or double the authoring work | Keep the shader set small; author shared effects deliberately; cross-compile from one source where practical; track native/web parity in tests. |
| Performance regression vs. libGDX's mature batcher | Benchmark the bgfx batcher before switching defaults. |
| API churn during migration | No external users yet, so churn is acceptable; confine it beneath the seam and land it on the `no-gdx` branch as the `1.0` line. |

## 16. Per-platform notes

- **Desktop (`flixelgdx-desktop`, was `-lwjgl3`):** lowest risk, done first. bgfx + SDL3 (via
  LWJGL) + miniaudio, on Java 17.
- **Android:** bgfx + SDL3 + miniaudio via JNI/NDK; native packaging to sort out.
- **Web (`flixelgdx-web`, was `-teavm`):** hardest. WebGPU + browser input + Web Audio, all via our
  own TeaVM JS-interop bindings; WebGL fallback TBD. Last and most experimental; may temporarily
  regress.
- **iOS:** currently unsupported anyway; bgfx (Metal) may make iOS *more* viable than the current
  MobiVM+GLES path. Mind the MobiVM ~Java 8 ceiling (5.1).

## 17. Status checklist

- [x] Decision to migrate off libGDX made (2026-08-03).
- [x] This planning document created.
- [x] Coupling inventory taken (Section 2) and utility inventory detailed (Section 9).
- [x] Utility naming convention decided (HaxeFlixel-idiomatic).
- [x] Breaking changes accepted; migration to run as a focused ~1-2 week Opus + Fable sprint.
- [ ] Coupling inventory reviewed and agreed by maintainer.
- [x] Branching strategy (5.5) decided: each phase gets its own branch.
- [x] Utilities are clean-room reimplemented (no copying); courtesy credit to libGDX/HaxeFlixel.
- [x] Graphics architecture decided: two-tier backend-agnostic device (4.1).
- [x] GPU backends decided: bgfx (native) + WebGPU via TeaVM (web) (5.2, 5.4).
- [x] Java baseline resolved: uniform Java 17 across all modules (5.1).
- [x] Module rename decided: `-lwjgl3` -> `-desktop`, `-teavm` -> `-web`.
- [x] Windowing/input/audio decided: SDL3 (platform) + miniaudio (audio) (5.3).
- [x] Web strategy finalized: WebGPU via TeaVM + WebGL fallback (5.4).
- [x] Branching decided: each phase on its own branch (5.5).
- [x] **All Part I strategic decisions resolved.**
- [ ] Phase 0 validation spikes run (bgfx desktop, WebGPU-via-TeaVM web).
- [x] Phase 1 started; slices 1a-1d (build + test all replacement utilities) complete on
  `phase-1-utilities`, and the primitive collections are now generated from templates (1d.1).
  Slice 1e (file-by-file sweep off gdx utils) still pending.
