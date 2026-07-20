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

import com.badlogic.gdx.utils.viewport.FitViewport;

import org.flixelgdx.FlixelCamera.FollowStyle;
import org.flixelgdx.functional.FlixelPositional;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(GdxHeadlessExtension.class)
class FlixelCameraTest {

  private static final float DELTA = 1f / 60f;
  private static final float EPS = 0.5f;

  private static FlixelCamera.ViewportFactory savedFactory;

  @BeforeAll
  static void installNoOpViewport() {
    savedFactory = FlixelCamera.viewportFactory;
    // Replace the viewport factory with one that skips the Gdx.gl.glViewport call, which is
    // unavailable in the headless test environment. Camera matrices are still updated so that
    // the follow and clamp math (which uses this.width / zoom, not camera.viewportWidth) works.
    FlixelCamera.viewportFactory = (w, h, cam) -> new FitViewport(w, h, cam) {
      @Override
      public void apply(boolean centerCamera) {
        if (centerCamera) {
          getCamera().position.set(getWorldWidth() / 2f, getWorldHeight() / 2f, 0);
          getCamera().update();
        }
      }
    };
  }

  @AfterAll
  static void restoreViewport() {
    FlixelCamera.viewportFactory = savedFactory;
  }

  private FlixelCamera camera;

  @BeforeEach
  void setUp() {
    camera = new FlixelCamera(400, 300);
    camera.scrollX = 0;
    camera.scrollY = 0;
  }

  @Test
  void lockonCentersOnTarget() {
    camera.follow(stub(500, 350, 0, 0), FollowStyle.LOCKON);
    camera.update(DELTA);
    assertEquals(500f, camera.scrollX + camera.getViewWidth() / 2f, EPS);
    assertEquals(350f, camera.scrollY + camera.getViewHeight() / 2f, EPS);
  }

  @Test
  void topdownKeepsCameraStillWhenTargetIsInsideDeadzone() {
    camera = new FlixelCamera(300, 300);
    camera.scrollX = 0;
    camera.scrollY = 0;
    camera.follow(stub(150, 150, 0, 0), FollowStyle.TOPDOWN);
    camera.update(DELTA);
    assertEquals(0f, camera.scrollX, EPS, "camera must not scroll when target is inside the deadzone");
    assertEquals(0f, camera.scrollY, EPS);
  }

  @Test
  void topdownMovesToDeadzoneEdgeAndNotToCenter() {
    camera = new FlixelCamera(300, 300);
    camera.scrollX = 0;
    camera.scrollY = 0;
    camera.follow(stub(50, 150, 0, 0), FollowStyle.TOPDOWN);
    camera.update(DELTA);
    assertEquals(-50f, camera.scrollX, EPS);
  }

  @Test
  void screenByScreenSnapsForwardWhenTargetCrossesRightEdge() {
    camera.follow(stub(400, 150, 0, 0), FollowStyle.SCREEN_BY_SCREEN);
    camera.update(DELTA);
    assertEquals(400f, camera.scrollX, EPS);
  }

  @Test
  void screenByScreenSnapsBackWhenTargetCrossesLeftEdge() {
    camera.scrollX = 400;
    // viewLeft = 400; target center at 399 < 400 -> snap back one screen.
    camera.follow(stub(399, 150, 0, 0), FollowStyle.SCREEN_BY_SCREEN);
    camera.update(DELTA);
    assertEquals(0f, camera.scrollX, EPS);
  }

  @Test
  void screenByScreenDoesNotSnapWhenTargetIsOnScreen() {
    camera.scrollX = 0;
    camera.follow(stub(200, 150, 0, 0), FollowStyle.SCREEN_BY_SCREEN);
    camera.update(DELTA);
    assertEquals(0f, camera.scrollX, EPS);
  }

  @Test
  void updateScrollClampsScrollXToMaxBound() {
    camera.setScrollBoundsRect(0, 0, 2000, 1000);
    camera.scrollX = 1700;
    camera.updateScroll();
    assertEquals(1600f, camera.scrollX, EPS);
  }

  @Test
  void updateScrollClampsScrollYToMaxBound() {
    camera.setScrollBoundsRect(0, 0, 2000, 1000);
    camera.scrollY = 800;
    camera.updateScroll();
    assertEquals(700f, camera.scrollY, EPS);
  }

  @Test
  void updateScrollClampsScrollToMinBound() {
    camera.setScrollBoundsRect(0, 0, 2000, 1000);
    camera.scrollX = -100;
    camera.scrollY = -50;
    camera.updateScroll();
    assertEquals(0f, camera.scrollX, EPS);
    assertEquals(0f, camera.scrollY, EPS);
  }

