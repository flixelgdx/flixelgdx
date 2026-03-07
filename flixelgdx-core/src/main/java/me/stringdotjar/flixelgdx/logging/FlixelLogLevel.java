package me.stringdotjar.flixelgdx.logging;

/**
 * An enum that defines the log levels for FlixelGDX's logging system. This is used to determine the
 * severity of a log message and how it should be displayed in the console.
 */
public enum FlixelLogLevel {

  /**
   * Simple white/gray text and simple informational log level that is used for general information about the game.
   */
  INFO,

  /**
   * Highlighted yellow in the console and, although not critical, indicates that something may be
   * wrong and should be looked into.
   */
  WARN,

  /**
   * Highlighted red in the console and indicates an error. Shows something is wrong and
   * should be looked into immediately.
   */
  ERROR
}
