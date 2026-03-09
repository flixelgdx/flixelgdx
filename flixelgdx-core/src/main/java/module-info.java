module me.stringdotjar.flixelgdx.core {
  exports me.stringdotjar.flixelgdx;
  exports me.stringdotjar.flixelgdx.audio;
  exports me.stringdotjar.flixelgdx.backend;
  exports me.stringdotjar.flixelgdx.display;
  exports me.stringdotjar.flixelgdx.text;
  exports me.stringdotjar.flixelgdx.group;
  exports me.stringdotjar.flixelgdx.input.keyboard;
  exports me.stringdotjar.flixelgdx.input.gamepad;
  exports me.stringdotjar.flixelgdx.logging;
  exports me.stringdotjar.flixelgdx.signal;
  exports me.stringdotjar.flixelgdx.tween;
  exports me.stringdotjar.flixelgdx.tween.builders;
  exports me.stringdotjar.flixelgdx.tween.settings;
  exports me.stringdotjar.flixelgdx.tween.type;
  exports me.stringdotjar.flixelgdx.util;

  // Automatic module names (from JAR filenames when on the module path).
  requires transitive gdx;
  requires transitive gdx.freetype;
  requires transitive anim8.gdx;
  requires transitive libgdx.utils;
  requires transitive miniaudio;
  requires org.fusesource.jansi;
  requires org.jetbrains.annotations;
}
