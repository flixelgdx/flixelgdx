/**
 * Code shared by the JVM backends (desktop and Android) that does not belong in core.
 *
 * <p>Core never touches {@code java.net}, because it is also compiled for the browser. This module holds the pieces
 * that need sockets and the other JVM-only parts of the standard library, and it only uses Java APIs that Android
 * has. Its main class is {@link org.flixelgdx.backend.jvm.FlixelSocketWebSocketTransport}, the connection between
 * a game in debug mode and the remote debugger app.
 */
package org.flixelgdx.backend.jvm;
