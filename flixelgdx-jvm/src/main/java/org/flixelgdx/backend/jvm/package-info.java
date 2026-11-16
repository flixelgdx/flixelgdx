/**
 * Code shared by the JVM backends (desktop and Android) that does not belong in core.
 *
 * <p>Core never touches {@code java.net}, because it is also compiled for the browser. This module holds the pieces
 * that need sockets and the other JVM-only parts of the standard library, and it only uses Java APIs that Android
 * has. Its main class is {@link org.flixelgdx.backend.jvm.FlixelSocketWebSocketTransport}, the connection between
 * a game in debug mode and the remote debugger app.
 *
 * <p>The {@code file} subpackage holds the file system code both backends share:
 * {@link org.flixelgdx.backend.jvm.file.FlixelJvmFile FlixelJvmFile} for files on disk and on the classpath, and
 * {@link org.flixelgdx.backend.jvm.file.FlixelJvmFiles FlixelJvmFiles}, the base each platform's file system extends.
 */
package org.flixelgdx.backend.jvm;
