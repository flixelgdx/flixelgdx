/**
 * The wire protocol of the remote debugger, and the agent that carries messages between the game and the app.
 *
 * <p>The debugger app is a separate desktop program, often on another machine than the game (a phone, a browser tab,
 * a second computer). The two sides share no code at all. All they share is a written agreement:
 * {@code DEBUG_PROTOCOL.md} at the repository root, which says exactly which bytes mean what. Anyone who reads that
 * file can write a compatible app in any language. This package is the Java side of that agreement.
 *
 * <p>Think of two embassies that speak different languages. They agree on a phrasebook (the protocol constants), a
 * way to fill in and read forms (the message writer and reader), and a diplomatic courier who carries the forms back
 * and forth every day (the agent). How the courier travels, by train or by plane, is a separate question (the
 * transport). Neither embassy needs to know anything else about the other.
 *
 * <h2>Where to start</h2>
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugProtocol FlixelDebugProtocol}: every numeric code of the
 *       protocol, such as message types, feed kinds, widget types, and style bits. Each name matches a row of
 *       {@code DEBUG_PROTOCOL.md}.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugMessageWriter FlixelDebugMessageWriter}: builds a message in
 *       one reusable buffer.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugMessageReader FlixelDebugMessageReader}: reads a received
 *       message field by field, with bounds checks.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugAgent FlixelDebugAgent}: the courier. It runs the handshake,
 *       sends panels and feeds, and hands the app's events and requests to the right panel or feed.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugTransport FlixelDebugTransport}: the pipe the agent talks
 *       through, which each platform supplies.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugEndpoint FlixelDebugEndpoint}: the host and port of the app.
 *       </li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugMessageQueue FlixelDebugMessageQueue}: a fixed-size queue of
 *       reusable message slots that transports use to hand messages between threads.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelNoopDebugTransport FlixelNoopDebugTransport}: the default transport,
 *       which connects to nothing.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugProtocolException FlixelDebugProtocolException}: thrown for a
 *       message that is cut short or cannot be encoded, so a bad message is dropped instead of crashing the game.
 *       </li>
 * </ul>
 *
 * <p>Game code rarely touches this package. The
 * {@link org.flixelgdx.debug.FlixelDebugManager FlixelDebugManager} creates and runs the agent by itself, and panels
 * and feeds write their own payloads. You come here when you write a transport for a new platform, or when you want
 * to understand what travels over the wire.
 *
 * <h2>The conversation</h2>
 *
 * <p>The game is the client and the app is the server. Every message is one binary WebSocket message, and every
 * conversation goes like this:
 *
 * <ol>
 *   <li>The game connects and sends {@code HELLO} with its protocol version, platform, and window size.</li>
 *   <li>The app answers {@code HELLO_ACK}. If the major versions differ, the game logs a warning and stops talking,
 *       because neither side could understand the other.</li>
 *   <li>The game sends {@code PANEL_LIST} (every panel's id, title, and dock position) and {@code FEED_LIST} (every
 *       feed's id and kind).</li>
 *   <li>The app sends {@code PANEL_OPEN} for the panels it shows and {@code FEED_SUBSCRIBE} for the feeds its tools
 *       need. The game answers with a full {@code PANEL_TREE} for every open panel and a first {@code FEED_DATA} for
 *       every subscribed feed.</li>
 *   <li>From then on, every frame, the game sends {@code PANEL_PATCH} messages with the properties that changed and
 *       {@code FEED_DATA} for subscribed feeds that changed. The app sends {@code PANEL_EVENT} when a person uses a
 *       widget and {@code FEED_ACTION} when a tool asks for something. {@code PING} and {@code PONG} measure the
 *       round trip, and {@code APP_REQUEST} lets the game ask the app to show a panel or a short message.</li>
 * </ol>
 *
 * <p>If the connection drops, the transport reconnects on its own and the whole conversation starts over from
 * {@code HELLO}, with everything sent again in full. Nothing is lost except the moments in between.
 *
 * <h2>Pacing</h2>
 *
 * <p>The agent never floods the network. A panel sends a patch at most
 * {@value org.flixelgdx.debug.remote.FlixelDebugAgent#PANEL_RATE} times a second, and right away after it handled an
 * event so clicks feel instant. A feed is only sent when its revision changed, and most feeds are also held to the
 * debugger's sample rate. When the transport's outgoing queue is full, regular updates wait for the next frame
 * instead of blocking the game.
 *
 * <h2>Writing a transport</h2>
 *
 * <p>Core never opens a socket itself, so it runs unchanged on every platform. A platform module supplies a
 * {@link org.flixelgdx.debug.remote.FlixelDebugTransport FlixelDebugTransport} and its launcher installs it in
 * {@link org.flixelgdx.debug.FlixelDebugManager#defaultTransport FlixelDebugManager.defaultTransport} before the game
 * starts, together with the address in
 * {@link org.flixelgdx.debug.FlixelDebugManager#defaultEndpoint FlixelDebugManager.defaultEndpoint}.
 *
 * <p>A transport has six methods: {@code open(...)} starts connecting in the background and keeps retrying,
 * {@code isConnected()} reports the state, {@code send(...)} queues one outgoing message, {@code poll(...)} hands over
 * one received message, {@code consumeConnectedEvent()} reports each new connection once, and {@code close()} stops
 * for good. Every method is called on the game thread and must return at once. A transport that needs a network
 * thread owns it, and hands messages across with two
 * {@link org.flixelgdx.debug.remote.FlixelDebugMessageQueue FlixelDebugMessageQueue} instances guarded by one lock:
 *
 * <pre>{@code
 * public class MyTransport implements FlixelDebugTransport {
 *
 *   private final Object lock = new Object();
 *   private final FlixelDebugMessageQueue outgoing = new FlixelDebugMessageQueue(64, 4096);
 *   private final FlixelDebugMessageQueue incoming = new FlixelDebugMessageQueue(64, 1024);
 *   private volatile boolean connected;
 *   private volatile boolean connectedEvent;
 *
 *   @Override
 *   public void open(String host, int port) {
 *     // Start a network thread that connects, retries, and moves messages between the socket and the queues.
 *     // It sets connected and connectedEvent to true each time a connection succeeds.
 *   }
 *
 *   @Override
 *   public boolean isConnected() {
 *     return connected;
 *   }
 *
 *   @Override
 *   public boolean send(ByteBuffer message) {
 *     if (!connected) {
 *       return false;
 *     }
 *     synchronized (lock) {
 *       return outgoing.offer(message); // Copies the bytes, so the agent can reuse its buffer at once.
 *     }
 *   }
 *
 *   @Override
 *   public int poll(ByteBuffer into) {
 *     into.clear();
 *     synchronized (lock) {
 *       int length = incoming.peekLength();
 *       if (length < 0) {
 *         return 0;                       // Nothing waiting.
 *       }
 *       if (length > into.capacity()) {
 *         incoming.discard();
 *         return -length;                 // Too big for the agent's buffer.
 *       }
 *       incoming.poll(into);
 *     }
 *     into.flip();
 *     return into.limit();
 *   }
 *
 *   @Override
 *   public boolean consumeConnectedEvent() {
 *     boolean event = connectedEvent;
 *     connectedEvent = false;
 *     return event;
 *   }
 *
 *   @Override
 *   public void close() {
 *     // Stop the network thread and close the socket. The transport is not reused afterward.
 *   }
 * }
 *
 * // In the platform launcher, before the game starts:
 * FlixelDebugManager.defaultTransport = new MyTransport();
 * FlixelDebugManager.defaultEndpoint = FlixelDebugEndpoint.parse(System.getProperty("flixel.debugHost"));
 * }</pre>
 *
 * <p>Keep slow work, such as writing to the socket, outside the lock, so the game thread never waits on the network.
 * When a message does not fit in the outgoing queue, drop it and return {@code false}: the agent tries again with
 * fresh data next frame. Read the full contract in the documentation of
 * {@link org.flixelgdx.debug.remote.FlixelDebugTransport FlixelDebugTransport}.
 *
 * <h2>Threading and performance</h2>
 *
 * <ul>
 *   <li>The agent, the writer, and the reader run on the game's main thread only. They are not thread safe.</li>
 *   <li>After a short warm-up nothing here allocates: the writer reuses one buffer that only grows, the reader copies
 *       text into a reused {@link org.flixelgdx.util.FlixelString FlixelString}, and the queue reuses its slots.</li>
 *   <li>Every read is bounds checked. A short or corrupt message throws a
 *       {@link org.flixelgdx.debug.remote.FlixelDebugProtocolException FlixelDebugProtocolException}, which the agent
 *       catches and drops, so a misbehaving app can never crash the game.</li>
 *   <li>Bytes after the last field a reader knows about are ignored, so a newer minor version of the protocol can
 *       add fields without breaking older games or apps.</li>
 * </ul>
 *
 * @see org.flixelgdx.debug.remote.FlixelDebugAgent
 * @see org.flixelgdx.debug.remote.FlixelDebugTransport
 * @see org.flixelgdx.debug.remote.FlixelDebugProtocol
 */
package org.flixelgdx.debug.remote;
