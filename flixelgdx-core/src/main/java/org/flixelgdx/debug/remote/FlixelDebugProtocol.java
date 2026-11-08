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
package org.flixelgdx.debug.remote;

/**
 * Numeric constants of the remote debug wire protocol.
 *
 * <p>Think of this class as the phrasebook two travelers share: the game and the debugger app never
 * share code, but both agree that "3" means "panel list" and "6" means "ping". Every name here matches
 * a row in {@code DEBUG_PROTOCOL.md} at the repository root, which is the single source of truth.
 * Codes are never reused; a retired code stays reserved forever.
 *
 * <p>Naming scheme: {@code MSG_*} are message types, {@code KIND_*} are panel kinds, {@code VALUE_*}
 * are property value kinds, {@code LEVEL_*} are log levels, {@code <PANEL>_<NAME>} are payload
 * subtypes and action codes of one panel. Custom panels use {@code CUSTOM_*} for their action codes.
 *
 * @see FlixelDebugMessageWriter
 * @see FlixelDebugMessageReader
 */
public final class FlixelDebugProtocol {

  public static final int VERSION_MAJOR = 1;
  public static final int VERSION_MINOR = 1;

  public static final int DEFAULT_PORT = 6510;
  public static final int RECONNECT_MILLIS = 2000;

  public static final int MAX_STRING_BYTES = 65535;
  public static final int MAX_ARRAY_COUNT = 65535;
  public static final int MAX_LOG_MESSAGE_CHARS = 2000;
  public static final int MAX_COMMAND_CHARS = 500;

  public static final int MSG_HELLO = 1;
  public static final int MSG_HELLO_ACK = 2;
  public static final int MSG_PANEL_LIST = 3;
  public static final int MSG_PANEL_DATA = 4;
  public static final int MSG_PANEL_ACTION = 5;
  public static final int MSG_PING = 6;
  public static final int MSG_PONG = 7;

  public static final int KIND_CUSTOM = 0;
  public static final int KIND_STATS = 1;
  public static final int KIND_LOGS = 2;
  public static final int KIND_WATCHES = 3;
  public static final int KIND_TRACKERS = 4;
  public static final int KIND_TEXTURES = 5;
  public static final int KIND_CONTROLS = 6;
  public static final int KIND_CONSOLE = 7;
  public static final int KIND_SCENE = 8;
  public static final int KIND_INSPECTOR = 9;
  public static final int KIND_SAVE = 10;
  public static final int KIND_VIEWPORT = 11;
  public static final int KIND_CAPTURE = 12;
  public static final int KIND_INPUT = 13;

  public static final int VALUE_BOOL = 1;
  public static final int VALUE_INT = 2;
  public static final int VALUE_LONG = 3;
  public static final int VALUE_FLOAT = 4;
  public static final int VALUE_DOUBLE = 5;
  public static final int VALUE_STRING = 6;
  public static final int VALUE_ERROR = 7;

  public static final int LEVEL_DEBUG = 0;
  public static final int LEVEL_INFO = 1;
  public static final int LEVEL_WARN = 2;
  public static final int LEVEL_ERROR = 3;
  public static final int LEVEL_DEFAULT = 255;

  public static final int LOGS_CLEAR = 1;

  public static final int TEXTURES_LIST = 1;
  public static final int TEXTURES_THUMBNAIL = 2;
  public static final int TEXTURES_REQUEST_THUMBNAIL = 1;

  public static final int CONTROLS_SET_PAUSED = 1;
  public static final int CONTROLS_STEP = 2;
  public static final int CONTROLS_SET_HITBOXES = 3;
  public static final int CONTROLS_SET_FREECAM = 4;
  public static final int CONTROLS_SET_RATE = 5;
  public static final int CONTROLS_SET_LOG_LEVEL = 6;

  public static final int CONSOLE_COMMANDS = 1;
  public static final int CONSOLE_RESULT = 2;
  public static final int CONSOLE_RUN = 1;
  public static final int CONSOLE_RESULT_RAN = 0;
  public static final int CONSOLE_RESULT_FAILED = 1;
  public static final int CONSOLE_RESULT_UNKNOWN = 2;

  public static final int SCENE_REQUEST_TREE = 1;

  public static final int INSPECTOR_STATE = 1;
  public static final int INSPECTOR_THUMBNAIL = 2;
  public static final int INSPECTOR_SELECT = 1;
  public static final int INSPECTOR_SELECT_PATH = 2;
  public static final int INSPECTOR_SELECT_PARENT = 3;
  public static final int INSPECTOR_CLEAR = 4;
  public static final int INSPECTOR_GO_TO = 5;
  public static final int INSPECTOR_SET_PROPERTY = 6;
  public static final int INSPECTOR_REQUEST_THUMBNAIL = 7;

  public static final int SAVE_SET = 1;
  public static final int SAVE_RESET_KEY = 2;
  public static final int SAVE_SAVE_NOW = 3;
  public static final int SAVE_RESET_ALL = 4;
  public static final int SAVE_SELECT = 5;

  public static final int VIEWPORT_SET_CAMERA = 1;
  public static final int VIEWPORT_PICK = 2;
  public static final int VIEWPORT_MOVE = 3;
  public static final int VIEWPORT_PAN = 4;
  public static final int VIEWPORT_ZOOM = 5;
  public static final int VIEWPORT_RESET_CAMERA = 6;
  public static final int VIEWPORT_BOX_SELECTED = 1;
  public static final int VIEWPORT_BOX_GROUP = 2;
  public static final int VIEWPORT_BOX_INACTIVE = 4;

  public static final int CAPTURE_STATE = 1;
  public static final int CAPTURE_IMAGE = 2;
  public static final int CAPTURE_REQUEST = 1;
  public static final int CAPTURE_MIN_SIDE = 64;
  public static final int CAPTURE_MAX_SIDE = 2048;

  public static final int INPUT_KEY = 1;
  public static final int INPUT_POINTER = 2;
  public static final int INPUT_SCROLL = 3;
  public static final int INPUT_TEXT = 4;
  public static final int INPUT_CLICK_OBJECT = 5;
  public static final int INPUT_POINTER_MOVE = 0;
  public static final int INPUT_POINTER_DOWN = 1;
  public static final int INPUT_POINTER_UP = 2;
  public static final int INPUT_FLAG_KEYS = 1;
  public static final int INPUT_FLAG_POINTER = 2;
  public static final int INPUT_FLAG_SCROLL = 4;
  public static final int INPUT_FLAG_TEXT = 8;
  public static final int INPUT_FLAG_TOUCH = 16;
  public static final int INPUT_MAX_TEXT_CHARS = 1000;

  public static final int CUSTOM_PRESS = 1;
  public static final int CUSTOM_SET_PROPERTY = 2;

  private FlixelDebugProtocol() {}
}
