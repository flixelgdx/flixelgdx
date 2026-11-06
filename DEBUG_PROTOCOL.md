# Remote Debug Protocol

This document is the single source of truth for the wire protocol between a FlixelGDX game (the
**agent**) and the desktop debugger app (the **app**). The two sides share no code. If you can read
this file, you can write a compatible app in any language.

The Java implementation lives in `org.flixelgdx.debug.remote` (`FlixelDebugProtocol`,
`FlixelDebugMessageWriter`, `FlixelDebugMessageReader`). Binary test vectors live in
[`flixelgdx-test/src/test/resources/org/flixelgdx/debug/remote/fixtures/`](flixelgdx-test/src/test/resources/org/flixelgdx/debug/remote/fixtures/)
with a README listing the exact values each file encodes. An app implementation should copy that folder
and assert the same values.

Current version: **1.0**.

## Table of contents

1. [Transport](#transport)
2. [Encoding](#encoding)
3. [Messages](#messages)
4. [Property values](#property-values)
5. [Built-in panels](#built-in-panels)
6. [Custom panels](#custom-panels)
7. [Versioning](#versioning)

---

## Transport

- WebSocket **binary** messages only. Exactly one protocol message per WebSocket message. Text frames
  must be ignored.
- The **game is the WebSocket client** and the **app is the server**. The default URL is
  `ws://127.0.0.1:6510/`.
- Android devices reach the PC through `adb reverse tcp:6510 tcp:6510`, so the game still connects to
  `127.0.0.1:6510`. A game can be pointed at another host with `host[:port]` in the `-Dflixel.debugHost` system
  property (desktop), the `flixel.debugHost` intent extra (Android), or the `flixel.debugHost` URL query parameter
  (web). The game only connects in debug mode.
- The game limits a message to 16 MB and closes the connection with code 1009 above that. Separately, the game
  reads each message from the app into a 128 KB buffer (`FlixelDebugAgent.RECEIVE_BYTES`) and drops any app
  message larger than that without closing. Every app-to-game message in this spec is far smaller.
- Either side may close the connection at any time. The game reconnects every 2 seconds until it
  succeeds. After every reconnect the whole handshake starts over (`HELLO` first, then everything the
  game sends is re-sent in full).

Connection flow:

1. The game connects and sends `HELLO`.
2. The app replies with `HELLO_ACK`. If the major versions differ, the app closes the connection.
3. The game sends `PANEL_LIST`, then `PANEL_DATA` messages for panels.
4. The app sends `PING` about once a second; the game answers each with `PONG`.

## Encoding

All numbers are **little-endian**.

| Type     | Size     | Description |
|----------|----------|-------------|
| `u8`     | 1        | Unsigned 8-bit integer. |
| `u16`    | 2        | Unsigned 16-bit integer. |
| `i32`    | 4        | Signed 32-bit integer. |
| `i64`    | 8        | Signed 64-bit integer. |
| `f32`    | 4        | IEEE 754 single. |
| `f64`    | 8        | IEEE 754 double. |
| `bool`   | 1        | A `u8`, `0` for false and `1` for true. Readers treat any non-zero value as true. |
| `string` | 2 + n    | A `u16` byte length `n` followed by `n` UTF-8 bytes. At most 65535 bytes. The writer truncates on a UTF-8 character boundary, so the result is always valid UTF-8. Unpaired surrogates are written as `?`. |
| `bytes`  | 4 + n    | An `i32` length `n` (never negative) followed by `n` raw bytes. |
| `array<T>` | 2 + ... | A `u16` element count followed by that many elements of type `T`. At most 65535 elements; senders cap larger collections. |

There is no padding and no alignment. A "row" or "entry" below is just its fields written one after
another.

## Messages

Every message starts with a `u8` **message type**.

| Code | Name           | Direction     | Purpose |
|------|----------------|---------------|---------|
| 1    | `HELLO`        | game to app   | First message of every connection. |
| 2    | `HELLO_ACK`    | app to game   | Answers `HELLO`. |
| 3    | `PANEL_LIST`   | game to app   | The panels that currently exist. |
| 4    | `PANEL_DATA`   | game to app   | A payload for one panel. |
| 5    | `PANEL_ACTION` | app to game   | A user action for one panel. |
| 6    | `PING`         | app to game   | Round-trip probe. |
| 7    | `PONG`         | game to app   | Echo of `PING`. |

### HELLO (1)

| Field             | Type     |
|-------------------|----------|
| `protocolMajor`   | `u16`    |
| `protocolMinor`   | `u16`    |
| `platform`        | `string` (`desktop`, `android`, or `web`) |
| `device`          | `string` (device or OS description) |
| `screenWidth`     | `i32`    |
| `screenHeight`    | `i32`    |
| `gameTitle`       | `string` |
| `frameworkVersion`| `string` |

### HELLO_ACK (2)

| Field           | Type  |
|-----------------|-------|
| `protocolMajor` | `u16` |
| `protocolMinor` | `u16` |

The app closes the connection when the majors differ. Minors may differ freely.

### PANEL_LIST (3)

`array<panel>`, where each panel is:

| Field        | Type     | Notes |
|--------------|----------|-------|
| `index`      | `u16`    | Used by all later messages to refer to this panel. |
| `id`         | `string` | Stable identifier, for example `stats` or `custom.player`. |
| `name`       | `string` | Display name. |
| `kind`       | `u8`     | A panel kind (see below). |

The list is re-sent whenever panels are registered or unregistered. Indices may change between lists,
so always match panels by `id`.

Panel kinds:

| Code | Name        | Built-in `id` | Payload |
|------|-------------|---------------|---------|
| 0    | `CUSTOM`    | any           | [Custom panels](#custom-panels) |
| 1    | `STATS`     | `stats`       | [stats](#stats) |
| 2    | `LOGS`      | `logs`        | [logs](#logs) |
| 3    | `WATCHES`   | `watches`     | [watches](#watches) |
| 4    | `TRACKERS`  | `trackers`    | [trackers](#trackers) |
| 5    | `TEXTURES`  | `textures`    | [textures](#textures) |
| 6    | `CONTROLS`  | `controls`    | [controls](#controls) |
| 7    | `CONSOLE`   | `console`     | [console](#console) |
| 8    | `SCENE`     | `scene`       | [scene](#scene) |
| 9    | `INSPECTOR` | `inspector`   | [inspector](#inspector) |
| 10   | `SAVE`      | `save`        | [save](#save) |
| 11   | `VIEWPORT`  | `viewport`    | [viewport](#viewport) |

### PANEL_DATA (4)

| Field   | Type  |
|---------|-------|
| `panel` | `u16` (panel index) |
| payload | defined by the panel kind |

### PANEL_ACTION (5)

| Field    | Type  |
|----------|-------|
| `panel`  | `u16` (panel index) |
| `action` | `u8` action code (defined per panel kind) |
| payload  | defined by the action; empty when the action lists no fields |

Action codes are scoped to the panel kind, so `1` means different things on different panels.

### PING (6) and PONG (7)

| Field   | Type  |
|---------|-------|
| `nonce` | `i64` |

The app sends `PING` about once a second with any nonce it likes (a timestamp works well). The game
answers with `PONG` carrying the same nonce. The app measures the round trip itself.

## Property values

Inspector rows, watches, trackers, save rows, and custom panels share one value encoding.

A **value** is a `u8` kind followed by the data:

| Code | Name     | Data |
|------|----------|------|
| 1    | `BOOL`   | `bool` |
| 2    | `INT`    | `i32` |
| 3    | `LONG`   | `i64` |
| 4    | `FLOAT`  | `f32` |
| 5    | `DOUBLE` | `f64` |
| 6    | `STRING` | `string` |
| 7    | `ERROR`  | `string` message; used when reading the value threw an exception |

A **property row** is `string name`, `value`, `bool editable`.

`ERROR` values are only ever sent by the game. The app never sends them.

## Built-in panels

Unless a panel says otherwise, it has a single payload kind and no subtype byte. A panel with more than
one payload kind starts every payload with a `u8` **subtype**; this is the pattern for `textures`,
`console`, and `inspector`.

Arrays are `u16`-counted, see [Encoding](#encoding). A "text" cell below that is not specified as
a value is a plain `string`.

### stats

Id `stats`, kind 1. One sample per message, sent at the sample rate (default 20 Hz, range 1 to 30,
changed through `controls`). The app keeps the history.

Payload fields in order:

| # | Field         | Type  | Notes |
|---|---------------|-------|-------|
| 1 | `fps`         | `f32` | |
| 2 | `frameMs`     | `f32` | Whole frame time. |
| 3 | `cpuMs`       | `f32` | |
| 4 | `presentMs`   | `f32` | |
| 5 | `draws`       | `i32` | Draw calls. |
| 6 | `triangles`   | `i32` | |
| 7 | `vertices`    | `i32` | |
| 8 | `passes`      | `i32` | Render passes. |
| 9 | `submits`     | `i32` | Queue submits. |
| 10| `pipelines`   | `i32` | |
| 11| `heapMb`      | `f32` | |
| 12| `nativeMb`    | `f32` | |
| 13| `textureMb`   | `f32` | |
| 14| `ringKb`      | `f32` | |
| 15| `objectCount` | `i32` | |
| 16| `sendKbps`    | `f32` | Agent send rate in KB/s. |
| 17| `recvKbps`    | `f32` | Agent receive rate in KB/s. |
| 18| `paused`      | `bool`| |
| 19| `elapsed`     | `f64` | Elapsed game time in seconds. |
| 20| `camX`        | `f32` | |
| 21| `camY`        | `f32` | |
| 22| `camZoom`     | `f32` | |

No actions.

### logs

Id `logs`, kind 2.

Payload:

| Field     | Type |
|-----------|------|
| `backlog` | `bool` (true for the batch sent right after connecting) |
| lines     | `array<line>` |

Line:

| Field      | Type     | Notes |
|------------|----------|-------|
| `seq`      | `i64`    | Increasing sequence number. |
| `time`     | `f64`    | Seconds since game start. |
| `level`    | `u8`     | Log level, below. |
| `uncaught` | `bool`   | True for uncaught exceptions and stderr output. |
| `tag`      | `string` | |
| `callSite` | `string` | For example `Main.java:10`. |
| `message`  | `string` | Cut to 2000 characters plus ` (cut off)` by the agent. |
| `stack`    | `string` | Stack trace; may be empty. |

Log levels: `0` Debug, `1` Info, `2` Warn, `3` Error. The value `255` (`DEFAULT`) is only used by the
`controls` panel to mean "no override".

Actions:

| Code | Name    | Fields |
|------|---------|--------|
| 1    | `CLEAR` | none |

### watches

Id `watches`, kind 3. Payload: `array<row>` where a row is `string name`, `value`.

No actions.

### trackers

Id `trackers`, kind 4. Payload: `array<block>` where a block is `string name`, `array<row>` and a row is
`string name`, `value`.

No actions.

### textures

Id `textures`, kind 5. Subtypes:

| Code | Name        | Direction   |
|------|-------------|-------------|
| 1    | `LIST`      | game to app |
| 2    | `THUMBNAIL` | game to app |

`LIST` payload: `u8 subtype=1`, `array<entry>`, then totals.

Entry:

| Field       | Type     |
|-------------|----------|
| `id`        | `i32`    |
| `name`      | `string` (name or path) |
| `width`     | `i32`    |
| `height`    | `i32`    |
| `format`    | `string` |
| `gpuBytes`  | `i64`    |
| `users`     | `i32`    |
| `duplicate` | `bool` (the same file was loaded more than once) |

Totals (after the array): `i32 textureCount`, `i64 totalGpuBytes`, `i32 duplicateCount`.

What the fields mean:

- `id` is stable for as long as the texture stays in the list. It is only meaningful for `REQUEST_THUMBNAIL`.
- `name` is the asset path. Graphics made in code have no file and use `flixel://synthetic/N`.
- `format` is a short display name such as `RGBA8` or `BC7`. Do not parse it.
- `gpuBytes` is the memory the texture takes on the GPU, as far as the graphics backend knows.
- `users` is the number of **existing sprites** in the current state (and its open sub states, including sprites
  inside groups) that are showing the texture at the moment the list was built. It is not the asset manager's
  reference count.
- `duplicate` is true for every texture of a group of two or more that were loaded from the same file path.

Only textures that belong to a graphic tracked by the asset manager are listed. Textures the framework creates for
itself (the font atlas, render targets) are not. The game rebuilds the list about once a second while the panel is
enabled and sends it again only when something in it changed.

`THUMBNAIL` payload: `u8 subtype=2`, `i32 id`, `i32 width`, `i32 height`, `bytes png`.

See [Thumbnails](#thumbnails) for how they are made.

Actions:

| Code | Name                | Fields |
|------|---------------------|--------|
| 1    | `REQUEST_THUMBNAIL` | `i32 id` (texture id). The game answers with a `THUMBNAIL` payload. |

### controls

Id `controls`, kind 6. Single payload (state), sent on connect and whenever it changes:

| Field       | Type  | Notes |
|-------------|-------|-------|
| `paused`    | `bool`| |
| `hitboxes`  | `bool`| |
| `freeCam`   | `bool`| True while the free camera is requested (see below). |
| `rate`      | `u8`  | Sample rate in Hz, 1 to 30. |
| `logLevel`  | `u8`  | Log level, or `255` (`DEFAULT`) for the game's own setting. |
| `keybinds`  | `array<keybind>` | `string key`, `string description` |

The `keybinds` list is for display only. The game sends, in this order, the pause key (default `F3`), the
hitboxes key (`F2`), the free camera key (`F4`), then fixed rows for `Alt + Click` (select the object under the
mouse), `W A S D / Arrows` (move the free camera), `Wheel / Q / E` (zoom the free camera), `Shift` (4 times faster),
and `Right Drag` (pan the free camera). A hotkey that was disabled in the game is left out. Show the rows as given
instead of hard-coding the keys, because the three hotkeys are configurable.

The **free camera** flies the main camera (or the camera that [viewport](#viewport) actions chose) without
touching the game. Turning it on saves the camera's scroll,
zoom, scroll bounds, and follow target and stops following; turning it off restores them exactly. It pans at 600
screen pixels per second (4 times that with Shift), zooms by 1.25 per wheel notch between 0.1 and 10, and runs on real
time, so it works while the game is paused. The keys and the right mouse button it uses are hidden from the game
while it is on. `Alt + Click` selects the topmost object drawn under the mouse on the `inspector` panel and the game
does not receive that click. The selected object is outlined in yellow in the game window even when hitboxes are
off.

Actions:

| Code | Name            | Fields |
|------|-----------------|--------|
| 1    | `SET_PAUSED`    | `bool` |
| 2    | `STEP`          | none (advance one frame while paused) |
| 3    | `SET_HITBOXES`  | `bool` |
| 4    | `SET_FREECAM`   | `bool` |
| 5    | `SET_RATE`      | `u8` (clamped by the game to 1 to 30) |
| 6    | `SET_LOG_LEVEL` | `u8` (a log level or `255`) |

### console

Id `console`, kind 7. Subtypes:

| Code | Name       | Direction   |
|------|------------|-------------|
| 1    | `COMMANDS` | game to app |
| 2    | `RESULT`   | game to app |

`COMMANDS` payload: `u8 subtype=1`, `array<command>` where a command is `string name`, `string usage`,
`string description`. Sent on connect and whenever the set of commands changes.

`RESULT` payload: `u8 subtype=2`, `u8 status`, `string message`. Status: `0` Ran, `1` Failed,
`2` Unknown command.

Command output itself arrives through [`logs`](#logs).

The game registers these commands for the debugger itself (the `COMMANDS` list always includes them, with usage and
description, and a game can add more):

| Command | Usage | What it does |
|---------|-------|--------------|
| `select` | `select [path]` | Selects the object at a [scene path](#scene-paths) in the inspector, or the object under the mouse when no path is given. |
| `freecam` | `freecam [on\|off]` | Turns the free camera on or off (see [controls](#controls)). Without an argument it toggles. |
| `goto` | `goto <x> <y>` | Centers the main camera on a world position. It turns the free camera on first, so the follow target does not pull the camera away again. |

Actions:

| Code | Name  | Fields |
|------|-------|--------|
| 1    | `RUN` | `string line` (at most 500 characters; longer lines are rejected) |

### scene

Id `scene`, kind 8. Payload: a full tree snapshot, `array<node>`:

| Field         | Type     | Notes |
|---------------|----------|-------|
| `id`          | `i32`    | |
| `parent`      | `i32`    | Parent node id, or `-1` for a root. |
| `type`        | `string` | Type name. |
| `label`       | `string` | |
| `trackedRoot` | `bool`   | True for objects added through the inspector's `track()`. |

Trees larger than 65535 nodes are cut by the agent. Parents always appear before their children.

The `label` is the step the object takes in a [scene path](#scene-paths): `state` for the state, `substate` for a
sub state open on top of its parent, the decimal member index for a member of a group, and the tracked label for an
extra root. Joining the labels from the root down with dots gives a valid scene path, for example `state.3.0`. Empty
member slots are not sent, but they keep their index, so a label can skip numbers. An object that was added through
`track()` and also lives in the state appears once, inside the state, with `trackedRoot` set; a tracked object that is
not in the state appears as an extra root with `parent` `-1`.

The game builds the tree again about twice a second and sends a snapshot only when its shape changed (an object or a
group member count changed, a sub state opened or closed, a root was tracked). A moved or edited object does not
change the shape. `REQUEST_TREE` makes the game send a snapshot even when nothing changed.

Actions:

| Code | Name           | Fields |
|------|----------------|--------|
| 1    | `REQUEST_TREE` | none (the game answers with a fresh snapshot) |

### inspector

Id `inspector`, kind 9. Subtypes:

| Code | Name        | Direction   |
|------|-------------|-------------|
| 1    | `STATE`     | game to app |
| 2    | `THUMBNAIL` | game to app |

`STATE` payload: `u8 subtype=1`, `i32 selectedId` (`-1` if nothing is selected), `string path`,
`array<section>`, `bool hasTexture`.

`path` is the selected object's address in the scene (see [Scene paths](#scene-paths)). An object that is not
in the scene tree (for example a tracked object that is not a group) gets `TypeName#id` instead, which is not a
valid scene path.

Section: `string typeName`, `string displayName`, `array<property row>`.

`THUMBNAIL` payload: `u8 subtype=2`, `i32 id`, `i32 width`, `i32 height`, `bytes png`.

Actions:

| Code | Name                | Fields |
|------|---------------------|--------|
| 1    | `SELECT`            | `i32 id` |
| 2    | `SELECT_PATH`       | `string path` |
| 3    | `SELECT_PARENT`     | none |
| 4    | `CLEAR`             | none (clears the selection) |
| 5    | `GO_TO`             | none (moves the game camera to the selection) |
| 6    | `SET_PROPERTY`      | `i32 id`, `u16 sectionIndex`, `u16 propertyIndex`, `value` |
| 7    | `REQUEST_THUMBNAIL` | `i32 id` (object id) |

`SELECT_PATH` selects the object a [scene path](#scene-paths) points to and does nothing when the path leads
nowhere.

`SET_PROPERTY` refers to a row by its position in the latest `STATE` the game sent: `sectionIndex` is the
position in the `array<section>` and `propertyIndex` is the position in that section's rows. The game ignores
the action when `id` is not the currently selected object, when an index is out of range, or when the row is
not editable.

The game converts the value by the row's kind. A numeric row (int, long, float, or double) accepts any numeric
value kind and converts it like a Java cast, a bool row accepts only a bool, and a string row accepts only a
string. A value of any other kind is ignored and a warning is logged.

### Thumbnails

A thumbnail is a PNG picture of a texture (or, for the inspector, of the part of the texture a sprite shows). The
app asks with `REQUEST_THUMBNAIL`; the game reads the pixels back from the GPU, which takes a few frames, and then
sends one `THUMBNAIL` payload. There is no answer at all when the pixels cannot be read, so the app must not wait
for one forever. The inspector's `hasTexture` flag is only `true` when the game's graphics backend can read pixels
back, so the app can hide its thumbnail button otherwise.

- `width` and `height` are the size of the PNG. The longest side is at most 256 pixels. Bigger pictures are
  shrunk with nearest-neighbor sampling, so pixel art stays sharp, and smaller pictures keep their size.
- `png` is an ordinary 8-bit RGBA PNG. The game does not compress it (the deflate data uses stored blocks), so
  every PNG reader can open it, but the file is as large as the raw pixels.
- At most 8 thumbnails are open at once. Requests beyond that, and a repeat request for an id that is already open,
  are dropped.
- The picture shows the texture's current pixels, not what is drawn on screen.

### Scene paths

A scene path is a readable address for one object, used by the inspector's `path` field, the `SELECT_PATH`
action, and the `select` console command. It is a list of steps separated by dots:

- The first step is `state` (the current game state), `substate` (the sub state open on top of the state), or the
  label of an object the game registered as an extra root (a label may itself contain dots).
- Every later step is a decimal **member index** (the position in the group's member list, counting empty slots)
  or the word `substate`, which steps from a state into its open sub state. `substate.substate` is the sub state
  open on top of the first one.

Examples: `state` (the state itself), `state.3` (its fourth member), `state.3.0` (the first member of that
member, which must be a group), `substate.2`, and `managers.0` for a tracked root labeled `managers`. The
framework has no per-object debug names, so indexes are the only naming. Indexes change when members are added or
removed in front of an object, so a path is a snapshot, not a permanent address. Use the numeric `id` for a stable
reference.

### save

Id `save`, kind 10. Payload:

| Field     | Type |
|-----------|------|
| rows      | `array<row>`: `string path`, `string display`, `u8 valueKind` (a value kind code), `bool unsaved` |
| removed   | `array<string>` (paths that were removed and not yet saved) |
| `status`  | `string` |
| `isError` | `bool` (true when `status` describes a failure) |
| saves     | `array<string>` (the label of every save the app can pick from, see below) |
| `selected` | `u16` (the index in `saves` of the save the rows belong to) |

The game sends the payload again whenever something in it changed (it checks about once a second, and right
after every action), so the app can simply poll or listen.

**Saves.** A game may keep more than one save, such as a settings file and a few profiles. The panel edits one
of them at a time, and `saves` lists every one the app can switch to with `SELECT`:

- Entry 0 is always the game's main save (`Flixel.save`). The game looks it up again on every refresh, so the
  entry follows the game when it replaces that save. The other entries are the saves the game listed with
  `Flixel.debug.save.add(...)`, in the order they were added.
- A label is `name/slot` for a bound save with a slot, `name` for one without a slot, `(not bound)` for a save
  that has no name (or was destroyed), and `(no save)` for entry 0 while the game has no main save.
- `selected` is always a valid index into `saves`. When the game takes the selected save off the list, the panel
  goes back to entry 0.

**Rows.** The save is a tree of maps, lists, and plain values. The game flattens it into one row per plain value:

- `path` is the dotted path of the value, for example `player.coins` or `inventory.2.name`. List elements are
  named by their index. A key that contains a dot is written as it is, so a path can be ambiguous; the game tries
  every way of cutting it when it resolves one.
- Keys that are whole numbers sort by value and come first; all other keys sort alphabetically without regard to
  case. At most 300 rows are sent, and the status line shows `300+ values` when more exist.
- `display` is the value as text: strings are in double quotes (with `\"`, `\\`, `\n`, `\r`, and `\t` escaped and
  cut after 200 characters), booleans are `true` or `false`, numbers are plain digits, and an empty map or list is
  `{}` or `[]`. A `null` is `null`.
- `valueKind` is the kind of the stored value: `BOOL`, `INT`, `LONG`, `FLOAT`, `DOUBLE`, or `STRING` (strings, nulls,
  and empty containers). A saved file does not remember whether a number was whole, so numbers read back from disk
  are always `DOUBLE`.
- `unsaved` is true when the value differs from the copy in storage: it is new, or it has another type of value
  (text, number, boolean) or another value. `5` and `5.0` are the same number.
- `removed` lists the paths that are in storage but no longer in the data.

**Actions.**

| Code | Name        | Fields |
|------|-------------|--------|
| 1    | `SET`       | `string path`, `string text` (both at most 200 characters) |
| 2    | `RESET_KEY` | `string path` |
| 3    | `SAVE_NOW`  | none |
| 4    | `RESET_ALL` | none |
| 5    | `SELECT`    | `u16 index` (an index into `saves`) |

- `SET` changes a value that already exists and keeps its type. A string may be typed with or without double
  quotes (inside quotes `\"`, `\\`, and `\n` mean a quote, a backslash, and a line break). A boolean must be `true`
  or `false`. A number is parsed as the type the value already has, so a whole number rejects `1.5`. A path that
  does not exist, a `null`, a map, or a list is rejected. The result is in `status`.
- `RESET_KEY` removes the value (or a whole branch). The game has no table of default values, so the game uses
  the default it passes when it reads the value. Removing a list element moves the later elements down.
- `SAVE_NOW` writes the data to storage.
- `RESET_ALL` erases the data and the stored save at once. The app should ask the person to confirm before
  sending it.
- `SELECT` switches the panel to another save of `saves`. The game refreshes at once and sends the rows of the
  new save, with its summary in `status`. An index that is out of range is ignored.

Nothing reaches storage until `SAVE_NOW` (or the game's own save call).

### viewport

Id `viewport`, kind 11. A live map of what one camera shows, so the app can pick objects, drag them, and fly the
camera without the device's keyboard and mouse (a phone has neither). Single payload (state):

| Field         | Type  | Notes |
|---------------|-------|-------|
| `camera`      | `u8`  | Index of the camera the map shows (a position in the game's camera list). |
| `cameraCount` | `u8`  | How many cameras the game has (at most 255). `0` means there is no camera and no boxes. |
| `viewWidth`   | `f32` | Visible width of the camera in view units (its world width divided by its zoom). |
| `viewHeight`  | `f32` | Visible height in view units. |
| `zoom`        | `f32` | |
| `scrollX`     | `f32` | Camera scroll in world units. |
| `scrollY`     | `f32` | |
| `freeCam`     | `bool`| Same as `freeCam` of [controls](#controls). |
| `paused`      | `bool`| |
| `hitboxes`    | `bool`| |
| `selectedId`  | `i32` | The object selected in the [inspector](#inspector), or `-1`. |
| types         | `array<string>` | Type names (simple class names) that boxes refer to by index. |
| boxes         | `array<box>` | See below. |

Box: `i32 id`, `f32 x`, `f32 y`, `f32 width`, `f32 height`, `u16 typeIndex` (into `types`), `u8 flags`.

| Bit | Value | Meaning |
|-----|-------|---------|
| 0   | 1     | The object is the inspector's selection. |
| 1   | 2     | The object is a group (it has members of its own, which have their own boxes). |
| 2   | 4     | The object is not active, so it is drawn but not updated. |

Other bits are reserved and are 0.

**View space.** Every position is in the view space of the camera: `0, 0` is the top left corner of what the
camera shows and the visible area is `viewWidth` by `viewHeight`. Each object is placed with its own scroll factor,
so parallax layers and objects fixed to the screen are where they appear. To draw the map, scale the view to the
size of your canvas; no camera math is needed. A box is the area the object is drawn in (a sprite's graphic with its
offset and scale, widened to the bounding box when it is rotated; other objects use their hitbox), which is exactly
what a click hits. The box `id` is the same object id that [scene](#scene) and [inspector](#inspector) use.

**Which objects.** Boxes are listed in draw order, bottom first, so the last box that contains a point is the one
on top. Only objects that exist, are visible, are drawn on the camera, and touch the visible area are listed, and an
open sub state is drawn on top of the state below it (the state below only counts while it still draws). At most
1024 boxes are sent; when more objects are on screen, the ones drawn on top are kept. Read `types` again from every
message, because the indices are only valid inside the message they came in. The table may hold names that no box
uses.

**When it is sent.** The game samples the camera at the sample rate of [controls](#controls) and right after each
action, and sends the state only when something in it changed.

Actions:

| Code | Name           | Fields |
|------|----------------|--------|
| 1    | `SET_CAMERA`   | `u8 index` (a position in the camera list) |
| 2    | `PICK`         | `f32 viewX`, `f32 viewY` |
| 3    | `MOVE`         | `i32 id`, `f32 dx`, `f32 dy` (world units) |
| 4    | `PAN`          | `f32 dx`, `f32 dy` (view units) |
| 5    | `ZOOM`         | `f32 factor`, `f32 anchorViewX`, `f32 anchorViewY` |
| 6    | `RESET_CAMERA` | none |

- `SET_CAMERA` chooses the camera the map shows and the camera actions act on. While the index is past the end of
  the camera list, the game shows the main camera (index 0) and says so in `camera`.
- `PICK` selects the topmost object drawn at that point of the view in the inspector, the same way `Alt + Click`
  does in the game. When nothing is drawn there, the selection is cleared.
- `MOVE` moves an object by a distance in world units. It is a distance, not a position, so updates that arrive late
  or close together add up instead of fighting. Any object with a position can be moved, selected or not, and a
  group moves its members with it. An unknown id or an object without a position is ignored. For a drag, send the
  pointer's movement in view units; for an object with a scroll factor of 1 a view unit is a world unit.
- `PAN` moves the camera by a distance in view units: a positive `dx` moves the camera right, so the world slides
  left. To drag the world with a finger, send the finger's movement with the sign flipped. It turns the free camera
  of [controls](#controls) on for the camera the map shows (restoring any other camera the free camera was flying),
  so the camera stops following its target and the move is not undone.
- `ZOOM` multiplies the zoom by `factor` (for example `1.25` for one wheel notch in, `0.8` for one notch out) and
  keeps the point at `anchorViewX, anchorViewY` (in view space before the zoom) still on the screen, like a pinch.
  The zoom stays between 0.1 and 10. Like `PAN`, it turns the free camera on. A factor that is not positive is
  ignored.
- `RESET_CAMERA` turns the free camera off, which restores the camera's scroll, zoom, scroll bounds, and follow
  target exactly, and points the free camera back at the main camera.

Non-finite numbers are ignored. Pause, step, and hitboxes are actions of [controls](#controls); the viewport only
reports them.

## Custom panels

A panel with kind `CUSTOM` (0) is registered by game code and has any `id`. Payload:

| Field    | Type |
|----------|------|
| rows     | `array<property row>` |
| buttons  | `array<button>`: `u16 actionIndex`, `string label` |

Actions:

| Code | Name           | Fields |
|------|----------------|--------|
| 1    | `PRESS`        | `u16 actionIndex` (taken from the button) |
| 2    | `SET_PROPERTY` | `u16 propertyIndex` (position in the rows array), `value` |

`SET_PROPERTY` converts the value by the row's kind exactly as the inspector does (see
[inspector](#inspector)); mismatched kinds and read-only rows are ignored.

## Versioning

The version is `major.minor`, starting at **1.0**.

- A **minor** bump only adds fields at the **end** of payloads, or adds new message types, panel kinds,
  subtypes, or action codes. Readers **must ignore trailing bytes** they do not understand and **must ignore
  unknown action codes, subtypes, and panel kinds**.
- A **major** bump may break compatibility. The app closes the connection when the majors differ.
- Codes are never reused. A retired code stays reserved forever.

## Fixtures

Test vectors are in `flixelgdx-test/src/test/resources/org/flixelgdx/debug/remote/fixtures/`:

- `msg_*.bin`: whole messages (including the message type byte).
- `panel_*.bin`: whole `PANEL_DATA` messages.
- `action_*.bin`: whole `PANEL_ACTION` messages.
- `value_*.bin`: a single value with no message header.

The folder's `README.md` lists the exact values every file encodes.
