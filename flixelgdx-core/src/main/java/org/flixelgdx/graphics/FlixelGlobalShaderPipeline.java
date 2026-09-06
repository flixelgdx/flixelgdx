/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.flixelgdx.graphics;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelShader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manages the global post-processing shader chain applied to all game cameras as a group.
 *
 * <p>This object holds the ordered list of shaders and the ping-pong render targets they write
 * through. It is accessed via {@link FlixelGraphicsManager} rather than directly, so game code
 * works through {@code Flixel.graphics.addGlobalShader(shader)} and related methods.
 *
 * <p>Multiple shaders chain automatically: each shader reads from one target and writes to the
 * other so every pass feeds the next without re-rendering the scene. The global overlay is always
 * drawn after the chain and is never affected by it.
 *
 * @see FlixelGraphicsManager#addGlobalShader(FlixelShader)
 * @see FlixelGraphicsManager#removeGlobalShader(FlixelShader)
 */
public class FlixelGlobalShaderPipeline {

  /**
   * No-op implementation returned by {@link FlixelGraphicsManager#getGlobalShaderPipeline()} on
   * backends that do not support post-processing. All methods do nothing and
   * {@link #hasShaders()} always returns {@code false}.
   */
  public static final FlixelGlobalShaderPipeline NOOP = new FlixelGlobalShaderPipeline() {
    @Override
    public void add(@NotNull FlixelGraphicsManager graphics, @NotNull FlixelShader shader) {}

    @Override
    public boolean remove(@NotNull FlixelGraphicsManager graphics, @NotNull FlixelShader shader) {
      return false;
    }

    @Override
    public boolean hasShaders() {
      return false;
    }

    @Override
    public void resize(@NotNull FlixelGraphicsManager graphics) {}

    @Override
    public void beginCapture(@NotNull FlixelGraphicsManager graphics) {}

    @Override
    public void endCapture() {}

    @Override
    public void apply(@NotNull FlixelBatch batch, @NotNull FlixelGraphicsManager graphics) {}

    @Override
    public void dispose() {}
  };

  private final FlixelArray<FlixelShader> shaders = new FlixelArray<>();
  private final FlixelMatrix ortho = new FlixelMatrix();

  @Nullable
  private FlixelRenderTarget fboA;
  @Nullable
  private FlixelRenderTarget fboB;

  private int orthoW = -1;
  private int orthoH = -1;

  /** Creates a new, initially empty shader pipeline with no render targets allocated. */
  public FlixelGlobalShaderPipeline() {}

  /**
   * Appends a shader to the chain, initializing render targets if this is the first addition.
   * Adding the same instance more than once is a no-op.
   *
   * @param graphics The active graphics manager, used to create render targets.
   * @param shader The shader to append.
   */
  public void add(@NotNull FlixelGraphicsManager graphics, @NotNull FlixelShader shader) {
    if (shaders.contains(shader, true)) {
      return;
    }
    boolean needsPingPong = !shaders.isEmpty();
    shaders.add(shader);
    initFbos(graphics, needsPingPong || shaders.getSize() > 1);
  }

  /**
   * Removes a shader from the chain, releasing render targets when the chain becomes empty.
   * Removing a shader that was never added is a no-op.
   *
   * @param graphics The active graphics manager, used to recreate render targets when needed.
   * @param shader The shader to remove.
   * @return {@code true} if the shader was found and removed.
   */
  public boolean remove(@NotNull FlixelGraphicsManager graphics, @NotNull FlixelShader shader) {
    boolean removed = shaders.removeValue(shader, true);
    if (removed) {
      if (shaders.isEmpty()) {
        disposeFbos();
      } else {
        initFbos(graphics, shaders.getSize() > 1);
      }
    }
    return removed;
  }

  /**
   * Returns {@code true} when at least one shader is registered and the primary render target is ready.
   *
   * @return {@code true} when the chain is active and can capture.
   */
  public boolean hasShaders() {
    return !shaders.isEmpty() && fboA != null;
  }

