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

import com.sun.management.ThreadMXBean;

import org.flixelgdx.Flixel;
import org.flixelgdx.FlixelVersion;
import org.flixelgdx.collections.FlixelMap;
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.FlixelDebugTrackerEntry;
import org.flixelgdx.debug.feeds.FlixelDebugFeed;
import org.flixelgdx.debug.ui.FlixelDebugButton;
import org.flixelgdx.debug.ui.FlixelDebugLabel;
import org.flixelgdx.debug.ui.FlixelDebugPanel;
import org.flixelgdx.debug.ui.FlixelDebugUi;
import org.flixelgdx.logging.FlixelDefaultLogger;
import org.flixelgdx.logging.FlixelLogMode;
import org.flixelgdx.logging.FlixelLogger;
import org.flixelgdx.util.FlixelString;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlixelDebugAgentTest {

  private static final float FRAME = 1f / 60f;

  private FlixelLogger originalLog;
  private FlixelDebugManager manager;
  private FakeDebugTransport transport;
  private FlixelDebugAgent agent;

  @BeforeEach
  void setUp() {
    originalLog = Flixel.log;
    FlixelDefaultLogger quiet = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    quiet.setConsoleSink(null);
    Flixel.log = quiet;
    manager = new FlixelDebugManager();
    transport = new FakeDebugTransport();
    agent = new FlixelDebugAgent(manager, transport);
    agent.start("127.0.0.1", FlixelDebugProtocol.DEFAULT_PORT);
  }

  @AfterEach
  void tearDown() {
    Flixel.log = originalLog;
    FlixelDebugManager.defaultTransport = FlixelNoopDebugTransport.INSTANCE;
    FlixelDebugManager.defaultEndpoint = FlixelDebugEndpoint.DEFAULT;
  }

  private static byte[] bytes(FlixelDebugMessageWriter w) {
    ByteBuffer b = w.finish();
    byte[] out = new byte[b.remaining()];
    b.get(out);
    return out;
  }

  private static byte[] ack(int major, int minor) {
    return bytes(new FlixelDebugMessageWriter(16).beginMessage(FlixelDebugProtocol.MSG_HELLO_ACK)
        .writeU16(major).writeU16(minor));
  }

  private static byte[] ping(long nonce) {
    return bytes(new FlixelDebugMessageWriter(16).beginMessage(FlixelDebugProtocol.MSG_PING).writeI64(nonce));
  }

  private static byte[] ping(long nonce, float rttMs) {
    return bytes(new FlixelDebugMessageWriter(16).beginMessage(FlixelDebugProtocol.MSG_PING).writeI64(nonce)
        .writeF32(rttMs));
  }

  private static byte[] action(int feed, int action, Consumer<FlixelDebugMessageWriter> fields) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_FEED_ACTION).writeU16(feed).writeU8(action);
    fields.accept(w);
    return bytes(w);
  }

  private static byte[] subscribe(int feed, boolean on) {
    return bytes(new FlixelDebugMessageWriter(16).beginMessage(FlixelDebugProtocol.MSG_FEED_SUBSCRIBE)
        .writeU16(feed).writeBool(on));
  }

  private static byte[] open(int panel, boolean open) {
    return bytes(new FlixelDebugMessageWriter(16).beginMessage(FlixelDebugProtocol.MSG_PANEL_OPEN)
        .writeU16(panel).writeBool(open));
  }

  private static byte[] click(int panel, int widget) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(32);
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_EVENT).writeU16(panel).writeI32(widget)
        .writeU8(FlixelDebugProtocol.EVENT_CLICK).beginArray().endArray(0);
    return bytes(w);
  }

  private static int type(byte[] message) {
    return message[0] & 0xFF;
  }

  private static int indexOf(byte[] message) {
    return (message[1] & 0xFF) | ((message[2] & 0xFF) << 8);
  }

  private static FlixelDebugMessageReader reader(byte[] message) {
    FlixelDebugMessageReader r = new FlixelDebugMessageReader(ByteBuffer.wrap(message));
    r.readU8();
    return r;
  }

  private static String str(FlixelDebugMessageReader r) {
    return r.readString(new FlixelString()).toString();
  }

  private static List<byte[]> of(List<byte[]> messages, int type, int index) {
    List<byte[]> out = new ArrayList<>();
    for (byte[] m : messages) {
      if (type(m) == type && (index < 0 || indexOf(m) == index)) {
        out.add(m);
      }
    }
    return out;
  }

  private List<byte[]> dataFor(List<byte[]> messages, int feed) {
    return of(messages, FlixelDebugProtocol.MSG_FEED_DATA, feed);
  }

  private void live() {
    transport.connect();
    agent.update(FRAME);
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    agent.update(FRAME);
    transport.drain();
  }

  private void subscribeAll() {
    for (int i = 0; i < manager.getFeedCount(); i++) {
      transport.receive(subscribe(i, true));
    }
    agent.update(FRAME);
    transport.drain();
  }

  private int feedIndex(String id) {
    for (int i = 0; i < manager.getFeedCount(); i++) {
      if (manager.getFeed(i).getId().equals(id)) {
        return i;
      }
    }
    throw new AssertionError("No feed " + id);
  }

  private void subscribe(String id) {
    transport.receive(subscribe(feedIndex(id), true));
    agent.update(FRAME);
    transport.drain();
  }

  @Test
  void startOpensTheTransportAtTheGivenAddress() {
    assertTrue(transport.opened);
    assertEquals("127.0.0.1", transport.host);
    assertEquals(6510, transport.port);
  }

  @Test
  void helloIsSentFirstAndNothingElseUntilTheAck() {
    transport.connect();
    agent.update(FRAME);
    agent.update(FRAME);

    List<byte[]> sent = transport.drain();
    assertEquals(1, sent.size());
    FlixelDebugMessageReader r = reader(sent.get(0));
    assertEquals(FlixelDebugProtocol.MSG_HELLO, type(sent.get(0)));
    assertEquals(FlixelDebugProtocol.VERSION_MAJOR, r.readU16());
    assertEquals(FlixelDebugProtocol.VERSION_MINOR, r.readU16());
    assertEquals("unknown", str(r));
    assertEquals("Unknown", str(r));
    r.readI32();
    r.readI32();
    str(r);
    assertEquals(FlixelVersion.VERSION, str(r));
    assertEquals(0, r.getRemaining());
    assertFalse(agent.isLive());
  }

  @Test
  void afterTheAckOnlyTheFeedAndPanelListsAreSent() {
    transport.connect();
    agent.update(FRAME);
    transport.drain();
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 3));
    agent.update(FRAME);
    agent.update(FRAME);

    assertTrue(agent.isLive());
    List<byte[]> sent = transport.drain();
    assertEquals(2, sent.size(), "nothing is subscribed or open yet");

    List<byte[]> feeds = of(sent, FlixelDebugProtocol.MSG_FEED_LIST, -1);
    assertEquals(1, feeds.size());
    FlixelDebugMessageReader list = reader(feeds.get(0));
    assertEquals(manager.getFeedCount(), list.readCount());
    for (int i = 0; i < manager.getFeedCount(); i++) {
      FlixelDebugFeed feed = manager.getFeed(i);
      assertEquals(i, list.readU16());
      assertEquals(feed.getId(), str(list));
      assertEquals(feed.getKind(), list.readU8());
    }
    assertEquals(0, list.getRemaining());

    List<byte[]> panels = of(sent, FlixelDebugProtocol.MSG_PANEL_LIST, -1);
    assertEquals(1, panels.size());
    list = reader(panels.get(0));
    assertEquals(manager.panels.getCount(), list.readCount());
    for (int i = 0; i < manager.panels.getCount(); i++) {
      FlixelDebugPanel panel = manager.panels.get(i);
      assertEquals(i, list.readU16());
      assertEquals(panel.getId(), str(list));
      assertEquals(panel.getTitle(), str(list));
      assertEquals(panel.getDock(), list.readU8());
      assertEquals(panel.isShown(), list.readBool());
    }
    assertEquals(0, list.getRemaining());
  }

  @Test
  void subscribingSendsTheWholeFeedAndUnsubscribingStopsIt() {
    live();
    int watches = feedIndex("watches");
    manager.watches.addInt("x", () -> 1);
    agent.update(1f);
    assertEquals(0, transport.drain().size(), "an unsubscribed feed sends nothing");

    transport.receive(subscribe(watches, true));
    agent.update(FRAME);
    assertEquals(1, dataFor(transport.drain(), watches).size());
    assertTrue(agent.isSubscribed(watches));

    transport.receive(subscribe(watches, false));
    agent.update(FRAME);
    manager.watches.addInt("y", () -> 2);
    agent.update(1f);
    assertEquals(0, dataFor(transport.drain(), watches).size());
    assertFalse(agent.isSubscribed(watches));
  }

  @Test
  void theLogBacklogIsSentWithTheBacklogFlagOnSubscribe() {
    FlixelDefaultLogger root = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    root.setConsoleSink(null);
    root.addSink(manager.logs.getSink());
    root.tagged("T").info("hello");
    live();
    transport.receive(subscribe(feedIndex("logs"), true));
    agent.update(FRAME);

    List<byte[]> logs = dataFor(transport.drain(), feedIndex("logs"));
    assertEquals(1, logs.size());
    FlixelDebugMessageReader r = reader(logs.get(0));
    r.readU16();
    assertTrue(r.readBool(), "backlog");
    assertEquals(1, r.readCount());
  }

  @Test
  void aDisabledFeedIsNotSent() {
    manager.textures.setEnabled(false);
    live();
    subscribeAll();
    manager.textures.refresh();
    agent.update(1f);
    assertEquals(0, dataFor(transport.drain(), feedIndex("textures")).size());
  }

  @Test
  void aMajorVersionMismatchClosesTheTransportForGood() {
    transport.connect();
    agent.update(FRAME);
    transport.drain();
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR + 1, 0));
    agent.update(FRAME);

    assertTrue(transport.closed);
    assertTrue(agent.isClosed());
    assertFalse(agent.isLive());
    transport.connected = true;
    transport.event = true;
    agent.update(FRAME);
    assertEquals(0, transport.sent.size());
  }

  @Test
  void aDifferentMinorVersionIsFine() {
    transport.connect();
    agent.update(FRAME);
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 99));
    agent.update(FRAME);

    assertTrue(agent.isLive());
    assertFalse(transport.closed);
  }

  @Test
  void pingGetsAPongWithTheSameNonce() {
    live();
    transport.receive(ping(0x1122334455667788L));
    transport.receive(ping(-5L));
    agent.update(FRAME);

    List<byte[]> sent = transport.drain();
    assertEquals(2, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PONG, type(sent.get(0)));
    assertEquals(0x1122334455667788L, reader(sent.get(0)).readI64());
    assertEquals(-5L, reader(sent.get(1)).readI64());
  }

  @Test
  void thePingCarriesTheRoundTripTheAppMeasured() {
    live();
    transport.receive(ping(1L, 12.5f));
    agent.update(FRAME);
    assertEquals(12.5f, manager.stats.getRoundTripMs());
    transport.receive(ping(2L, -1f));
    agent.update(FRAME);
    assertEquals(-1f, manager.stats.getRoundTripMs());
  }

  @Test
  void pingIsAnsweredEvenBeforeTheAck() {
    transport.connect();
    agent.update(FRAME);
    transport.drain();
    transport.receive(ping(7L));
    agent.update(FRAME);

    List<byte[]> sent = transport.drain();
    assertEquals(1, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PONG, type(sent.get(0)));
  }

  @Test
  void aFeedActionGoesToTheFeedAtThatIndexEvenWithoutASubscription() {
    live();
    transport.receive(action(feedIndex("controls"), FlixelDebugProtocol.CONTROLS_SET_HITBOXES, w -> w.writeBool(true)));
    agent.update(FRAME);
    assertTrue(manager.controls.isHitboxes());
  }

  @Test
  void aFeedActionForAnUnknownIndexOrBeforeTheAckIsIgnored() {
    transport.connect();
    agent.update(FRAME);
    transport.receive(action(feedIndex("controls"), FlixelDebugProtocol.CONTROLS_SET_HITBOXES, w -> w.writeBool(true)));
    agent.update(FRAME);
    assertFalse(manager.controls.isHitboxes(), "the handshake is not finished");

    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    transport.receive(action(500, 1, w -> w.writeI32(1)));
    agent.update(FRAME);
    assertTrue(agent.isLive());
  }

  @Test
  void changingThePanelsSendsThePanelListAgainAndClosesThem() {
    live();
    int before = manager.panels.getCount();
    transport.receive(open(0, true));
    agent.update(FRAME);
    assertTrue(manager.panels.get(0).isOpen());
    transport.drain();

    manager.panels.register(new FlixelDebugPanel("game.new", "New"));
    agent.update(FRAME);
    List<byte[]> sent = transport.drain();
    assertEquals(1, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PANEL_LIST, type(sent.get(0)));
    assertEquals(before + 1, reader(sent.get(0)).readCount());
    assertFalse(manager.panels.get(0).isOpen(), "the app opens its panels again after a new list");

    manager.panels.unregister("game.new");
    agent.update(FRAME);
    sent = transport.drain();
    assertEquals(1, sent.size());
    assertEquals(before, reader(sent.get(0)).readCount());
  }

  @Test
  void openingAPanelSendsItsTreeThenOnlyPatches() {
    FlixelDebugLabel label = new FlixelDebugLabel("one");
    FlixelDebugPanel panel = manager.panels.register(new FlixelDebugPanel("game.p", "P", FlixelDebugUi.column(label)));
    live();
    int index = manager.panels.indexOf("game.p");
    agent.update(1f);
    assertEquals(0, transport.drain().size(), "a closed panel sends nothing");

    transport.receive(open(index, true));
    agent.update(FRAME);
    List<byte[]> trees = of(transport.drain(), FlixelDebugProtocol.MSG_PANEL_TREE, index);
    assertEquals(1, trees.size());
    FlixelDebugMessageReader r = reader(trees.get(0));
    assertEquals(index, r.readU16());
    assertEquals(FlixelDebugProtocol.WIDGET_STACK, r.readU8());

    agent.update(1f);
    assertEquals(0, transport.drain().size(), "nothing changed");

    label.setText("two");
    agent.update(1f);
    List<byte[]> sent = transport.drain();
    assertEquals(1, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PANEL_PATCH, type(sent.get(0)));
    r = reader(sent.get(0));
    assertEquals(index, r.readU16());
    assertEquals(1, r.readCount());
    assertEquals(label.getId(), r.readI32());
    assertEquals(FlixelDebugProtocol.PATCH_SET, r.readU8());
    assertEquals(FlixelDebugProtocol.PROP_TEXT, r.readU8());
    assertEquals(FlixelDebugProtocol.VALUE_STRING, r.readU8());
    assertEquals("two", str(r));

    transport.receive(open(index, false));
    agent.update(FRAME);
    assertFalse(panel.isOpen());
    label.setText("three");
    agent.update(1f);
    assertEquals(0, transport.drain().size());
  }

  @Test
  void patchesAreHeldBackToThirtyPerSecondExceptAfterAnEvent() {
    FlixelDebugLabel label = new FlixelDebugLabel("0");
    int[] clicks = new int[1];
    FlixelDebugButton button = new FlixelDebugButton("Go", () -> clicks[0]++);
    manager.panels.register(new FlixelDebugPanel("game.p", "P", FlixelDebugUi.column(label, button)));
    live();
    int index = manager.panels.indexOf("game.p");
    transport.receive(open(index, true));
    agent.update(FRAME);
    transport.drain();

    int patches = 0;
    for (int i = 0; i < 120; i++) {
      label.setText((i & 1) == 0 ? "a" : "b");
      agent.update(1f / 120f);
      patches += of(transport.drain(), FlixelDebugProtocol.MSG_PANEL_PATCH, index).size();
    }
    assertTrue(patches >= 29 && patches <= 31, "sent " + patches + " patches in a second");

    transport.receive(click(index, button.getId()));
    agent.update(0.0001f);
    assertEquals(1, clicks[0]);
    label.setText("clicked");
    transport.receive(click(index, button.getId()));
    agent.update(0.0001f);
    assertEquals(2, clicks[0]);
  }

  @Test
  void anEventForAClosedPanelIsIgnored() {
    int[] clicks = new int[1];
    FlixelDebugButton button = new FlixelDebugButton("Go", () -> clicks[0]++);
    manager.panels.register(new FlixelDebugPanel("game.p", "P", button));
    live();
    transport.receive(click(manager.panels.indexOf("game.p"), button.getId()));
    agent.update(FRAME);
    assertEquals(0, clicks[0]);
  }

  @Test
  void requestsOfThePanelsReachTheApp() {
    live();
    manager.panels.show("inspector");
    manager.panels.toast(FlixelDebugProtocol.TONE_SUCCESS, "Saved");
    manager.panels.resetLayout();
    agent.update(FRAME);

    List<byte[]> sent = of(transport.drain(), FlixelDebugProtocol.MSG_APP_REQUEST, -1);
    assertEquals(3, sent.size());
    FlixelDebugMessageReader r = reader(sent.get(0));
    assertEquals(FlixelDebugProtocol.REQUEST_SHOW_PANEL, r.readU8());
    assertEquals("inspector", str(r));
    r = reader(sent.get(1));
    assertEquals(FlixelDebugProtocol.REQUEST_NOTIFY, r.readU8());
    assertEquals(FlixelDebugProtocol.TONE_SUCCESS, r.readU8());
    assertEquals("Saved", str(r));
    assertEquals(FlixelDebugProtocol.REQUEST_RESET_LAYOUT, reader(sent.get(2)).readU8());
  }

  @Test
  void unchangedFeedsAreNotSentAgain() {
    live();
    subscribe("watches");
    for (int i = 0; i < 30; i++) {
      agent.update(FRAME);
    }
    assertEquals(0, transport.drain().size());

    manager.watches.addInt("x", () -> 1);
    agent.update(1f);
    assertEquals(1, dataFor(transport.drain(), feedIndex("watches")).size());
  }

  @Test
  void ordinaryFeedsAreHeldBackToTheSampleRate() {
    manager.controls.setRate(10);
    live();
    subscribe("watches");
    int index = feedIndex("watches");
    int sends = 0;
    for (int i = 0; i < 120; i++) {
      manager.watches.addInt("x", () -> 1);
      agent.update(1f / 120f);
      sends += dataFor(transport.drain(), index).size();
    }
    assertTrue(sends >= 9 && sends <= 11, "sent " + sends + " times in a second at 10 Hz");

    manager.controls.setRate(30);
    sends = 0;
    for (int i = 0; i < 120; i++) {
      manager.watches.addInt("x", () -> 1);
      agent.update(1f / 120f);
      sends += dataFor(transport.drain(), index).size();
    }
    assertTrue(sends >= 29 && sends <= 31, "sent " + sends + " times in a second at 30 Hz");
  }

  @Test
  void theInspectorRunsAtThirtyHertzWhateverTheSampleRate() {
    manager.controls.setRate(1);
    live();
    subscribe("inspector");
    int index = feedIndex("inspector");
    Object target = new Object();
    int sends = 0;
    for (int i = 0; i < 120; i++) {
      manager.inspector.select(target);
      agent.update(1f / 120f);
      sends += dataFor(transport.drain(), index).size();
    }
    assertTrue(sends >= 29 && sends <= 31, "sent " + sends + " times in a second");
  }

  @Test
  void logLinesAreSentInBatchesEveryTenthOfASecond() {
    FlixelDefaultLogger root = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    root.setConsoleSink(null);
    root.addSink(manager.logs.getSink());
    FlixelLogger log = root.tagged("T");
    manager.controls.setRate(1);
    live();
    subscribe("logs");
    int index = feedIndex("logs");
    int batches = 0;
    int lines = 0;
    for (int i = 0; i < 120; i++) {
      log.info("line");
      agent.update(1f / 120f);
      for (byte[] m : dataFor(transport.drain(), index)) {
        batches++;
        FlixelDebugMessageReader r = reader(m);
        r.readU16();
        assertFalse(r.readBool());
        lines += r.readCount();
      }
    }
    assertTrue(batches >= 9 && batches <= 11, "sent " + batches + " batches");
    assertTrue(lines >= 108, "no line was lost: " + lines);
  }

  @Test
  void aConsoleResultIsSentRightAfterTheRunActionDespiteTheRate() {
    manager.controls.setRate(1);
    live();
    subscribe("console");
    int index = feedIndex("console");
    transport.receive(action(index, FlixelDebugProtocol.CONSOLE_RUN, w -> w.writeString("no-such-command")));
    agent.update(0.001f);

    List<byte[]> data = dataFor(transport.drain(), index);
    assertEquals(1, data.size());
    FlixelDebugMessageReader r = reader(data.get(0));
    r.readU16();
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT, r.readU8());
    assertEquals(FlixelDebugProtocol.CONSOLE_RESULT_UNKNOWN, r.readU8());
  }

  @Test
  void reconnectingStartsTheHandshakeOverAndForgetsSubscriptionsAndOpenPanels() {
    live();
    subscribeAll();
    transport.receive(open(0, true));
    agent.update(FRAME);
    transport.drain();

    transport.disconnect();
    agent.update(FRAME);
    assertFalse(agent.isLive());
    assertFalse(manager.panels.get(0).isOpen());
    assertEquals(0, transport.drain().size());

    transport.connect();
    agent.update(FRAME);
    assertEquals(FlixelDebugProtocol.MSG_HELLO, type(transport.drain().get(0)));
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    agent.update(FRAME);
    List<byte[]> sent = transport.drain();
    assertEquals(2, sent.size());
    for (int i = 0; i < manager.getFeedCount(); i++) {
      assertFalse(agent.isSubscribed(i));
    }
  }

  @Test
  void aConnectionThatFlapsBetweenFramesAlsoStartsOver() {
    live();
    transport.disconnect();
    transport.connect();
    agent.update(FRAME);

    List<byte[]> sent = transport.drain();
    assertEquals(1, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_HELLO, type(sent.get(0)));
    assertFalse(agent.isLive());
  }

  @Test
  void garbageFromTheAppNeverCrashesTheAgent() {
    live();
    transport.receive(new byte[] { 99 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_FEED_ACTION });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_FEED_ACTION, 0 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_FEED_ACTION, 0, 0, 3, 1, 2 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PANEL_EVENT, 0, 0, 1 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PANEL_OPEN, 0 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_FEED_SUBSCRIBE, (byte) 0xFF, (byte) 0xFF, 1 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PING, 1, 2 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_HELLO_ACK, 1 });
    transport.receive(new byte[] { 0 });
    transport.receive(new byte[FlixelDebugAgent.RECEIVE_BYTES + 10]);
    byte[] noise = new byte[200];
    for (int i = 0; i < noise.length; i++) {
      noise[i] = (byte) (i * 37 + 11);
    }
    for (int type = 0; type < 16; type++) {
      noise[0] = (byte) type;
      transport.receive(noise.clone());
    }
    agent.update(FRAME);
    transport.receive(ping(9L));
    agent.update(FRAME);

    assertTrue(agent.isLive());
    assertTrue(agent.getDropped() >= 1, "the oversized message is dropped and counted");
    List<byte[]> sent = transport.drain();
    assertEquals(FlixelDebugProtocol.MSG_PONG, type(sent.get(sent.size() - 1)));
    assertEquals(9L, reader(sent.get(sent.size() - 1)).readI64());
  }

  @Test
  void aFeedThatFailsToWriteDoesNotStopTheOthers() {
    manager.trackers.add(new Entry(null));
    live();
    subscribe("trackers");
    subscribe("watches");
    manager.trackers.add(new Entry(null));
    manager.watches.addInt("x", () -> 1);
    agent.update(1f);

    List<byte[]> sent = transport.drain();
    assertEquals(0, dataFor(sent, feedIndex("trackers")).size());
    assertEquals(1, dataFor(sent, feedIndex("watches")).size());
  }

  @Test
  void aPanelThatFailsToWriteDoesNotStopTheOthers() {
    FlixelDebugLabel good = new FlixelDebugLabel("ok");
    manager.panels.register(new FlixelDebugPanel("game.bad", "Bad") {
      @Override
      public void writeTree(@NotNull FlixelDebugMessageWriter writer) {
        throw new IllegalStateException("broken panel");
      }
    });
    manager.panels.register(new FlixelDebugPanel("game.good", "Good", good));
    live();
    transport.receive(open(manager.panels.indexOf("game.bad"), true));
    transport.receive(open(manager.panels.indexOf("game.good"), true));
    agent.update(FRAME);

    List<byte[]> sent = transport.drain();
    assertEquals(0, of(sent, FlixelDebugProtocol.MSG_PANEL_TREE, manager.panels.indexOf("game.bad")).size());
    assertEquals(1, of(sent, FlixelDebugProtocol.MSG_PANEL_TREE, manager.panels.indexOf("game.good")).size());
  }

  @Test
  void aFullQueueSkipsTheUpdateAndTriesAgainNextFrame() {
    live();
    subscribe("watches");
    manager.watches.addInt("x", () -> 1);
    transport.accept = false;
    agent.update(1f);
    assertEquals(0, transport.sent.size());
    assertTrue(agent.getDropped() >= 1);

    transport.accept = true;
    agent.update(FRAME);
    assertEquals(1, dataFor(transport.drain(), feedIndex("watches")).size());
  }

  @Test
  void theTransferRatesReachTheStatsFeed() {
    manager.controls.setRate(30);
    live();
    subscribe("watches");
    transport.keepSent = false;
    for (int i = 0; i < 70; i++) {
      manager.watches.addInt("x", () -> 1);
      transport.receive(ping(i));
      agent.update(FRAME);
    }
    assertTrue(manager.stats.getSendKbps() > 0f);
    assertTrue(manager.stats.getRecvKbps() > 0f);
  }

  @Test
  void aSteadyStateFrameDoesNotAllocate() {
    FlixelDebugLabel label = new FlixelDebugLabel("a");
    manager.panels.register(new FlixelDebugPanel("game.p", "P", FlixelDebugUi.column(label)));
    manager.controls.setRate(30);
    live();
    transport.receive(open(manager.panels.indexOf("game.p"), true));
    subscribe("watches");
    transport.keepSent = false;
    byte[] ping = ping(1L, 3f);
    for (int i = 0; i < 400; i++) {
      label.setText((i & 1) == 0 ? "a" : "b");
      transport.receive(ping);
      agent.update(FRAME);
    }
    ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    long id = Thread.currentThread().threadId();
    long before = bean.getThreadAllocatedBytes(id);
    for (int i = 0; i < 400; i++) {
      label.setText((i & 1) == 0 ? "a" : "b");
      transport.receive(ping);
      agent.update(FRAME);
    }
    long used = bean.getThreadAllocatedBytes(id) - before;
    assertTrue(used < 4096, "allocated " + used + " bytes");
  }

  @Test
  void theManagerRunsTheAgentOnlyWhenATransportIsInstalled() {
    FlixelDebugManager plain = new FlixelDebugManager();
    plain.start();
    assertNull(plain.getAgent());
    plain.update(FRAME);
    plain.dispose();

    FlixelDebugManager.defaultTransport = transport;
    FlixelDebugManager.defaultEndpoint = FlixelDebugEndpoint.parse("pc.local:7000");
    FlixelDebugManager wired = new FlixelDebugManager();
    wired.start();
    assertNotNull(wired.getAgent());
    assertSame(transport, wired.getTransport());
    assertEquals("pc.local", transport.host);
    assertEquals(7000, transport.port);

    transport.connect();
    wired.update(FRAME);
    assertEquals(FlixelDebugProtocol.MSG_HELLO, type(transport.drain().get(0)));
    wired.dispose();
    assertTrue(transport.closed);
    assertNull(wired.getAgent());
  }

  @Test
  void setTransportSwapsTheConnectionOfARunningDebugger() {
    FlixelDebugManager running = new FlixelDebugManager();
    running.start();
    running.setTransport(transport);
    assertTrue(transport.opened);
    FakeDebugTransport other = new FakeDebugTransport();
    running.setTransport(other);
    assertTrue(transport.closed);
    assertTrue(other.opened);
    running.dispose();
    assertTrue(other.closed);
  }

  /** A tracker whose values are fixed, or whose getter throws when it has no name. */
  private static final class Entry extends FlixelDebugTrackerEntry {

    private final FlixelMap<String, String> values = new FlixelMap<>();
    private final boolean broken;

    Entry(String name) {
      super(name != null ? name : "Broken");
      broken = name == null;
      values.put("x", "1");
    }

    @Override
    public FlixelMap<String, String> getTrackedValues() {
      if (broken) {
        throw new IllegalStateException("broken tracker");
      }
      return values;
    }
  }
}
