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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes every protocol fixture once, as a list of typed fields, so the same description drives
 * encoding, decoding, and the committed {@code .bin} files.
 */
final class DebugProtocolFixtures {

  static final String RESOURCE_DIR = "/org/flixelgdx/debug/remote/fixtures/";
  static final String SOURCE_DIR = "src/test/resources/org/flixelgdx/debug/remote/fixtures/";

  static final String CJK = Character.toString(0x4E16);
  static final String EMOJI = Character.toString(0x1F600);
  static final String UNICODE =
      "h" + Character.toString(0xE9) + "llo " + CJK + Character.toString(0x754C) + " " + EMOJI;

  static final int P_STATS = 0;
  static final int P_LOGS = 1;
  static final int P_WATCHES = 2;
  static final int P_TRACKERS = 3;
  static final int P_TEXTURES = 4;
  static final int P_CONTROLS = 5;
  static final int P_CONSOLE = 6;
  static final int P_SCENE = 7;
  static final int P_INSPECTOR = 8;
  static final int P_SAVE = 9;
  static final int P_CUSTOM = 10;
  static final int P_VIEWPORT = 11;
  static final int P_CAPTURE = 12;
  static final int P_INPUT = 13;

  static final byte[] PNG_BYTES = { (byte) 0x89, 'P', 'N', 'G' };
  static final byte[] THUMB_BYTES = { 9, 8, 7 };

  static final Map<String, Op[]> ALL = new LinkedHashMap<>();

  static Op u8(int v) {
    return new Op('b', v);
  }

  static Op u16(int v) {
    return new Op('s', v);
  }

  static Op i32(int v) {
    return new Op('i', v);
  }

  static Op i64(long v) {
    return new Op('l', v);
  }

  static Op f32(float v) {
    return new Op('f', v);
  }

  static Op f64(double v) {
    return new Op('d', v);
  }

  static Op bool(boolean v) {
    return new Op('?', v);
  }

  static Op str(String v) {
    return new Op('S', v);
  }

  static Op bytes(byte[] v) {
    return new Op('B', v);
  }

  static Op begin() {
    return new Op('[', null);
  }

  static Op end(int count) {
    return new Op(']', count);
  }

  static Op val(Object v) {
    return new Op('V', v);
  }

  static Op err(String v) {
    return new Op('E', v);
  }

  static Op[] ops(Object... parts) {
    List<Op> list = new ArrayList<>();
    for (Object part : parts) {
      if (part instanceof Op op) {
        list.add(op);
      } else {
        for (Op op : (Op[]) part) {
          list.add(op);
        }
      }
    }
    return list.toArray(new Op[0]);
  }

  static void add(String name, Op[] fields) {
    ALL.put(name, fields);
  }

  static void add(String name, Object... parts) {
    ALL.put(name, ops(parts));
  }

  static Op[] row(String name, Object value, boolean editable) {
    return ops(str(name), val(value), bool(editable));
  }

  static Op[] data(int panel, Object... payload) {
    return ops(u8(FlixelDebugProtocol.MSG_PANEL_DATA), u16(panel), ops(payload));
  }

  static Op[] action(int panel, int code, Object... payload) {
    return ops(u8(FlixelDebugProtocol.MSG_PANEL_ACTION), u16(panel), u8(code), ops(payload));
  }

  static Op[] panel(int index, String id, String name, int kind) {
    return ops(u16(index), str(id), str(name), u8(kind));
  }