  /**
   * Recreates the render targets to match the current scene dimensions.
   * Call this whenever the window resizes and the chain is non-empty.
   *
   * @param graphics The active graphics manager.
   */
  public void resize(@NotNull FlixelGraphicsManager graphics) {
    if (!shaders.isEmpty()) {
      initFbos(graphics, shaders.getSize() > 1);
    }
  }

  /**
   * Begins capturing all camera draws into the primary render target.
   * Must be paired with {@link #endCapture()} before calling {@link #apply}.
   *
   * @param graphics The active graphics manager, used to clear the target.
   */
  public void beginCapture(@NotNull FlixelGraphicsManager graphics) {
    if (fboA == null) {
      return;
    }
    fboA.begin();
    graphics.clear(0f, 0f, 0f, 0f);
  }

  /** Ends scene capture, returning drawing to the window (or the enclosing render target). */
  public void endCapture() {
    if (fboA == null) {
      return;
    }
    fboA.end();
  }

  /**
   * Runs the captured scene through the shader chain and composites the final result to the
   * current draw surface. When more than one shader is present the passes ping-pong between the
   * two render targets so each reads from one and writes to the other.
   *
   * @param batch The batch to use for full-screen quad draws.
   * @param graphics The active graphics manager.
   */
  public void apply(@NotNull FlixelBatch batch, @NotNull FlixelGraphicsManager graphics) {
    if (fboA == null) {
      return;
    }
    int w = graphics.getRenderWidth();
    int h = graphics.getRenderHeight();
    boolean usingA = true;
    FlixelRenderTarget src = fboA;
    int n = shaders.getSize();

    for (int i = 0; i < n; i++) {
      FlixelShader gs = shaders.get(i);
      boolean isLast = (i == n - 1);

      if (w != orthoW || h != orthoH) {
        orthoW = w;
        orthoH = h;
        // Y-down composite ortho so the blit matches the batch's Y-down vertex layout and the
        // FBO draws upright. Match the active backend's depth range too; the [-1, 1]
        // default depth-clips the composite quad to black on Vulkan, Metal, and Direct3D.
        ortho.setToOrtho2DYDown(0, 0, w, h, graphics.isDepthZeroToOne());
      }
      batch.setProjection(ortho);
      batch.setShader(gs);

      if (!isLast) {
        FlixelRenderTarget dst = usingA ? fboB : fboA;
        if (dst != null) {
          dst.begin();
        }
        graphics.clear(0f, 0f, 0f, 0f);
        batch.begin();
        gs.applyUniforms();
        if (src != null) {
          drawFullTarget(batch, src, w, h);
        }
        batch.end();
        if (dst != null) {
          dst.end();
        }
        src = dst;
        usingA = !usingA;
      } else {
        batch.begin();
        gs.applyUniforms();
        if (src != null) {
          drawFullTarget(batch, src, w, h);
        }
        batch.end();
      }
    }
    batch.setShader(null);
  }

  /** Releases all render targets and clears the shader list. */
  public void dispose() {
    disposeFbos();
    shaders.clear();
    orthoW = -1;
    orthoH = -1;
  }

  private void initFbos(@NotNull FlixelGraphicsManager graphics, boolean needPingPong) {
    disposeFbos();
    // Size to the scene render resolution, which equals the back buffer unless a fixed render
    // resolution is active, so the shader chain matches whatever size the cameras draw at.
    int w = graphics.getRenderWidth();
    int h = graphics.getRenderHeight();
    fboA = graphics.createRenderTarget(w, h);
    if (needPingPong) {
      fboB = graphics.createRenderTarget(w, h);
    }
  }

  private void disposeFbos() {
    if (fboA != null) {
      fboA.destroy();
      fboA = null;
    }
    if (fboB != null) {
      fboB.destroy();
      fboB = null;
    }
  }

  private static void drawFullTarget(@NotNull FlixelBatch batch, @NotNull FlixelRenderTarget target,
      float width, float height) {
    if (target.isFlipped()) {
      batch.draw(target.getTexture(), 0, 0, width, height, 0f, 1f, 1f, 0f);
    } else {
      batch.draw(target.getTexture(), 0, 0, width, height);
    }
  }
}
