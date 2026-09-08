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
package org.flixelgdx.backend.html5.debug;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelCamera;
import org.flixelgdx.backend.FlixelRuntimeMode;
import org.flixelgdx.backend.html5.FlixelHtml5Launcher;
import org.flixelgdx.collections.FlixelArray;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugOverlay;
import org.flixelgdx.debug.FlixelDebugTrackerEntry;
import org.teavm.jso.JSBody;

/**
 * DOM based debug overlay for the HTML5 (TeaVM + WebGL2) backend, built to mirror the desktop
 * Dear ImGui overlay ({@code FlixelImGuiDebugOverlay}) as closely as the browser allows.
 *
 * <h2>Why the DOM instead of a GUI toolkit</h2>
 *
 * <p>The desktop backend renders its debugger with Dear ImGui submitted through bgfx. A browser has
 * no such toolkit, and pulling one in would bloat every web build. The browser already ships a
 * perfectly good, hardware-accelerated UI layer though: the DOM. This overlay therefore builds a
 * single docked panel out of ordinary HTML elements layered on top of the game canvas, exactly the
 * way {@code FlixelHtml5Alerter} and the crash overlay in
 * {@link org.flixelgdx.backend.html5.FlixelHtml5RuntimeDevice FlixelHtml5RuntimeDevice} build their
 * dialogs. Nothing here touches WebGL.
 *
 * <h2>Parity with the desktop overlay</h2>
 *
 * <p>The panel exposes the same information the desktop overlay does, split across tabs rather than
 * floating windows (a single docked panel reads far better in a browser than draggable windows):
 * <ul>
 *   <li><b>Stats</b> - FPS, heap, active members, assets, render calls, and the inspected camera.</li>
 *   <li><b>Performance</b> - live line graphs of FPS, frame time, draw calls, and heap, drawn on a
 *       {@code <canvas>} from the same ring buffers the base class fills.</li>
 *   <li><b>Watch</b> - the {@code Flixel.watch} key and value table.</li>
 *   <li><b>Tracker</b> - grouped blocks registered through
 *       {@link FlixelDebugManager#addTrackerEntry(FlixelDebugTrackerEntry)}.</li>
 *   <li><b>Log</b> - the live, level-filtered log stream.</li>
 *   <li><b>Controls</b> - hitbox and pause toggles, a time-scale slider, an overlay update-rate
 *       slider, and the keybind reference.</li>
 *   <li><b>Command</b> - a text field routed through {@code Flixel.debug.executeCommand(...)}, with
 *       output flowing to the Log tab exactly as it does on desktop.</li>
 * </ul>
 *
 * <p>The texture inspector is intentionally left out of this first version; displaying a WebGL
 * texture needs a second canvas and readback path that is better handled on its own.
 *
 * <h2>The JavaScript bridge</h2>
 *
 * <p>All UI lives under a single {@code window.__flixelDebug} object created once by
 * {@link #buildDom()}. Data flows from Java into the DOM through small {@code @JSBody} setters
 * ({@link #pushStats}, {@link #appendLog}, {@link #drawGraph}, and friends). User interaction flows
 * the other way through plain state fields on {@code window.__flixelDebug} that Java polls once per
 * frame ({@link #consumeToggle()}, {@link #consumePendingCommand()}, and friends). No
 * {@code @JSFunctor} callback is used, so the overlay works on both the JavaScript and WebAssembly GC
 * TeaVM targets.
 *
 * <h2>Wiring</h2>
 *
 * <p>{@link FlixelHtml5Launcher} registers this class as the overlay factory when the game starts in
 * {@link FlixelRuntimeMode#DEBUG DEBUG} mode. Games may register it manually with
 * {@link FlixelDebugManager#setOverlayFactory} as well.
 */
public class FlixelHtml5DebugOverlay extends FlixelDebugOverlay {

  /** Default overlay data update rate in Hertz, matching the desktop overlay's default. */
  private static final float DEFAULT_UPDATE_RATE = 20f;

  /** Lowest overlay update rate the slider allows, in Hertz. */
  private static final float MIN_UPDATE_RATE = 1f;

  /** Highest overlay update rate the slider allows, in Hertz. */
  private static final float MAX_UPDATE_RATE = 30f;

  /** Tab index for the Performance panel; used to gate the (relatively costly) graph redraw. */
  private static final int TAB_PERFORMANCE = 1;

  /** Reused buffer for the comma-separated graph samples, so the redraw path allocates only the final string. */
  private final StringBuilder graphBuffer = new StringBuilder(768);

