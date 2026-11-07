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
package org.flixelgdx.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.panels.FlixelDebugConsolePanel;
import org.flixelgdx.debug.panels.FlixelDebugPanelBase;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugConsolePanelTest {

  private FlixelLogger originalLog;
  private FlixelDebugManager manager;
  private FlixelDebugConsolePanel console;
  private List<String> lines;
  private List<FlixelLogLevel> levels;
  private String[] lastArgs;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger log = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    log.setConsoleSink(null);
    lines = new ArrayList<>();
    levels = new ArrayList<>();
    log.addSink(entry -> {
      lines.add(entry.getMessage().toString());
      levels.add(entry.getLevel());
    });
    Flixel.log = log;
    manager = new FlixelDebugManager();
    console = manager.console;
    console.register("echo", args -> lastArgs = args.getRawArgs());
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
  }

  @Test
  void quotesGroupWordsAndEmptyQuotesMakeAnEmptyArgument() {
    assertTrue(console.execute("echo \"hello world\" \"\" plain   \"a  b\""));
    assertArrayEquals(new String[] { "hello world", "", "plain", "a  b" }, lastArgs);
  }

  @Test
  void anUnterminatedQuoteTakesTheRestOfTheLine() {
    console.execute("echo one \"two three");
    assertArrayEquals(new String[] { "one", "two three" }, lastArgs);
  }

  @Test
  void commandNamesIgnoreCase() {
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_RAN, console.run("ECHO x"));
    assertArrayEquals(new String[] { "x" }, lastArgs);
    assertTrue(console.has("EcHo"));
  }

  @Test
  void registeringAgainReplacesTheCommand() {
    String[] seen = new String[1];
    console.register("Echo", args -> seen[0] = "second");
    console.run("echo");
    assertEquals("second", seen[0]);
    assertEquals(1, countNamed("echo"));
  }

  @Test
  void nameRules() {
    console.register("a1.b2", args -> {
    });
    console.register("watch.mouse2", args -> {
    });
    for (String bad : new String[] { "", "1abc", "a-b", "a b", "_x", ".x", "café" }) {
      assertThrows(IllegalArgumentException.class, () -> console.register(bad, args -> {
      }), "'" + bad + "'");
    }
    assertThrows(IllegalArgumentException.class, () -> console.register(null, args -> {
    }));
  }

  @ParameterizedTest
  @ValueSource(strings = { "true", "on", "yes", "1", "TRUE", "On", "YES" })
  void booleanTrueWords(String word) {
    FlixelDebugCommandArgs args = new FlixelDebugCommandArgs(new String[] { word });
    assertTrue(args.getBoolean(0, false));
  }

  @ParameterizedTest
  @ValueSource(strings = { "false", "off", "no", "0", "FALSE", "Off", "NO" })
  void booleanFalseWords(String word) {
    FlixelDebugCommandArgs args = new FlixelDebugCommandArgs(new String[] { word });
    assertFalse(args.getBoolean(0, true));
  }

  @Test
  void booleanFallsBackToTheDefault() {
    assertTrue(new FlixelDebugCommandArgs(new String[] { "maybe" }).getBoolean(0, true));
    assertFalse(new FlixelDebugCommandArgs(new String[0]).getBoolean(0, false));
  }

  @Test
  void historyKeepsLinesInOrderAndSkipsARepeatOfTheLastOne() {
    console.run("echo 1");
    console.run("echo 1");
    console.run("  echo 2  ");
    console.run("echo 1");
    console.run("");
    console.run("   ");
    FlixelArray<String> history = (FlixelArray<String>) console.getHistory();
    assertEquals(3, history.getSize());
    assertEquals("echo 1", history.get(0));
    assertEquals("echo 2", history.get(1));
    assertEquals("echo 1", history.get(2));
  }

  @Test
  void historyKeepsOnlyTheNewestLines() {
    int max = FlixelDebugConsolePanel.MAX_HISTORY_ENTRIES;
    int total = max + 6;
    for (int i = 0; i < total; i++) {
      console.run("echo " + i);
    }
    FlixelArray<String> history = (FlixelArray<String>) console.getHistory();
    assertEquals(max, history.getSize());
    assertEquals("echo 6", history.get(0));
    assertEquals("echo " + (total - 1), history.get(max - 1));
  }

  @Test
  void unknownCommandsAreRememberedInTheHistory() {
    console.run("nothing here");
    assertEquals(1, ((FlixelArray<String>) console.getHistory()).getSize());
  }

  @Test
  void aLineOf500CharactersIsAcceptedAndOneOf501IsRejected() {
    int[] runs = { 0 };
    console.register("pad", args -> runs[0]++);
    String ok = "pad " + "x".repeat(496);
    assertEquals(500, ok.length());
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_RAN, console.run(ok));
    assertEquals(1, runs[0]);

    int historyBefore = ((FlixelArray<String>) console.getHistory()).getSize();
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_FAILED, console.run(ok + "x"));
    assertEquals(1, runs[0], "the handler must not run for a rejected line");
    assertEquals(historyBefore, ((FlixelArray<String>) console.getHistory()).getSize());
    assertFalse(console.getResultMessage().isEmpty());
  }

  @Test
  void ranResult() {
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_RAN, console.run("echo hi"));
    assertEquals("", console.getResultMessage());
    assertTrue(console.hasPendingResult());
  }

  @Test
  void failedResultLogsTheNameAndTheMessage() {
    console.register("boom", args -> {
      throw new IllegalStateException("kaboom");
    });
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_FAILED, console.run("boom now"));
    assertEquals("kaboom", console.getResultMessage());
    assertTrue(lines.contains("boom failed: kaboom"), lines.toString());
    assertEquals(FlixelLogLevel.ERROR, levels.get(lines.indexOf("boom failed: kaboom")));
  }

  @Test
  void aThrowWithoutAMessageUsesTheClassName() {
    console.register("npe", args -> {
      throw new IllegalStateException();
    });
    console.run("npe");
    assertEquals(IllegalStateException.class.getName(), console.getResultMessage());
  }

  @Test
  void unknownResultWarns() {
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_UNKNOWN, console.run("frobnicate 1 2"));
    String expected = "Unknown command \"frobnicate\". Type \"help\" to list commands.";
    assertTrue(lines.contains(expected), lines.toString());
    assertEquals(FlixelLogLevel.WARN, levels.get(lines.indexOf(expected)));
  }

  @Test
  void blankLinesAreIgnored() {
    assertEquals(FlixelDebugConsolePanel.RESULT_IGNORED, console.run("   "));
    assertEquals(FlixelDebugConsolePanel.RESULT_IGNORED, console.run(null));
    assertFalse(console.hasPendingResult());
  }

  @Test
  void helpListsEveryCommandWithUsageAndDescription() {
    console.run("help");
    assertTrue(lines.contains("pause [on|off]  -  Pauses or resumes the game."), lines.toString());
    assertTrue(lines.contains("help [filter]  -  Lists the commands, or only those whose name starts with filter."),
        lines.toString());
    assertTrue(lines.contains("echo"), "a command with no usage or description shows just its name");
  }

  @Test
  void helpFiltersByNamePrefixIgnoringCase() {
    console.register("zeta", "zeta <n>", "Does zeta things.", args -> {
    });
    lines.clear();
    console.run("help ZE");
    assertEquals(List.of("zeta <n>  -  Does zeta things."), lines);

    lines.clear();
    console.run("help watch.");
    assertEquals(3, lines.size(), lines.toString());
    for (String line : lines) {
      assertTrue(line.startsWith("watch."), line);
    }

    lines.clear();
    console.run("help qqq");
    assertEquals(List.of("No commands start with \"qqq\"."), lines);
  }

  @Test
  void builtInCommandsExist() {
    for (String name : new String[] { "help", "pause", "hitboxes", "step", "rate", "loglevel", "watch.mouse",
        "watch.camera", "watch.clear", "resetState" }) {
      assertTrue(console.has(name), name);
    }
    assertFalse(console.has("hide"));
  }

  @Test
  void rateCommandClamps() {
    console.run("rate 99");
    assertEquals(30, manager.controls.getRate());
    console.run("rate 0");
    assertEquals(1, manager.controls.getRate());
    console.run("rate 12");
    assertEquals(12, manager.controls.getRate());
  }

  @Test
  void logLevelCommandSetsAndRestoresTheLevel() {
    FlixelLogLevel before = Flixel.log.getLevel();
    console.run("loglevel Warn");
    assertEquals(FlixelLogLevel.WARN, Flixel.log.getLevel());
    assertEquals(FlixelDebugProtocol.LEVEL_WARN, manager.controls.getLogLevel());
    console.run("loglevel default");
    assertEquals(before, Flixel.log.getLevel());
    assertEquals(FlixelDebugProtocol.LEVEL_DEFAULT, manager.controls.getLogLevel());
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_FAILED, console.run("loglevel loud"));
  }

  @Test
  void watchCommandsToggleTheBuiltInWatches() {
    console.run("watch.mouse");
    assertTrue(manager.watches.isMouseWatched());
    console.run("watch.mouse off");
    assertFalse(manager.watches.isMouseWatched());
    console.run("watch.camera on");
    assertTrue(manager.watches.isCameraWatched());
    console.run("watch.clear");
    assertEquals(0, manager.watches.getCount());
  }

  @Test
  void stepWarnsWhenTheGameIsNotPaused() {
    console.run("step");
    assertTrue(lines.contains("The game must be paused to step a frame."), lines.toString());
  }

  @Test
  void unregisterRemovesACommand() {
    console.unregister("ECHO");
    assertFalse(console.has("echo"));
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_UNKNOWN, console.run("echo"));
  }

  @Test
  void commandsPayloadListsEveryCommandSortedByName() {
    long before = console.getRevision();
    console.register("aaa", "aaa <x>", "First.", args -> {
    });
    assertTrue(console.getRevision() > before);

    FlixelDebugMessageReader r = DebugTestSupport.payload(console);
    assertEquals(FlixelDebugProtocol.CONSOLE_COMMANDS, r.readU8());
    int count = r.readCount();
    assertEquals(console.getNames().getSize(), count);
    String previous = "";
    boolean sawAaa = false;
    boolean sawPause = false;
    for (int i = 0; i < count; i++) {
      String name = DebugTestSupport.str(r);
      String usage = DebugTestSupport.str(r);
      String description = DebugTestSupport.str(r);
      assertTrue(previous.compareToIgnoreCase(name) <= 0, previous + " before " + name);
      previous = name;
      if (name.equals("aaa")) {
        sawAaa = true;
        assertEquals("aaa <x>", usage);
        assertEquals("First.", description);
      }
      if (name.equals("pause")) {
        sawPause = true;
        assertEquals("pause [on|off]", usage);
      }
      if (name.equals("echo")) {
        assertEquals("echo", usage, "a missing usage falls back to the name");
        assertEquals("", description);
      }
    }
    assertTrue(sawAaa && sawPause);
    assertEquals(0, r.getRemaining());
  }

  @Test
  void resultPayloadCarriesStatusAndMessage() {
    console.register("boom", args -> {
      throw new IllegalStateException("kaboom");
    });
    console.run("boom");
    FlixelDebugMessageReader r = DebugTestSupport.payload(new ResultView(console));
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT, r.readU8());
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_FAILED, r.readU8());
    assertEquals("kaboom", DebugTestSupport.str(r));
    assertEquals(0, r.getRemaining());
    assertFalse(console.hasPendingResult());

    console.run("nope");
    r = DebugTestSupport.payload(new ResultView(console));
    r.readU8();
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_UNKNOWN, r.readU8());
    assertEquals("Unknown command \"nope\".", DebugTestSupport.str(r));

    console.run("echo ok");
    r = DebugTestSupport.payload(new ResultView(console));
    r.readU8();
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_RAN, r.readU8());
    assertEquals("", DebugTestSupport.str(r));
  }

  @Test
  void theRunActionExecutesTheLine() {
    DebugTestSupport.send(console, FlixelDebugProtocol.CONSOLE_RUN, w -> w.writeString("echo \"a b\" c"));
    assertArrayEquals(new String[] { "a b", "c" }, lastArgs);
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_RAN, console.getResultStatus());
  }

  @Test
  void theRunActionRejectsALongLine() {
    DebugTestSupport.send(console, FlixelDebugProtocol.CONSOLE_RUN, w -> w.writeString("echo " + "y".repeat(600)));
    assertNull(lastArgs, "a rejected line must not reach the handler");
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_FAILED, console.getResultStatus());
  }

  @Test
  void unknownActionCodesAreIgnored() {
    lastArgs = null;
    DebugTestSupport.send(console, 99, w -> w.writeString("echo hi"));
    assertNull(lastArgs);
    assertFalse(console.hasPendingResult());
  }

  private int countNamed(String name) {
    int n = 0;
    FlixelArray<String> names = console.getNames();
    for (int i = 0; i < names.getSize(); i++) {
      if (names.get(i).equalsIgnoreCase(name)) {
        n++;
      }
    }
    return n;
  }

  /** Makes {@link FlixelDebugConsolePanel#writeResult} usable through the generic test helper. */
  private static final class ResultView extends FlixelDebugPanelBase {

    private final FlixelDebugConsolePanel console;

    ResultView(FlixelDebugConsolePanel console) {
      super("view", "View", FlixelDebugProtocol.KIND_CONSOLE);
      this.console = console;
    }

    @Override
    public void write(@NotNull FlixelDebugMessageWriter writer) {
      console.writeResult(writer);
    }
  }
}
