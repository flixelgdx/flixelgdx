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
package org.flixelgdx.backend.html5.audio;

import org.flixelgdx.audio.FlixelBandPassEffect;
import org.jetbrains.annotations.NotNull;
import org.teavm.jso.webaudio.BiquadFilterNode;

/**
 * Web Audio band-pass filter backed by a {@code BiquadFilterNode} of type {@code "bandpass"}.
 *
 * <p>Frequencies outside the band around the cutoff are attenuated. The bandwidth is controlled by
 * the Q factor: higher Q produces a narrower resonant band. Both cutoff and Q update the matching
 * {@code AudioParam}s immediately on change.
 */
class FlixelWebAudioBandPassEffect extends FlixelWebAudioBiquadEffect implements FlixelBandPassEffect {

  private double q;

  FlixelWebAudioBandPassEffect(@NotNull BiquadFilterNode filter, double cutoffHz, double q) {
    super(filter, BiquadFilterNode.TYPE_BAND_PASS, cutoffHz);
    this.q = q;
    filter.getQ().setValue((float) q);
  }

  @Override
  public double getCutoff() {
    return cutoff();
  }

  @Override
  public double getQ() {
    return q;
  }

  @Override
  public void setCutoff(double hz) {
    applyCutoff(hz);
  }

  @Override
  public void setQ(double qv) {
    q = qv;
    filter().getQ().setValue((float) qv);
  }

  @Override
  public void setParam(int paramId, float value) {
    if (paramId == 0) {
      applyCutoff(value);
    } else if (paramId == 1) {
      setQ(value);
    }
  }

  @Override
  public float getParam(int paramId) {
    if (paramId == 0) {
      return (float) cutoff();
    }
    if (paramId == 1) {
      return (float) q;
    }
    return 0f;
  }
}
