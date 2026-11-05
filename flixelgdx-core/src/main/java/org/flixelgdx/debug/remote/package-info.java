/**
 * Wire protocol and zero-garbage codec for the remote debugger.
 *
 * <p>The game talks to a separate desktop debugger app over WebSocket binary messages. The two sides
 * share no code, only the written specification in {@code DEBUG_PROTOCOL.md} at the repository root.
 * This package holds the Java side of that contract:
 *
 * <ul>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugProtocol FlixelDebugProtocol}: every numeric code.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugMessageWriter FlixelDebugMessageWriter}: builds
 *       messages in a reusable buffer.</li>
 *   <li>{@link org.flixelgdx.debug.remote.FlixelDebugMessageReader FlixelDebugMessageReader}: parses
 *       received messages.</li>
 * </ul>
 *
 * <p>Nothing in this package touches the network, so it is safe on every platform.
 */
package org.flixelgdx.debug.remote;