  private float overlayUpdateRate = DEFAULT_UPDATE_RATE;
  private float graphSampleTimer;

  private boolean domBuilt;

  @Override
  public void update(float elapsed) {
    ensureDomBuilt();
    // Poll the DOM driven controls before the base update so a click this frame (pause, hitbox,
    // toggle) takes effect on the same frame the keyboard shortcuts would.
    pollControls();
    super.update(elapsed);
  }

  @Override
  public void setVisible(boolean visible) {
    super.setVisible(visible);
    if (domBuilt) {
      showDom(isVisible());
    }
  }

  @Override
  public void toggleVisible() {
    super.toggleVisible();
    if (domBuilt) {
      showDom(isVisible());
    }
  }

  @Override
  public boolean isMouseCapturedByUI() {
    return domBuilt && isPointerOverPanel();
  }

  @Override
  public boolean isKeyboardCapturedByUI() {
    return domBuilt && isCommandFieldFocused();
  }

  @Override
  protected void drawUI() {
    if (!domBuilt) {
      return;
    }
    pushStatsSnapshot();
    syncControls(Flixel.game.isGamePaused(), isDrawDebug());

    if (getActiveTabIndex() == TAB_PERFORMANCE) {
      graphSampleTimer += Flixel.getRawElapsed();
      float interval = 1f / overlayUpdateRate;
      if (graphSampleTimer >= interval) {
        graphSampleTimer -= interval;
        redrawGraphs();
      }
    }
  }

  @Override
  protected void onWatchEntriesRefreshed() {
    if (!domBuilt) {
      return;
    }
    clearWatch();
    for (int i = 0, n = cachedWatchKeys.getSize(); i < n; i++) {
      addWatchRow(cachedWatchKeys.get(i).toString(), cachedWatchValues.get(i).toString());
    }
  }

  @Override
  protected void onTrackerBlocksRebuilt() {
    if (!domBuilt) {
      return;
    }
    clearTracker();
    for (int i = 0, n = cachedTrackerBlocks.getSize(); i < n; i++) {
      CachedTrackerBlock block = cachedTrackerBlocks.get(i);
      addTrackerHeader(block.name.toString());
      for (int p = 0; p < block.pairCount; p++) {
        addTrackerRow(block.keys[p].toString(), block.values[p].toString());
      }
    }
  }

  @Override
  protected void onLogEntryAppended(BufferedLogLine line) {
    if (!domBuilt) {
      return;
    }
    appendLog(line.level.name(), line.tagStr, line.messageStr);
  }

  @Override
  public void destroy() {
    if (domBuilt) {
      removeDom();
      domBuilt = false;
    }
    super.destroy();
  }

  /**
   * Returns the overlay data update rate in updates per second.
   *
   * <p>The rate governs how often the performance graphs redraw; the base class uses the same value
   * to throttle its stat, watch, and tracker refreshes. The value is always within
   * {@code [1, 30]} Hz.
   *
   * @return The update rate in Hertz.
   */
  public float getOverlayUpdateRate() {
    return overlayUpdateRate;
  }

  /**
   * Sets the overlay data update rate in updates per second, clamped to {@code [1, 30]} Hz.
   *
   * <p>Lower values reduce the overlay's overhead; higher values give smoother, more reactive
   * graphs and readouts. Mirrors {@code FlixelImGuiDebugOverlay.setOverlayUpdateRate(float)} so the
   * two backends behave the same way.
   *
   * @param hz The desired rate in Hertz. Values below 1 are raised to 1; values above 30 are lowered
   *     to 30.
   */
  public void setOverlayUpdateRate(float hz) {
    overlayUpdateRate = Math.max(MIN_UPDATE_RATE, Math.min(MAX_UPDATE_RATE, hz));
    float interval = 1f / overlayUpdateRate;
    perfSampleInterval = interval;
    watchRefreshInterval = interval;
    statsUpdateInterval = interval;
  }

  private void ensureDomBuilt() {
    if (domBuilt) {
      return;
    }
    buildDom();
    domBuilt = true;
    setUpdateRateValue(overlayUpdateRate);
    setTimeScaleValue(Flixel.timeScale);
    showDom(isVisible());
  }

