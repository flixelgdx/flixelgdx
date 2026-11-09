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
package org.flixelgdx.debug.ui;

import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Lines that show how numbers changed over time, such as the frame rate or memory use.
 *
 * <p>A graph is a heart monitor for the game: every time you {@linkplain #push(float) push} a sample, the lines move
 * one step to the left and the newest value appears on the right. Only new samples travel to the app. The graph keeps
 * the last {@linkplain #setLimit(int) limit} samples, so a person who opens the panel later still sees the recent
 * history.
 *
 * <p>A graph can draw several series at once (for example frame time and CPU time). Each series has a name, a color,
 * and a unit, and every sample has one value per series:
 *
 * <pre>{@code
 * FlixelDebugGraph frame = new FlixelDebugGraph("Frame time")
 *     .series("Frame", 0, "ms")
 *     .series("CPU", 0, "ms");
 * frame.push(frameMs, cpuMs);
 * }</pre>
 *
 * <p>The vertical axis fits itself to the data unless you {@linkplain #setRange(double, double) fix it}.
 */
public class FlixelDebugGraph extends FlixelDebugWidget<FlixelDebugGraph> {

  /** How many samples a graph keeps when no limit is set. */
  public static final int DEFAULT_LIMIT = 120;

  private double min = Double.NaN;
  private double max = Double.NaN;
  private final FlixelString title = new FlixelString(16);
  private final FlixelDebugListBuffer series = new FlixelDebugListBuffer();
  private final FlixelDebugMessageWriter seriesScratch = new FlixelDebugMessageWriter(64);
  private float[] samples = new float[DEFAULT_LIMIT];
  private int seriesCount;
  private int limit = DEFAULT_LIMIT;
  private int head;
  private int count;
  private int pending;

  /**
   * Creates a graph with no series.
   *
   * @param title The title drawn above the lines.
   */
  public FlixelDebugGraph(@NotNull CharSequence title) {
    super(FlixelDebugProtocol.WIDGET_GRAPH);
    this.title.set(title);
  }

  /**
   * Adds a series. Add every series before the first sample.
   *
   * @param name The series' name.
   * @param color The color as {@code 0xAARRGGBB}, or {@code 0} to let the app pick one.
   * @param unit The unit shown after the latest value, such as {@code "ms"}, or empty.
   * @return This graph, for chaining.
   */
  @NotNull
  public FlixelDebugGraph series(@NotNull String name, int color, @NotNull String unit) {
    seriesScratch.beginList().writeValue(name).writeValue(color).writeValue(unit).endList(3);
    seriesCount++;
    FlixelDebugMessageWriter writer = series.begin();
    writer.writeRaw(seriesScratch);
    series.end(seriesCount);
    samples = new float[limit * seriesCount];
    head = 0;
    count = 0;
    pending = 0;
    markDirty(FlixelDebugProtocol.PROP_SERIES);
    markStructureChanged();
    return this;
  }

  /**
   * Adds a sample to a graph with one series.
   *
   * @param a The value.
   */
  public void push(float a) {
    int base = beginSample();
    set(base, 0, a);
    endSample();
  }

  /**
   * Adds a sample to a graph with two series.
   *
   * @param a The value of the first series.
   * @param b The value of the second series.
   */
  public void push(float a, float b) {
    int base = beginSample();
    set(base, 0, a);
    set(base, 1, b);
    endSample();
  }

  /**
   * Adds a sample to a graph with three series.
   *
   * @param a The value of the first series.
   * @param b The value of the second series.
   * @param c The value of the third series.
   */
  public void push(float a, float b, float c) {
    int base = beginSample();
    set(base, 0, a);
    set(base, 1, b);
    set(base, 2, c);
    endSample();
  }

  /**
   * Adds a sample with one value per series, read from an array. Use it for graphs with more than three series.
   *
   * @param values The values, in series order. Missing values count as 0.
   */
  public void push(@NotNull float[] values) {
    int base = beginSample();
    for (int i = 0; i < seriesCount; i++) {
      set(base, i, i < values.length ? values[i] : 0f);
    }
    endSample();
  }

  /** Removes every sample. */
  public void clear() {
    head = 0;
    count = 0;
    pending = 0;
    markDirty(FlixelDebugProtocol.PROP_SAMPLES);
  }

  @Override
  protected long getPropMask() {
    long mask = super.getPropMask() | bit(FlixelDebugProtocol.PROP_TEXT) | bit(FlixelDebugProtocol.PROP_SERIES)
        | bit(FlixelDebugProtocol.PROP_SAMPLES);
    if (limit != DEFAULT_LIMIT) {
      mask |= bit(FlixelDebugProtocol.PROP_LIMIT);
    }
    if (!Double.isNaN(min)) {
      mask |= bit(FlixelDebugProtocol.PROP_MIN);
    }
    if (!Double.isNaN(max)) {
      mask |= bit(FlixelDebugProtocol.PROP_MAX);
    }
    return mask;
  }

  @Override
  protected void writeProp(@NotNull FlixelDebugMessageWriter writer, int key) {
    switch (key) {
      case FlixelDebugProtocol.PROP_TEXT -> writer.writeValue(title);
      case FlixelDebugProtocol.PROP_SERIES -> series.write(writer);
      case FlixelDebugProtocol.PROP_SAMPLES -> {
        writeSamples(writer, count);
        pending = 0;
      }
      case FlixelDebugProtocol.PROP_LIMIT -> writer.writeValue(limit);
      case FlixelDebugProtocol.PROP_MIN -> writer.writeValue(min);
      case FlixelDebugProtocol.PROP_MAX -> writer.writeValue(max);
      default -> super.writeProp(writer, key);
    }
  }

  @Override
  protected boolean hasPending() {
    return pending > 0;
  }

  @Override
  protected int writeAppends(@NotNull FlixelDebugMessageWriter writer) {
    if (pending == 0) {
      return 0;
    }
    writer.writeI32(getId()).writeU8(FlixelDebugProtocol.PATCH_APPEND).writeU8(FlixelDebugProtocol.PROP_SAMPLES);
    writeSamples(writer, pending);
    pending = 0;
    return 1;
  }

  @Override
  protected void clearPending() {
    pending = 0;
  }

  private int beginSample() {
    if (seriesCount == 0) {
      series("Value", 0, "");
    }
    return head * seriesCount;
  }

  private void set(int base, int series, float value) {
    if (series < seriesCount) {
      samples[base + series] = value;
    }
  }

  private void endSample() {
    head = (head + 1) % limit;
    if (count < limit) {
      count++;
    }
    if (isDirty(FlixelDebugProtocol.PROP_SAMPLES)) {
      // The whole history is sent anyway.
      return;
    }
    if (pending < limit) {
      pending++;
    }
    markPending();
  }

  /** Writes the newest {@code n} samples, oldest first, as a list of lists. */
  private void writeSamples(FlixelDebugMessageWriter writer, int n) {
    writer.beginList();
    int start = (head - n + limit) % limit;
    for (int i = 0; i < n; i++) {
      int base = ((start + i) % limit) * seriesCount;
      writer.beginList();
      for (int s = 0; s < seriesCount; s++) {
        writer.writeValue(samples[base + s]);
      }
      writer.endList(seriesCount);
    }
    writer.endList(n);
  }

  @NotNull
  public CharSequence getTitle() {
    return title;
  }

  /**
   * Sets the title above the graph. The text is copied, so a reused buffer is safe to pass.
   *
   * @param title The new title, or {@code null} for none.
   * @return This graph, for chaining.
   */
  @NotNull
  public FlixelDebugGraph setTitle(@Nullable CharSequence title) {
    if (copyText(this.title, title)) {
      markDirty(FlixelDebugProtocol.PROP_TEXT);
    }
    return this;
  }

  /**
   * Returns the newest value of a series.
   *
   * @param seriesIndex The position of the series.
   * @return The value, or {@code 0} before the first sample.
   */
  public float getLatest(int seriesIndex) {
    if (count == 0 || seriesIndex < 0 || seriesIndex >= seriesCount) {
      return 0f;
    }
    int last = (head - 1 + limit) % limit;
    return samples[last * seriesCount + seriesIndex];
  }

  /**
   * Returns how many samples the graph holds.
   *
   * @return The count, at most the limit.
   */
  public int getSampleCount() {
    return count;
  }

  public int getLimit() {
    return limit;
  }

  /**
   * Sets how many samples the graph keeps. Changing it removes the samples it holds.
   *
   * @param limit The most samples, at least 2.
   * @return This graph, for chaining.
   */
  @NotNull
  public FlixelDebugGraph setLimit(int limit) {
    int value = Math.max(2, limit);
    if (this.limit != value) {
      this.limit = value;
      samples = new float[value * Math.max(1, seriesCount)];
      clear();
      markDirty(FlixelDebugProtocol.PROP_LIMIT);
    }
    return this;
  }

  /**
   * Fixes the range of the vertical axis. Pass {@code Double.NaN} for an end that should fit the data.
   *
   * @param min The bottom of the axis.
   * @param max The top of the axis.
   * @return This graph, for chaining.
   */
  @NotNull
  public FlixelDebugGraph setRange(double min, double max) {
    if (Double.compare(this.min, min) != 0) {
      this.min = min;
      markDirty(FlixelDebugProtocol.PROP_MIN);
    }
    if (Double.compare(this.max, max) != 0) {
      this.max = max;
      markDirty(FlixelDebugProtocol.PROP_MAX);
    }
    return this;
  }
}
