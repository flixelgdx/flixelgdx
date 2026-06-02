module org.flixelgdx.core {
  exports org.flixelgdx;
  exports org.flixelgdx.animation;
  exports org.flixelgdx.asset;
  exports org.flixelgdx.audio;
  exports org.flixelgdx.backend.alert;
  exports org.flixelgdx.backend.host;
  exports org.flixelgdx.backend.window;
  exports org.flixelgdx.backend.runtime;
  exports org.flixelgdx.debug;
  exports org.flixelgdx.functional;
  exports org.flixelgdx.functional.supplier;
  exports org.flixelgdx.graphics;
  exports org.flixelgdx.text;
  exports org.flixelgdx.group;
  exports org.flixelgdx.input;
  exports org.flixelgdx.input.action;
  exports org.flixelgdx.input.gamepad;
  exports org.flixelgdx.input.keyboard;
  exports org.flixelgdx.input.mouse;
  exports org.flixelgdx.logging;
  exports org.flixelgdx.tween;
  exports org.flixelgdx.tween.settings;
  exports org.flixelgdx.tween.type;
  exports org.flixelgdx.tween.type.motion;
  exports org.flixelgdx.ui;
  exports org.flixelgdx.util;
  exports org.flixelgdx.util.save;
  exports org.flixelgdx.util.signal;
  exports org.flixelgdx.util.timer;

    // Automatic module names (from JAR filenames when on the module path).
  requires transitive gdx;
  requires transitive gdx.controllers.core;
  requires transitive gdx.freetype;
  requires transitive anim8.gdx;
  requires transitive libgdx.utils;
  requires org.jetbrains.annotations;
}
