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
package org.flixelgdx.debug.panels;

import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugControls;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugGrid;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugScroll;
import org.flixelgdx.debug.ui.FlixelDebugSlider;
import org.flixelgdx.debug.ui.FlixelDebugToggle;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.debug.ui.FlixelDebugWrap;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The built-in "Controls" panel: pause, step, hitboxes, the free camera, the stats sample rate, the log level, and
 * the game's debugger keys.
 *
 * <p>Every control reads and writes the {@linkplain FlixelDebugManager#controls controls feed}, so pressing the pause
 * hotkey in the game ticks the box here too. A game can replace the panel under the id
 * {@value FlixelDebugDefaultPanels#CONTROLS}.
 */
public class FlixelDebugControlsPanel extends FlixelDebugPanel {

  private static final String[] LEVEL_NAMES = { "Default", "Debug", "Info", "Warn", "Error" };
  private static final int[] LEVEL_CODES = {
      FlixelDebugProtocol.LEVEL_DEFAULT,
      FlixelDebugProtocol.LEVEL_DEBUG,
      FlixelDebugProtocol.LEVEL_INFO,
      FlixelDebugProtocol.LEVEL_WARN,
      FlixelDebugProtocol.LEVEL_ERROR,
  };

  private final FlixelDebugControls controls;
  private final FlixelArray<String> keys = new FlixelArray<>();
  private final FlixelArray<String> descriptions = new FlixelArray<>();
  private final FlixelArray<String> shownKeys = new FlixelArray<>();
  private final FlixelDebugGrid keybinds = new FlixelDebugGrid("2*", "3*").setSpacing(2f);
  private final FlixelDebugLabel rateText = new FlixelDebugLabel().setStyle(FlixelDebugProtocol.STYLE_MONO);
  private final FlixelString text = new FlixelString(8);

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose controls feed the panel drives.
   */
  public FlixelDebugControlsPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.CONTROLS, "Controls");
    this.controls = manager.controls;
    setDock(FlixelDebugProtocol.DOCK_RIGHT);

    FlixelDebugSlider rate = FlixelDebugUi.slider(FlixelDebugControls.MIN_RATE, FlixelDebugControls.MAX_RATE,
        controls::getRate, v -> controls.setRate((int) Math.round(v)));
    rate.setStep(1.0).setGrow(1f).setName("rate");

    FlixelDebugWrap levels = new FlixelDebugWrap();
    for (int i = 0; i < LEVEL_NAMES.length; i++) {
      int code = LEVEL_CODES[i];
      levels.add(new FlixelDebugToggle(LEVEL_NAMES[i], () -> controls.getLogLevel() == code,
          on -> controls.setLogLevel(code)).setName("level." + LEVEL_NAMES[i].toLowerCase(Locale.ROOT)));
    }

    setContent(new FlixelDebugScroll(FlixelDebugUi.column(
        FlixelDebugUi.checkbox("Show hitboxes", controls::isHitboxes, controls::setHitboxes).setName("hitboxes"),
        FlixelDebugUi.checkbox("Pause", controls::isPaused, controls::setPaused).setName("pause"),
        FlixelDebugUi.checkbox("Free camera", controls::isFreeCam, controls::setFreeCam).setName("freecam"),
        new FlixelDebugButton("Step frame", controls::stepFrame).setName("step")
            .setTooltip("Runs one frame while the game is paused"),
        FlixelDebugUi.heading("Update rate"),
        FlixelDebugUi.row(rate, rateText,
            FlixelDebugUi.button("Reset", () -> controls.setRate(FlixelDebugControls.DEFAULT_RATE))),
        FlixelDebugUi.heading("Log level"),
        levels,
        FlixelDebugUi.heading("Keybinds in the game"),
        keybinds,
        FlixelDebugUi.button("Reset layout", () -> {
          if (getOwner() != null) {
            getOwner().resetLayout();
          }
        }).setTooltip("Puts every panel of the debugger app back in its first place"))));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    rateText.setText(text.setLength(0).concat(controls.getRate()).concat(" Hz"));
    controls.getKeybinds(keys, descriptions);
    if (!sameKeys()) {
      rebuildKeybinds();
    }
  }

  private boolean sameKeys() {
    if (keys.getSize() != shownKeys.getSize()) {
      return false;
    }
    for (int i = 0, n = keys.getSize(); i < n; i++) {
      if (keys.get(i) != shownKeys.get(i)) {
        return false;
      }
    }
    return true;
  }

  private void rebuildKeybinds() {
    keybinds.clear();
    shownKeys.clear();
    for (int i = 0, n = keys.getSize(); i < n; i++) {
      shownKeys.add(keys.get(i));
      keybinds.add(new FlixelDebugLabel(keys.get(i)).setStyle(FlixelDebugProtocol.STYLE_MONO),
          new FlixelDebugLabel(descriptions.get(i)).setWrap(true));
    }
  }
}
