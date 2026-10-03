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
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Owns the console commands of the FlixelGDX debugger: registering, running, and remembering them.
 *
 * <p>Think of it as a restaurant's order window. You hand in a name ("hello") and a handler that
 * knows what to do, and later anyone can shout that name through the window (from the debug
 * overlay's console or from code) and the matching handler runs with the extra words as its
 * arguments.
 *
 * <p>An instance lives on the debug manager and is reached through {@link Flixel#debug}:
 *
 * <pre>{@code
 * Flixel.debug.commands.register("hello", args -> {
 *   String name = args.getString(0, "World"); // 0 = The first argument after the command name.
 *   Flixel.info("Hello, " + name + "!");
 * });
 *
 * Flixel.debug.commands.execute("hello Ada");
 * }</pre>
 *
 * <p>Command handlers receive {@link FlixelDebugCommandArgs}. Parsing stays explicit, which avoids
 * reflection and keeps this API working on platforms where reflection is restricted.
 *
 * <p>Several commands are always available: {@code help}, {@code pause}, {@code hitboxes},
 * {@code hide}, {@code resetState}, {@code watch.clear}, and {@code watch.mouse}.
 *
 * <p>All methods are intended to be called from the game's main thread.
 */
public class FlixelDebugCommandManager {

  /** Maximum entries kept in the input history (oldest are dropped first). */
  public static final int MAX_HISTORY_ENTRIES = 64;

  /**
   * Regex enforced by {@link #register(String, Consumer)}. A valid command name must
   * consist of one or more letters and / or periods only; everything else (numbers, hyphens, underscores,
   * whitespace, symbols, etc.) triggers an {@link IllegalArgumentException} from {@link #validate(String)}.
   */
  private static final Pattern VALID_COMMAND_NAME = Pattern.compile("^[a-zA-Z.]+$");
  private static final FlixelLogger LOG = Flixel.log.tagged("FlixelDebug");

  private final FlixelDebugManager owner;
  private final FlixelMap<String, RegisteredCommand> commands = new FlixelMap<>();
  private final FlixelArray<String> history = new FlixelArray<>(MAX_HISTORY_ENTRIES);

  /**
   * Creates a command manager and registers the built-in commands.
   *
   * @param owner The debug manager whose overlay the built-in commands control.
   */
  public FlixelDebugCommandManager(@NotNull FlixelDebugManager owner) {
    this.owner = owner;
    registerBuiltins();
  }

  /**
   * Registers a console command. The {@code handler} receives a {@link FlixelDebugCommandArgs}
   * object that wraps the positional tokens the user typed after the command name.
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.debug.commands.register("hello", args -> {
   *   String name = args.getString(0, "World");
   *   Flixel.info("Hello, " + name + "!");
   * });
   * }</pre>
   *
   * @param name The command name (the first token typed in the console).
   * @param handler The handler invoked when the command runs.
   * @throws IllegalArgumentException If {@code name} is null, empty, or contains characters
   *   outside {@code [a-zA-Z.]}.
   */
  public void register(@NotNull String name, @NotNull Consumer<FlixelDebugCommandArgs> handler) {
    if (handler == null) {
      return;
    }
    validate(name);
    commands.put(name, new RegisteredCommand(name, handler::accept));
  }

  /**
   * Removes a previously registered command.
   *
   * @param name The command name to remove.
   */
  public void unregister(@NotNull String name) {
    if (name != null) {
      commands.remove(name);
    }
  }

  /**
   * Executes a raw command line. The first whitespace-separated token is the command name and
   * any remaining tokens become positional arguments. Logs an error to {@link Flixel#log} if the
   * command does not exist.
   *
   * @param commandLine The raw input line (for example {@code "spawn enemy.png 1.5"}).
   * @return {@code true} if a command matched and was executed; {@code false} otherwise.
   */
  public boolean execute(@NotNull String commandLine) {
    if (commandLine == null) {
      return false;
    }
    String trimmed = commandLine.trim();
    if (trimmed.isEmpty()) {
      return false;
    }
    addToHistory(trimmed);

    String[] tokens = trimmed.split("\\s+");
    String name = tokens[0];
    RegisteredCommand cmd = commands.get(name);
    if (cmd == null) {
      LOG.error("Unknown command \"{}\". Type \"help\" to see the registered commands.", name);
      return false;
    }
    String[] argv = new String[tokens.length - 1];
    System.arraycopy(tokens, 1, argv, 0, argv.length);
    try {
      cmd.handler.invoke(new FlixelDebugCommandArgs(argv));
    } catch (Throwable t) {
      LOG.error("Command \"{}\" threw {}: {}", name, t.getClass().getSimpleName(), t.getMessage());
      return false;
    }
    return true;
  }

  /**
   * Returns {@code true} if a command with the given name is registered.
   *
   * @param name The command name to look up.
   * @return {@code true} if a command with that name has been registered.
   */
  public boolean has(@NotNull String name) {
    return commands.containsKey(name);
  }

  /**
   * Returns the in-memory command history (oldest first).
   *
   * @return The command history.
   */
  @NotNull
  public FlixelList<String> getHistory() {
    return history;
  }

  /**
   * Returns the {@link FlixelArray} of registered command names. The returned array is freshly allocated.
   *
   * @return A sorted array of all registered command names.
   */
  @NotNull
  public FlixelArray<String> getNames() {
    FlixelArray<String> out = new FlixelArray<>(commands.getSize());
    for (FlixelMap.Entry<String, RegisteredCommand> e : commands.entries()) {
      out.add(e.key);
    }
    out.sort();
    return out;
  }

  /**
   * Verifies that {@code name} is a syntactically valid command identifier (only ASCII letters
   * and periods). Throws {@link IllegalArgumentException} for {@code null}, empty, or otherwise
   * invalid inputs so registration mistakes surface immediately at startup instead of silently
   * dropping the command.
   *
   * <p>The pattern is intentionally restrictive: numbers, hyphens, underscores, whitespace,
   * and special symbols are all rejected.
   *
   * @param name The candidate command name. Must not be {@code null} or empty.
   * @throws IllegalArgumentException If {@code name} is null, empty, or contains characters
   *   outside {@code [a-zA-Z.]}.
   */
  private static void validate(@Nullable String name) {
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Command name must not be null or empty.");
    }
    if (!VALID_COMMAND_NAME.matcher(name).matches()) {
      throw new IllegalArgumentException(
          "Invalid command name '" + name
              + "'. Command names may only contain letters and periods (regex: "
              + VALID_COMMAND_NAME.pattern() + ").");
    }
  }

  private void addToHistory(@NotNull String line) {
    // Skip duplicate of the most recent entry to avoid spamming the up-arrow buffer.
    if (history.getSize() > 0 && history.peek().equals(line)) {
      return;
    }
    while (history.getSize() >= MAX_HISTORY_ENTRIES) {
      history.removeIndex(0);
    }
    history.add(line);
  }

  private void registerBuiltins() {
    register("help", args -> {
      String filter = args.getString(0, null);
      FlixelArray<String> names = getNames();
      LOG.info("Registered commands:");
      for (int i = 0; i < names.getSize(); i++) {
        String n = names.get(i);
        if (filter != null && !n.startsWith(filter)) {
          continue;
        }
        LOG.info("  {}", n);
      }
    });

    register("pause", args -> {
      boolean target = args.getBoolean(0, !Flixel.game.isGamePaused());
      Flixel.game.setGamePaused(target);
      LOG.info("Pause state: {}", Flixel.game.isGamePaused());
    });

    register("hitboxes", args -> {
      FlixelDebugOverlay overlay = owner.overlay;
      boolean target = args.getBoolean(0, !overlay.isDrawDebug());
      overlay.setDrawDebug(target);
      LOG.info("Hitboxes: {}", overlay.isDrawDebug());
    });

    register("hide", args -> owner.overlay.setVisible(false));

    register("resetState", args -> {
      LOG.info("Resetting current state.");
      Flixel.resetState();
    });

    register("watch.clear", args -> {
      Flixel.watch.clear();
      LOG.info("Cleared watch entries.");
    });

    register("watch.mouse", args -> {
      Flixel.watch.addMouse();
    });
  }

  /**
   * Internal handler form used after registration has wrapped the raw consumer. Kept
   * package-private because external code never has a reason to construct one directly.
   */
  @FunctionalInterface
  interface CommandHandler {
    void invoke(@NotNull FlixelDebugCommandArgs args);
  }

  record RegisteredCommand(String name, CommandHandler handler) {
  }
}
