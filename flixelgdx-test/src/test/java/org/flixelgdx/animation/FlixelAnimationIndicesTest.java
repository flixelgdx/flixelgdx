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

import org.flixelgdx.FlixelSprite;
import org.flixelgdx.asset.FlixelBaseAssetManager;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelGraphic;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Covers the index-based clip builders on {@link FlixelAnimationController}: {@code add(...)} for
 * grid and atlas frame numbers, and {@code addByIndices(...)} for numbered atlas frame names.
 */
class FlixelAnimationIndicesTest {

  @Test
  void frameNumberReadsDigitsBetweenPrefixAndPostfix() {
    assertEquals(3, FlixelAnimationController.frameNumber("idle0003", "idle", ""));
    assertEquals(3, FlixelAnimationController.frameNumber("idle3", "idle", ""));
    assertEquals(12, FlixelAnimationController.frameNumber("run0012.png", "run", ".png"));
    assertEquals(0, FlixelAnimationController.frameNumber("idle0000", "idle", ""));
  }

  @Test
  void frameNumberRejectsNamesWithTheWrongShape() {
    assertEquals(-1, FlixelAnimationController.frameNumber(null, "idle", ""));
    assertEquals(-1, FlixelAnimationController.frameNumber("walk0001", "idle", ""));
    assertEquals(-1, FlixelAnimationController.frameNumber("idle", "idle", ""));
    assertEquals(-1, FlixelAnimationController.frameNumber("idle-alt0001", "idle", ""));
    assertEquals(-1, FlixelAnimationController.frameNumber("run0012.jpg", "run", ".png"));
    assertEquals(-1, FlixelAnimationController.frameNumber("idle99999999999", "idle", ""));
  }

  @Test
  void addByIndicesPicksFramesByNumberInListedOrder() {
    FlixelSprite sprite = atlasSprite("idle0000", "idle0001", "idle0002", "idle-alt0001", "walk0001");
    FlixelArray<FlixelFrame> atlas = sprite.getAtlasRegions();
    FlixelAnimationController anim = sprite.animation;

    anim.addByIndices("idle", "idle", new int[] { 2, 0, 2, 1 }, "", 24, false);

    Object[] keyframes = anim.getAnimations().get("idle").getKeyFrames();
    assertEquals(4, keyframes.length);
    assertSame(atlas.get(2), keyframes[0]);
    assertSame(atlas.get(0), keyframes[1]);
    assertSame(atlas.get(2), keyframes[2]);
    assertSame(atlas.get(1), keyframes[3], "idle-alt0001 must not match the idle prefix");
  }

  @Test
  void addByIndicesSkipsMissingNumbersAndRegistersNothingWhenNoneMatch() {
    FlixelSprite sprite = atlasSprite("idle0000", "idle0001");
    FlixelAnimationController anim = sprite.animation;

    anim.addByIndices("partial", "idle", new int[] { 0, 7, 1 }, 24, true);
    Object[] partial = anim.getAnimations().get("partial").getKeyFrames();
    assertEquals(2, partial.length);

    anim.addByIndices("none", "idle", new int[] { 5, 6 }, 24, true);
    assertFalse(anim.getAnimations().containsKey("none"));
  }

  @Test
  void addUsesAtlasOrderWhenTheSpriteHasAnAtlas() {
    FlixelSprite sprite = atlasSprite("a", "b", "c");
    FlixelArray<FlixelFrame> atlas = sprite.getAtlasRegions();
    FlixelAnimationController anim = sprite.animation;

    anim.add("clip", new int[] { 2, 0, 9 }, 12, true);

    Object[] keyframes = anim.getAnimations().get("clip").getKeyFrames();
    assertEquals(2, keyframes.length);
    assertSame(atlas.get(2), keyframes[0]);
    assertSame(atlas.get(0), keyframes[1]);
  }

  @Test
  void addCountsGridFramesLeftToRightThenTopToBottom() {
    FlixelSprite sprite = new FlixelSprite();
    sprite.loadGraphic(graphic(new FlixelNoopTexture(64, 32)), 16, 16); // 4 columns, 2 rows.
    FlixelFrame[][] grid = sprite.getFrames();
    FlixelAnimationController anim = sprite.ensureAnimation();

    anim.add("walk", new int[] { 0, 5, 7, 8 }, 10);

    FlixelAnimation<FlixelFrame> clip = anim.getAnimations().get("walk");
    assertNotNull(clip);
    Object[] keyframes = clip.getKeyFrames();
    assertEquals(3, keyframes.length, "index 8 is past the end of the grid and must be skipped");
    assertSame(grid[0][0], keyframes[0]);
    assertSame(grid[1][1], keyframes[1]);
    assertSame(grid[1][3], keyframes[2]);
    assertEquals(0.1f, clip.getFrameDuration(), 1e-6f);
  }

  private static FlixelSprite atlasSprite(String... names) {
    FlixelTexture texture = new FlixelNoopTexture(64, 64);
    FlixelArray<FlixelFrame> frames = new FlixelArray<>();
    for (String name : names) {
      FlixelFrame frame = new FlixelFrame(texture, 0, 0, 8, 8);
      frame.name = name;
      frames.add(frame);
    }
    FlixelSprite sprite = new FlixelSprite();
    sprite.ensureAnimation();
    sprite.applySparrowAtlas(graphic(texture), frames);
    return sprite;
  }

  private static FlixelGraphic graphic(FlixelTexture texture) {
    FlixelBaseAssetManager assets = new FlixelBaseAssetManager();
    return new FlixelGraphic(assets, assets.allocateSyntheticKey(), texture);
  }
}
