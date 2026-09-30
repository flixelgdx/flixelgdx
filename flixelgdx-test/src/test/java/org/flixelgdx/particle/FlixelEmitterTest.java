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
package org.flixelgdx.particle;

import org.flixelgdx.FlixelHeadlessExtension;
import org.flixelgdx.tween.ease.FlixelEase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(FlixelHeadlessExtension.class)
class FlixelEmitterTest {

  @Test
  void constructorCreatesEveryParticleUpFront() {
    int[] created = { 0 };
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(8, () -> {
      created[0]++;
      return new FlixelParticle();
    });
    assertEquals(8, created[0]);
    assertEquals(8, emitter.getCapacity());
    assertEquals(0, emitter.getCount());
    for (int i = 0; i < 8; i++) {
      assertSame(emitter, emitter.getParticle(i).emitter);
    }
  }

  @Test
  void constructorRejectsZeroCapacity() {
    assertThrows(IllegalArgumentException.class, () -> new FlixelEmitter<>(0, FlixelParticle::new));
  }

  @Test
  void explodeLaunchesQuantityOrEverySlot() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(10, FlixelParticle::new);
    emitter.start(true, 0f, 4);
    assertEquals(4, emitter.getCount());
    assertFalse(emitter.emitting);

    emitter.clear();
    emitter.start(true);
    assertEquals(10, emitter.getCount());
  }

  @Test
  void continuousEmissionFollowsFrequency() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(32, FlixelParticle::new);
    emitter.start(false, 0.1f);
    emitter.update(0.35f);
    assertEquals(3, emitter.getCount());
    assertTrue(emitter.emitting);
  }

  @Test
  void continuousEmissionStopsAtQuantity() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(32, FlixelParticle::new);
    emitter.start(false, 0.1f, 2);
    emitter.update(1f);
    assertEquals(2, emitter.getCount());
    assertFalse(emitter.emitting);
  }

  @Test
  void particlesDieAtLifespanAndRunOnDeath() {
    int[] deaths = { 0 };
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(4, () -> new FlixelParticle() {
      @Override
      public void onDeath() {
        deaths[0]++;
      }
    });
    emitter.lifespan.set(1f);
    emitter.start(true, 0f, 3);
    emitter.update(0.5f);
    assertEquals(3, emitter.getCount());
    emitter.update(0.6f);
    assertEquals(0, emitter.getCount());
    assertEquals(3, deaths[0]);
  }

  @Test
  void killedParticleIsRecycledAndSurvivorsStayPacked() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(5, FlixelParticle::new);
    emitter.lifespan.set(0f);
    emitter.start(true);
    FlixelParticle victim = emitter.getParticle(1);
    victim.kill();
    emitter.update(0.016f);
    assertEquals(4, emitter.getCount());
    for (int i = 0; i < emitter.getCount(); i++) {
      assertTrue(emitter.getParticle(i).alive);
      assertTrue(emitter.getParticle(i) != victim);
    }
    assertSame(victim, emitter.getParticle(4));
  }

  @Test
  void fullEmitterRecyclesOrSkips() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(3, FlixelParticle::new);
    emitter.start(true);
    assertNotNull(emitter.emitParticle());
    assertEquals(3, emitter.getCount());

    emitter.recycleWhenFull = false;
    assertNull(emitter.emitParticle());
    assertEquals(3, emitter.getCount());
  }

  @Test
  void alphaFadesOverLifeAndFollowsEase() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(1, FlixelParticle::new);
    emitter.lifespan.set(1f);
    emitter.alpha.set(1f, 1f, 0f, 0f);
    FlixelParticle p = emitter.emitParticle();
    assertNotNull(p);
    assertEquals(1f, p.alpha, 1e-5f);
    emitter.update(0.5f);
    assertEquals(0.5f, p.alpha, 1e-5f);

    emitter.clear();
    emitter.alpha.ease = FlixelEase::quadIn;
    p = emitter.emitParticle();
    assertNotNull(p);
    emitter.update(0.5f);
    assertEquals(0.75f, p.alpha, 1e-5f);
  }

  @Test
  void matchingStartAndEndKeepsValueConstant() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(1, FlixelParticle::new);
    emitter.lifespan.set(1f);
    emitter.speed.set(50f, 150f);
    FlixelParticle p = emitter.emitParticle();
    assertNotNull(p);
    assertFalse(p.velocityRangeActive);
    float vx = p.velocityX;
    float vy = p.velocityY;
    emitter.update(0.5f);
    assertEquals(vx, p.velocityX, 1e-5f);
    assertEquals(vy, p.velocityY, 1e-5f);
  }

  @Test
  void circleModeLaunchesAlongLaunchAngle() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(1, FlixelParticle::new);
    emitter.launchAngle.set(90f);
    emitter.speed.set(100f);
    FlixelParticle p = emitter.emitParticle();
    assertNotNull(p);
    assertEquals(0f, p.velocityX, 0.5f);
    assertEquals(100f, p.velocityY, 0.5f);
  }

  @Test
  void squareModeRollsInsideVelocityBounds() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(64, FlixelParticle::new);
    emitter.launchMode = FlixelEmitterMode.SQUARE;
    emitter.velocity.set(-10f, -60f, 10f, -30f);
    emitter.start(true);
    for (int i = 0; i < emitter.getCount(); i++) {
      FlixelParticle p = emitter.getParticle(i);
      assertTrue(p.velocityX >= -10f && p.velocityX <= 10f);
      assertTrue(p.velocityY >= -60f && p.velocityY <= -30f);
    }
  }

  @Test
  void accelerationChangesVelocityOverTime() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(1, FlixelParticle::new);
    emitter.speed.set(0f);
    emitter.acceleration.set(0f, 100f);
    FlixelParticle p = emitter.emitParticle();
    assertNotNull(p);
    emitter.update(1f);
    assertEquals(100f, p.velocityY, 1e-4f);
    assertEquals(100f, p.y, 1e-4f);
  }

  @Test
  void spawnPointStaysInsideSpawnArea() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(50f, 20f, 64, FlixelParticle::new);
    emitter.setSize(30f, 10f);
    emitter.start(true);
    for (int i = 0; i < emitter.getCount(); i++) {
      FlixelParticle p = emitter.getParticle(i);
      assertTrue(p.x >= 50f && p.x <= 80f);
      assertTrue(p.y >= 20f && p.y <= 30f);
    }
  }

  @Test
  void sameSeedProducesSameEffect() {
    FlixelEmitter<FlixelParticle> a = new FlixelEmitter<>(16, FlixelParticle::new);
    FlixelEmitter<FlixelParticle> b = new FlixelEmitter<>(16, FlixelParticle::new);
    a.random.setSeed(1234L);
    b.random.setSeed(1234L);
    a.lifespan.set(0.5f, 2f);
    b.lifespan.set(0.5f, 2f);
    a.start(true);
    b.start(true);
    for (int i = 0; i < 16; i++) {
      assertEquals(a.getParticle(i).velocityX, b.getParticle(i).velocityX);
      assertEquals(a.getParticle(i).lifespan, b.getParticle(i).lifespan);
    }
  }

  @Test
  void keepScaleRatioCopiesXToY() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(16, FlixelParticle::new);
    emitter.scale.set(0.5f, 0.5f, 2f, 2f);
    emitter.keepScaleRatio = true;
    emitter.start(true);
    for (int i = 0; i < emitter.getCount(); i++) {
      FlixelParticle p = emitter.getParticle(i);
      assertEquals(p.scaleX, p.scaleY);
    }
  }

  @Test
  void onDeathCanLaunchNewParticles() {
    FlixelEmitter<FlixelParticle> emitter = new FlixelEmitter<>(4, () -> new FlixelParticle() {
      @Override
      public void onDeath() {
        if (lifespan < 0.5f) {
          FlixelParticle child = emitter.emitParticle();
          assertNotNull(child);
          child.lifespan = 10f;
        }
      }
    });
    emitter.lifespan.set(0.25f);
    emitter.start(true, 0f, 2);
    emitter.update(0.3f);
    assertEquals(2, emitter.getCount());
    for (int i = 0; i < emitter.getCount(); i++) {
      assertTrue(emitter.getParticle(i).alive);
      assertEquals(10f, emitter.getParticle(i).lifespan);
    }
  }

  @Test
  void particleWithoutEmitterKeepsItsFrame() {
    FlixelParticle p = new FlixelParticle();
    p.lifespan = 1f;
    p.alive = true;
    p.update(0.5f);
    assertEquals(0, p.frame, "a particle without an emitter never changes frame");
  }

  @Test
  void rangeBoundsSetCopiesStartToEnd() {
    FlixelRangeBounds range = new FlixelRangeBounds(0f);
    range.set(2f, 4f);
    assertEquals(2f, range.end.min);
    assertEquals(4f, range.end.max);
    assertFalse(range.changes());
    range.set(2f, 4f, 0f, 0f);
    assertTrue(range.changes());
  }
}