  /**
   * Reads and applies every DOM driven control once per frame. Each control writes a plain state
   * field on {@code window.__flixelDebug}; the consume helpers return and clear the pending signals
   * so an action fires exactly once.
   */
  private void pollControls() {
    if (!domBuilt) {
      return;
    }
    if (consumeToggle()) {
      toggleVisible();
    }
    if (consumeHitbox()) {
      toggleDrawDebug();
    }
    if (consumePause()) {
      Flixel.game.setGamePaused(!Flixel.game.isGamePaused());
    }
    if (consumeReset()) {
      Flixel.resetState();
    }
    double newTimeScale = consumeTimeScale();
    if (newTimeScale >= 0.0) {
      Flixel.timeScale = (float) newTimeScale;
    }
    double newRate = consumeUpdateRate();
    if (newRate >= 0.0) {
      setOverlayUpdateRate((float) newRate);
    }
    String command = consumePendingCommand();
    if (command != null) {
      String trimmed = command.trim();
      if (!trimmed.isEmpty()) {
        Flixel.info("FlixelDebug", "> " + trimmed);
        Flixel.debug.executeCommand(trimmed);
      }
    }
  }

  /** Pushes the base class's cached stats plus the inspected-camera readout into the Stats panel. */
  private void pushStatsSnapshot() {
    FlixelArray<FlixelCamera> cams = Flixel.cameras;
    int camCount = cams != null ? cams.getSize() : 0;
    int inspect = getInspectCameraIndex();
    float scrollX = 0f;
    float scrollY = 0f;
    float zoom = 1f;
    if (camCount > 0 && inspect >= 0) {
      FlixelCamera cam = cams.get(inspect);
      scrollX = cam.scrollX;
      scrollY = cam.scrollY;
      zoom = cam.getZoom();
    }
    pushStats(cachedFps, cachedHeapMegabytes, cachedNativeMegabytes, cachedObjectCount, cachedAssetCount,
        cachedRenderCalls, Flixel.game.isGamePaused(), inspect, camCount, scrollX, scrollY, zoom);
  }

  /** Rebuilds and pushes the four performance graphs (FPS, frame time, draw calls, heap). */
  private void redrawGraphs() {
    drawGraph(0, buildGraphCsv(getPerfFps()), 0);
    drawGraph(1, buildGraphCsv(getPerfFrameMs()), 1);
    drawGraph(2, buildGraphCsv(getPerfRenderCalls()), 0);
    drawGraph(3, buildGraphCsv(getPerfHeapMb()), 1);
  }

  /**
   * Builds a comma-separated, oldest-first list of the valid samples in {@code ring}, reusing
   * {@link #graphBuffer} so only the returned {@link String} is freshly allocated.
   *
   * @param ring The ring buffer to serialize.
   * @return The samples joined by commas, or an empty string when no samples exist yet.
   */
  private String buildGraphCsv(float[] ring) {
    int count = getPerfCount();
    graphBuffer.setLength(0);
    if (count == 0) {
      return "";
    }
    int n = ring.length;
    // Before the ring fills, samples sit at 0..count-1; once full, the oldest is at the write head.
    int start = count < n ? 0 : getPerfHead();
    for (int i = 0; i < count; i++) {
      int idx = (start + i) % n;
      if (i > 0) {
        graphBuffer.append(',');
      }
      graphBuffer.append(Math.round(ring[idx] * 100f) / 100f);
    }
    return graphBuffer.toString();
  }

