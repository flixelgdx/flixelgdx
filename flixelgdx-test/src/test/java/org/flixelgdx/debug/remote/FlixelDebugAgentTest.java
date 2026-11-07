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
import org.flixelgdx.debug.FlixelDebugManager;
import org.flixelgdx.debug.panels.FlixelDebugPanel;
import org.flixelgdx.debug.panels.FlixelDebugPanelBase;
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

  private static byte[] action(int panel, int action, Consumer<FlixelDebugMessageWriter> fields) {
    FlixelDebugMessageWriter w = new FlixelDebugMessageWriter(64);
    w.beginMessage(FlixelDebugProtocol.MSG_PANEL_ACTION).writeU16(panel).writeU8(action);
    fields.accept(w);
    return bytes(w);
  }

  private static int type(byte[] message) {
    return message[0] & 0xFF;
  }

  private static int panelOf(byte[] message) {
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

  private List<byte[]> dataFor(List<byte[]> messages, int panel) {
    List<byte[]> out = new ArrayList<>();
    for (byte[] m : messages) {
      if (type(m) == FlixelDebugProtocol.MSG_PANEL_DATA && panelOf(m) == panel) {
        out.add(m);
      }
    }
    return out;
  }

  private void live() {
    transport.connect();
    agent.update(FRAME);
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    agent.update(FRAME);
    transport.drain();
  }

  private void disableBuiltIns() {
    for (int i = 0; i < manager.getPanelCount(); i++) {
      manager.getPanel(i).setEnabled(false);
    }
  }

  private int indexOf(String id) {
    for (int i = 0; i < manager.getPanelCount(); i++) {
      if (manager.getPanel(i).getId().equals(id)) {
        return i;
      }
    }
    throw new AssertionError("No panel " + id);
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
  void afterTheAckThePanelListComesFirstThenEveryEnabledPanelInFull() {
    manager.logs.getSink();
    transport.connect();
    agent.update(FRAME);
    transport.drain();
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 3));
    agent.update(FRAME);

    assertTrue(agent.isLive());
    List<byte[]> sent = transport.drain();
    int count = manager.getPanelCount();
    assertEquals(1 + count, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PANEL_LIST, type(sent.get(0)));
    FlixelDebugMessageReader list = reader(sent.get(0));
    assertEquals(count, list.readCount());
    for (int i = 0; i < count; i++) {
      FlixelDebugPanel panel = manager.getPanel(i);
      assertEquals(i, list.readU16());
      assertEquals(panel.getId(), str(list));
      assertEquals(panel.getDisplayName(), str(list));
      assertEquals(panel.getKind(), list.readU8());
      byte[] data = sent.get(1 + i);
      assertEquals(FlixelDebugProtocol.MSG_PANEL_DATA, type(data));
      assertEquals(i, panelOf(data));
    }
    assertEquals(0, list.getRemaining());
  }

  @Test
  void theLogBacklogIsSentWithTheBacklogFlag() {
    FlixelDefaultLogger root = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    root.setConsoleSink(null);
    root.addSink(manager.logs.getSink());
    root.tagged("T").info("hello");
    transport.connect();
    agent.update(FRAME);
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    agent.update(FRAME);

    List<byte[]> logs = dataFor(transport.drain(), indexOf("logs"));
    assertEquals(1, logs.size());
    FlixelDebugMessageReader r = reader(logs.get(0));
    r.readU16();
    assertTrue(r.readBool(), "backlog");
    assertEquals(1, r.readCount());
  }

  @Test
  void aDisabledPanelIsNotSent() {
    manager.textures.setEnabled(false);
    transport.connect();
    agent.update(FRAME);
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    agent.update(FRAME);

    assertEquals(0, dataFor(transport.drain(), indexOf("textures")).size());
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
  void aPanelActionGoesToThePanelAtThatIndex() {
    TestPanel first = manager.register(new TestPanel("custom.first", FlixelDebugProtocol.KIND_CUSTOM));
    TestPanel second = manager.register(new TestPanel("custom.second", FlixelDebugProtocol.KIND_CUSTOM));
    live();
    transport.receive(action(indexOf("custom.second"), 7, w -> w.writeI32(42)));
    agent.update(FRAME);

    assertEquals(0, first.actions.size());
    assertEquals(List.of(7), second.actions);
    assertEquals(List.of(42), second.values);
  }

  @Test
  void aPanelActionForAnUnknownIndexOrBeforeTheAckIsIgnored() {
    TestPanel panel = manager.register(new TestPanel("custom.p", FlixelDebugProtocol.KIND_CUSTOM));
    transport.connect();
    agent.update(FRAME);
    transport.receive(action(indexOf("custom.p"), 1, w -> w.writeI32(1)));
    agent.update(FRAME);
    assertEquals(0, panel.actions.size(), "the handshake is not finished");

    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    transport.receive(action(500, 1, w -> w.writeI32(1)));
    agent.update(FRAME);
    assertEquals(0, panel.actions.size());
    assertTrue(agent.isLive());
  }

  @Test
  void registeringOrUnregisteringAPanelSendsThePanelListAgain() {
    live();
    int before = manager.getPanelCount();
    manager.register(new TestPanel("custom.new", FlixelDebugProtocol.KIND_CUSTOM));
    agent.update(FRAME);

    List<byte[]> sent = transport.drain();
    assertEquals(FlixelDebugProtocol.MSG_PANEL_LIST, type(sent.get(0)));
    assertEquals(before + 1, reader(sent.get(0)).readCount());
    assertEquals(1, dataFor(sent, before).size(), "the new panel is sent in full");
    assertEquals(2, sent.size());

    manager.unregister("custom.new");
    agent.update(FRAME);
    sent = transport.drain();
    assertEquals(1, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PANEL_LIST, type(sent.get(0)));
    assertEquals(before, reader(sent.get(0)).readCount());
  }

  @Test
  void unchangedPanelsAreNotSentAgain() {
    TestPanel panel = manager.register(new TestPanel("custom.p", FlixelDebugProtocol.KIND_CUSTOM));
    live();
    for (int i = 0; i < 30; i++) {
      agent.update(FRAME);
    }
    assertEquals(0, transport.drain().size());

    panel.bump();
    agent.update(FRAME);
    assertEquals(1, dataFor(transport.drain(), indexOf("custom.p")).size());
  }

  @Test
  void ordinaryPanelsAreHeldBackToTheSampleRate() {
    disableBuiltIns();
    TestPanel panel = manager.register(new TestPanel("custom.rate", FlixelDebugProtocol.KIND_CUSTOM));
    manager.controls.setRate(10);
    live();
    int index = indexOf("custom.rate");
    int sends = 0;
    for (int i = 0; i < 120; i++) {
      panel.bump();
      agent.update(1f / 120f);
      sends += dataFor(transport.drain(), index).size();
    }
    assertTrue(sends >= 9 && sends <= 11, "sent " + sends + " times in a second at 10 Hz");

    manager.controls.setRate(30);
    sends = 0;
    for (int i = 0; i < 120; i++) {
      panel.bump();
      agent.update(1f / 120f);
      sends += dataFor(transport.drain(), index).size();
    }
    assertTrue(sends >= 29 && sends <= 31, "sent " + sends + " times in a second at 30 Hz");
  }

  @Test
  void theInspectorRunsAtThirtyHertzWhateverTheSampleRate() {
    disableBuiltIns();
    TestPanel panel = manager.register(new TestPanel("custom.inspector", FlixelDebugProtocol.KIND_INSPECTOR));
    manager.controls.setRate(1);
    live();
    int index = indexOf("custom.inspector");
    int sends = 0;
    for (int i = 0; i < 120; i++) {
      panel.bump();
      agent.update(1f / 120f);
      sends += dataFor(transport.drain(), index).size();
    }
    assertTrue(sends >= 29 && sends <= 31, "sent " + sends + " times in a second");
  }

  @Test
  void logLinesAreSentInBatchesEveryTenthOfASecond() {
    disableBuiltIns();
    manager.logs.setEnabled(true);
    FlixelDefaultLogger root = new FlixelDefaultLogger(FlixelLogMode.SIMPLE);
    root.setConsoleSink(null);
    root.addSink(manager.logs.getSink());
    FlixelLogger log = root.tagged("T");
    manager.controls.setRate(1);
    live();
    int index = indexOf("logs");
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
    disableBuiltIns();
    manager.console.setEnabled(true);
    manager.controls.setRate(1);
    live();
    int index = indexOf("console");
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
  void reconnectingStartsTheHandshakeOverAndResendsEverything() {
    TestPanel panel = manager.register(new TestPanel("custom.p", FlixelDebugProtocol.KIND_CUSTOM));
    live();
    int count = manager.getPanelCount();

    transport.disconnect();
    agent.update(FRAME);
    assertFalse(agent.isLive());
    assertEquals(0, transport.drain().size());
    agent.update(FRAME);
    assertEquals(0, transport.drain().size());

    transport.connect();
    agent.update(FRAME);
    assertEquals(FlixelDebugProtocol.MSG_HELLO, type(transport.drain().get(0)));
    transport.receive(ack(FlixelDebugProtocol.VERSION_MAJOR, 0));
    agent.update(FRAME);
    List<byte[]> sent = transport.drain();
    assertEquals(1 + count, sent.size());
    assertEquals(FlixelDebugProtocol.MSG_PANEL_LIST, type(sent.get(0)));
    assertEquals(1, dataFor(sent, indexOf("custom.p")).size());
    assertEquals(0, panel.actions.size());
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
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PANEL_ACTION });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PANEL_ACTION, 0 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PANEL_ACTION, 0, 0, 3, 1, 2 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_PING, 1, 2 });
    transport.receive(new byte[] { FlixelDebugProtocol.MSG_HELLO_ACK, 1 });
    transport.receive(new byte[] { 0 });
    transport.receive(new byte[FlixelDebugAgent.RECEIVE_BYTES + 10]);
    byte[] noise = new byte[200];
    for (int i = 0; i < noise.length; i++) {
      noise[i] = (byte) (i * 37 + 11);
    }
    for (int type = 0; type < 12; type++) {
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
  void aPanelThatFailsToWriteDoesNotStopTheOthers() {
    disableBuiltIns();
    TestPanel bad = manager.register(new TestPanel("custom.bad", FlixelDebugProtocol.KIND_CUSTOM));
    TestPanel good = manager.register(new TestPanel("custom.good", FlixelDebugProtocol.KIND_CUSTOM));
    bad.fail = true;
    live();
    bad.bump();
    good.bump();
    agent.update(1f);

    List<byte[]> sent = transport.drain();
    assertEquals(0, dataFor(sent, indexOf("custom.bad")).size());
    assertEquals(1, dataFor(sent, indexOf("custom.good")).size());
  }

  @Test
  void aFullQueueSkipsTheUpdateAndTriesAgainNextFrame() {
    disableBuiltIns();
    TestPanel panel = manager.register(new TestPanel("custom.p", FlixelDebugProtocol.KIND_CUSTOM));
    live();
    panel.bump();
    transport.accept = false;
    agent.update(1f);
    assertEquals(0, transport.sent.size());
    assertTrue(agent.getDropped() >= 1);

    transport.accept = true;
    agent.update(FRAME);
    assertEquals(1, dataFor(transport.drain(), indexOf("custom.p")).size());
  }

  @Test
  void theTransferRatesReachTheStatsPanel() {
    disableBuiltIns();
    TestPanel panel = manager.register(new TestPanel("custom.p", FlixelDebugProtocol.KIND_CUSTOM));
    manager.controls.setRate(30);
    live();
    transport.keepSent = false;
    for (int i = 0; i < 70; i++) {
      panel.bump();
      transport.receive(ping(i));
      agent.update(FRAME);
    }
    assertTrue(manager.stats.getSendKbps() > 0f);
    assertTrue(manager.stats.getRecvKbps() > 0f);
  }

  @Test
  void aSteadyStateFrameDoesNotAllocate() {
    TestPanel panel = manager.register(new TestPanel("custom.p", FlixelDebugProtocol.KIND_CUSTOM));
    manager.controls.setRate(30);
    live();
    transport.keepSent = false;
    byte[] ping = ping(1L);
    for (int i = 0; i < 400; i++) {
      panel.bump();
      transport.receive(ping);
      agent.update(FRAME);
    }
    ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
    long id = Thread.currentThread().threadId();
    long before = bean.getThreadAllocatedBytes(id);
    for (int i = 0; i < 400; i++) {
      panel.bump();
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

  private static final class TestPanel extends FlixelDebugPanelBase {

    final List<Integer> actions = new ArrayList<>();
    final List<Integer> values = new ArrayList<>();
    boolean fail;

    TestPanel(String id, int kind) {
      super(id, id, kind);
    }

    void bump() {
      markChanged();
    }

    @Override
    public void write(@NotNull FlixelDebugMessageWriter writer) {
      if (fail) {
        throw new IllegalStateException("broken panel");
      }
      writer.writeI32((int) getRevision());
    }

    @Override
    public void handleAction(int action, @NotNull FlixelDebugMessageReader reader) {
      actions.add(action);
      if (reader.getRemaining() >= 4) {
        values.add(reader.readI32());
      }
    }
  }
}
