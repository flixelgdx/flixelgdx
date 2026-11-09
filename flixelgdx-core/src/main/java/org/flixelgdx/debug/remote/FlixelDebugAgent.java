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
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.feeds.FlixelDebugFeed;
import org.flixelgdx.debug.feeds.FlixelDebugLogs;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugPanels;
import org.flixelgdx.logging.FlixelLogger;
import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * The messenger of the remote debugger: it shuttles protocol messages between the debugger's feeds and panels and a
 * {@link FlixelDebugTransport}.
 *
 * <p>Think of a courier who visits every desk in an office once per frame. The courier introduces the game to the
 * debugger app (the handshake), then walks around collecting what is new, at a pace each desk can bear, and hands
 * the replies of the app (clicks, edits, tool requests, pings) to the right desk. The courier never reads what it
 * carries; every feed and panel writes its own payload, and the transport does the actual traveling. If the app is
 * not there, the courier simply waits at the door.
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
 *   <li>The agent sends {@code PANEL_LIST} and {@code FEED_LIST}.</li>
 *   <li>The app opens the panels it shows ({@code PANEL_OPEN}) and subscribes to the feeds it needs
 *       ({@code FEED_SUBSCRIBE}). The agent sends the whole tree of every panel that opens and the first payload of
 *       every feed that is subscribed (the log feed sends its backlog).</li>
 *   <li>Every frame the agent answers {@code PING} with {@code PONG}, hands each {@code PANEL_EVENT} to its panel and
 *       each {@code FEED_ACTION} to its feed, sends the panel list again when panels were registered, replaced, or
 *       removed, sends patches for open panels that changed, sends the payloads of subscribed feeds that changed,
 *       and forwards the requests panels made to the app (show a panel, reset the layout, show a message).</li>
 * </ol>
 *
 * <h2>How often things are sent</h2>
 *
 * <p>A panel sends a patch at most {@value #PANEL_RATE} times a second, and at once right after it handled an event,
 * so clicks feel instant. A feed is sent only when its {@linkplain FlixelDebugFeed#getRevision() revision} changed.
 * Most feeds are also held back to the sample rate of the controls feed (20 Hz by default). The exceptions are the
 * inspector (up to 30 Hz), the log feed (new lines are sent in batches every 0.1 seconds), and the stats, controls,
 * console, save, and viewport feeds, which are sent as soon as they change. A feed's extra payloads (thumbnails,
 * pictures, and the result of a console command) go out right away.
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

  /** How many bytes one message from the app may have. Larger messages are dropped. */
  public static final int RECEIVE_BYTES = 128 * 1024;

  /** Seconds between two inspector feed updates, at most. */
  public static final float INSPECTOR_INTERVAL = 1f / 30f;

  /** Seconds between two batches of new log lines in the log feed. */
  public static final float LOG_INTERVAL = 0.1f;

  /** How many patches an open panel sends per second, at most. */
  public static final int PANEL_RATE = 30;

  /** Seconds over which the send and receive rates are averaged. */
  public static final float RATE_WINDOW = 1f;

  private static final int IDLE = 0;
  private static final int WAIT_ACK = 1;
  private static final int LIVE = 2;
  private static final int CLOSED = 3;

  private static final int MAX_MESSAGES_PER_FRAME = 256;
  private static final int MAX_PENDING_PER_FRAME = 4;
  private static final int MAX_REQUESTS_PER_FRAME = 8;
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
  private long panelListRevision = NEVER;
  private long dropped;

  private long[] sentRevisions = new long[16];
  private float[] feedTimers = new float[16];
  private boolean[] subscribed = new boolean[16];
  private float[] panelTimers = new float[16];
  private FlixelDebugPanel[] panelOwners = new FlixelDebugPanel[16];
  private String host = FlixelDebugEndpoint.DEFAULT_HOST;
  private float rateTimer;
  private int port = FlixelDebugProtocol.DEFAULT_PORT;
  private int phase = IDLE;

  private boolean started;
  private boolean helloPending;
  private boolean protocolErrorLogged;
  private boolean feedErrorLogged;
  private boolean panelErrorLogged;

  /**
   * Creates an agent. It does nothing until {@link #start(String, int)} is called.
   *
   * @param manager The manager whose feeds and panels are sent.
   * @param transport The connection to the debugger app.
   */
  public FlixelDebugAgent(@NotNull FlixelDebugManager manager, @NotNull FlixelDebugTransport transport) {
    this.manager = manager;
    this.transport = transport;
  }

  /**
   * Opens the transport and starts talking as soon as it connects. Calling it again does nothing.
   *
   * @param host The address of the debugger app.
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
   * Runs one frame of the conversation: notices connections, handles messages from the app, and sends what changed.
   * It does not allocate.
   *
   * @param elapsed Real seconds since the last frame.
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
      pumpPanels(elapsed);
      pumpFeeds(elapsed);
      pumpRequests();
    }
    updateRates(elapsed);
  }

  /** Closes the transport for good. The agent cannot be started again. */
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
    feedErrorLogged = false;
    panelErrorLogged = false;
    resetState();
    LOG.info("Connected to the remote debugger at {}:{}.", host, port);
  }

  private void lost() {
    phase = IDLE;
    helloPending = false;
    resetState();
    manager.stats.setAgentRates(0f, 0f);
    manager.stats.setRoundTrip(-1f);
    manager.panels.closeAll();
    manager.panels.clearRequests();
    // Nobody is waiting for a picture any more, so let the big capture buffers go.
    manager.capture.reset();
    Flixel.graphics.releaseReadbackBuffers();
    LOG.warn("Lost the connection to the remote debugger. Waiting for it to come back.");
  }

  private void resetState() {
    panelListRevision = NEVER;
    sentBytes = 0L;
    recvBytes = 0L;
    rateTimer = 0f;
    for (int i = 0; i < sentRevisions.length; i++) {
      sentRevisions[i] = NEVER;
      feedTimers[i] = 0f;
      subscribed[i] = false;
    }
    for (int i = 0; i < panelOwners.length; i++) {
      panelTimers[i] = 0f;
      panelOwners[i] = null;
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
          if (reader.getRemaining() >= 4) {
            manager.stats.setRoundTrip(reader.readF32());
          }
          writer.beginMessage(FlixelDebugProtocol.MSG_PONG).writeI64(nonce);
          transmit();
        }
        case FlixelDebugProtocol.MSG_FEED_ACTION -> {
          if (phase == LIVE) {
            int index = reader.readU16();
            int action = reader.readU8();
            if (index < manager.getFeedCount()) {
              manager.getFeed(index).handleAction(action, reader);
            }
          }
        }
        case FlixelDebugProtocol.MSG_FEED_SUBSCRIBE -> {
          if (phase == LIVE) {
            int index = reader.readU16();
            boolean on = reader.readBool();
            if (index < manager.getFeedCount()) {
              ensureFeedSlots(index + 1);
              subscribed[index] = on;
              sentRevisions[index] = NEVER;
              feedTimers[index] = 0f;
            }
          }
        }
        case FlixelDebugProtocol.MSG_PANEL_OPEN -> {
          if (phase == LIVE) {
            int index = reader.readU16();
            boolean open = reader.readBool();
            if (index < manager.panels.getCount()) {
              manager.panels.get(index).setOpen(open);
            }
          }
        }
        case FlixelDebugProtocol.MSG_PANEL_EVENT -> {
          if (phase == LIVE) {
            int index = reader.readU16();
            if (index < manager.panels.getCount()) {
              FlixelDebugPanel panel = manager.panels.get(index);
              if (panel.isOpen()) {
                panel.handleEvent(reader);
              }
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
    ensureFeedSlots(manager.getFeedCount());
    sendFeedList();
    if (sendPanelList()) {
      panelListRevision = manager.panels.getRevision();
    }
  }

  private void pumpPanels(float elapsed) {
    FlixelDebugPanels panels = manager.panels;
    long listRevision = panels.getRevision();
    if (listRevision != panelListRevision && sendPanelList()) {
      panelListRevision = listRevision;
    }
    int count = panels.getCount();
    ensurePanelSlots(count);
    float interval = 1f / PANEL_RATE;
    for (int i = 0; i < count; i++) {
      FlixelDebugPanel panel = panels.get(i);
      if (panelOwners[i] != panel) {
        panelOwners[i] = panel;
        panelTimers[i] = 0f;
      }
      if (!panel.isOpen()) {
        continue;
      }
      panelTimers[i] = Math.min(panelTimers[i] + elapsed, MAX_TIMER);
      if (panel.needsTree()) {
        writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_TREE).writeU16(i);
        if (writePanel(panel, true) && transmit()) {
          panelTimers[i] = 0f;
        }
      } else if (panel.hasChanges() && (panel.isUrgent() || panelTimers[i] >= interval)) {
        writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_PATCH).writeU16(i);
        if (writePanel(panel, false)) {
          transmit();
          panelTimers[i] = 0f;
        }
      }
    }
  }

  private boolean writePanel(FlixelDebugPanel panel, boolean tree) {
    try {
      if (tree) {
        panel.writeTree(writer);
      } else {
        panel.writePatch(writer);
      }
      return true;
    } catch (RuntimeException e) {
      if (!panelErrorLogged) {
        panelErrorLogged = true;
        LOG.warn("The debug panel '{}' failed while writing its widgets: {}", panel.getId(), e.toString());
      }
      return false;
    }
  }

  private void pumpFeeds(float elapsed) {
    int count = manager.getFeedCount();
    ensureFeedSlots(count);
    float interval = 1f / manager.controls.getRate();
    for (int i = 0; i < count; i++) {
      FlixelDebugFeed feed = manager.getFeed(i);
      if (!subscribed[i] || !feed.isEnabled()) {
        continue;
      }
      feedTimers[i] = Math.min(feedTimers[i] + elapsed, MAX_TIMER);
      for (int k = 0; k < MAX_PENDING_PER_FRAME && feed.hasPendingPayload(); k++) {
        if (!sendPending(i, feed)) {
          break;
        }
      }
      if (sentRevisions[i] == NEVER) {
        sendFeed(i, feed, true);
        continue;
      }
      long revision = feed.getRevision();
      boolean changed = revision != sentRevisions[i];
      float wait;
      switch (feed.getKind()) {
        case FlixelDebugProtocol.KIND_LOGS -> {
          changed = feed instanceof FlixelDebugLogs logs ? logs.hasNewLines() : changed;
          wait = LOG_INTERVAL;
        }
        case FlixelDebugProtocol.KIND_INSPECTOR -> wait = INSPECTOR_INTERVAL;
        case FlixelDebugProtocol.KIND_STATS, FlixelDebugProtocol.KIND_CONTROLS, FlixelDebugProtocol.KIND_CONSOLE,
            FlixelDebugProtocol.KIND_SAVE, FlixelDebugProtocol.KIND_VIEWPORT ->
          wait = 0f;
        default -> wait = interval;
      }
      if (changed && feedTimers[i] >= wait && sendFeed(i, feed, false)) {
        feedTimers[i] -= wait;
        if (feedTimers[i] > wait) {
          feedTimers[i] = 0f;
        }
      }
    }
  }

  private void pumpRequests() {
    FlixelDebugPanels panels = manager.panels;
    for (int i = 0; i < MAX_REQUESTS_PER_FRAME && panels.hasRequest(); i++) {
      writer.beginMessage(FlixelDebugProtocol.MSG_APP_REQUEST);
      panels.writeRequest(writer);
      if (!transmit()) {
        return;
      }
    }
  }

  private void sendFeedList() {
    int count = Math.min(manager.getFeedCount(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginMessage(FlixelDebugProtocol.MSG_FEED_LIST).beginArray();
    for (int i = 0; i < count; i++) {
      FlixelDebugFeed feed = manager.getFeed(i);
      writer.writeU16(i).writeString(feed.getId()).writeU8(feed.getKind());
    }
    writer.endArray(count);
    transmit();
  }

  private boolean sendPanelList() {
    FlixelDebugPanels panels = manager.panels;
    int count = Math.min(panels.getCount(), FlixelDebugProtocol.MAX_ARRAY_COUNT);
    writer.beginMessage(FlixelDebugProtocol.MSG_PANEL_LIST).beginArray();
    for (int i = 0; i < count; i++) {
      FlixelDebugPanel panel = panels.get(i);
      writer.writeU16(i)
          .writeString(panel.getId())
          .writeString(panel.getTitle())
          .writeU8(panel.getDock())
          .writeBool(panel.isShown());
    }
    writer.endArray(count);
    if (!transmit()) {
      return false;
    }
    // The app opens again the panels it shows, so the game starts from a clean slate with every new list.
    panels.closeAll();
    return true;
  }

  private boolean sendFeed(int index, FlixelDebugFeed feed, boolean first) {
    long revision = feed.getRevision();
    writer.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA).writeU16(index);
    try {
      if (first) {
        feed.writeFirst(writer);
      } else {
        feed.write(writer);
      }
    } catch (RuntimeException e) {
      logFeedError(feed, e);
      sentRevisions[index] = revision;
      return false;
    }
    if (!transmit()) {
      return false;
    }
    sentRevisions[index] = revision;
    return true;
  }

  private boolean sendPending(int index, FlixelDebugFeed feed) {
    writer.beginMessage(FlixelDebugProtocol.MSG_FEED_DATA).writeU16(index);
    try {
      feed.writePendingPayload(writer);
    } catch (RuntimeException e) {
      logFeedError(feed, e);
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

  private void logFeedError(FlixelDebugFeed feed, RuntimeException e) {
    if (!feedErrorLogged) {
      feedErrorLogged = true;
      LOG.warn("The debug feed '{}' failed while writing its data: {}", feed.getId(), e.toString());
    }
  }

  private void ensureFeedSlots(int count) {
    if (count <= sentRevisions.length) {
      return;
    }
    int old = sentRevisions.length;
    int size = Math.max(count, old * 2);
    long[] revisions = new long[size];
    float[] times = new float[size];
    boolean[] subs = new boolean[size];
    System.arraycopy(sentRevisions, 0, revisions, 0, old);
    System.arraycopy(feedTimers, 0, times, 0, old);
    System.arraycopy(subscribed, 0, subs, 0, old);
    for (int i = old; i < size; i++) {
      revisions[i] = NEVER;
    }
    sentRevisions = revisions;
    feedTimers = times;
    subscribed = subs;
  }

  private void ensurePanelSlots(int count) {
    if (count <= panelOwners.length) {
      return;
    }
    int old = panelOwners.length;
    int size = Math.max(count, old * 2);
    float[] times = new float[size];
    FlixelDebugPanel[] owners = new FlixelDebugPanel[size];
    System.arraycopy(panelTimers, 0, times, 0, old);
    System.arraycopy(panelOwners, 0, owners, 0, old);
    panelTimers = times;
    panelOwners = owners;
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
   * Returns whether the handshake finished and the agent is talking to the app.
   *
   * @return {@code true} while live.
   */
  public boolean isLive() {
    return phase == LIVE;
  }

  /**
   * Returns whether the agent was stopped, or closed the connection because the app speaks another major version.
   *
   * @return {@code true} when closed for good.
   */
  public boolean isClosed() {
    return phase == CLOSED;
  }

  /**
   * Returns whether the app subscribed to a feed.
   *
   * @param index The feed's position in the feed list.
   * @return {@code true} while the feed is subscribed.
   */
  public boolean isSubscribed(int index) {
    return index >= 0 && index < subscribed.length && subscribed[index];
  }

  /**
   * Returns how many messages were dropped because the transport's queue was full or a message from the app was too
   * large.
   *
   * @return The count since the agent was created.
   */
  public long getDropped() {
    return dropped;
  }
}