  @JSBody(
      script = """
          var D = window.__flixelDebug = window.__flixelDebug || {};
          if (D.__built) { return; }
          D.__built = true;
          D.pendingCommand = null;
          D.toggle = false;
          D.hitbox = false;
          D.pause = false;
          D.reset = false;
          D.timeScale = -1;
          D.updateRate = -1;
          D.activeTab = 0;
          D.hover = false;
          D.focus = false;
          D.filters = { INFO: true, WARN: true, ERROR: true, DEBUG: true };
          D.history = [];
          D.historyCursor = -1;
          D.graphs = [];

          var LEVEL_COLORS = { INFO: '#d9d9d9', WARN: '#f5c518', ERROR: '#e94560', DEBUG: '#5a7dff' };
          var KEY_COLOR = '#e94560';
          var VALUE_COLOR = '#f2f2f2';
          var TABS = ['Stats', 'Perf', 'Watch', 'Tracker', 'Log', 'Controls', 'Cmd'];

          function mk(tag, css, text) {
            var el = document.createElement(tag);
            if (css) { el.style.cssText = css; }
            if (text != null) { el.textContent = text; }
            return el;
          }
          function row(label, valueId) {
            var r = mk('div', 'display:flex;justify-content:space-between;gap:12px;padding:1px 0;');
            r.appendChild(mk('span', 'color:' + KEY_COLOR + ';', label));
            var v = mk('span', 'color:' + VALUE_COLOR + ';text-align:right;', '');
            v.id = valueId;
            r.appendChild(v);
            return r;
          }

          var panel = mk('div', 'position:fixed;top:0;right:0;bottom:0;width:360px;max-width:90vw;z-index:9000;'
            + 'background:rgba(17,17,17,0.94);border-left:1px solid #333;color:#ccc;'
            + 'font-family:monospace;font-size:12px;display:none;flex-direction:column;box-sizing:border-box;');
          panel.id = 'flixel-debug-panel';
          D.panel = panel;

          var header = mk('div', 'display:flex;align-items:center;justify-content:space-between;'
            + 'padding:8px 12px;border-bottom:1px solid #333;flex:0 0 auto;');
          header.appendChild(mk('span', 'color:' + KEY_COLOR + ';font-weight:bold;', 'FlixelGDX Debug'));
          var hideBtn = mk('button', 'background:#222;color:#ccc;border:1px solid #444;cursor:pointer;'
            + 'font-family:monospace;font-size:11px;padding:2px 8px;', 'Hide');
          hideBtn.onclick = function () { D.toggle = true; };
          header.appendChild(hideBtn);
          panel.appendChild(header);

          var tabBar = mk('div', 'display:flex;flex-wrap:wrap;gap:2px;padding:6px 8px;'
            + 'border-bottom:1px solid #333;flex:0 0 auto;');
          var sections = [];
          D.tabButtons = [];
          function selectTab(i) {
            D.activeTab = i;
            for (var t = 0; t < TABS.length; t++) {
              sections[t].style.display = (t === i) ? 'block' : 'none';
              D.tabButtons[t].style.background = (t === i) ? '#7a1620' : '#222';
            }
          }
          TABS.forEach(function (name, i) {
            var b = mk('button', 'background:#222;color:#ccc;border:1px solid #444;cursor:pointer;'
              + 'font-family:monospace;font-size:11px;padding:2px 7px;', name);
            b.onclick = function () { selectTab(i); };
            D.tabButtons.push(b);
            tabBar.appendChild(b);
          });
          panel.appendChild(tabBar);

          var content = mk('div', 'flex:1 1 auto;overflow-y:auto;padding:10px 12px;');
          panel.appendChild(content);
          TABS.forEach(function () {
            var s = mk('div', 'display:none;');
            sections.push(s);
            content.appendChild(s);
          });

          var stats = sections[0];
          ['fps:FPS', 'heap:Heap (MB)', 'native:Native (MB)', 'objects:Active members',
            'assets:Assets loaded', 'calls:Render calls'].forEach(function (spec) {
            var parts = spec.split(':');
            stats.appendChild(row(parts[1], 'fxstat-' + parts[0]));
          });
          stats.appendChild(mk('div', 'border-top:1px solid #333;margin:6px 0;'));
          stats.appendChild(row('Update', 'fxstat-update'));
          stats.appendChild(row('Cameras', 'fxstat-cams'));
          stats.appendChild(row('  Scroll X', 'fxstat-sx'));
          stats.appendChild(row('  Scroll Y', 'fxstat-sy'));
          stats.appendChild(row('  Zoom', 'fxstat-zoom'));

          var perf = sections[1];
          var GRAPH_LABELS = ['FPS', 'Frame (ms)', 'Draw calls', 'Heap (MB)'];
          GRAPH_LABELS.forEach(function (label, i) {
            var head = mk('div', 'display:flex;justify-content:space-between;margin-top:6px;');
            head.appendChild(mk('span', 'color:' + KEY_COLOR + ';', label));
            var val = mk('span', 'color:' + VALUE_COLOR + ';', '-');
            val.id = 'fxgraph-val-' + i;
            head.appendChild(val);
            perf.appendChild(head);
            var canvas = mk('canvas', 'width:100%;height:40px;display:block;background:#0c0c0c;border:1px solid #2a2a2a;');
            canvas.width = 320;
            canvas.height = 40;
            perf.appendChild(canvas);
            D.graphs.push(canvas);
          });

          var watch = sections[2];
          D.watchBody = mk('div', '');
          D.watchEmpty = mk('div', 'color:#888;', 'No watches registered. Use Flixel.watch.add(...) to track values.');
          watch.appendChild(D.watchEmpty);
          watch.appendChild(D.watchBody);

          var tracker = sections[3];
          D.trackerBody = mk('div', '');
          D.trackerEmpty = mk('div', 'color:#888;',
            'No trackers registered. Use Flixel.debug.addTrackerEntry(...) to show grouped values here.');
          tracker.appendChild(D.trackerEmpty);
          tracker.appendChild(D.trackerBody);

          var log = sections[4];
          var filterBar = mk('div', 'display:flex;flex-wrap:wrap;gap:8px;margin-bottom:6px;');
          ['INFO', 'WARN', 'ERROR', 'DEBUG'].forEach(function (lvl) {
            var lab = mk('label', 'display:flex;align-items:center;gap:3px;color:' + LEVEL_COLORS[lvl] + ';cursor:pointer;');
            var cb = mk('input', '');
            cb.type = 'checkbox';
            cb.checked = true;
            cb.onchange = function () { D.filters[lvl] = cb.checked; D.applyFilters(); };
            lab.appendChild(cb);
            lab.appendChild(mk('span', '', lvl));
            filterBar.appendChild(lab);
          });
          log.appendChild(filterBar);
          D.logBody = mk('div', 'white-space:pre-wrap;word-break:break-word;');
          log.appendChild(D.logBody);
          D.applyFilters = function () {
            var rows = D.logBody.children;
            for (var i = 0; i < rows.length; i++) {
              var lvl = rows[i].getAttribute('data-level');
              rows[i].style.display = D.filters[lvl] ? 'block' : 'none';
            }
          };

          var controls = sections[5];
          function checkRow(label, id) {
            var lab = mk('label', 'display:flex;align-items:center;gap:6px;cursor:pointer;padding:2px 0;');
            var cb = mk('input', '');
            cb.type = 'checkbox';
            cb.id = id;
            lab.appendChild(cb);
            lab.appendChild(mk('span', '', label));
            return { label: lab, box: cb };
          }
          var hitboxCtl = checkRow('Show hitboxes', 'fxctl-hitbox');
          hitboxCtl.box.onchange = function () { D.hitbox = true; };
          controls.appendChild(hitboxCtl.label);
          var pauseCtl = checkRow('Pause game loop', 'fxctl-pause');
          pauseCtl.box.onchange = function () { D.pause = true; };
          controls.appendChild(pauseCtl.label);

          controls.appendChild(mk('div', 'border-top:1px solid #333;margin:6px 0;'));
          controls.appendChild(mk('div', 'color:' + KEY_COLOR + ';', 'Time scale'));
          var tsWrap = mk('div', 'display:flex;align-items:center;gap:8px;');
          var tsSlider = mk('input', 'flex:1;');
          tsSlider.type = 'range';
          tsSlider.min = '0.1';
          tsSlider.max = '4';
          tsSlider.step = '0.05';
          tsSlider.value = '1';
          tsSlider.id = 'fxctl-timescale';
          var tsVal = mk('span', 'color:' + VALUE_COLOR + ';min-width:44px;text-align:right;', '1.00x');
          tsSlider.oninput = function () { D.timeScale = parseFloat(tsSlider.value); tsVal.textContent = parseFloat(tsSlider.value).toFixed(2) + 'x'; };
          var tsReset = mk('button', 'background:#222;color:#ccc;border:1px solid #444;cursor:pointer;'
            + 'font-family:monospace;font-size:11px;padding:2px 8px;', 'Reset');
          tsReset.onclick = function () { D.timeScale = 1; };
          tsWrap.appendChild(tsSlider);
          tsWrap.appendChild(tsVal);
          tsWrap.appendChild(tsReset);
          controls.appendChild(tsWrap);
          D.tsSlider = tsSlider;
          D.tsVal = tsVal;

          controls.appendChild(mk('div', 'border-top:1px solid #333;margin:6px 0;'));
          controls.appendChild(mk('div', 'color:' + KEY_COLOR + ';', 'Update rate (Hz)'));
          var urWrap = mk('div', 'display:flex;align-items:center;gap:8px;');
          var urSlider = mk('input', 'flex:1;');
          urSlider.type = 'range';
          urSlider.min = '1';
          urSlider.max = '30';
          urSlider.step = '1';
          urSlider.value = '20';
          urSlider.id = 'fxctl-updaterate';
          var urVal = mk('span', 'color:' + VALUE_COLOR + ';min-width:44px;text-align:right;', '20 Hz');
          urSlider.oninput = function () { D.updateRate = parseFloat(urSlider.value); urVal.textContent = urSlider.value + ' Hz'; };
          urWrap.appendChild(urSlider);
          urWrap.appendChild(urVal);
          controls.appendChild(urWrap);
          D.urSlider = urSlider;
          D.urVal = urVal;

          controls.appendChild(mk('div', 'border-top:1px solid #333;margin:6px 0;'));
          var resetBtn = mk('button', 'background:#7a1620;color:#fff;border:1px solid #a3202d;cursor:pointer;'
            + 'font-family:monospace;font-size:11px;padding:3px 10px;', 'Reset state');
          resetBtn.onclick = function () { D.reset = true; };
          controls.appendChild(resetBtn);

          controls.appendChild(mk('div', 'border-top:1px solid #333;margin:6px 0;'));
          controls.appendChild(mk('div', 'color:' + KEY_COLOR + ';', 'Keybinds'));
          [['Toggle overlay', 'F2'], ['Toggle hitboxes', 'F3'], ['Pause', 'F4'],
            ['Cycle camera left', 'Alt + Left'], ['Cycle camera right', 'Alt + Right'],
            ['Pan camera (paused)', 'Right mouse drag'], ['Move sprite (paused)', 'Left mouse drag'],
            ['Zoom camera (paused)', 'Mouse wheel']].forEach(function (pair) {
            var r = mk('div', 'display:flex;justify-content:space-between;gap:12px;padding:1px 0;');
            r.appendChild(mk('span', 'color:' + KEY_COLOR + ';', pair[0]));
            r.appendChild(mk('span', 'color:' + VALUE_COLOR + ';text-align:right;', pair[1]));
            controls.appendChild(r);
          });

          var cmd = sections[6];
          cmd.appendChild(mk('div', 'color:#888;margin-bottom:6px;',
            'Enter a command and press Enter. Type "help" for a list. Output appears in the Log tab.'));
          var cmdWrap = mk('div', 'display:flex;gap:6px;');
          var cmdInput = mk('input', 'flex:1;background:#0c0c0c;color:#eee;border:1px solid #444;'
            + 'font-family:monospace;font-size:12px;padding:4px 6px;');
          cmdInput.type = 'text';
          cmdInput.spellcheck = false;
          var runBtn = mk('button', 'background:#222;color:#ccc;border:1px solid #444;cursor:pointer;'
            + 'font-family:monospace;font-size:11px;padding:2px 10px;', 'Run');
          function submitCommand() {
            var line = cmdInput.value;
            if (line && line.trim().length > 0) {
              D.pendingCommand = line;
              D.history.push(line);
              D.historyCursor = D.history.length;
            }
            cmdInput.value = '';
          }
          cmdInput.onfocus = function () { D.focus = true; };
          cmdInput.onblur = function () { D.focus = false; };
          cmdInput.onkeydown = function (e) {
            if (e.key === 'Enter') { submitCommand(); e.preventDefault(); }
            else if (e.key === 'ArrowUp') {
              if (D.historyCursor > 0) { D.historyCursor--; cmdInput.value = D.history[D.historyCursor]; }
              e.preventDefault();
            } else if (e.key === 'ArrowDown') {
              if (D.historyCursor < D.history.length - 1) { D.historyCursor++; cmdInput.value = D.history[D.historyCursor]; }
              else { D.historyCursor = D.history.length; cmdInput.value = ''; }
              e.preventDefault();
            }
          };
          runBtn.onclick = submitCommand;
          cmdWrap.appendChild(cmdInput);
          cmdWrap.appendChild(runBtn);
          cmd.appendChild(cmdWrap);

          var reveal = mk('button', 'position:fixed;top:8px;right:8px;z-index:9001;'
            + 'background:rgba(122,22,32,0.85);color:#fff;border:1px solid #a3202d;cursor:pointer;'
            + 'font-family:monospace;font-size:11px;padding:3px 8px;', 'DBG');
          reveal.id = 'flixel-debug-reveal';
          reveal.onclick = function () { D.toggle = true; };
          D.reveal = reveal;

          panel.addEventListener('pointerenter', function () { D.hover = true; });
          panel.addEventListener('pointerleave', function () { D.hover = false; });
          reveal.addEventListener('pointerenter', function () { D.hover = true; });
          reveal.addEventListener('pointerleave', function () { D.hover = false; });

          D.setStat = function (id, text) { var el = document.getElementById('fxstat-' + id); if (el) { el.textContent = text; } };
          D.drawGraph = function (index, csv, decimals) {
            var canvas = D.graphs[index];
            if (!canvas) { return; }
            var ctx = canvas.getContext('2d');
            var w = canvas.width, h = canvas.height;
            ctx.clearRect(0, 0, w, h);
            if (!csv) { return; }
            var parts = csv.split(',');
            var max = 0.0001;
            var i;
            for (i = 0; i < parts.length; i++) { var v = parseFloat(parts[i]); if (v > max) { max = v; } }
            max *= 1.15;
            ctx.strokeStyle = '#e94560';
            ctx.lineWidth = 1;
            ctx.beginPath();
            for (i = 0; i < parts.length; i++) {
              var y = h - (parseFloat(parts[i]) / max) * (h - 2) - 1;
              var x = parts.length > 1 ? (i / (parts.length - 1)) * (w - 1) : 0;
              if (i === 0) { ctx.moveTo(x, y); } else { ctx.lineTo(x, y); }
            }
            ctx.stroke();
            var last = parseFloat(parts[parts.length - 1]);
            var valEl = document.getElementById('fxgraph-val-' + index);
            if (valEl) { valEl.textContent = decimals > 0 ? last.toFixed(1) : String(Math.round(last)); }
          };
          D.appendLog = function (level, tag, msg) {
            var lvl = D.filters.hasOwnProperty(level) ? level : 'INFO';
            var line = document.createElement('div');
            line.setAttribute('data-level', lvl);
            line.style.color = LEVEL_COLORS[lvl] || '#ccc';
            line.style.display = D.filters[lvl] ? 'block' : 'none';
            var prefix = tag && tag.length > 0 ? ('[' + level + '] [' + tag + '] ') : ('[' + level + '] ');
            line.textContent = prefix + msg;
            D.logBody.appendChild(line);
            while (D.logBody.children.length > 400) { D.logBody.removeChild(D.logBody.firstChild); }
            var sc = D.logBody.parentNode;
            sc.scrollTop = sc.scrollHeight;
          };
          D.clearWatch = function () { D.watchBody.textContent = ''; };
          D.addWatchRow = function (k, v) {
            D.watchEmpty.style.display = 'none';
            var r = mk('div', 'display:flex;justify-content:space-between;gap:12px;padding:1px 0;');
            r.appendChild(mk('span', 'color:' + KEY_COLOR + ';', k));
            r.appendChild(mk('span', 'color:' + VALUE_COLOR + ';text-align:right;', v));
            D.watchBody.appendChild(r);
          };
          D.clearTracker = function () { D.trackerBody.textContent = ''; };
          D.addTrackerHeader = function (name) {
            D.trackerEmpty.style.display = 'none';
            D.trackerBody.appendChild(mk('div', 'color:' + KEY_COLOR + ';font-weight:bold;margin-top:6px;', name));
          };
          D.addTrackerRow = function (k, v) {
            var r = mk('div', 'display:flex;justify-content:space-between;gap:12px;padding:1px 0 1px 8px;');
            r.appendChild(mk('span', 'color:#bbb;', k));
            r.appendChild(mk('span', 'color:' + VALUE_COLOR + ';text-align:right;', v));
            D.trackerBody.appendChild(r);
          };
          D.show = function (visible) {
            D.panel.style.display = visible ? 'flex' : 'none';
            D.reveal.style.display = visible ? 'none' : 'block';
          };

          document.body.appendChild(panel);
          document.body.appendChild(reveal);
          selectTab(0);
          """)
  private static native void buildDom();

