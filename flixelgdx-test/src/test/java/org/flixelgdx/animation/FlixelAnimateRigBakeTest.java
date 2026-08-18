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
package org.flixelgdx.animation;

import org.flixelgdx.math.FlixelAffine;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the pure geometry of {@link FlixelAnimateRigLoader#bakePartAffine} for both the standard
 * (non-rotated) and the 90-degree-CW-rotated atlas packing cases.
 *
 * <p>Coordinate space primer: the baked affine maps from <strong>atlas local space</strong>
 * (Y-down, top-left at the origin) to <strong>rig local space</strong> (also Y-down, top-left of
 * the anchor box at the origin). For a non-rotated part the atlas quad is {@code (0..origW) x (0..origH)}.
 * For a 90-degree-CW packed part the atlas region has its width and height swapped, so the quad is
 * {@code (0..origH) x (0..origW)} - that is, atlas width = origH, atlas height = origW.
 *
 * <p>No GPU texture is required; all arithmetic is pure matrix math.
 */
class FlixelAnimateRigBakeTest {

  private static final float DELTA = 1e-4f;

  /** Applies the affine to the given local point and returns (x', y'). */
  private static float[] apply(FlixelAffine a, float x, float y) {
    return new float[] { a.m00 * x + a.m01 * y + a.m02, a.m10 * x + a.m11 * y + a.m12 };
  }

  /**
   * Non-rotated part at the anchor origin. With an identity Flash matrix and a zero-offset anchor,
   * the baked affine must be identity so every local atlas coordinate maps 1:1 to rig space.
   */
  @Test
  void nonRotatedIdentityFlashMatrix() {
    float origW = 100f;
    float origH = 60f;
    FlixelAffine identity = new FlixelAffine();
    FlixelAffine out = new FlixelAffine();

    FlixelAnimateRigLoader.bakePartAffine(out, identity, false, 0f, 0f);

    float[] tl = apply(out, 0f, 0f);
    assertEquals(0f, tl[0], DELTA, "top-left x");
    assertEquals(0f, tl[1], DELTA, "top-left y");

    float[] br = apply(out, origW, origH);
    assertEquals(origW, br[0], DELTA, "bottom-right x");
    assertEquals(origH, br[1], DELTA, "bottom-right y");
  }

  /**
   * Rotated (90 degrees CW) part at the anchor origin. The atlas quad has width = origH and
   * height = origW, so local space is {@code (0..origH) x (0..origW)}. Un-rotating the packing in
   * Y-down space is a reflection across the main diagonal, so the baked affine maps each atlas corner
   * to the rig corner that holds the same pixel:
   * <ul>
   *   <li>Atlas top-left (0, 0) must land at rig top-left (0, 0).</li>
   *   <li>Atlas top-right (origH, 0) must land at rig bottom-left (0, origH).</li>
   *   <li>Atlas bottom-right (origH, origW) must land at rig bottom-right (origW, origH).</li>
   *   <li>Atlas bottom-left (0, origW) must land at rig top-right (origW, 0).</li>
   * </ul>
   */
  @Test
  void rotatedIdentityFlashMatrix() {
    float origW = 100f;
    float origH = 60f;
    FlixelAffine identity = new FlixelAffine();
    FlixelAffine out = new FlixelAffine();

    FlixelAnimateRigLoader.bakePartAffine(out, identity, true, 0f, 0f);

    float[] tl = apply(out, 0f, 0f);
    assertEquals(0f, tl[0], DELTA, "atlas top-left x -> rig top-left x");
    assertEquals(0f, tl[1], DELTA, "atlas top-left y -> rig top-left y");

    float[] tr = apply(out, origH, 0f);
    assertEquals(0f, tr[0], DELTA, "atlas top-right x -> rig bottom-left x");
    assertEquals(origH, tr[1], DELTA, "atlas top-right y -> rig bottom-left y");

    float[] br = apply(out, origH, origW);
    assertEquals(origW, br[0], DELTA, "atlas bottom-right x -> rig bottom-right x");
    assertEquals(origH, br[1], DELTA, "atlas bottom-right y -> rig bottom-right y");

    float[] bl = apply(out, 0f, origW);
    assertEquals(origW, bl[0], DELTA, "atlas bottom-left x -> rig top-right x");
    assertEquals(0f, bl[1], DELTA, "atlas bottom-left y -> rig top-right y");
  }

  /**
   * Non-zero anchor offsets are applied identically to both rotated and non-rotated parts. With a
   * Flash translate that positions the sprite at the anchor's top-left corner, both cases must
   * produce a translation that cancels the Flash translation symmetrically.
   */
  @Test
  void anchorOffsetAppliedToBothCases() {
    float origW = 80f;
    float origH = 40f;
    float anchorMinX = 10f;
    float anchorMinY = 5f;

    // Placing the sprite at the anchor position so the anchor shift cancels out and the baked
    // affine should reduce to identity for the non-rotated case.
    FlixelAffine flashAtAnchor = new FlixelAffine();
    flashAtAnchor.m02 = anchorMinX;
    flashAtAnchor.m12 = anchorMinY;

    FlixelAffine outNormal = new FlixelAffine();
    FlixelAnimateRigLoader.bakePartAffine(outNormal, flashAtAnchor, false, anchorMinX, anchorMinY);

    // With the sprite exactly at the anchor, the non-rotated baked affine must be identity.
    float[] tlNormal = apply(outNormal, 0f, 0f);
    assertEquals(0f, tlNormal[0], DELTA, "non-rotated: top-left x at anchor");
    assertEquals(0f, tlNormal[1], DELTA, "non-rotated: top-left y at anchor");

    float[] brNormal = apply(outNormal, origW, origH);
    assertEquals(origW, brNormal[0], DELTA, "non-rotated: bottom-right x at anchor");
    assertEquals(origH, brNormal[1], DELTA, "non-rotated: bottom-right y at anchor");

    // The rotated case with the same Flash translate and anchor should also cancel the anchor shift.
    // Atlas quad is (0..origH) x (0..origW).
    FlixelAffine outRotated = new FlixelAffine();
    FlixelAnimateRigLoader.bakePartAffine(outRotated, flashAtAnchor, true, anchorMinX, anchorMinY);

    // Atlas top-left (0, 0) -> rig top-left (0, 0).
    float[] tlRotated = apply(outRotated, 0f, 0f);
    assertEquals(0f, tlRotated[0], DELTA, "rotated: top-left x at anchor");
    assertEquals(0f, tlRotated[1], DELTA, "rotated: top-left y at anchor");

    // Atlas bottom-right (origH, origW) -> rig bottom-right (origW, origH).
    float[] brRotated = apply(outRotated, origH, origW);
    assertEquals(origW, brRotated[0], DELTA, "rotated: bottom-right x at anchor");
    assertEquals(origH, brRotated[1], DELTA, "rotated: bottom-right y at anchor");
  }

  /**
   * The bounding box of the rig-space corners must be identical for a non-rotated and a
   * rotated part with the same Flash world matrix and anchor, because atlas rotation is purely
   * a packing detail - it must not change where the pixels land on screen. The rotated atlas
   * quad has corners {@code (0..origH) x (0..origW)} while the non-rotated quad is
   * {@code (0..origW) x (0..origH)}, so the test evaluates different corner coordinates through
   * two different baked affines and verifies their screen-space bounding boxes agree.
   */
  @Test
  void rotatedAndNonRotatedHaveSameWorldBoundingBox() {
    float origW = 120f;
    float origH = 80f;
    FlixelAffine flashTranslate = new FlixelAffine();
    flashTranslate.m02 = 20f;
    flashTranslate.m12 = 15f;

    FlixelAffine outNormal = new FlixelAffine();
    FlixelAnimateRigLoader.bakePartAffine(outNormal, flashTranslate, false, 0f, 0f);

    FlixelAffine outRotated = new FlixelAffine();
    FlixelAnimateRigLoader.bakePartAffine(outRotated, flashTranslate, true, 0f, 0f);

    // 4 corners of the non-rotated quad: (0..origW) x (0..origH)
    float[][] normalCorners = {
        apply(outNormal, 0f, 0f),
        apply(outNormal, origW, 0f),
        apply(outNormal, origW, origH),
        apply(outNormal, 0f, origH)
    };
    // 4 corners of the rotated quad: atlas width = origH, atlas height = origW
    float[][] rotatedCorners = {
        apply(outRotated, 0f, 0f),
        apply(outRotated, origH, 0f),
        apply(outRotated, origH, origW),
        apply(outRotated, 0f, origW)
    };

    assertEquals(minX(normalCorners), minX(rotatedCorners), DELTA, "bbox minX");
    assertEquals(minY(normalCorners), minY(rotatedCorners), DELTA, "bbox minY");
    assertEquals(maxX(normalCorners), maxX(rotatedCorners), DELTA, "bbox maxX");
    assertEquals(maxY(normalCorners), maxY(rotatedCorners), DELTA, "bbox maxY");
  }

  private static float minX(float[][] pts) {
    float m = Float.POSITIVE_INFINITY;
    for (float[] p : pts) {
      if (p[0] < m)
        m = p[0];
    }
    return m;
  }

  private static float minY(float[][] pts) {
    float m = Float.POSITIVE_INFINITY;
    for (float[] p : pts) {
      if (p[1] < m)
        m = p[1];
    }
    return m;
  }

  private static float maxX(float[][] pts) {
    float m = Float.NEGATIVE_INFINITY;
    for (float[] p : pts) {
      if (p[0] > m)
        m = p[0];
    }
    return m;
  }

  private static float maxY(float[][] pts) {
    float m = Float.NEGATIVE_INFINITY;
    for (float[] p : pts) {
      if (p[1] > m)
        m = p[1];
    }
    return m;
  }
}
