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
package org.flixelgdx;

import org.flixelgdx.graphics.FlixelBatch;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.math.FlixelAffine;
import org.flixelgdx.math.FlixelMatrix;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Checks where {@link FlixelSprite#draw(FlixelBatch)} places the graphic relative to the hitbox.
 *
 * <p>The offset is subtracted from the draw position, the same way HaxeFlixel does it, so a positive
 * offset moves the artwork left and up. This lives in {@code org.flixelgdx} so it can set the draw
 * camera directly through the package-private {@link Flixel#setDrawCamera(FlixelCamera)}.
 */
class FlixelSpriteOffsetTest {

  private static final float EPS = 1e-4f;

  private RecordingBatch batch;

  @BeforeEach
  void setUp() {
    FlixelCamera camera = new FlixelCamera(400, 300);
    camera.scrollX = 0f;
    camera.scrollY = 0f;
    Flixel.setDrawCamera(camera);
    batch = new RecordingBatch();
  }

  @AfterEach
  void tearDown() {
    Flixel.setDrawCamera(null);
  }

  @Test
  void zeroOffsetDrawsTheGraphicAtThePosition() {
    FlixelSprite sprite = sprite(100f, 50f);
    sprite.draw(batch);
    assertEquals(100f, batch.x, EPS);
    assertEquals(50f, batch.y, EPS);
  }

  @Test
  void positiveOffsetMovesTheGraphicLeftAndUp() {
    FlixelSprite sprite = sprite(100f, 50f);
    sprite.setOffset(3f, 8f);
    sprite.draw(batch);
    assertEquals(97f, batch.x, EPS);
    assertEquals(42f, batch.y, EPS);
  }

  @Test
  void centeringOffsetPutsTheHitboxInTheMiddleOfTheGraphic() {
    // A 16x16 graphic with a 10x10 hitbox is centered with an offset of (16 - 10) / 2 = 3.
    FlixelSprite sprite = sprite(100f, 50f);
    sprite.updateHitbox(10f, 10f);
    sprite.setOffset(3f, 3f);
    sprite.draw(batch);
    float graphicCenterX = batch.x + 8f;
    float graphicCenterY = batch.y + 8f;
    assertEquals(sprite.getX() + sprite.getWidth() / 2f, graphicCenterX, EPS);
    assertEquals(sprite.getY() + sprite.getHeight() / 2f, graphicCenterY, EPS);
  }

  private static FlixelSprite sprite(float x, float y) {
    FlixelSprite sprite = new FlixelSprite(x, y);
    sprite.setRegion(new FlixelFrame(new FlixelNoopTexture(16, 16)));
    return sprite;
  }

  /** Remembers where the last frame was drawn and ignores everything else. */
  private static final class RecordingBatch implements FlixelBatch {

    private final FlixelColor color = new FlixelColor();
    private final FlixelMatrix projection = new FlixelMatrix();
    private final FlixelMatrix transform = new FlixelMatrix();
    private float x = Float.NaN;
    private float y = Float.NaN;

    @Override
    public void begin() {}

    @Override
    public void end() {}

    @Override
    public void flush() {}

    @Override
    public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height) {}

    @Override
    public void draw(@NotNull FlixelTexture texture, float x, float y, float width, float height,
        float u, float v, float u2, float v2) {}

    @Override
    public void draw(@NotNull FlixelFrame frame, float x, float y, float width, float height) {
      this.x = x;
      this.y = y;
    }

    @Override
    public void draw(@NotNull FlixelFrame frame, float x, float y, float originX, float originY,
        float width, float height, float scaleX, float scaleY, float rotation,
        boolean flipX, boolean flipY) {
      this.x = x;
      this.y = y;
    }

    @Override
    public void draw(@NotNull FlixelFrame frame, float width, float height, @NotNull FlixelAffine transform) {}

    @Override
    public void draw(@NotNull FlixelTexture texture, float @NotNull [] vertices, int offset, int count) {}

    @Override
    public void destroy() {}

    @Override
    public int getRenderCalls() {
      return 0;
    }

    @Override
    public int getTotalRenderCalls() {
      return 0;
    }

    @Override
    public FlixelColor getColor() {
      return color;
    }

    @Override
    public void setColor(@NotNull FlixelColor color) {
      this.color.set(color);
    }

    @Override
    public void setColor(float r, float g, float b, float a) {
      color.set(r, g, b, a);
    }

    @Override
    public FlixelBlendMode getBlendMode() {
      return FlixelBlendMode.NORMAL;
    }

    @Override
    public void setBlendMode(@Nullable FlixelBlendMode mode) {}

    @Override
    public FlixelShader getShader() {
      return null;
    }

    @Override
    public void setShader(@Nullable FlixelShader shader) {}

    @Override
    public FlixelMatrix getProjection() {
      return projection;
    }

    @Override
    public void setProjection(@NotNull FlixelMatrix projection) {}

    @Override
    public FlixelMatrix getTransform() {
      return transform;
    }

    @Override
    public void setTransform(@NotNull FlixelMatrix transform) {}
  }
}