  @JSBody(params = "visible", script = "if (window.__flixelDebug) { window.__flixelDebug.show(visible); }")
  private static native void showDom(boolean visible);

  @JSBody(
      script = "if (window.__flixelDebug) { window.__flixelDebug.panel.parentNode.removeChild(window.__flixelDebug.panel);"
          + " window.__flixelDebug.reveal.parentNode.removeChild(window.__flixelDebug.reveal);"
          + " window.__flixelDebug.__built = false; }")
  private static native void removeDom();

  @JSBody(params = { "fps", "heap", "nativeMb", "objects", "assets", "calls", "paused", "camIndex",
      "camCount", "scrollX", "scrollY", "zoom" }, script = """
          var D = window.__flixelDebug;
          if (!D) { return; }
          D.setStat('fps', String(fps));
          D.setStat('heap', heap.toFixed(1));
          D.setStat('native', nativeMb.toFixed(1));
          D.setStat('objects', String(objects));
          D.setStat('assets', String(assets));
          D.setStat('calls', String(calls));
          D.setStat('update', paused ? 'PAUSED' : 'RUNNING');
          D.setStat('cams', camCount === 0 ? '0 (none)' : ((camIndex + 1) + ' / ' + camCount));
          D.setStat('sx', camCount === 0 ? '-' : scrollX.toFixed(1));
          D.setStat('sy', camCount === 0 ? '-' : scrollY.toFixed(1));
          D.setStat('zoom', camCount === 0 ? '-' : zoom.toFixed(2));
          """)
  private static native void pushStats(int fps, double heap, double nativeMb, int objects, int assets,
      int calls, boolean paused, int camIndex, int camCount, double scrollX, double scrollY, double zoom);

