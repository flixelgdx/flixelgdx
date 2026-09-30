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
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelNoopTexture;
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

/**
 * Checks that {@link FlixelEmitter} submits every visible particle in a single vertex draw.
 *
 * <p>Lives in {@code org.flixelgdx} to reach the package-private {@link Flixel#setDrawCamera(FlixelCamera)}.
 * The batch is a recording proxy since the test has no GPU.
 */
@ExtendWith(FlixelHeadlessExtension.class)
class FlixelEmitterDrawTest {

  private int vertexDraws;
  private int lastFloatCount;
  private float[] lastVertices;
  private FlixelBlendMode lastBlend;
  private FlixelBatch batch;
  private FlixelEmitter<FlixelParticle> emitter;

  @BeforeEach
  void setUp() {
    Flixel.setDrawCamera(new FlixelCamera(200, 150));
    batch = (FlixelBatch) Proxy.newProxyInstance(
        FlixelBatch.class.getClassLoader(), new Class<?>[] { FlixelBatch.class }, (proxy, method, args) -> {
          String name = method.getName();
          if (name.equals("draw") && args.length == 4 && args[1] instanceof float[] v) {
            vertexDraws++;
            lastVertices = v;
            lastFloatCount = (int) args[3];
          } else if (name.equals("setBlendMode") && lastBlend == null) {
            lastBlend = (FlixelBlendMode) args[0];
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
  void drawsAllParticlesInOneCall() {
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 3);
    emitter.draw(batch);
    assertEquals(1, vertexDraws);
    assertEquals(3 * 20, lastFloatCount);
  }

  @Test
  void quadIsCenteredOnParticle() {
    emitter.setPosition(50f, 60f);
    emitter.start(true, 0f, 1);
    emitter.draw(batch);
    // Bottom-left corner of a 4x4 quad centered at (50, 60), with the view's Y pointing down.
    assertEquals(48f, lastVertices[0], 1e-4f);
    assertEquals(62f, lastVertices[1], 1e-4f);
    // Top-right corner.
    assertEquals(52f, lastVertices[10], 1e-4f);
    assertEquals(58f, lastVertices[11], 1e-4f);
    assertEquals(FlixelColor.WHITE.toFloatBits(), lastVertices[4]);
  }

  @Test
  void offscreenParticlesAreCulled() {
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 2);
    emitter.getParticle(0).x = -500f;
    emitter.draw(batch);
    assertEquals(20, lastFloatCount);
  }

  @Test
  void nothingDrawnWhenEveryParticleIsCulled() {
    emitter.setPosition(-500f, -500f);
    emitter.start(true, 0f, 2);
    emitter.draw(batch);
    assertEquals(0, vertexDraws);
  }

  @Test
  void animateFramesStepsThroughFramesOverLife() {
    FlixelGraphic sheet = new FlixelGraphic(Flixel.assets, "test-sheet", new FlixelNoopTexture(4, 4));
    emitter.loadGraphic(sheet.retain(), 2, 2);
    assertEquals(4, emitter.getFrameCount());
    emitter.animateFrames = true;
    emitter.lifespan.set(1f);
    FlixelParticle p = emitter.emitParticle();
    emitter.update(0.6f);
    assertEquals(2, p.frame);
  }

  @Test
  void blendModeIsAppliedAroundTheDraw() {
    emitter.setBlendMode(FlixelBlendMode.ADD);
    emitter.setPosition(50f, 50f);
    emitter.start(true, 0f, 1);
    emitter.draw(batch);
    assertEquals(FlixelBlendMode.ADD, lastBlend);
  }
}
