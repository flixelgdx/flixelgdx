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
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelShader;
import org.flixelgdx.particle.FlixelEmitter;
import org.flixelgdx.particle.FlixelParticle;
import org.flixelgdx.util.FlixelBlendMode;
import org.flixelgdx.util.FlixelColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.lang.reflect.Proxy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Checks how {@link FlixelEmitter} hands its particles to the batch.
 *
 * <p>Lives in {@code org.flixelgdx} to reach the package-private {@link Flixel#setDrawCamera(FlixelCamera)}.
 * The batch is a recording proxy since the test has no GPU.
 */
@ExtendWith(FlixelHeadlessExtension.class)
class FlixelEmitterDrawTest {

  private int frameDraws;
  private int blendChanges;
  private int shaderChanges;
  private FlixelBlendMode firstBlend;
  private FlixelBatch batch;
  private FlixelEmitter<FlixelParticle> emitter;

  @BeforeEach
  void setUp() {
    Flixel.setDrawCamera(new FlixelCamera(200, 150));
    batch = (FlixelBatch) Proxy.newProxyInstance(
        FlixelBatch.class.getClassLoader(), new Class<?>[] { FlixelBatch.class }, (proxy, method, args) -> {
          String name = method.getName();
          if (name.equals("draw") && args[0] instanceof FlixelFrame) {
            frameDraws++;
          } else if (name.equals("setShader")) {
            shaderChanges++;
          } else if (name.equals("setBlendMode")) {
            blendChanges++;
            if (firstBlend == null) {
              firstBlend = (FlixelBlendMode) args[0];
            }
          }
          Class<?> r = method.getReturnType();
          if (r == int.class) {
            return 0;
          }
          if (r == boolean.class) {
            return false;
          }
          if (r == float.class) {
            return 0f;
          }
          if (r == FlixelColor.class) {
            return new FlixelColor();
          }
          return null;
        });

    emitter = new FlixelEmitter<>(8, FlixelParticle::new);
    FlixelGraphic graphic = new FlixelGraphic(Flixel.assets, "test-particle", new FlixelNoopTexture(4, 4));
    emitter.loadGraphic(graphic.retain(), 4, 4);
    emitter.speed.set(0f);
  }

  @AfterEach
  void tearDown() {
    Flixel.setDrawCamera(null);
  }

  @Test
  void drawsEveryVisibleParticle() {
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 3);
    emitter.draw(batch);
    assertEquals(3, frameDraws);
  }

  @Test
  void particleIsCenteredOnSpawnPoint() {
    emitter.setPosition(50f, 60f);
    FlixelParticle p = emitter.emitParticle();
    assertNotNull(p);
    assertEquals(48f, p.getX(), 1e-4f);
    assertEquals(58f, p.getY(), 1e-4f);
    assertSame(emitter.getFrame(0), p.getCurrentFrame());
  }

  @Test
  void offscreenParticlesAreCulled() {
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 2);
    emitter.getParticle(0).setX(-500f);
    emitter.draw(batch);
    assertEquals(1, frameDraws);
  }

  @Test
  void blendModeIsSetOnceAroundAllParticles() {
    emitter.setBlendMode(FlixelBlendMode.ADD);
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 4);
    emitter.draw(batch);
    assertEquals(FlixelBlendMode.ADD, firstBlend);
    assertEquals(2, blendChanges, "the emitter should switch to ADD once and back to NORMAL once");
  }

  @Test
  void shaderIsSetOnceAroundAllParticles() {
    FlixelShader glow = new FlixelShader("void main() {}") {
      @Override
      public boolean isCompiled() {
        return true;
      }
    };
    emitter.setShader(glow);
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 4);
    emitter.draw(batch);
    assertEquals(4, frameDraws);
    assertEquals(2, shaderChanges, "the emitter should switch the shader on once and off once");
  }

  @Test
  void emitterCamerasArePassedToParticles() {
    FlixelCamera hud = new FlixelCamera(200, 150);
    emitter.cameras = new FlixelCamera[] { hud };
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 1);
    Flixel.setDrawCamera(hud);
    emitter.draw(batch);
    assertEquals(1, frameDraws);
    assertSame(emitter.cameras, emitter.getParticle(0).cameras);
  }

  @Test
  void animateFramesStepsThroughFramesOverLife() {
    FlixelGraphic sheet = new FlixelGraphic(Flixel.assets, "test-sheet", new FlixelNoopTexture(4, 4));
    emitter.loadGraphic(sheet.retain(), 2, 2);
    assertEquals(4, emitter.getFrameCount());
    emitter.animateFrames = true;
    emitter.lifespan.set(1f);
    FlixelParticle p = emitter.emitParticle();
    assertNotNull(p);
    emitter.update(0.6f);
    assertSame(emitter.getFrame(2), p.getCurrentFrame());
  }
}