  @JSBody(params = { "paused", "hitbox" }, script = """
      var D = window.__flixelDebug;
      if (!D) { return; }
      var p = document.getElementById('fxctl-pause');
      if (p) { p.checked = paused; }
      var h = document.getElementById('fxctl-hitbox');
      if (h) { h.checked = hitbox; }
      """)
  private static native void syncControls(boolean paused, boolean hitbox);

  @JSBody(params = "value", script = """
      var D = window.__flixelDebug;
      if (D && D.tsSlider) { D.tsSlider.value = value; D.tsVal.textContent = value.toFixed(2) + 'x'; }
      """)
  private static native void setTimeScaleValue(double value);

  @JSBody(params = "value", script = """
      var D = window.__flixelDebug;
      if (D && D.urSlider) { D.urSlider.value = value; D.urVal.textContent = Math.round(value) + ' Hz'; }
      """)
  private static native void setUpdateRateValue(double value);

  @JSBody(params = { "level", "tag", "msg" },
      script = "if (window.__flixelDebug) { window.__flixelDebug.appendLog(level, tag, msg); }")
  private static native void appendLog(String level, String tag, String msg);

  @JSBody(script = "if (window.__flixelDebug) { window.__flixelDebug.clearWatch(); }")
  private static native void clearWatch();

