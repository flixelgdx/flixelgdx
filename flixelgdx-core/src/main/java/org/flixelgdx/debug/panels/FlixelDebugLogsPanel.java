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

import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugLogs;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugTable;
import org.flixelgdx.debug.ui.FlixelDebugTextBox;
import org.flixelgdx.debug.ui.FlixelDebugToggle;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.debug.ui.FlixelDebugWrap;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * The built-in "Logs" panel: the most recent lines of the {@linkplain FlixelDebugManager#logs log feed}, with level
 * filters, a text filter, auto-scroll, and the details of the selected line.
 *
 * <p>New lines are appended to the table as they arrive, so only the new ones travel to the app. Switching a level
 * filter rebuilds the table from the feed. Clicking a line shows its full message and stack trace below the table;
 * clicking it again hides them. A game can replace the panel under the id {@value FlixelDebugDefaultPanels#LOGS}.
 */
public class FlixelDebugLogsPanel extends FlixelDebugPanel {

  private long seenSeq;
  private long detailSeq = -1L;

  private final FlixelDebugLogs logs;
  private final FlixelDebugTable table = new FlixelDebugTable();
  private final FlixelDebugTextBox details = new FlixelDebugTextBox();
  private final FlixelString text = new FlixelString(32);
  private final FlixelString detailText = new FlixelString(256);
  private final FlixelDebugLogs.LineVisitor appender = this::append;
  private final FlixelDebugLogs.LineVisitor detailFinder = this::findDetail;
  private int seenClears;

  private boolean showDebug = true;
  private boolean showInfo = true;
  private boolean showWarn = true;
  private boolean showError = true;
  private boolean showUncaught = true;
  private boolean autoScroll = true;
  private boolean rebuild = true;

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose log feed the panel shows.
   */
  public FlixelDebugLogsPanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.LOGS, "Logs");
    this.logs = manager.logs;
    setDock(FlixelDebugProtocol.DOCK_LEFT);

    FlixelDebugTextBox filter = FlixelDebugUi.textBox("Filter").setWidth(180f).setName("filter");
    table.column("Time", 70f, FlixelDebugProtocol.COLUMN_RIGHT | FlixelDebugProtocol.COLUMN_MONO)
        .column("Level", 52f, 0)
        .column("Tag", 120f, 0)
        .column("Message", 0f, FlixelDebugProtocol.COLUMN_MONO)
        .column("Where", 130f, FlixelDebugProtocol.COLUMN_MONO)
        .setLimit(FlixelDebugLogs.MAX_LINES)
        .setFollow(true)
        .setFilter(filter)
        .setGrow(1f)
        .setName("lines");
    table.onSelect(this::showDetails);
    details.setReadOnly(true).setMultiline(true).setHeight(104f).setVisible(false).setName("details")
        .setTooltip("Click the line again to hide its details.");

    FlixelDebugWrap toolbar = FlixelDebugUi.wrap(
        levelToggle("Debug", 0),
        levelToggle("Info", 1),
        levelToggle("Warn", 2),
        levelToggle("Error", 3),
        levelToggle("Uncaught", 4),
        new FlixelDebugToggle("Auto-scroll", () -> autoScroll, on -> {
          autoScroll = on;
          table.setFollow(on);
        }).setName("autoscroll"),
        new FlixelDebugButton("Clear", logs::clear).setName("clear"),
        filter);
    setContent(FlixelDebugUi.column(toolbar, table, details));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    int clears = logs.getClears();
    if (clears != seenClears) {
      seenClears = clears;
      rebuild = true;
    }
    if (rebuild) {
      rebuild = false;
      table.clearRows();
      hideDetails();
      seenSeq = 0L;
    }
    seenSeq = logs.visitSince(seenSeq, appender);
  }

  @Override
  protected void onOpen() {
    rebuild = true;
  }

  private FlixelDebugToggle levelToggle(String name, int which) {
    return new FlixelDebugToggle(name, () -> isShown(which), on -> {
      if (isShown(which) != on) {
        setShown(which, on);
        rebuild = true;
      }
    }).setName("show." + name.toLowerCase(Locale.ROOT));
  }

  private boolean isShown(int which) {
    return switch (which) {
      case 0 -> showDebug;
      case 1 -> showInfo;
      case 2 -> showWarn;
      case 3 -> showError;
      default -> showUncaught;
    };
  }

  private void setShown(int which, boolean on) {
    switch (which) {
      case 0 -> showDebug = on;
      case 1 -> showInfo = on;
      case 2 -> showWarn = on;
      case 3 -> showError = on;
      default -> showUncaught = on;
    }
  }

  private boolean passes(int level, boolean uncaught) {
    if (uncaught) {
      return showUncaught;
    }
    return switch (level) {
      case FlixelDebugProtocol.LEVEL_DEBUG -> showDebug;
      case FlixelDebugProtocol.LEVEL_INFO -> showInfo;
      case FlixelDebugProtocol.LEVEL_WARN -> showWarn;
      default -> showError;
    };
  }

  private void append(long seq, double time, int level, boolean uncaught, CharSequence tag, CharSequence callSite,
      CharSequence message, CharSequence stack) {
    if (!passes(level, uncaught)) {
      return;
    }
    int style = switch (level) {
      case FlixelDebugProtocol.LEVEL_DEBUG -> FlixelDebugProtocol.TONE_MUTED;
      case FlixelDebugProtocol.LEVEL_WARN -> FlixelDebugProtocol.TONE_WARNING;
      case FlixelDebugProtocol.LEVEL_ERROR -> FlixelDebugProtocol.TONE_ERROR;
      default -> FlixelDebugProtocol.TONE_NORMAL;
    };
    if (uncaught) {
      style = FlixelDebugProtocol.TONE_ERROR | FlixelDebugProtocol.STYLE_BOLD;
    }
    text.setLength(0).concatFloatRounded((float) time, 2);
    table.appendRow(seq, style)
        .cell(text, time)
        .cell(levelName(level))
        .cell(tag)
        .cell(message)
        .cell(callSite)
        .detail(stack);
  }

  private void showDetails(long seq) {
    if (seq < 0L || seq == detailSeq) {
      // Clicking the shown line again hides its details.
      hideDetails();
      table.setSelected(-1L);
      return;
    }
    detailSeq = seq;
    detailText.clear();
    logs.visitSince(seq - 1L, detailFinder);
    details.setText(detailText);
    details.setVisible(true);
  }

  private void hideDetails() {
    detailSeq = -1L;
    details.setVisible(false);
    details.setText("");
  }

  private void findDetail(long seq, double time, int level, boolean uncaught, CharSequence tag,
      CharSequence callSite, CharSequence message, CharSequence stack) {
    if (seq != detailSeq) {
      return;
    }
    detailText.concat(message);
    if (callSite.length() > 0) {
      detailText.concat("\n    at ").concat(callSite);
    }
    if (stack.length() > 0) {
      detailText.concat('\n').concat(stack);
    }
  }

  private static String levelName(int level) {
    return switch (level) {
      case FlixelDebugProtocol.LEVEL_DEBUG -> "Debug";
      case FlixelDebugProtocol.LEVEL_INFO -> "Info";
      case FlixelDebugProtocol.LEVEL_WARN -> "Warn";
      default -> "Error";
    };
  }
}
