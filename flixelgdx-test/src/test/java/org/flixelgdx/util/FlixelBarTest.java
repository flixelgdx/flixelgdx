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
package org.flixelgdx.util;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.graphics.FlixelFrame;
import org.flixelgdx.graphics.FlixelNoopTexture;
import org.flixelgdx.graphics.FlixelTexture;
import org.flixelgdx.testing.RecordingBatch;
import org.flixelgdx.testing.RecordingBatch.Call;
import org.flixelgdx.util.FlixelBar.BarFillDirection;
import org.flixelgdx.util.FlixelBar.ThresholdStop;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlixelBarTest {

  private static final float EPS = 1e-3f;

  private FlixelCamera camera;
  private FlixelBar bar;
  private RecordingBatch batch;

  @BeforeEach
  void setUp() {
    camera = new FlixelCamera(320, 240);
    Flixel.cameras.add(camera);
    bar = new FlixelBar(10f, 20f, 100f, 10f);
    batch = new RecordingBatch();
  }

  private Call fill() {
    return batch.calls.get(1);
  }

  @Test
  void valuesAreClampedToTheRange() {
    assertEquals(FlixelBar.DEFAULT_MIN, bar.getMin());
    assertEquals(FlixelBar.DEFAULT_MAX, bar.getMax());
    bar.setValue(150f);
    assertEquals(100f, bar.getValue());
    bar.setValue(Float.NaN);
    assertEquals(0f, bar.getValue());
    bar.setRange(50f, 10f);
    assertEquals(10f, bar.getMin(), "a reversed range is swapped");
    assertEquals(50f, bar.getMax());
    assertEquals(10f, bar.getValue());
    assertEquals(10f, bar.getDisplayedValue());
  }

  @Test
  void graphicsCannotBeLoadedOntoABar() {
    assertThrows(UnsupportedOperationException.class, () -> bar.loadGraphic((FlixelTexture) null, 1, 1));
    assertThrows(UnsupportedOperationException.class, () -> bar.makeGraphic(1, 1, FlixelColor.RED));
  }

  @Test
  void drawShowsTheEmptyBackgroundAndThePartThatIsFilled() {
    bar.setValue(25f);
    bar.draw(batch);
    assertEquals(2, batch.calls.getSize());
    Call empty = batch.calls.get(0);
    assertEquals(10f, empty.x(), EPS);
    assertEquals(100f, empty.w(), EPS);
    assertEquals(0.5f, empty.color().a, EPS);
    assertEquals(25f, fill().w(), EPS);
    assertEquals(1f, fill().color().g, EPS);
  }

  @Test
  void anEmptyBarDrawsOnlyTheBackground() {
    bar.draw(batch);
    assertEquals(1, batch.calls.getSize());
  }

  @Test
  void everyFillDirectionGrowsFromItsOwnSide() {
    bar.setValue(25f);
    bar.setFillDirection(BarFillDirection.RIGHT_TO_LEFT).draw(batch);
    assertEquals(BarFillDirection.RIGHT_TO_LEFT, bar.getFillDirection());
    assertEquals(85f, fill().x(), EPS);

    batch.calls.clear();
    bar.setFillDirection(BarFillDirection.TOP_TO_BOTTOM).draw(batch);
    assertEquals(2.5f, fill().h(), EPS);
    assertEquals(27.5f, fill().y(), EPS);

    batch.calls.clear();
    bar.setFillDirection(BarFillDirection.BOTTOM_TO_TOP).draw(batch);
    assertEquals(20f, fill().y(), EPS);
    assertEquals(2.5f, fill().h(), EPS);
    assertThrows(NullPointerException.class, () -> bar.setFillDirection(null));
  }

  @Test
  void customGraphicsAreCroppedByTheFill() {
    FlixelTexture tex = new FlixelNoopTexture(64, 8);
    FlixelFrame empty = new FlixelFrame(tex);
    FlixelFrame filled = new FlixelFrame(tex);
    bar.setEmptyGraphic(empty).setFilledGraphic(filled).setValue(50f);
    for (BarFillDirection d : BarFillDirection.values()) {
      batch.calls.clear();
      bar.setFillDirection(d).draw(batch);
      assertSame(empty, batch.calls.get(0).frame());
      Call c = fill();
      assertSame(tex, c.texture());
      float uSpan = c.u2() - c.u();
      float vSpan = c.v2() - c.v();
      boolean horizontal = d == BarFillDirection.LEFT_TO_RIGHT || d == BarFillDirection.RIGHT_TO_LEFT;
      assertEquals(horizontal ? 0.5f : 1f, Math.abs(uSpan), EPS, d.name());
      assertEquals(horizontal ? 1f : 0.5f, Math.abs(vSpan), EPS, d.name());
    }
    bar.setEmptyColor(FlixelColor.RED).setFilledColor(FlixelColor.BLUE);
    batch.calls.clear();
    bar.draw(batch);
    assertNull(batch.calls.get(0).texture());
    assertEquals(1f, fill().color().b, EPS);
  }

  @Test
  void gradientsReplaceTheFillColorAndFollowTheDirection() {
    bar.setValue(100f).setGradient(FlixelColor.RED, FlixelColor.BLUE);
    bar.draw(batch);
    Call c = fill();
    assertNotNull(c.texture());
    assertEquals(100, c.texture().getWidth());
    assertEquals(1f, c.color().g, EPS, "a gradient is drawn untinted");

    bar.setFillDirection(BarFillDirection.TOP_TO_BOTTOM);
    batch.calls.clear();
    bar.draw(batch);
    assertEquals(10, fill().texture().getHeight());
    bar.setFillDirection(BarFillDirection.BOTTOM_TO_TOP);

    bar.setGradient(null, FlixelColor.BLUE);
    batch.calls.clear();
    bar.draw(batch);
    assertNull(fill().texture(), "without both colors the plain fill comes back");
    bar.setGradient(null, null);
  }

  @Test
  void thresholdColorsBlendBetweenStops() {
    bar.setThresholdColors(FlixelColor.GREEN, FlixelColor.RED, 0.25f);
    bar.setValue(10f).draw(batch);
    assertEquals(1f, fill().color().r, EPS, "below the low stop the low color is used");

    batch.calls.clear();
    bar.setValue(62.5f).draw(batch);
    assertEquals(0.5f, fill().color().g, EPS, "half way between the stops is half way between the colors");

    FlixelArray<ThresholdStop> stops = new FlixelArray<>(ThresholdStop[]::new);
    stops.add(new ThresholdStop(0.5f, FlixelColor.BLUE));
    stops.add(null);
    stops.add(new ThresholdStop(2f, FlixelColor.WHITE));
    bar.setThresholdStops(stops);
    batch.calls.clear();
    bar.setValue(100f).draw(batch);
    assertEquals(1f, fill().color().r, EPS);

    bar.clearThresholds();
    batch.calls.clear();
    bar.draw(batch);
    assertEquals(1f, fill().color().g, EPS);
    assertEquals(0f, fill().color().r, EPS);

    bar.setThresholdStops(new FlixelArray<>(ThresholdStop[]::new));
    assertThrows(NullPointerException.class, () -> bar.setThresholdStops(null));
    assertThrows(NullPointerException.class, () -> bar.setThresholdColors(null, FlixelColor.RED, 0f));
    assertThrows(NullPointerException.class, () -> new ThresholdStop(0f, null));
  }

  @Test
  void thresholdSmoothingEasesTowardTheNewColor() {
    bar.setThresholdColors(FlixelColor.GREEN, FlixelColor.RED, 0f).setThresholdSmoothing(0.5f);
    bar.setValue(100f).draw(batch);
    batch.calls.clear();
    bar.setValue(0f);
    bar.update(1f / 60f);
    bar.setValue(1f).draw(batch);
    float r = fill().color().r;
    assertEquals(0.5f, r, 0.05f, "half of the change is applied in one frame");

    bar.setThresholdSmoothing(0.5f, false);
    batch.calls.clear();
    bar.setValue(100f).draw(batch);
    assertEquals(0.5f, fill().color().g, 0.3f, "smoothing also applies when the value rises");
  }

  @Test
  void lerpEasesTheDisplayedValue() {
    bar.setLerp(0.5f);
    assertEquals(0.5f, bar.getLerp());
    bar.setValue(100f);
    assertEquals(0f, bar.getDisplayedValue());
    bar.update(1f / 60f);
    assertEquals(50f, bar.getDisplayedValue(), 0.5f);
    bar.setLerp(5f);
    assertEquals(100f, bar.getDisplayedValue(), "a lerp of one or more jumps straight to the value");
  }

  @Test
  void trackAndMaxSuppliersAreReadOnUpdate() {
    float[] hp = { 30f };
    float[] maxHp = { 60f };
    bar.setTrack(() -> hp[0]).setMaxSupplier(() -> maxHp[0]);
    assertNotNull(bar.getTrack());
    assertNotNull(bar.getMaxSupplier());
    bar.update(0.016f);
    assertEquals(60f, bar.getMax());
    assertEquals(30f, bar.getValue());

    hp[0] = Float.POSITIVE_INFINITY;
    maxHp[0] = Float.NaN;
    bar.update(0.016f);
    assertEquals(60f, bar.getMax(), "a bad max is ignored");
    assertEquals(0f, bar.getValue(), "a bad value falls back to the minimum");
  }

  @Test
  void borderDrawsFourEdges() {
    bar.setBorder(FlixelColor.WHITE, 2f).draw(batch);
    assertEquals(5, batch.calls.getSize());
    bar.setBorder(null, 2f);
    batch.calls.clear();
    bar.draw(batch);
    assertEquals(1, batch.calls.getSize());
    bar.setBorder(FlixelColor.WHITE, -1f).clearBorder();
  }

  @Test
  void worldSpaceBarsMoveWithTheCamera() {
    camera.scrollX = 50f;
    bar.draw(batch);
    assertEquals(-40f, batch.calls.get(0).x(), EPS);
  }

  @Test
  void screenSpaceBarsStayPutWhenTheCameraScrolls() {
    camera.scrollX = 50f;
    camera.scrollY = 30f;
    bar.setScreenSpace(true).setText("HP");
    bar.draw(batch);
    assertEquals(10f, batch.calls.get(0).x(), EPS);
    assertEquals(20f, batch.calls.get(0).y(), EPS);
    assertEquals(0f, bar.text.getScrollX(), EPS, "the label is pinned with the bar");

    bar.setScreenSpace(false);
    bar.draw(batch);
    assertEquals(1f, bar.text.getScrollX(), EPS);
  }

  @Test
  void overlayTextIsCreatedAndCleared() {
    bar.setText("HP");
    assertNotNull(bar.text);
    bar.setTextOffset(1f, 2f);
    bar.update(0f);
    bar.update(0f);
    bar.draw(batch);
    bar.setText(null);
    assertNull(bar.text);
  }

  @Test
  void aBarWithAnEmptyRangeShowsNoFill() {
    bar.setRange(5f, 5f).draw(batch);
    assertEquals(1, batch.calls.getSize());
  }

  @Test
  void destroyReleasesTheGradientAndText() {
    bar.setGradient(FlixelColor.RED, FlixelColor.BLUE).setText("x");
    bar.destroy();
    assertNull(bar.text);
  }
}
