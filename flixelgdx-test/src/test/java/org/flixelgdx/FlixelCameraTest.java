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

import org.flixelgdx.FlixelCamera.FollowStyle;
import org.flixelgdx.functional.FlixelPositional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Exercises {@link FlixelCamera} follow and scroll-clamp math.
 *
 * <p>No graphics backend is installed, so the camera's viewport applies through the no-op graphics
 * manager. That is fine here: every assertion is about {@code scrollX}/{@code scrollY}, which the
 * follow and clamp math derives from the camera's own width and zoom, not from any GPU state.
 */
class FlixelCameraTest {

  private static final float DELTA = 1f / 60f;
  private static final float EPS = 0.5f;

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
    public float getAngle() {
      return 0;
    }

    @Override
    public void setAngle(float degrees) {}

    @Override
    public void changeAngle(float deltaDegrees) {}
  }
}
