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

import org.flixelgdx.Flixel;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugConsole;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugTextBox;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

/**
 * The built-in "Console" panel: a command line for the commands of {@code Flixel.debug.console}.
 *
 * <p>The text box knows every command's name, usage, and description, so the app suggests and completes command
 * names with Tab and brings earlier lines back with Up and Down without asking the game. Pressing Enter (or Run) sends
 * the line to the game, which echoes it into the log as {@code > line} and runs it. What a command prints shows up in
 * the logs panel. A game can replace the panel under the id {@value FlixelDebugDefaultPanels#CONSOLE}.
 */
public class FlixelDebugConsolePanel extends FlixelDebugPanel {

  /** The hint shown above the command line. */
  public static final String HINT = "Type a command and press Enter. Tab completes a name, Up and Down bring back"
      + " earlier lines. Type help to list the commands.";

  /** How many submitted lines Up and Down bring back. */
  public static final int HISTORY = 64;

  private long seenRevision = -1L;

  private final FlixelDebugConsole console;
  private final FlixelDebugTextBox input = new FlixelDebugTextBox();
  private final FlixelLogger log = Flixel.log.tagged("Console");

  /**
   * Creates the panel.
   *
   * @param manager The debug manager whose console feed runs the commands.
   */
  public FlixelDebugConsolePanel(@NotNull FlixelDebugManager manager) {
    super(FlixelDebugDefaultPanels.CONSOLE, "Console");
    this.console = manager.console;
    setDock(FlixelDebugProtocol.DOCK_BOTTOM);
    input.setPlaceholder("Command")
        .setHistory(HISTORY)
        .setClearOnSubmit(true)
        .setLive(true)
        .setStyle(FlixelDebugProtocol.STYLE_MONO)
        .setGrow(1f)
        .setName("command")
        .onSubmit(this::run);
    FlixelDebugButton runButton = new FlixelDebugButton("Run", () -> {
      run(input.getText());
      input.setText("");
    }).setName("run");
    setContent(FlixelDebugUi.column(
        FlixelDebugUi.hint(HINT),
        FlixelDebugUi.row(input, runButton)));
  }

  @Override
  public void update(float elapsed) {
    super.update(elapsed);
    long revision = console.getRevision();
    if (revision == seenRevision) {
      return;
    }
    seenRevision = revision;
    FlixelDebugMessageWriter writer = input.beginCompletions();
    int count = console.getCommandCount();
    for (int i = 0; i < count; i++) {
      writer.beginList()
          .writeValue(console.getCommandName(i))
          .writeValue(console.getCommandUsage(i))
          .writeValue(console.getCommandDescription(i))
          .endList(3);
    }
    input.endCompletions(count);
  }

  @Override
  protected void onOpen() {
    seenRevision = -1L;
  }

  private void run(CharSequence line) {
    if (line == null || line.length() == 0) {
      return;
    }
    log.info("> {}", line);
    console.run(line);
  }
}
