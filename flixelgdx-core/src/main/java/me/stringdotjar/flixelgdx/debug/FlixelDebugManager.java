/**********************************************************************************
 * Copyright (c) 2025-2026 stringdotjar
 *
 * This file is part of the FlixelGDX framework, licensed under the MIT License.
 * See the LICENSE file in the repository root for full license information.
 **********************************************************************************/

package me.stringdotjar.flixelgdx.debug;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelObject;

import java.lang.reflect.Constructor;
import java.lang.reflect.RecordComponent;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The single entry point for everything related to the FlixelGDX debugger.
 *
 * <p>An instance of this class is automatically created when {@link Flixel#initialize(me.stringdotjar.flixelgdx.FlixelGame)}
 * runs and is exposed as the static field {@link Flixel#debug}, mirroring how
 * {@link Flixel#sound}, {@link Flixel#assets}, and friends work. From your game code you can do things
 * like:
 *
 * <pre>{@code
 * Flixel.debug.toggleVisible();
 * Flixel.debug.setDrawDebug(true);
 * Flixel.debug.registerCommand("hello", args -> Flixel.info("Hi!"));
 * Flixel.debug.executeCommand("hello");
 * }</pre>
 *
 * <p>The manager is intentionally lightweight: it forwards visibility/hitbox toggles to the active
 * {@link FlixelDebugOverlay}, owns the registry of console commands, and tracks the
 * "currently inspected sprite" for the texture inspector window. The overlay itself (and the
 * platform-specific UI) reads from the manager rather than the other way around so the manager can
 * stay platform-agnostic and reflection-light.
 *
 * <h2>Custom commands</h2>
 *
 * <p>Two registration paths are provided:
 * <ol>
 *   <li>{@link #registerCommand(String, Consumer)} takes a handler that receives a
 *       {@link FlixelDebugCommandArgs} object. <strong>No reflection is used.</strong> This is the
 *       preferred form on platforms where reflection is restricted (TeaVM, R8/ProGuard-shrunk
 *       Android builds).</li>
 *   <li>{@link #registerCommand(String, Class, Consumer)} takes a record class and uses
 *       {@link Class#getRecordComponents()} plus the record's canonical constructor to assemble a
 *       fresh record instance from the console arguments. This is more convenient but
 *       <strong>does</strong> rely on Java reflection (records and {@code getRecordComponents} are
 *       only available on Java 16+ and platforms with full reflection support).</li>
 * </ol>
 *
 * <h2>Thread safety</h2>
 *
 * <p>All public methods are intended to be called from the libGDX main thread. Reading the
 * registered commands map outside the main thread is unsupported.
 */
public final class FlixelDebugManager {

  /**
   * Internal handler form used after both registration paths have parsed their input. Kept
   * package-private because external code never has a reason to construct one directly.
   */
  @FunctionalInterface
  interface CommandHandler {

    void invoke(@NotNull FlixelDebugCommandArgs args);
  }

  /**
   * A registered command, with its raw handler and (optional) per-argument metadata for help.
   *
   * @param argLabels Pretty per-argument descriptors used for help output. May be {@code null}.
   */
  record RegisteredCommand(String name, CommandHandler handler, @Nullable String[] argLabels) {

    RegisteredCommand(@NotNull String name, @NotNull CommandHandler handler, @Nullable String[] argLabels) {
      this.name = name;
      this.handler = handler;
      this.argLabels = argLabels;
    }
  }

  /**
   * Regex enforced by {@link #registerCommand(String, Consumer)} and the record overload. A
   * valid command name must consist of one or more letters and / or periods only. Periods are
   * allowed so that namespaced commands like {@code flixel.sound.pause} or {@code watch.clear}
   * stay valid; everything else (numbers, hyphens, underscores, whitespace, symbols, etc.)
   * triggers an {@link IllegalArgumentException} from {@link #validateCommandName(String)}.
   */
  private static final Pattern VALID_COMMAND_NAME = Pattern.compile("^[a-zA-Z.]+$");

  /** Maximum entries kept in the input history (oldest are dropped first). */
  public static final int MAX_HISTORY_ENTRIES = 64;

  private final ObjectMap<String, RegisteredCommand> commands = new ObjectMap<>();
  private final Array<String> commandHistory = new Array<>(MAX_HISTORY_ENTRIES);

  /** The sprite currently selected by the LMB picker, or {@code null}. */
  @Nullable
  private FlixelObject inspectedSprite;

  /** The sprite currently being dragged, or {@code null} if no drag is in progress. */
  @Nullable
  private FlixelObject draggedSprite;

  /** Internal: registers the small set of always-available commands ({@code help}, {@code clear}, etc.). */
  public FlixelDebugManager() {
    registerBuiltinCommands();
  }

  /**
   * Returns the currently active {@link FlixelDebugOverlay}, or {@code null} if the game is not
   * running in debug mode (or the overlay has not been created yet).
   *
   * @return The active overlay, or {@code null}.
   */
  @Nullable
  public FlixelDebugOverlay getOverlay() {
    return Flixel.getDebugOverlay();
  }

  /** Returns {@code true} when the overlay is on screen. */
  public boolean isVisible() {
    FlixelDebugOverlay overlay = getOverlay();
    return overlay != null && overlay.isVisible();
  }

  /**
   * Shows or hides the overlay.
   *
   * @param visible {@code true} to show, {@code false} to hide.
   */
  public void setVisible(boolean visible) {
    FlixelDebugOverlay overlay = getOverlay();
    if (overlay != null) {
      overlay.setVisible(visible);
    }
  }

  /** Toggles the overlay visibility. */
  public void toggleVisible() {
    FlixelDebugOverlay overlay = getOverlay();
    if (overlay != null) {
      overlay.toggleVisible();
    }
  }

  /** Returns {@code true} when bounding-box drawing (hitboxes) is on. */
  public boolean isDrawDebug() {
    FlixelDebugOverlay overlay = getOverlay();
    return overlay != null && overlay.isDrawDebug();
  }

  /**
   * Sets whether bounding boxes (hitboxes) should be drawn.
   *
   * @param drawDebug {@code true} to draw, {@code false} to hide.
   */
  public void setDrawDebug(boolean drawDebug) {
    FlixelDebugOverlay overlay = getOverlay();
    if (overlay != null) {
      overlay.setDrawDebug(drawDebug);
    }
  }

  /** Toggles bounding-box drawing. */
  public void toggleDrawDebug() {
    FlixelDebugOverlay overlay = getOverlay();
    if (overlay != null) {
      overlay.toggleDrawDebug();
    }
  }

  /**
   * Sets the sprite currently inspected by the texture inspector window.
   * Pass {@code null} to clear the selection.
   *
   * @param obj The sprite to inspect, or {@code null} to clear.
   */
  public void setInspectedSprite(@Nullable FlixelObject obj) {
    inspectedSprite = obj;
  }

  /**
   * Returns the sprite currently inspected, or {@code null} if no sprite is selected (or the previously
   * selected sprite has been destroyed). The returned sprite may be of any subclass of
   * {@link FlixelObject}.
   *
   * @return The inspected sprite, or {@code null}.
   */
  @Nullable
  public FlixelObject getInspectedSprite() {
    if (inspectedSprite != null && !inspectedSprite.exists) {
      inspectedSprite = null;
    }
    return inspectedSprite;
  }

  /** Sets the sprite that is currently being dragged via the LMB picker. Internal API. */
  void setDraggedSprite(@Nullable FlixelObject obj) {
    draggedSprite = obj;
  }

  /** Returns the sprite that is currently being dragged via the LMB picker, or {@code null}. */
  @Nullable
  public FlixelObject getDraggedSprite() {
    if (draggedSprite != null && !draggedSprite.exists) {
      draggedSprite = null;
    }
    return draggedSprite;
  }

  /**
   * Registers a console command using the no-reflection form. The {@code handler} receives a
   * {@link FlixelDebugCommandArgs} object that wraps the positional tokens the user typed after the
   * command name.
   *
   * <p>Use this overload on platforms where reflection is unavailable or undesirable (web/TeaVM,
   * heavily shrunk Android builds, etc.).
   *
   * <p>Example:
   * <pre>{@code
   * Flixel.debug.registerCommand("hello", args -> {
   *   String name = args.getString(0, "World"); // 0 = The first argument after the command name.
   *   Flixel.info("Hello, " + name + "!");
   * });
   * }</pre>
   *
   * @param name The command name (the first token typed in the console).
   * @param handler The handler invoked when the command runs.
   */
  public void registerCommand(@NotNull String name, @NotNull Consumer<FlixelDebugCommandArgs> handler) {
    if (handler == null) {
      return;
    }
    validateCommandName(name);
    commands.put(name, new RegisteredCommand(name, handler::accept, null));
  }

  /**
   * Registers a console command whose arguments are supplied as a {@link Record}. Uses
   * reflection ({@link Class#getRecordComponents()} plus the canonical constructor) to assemble a
   * fresh record instance from the console arguments and forward it to {@code handler}.
   *
   * <p>Supported record component types:
   * <ul>
   *   <li>{@code String}</li>
   *   <li>{@code int}, {@code long}, {@code short}, {@code byte}</li>
   *   <li>{@code float}, {@code double}</li>
   *   <li>{@code boolean}</li>
   * </ul>
   *
   * <p><strong>Reflection note:</strong> this overload uses {@link Class#getRecordComponents()},
   * which is part of standard Java reflection (Java 16+). On platforms that disallow reflection
   * (TeaVM, aggressively shrunk Android builds) prefer
   * {@link #registerCommand(String, Consumer)} instead.
   *
   * <p>Example:
   * <pre>{@code
   * record SpawnParams(String graphicKey, float scale) {}
   * Flixel.debug.registerCommand("spawn", SpawnParams.class, params -> {
   *   // params.graphicKey(), params.scale()
   * });
   * }</pre>
   *
   * @param name The command name (the first token typed in the console).
   * @param recordClass The record class describing the command's parameters.
   * @param handler The handler invoked when the command runs.
   * @param <R> The record type.
   */
  public <R extends Record> void registerCommand(@NotNull String name, @NotNull Class<R> recordClass,
                                                 @NotNull Consumer<R> handler) {
    if (recordClass == null || handler == null) {
      return;
    }
    validateCommandName(name);
    RecordComponent[] components = recordClass.getRecordComponents();
    if (components == null) {
      throw new IllegalArgumentException("Class " + recordClass.getName() + " is not a record.");
    }
    Class<?>[] paramTypes = new Class<?>[components.length];
    String[] argLabels = new String[components.length];
    for (int i = 0; i < components.length; i++) {
      paramTypes[i] = components[i].getType();
      argLabels[i] = components[i].getName() + ": " + components[i].getType().getSimpleName();
    }
    final Constructor<R> ctor;
    try {
      ctor = recordClass.getDeclaredConstructor(paramTypes);
    } catch (NoSuchMethodException e) {
      throw new IllegalArgumentException("No canonical constructor found for record "
        + recordClass.getName(), e);
    }
    ctor.setAccessible(true);

    CommandHandler runner = args -> {
      Object[] values = new Object[paramTypes.length];
      for (int i = 0; i < paramTypes.length; i++) {
        values[i] = parseRecordArg(args, i, paramTypes[i]);
      }
      try {
        R rec = ctor.newInstance(values);
        handler.accept(rec);
      } catch (ReflectiveOperationException e) {
        Flixel.error("FlixelDebug", "Failed to instantiate record " + recordClass.getSimpleName()
          + " for command \"" + name + "\": " + e.getMessage());
      }
    };
    commands.put(name, new RegisteredCommand(name, runner, argLabels));
  }

  private static Object parseRecordArg(@NotNull FlixelDebugCommandArgs args, int index, @NotNull Class<?> type) {
    if (type == String.class) {
      return args.getString(index, "");
    }
    if (type == int.class || type == Integer.class) {
      return args.getInt(index, 0);
    }
    if (type == long.class || type == Long.class) {
      return args.getLong(index, 0L);
    }
    if (type == short.class || type == Short.class) {
      return (short) args.getInt(index, 0);
    }
    if (type == byte.class || type == Byte.class) {
      return (byte) args.getInt(index, 0);
    }
    if (type == float.class || type == Float.class) {
      return args.getFloat(index, 0f);
    }
    if (type == double.class || type == Double.class) {
      return args.getDouble(index, 0.0);
    }
    if (type == boolean.class || type == Boolean.class) {
      return args.getBoolean(index, false);
    }
    if (type == char.class || type == Character.class) {
      String s = args.getString(index, "");
      return s.isEmpty() ? '\0' : s.charAt(0);
    }
    throw new IllegalArgumentException("Unsupported record component type: " + type.getName());
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
  private static void validateCommandName(@Nullable String name) {
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

  /**
   * Removes a previously registered command.
   *
   * @param name The command name to remove.
   */
  public void unregisterCommand(@NotNull String name) {
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
  public boolean executeCommand(@NotNull String commandLine) {
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
      Flixel.error("FlixelDebug", "Unknown command \"" + name + "\". Type \"help\" to see "
        + "the registered commands.");
      return false;
    }
    String[] argv = new String[tokens.length - 1];
    System.arraycopy(tokens, 1, argv, 0, argv.length);
    try {
      cmd.handler.invoke(new FlixelDebugCommandArgs(argv));
    } catch (Throwable t) {
      Flixel.error("FlixelDebug", "Command \"" + name + "\" threw " + t.getClass().getSimpleName()
        + ": " + t.getMessage());
      return false;
    }
    return true;
  }

  private void addToHistory(@NotNull String line) {
    // Skip duplicate of the most recent entry to avoid spamming the up-arrow buffer.
    if (commandHistory.size > 0 && commandHistory.peek().equals(line)) {
      return;
    }
    while (commandHistory.size >= MAX_HISTORY_ENTRIES) {
      commandHistory.removeIndex(0);
    }
    commandHistory.add(line);
  }

  /**
   * Returns the in-memory command history (oldest first). The returned array is the live backing
   * store and must not be modified. Use {@link Array#size} and indexed access to read it.
   *
   * @return The command history (live, do not modify).
   */
  @NotNull
  public Array<String> getCommandHistory() {
    return commandHistory;
  }

  /** Returns the {@link Array} of registered command names. The returned array is freshly allocated. */
  @NotNull
  public Array<String> getRegisteredCommandNames() {
    Array<String> out = new Array<>(commands.size);
    for (ObjectMap.Entry<String, RegisteredCommand> e : commands.entries()) {
      out.add(e.key);
    }
    out.sort();
    return out;
  }

  /** Returns {@code true} if a command with the given name is registered. */
  public boolean hasCommand(@NotNull String name) {
    return commands.containsKey(name);
  }

  /**
   * Returns the labels for the arguments of a command registered via the record-based form, or
   * {@code null} if the command does not exist or was registered via the free-form variant.
   *
   * @param name The command name.
   * @return Per-argument labels (e.g. {@code "graphicKey: String"}), or {@code null}.
   */
  @Nullable
  public String[] getCommandArgLabels(@NotNull String name) {
    RegisteredCommand cmd = commands.get(name);
    return cmd != null ? cmd.argLabels : null;
  }

  private void registerBuiltinCommands() {
    registerCommand("help", args -> {
      String filter = args.getString(0, null);
      Array<String> names = getRegisteredCommandNames();
      Flixel.info("FlixelDebug", "Registered commands:");
      for (int i = 0; i < names.size; i++) {
        String n = names.get(i);
        if (filter != null && !n.startsWith(filter)) {
          continue;
        }
        String[] labels = getCommandArgLabels(n);
        if (labels != null && labels.length > 0) {
          StringBuilder sb = new StringBuilder(64);
          sb.append("  ").append(n);
          for (String label : labels) {
            sb.append(" <").append(label).append(">");
          }
          Flixel.info("FlixelDebug", sb.toString());
        } else {
          Flixel.info("FlixelDebug", "  " + n);
        }
      }
    });

    registerCommand("pause", args -> {
      boolean target = args.getBoolean(0, !Flixel.isPaused());
      Flixel.setPaused(target);
      Flixel.info("FlixelDebug", "Pause state: " + Flixel.isPaused());
    });

    registerCommand("hitboxes", args -> {
      boolean target = args.getBoolean(0, !isDrawDebug());
      setDrawDebug(target);
      Flixel.info("FlixelDebug", "Hitboxes: " + isDrawDebug());
    });

    registerCommand("hide", args -> setVisible(false));

    registerCommand("resetState", args -> {
      Flixel.info("FlixelDebug", "Resetting current state.");
      Flixel.resetState();
    });

    registerCommand("watch.clear", args -> {
      if (Flixel.watch != null) {
        Flixel.watch.clear();
        Flixel.info("FlixelDebug", "Cleared watch entries.");
      }
    });

    registerCommand("watch.mouse", args -> {
      if (Flixel.watch != null) {
        Flixel.watch.addMouse();
      }
    });
  }
}