  @JSBody(params = { "key", "value" },
      script = "if (window.__flixelDebug) { window.__flixelDebug.addWatchRow(key, value); }")
  private static native void addWatchRow(String key, String value);

  @JSBody(script = "if (window.__flixelDebug) { window.__flixelDebug.clearTracker(); }")
  private static native void clearTracker();

  @JSBody(params = "name", script = "if (window.__flixelDebug) { window.__flixelDebug.addTrackerHeader(name); }")
  private static native void addTrackerHeader(String name);

  @JSBody(params = { "key", "value" },
      script = "if (window.__flixelDebug) { window.__flixelDebug.addTrackerRow(key, value); }")
  private static native void addTrackerRow(String key, String value);

  @JSBody(params = { "index", "csv", "decimals" },
      script = "if (window.__flixelDebug) { window.__flixelDebug.drawGraph(index, csv, decimals); }")
  private static native void drawGraph(int index, String csv, int decimals);

  @JSBody(script = "var D = window.__flixelDebug; if (D && D.toggle) { D.toggle = false; return true; } return false;")
  private static native boolean consumeToggle();

  @JSBody(script = "var D = window.__flixelDebug; if (D && D.hitbox) { D.hitbox = false; return true; } return false;")
  private static native boolean consumeHitbox();

  @JSBody(script = "var D = window.__flixelDebug; if (D && D.pause) { D.pause = false; return true; } return false;")
  private static native boolean consumePause();