  @Test
  void updateScrollPinsToMinWhenLevelIsSmallerThanView() {
    camera.setScrollBoundsRect(0, 0, 100, 100);
    camera.scrollX = 50;
    camera.scrollY = 50;
    camera.updateScroll();
    assertEquals(0f, camera.scrollX, EPS, "min edge must win when level is smaller than the view");
    assertEquals(0f, camera.scrollY, EPS);
  }

  @Test
  void followWithBoundsStopsAtRightEdgeOfLevel() {
    camera.setScrollBoundsRect(0, 0, 2000, 1000);
    camera.follow(stub(1950, 500, 32, 32), FollowStyle.LOCKON);
    camera.update(DELTA);
    assertEquals(1600f, camera.scrollX, EPS);
  }

  @Test
  void followWithBoundsStopsAtLeftEdgeOfLevel() {
    camera.setScrollBoundsRect(0, 0, 2000, 1000);
    camera.scrollX = 0;
    camera.follow(stub(-200, 150, 0, 0), FollowStyle.LOCKON);
    camera.update(DELTA);
    assertEquals(0f, camera.scrollX, EPS);
  }

  private static FlixelPositional stub(float x, float y, float w, float h) {
    return new StubPositional(x, y, w, h);
  }

  private static final class StubPositional implements FlixelPositional {

    private float x;
    private float y;
    private final float w;
    private final float h;

    StubPositional(float x, float y, float w, float h) {
      this.x = x;
      this.y = y;
      this.w = w;
      this.h = h;
    }

    @Override
    public float getX() {
      return x;
    }

    @Override
    public void setX(float x) {
      this.x = x;
    }

    @Override
    public float getY() {
      return y;
    }

    @Override
    public void setY(float y) {
      this.y = y;
    }

    @Override
    public float getWidth() {
      return w;
    }

    @Override
    public void setWidth(float width) {}

    @Override
    public float getHeight() {
      return h;
    }

    @Override
    public void setHeight(float height) {}

    @Override
    public float getScrollX() {
      return 1f;
    }

    @Override
    public float getScrollY() {
      return 1f;
    }

    @Override
    public void setScrollFactor(float scrollX, float scrollY) {}

    @Override
    public float getLastX() {
      return x;
    }

    @Override
    public float getLastY() {
      return y;
    }

    @Override
    public void setSize(float width, float height) {}

    @Override
    public void changeX(float dx) {
      x += dx;
    }

    @Override
    public void changeY(float dy) {
      y += dy;
    }

    @Override
    public float getMidpointX() {
      return x + w / 2f;
    }

    @Override
    public float getMidpointY() {
      return y + h / 2f;
    }

    @Override
    public float getVelocityX() {
      return 0;
    }

    @Override
    public void setVelocityX(float velocityX) {}

    @Override
    public float getVelocityY() {
      return 0;
    }

    @Override
    public void setVelocityY(float velocityY) {}

    @Override
    public void setVelocity(float vx, float vy) {}

    @Override
    public float getAccelerationX() {
      return 0;
    }

    @Override
    public void setAccelerationX(float ax) {}

    @Override
    public float getAccelerationY() {
      return 0;
    }

    @Override
    public void setAccelerationY(float ay) {}

    @Override
    public void setAcceleration(float ax, float ay) {}

    @Override
    public float getDragX() {
      return 0;
    }

    @Override
    public void setDragX(float dx) {}

    @Override
    public float getDragY() {
      return 0;
    }

    @Override
    public void setDragY(float dy) {}

    @Override
    public void setDrag(float dx, float dy) {}

    @Override
    public float getMaxVelocityX() {
      return 0;
    }

    @Override
    public void setMaxVelocityX(float mvx) {}

    @Override
    public float getMaxVelocityY() {
      return 0;
    }

    @Override
    public void setMaxVelocityY(float mvy) {}

    @Override
    public void setMaxVelocity(float mvx, float mvy) {}

    @Override
    public float getAngularVelocity() {
      return 0;
    }

    @Override
    public void setAngularVelocity(float av) {}

    @Override
    public float getAngularAcceleration() {
      return 0;
    }

    @Override
    public void setAngularAcceleration(float aa) {}

    @Override
    public float getAngularDrag() {
      return 0;
    }

    @Override
    public void setAngularDrag(float ad) {}

    @Override
    public float getMaxAngularVelocity() {
      return 0;
    }

    @Override
    public void setMaxAngularVelocity(float mav) {}

    @Override
    public boolean getMoves() {
      return false;
    }

    @Override
    public void setMoves(boolean moves) {}

    @Override
    public boolean isImmovable() {
      return false;
    }

    @Override
    public void setImmovable(boolean immovable) {}

    @Override
    public float getAngle() {
      return 0;
    }

    @Override
    public void setAngle(float degrees) {}

    @Override
    public void changeAngle(float deltaDegrees) {}
  }
}
