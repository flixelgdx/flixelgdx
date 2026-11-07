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

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelConfig;
import org.flixelgdx.FlixelVersion;
import org.flixelgdx.backend.FlixelPlatform;
import org.flixelgdx.debug.panels.FlixelDebugLogPanel;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.panels.FlixelDebugPanel;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * The messenger of the remote debugger: it shuttles protocol messages between the debug panels and a
 * {@link FlixelDebugTransport}.
 *
 * <p>Think of a courier who visits every desk (panel) in an office once per frame. The courier introduces the
 * game to the debugger app (the handshake), then walks around collecting a fresh report from each desk that
 * has something new, at a pace each desk can bear, and hands the replies of the app (button presses, edits,
 * pings) to the right desk. The courier never reads the reports; each panel writes its own payload, and the
 * transport does the actual traveling. If the app is not there, the courier simply waits at the door.
 *
 * <p>{@link FlixelDebugManager} owns the agent and starts it only in debug mode, when a transport other than
 * {@link FlixelNoopDebugTransport} is installed. Everything runs on the game thread from
 * {@link #update(float)}. After the first few frames it allocates nothing: it reuses one writer, one reader, and
 * one receive buffer.
 *
 * <h2>The conversation</h2>
 *
 * <ol>
 *   <li>The transport reports a new connection and the agent sends {@code HELLO}.</li>
 *   <li>The app answers {@code HELLO_ACK}. When its major version differs from
 *       {@link FlixelDebugProtocol#VERSION_MAJOR}, the agent logs one warning and closes the transport for good.</li>
 *   <li>The agent sends {@code PANEL_LIST}, then the full payload of every enabled panel (the log panel sends its
 *       backlog).</li>
 *   <li>Every frame the agent answers {@code PING} with {@code PONG}, hands each {@code PANEL_ACTION} to its
 *       panel, sends the panel list again when panels were registered or removed, and sends the payload of every
 *       panel that changed.</li>
 * </ol>
 *
 * <h2>How often panels are sent</h2>
 *
 * <p>A panel is sent only when its {@linkplain FlixelDebugPanel#getRevision() revision} changed. Most panels are
 * also held back to the sample rate of the controls panel (20 Hz by default). The exceptions are the inspector
 * (up to 30 Hz), the log panel (new lines are sent in batches every 0.1 seconds), and the stats, controls,
 * console, and save panels, which are sent as soon as they change. A panel's extra payloads (thumbnails and the
 * result of a console command) go out right away.
 *
 * <p>When the transport's outgoing queue is full, regular updates are skipped and tried again next frame, so a
 * slow network never freezes the game.
 *
 * <p>Example:
 *
 * <pre>{@code
 * // A launcher installs a transport before the game starts. The manager does the rest.
 * FlixelDebugManager.defaultTransport = new FlixelSocketWebSocketTransport();
 * FlixelDebugManager.defaultEndpoint = FlixelDebugEndpoint.parse(System.getProperty("flixel.debugHost"));
 * }</pre>
 *
 * @see FlixelDebugTransport
 * @see FlixelDebugProtocol
 */
public final class FlixelDebugAgent {

  /** The size of the buffer that receives messages. A longer message from the app is dropped. */
  public static final int RECEIVE_BYTES = 128 * 1024;

  /** Seconds between two sends of the inspector (30 Hz). */
  public static final float INSPECTOR_INTERVAL = 1f / 30f;

  /** Seconds between two batches of log lines. */
  public static final float LOG_INTERVAL = 0.1f;

  /** Seconds over which the transfer rates are averaged. */
  public static final float RATE_WINDOW = 1f;

  private static final int IDLE = 0;
  private static final int WAIT_ACK = 1;
  private static final int LIVE = 2;
  private static final int CLOSED = 3;

  private static final int MAX_MESSAGES_PER_FRAME = 256;
  private static final int MAX_PENDING_PER_FRAME = 4;
  private static final float MAX_TIMER = 10f;
  private static final long NEVER = -1L;

  private static final FlixelLogger LOG = Flixel.log.tagged("Debugger/Agent");

  private final FlixelDebugMessageWriter writer = new FlixelDebugMessageWriter();
  private final FlixelDebugMessageReader reader = new FlixelDebugMessageReader();
  private final ByteBuffer receive = ByteBuffer.allocate(RECEIVE_BYTES);
  private final FlixelDebugManager manager;
  private final FlixelDebugTransport transport;

  private long sentBytes;
  private long recvBytes;
  private long listRevision = NEVER;
  private long dropped;

  private long[] sentRevisions = new long[16];
  private float[] timers = new float[16];
  private FlixelDebugPanel[] owners = new FlixelDebugPanel[16];
  private String host = FlixelDebugEndpoint.DEFAULT_HOST;
  private float rateTimer;
  private int port = FlixelDebugProtocol.DEFAULT_PORT;
  private int phase = IDLE;

  private boolean started;
  private boolean helloPending;
  private boolean protocolErrorLogged;
  private boolean panelErrorLogged;

  /**
   * Creates an agent. It does nothing until {@link #start(String, int)}.
   *
   * @param manager The manager whose panels the agent sends.
   * @param transport The transport that carries the messages.
   */
  public FlixelDebugAgent(@NotNull FlixelDebugManager manager, @NotNull FlixelDebugTransport transport) {
    this.manager = manager;
    this.transport = transport;
  }

  /**
   * Opens the transport towards the debugger app. The connection is made in the background.
   *
   * @param host The host of the debugger app.
   * @param port The port of the debugger app.
   */
  public void start(@NotNull String host, int port) {
    if (started) {
      return;
    }
    started = true;
    this.host = host;
    this.port = port;
    transport.open(host, port);
  }

  /**
   * Does one frame of work: starts or ends the handshake, answers the app, and sends changed panels.
   *
   * <p>Call it once per frame from the game thread. It does not allocate once it is warmed up.
   *
   * @param elapsed Real seconds since the last frame, not scaled by {@code Flixel.timeScale}.
   */
  public void update(float elapsed) {
    if (!started || phase == CLOSED) {
      return;
    }
    if (transport.consumeConnectedEvent()) {
      if (phase != IDLE) {
        lost();
      }
      connected();
    } else if (phase != IDLE && !transport.isConnected()) {
      lost();
      return;
    }
    if (phase == IDLE) {
      return;
    }
    if (helloPending) {
      helloPending = !sendHello();
    }
    drain();
    if (phase == LIVE) {
      pump(elapsed);
    }
    updateRates(elapsed);
  }

  /** Stops the agent and closes the transport. The agent cannot be started again. */
  public void stop() {
    if (started && phase != CLOSED) {
      transport.close();
    }
    started = false;
    phase = CLOSED;
  }

  private void connected() {
    phase = WAIT_ACK;
    helloPending = true;
    protocolErrorLogged = false;
    panelErrorLogged = false;
    resetState();
    LOG.info("Connected to the remote debugger at {}:{}.", host, port);
  }

  private void lost() {
    phase = IDLE;
    helloPending = false;
    resetState();
    manager.stats.setAgentRates(0f, 0f);
    LOG.info("Lost the connection to the remote debugger. Waiting for it to come back.");
  }

  private void resetState() {
    listRevision = NEVER;
    sentBytes = 0L;
    recvBytes = 0L;
    rateTimer = 0f;
    for (int i = 0; i < sentRevisions.length; i++) {
      sentRevisions[i] = NEVER;
      timers[i] = 0f;
      owners[i] = null;
    }
  }

  private boolean sendHello() {
    FlixelConfig config = Flixel.config;
    int width = Flixel.window.getWidth();
    int height = Flixel.window.getHeight();
    if (width <= 0 || height <= 0) {
      width = config != null ? config.getWidth() : 0;
      height = config != null ? config.getHeight() : 0;
    }
    writer.beginMessage(FlixelDebugProtocol.MSG_HELLO)
        .writeU16(FlixelDebugProtocol.VERSION_MAJOR)
        .writeU16(FlixelDebugProtocol.VERSION_MINOR)
        .writeString(platformName(Flixel.host.getPlatform()))
        .writeString(Flixel.runtime.getDeviceDescription())
        .writeI32(width)
        .writeI32(height)
        .writeString(config != null ? config.getTitle() : "")
        .writeString(FlixelVersion.VERSION);
    return transmit();
  }

  private void drain() {
    for (int i = 0; i < MAX_MESSAGES_PER_FRAME && phase != CLOSED; i++) {
      int length = transport.poll(receive);
      if (length == 0) {
        return;
      }
      if (length < 0) {
        dropped++;
        continue;
      }
      recvBytes += length;
      handle();
    }
  }

  private void handle() {
    try {
      reader.wrap(receive);
      int type = reader.readU8();
      switch (type) {
        case FlixelDebugProtocol.MSG_HELLO_ACK -> onAck();
        case FlixelDebugProtocol.MSG_PING -> {
          long nonce = reader.readI64();
          writer.beginMessage(FlixelDebugProtocol.MSG_PONG).writeI64(nonce);
          transmit();
        }
        case FlixelDebugProtocol.MSG_PANEL_ACTION -> {
          if (phase == LIVE) {
            int index = reader.readU16();
            int action = reader.readU8();
            if (index < manager.getPanelCount()) {
              manager.getPanel(index).handleAction(action, reader);
            }
          }
        }
        default -> {
        }
      }
    } catch (RuntimeException e) {
      if (!protocolErrorLogged) {
        protocolErrorLogged = true;
        LOG.warn("Ignored a message from the remote debugger that could not be handled: {}", e.getMessage());
      }
    }
  }

  private void onAck() {
    if (phase != WAIT_ACK) {
      return;
    }
    int major = reader.readU16();
    int minor = reader.readU16();
    if (major != FlixelDebugProtocol.VERSION_MAJOR) {
      LOG.warn("The remote debugger speaks protocol {}.{} but this game speaks {}.{}. Closing the connection;"
          + " use a debugger app that matches the framework.", major, minor, FlixelDebugProtocol.VERSION_MAJOR,
          FlixelDebugProtocol.VERSION_MINOR);
      phase = CLOSED;
      transport.close();
      return;
    }
    phase = LIVE;
    sendPanelList();
    listRevision = manager.getPanelRevision();
    int count = manager.getPanelCount();
    ensureSlots(count);
    for (int i = 0; i < count; i++) {
      FlixelDebugPanel panel = manager.getPanel(i);
      owners[i] = panel;
      timers[i] = 0f;
      if (panel.isEnabled()) {
        sendPanel(i, panel, true);
      }
    }
  }

  private void pump(float elapsed) {
    long listRev = manager.getPanelRevision();
    if (listRev != listRevision && sendPanelList()) {
      listRevision = listRev;
    }
    int count = manager.getPanelCount();
    ensureSlots(count);
    float interval = 1f / manager.controls.getRate();
    for (int i = 0; i < count; i++) {
      FlixelDebugPanel panel = manager.getPanel(i);
      if (owners[i] != panel) {
        owners[i] = panel;
        sentRevisions[i] = NEVER;
        timers[i] = 0f;
      }
      if (!panel.isEnabled()) {
        continue;
      }
      timers[i] = Math.min(timers[i] + elapsed, MAX_TIMER);
      for (int k = 0; k < MAX_PENDING_PER_FRAME && panel.hasPendingPayload(); k++) {
        if (!sendPending(i, panel)) {
          break;
        }
      }
      long revision = panel.getRevision();
      boolean changed = revision != sentRevisions[i];
      float wait;
      switch (panel.getKind()) {
        case FlixelDebugProtocol.KIND_LOGS -> {
          changed = panel instanceof FlixelDebugLogPanel logs ? logs.hasNewLines() : changed;
          wait = LOG_INTERVAL;
        }
        case FlixelDebugProtocol.KIND_INSPECTOR -> wait = INSPECTOR_INTERVAL;
        case FlixelDebugProtocol.KIND_STATS, FlixelDebugProtocol.KIND_CONTROLS, FlixelDebugProtocol.KIND_CONSOLE,
            FlixelDebugProtocol.KIND_SAVE, FlixelDebugProtocol.KIND_VIEWPORT ->
          wait = 0f;
        default -> wait = interval;
      }
      if (changed && (sentRevisions[i] == NEVER || timers[i] >= wait)) {
        if (sendPanel(i, panel, false)) {
          timers[i] -= wait;
          if (timers[i] > wait) {
            timers[i] = 0f;
          }
        }
      }
    }
  }

  private boolean sendPanelList() {
    int count = Math.min(manager.getPanelCount(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_LIST).beginArray();
    for (int i = 0; i < count; i++) {
      FlixelDebugPanel panel = manager.getPanel(i);
      writer.writeU16(i).writeString(panel.getId()).writeString(panel.getDisplayName()).writeU8(panel.getKind());
    }
    writer.endArray(count);
    return transmit();
  }

  private boolean sendPanel(int index, FlixelDebugPanel panel, boolean backlog) {
    long revision = panel.getRevision();
    writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_DATA).writeU16(index);
    try {
      if (backlog && panel instanceof FlixelDebugLogPanel logs) {
        logs.writeBacklog(writer);
      } else {
        panel.write(writer);
      }
    } catch (RuntimeException e) {
      logPanelError(panel, e);
      sentRevisions[index] = revision;
      return false;
    }
    if (!transmit()) {
      return false;
    }
    sentRevisions[index] = revision;
    return true;
  }

  private boolean sendPending(int index, FlixelDebugPanel panel) {
    writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_DATA).writeU16(index);
    try {
      panel.writePendingPayload(writer);
    } catch (RuntimeException e) {
      logPanelError(panel, e);
      return false;
    }
    return transmit();
  }

  private boolean transmit() {
    ByteBuffer message = writer.finish();
    int length = message.limit();
    if (transport.send(message)) {
      sentBytes += length;
      return true;
    }
    dropped++;
    return false;
  }

  private void logPanelError(FlixelDebugPanel panel, RuntimeException e) {
    if (!panelErrorLogged) {
      panelErrorLogged = true;
      LOG.warn("The debug panel '{}' failed while writing its data: {}", panel.getId(), e.toString());
    }
  }

  private void ensureSlots(int count) {
    if (count <= sentRevisions.length) {
      return;
    }
    int old = sentRevisions.length;
    int size = Math.max(count, old * 2);
    long[] revisions = new long[size];
    float[] times = new float[size];
    FlixelDebugPanel[] panels = new FlixelDebugPanel[size];
    System.arraycopy(sentRevisions, 0, revisions, 0, old);
    System.arraycopy(timers, 0, times, 0, old);
    System.arraycopy(owners, 0, panels, 0, old);
    for (int i = old; i < size; i++) {
      revisions[i] = NEVER;
    }
    sentRevisions = revisions;
    timers = times;
    owners = panels;
  }

  private void updateRates(float elapsed) {
    rateTimer += elapsed;
    if (rateTimer >= RATE_WINDOW) {
      manager.stats.setAgentRates(sentBytes / 1024f / rateTimer, recvBytes / 1024f / rateTimer);
      sentBytes = 0L;
      recvBytes = 0L;
      rateTimer = 0f;
    }
  }

  private static String platformName(FlixelPlatform platform) {
    if (platform == FlixelPlatform.DESKTOP) {
      return "desktop";
    } else if (platform == FlixelPlatform.ANDROID) {
      return "android";
    } else if (platform == FlixelPlatform.HTML5) {
      return "web";
    }
    return platform.getId().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns whether the handshake is done and panels are being sent.
   *
   * @return {@code true} while the app is connected and acknowledged.
   */
  public boolean isLive() {
    return phase == LIVE;
  }

  /**
   * Returns whether the agent gave up on this transport, which happens after {@link #stop()} or when the app
   * speaks another major protocol version.
   *
   * @return {@code true} when closed for good.
   */
  public boolean isClosed() {
    return phase == CLOSED;
  }

  /**
   * Returns how many messages were dropped because the transport's queue was full or an incoming message was
   * too long.
   *
   * @return The drop count since the agent was created.
   */
  public long getDropped() {
    return dropped;
  }
}
