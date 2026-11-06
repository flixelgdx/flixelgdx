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
import org.flixelgdx.collections.FlixelList;
import org.flixelgdx.debug.remote.FlixelDebugMessageReader;
import org.flixelgdx.debug.remote.FlixelDebugMessageWriter;
import org.flixelgdx.debug.remote.FlixelDebugProtocol;
import org.flixelgdx.logging.FlixelLogLevel;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The console of the remote debugger: a command line where you type a command name followed by
 * arguments, and the game runs the matching handler.
 *
 * <p>Think of it as a restaurant's order window. You hand in a name ("hello") and a handler that knows
 * what to do. Later anyone can shout that name through the window (from the debugger app or from code) and
 * the handler runs with the extra words as its arguments. Whatever the handler prints with the logger
 * shows up in the log panel.
 *
 * <pre>{@code
 * Flixel.debug.console.register("spawn", "spawn <type> [count]", "Creates enemies at the cursor.", args -> {
 *   String type = args.getString(0, "grunt"); // 0 = The first argument after the command name.
 *   int count = args.getInt(1, 1);
 *   spawner.spawn(type, count);
 *   Flixel.info("Spawned {} {}.", count, type);
 * });
 *
 * Flixel.debug.console.execute("spawn \"big grunt\" 3"); // Double quotes keep spaces in one argument.
 * }</pre>
 *
 * <h2>Rules</h2>
 *
 * <ul>
 *   <li>A command name starts with a letter and continues with letters, digits, or dots. Names are not
 *       case sensitive when you run them.</li>
 *   <li>Arguments are separated by spaces. Double quotes group words, and {@code ""} is an empty
 *       argument.</li>
 *   <li>A line can have at most {@value FlixelDebugProtocol#MAX_COMMAND_CHARS} characters.</li>
 *   <li>The last {@value #MAX_HISTORY_ENTRIES} different lines are remembered in {@link #getHistory()}.</li>
 *   <li>A handler that throws does not crash the game. The error is logged as {@code <name> failed: <message>}
 *       and the result is {@link FlixelDebugProtocol#CONSOLE_RESULT_FAILED}.</li>
 * </ul>
 *
 * <h2>Built-in commands</h2>
 *
 * <p>{@code help [filter]}, {@code pause [on|off]}, {@code hitboxes [on|off]}, {@code step},
 * {@code rate [hz]}, {@code loglevel [Debug|Info|Warn|Error|Default]}, {@code watch.mouse [on|off]},
 * {@code watch.camera [on|off]}, {@code watch.clear}, {@code select [path]}, {@code select.clear},
 * {@code select.parent}, {@code freecam [on|off]}, {@code goto <x> <y>}, and {@code resetState}.
 *
 * <p>All methods must be called from the game's main thread.
 *
 * @see FlixelDebugCommandArgs
 * @see FlixelDebugProtocol#KIND_CONSOLE
 */
public class FlixelDebugConsolePanel extends FlixelDebugPanelBase {

  /** The most lines the history keeps; the oldest is dropped first. */
  public static final int MAX_HISTORY_ENTRIES = 100;

  /** The longest command line that is accepted, in characters. */
  public static final int MAX_LINE_CHARS = FlixelDebugProtocol.MAX_COMMAND_CHARS;

  /** The value {@link #run(CharSequence)} returns for a blank line, which neither runs nor fails. */
  public static final int RESULT_IGNORED = -1;

  private static final String TAG = "FlixelDebug";

  private final FlixelDebugManager owner;
  private final FlixelArray<Command> commands = new FlixelArray<>();
  private final FlixelArray<String> history = new FlixelArray<>(MAX_HISTORY_ENTRIES);
  private final FlixelString lineScratch = new FlixelString(128);

  private String resultMessage = "";

  private int resultStatus = FlixelDebugProtocol.CONSOLE_RESULT_RAN;

  private boolean resultPending;

  /**
   * Creates the console and registers the built-in commands.
   *
   * @param owner The debug manager whose panels the built-in commands control.
   */
  public FlixelDebugConsolePanel(@NotNull FlixelDebugManager owner) {
    super("console", "Console", FlixelDebugProtocol.KIND_CONSOLE);
    this.owner = owner;
    registerBuiltins();
  }

  /**
   * Registers a command that has no usage text or description.
   *
   * @param name The command name.
   * @param handler The code that runs when the command is typed.
   * @throws IllegalArgumentException If the name is not valid.
   * @see #register(String, String, String, Consumer)
   */
  public void register(@NotNull String name, @NotNull Consumer<FlixelDebugCommandArgs> handler) {
    register(name, "", "", handler);
  }

  /**
   * Registers a command. A command with the same name (ignoring case) is replaced.
   *
   * @param name The command name. It must start with a letter and continue with letters, digits, or dots.
   * @param usage A short syntax hint such as {@code "spawn <type> [count]"}. Empty means just the name.
   * @param description One sentence about what the command does. May be empty.
   * @param handler The code that runs when the command is typed.
   * @throws IllegalArgumentException If the name is not valid.
   */
  public void register(@NotNull String name, @Nullable String usage, @Nullable String description,
      @NotNull Consumer<FlixelDebugCommandArgs> handler) {
    if (handler == null) {
      return;
    }
    validate(name);
    Command command = new Command(name, usage != null ? usage : "", description != null ? description : "", handler);
    int existing = indexOf(name);
    if (existing >= 0) {
      commands.removeIndex(existing);
    }
    int at = 0;
    while (at < commands.getSize() && commands.get(at).name.compareToIgnoreCase(name) < 0) {
      at++;
    }
    commands.insert(at, command);
    markChanged();
  }

  /**
   * Removes a command.
   *
   * @param name The command name, ignoring case. An unknown name does nothing.
   */
  public void unregister(@Nullable String name) {
    int index = indexOf(name);
    if (index >= 0) {
      commands.removeIndex(index);
      markChanged();
    }
  }

  /**
   * Runs a command line and returns how it went, without throwing.
   *
   * <p>The outcome is also remembered for {@link #writeResult(FlixelDebugMessageWriter)}.
   *
   * @param line The raw line, for example {@code "spawn \"big grunt\" 3"}.
   * @return {@link FlixelDebugProtocol#CONSOLE_RESULT_RAN}, {@link FlixelDebugProtocol#CONSOLE_RESULT_FAILED}
   *   (the handler threw, or the line was longer than {@value #MAX_LINE_CHARS} characters),
   *   {@link FlixelDebugProtocol#CONSOLE_RESULT_UNKNOWN}, or {@link #RESULT_IGNORED} for a blank line.
   */
  public int run(@Nullable CharSequence line) {
    if (line == null) {
      return RESULT_IGNORED;
    }
    if (line.length() > MAX_LINE_CHARS) {
      logger().error("Rejected a command line longer than {} characters.", MAX_LINE_CHARS);
      return setResult(FlixelDebugProtocol.CONSOLE_RESULT_FAILED,
          "The line is longer than " + MAX_LINE_CHARS + " characters.");
    }
    String trimmed = line.toString().trim();
    if (trimmed.isEmpty()) {
      return RESULT_IGNORED;
    }
    addToHistory(trimmed);

    FlixelArray<String> tokens = tokenize(trimmed);
    String name = tokens.get(0);
    Command command = find(name);
    if (command == null) {
      logger().warn("Unknown command \"{}\". Type \"help\" to list commands.", name);
      return setResult(FlixelDebugProtocol.CONSOLE_RESULT_UNKNOWN, "Unknown command \"" + name + "\".");
    }
    String[] argv = new String[tokens.getSize() - 1];
    for (int i = 0; i < argv.length; i++) {
      argv[i] = tokens.get(i + 1);
    }
    try {
      command.handler.accept(new FlixelDebugCommandArgs(argv));
    } catch (Exception e) {
      String message = e.getMessage() != null ? e.getMessage() : e.getClass().getName();
      logger().error("{} failed: {}", name, message);
      return setResult(FlixelDebugProtocol.CONSOLE_RESULT_FAILED, message);
    }
    return setResult(FlixelDebugProtocol.CONSOLE_RESULT_RAN, "");
  }

  /**
   * Runs a command line.
   *
   * @param line The raw line.
   * @return {@code true} when a command ran without failing.
   * @see #run(CharSequence)
   */
  public boolean execute(@Nullable CharSequence line) {
    return run(line) == FlixelDebugProtocol.CONSOLE_RESULT_RAN;
  }

  /**
   * Returns whether a command with the given name is registered.
   *
   * @param name The command name, ignoring case.
   * @return {@code true} when it exists.
   */
  public boolean has(@Nullable String name) {
    return indexOf(name) >= 0;
  }

  /**
   * Returns the registered command names, sorted without regard to case.
   *
   * @return A new array of names.
   */
  @NotNull
  public FlixelArray<String> getNames() {
    FlixelArray<String> out = new FlixelArray<>(commands.getSize());
    for (int i = 0; i < commands.getSize(); i++) {
      out.add(commands.get(i).name);
    }
    return out;
  }

  /**
   * Returns whether a result was produced that has not been written yet.
   *
   * @return {@code true} when {@link #writeResult(FlixelDebugMessageWriter)} has something new.
   */
  public boolean hasPendingResult() {
    return resultPending;
  }

  /**
   * Writes the outcome of the most recent {@link #run(CharSequence)} as a {@code RESULT} payload, then
   * marks it as sent.
   *
   * @param writer The writer positioned right after the panel index.
   */
  public void writeResult(@NotNull FlixelDebugMessageWriter writer) {
    writer.writeU8(FlixelDebugProtocol.CONSOLE_RESULT)
        .writeU8(resultStatus)
        .writeString(resultMessage);
    resultPending = false;
  }

  /**
   * Writes the list of commands as a {@code COMMANDS} payload. The panel's revision changes whenever the
   * list does.
   *
   * @param writer The writer positioned right after the panel index.
   */
  @Override
  public void write(@NotNull FlixelDebugMessageWriter writer) {
    int n = Math.min(commands.getSize(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.writeU8(FlixelDebugProtocol.CONSOLE_COMMANDS);
    writer.beginArray();
    for (int i = 0; i < n; i++) {
      Command command = commands.get(i);
      writer.writeString(command.name)
          .writeString(command.usage.isEmpty() ? command.name : command.usage)
          .writeString(command.description);
    }
    writer.endArray(n);
  }

  @Override
  public boolean hasPendingPayload() {
    return resultPending;
  }

  @Override
  public void writePendingPayload(@NotNull FlixelDebugMessageWriter writer) {
    if (resultPending) {
      writeResult(writer);
    }
  }

  @Override
  public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
    if (action == FlixelDebugProtocol.CONSOLE_RUN) {
      reader.readString(lineScratch);
      run(lineScratch);
    }
  }

  @Override
  public void reset() {
    history.clear();
    resultPending = false;
  }

  private int setResult(int status, String message) {
    resultStatus = status;
    resultMessage = message;
    resultPending = true;
    return status;
  }

  private void addToHistory(String line) {
    if (history.getSize() > 0 && history.peek().equals(line)) {
      return;
    }
    while (history.getSize() >= MAX_HISTORY_ENTRIES) {
      history.removeIndex(0);
    }
    history.add(line);
  }

  private int indexOf(@Nullable String name) {
    if (name == null) {
      return -1;
    }
    for (int i = 0, n = commands.getSize(); i < n; i++) {
      if (commands.get(i).name.equalsIgnoreCase(name)) {
        return i;
      }
    }
    return -1;
  }

  @Nullable
  private Command find(String name) {
    int index = indexOf(name);
    return index >= 0 ? commands.get(index) : null;
  }

  private FlixelLogger logger() {
    return Flixel.log.tagged(TAG);
  }

  /**
   * Splits a line into words. Spaces separate words, double quotes keep spaces inside one word, and a pair
   * of quotes with nothing between them is an empty word.
   */
  private static FlixelArray<String> tokenize(String line) {
    FlixelArray<String> tokens = new FlixelArray<>();
    StringBuilder current = new StringBuilder();
    boolean inQuotes = false;
    boolean started = false;
    for (int i = 0; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '"') {
        inQuotes = !inQuotes;
        started = true;
      } else if (!inQuotes && (c == ' ' || c == '\t')) {
        if (started) {
          tokens.add(current.toString());
          current.setLength(0);
          started = false;
        }
      } else {
        current.append(c);
        started = true;
      }
    }
    if (started) {
      tokens.add(current.toString());
    }
    if (tokens.getSize() == 0) {
      tokens.add("");
    }
    return tokens;
  }

  /**
   * Checks that a command name starts with a letter and continues with letters, digits, or dots.
   *
   * @param name The candidate name.
   * @throws IllegalArgumentException If the name is null, empty, or uses other characters.
   */
  private static void validate(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Command name must not be null or empty.");
    }
    boolean valid = isLetter(name.charAt(0));
    for (int i = 1; valid && i < name.length(); i++) {
      char c = name.charAt(i);
      valid = isLetter(c) || (c >= '0' && c <= '9') || c == '.';
    }
    if (!valid) {
      throw new IllegalArgumentException("Invalid command name '" + name
          + "'. A name starts with a letter and continues with letters, digits, or dots.");
    }
  }

  private static boolean isLetter(char c) {
    return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
  }

  private static boolean startsWithIgnoreCase(String text, String prefix) {
    return text.regionMatches(true, 0, prefix, 0, prefix.length());
  }

  private void registerBuiltins() {
    register("help", "help [filter]", "Lists the commands, or only those whose name starts with filter.", args -> {
      String filter = args.getString(0, "");
      int shown = 0;
      for (int i = 0; i < commands.getSize(); i++) {
        Command command = commands.get(i);
        if (!startsWithIgnoreCase(command.name, filter)) {
          continue;
        }
        String usage = command.usage.isEmpty() ? command.name : command.usage;
        if (command.description.isEmpty()) {
          logger().info(usage);
        } else {
          logger().info("{}  -  {}", usage, command.description);
        }
        shown++;
      }
      if (shown == 0) {
        logger().info("No commands start with \"{}\".", filter);
      }
    });

    register("pause", "pause [on|off]", "Pauses or resumes the game.", args -> {
      FlixelDebugControlsPanel controls = owner.controls;
      controls.setPaused(args.getBoolean(0, !controls.isPaused()));
      logger().info("Paused: {}", controls.isPaused());
    });

    register("hitboxes", "hitboxes [on|off]", "Shows or hides object hitboxes.", args -> {
      FlixelDebugControlsPanel controls = owner.controls;
      controls.setHitboxes(args.getBoolean(0, !controls.isHitboxes()));
      logger().info("Hitboxes: {}", controls.isHitboxes());
    });

    register("step", "step", "Runs one frame while the game is paused.", args -> {
      if (!owner.controls.stepFrame()) {
        logger().warn("The game must be paused to step a frame.");
      }
    });

    register("rate", "rate [hz]", "Shows or sets how many times per second the stats are sampled (1 to 30).", args -> {
      FlixelDebugControlsPanel controls = owner.controls;
      if (!args.isEmpty()) {
        controls.setRate(args.getInt(0, controls.getRate()));
      }
      logger().info("Sample rate: {} Hz", controls.getRate());
    });

    register("loglevel", "loglevel [Debug|Info|Warn|Error|Default]",
        "Shows or sets the minimum level of logged messages.", args -> {
          FlixelDebugControlsPanel controls = owner.controls;
          if (!args.isEmpty()) {
            controls.setLogLevel(parseLogLevel(args.getString(0, "")));
          }
          FlixelLogLevel level = FlixelDebugControlsPanel.toLogLevel(controls.getLogLevel());
          logger().info("Log level: {}", level != null ? level.name() : "DEFAULT");
        });

    register("watch.mouse", "watch.mouse [on|off]", "Shows or hides the mouse position watch.", args -> {
      FlixelDebugWatchPanel watches = owner.watches;
      boolean on = args.getBoolean(0, !watches.isMouseWatched());
      if (on) {
        watches.addMouse();
      } else {
        watches.removeMouse();
      }
      logger().info("Mouse watch: {}", on);
    });

    register("watch.camera", "watch.camera [on|off]", "Shows or hides the camera watch.", args -> {
      FlixelDebugWatchPanel watches = owner.watches;
      boolean on = args.getBoolean(0, !watches.isCameraWatched());
      if (on) {
        watches.addCamera();
      } else {
        watches.removeCamera();
      }
      logger().info("Camera watch: {}", on);
    });

    register("watch.clear", "watch.clear", "Removes every watch.", args -> {
      owner.watches.clear();
      logger().info("Cleared the watches.");
    });

    register("select", "select [path]",
        "Selects the object at a scene path such as state.3.0, or the object under the mouse without a path.", args -> {
          FlixelDebugInspectorPanel inspector = owner.inspector;
          String path = args.getString(0, "");
          if (path.isEmpty()) {
            Object hit = owner.controls.pickAtMouse();
            if (hit == null) {
              logger().warn("Nothing is drawn under the mouse.");
            } else {
              inspector.select(hit);
              logger().info("Selected {}", hit);
            }
          } else if (inspector.selectPath(path)) {
            logger().info("Selected {}", inspector.getSelected());
          } else {
            logger().warn("No object lives at \"{}\".", path);
          }
        });

    register("freecam", "freecam [on|off]", "Turns the free camera (WASD, wheel, right drag) on or off.", args -> {
      FlixelDebugControlsPanel controls = owner.controls;
      controls.setFreeCam(args.getBoolean(0, !controls.isFreeCam()));
      logger().info("Free camera: {}", controls.isFreeCam());
    });

    register("goto", "goto <x> <y>", "Moves the main camera to center on a world position (turns on the free camera).",
        args -> {
          if (args.getArgumentCount() < 2) {
            logger().warn("Usage: goto <x> <y>, for example goto 640 360");
            return;
          }
          float x = args.getFloat(0, Float.NaN);
          float y = args.getFloat(1, Float.NaN);
          if (Float.isNaN(x) || Float.isNaN(y)) {
            logger().warn("goto needs two numbers, for example goto 640 360");
          } else if (owner.controls.goTo(x, y)) {
            logger().info("Camera centered on {}, {}", x, y);
          } else {
            logger().warn("There is no camera to move.");
          }
        });

    register("select.clear", "select.clear", "Clears the object selected in the inspector.", args -> {
      owner.inspector.clearSelection();
      logger().info("Cleared the selection.");
    });

    register("select.parent", "select.parent", "Selects the group or state that holds the selected object.", args -> {
      FlixelDebugInspectorPanel inspector = owner.inspector;
      if (inspector.getSelected() == null) {
        logger().warn("Nothing is selected.");
      } else if (inspector.selectParent()) {
        logger().info("Selected the parent: {}", inspector.getSelected());
      } else {
        logger().warn("The selected object has no parent.");
      }
    });

    register("resetState", "resetState", "Restarts the current state.", args -> {
      logger().info("Resetting the current state.");
      Flixel.resetState();
    });
  }

  private static int parseLogLevel(String text) {
    if (text.equalsIgnoreCase("debug")) {
      return FlixelDebugProtocol.LEVEL_DEBUG;
    } else if (text.equalsIgnoreCase("info")) {
      return FlixelDebugProtocol.LEVEL_INFO;
    } else if (text.equalsIgnoreCase("warn")) {
      return FlixelDebugProtocol.LEVEL_WARN;
    } else if (text.equalsIgnoreCase("error")) {
      return FlixelDebugProtocol.LEVEL_ERROR;
    } else if (text.equalsIgnoreCase("default")) {
      return FlixelDebugProtocol.LEVEL_DEFAULT;
    }
    throw new IllegalArgumentException("Unknown log level \"" + text + "\". Use Debug, Info, Warn, Error, or Default.");
  }

  /**
   * Returns the history of accepted command lines, oldest first.
   *
   * @return The live history list. Do not modify it.
   */
  @NotNull
  public FlixelList<String> getHistory() {
    return history;
  }

  public int getResultStatus() {
    return resultStatus;
  }

  @NotNull
  public String getResultMessage() {
    return resultMessage;
  }

  private record Command(String name, String usage, String description, Consumer<FlixelDebugCommandArgs> handler) {
  }
}