  static {
    add("value_bool", val(true));
    add("value_int", val(-123456));
    add("value_long", val(0x1122334455667788L));
    add("value_float", val(-1.5f));
    add("value_double", val(3.141592653589793));
    add("value_string", val("hello"));
    add("value_string_unicode", val(UNICODE));
    add("value_error", err("getter threw"));

    add("msg_hello", u8(FlixelDebugProtocol.MSG_HELLO), u16(1), u16(0), str("desktop"), str("Linux 6.17 amd64"),
        i32(1280), i32(720), str("Demo Game"), str("1.2.3"));
    add("msg_hello_ack", u8(FlixelDebugProtocol.MSG_HELLO_ACK), u16(1), u16(0));
    add("msg_panel_list", u8(FlixelDebugProtocol.MSG_PANEL_LIST), begin(),
        panel(P_STATS, "stats", "Stats", FlixelDebugProtocol.KIND_STATS),
        panel(P_LOGS, "logs", "Logs", FlixelDebugProtocol.KIND_LOGS),
        panel(P_WATCHES, "watches", "Watches", FlixelDebugProtocol.KIND_WATCHES),
        panel(P_TRACKERS, "trackers", "Trackers", FlixelDebugProtocol.KIND_TRACKERS),
        panel(P_TEXTURES, "textures", "Textures", FlixelDebugProtocol.KIND_TEXTURES),
        panel(P_CONTROLS, "controls", "Controls", FlixelDebugProtocol.KIND_CONTROLS),
        panel(P_CONSOLE, "console", "Console", FlixelDebugProtocol.KIND_CONSOLE),
        panel(P_SCENE, "scene", "Scene", FlixelDebugProtocol.KIND_SCENE),
        panel(P_INSPECTOR, "inspector", "Inspector", FlixelDebugProtocol.KIND_INSPECTOR),
        panel(P_SAVE, "save", "Save", FlixelDebugProtocol.KIND_SAVE),
        panel(P_CUSTOM, "custom.demo", "Demo", FlixelDebugProtocol.KIND_CUSTOM),
        panel(P_VIEWPORT, "viewport", "Viewport", FlixelDebugProtocol.KIND_VIEWPORT),
        panel(P_CAPTURE, "capture", "Capture", FlixelDebugProtocol.KIND_CAPTURE),
        panel(P_INPUT, "input", "Input", FlixelDebugProtocol.KIND_INPUT),
        end(14));
    add("msg_ping", u8(FlixelDebugProtocol.MSG_PING), i64(1234567890123L));
    add("msg_pong", u8(FlixelDebugProtocol.MSG_PONG), i64(1234567890123L));

    add("panel_stats", data(P_STATS,
        f32(59.5f), f32(16.25f), f32(3.5f), f32(0.75f),
        i32(120), i32(4000), i32(8000), i32(3), i32(2), i32(9),
        f32(64.5f), f32(32.25f), f32(12.5f), f32(256f),
        i32(777), f32(1.5f), f32(0.5f), bool(true), f64(12.5),
        f32(100.25f), f32(-50.5f), f32(2f)));
    add("panel_logs", data(P_LOGS, bool(true), begin(),
        i64(1), f64(0.5), u8(FlixelDebugProtocol.LEVEL_INFO), bool(false), str("Game"), str("Main.java:10"),
        str("hello"), str(""),
        i64(2), f64(1.25), u8(FlixelDebugProtocol.LEVEL_ERROR), bool(true), str("Err"), str("Foo.bar"), str("boom"),
        str("java.lang.Error\n\tat Foo.bar"),
        end(2)));
    add("panel_watches", data(P_WATCHES, begin(),
        str("fps"), val(60f), str("name"), val("hero"), str("broken"), err("NullPointerException"),
        end(3)));
    add("panel_trackers", data(P_TRACKERS, begin(),
        str("Player"), begin(), str("x"), val(10.5f), str("alive"), val(true), end(2),
        str("World"), begin(), str("seed"), val(42L), end(1),
        end(2)));
    add("panel_textures_list", data(P_TEXTURES, u8(FlixelDebugProtocol.TEXTURES_LIST), begin(),
        i32(7), str("assets/hero.png"), i32(64), i32(32), str("RGBA8"), i64(8192L), i32(2), bool(false),
        i32(9), str("assets/hero_copy.png"), i32(64), i32(32), str("RGBA8"), i64(8192L), i32(1), bool(true),
        end(2), i32(2), i64(16384L), i32(1)));
    add("panel_textures_thumbnail", data(P_TEXTURES, u8(FlixelDebugProtocol.TEXTURES_THUMBNAIL),
        i32(7), i32(2), i32(1), bytes(PNG_BYTES)));
    add("panel_controls",
        data(P_CONTROLS, bool(true), bool(false), bool(true), u8(20), u8(FlixelDebugProtocol.LEVEL_DEFAULT), begin(),
            str("F1"), str("Toggle overlay"), str("F2"), str("Pause"),
            end(2)));
    add("panel_console_commands", data(P_CONSOLE, u8(FlixelDebugProtocol.CONSOLE_COMMANDS), begin(),
        str("god"), str("god"), str("Toggles invincibility"),
        str("quit"), str("quit [code]"), str("Exits the game"),
        end(2)));
    add("panel_console_result",
        data(P_CONSOLE, u8(FlixelDebugProtocol.CONSOLE_RESULT), u8(FlixelDebugProtocol.CONSOLE_RESULT_FAILED),
            str("bad arguments")));
    add("panel_scene", data(P_SCENE, begin(),
        i32(1), i32(-1), str("PlayState"), str("play"), bool(false),
        i32(2), i32(1), str("FlixelSprite"), str("player"), bool(true),
        i32(3), i32(2), str("FlixelText"), str("label"), bool(false),
        end(3)));
    add("panel_inspector_state",
        data(P_INSPECTOR, u8(FlixelDebugProtocol.INSPECTOR_STATE), i32(2), str("play/player"), begin(),
            str("FlixelSprite"), str("Sprite"), begin(),
            row("x", 10.5f, true), row("visible", true, true), row("id", 2, false), end(3),
            str("FlixelObject"), str("Object"), begin(), row("tag", "p", true), end(1),
            end(2), bool(true)));
    add("panel_inspector_thumbnail", data(P_INSPECTOR, u8(FlixelDebugProtocol.INSPECTOR_THUMBNAIL),
        i32(2), i32(2), i32(1), bytes(THUMB_BYTES)));
    add("panel_save", data(P_SAVE, begin(),
        str("player.name"), str("\"Kori\""), u8(FlixelDebugProtocol.VALUE_STRING), bool(false),
        str("player.level"), str("12"), u8(FlixelDebugProtocol.VALUE_INT), bool(true),
        end(2), begin(), str("old.key"), end(1), str("Saved"), bool(false),
        begin(), str("Game/settings"), str("Game/profile1"), str("(not bound)"), end(3), u16(1)));
    add("panel_custom", data(P_CUSTOM, begin(),
        row("speed", 2.5f, true), row("label", "hi", false), end(2),
        begin(), u16(0), str("Reset"), u16(1), str("Spawn"), end(2)));
    add("panel_viewport", data(P_VIEWPORT,
        u8(0), u8(2), f32(320f), f32(180f), f32(2f), f32(100.5f), f32(-40f),
        bool(true), bool(false), bool(true), i32(3),
        begin(), str("FlixelSprite"), str("FlixelSpriteGroup"), end(2),
        begin(),
        i32(2), f32(10f), f32(20f), f32(64f), f32(32f), u16(1),
        u8(FlixelDebugProtocol.VIEWPORT_BOX_GROUP | FlixelDebugProtocol.VIEWPORT_BOX_INACTIVE),
        i32(3), f32(-8.5f), f32(150.25f), f32(16f), f32(16f), u16(0), u8(FlixelDebugProtocol.VIEWPORT_BOX_SELECTED),
        end(2)));
    add("panel_capture_state", data(P_CAPTURE, u8(FlixelDebugProtocol.CAPTURE_STATE), bool(true)));
    add("panel_capture_image", data(P_CAPTURE, u8(FlixelDebugProtocol.CAPTURE_IMAGE),
        i32(7), i32(2), i32(1), bytes(PNG_BYTES)));
    add("panel_input_state", data(P_INPUT, u8(FlixelDebugProtocol.INPUT_FLAG_KEYS
        | FlixelDebugProtocol.INPUT_FLAG_POINTER | FlixelDebugProtocol.INPUT_FLAG_SCROLL
        | FlixelDebugProtocol.INPUT_FLAG_TEXT | FlixelDebugProtocol.INPUT_FLAG_TOUCH)));

    add("action_logs_clear", action(P_LOGS, FlixelDebugProtocol.LOGS_CLEAR));
    add("action_textures_request_thumbnail",
        action(P_TEXTURES, FlixelDebugProtocol.TEXTURES_REQUEST_THUMBNAIL, i32(7)));
    add("action_controls_set_paused", action(P_CONTROLS, FlixelDebugProtocol.CONTROLS_SET_PAUSED, bool(true)));
    add("action_controls_step", action(P_CONTROLS, FlixelDebugProtocol.CONTROLS_STEP));
    add("action_controls_set_hitboxes", action(P_CONTROLS, FlixelDebugProtocol.CONTROLS_SET_HITBOXES, bool(true)));
    add("action_controls_set_freecam", action(P_CONTROLS, FlixelDebugProtocol.CONTROLS_SET_FREECAM, bool(false)));
    add("action_controls_set_rate", action(P_CONTROLS, FlixelDebugProtocol.CONTROLS_SET_RATE, u8(30)));
    add("action_controls_set_log_level",
        action(P_CONTROLS, FlixelDebugProtocol.CONTROLS_SET_LOG_LEVEL, u8(FlixelDebugProtocol.LEVEL_WARN)));
    add("action_console_run", action(P_CONSOLE, FlixelDebugProtocol.CONSOLE_RUN, str("god on")));
    add("action_scene_request_tree", action(P_SCENE, FlixelDebugProtocol.SCENE_REQUEST_TREE));
    add("action_inspector_select", action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_SELECT, i32(2)));
    add("action_inspector_select_path",
        action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_SELECT_PATH, str("play/player")));
    add("action_inspector_select_parent", action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_SELECT_PARENT));
    add("action_inspector_clear", action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_CLEAR));
    add("action_inspector_go_to", action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_GO_TO));
    add("action_inspector_set_property", action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_SET_PROPERTY,
        i32(2), u16(0), u16(0), val(12.5f)));
    add("action_inspector_request_thumbnail",
        action(P_INSPECTOR, FlixelDebugProtocol.INSPECTOR_REQUEST_THUMBNAIL, i32(2)));
    add("action_save_set", action(P_SAVE, FlixelDebugProtocol.SAVE_SET, str("player.level"), str("13")));
    add("action_save_reset_key", action(P_SAVE, FlixelDebugProtocol.SAVE_RESET_KEY, str("player.level")));
    add("action_save_save_now", action(P_SAVE, FlixelDebugProtocol.SAVE_SAVE_NOW));
    add("action_save_reset_all", action(P_SAVE, FlixelDebugProtocol.SAVE_RESET_ALL));
    add("action_save_select", action(P_SAVE, FlixelDebugProtocol.SAVE_SELECT, u16(1)));
    add("action_viewport_set_camera", action(P_VIEWPORT, FlixelDebugProtocol.VIEWPORT_SET_CAMERA, u8(1)));
    add("action_viewport_pick", action(P_VIEWPORT, FlixelDebugProtocol.VIEWPORT_PICK, f32(40.5f), f32(30f)));
    add("action_viewport_move",
        action(P_VIEWPORT, FlixelDebugProtocol.VIEWPORT_MOVE, i32(2), f32(16f), f32(-4.25f)));
    add("action_viewport_pan", action(P_VIEWPORT, FlixelDebugProtocol.VIEWPORT_PAN, f32(-12f), f32(8.5f)));
    add("action_viewport_zoom",
        action(P_VIEWPORT, FlixelDebugProtocol.VIEWPORT_ZOOM, f32(1.25f), f32(160f), f32(90f)));
    add("action_viewport_reset_camera", action(P_VIEWPORT, FlixelDebugProtocol.VIEWPORT_RESET_CAMERA));
    add("action_capture_request", action(P_CAPTURE, FlixelDebugProtocol.CAPTURE_REQUEST, i32(7), u16(640)));
    add("action_input_key", action(P_INPUT, FlixelDebugProtocol.INPUT_KEY, bool(true), i32(62)));
    add("action_input_pointer", action(P_INPUT, FlixelDebugProtocol.INPUT_POINTER,
        u8(FlixelDebugProtocol.INPUT_POINTER_DOWN), u8(0), f32(40.5f), f32(30.25f)));
    add("action_input_scroll", action(P_INPUT, FlixelDebugProtocol.INPUT_SCROLL, f32(-2.5f)));
    add("action_input_text", action(P_INPUT, FlixelDebugProtocol.INPUT_TEXT, str(UNICODE)));
    add("action_input_click_object", action(P_INPUT, FlixelDebugProtocol.INPUT_CLICK_OBJECT, i32(12), u8(0)));
    add("action_custom_press", action(P_CUSTOM, FlixelDebugProtocol.CUSTOM_PRESS, u16(1)));
    add("action_custom_set_property", action(P_CUSTOM, FlixelDebugProtocol.CUSTOM_SET_PROPERTY, u16(0), val(3f)));
  }

  private DebugProtocolFixtures() {}

  /** One typed field. */
  record Op(char type, Object value) {
  }
}