  @JSBody(script = "var D = window.__flixelDebug; if (D && D.reset) { D.reset = false; return true; } return false;")
  private static native boolean consumeReset();

  @JSBody(script = """
      var D = window.__flixelDebug;
      if (D && D.timeScale >= 0) { var v = D.timeScale; D.timeScale = -1; return v; }
      return -1;
      """)
  private static native double consumeTimeScale();

  @JSBody(script = """
      var D = window.__flixelDebug;
      if (D && D.updateRate >= 0) { var v = D.updateRate; D.updateRate = -1; return v; }
      return -1;
      """)
  private static native double consumeUpdateRate();

  @JSBody(script = """
      var D = window.__flixelDebug;
      if (D && D.pendingCommand != null) { var c = D.pendingCommand; D.pendingCommand = null; return c; }
      return null;
      """)
  private static native String consumePendingCommand();

  @JSBody(script = "return window.__flixelDebug ? window.__flixelDebug.activeTab : 0;")
  private static native int getActiveTabIndex();

  @JSBody(script = "return !!(window.__flixelDebug && window.__flixelDebug.hover);")
  private static native boolean isPointerOverPanel();

  @JSBody(script = "return !!(window.__flixelDebug && window.__flixelDebug.focus);")
  private static native boolean isCommandFieldFocused();
}
