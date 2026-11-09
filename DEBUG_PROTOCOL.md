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
4. [Values](#values)
5. [Panels](#panels)
6. [Widgets](#widgets)
7. [Feeds](#feeds)
8. [Versioning](#versioning)

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
3. The game sends `PANEL_LIST` and `FEED_LIST`.
4. The app sends `PANEL_OPEN` for every panel it shows and `FEED_SUBSCRIBE` for every feed it needs.
5. The game sends a `PANEL_TREE` for every open panel, then `PANEL_PATCH` messages whenever an open panel changes,
   and `FEED_DATA` messages for subscribed feeds.
6. The app sends `PANEL_EVENT` when a person uses a widget and `FEED_ACTION` when a tool (such as an AI agent)
   asks the game for something.
7. The app sends `PING` about once a second; the game answers each with `PONG`.

### Panels and feeds

The game talks to the app through two separate channels:

- **Panels** are the user interface. The game builds every panel, the built-in ones included, out of
  [widgets](#widgets) and decides what every button, row, and checkbox does. The app only draws what it is told and
  reports what the person did. It has no built-in knowledge of any panel, so a game can replace, remove, or add
  panels freely.
- **Feeds** are typed data for programs, such as the app's tools for AI agents. A feed has a fixed format (the scene
  tree, the viewport boxes, the save rows, and so on) and fixed actions. Feeds have no user interface meaning, so
  replacing a panel never breaks a tool that reads a feed. A feed only sends data while the app subscribes to it.

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

| Code | Name             | Direction     | Purpose |
|------|------------------|---------------|---------|
| 1    | `HELLO`          | game to app   | First message of every connection. |
| 2    | `HELLO_ACK`      | app to game   | Answers `HELLO`. |
| 3    | `FEED_LIST`      | game to app   | The feeds that exist. |
| 4    | `FEED_DATA`      | game to app   | A payload for one feed. |
| 5    | `FEED_ACTION`    | app to game   | An action for one feed. |
| 6    | `PING`           | app to game   | Round-trip probe. |
| 7    | `PONG`           | game to app   | Echo of `PING`. |
| 8    | `PANEL_LIST`     | game to app   | The panels that currently exist. |
| 9    | `PANEL_TREE`     | game to app   | The whole widget tree of one panel. |
| 10   | `PANEL_PATCH`    | game to app   | Changes to the widgets of one panel. |
| 11   | `PANEL_EVENT`    | app to game   | Something the person did with a widget. |
| 12   | `PANEL_OPEN`     | app to game   | Starts or stops the updates of one panel. |
| 13   | `FEED_SUBSCRIBE` | app to game   | Starts or stops the data of one feed. |
| 14   | `APP_REQUEST`    | game to app   | Asks the app to show a panel, reset its layout, and so on. |

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

### PING (6) and PONG (7)

`PING`:

| Field   | Type  | Notes |
|---------|-------|-------|
| `nonce` | `i64` | |
| `rttMs` | `f32` | The round trip the app measured for the previous `PING`, in milliseconds, or `-1` when it has none yet. |

`PONG`:

| Field   | Type  |
|---------|-------|
| `nonce` | `i64` |

The app sends `PING` about once a second with any nonce it likes (a timestamp works well). The game
answers with `PONG` carrying the same nonce. The app measures the round trip itself and tells the game the result
in the next `PING`, so the game's own panels can show it.

### APP_REQUEST (14)

| Field     | Type | Notes |
|-----------|------|-------|
| `request` | `u8` | Below. |
| fields    |      | Defined by the request. |

| Code | Name           | Fields | What the app does |
|------|----------------|--------|-------------------|
| 1    | `SHOW_PANEL`   | `string id` | Shows the panel with that id and brings it to the front. |
| 2    | `HIDE_PANEL`   | `string id` | Closes the panel with that id (the person can open it again). |
| 3    | `RESET_LAYOUT` | none | Puts every panel back where its `dock` says. |
| 4    | `NOTIFY`       | `u8 tone`, `string text` | Shows a short message to the person. `tone` is a [tone](#style). |

An unknown id is ignored.

## Values

Widget properties, event arguments, inspector rows, watches, trackers, and save rows share one value encoding.

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
| 8    | `NULL`   | nothing; "no value" |
| 9    | `BYTES`  | `bytes` |
| 10   | `LIST`   | `array<value>`; the items may have different kinds |

A **property row** is `string name`, `value`, `bool editable`.

`ERROR` values are only ever sent by the game. The app never sends them.

Below, `LIST [a, b, c]` means a `LIST` value whose items are `a`, `b`, and `c` in that order, and `LIST<x>` means a
`LIST` value whose items are all `x`. A reader treats a missing item at the end of a list as its default and ignores
extra items at the end, so later versions can add items.

## Panels

A panel is one dockable tab of the app. The game builds its content as a tree of [widgets](#widgets) and sends it;
the app draws the tree and sends back what the person does. All logic lives in the game.

### PANEL_LIST (8)

`array<panel>`, where each panel is:

| Field   | Type     | Notes |
|---------|----------|-------|
| `index` | `u16`    | Used by all later panel messages to refer to this panel. |
| `id`    | `string` | Stable identifier, for example `logs` or `my.enemies`. |
| `title` | `string` | The name on the panel's tab. |
| `dock`  | `u8`     | Where the panel goes the first time: `0` the middle, `1` left, `2` right, `3` bottom. |
| `shown` | `bool`   | Whether the panel is shown the first time the app sees its id. |

The list is re-sent whenever panels are registered, unregistered, or replaced. Indices may change between lists,
so always match panels by `id`. `dock` and `shown` only matter for an id the app has never placed; after that the
app's saved layout wins.

**After every `PANEL_LIST` the game treats every panel as closed.** The app answers each list with a `PANEL_OPEN`
for every panel it shows.

### PANEL_OPEN (12)

| Field   | Type   |
|---------|--------|
| `panel` | `u16`  |
| `open`  | `bool` |

An open panel is updated and sent; a closed one is not. Opening a panel makes the game send its `PANEL_TREE` (in the
next frame), even when the panel was already open, so the app can also use it to ask for a fresh tree. The app keeps
a panel open while it is visible, and may also open it for a tool that reads it.

### PANEL_TREE (9)

| Field   | Type     |
|---------|----------|
| `panel` | `u16`    |
| `root`  | `widget` |

A `widget` is:

| Field      | Type            | Notes |
|------------|-----------------|-------|
| `type`     | `u8`            | A [widget type](#widget-types). |
| `id`       | `i32`           | Positive and unique inside the panel. |
| `name`     | `string`        | An optional stable name for tools, for example `pause`. Empty when the widget has none. |
| props      | `array<prop>`   | `u8 key`, `value`. Only properties that differ from their default are sent. |
| children   | `array<widget>` | Empty for widgets that hold no other widgets. |

A tree replaces everything the app knew about the panel. A widget keeps its `id` for as long as it exists, so the
app can carry over what only the app knows (scroll position, expanded tree nodes, half-typed text, column order)
from the old tree to the new one by matching ids. The game sends a new tree when it adds or removes widgets and a
patch when it only changes properties.

### PANEL_PATCH (10)

| Field   | Type        |
|---------|-------------|
| `panel` | `u16`       |
| ops     | `array<op>` |

An `op` is `i32 widget`, `u8 code`, `u8 key`, `value`:

| Code | Name     | Meaning |
|------|----------|---------|
| 1    | `SET`    | The value replaces the property. |
| 2    | `APPEND` | The value is a `LIST`; its items are added to the end of the property, which is a `LIST` too. When the widget has a `LIMIT`, the app then drops items from the front until at most `LIMIT` remain. |

Ops apply in order. An op for an unknown widget id is ignored. A patch never arrives before the panel's first tree.

### PANEL_EVENT (11)

| Field    | Type           |
|----------|----------------|
| `panel`  | `u16`          |
| `widget` | `i32`          |
| `event`  | `u8`           |
| args     | `array<value>` |

| Code | Name       | Args | Sent by |
|------|------------|------|---------|
| 1    | `CLICK`    | none | `BUTTON` |
| 2    | `CHANGE`   | the new `VALUE` (or `TEXT` for a text box) | `CHECKBOX`, `TOGGLE`, `DROPDOWN`, `TEXT_BOX`, `NUMBER`, `SLIDER`, `COLOR`, `TABS`, `EXPANDER`, `SPLIT` |
| 3    | `SUBMIT`   | `STRING text` | `TEXT_BOX` (Enter) |
| 4    | `SELECT`   | `LONG id`, or `NULL` when the selection was cleared | `TABLE`, `TREE` |
| 5    | `ACTIVATE` | `LONG id` | `TABLE`, `TREE` (double click or Enter on an item) |
| 6    | `MENU`     | `LONG id`, `INT item` (the position in `MENU`) | `TABLE`, `TREE` |
| 7    | `EDIT`     | `INT section`, `INT row`, the new value | `PROPERTIES` |
| 8    | `PICK`     | `FLOAT x`, `FLOAT y` (view units) | `CANVAS` |
| 9    | `DRAG`     | `LONG id`, `FLOAT dx`, `FLOAT dy` (view units) | `CANVAS` |
| 10   | `PAN`      | `FLOAT dx`, `FLOAT dy` (view units) | `CANVAS` |
| 11   | `ZOOM`     | `FLOAT factor`, `FLOAT x`, `FLOAT y` (view units) | `CANVAS` |

The app shows the result of an input right away (a ticked checkbox stays ticked) and does not wait for the game. When
the game does not accept a value, it sends the property again with the value it kept, and the app shows that. An
event for an unknown widget, or with arguments of the wrong kind, is ignored.

## Widgets

### Common properties

| Key | Name      | Value    | Default | Meaning |
|-----|-----------|----------|---------|---------|
| 1   | `VISIBLE` | `BOOL`   | true    | A hidden widget takes no space. |
| 2   | `ENABLED` | `BOOL`   | true    | A disabled widget is drawn dimmed and sends no events. |
| 3   | `TOOLTIP` | `STRING` | empty   | Shown when the pointer rests on the widget. |
| 4   | `WIDTH`   | `FLOAT`  | 0       | Preferred width in device-independent pixels; `0` sizes to the content. |
| 5   | `HEIGHT`  | `FLOAT`  | 0       | Preferred height; `0` sizes to the content. |
| 6   | `GROW`    | `FLOAT`  | 0       | Inside a `STACK`, the share of the leftover space this child takes. `0` takes none. |
| 7   | `STYLE`   | `INT`    | 0       | See [Style](#style). |

### Style

`STYLE` is a set of bits:

| Bits | Meaning |
|------|---------|
| 0-3  | The **tone**: `0` normal, `1` muted, `2` accent, `3` success, `4` warning, `5` error. |
| 4    | Bold. |
| 5    | Monospaced font. |
| 6    | Heading (a larger, bold font for titles). |
| 7    | Italic. |
| 8    | `TREE` nodes only: the node starts expanded. |

The app picks the actual colors and fonts so that every panel matches its theme.

### Widget-specific properties

| Key | Name              | Value |
|-----|-------------------|-------|
| 16  | `TEXT`            | `STRING` |
| 17  | `VALUE`           | depends on the widget |
| 18  | `ITEMS`           | `LIST`, depends on the widget |
| 19  | `MIN`             | `DOUBLE` |
| 20  | `MAX`             | `DOUBLE` |
| 21  | `STEP`            | `DOUBLE` |
| 22  | `PLACEHOLDER`     | `STRING` |
| 23  | `HORIZONTAL`      | `BOOL` |
| 24  | `SPACING`         | `FLOAT` |
| 25  | `COLUMNS`         | `LIST`, depends on the widget |
| 26  | `SELECTED`        | `LONG` (`-1` for none) |
| 27  | `MENU`            | `LIST<STRING>` |
| 28  | `LIMIT`           | `INT` |
| 29  | `FOLLOW`          | `BOOL` |
| 30  | `FILTER`          | `INT` (a widget id) |
| 31  | `SORT`            | `INT` |
| 32  | `IMAGE`           | `BYTES` (a PNG) or `NULL` |
| 33  | `SCALE`           | `FLOAT` |
| 34  | `SMOOTH`          | `BOOL` |
| 35  | `MULTILINE`       | `BOOL` |
| 36  | `READ_ONLY`       | `BOOL` |
| 37  | `CLEAR_ON_SUBMIT` | `BOOL` |
| 38  | `HISTORY`         | `INT` |
| 39  | `COMPLETIONS`     | `LIST` |
| 40  | `CONFIRM`         | `STRING` |
| 41  | `EXPANDED`        | `BOOL` |
| 42  | `LIVE`            | `BOOL` |
| 43  | `DECIMALS`        | `INT` |
| 44  | `SERIES`          | `LIST` |
| 45  | `SAMPLES`         | `LIST` |
| 46  | `VIEW`            | `LIST [FLOAT width, FLOAT height]` |
| 47  | `SHAPES`          | `LIST` |
| 48  | `INTERACT`        | `INT` |
| 49  | `SECTIONS`        | `LIST` |
| 50  | `WRAP`            | `BOOL` |

A property that a widget type does not list below is ignored for that type. Unknown keys are ignored.

### Widget types

| Code | Name        | Children | Properties and events |
|------|-------------|----------|-----------------------|
| 1    | `STACK`     | any      | Lays the children out in a line, top to bottom, or left to right when `HORIZONTAL` is true, with `SPACING` between them (default 6). A child with a `GROW` above 0 stretches. |
| 2    | `WRAP`      | any      | Lays the children out left to right and starts a new line when the width runs out. `SPACING` (default 6). |
| 3    | `GRID`      | any      | Puts the children in cells, row by row. `COLUMNS` is a `LIST<STRING>` of column sizes: `auto` (as wide as the widest cell), `*` or `2*` (a share of the leftover width), or a number of pixels such as `120`. `SPACING` (default 6). |
| 4    | `SPLIT`     | 2        | Two children with a bar between them that the person can drag. Side by side when `HORIZONTAL` is true, otherwise one above the other. `VALUE` (`DOUBLE`, default 0.5) is the share of the first child. Sends `CHANGE` with the new share when the person lets go of the bar. |
| 5    | `TABS`      | any      | One page per child. `ITEMS` is a `LIST<STRING>` of page titles in child order. `VALUE` (`INT`) is the open page. Sends `CHANGE` when the person opens another page. |
| 6    | `SCROLL`    | 1        | Scrolls its child when it is taller (or wider) than the space it gets. |
| 7    | `EXPANDER`  | 1        | A header (`TEXT`) that shows or hides its child. `EXPANDED` (default true). Sends `CHANGE` with the new `BOOL`. |
| 8    | `LABEL`     | none     | `TEXT`. `WRAP` breaks long text into lines (default false: long text is cut with an ellipsis). The person can select and copy the text. |
| 9    | `BUTTON`    | none     | `TEXT`. Sends `CLICK`. When `CONFIRM` is not empty the app first asks the person that question and only sends `CLICK` on yes. |
| 10   | `CHECKBOX`  | none     | `TEXT`, `VALUE` (`BOOL`). Sends `CHANGE`. |
| 11   | `TOGGLE`    | none     | A button that stays pressed. `TEXT`, `VALUE` (`BOOL`). Sends `CHANGE`. |
| 12   | `DROPDOWN`  | none     | `ITEMS` (`LIST<STRING>`), `VALUE` (`INT`, the chosen item, `-1` for none). Sends `CHANGE` with the `INT`. |
| 13   | `TEXT_BOX`  | none     | See [Text boxes](#text-boxes). |
| 14   | `NUMBER`    | none     | A number field with up and down buttons. `VALUE`, `MIN`, `MAX`, `STEP` (all `DOUBLE`), `DECIMALS` (`INT`, default 2; `0` for whole numbers). Sends `CHANGE` with a `DOUBLE`. |
| 15   | `SLIDER`    | none     | `VALUE`, `MIN` (default 0), `MAX` (default 1), `STEP` (`0` for any value). Sends `CHANGE` with a `DOUBLE` when the person lets go of the thumb, or on each arrow key. |
| 16   | `PROGRESS`  | none     | A bar. `VALUE` (`DOUBLE`, 0 to 1) and `TEXT` drawn over the bar. |
| 17   | `IMAGE`     | none     | `IMAGE` (a PNG, or `NULL` for none). `SCALE` (default 1; `0` fits the image into the widget). `SMOOTH` (default false: pixel art stays sharp). |
| 18   | `SEPARATOR` | none     | A thin line. |
| 19   | `TABLE`     | none     | See [Tables](#tables). |
| 20   | `TREE`      | none     | See [Trees](#trees). |
| 21   | `PROPERTIES`| none     | See [Property grids](#property-grids). |
| 22   | `GRAPH`     | none     | See [Graphs](#graphs). |
| 23   | `CANVAS`    | none     | See [Canvases](#canvases). |
| 24   | `COLOR`     | none     | A color picker. `VALUE` (`INT`, `0xAARRGGBB`). Sends `CHANGE` with the `INT`. |

A widget of an unknown type is drawn as nothing, together with its children.

### Text boxes

`TEXT_BOX` properties:

- `TEXT`: the text. The app keeps what the person typed until the game sets `TEXT` again.
- `PLACEHOLDER`: shown while the box is empty.
- `MULTILINE`: allows line breaks. A multiline box never sends `SUBMIT`.
- `READ_ONLY`: the person can select and copy but not type.
- `LIVE`: sends `CHANGE` with the text as the person types (the app may wait up to 150 ms between changes).
  Without it, the box only sends `SUBMIT`.
- `CLEAR_ON_SUBMIT`: empties the box after `SUBMIT`.
- `HISTORY`: the number of submitted lines Up and Down bring back (`0`, the default, turns it off).
- `COMPLETIONS`: a `LIST` of `LIST [STRING name, STRING usage, STRING description]`. When it is not empty, the box
  behaves like a command line: the first word of the text is matched against the names as a case-insensitive
  prefix and the matches (or the usage and description of a single match) are shown under the box; Tab completes
  the name, and repeated Tab or Shift+Tab cycles through several matches.

### Tables

`TABLE` properties:

- `COLUMNS`: `LIST` of `LIST [STRING title, FLOAT width, INT flags]`. A `width` of `0` shares the leftover width.
  Flags: `1` the person can sort by this column, `2` align to the right, `4` monospaced font.
- `ITEMS`: the rows, a `LIST` of `LIST [LONG id, INT style, LIST cells, STRING detail]`.
  - `id` is the row's identity in `SELECT`, `ACTIVATE`, and `MENU`.
  - `style` is a [style](#style) for the whole row.
  - A cell is any value and is shown as text. A cell can also be `LIST [STRING text, value sortKey]` to show one
    thing and sort by another (for example `"64 x 64"` sorted by the pixel count).
  - `detail` is extra text (for example a stack trace), shown as a tooltip. Empty when there is none.
- `SELECTED`: the selected row id, `-1` for none.
- `MENU`: the items of the right-click menu of a row. The app always adds `Copy row` and, for a row with a detail,
  `Copy detail`, which it handles itself.
- `LIMIT`: the most rows kept after an `APPEND`.
- `FOLLOW`: keeps the newest row in view after an `APPEND` while the person has not scrolled away from it.
- `FILTER`: the id of a `TEXT_BOX` in the same panel. The app hides the rows in which no cell contains that box's text
  (ignoring case).
- `SORT`: the column the rows are sorted by, counting from 1; negative sorts descending; `0` keeps the game's order.
  When the person clicks a sortable header, the app sorts by it on its own.

Sorting and filtering only change what the app shows. Events always carry row ids.

### Trees

`TREE` properties:

- `ITEMS`: the nodes, a `LIST` of `LIST [LONG id, LONG parent, STRING text, STRING detail, INT style, LIST badges]`.
  `parent` is `-1` for a root, and parents always come before their children. `detail` is drawn muted after the
  text. A badge is `LIST [STRING text, INT tone]`, drawn as a small colored tag. Style bit 8 expands the node the
  first time the app sees its id; after that the app remembers what the person expanded.
- `SELECTED`, `MENU`, and `FILTER` work like in a table. A filtered tree keeps the parents of every matching node.

### Property grids

`PROPERTIES` shows named values in sections, like an object inspector.

- `SECTIONS`: a `LIST` of `LIST [STRING title, BOOL expanded, LIST rows]`. A row is
  `LIST [STRING name, value, BOOL editable]`.
- An editable row gets an editor that fits its value: a checkbox for `BOOL`, a number field for `INT`, `LONG`,
  `FLOAT`, and `DOUBLE`, and a text field for `STRING`. An `ERROR` value is shown in the error tone and is never
  editable.
- When the person changes a value, the app sends `EDIT` with the section index, the row index, and the new value of
  the row's kind.

### Graphs

`GRAPH` draws lines over time.

- `TEXT`: the title.
- `SERIES`: a `LIST` of `LIST [STRING name, INT color, STRING unit]`. `color` is `0xAARRGGBB`, or `0` to let the app
  pick. The latest value of every series is shown next to the title with its unit.
- `SAMPLES`: a `LIST` of samples, oldest first, where a sample is a `LIST<FLOAT>` with one value per series. The game
  adds samples with `APPEND`.
- `LIMIT`: the most samples kept (default 120).
- `MIN` and `MAX`: the range of the vertical axis. A missing value, or `NaN`, makes the app fit the axis to the data.

### Canvases

`CANVAS` draws boxes in a 2D view, like a map of a game camera.

- `VIEW`: the size of the view in view units. The app scales it to fit the widget and keeps its aspect ratio.
- `IMAGE`: an optional picture drawn under the shapes, stretched over the whole view. `SMOOTH` as for `IMAGE`.
- `SHAPES`: a `LIST` of `LIST [LONG id, FLOAT x, FLOAT y, FLOAT width, FLOAT height, INT style, STRING label]`, in
  drawing order, bottom first. Positions are in view units with `0, 0` at the top left. Style bits: `0-3` the tone,
  `4` a thick outline (for a selection), `5` a dashed outline, `6` dimmed, `7` filled. The app shows the label of the
  topmost shape under the pointer.
- `INTERACT`: what the person may do, as bits:

| Bit | Value | Gesture | Event |
|-----|-------|---------|-------|
| 0   | 1     | Left click | `PICK` with the point in view units. |
| 1   | 2     | Left drag on a shape | `DRAG` with the movement since the last `DRAG`, in view units. The app moves the shape on its own while dragging, until the next `SHAPES` arrives. |
| 2   | 4     | Right or middle drag | `PAN` with the movement of the pointer since the last `PAN`, in view units. |
| 3   | 8     | Mouse wheel | `ZOOM` with the factor (`1.25` per notch in, `0.8` per notch out) and the point under the pointer. |

## Feeds

A feed is typed game data for programs. Every feed has a fixed `kind` that defines its payloads and actions below.
Feeds send nothing until the app subscribes to them, so a person who only looks at panels pays nothing for them.

### FEED_LIST (3)

`array<feed>`, where each feed is:

| Field   | Type     | Notes |
|---------|----------|-------|
| `index` | `u16`    | Used by all later feed messages to refer to this feed. |
| `id`    | `string` | Stable identifier, for example `scene`. |
| `kind`  | `u8`     | A feed kind (see below). |

The list is sent once per connection, right after `HELLO_ACK`. Every feed starts unsubscribed.

Feed kinds (code `0` is reserved):

| Code | Name        | `id`        | Payload |
|------|-------------|-------------|---------|
| 1    | `STATS`     | `stats`     | [stats](#stats) |
| 2    | `LOGS`      | `logs`      | [logs](#logs) |
| 3    | `WATCHES`   | `watches`   | [watches](#watches) |
| 4    | `TRACKERS`  | `trackers`  | [trackers](#trackers) |
| 5    | `TEXTURES`  | `textures`  | [textures](#textures) |
| 6    | `CONTROLS`  | `controls`  | [controls](#controls) |
| 7    | `CONSOLE`   | `console`   | [console](#console) |
| 8    | `SCENE`     | `scene`     | [scene](#scene) |
| 9    | `INSPECTOR` | `inspector` | [inspector](#inspector) |
| 10   | `SAVE`      | `save`      | [save](#save) |
| 11   | `VIEWPORT`  | `viewport`  | [viewport](#viewport) |
| 12   | `CAPTURE`   | `capture`   | [capture](#capture) |
| 13   | `INPUT`     | `input`     | [input](#input) |

### FEED_SUBSCRIBE (13)

| Field  | Type   |
|--------|--------|
| `feed` | `u16`  |
| `on`   | `bool` |

Subscribing makes the game send the feed's whole current state (for `logs`, the backlog) and then keep it up to
date. Unsubscribing stops it. Actions work whether or not the feed is subscribed.

### FEED_DATA (4)

| Field  | Type  |
|--------|-------|
| `feed` | `u16` (feed index) |
| payload | defined by the feed kind |

### FEED_ACTION (5)

| Field    | Type  |
|----------|-------|
| `feed`   | `u16` (feed index) |
| `action` | `u8` action code (defined per feed kind) |
| payload  | defined by the action; empty when the action lists no fields |

Action codes are scoped to the feed kind, so `1` means different things on different feeds. The game treats every
feed action as coming from a tool, not a person: objects a feed action selects, moves, edits, or clicks are marked as
touched by an agent, and the game's own panels show that mark.

Unless a feed says otherwise, it has a single payload kind and no subtype byte. A feed with more than
one payload kind starts every payload with a `u8` **subtype**; this is the pattern for `textures`,
`console`, `inspector`, and `capture`.

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
`controls` feed to mean "no override".

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
itself (the font atlas, render targets) are not. The game rebuilds the list about once a second while the feed is
enabled and sends it again only when something in it changed.

`THUMBNAIL` payload: `u8 subtype=2`, `i32 id`, `i32 width`, `i32 height`, `bytes png`.

See [Thumbnails](#thumbnails) for how they are made.

Actions:

| Code | Name                | Fields |
|------|---------------------|--------|
| 1    | `REQUEST_THUMBNAIL` | `i32 id` (texture id). The game answers with a `THUMBNAIL` payload, which is empty when no picture can be made (see [Thumbnails](#thumbnails)). |

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
while it is on. `Alt + Click` selects the topmost object drawn under the mouse in the `inspector` feed and the game
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

**Debug nodes.** Besides groups, the game descends into objects that implement the framework's `FlixelDebugNode`
interface (`getDebugChildCount()`, `getDebugChild(int)`, and `getDebugBounds(camera, out)`), for example user interface
widgets that are neither a group nor a sprite. A node's children are listed like group members: the `label` is the child
index, an empty slot keeps its index and is not sent, and the node gets an object `id` like any other object. When an
object is both a group (or a sprite) and a debug node, the debug node wins for its children and its box. Nothing on the
wire tells the two apart, so the app needs no change to show them.

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
sends one `THUMBNAIL` payload. Every request gets exactly one answer. When no picture can be made (an unknown id, an
object without a texture such as text, a compressed texture, a backend that cannot read pixels, a failed read, or too
many open requests), the answer is an **empty thumbnail**: `width` and `height` are `0` and `png` has no bytes. It
usually arrives in the same frame. Games built before this rule sent nothing in those cases, so the app should still
give up after a timeout. The inspector's `hasTexture` flag is only `true` when the game's graphics backend can read pixels
back, so the app can hide its thumbnail button otherwise.

- `width` and `height` are the size of the PNG. The longest side is at most 256 pixels. Bigger pictures are
  shrunk with nearest-neighbor sampling, so pixel art stays sharp, and smaller pictures keep their size.
- `png` is an ordinary 8-bit RGBA PNG. The game does not compress it (the deflate data uses stored blocks), so
  every PNG reader can open it, but the file is as large as the raw pixels.
- At most 8 thumbnails are open at once. Requests beyond that get an empty thumbnail. A repeat request for an id
  that is already open, or whose answer is waiting to be sent, shares that one answer.
- The picture shows the texture's current pixels, not what is drawn on screen.

### Scene paths

A scene path is a readable address for one object, used by the inspector's `path` field, the `SELECT_PATH`
action, and the `select` console command. It is a list of steps separated by dots:

- The first step is `state` (the current game state), `substate` (the sub state open on top of the state), or the
  label of an object the game registered as an extra root (a label may itself contain dots).
- Every later step is a decimal **member index** (the position in the group's member list, or the child index of a
  debug node, counting empty slots) or the word `substate`, which steps from a state into its open sub state. `substate.substate` is the sub state
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

**Saves.** A game may keep more than one save, such as a settings file and a few profiles. The feed edits one
of them at a time, and `saves` lists every one the app can switch to with `SELECT`:

- Entry 0 is always the game's main save (`Flixel.save`). The game looks it up again on every refresh, so the
  entry follows the game when it replaces that save. The other entries are the saves the game listed with
  `Flixel.debug.save.add(...)`, in the order they were added.
- A label is `name/slot` for a bound save with a slot, `name` for one without a slot, `(not bound)` for a save
  that has no name (or was destroyed), and `(no save)` for entry 0 while the game has no main save.
- `selected` is always a valid index into `saves`. When the game takes the selected save off the list, the feed
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
- `SELECT` switches the feed to another save of `saves`. The game refreshes at once and sends the rows of the
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
| `actionCount` | `i32` | How many actions this feed has received, counted before each one is handled, so even an action that changes nothing raises it. It starts at `0` and wraps around. Added in 1.1; a reader treats a message that ends before it as `0`. |

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
size of your canvas; no camera math is needed. A box is the object's **hitbox** (its collision box, the same one the
game outlines when hitboxes are on), not the area its graphic covers, so a sprite's offset, scale, angle, and
animation frame do not change it. One exception is the selected box (flag bit 0): it is the area the object's
graphic covers (its offset, scale, and frame, widened to the bounding box when rotated), the same area the game
outlines in yellow, so both show the same selection outline. `PICK` always tests hitboxes, so a tap selects the
hitbox under it even where the selected box is larger. A group (any `FlixelGroupable`) is the other exception: its
box is the smallest box around the boxes of its members that are drawn on the camera (nested groups included), so
it outlines what it holds even when it has no size of its own. A group with nothing drawn uses its own hitbox when
it has one, and otherwise has no box. `PICK` treats that box like any other, so the space between members selects
the group. The box `id` is the same object id that [scene](#scene) and [inspector](#inspector) use.

**Debug nodes.** An object that implements `FlixelDebugNode` (see [scene](#scene)) measures its own box in the
camera's view space instead of using a hitbox, and returns no box when it is not drawn on that camera. Its children are
walked like group members, and a node with children carries the group flag. `PICK`, `Alt + Click`, the selection
outline, `MOVE` (when the node has a position), and `CLICK_OBJECT` of [input](#input) all use that box.

**Which objects.** Boxes are listed in draw order, bottom first, so the last box that contains a point is the one
on top. Only objects that exist, are visible, are drawn on the camera, and touch the visible area are listed, and an
open sub state is drawn on top of the state below it (the state below only counts while it still draws). At most
1024 boxes are sent; when more objects are on screen, the ones drawn on top are kept. Read `types` again from every
message, because the indices are only valid inside the message they came in. The table may hold names that no box
uses.

**When it is sent.** The game samples the camera at the sample rate of [controls](#controls) and right after each
action, and sends the state only when something in it changed. Because `actionCount` is part of the state, every
action is followed by a state message, even one that changes nothing else.

**Waiting for an action.** A state that the game sampled, or already sent, before an action arrived can reach the app
after the action was sent, so do not take just the next state as the answer. The game handles actions in the order
they arrive, so number the viewport actions you send (1, 2, 3, ...) in the order they go out, and remember the
`actionCount` of a state that arrived before you sent any. Your action number `n` has been handled once a state's
`actionCount` reaches that remembered count plus `n` (allowing for the wrap). Waiting for just any higher count is not
enough when more than one tool sends actions at the same time, because another tool's action raises it too.

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
- `PICK` selects the topmost object whose hitbox contains that point of the view in the inspector. It searches in
  the same order as `Alt + Click` in the game, but `Alt + Click` tests the drawn graphic instead. When no hitbox
  contains the point, the selection is cleared.
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

### capture

Id `capture`, kind 12. Takes a picture of the game's screen. Subtypes:

| Code | Name    | Direction   |
|------|---------|-------------|
| 1    | `STATE` | game to app |
| 2    | `IMAGE` | game to app |

`STATE` payload: `u8 subtype=1`, `bool supported`. Sent on connect and whenever it changes. It is `false` when the game's
graphics backend cannot read the finished frame back (the app should then hide its capture button) and `true` on the
WebGPU renderer, which every current platform (desktop, Android, web) uses.

`IMAGE` payload: `u8 subtype=2`, `i32 requestId`, `i32 width`, `i32 height`, `bytes png`.

Actions:

| Code | Name      | Fields |
|------|-----------|--------|
| 1    | `REQUEST` | `i32 requestId`, `u16 maxSide` |

- `REQUEST` asks for a picture of the next finished frame. `requestId` is any number the app chooses and comes back in
  `IMAGE`. `maxSide` is the most pixels the picture may have on its longest side; the game clamps it to 64 through 2048.
- The picture is the **finished game frame**: everything the cameras and the game's own overlay drew, at the size the
  game drew it (the render resolution when one is set, otherwise the window). It is taken **before** the debugger draws
  its own outlines (hitboxes and the selection), so those are not in it. Pixels that were not drawn are written fully
  opaque. It is the frame drawn after the request arrived, so it also works while the game is paused.
- The game reads the frame back from the GPU, which takes a few frames, shrinks it with nearest-neighbor sampling (a
  frame that is already small enough keeps its size), and encodes it as the same uncompressed 8-bit RGBA PNG as a
  [thumbnail](#thumbnails). Read `width` and `height` from the answer instead of computing them.
- **One request is open at a time.** A `REQUEST` that arrives while another one has not been answered yet is dropped, and
  so is one that arrives while `supported` is `false`.
- **There is no answer at all** when the capture fails (the read failed, the frame could not be encoded, or it took
  longer than 10 seconds), so the app must time out.
- The PNG is as large as the raw pixels, and a message may not exceed 16 MB (the game closes the connection above that).
  Because of this, the game shrinks the picture further than `maxSide` when the PNG would pass 15 MB, which only happens
  for an almost square picture near the largest sizes (a 2048 by 2048 picture would be 16.8 MB). A 16:9 picture at
  `maxSide` 2048 is about 9.4 MB.

### input

Id `input`, kind 13. Presses keys, moves and clicks a pointer, scrolls, and types text in the game, through the same input
managers real input uses, so game code and user interface widgets see it exactly like a person's input. Single payload
(state), sent on connect and whenever it changes:

| Field   | Type | Notes |
|---------|------|-------|
| `flags` | `u8` | What the game supports, below. |

| Bit | Value | Meaning |
|-----|-------|---------|
| 0   | 1     | `KEY` works. |
| 1   | 2     | `POINTER` and `CLICK_OBJECT` work. |
| 2   | 4     | `SCROLL` works. |
| 3   | 8     | `TEXT` works. |
| 4   | 16    | Touch input is on, so a pointer that presses the left button also starts touch pointer 0. |

Other bits are reserved and are 0.

Actions:

| Code | Name           | Fields |
|------|----------------|--------|
| 1    | `KEY`          | `bool down`, `i32 key` |
| 2    | `POINTER`      | `u8 phase`, `u8 button`, `f32 viewX`, `f32 viewY` |
| 3    | `SCROLL`       | `f32 amount` |
| 4    | `TEXT`         | `string text` |
| 5    | `CLICK_OBJECT` | `i32 id`, `u8 button` |

- `KEY` presses (`down` true) or releases a key. `key` is the framework's key code (`FlixelKey`, which is not an ASCII
  code and not an operating system scan code), from 0 to 255. Other values are ignored. An injected key is visible to every
  way the game reads input: `pressed`, `rawPressed`, `justPressed`, `justReleased`, `firstPressed`, and keyboard listeners.
- `POINTER` moves (phase `0`), presses (`1`), or releases (`2`) a pointer. `button` is `0` left, `1` right, or `2` middle
  (3 and 4 are the extra buttons) and is used by phases `1` and `2`; a press and a release also move the pointer to the
  position first. The position is in the **view space** of the camera that the [viewport](#viewport) feed shows
  (`SET_CAMERA`), the same space as its boxes. The game converts it to window pixels, so `Flixel.mouse.getWorldX(camera)`
  lands on the point within a pixel of rounding. The pointer stays where it was put until the real mouse moves or the
  app disconnects.
- `SCROLL` turns the mouse wheel. A positive amount scrolls down, like a wheel turned towards the user.
- `TEXT` is delivered as typed characters (the way text from a keyboard arrives), so text boxes receive it. It presses no
  key, so use `KEY` for Enter, Backspace, and arrows. Text longer than 1000 characters is cut.
- `CLICK_OBJECT` clicks the center of the object's box (the same box [viewport](#viewport) reports) on the first camera
  that draws the object: the camera the viewport shows is tried first, then the other cameras in order. The pointer moves
  to that spot in window pixels, `button` goes down, and in the next frame it goes up. An unknown `id`, or an object that
  no camera draws when the click runs, does nothing (the game logs a warning).

**Timing.** The game puts actions in a queue of at most 256 entries and applies it once per frame, before it reads the
input devices. A key or button transition needs a frame of its own, or `justPressed` and `justReleased` would never be
seen for a quick tap. So the game applies **at most one key or button transition per frame** (a `KEY`, a `POINTER` press
or release, or one stage of `CLICK_OBJECT`) and leaves the rest for the next frames. Pointer moves, `SCROLL`, and `TEXT` are
not transitions and are all applied in the frame they reach the front of the queue. A tap (press, then release) therefore
takes two frames. Actions beyond the queue's size are dropped.

**While paused.** The queue waits while the game is paused. It is applied on the next `STEP` of [controls](#controls)
(that frame sees the transition as `justPressed` or `justReleased`), or when the game resumes. A paused game never sees
input it cannot act on, and an app that steps one frame at a time knows exactly what the game saw in each frame.

**Free camera.** [The free camera](#controls) hides its keys, the right mouse button, and the wheel from the game and
reads them to fly the camera. Injected input is never hidden from the game, and the free camera never reacts to it, so an
agent can press `W` to walk the player while the free camera is on.

**Disconnect.** When the connection to the app is lost, the game releases every key and button it was holding, hands the
pointer back to the real mouse, and empties the queue, so nothing stays stuck.


## Versioning

The version is `major.minor`, starting at **1.0**.

- A **minor** bump only adds fields at the **end** of payloads, or adds new message types, feed kinds,
  subtypes, action codes, app requests, widget types, property keys, or events. Readers **must ignore trailing
  bytes** they do not understand and **must ignore unknown action codes, subtypes, feed kinds, app requests, widget
  types, property keys, and events**.
- A **major** bump may break compatibility. The app closes the connection when the majors differ.
- Codes are never reused. A retired code stays reserved forever.

## Fixtures

Test vectors are in `flixelgdx-test/src/test/resources/org/flixelgdx/debug/remote/fixtures/`:

- `msg_*.bin`: whole messages (including the message type byte).
- `feed_*.bin`: whole `FEED_DATA` messages.
- `action_*.bin`: whole `FEED_ACTION` messages.
- `panel_*.bin`: whole `PANEL_LIST`, `PANEL_TREE`, `PANEL_PATCH`, `PANEL_EVENT`, and `PANEL_OPEN` messages.
- `value_*.bin`: a single value with no message header.

The folder's `README.md` lists the exact values every file encodes.
