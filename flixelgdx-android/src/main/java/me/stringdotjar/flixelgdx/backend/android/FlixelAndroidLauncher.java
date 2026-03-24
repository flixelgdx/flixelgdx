package me.stringdotjar.flixelgdx.backend.android;

import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import me.stringdotjar.flixelgdx.Flixel;
import me.stringdotjar.flixelgdx.FlixelGame;
import me.stringdotjar.flixelgdx.backend.android.alert.FlixelAndroidAlerter;
import me.stringdotjar.flixelgdx.backend.jvm.logging.FlixelDefaultStackTraceProvider;
import me.stringdotjar.flixelgdx.backend.reflect.FlixelDefaultReflectionHandler;
import me.stringdotjar.flixelgdx.backend.runtime.FlixelRuntimeMode;

/**
 * Launches the Android version of the FlixelGDX game.
 *
 * <p>The developer creates a subclass of {@link FlixelGame} and an Android launcher activity that
 * extends {@link AndroidApplication}. In {@code onCreate}, create the game instance and call
 * {@link #launch(FlixelGame, AndroidApplication)}.
 */
public class FlixelAndroidLauncher {

  /**
   * Launches the Android version of the game in {@link FlixelRuntimeMode#RELEASE RELEASE} mode.
   *
   * @param game The game instance to launch.
   * @param activity The Android application activity.
   */
  public static void launch(FlixelGame game, AndroidApplication activity) {
    launch(game, activity, FlixelRuntimeMode.RELEASE);
  }

  /**
   * Launches the Android version of the game with the given runtime mode.
   *
   * <p>Call this from the {@code onCreate} method of your {@link AndroidApplication} activity.
   * Create your {@link FlixelGame} subclass instance and pass it here along with the activity
   * (typically {@code this}).
   *
   * @param game The game instance to launch (e.g. {@code new MyGame(...)}).
   * @param activity The Android application activity (must extend {@link AndroidApplication}).
   * @param runtimeMode The {@link FlixelRuntimeMode} for this session (TEST, DEBUG, or RELEASE).
   */
  public static void launch(FlixelGame game, AndroidApplication activity, FlixelRuntimeMode runtimeMode) {
    Flixel.initialize(game, new FlixelAndroidAlerter(activity), new FlixelDefaultStackTraceProvider());
    Flixel.setReflection(new FlixelDefaultReflectionHandler());
    Flixel.setRuntimeMode(runtimeMode);
    Flixel.setDebugMode(runtimeMode == FlixelRuntimeMode.DEBUG);

    AndroidApplicationConfiguration configuration = new AndroidApplicationConfiguration();
    configuration.useImmersiveMode = true;

    activity.initialize(game, configuration);
  }
}
