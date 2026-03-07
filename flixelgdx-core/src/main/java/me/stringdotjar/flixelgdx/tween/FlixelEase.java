package me.stringdotjar.flixelgdx.tween;

/** Class where all easer functions are stored, mostly used for tweening. */
public final class FlixelEase {

  // Easing constants for specific functions.
  private static final float PI2 = (float) Math.PI / 2;
  private static final float EL = (float) ((float) 2 * Math.PI / .45);
  private static final float B1 = (float) ((float) 1 / 2.75);
  private static final float B2 = (float) ((float) 2 / 2.75);
  private static final float B3 = (float) ((float) 1.5 / 2.75);
  private static final float B4 = (float) ((float) 2.5 / 2.75);
  private static final float B5 = (float) ((float) 2.25 / 2.75);
  private static final float B6 = (float) ((float) 2.625 / 2.75);
  private static final float ELASTIC_AMPLITUDE = 1;
  private static final float ELASTIC_PERIOD = 0.4f;

  private FlixelEase() {}

  public static float linear(float t) {
    return t;
  }

  public static float quadIn(float t) {
    return t * t;
  }

  public static float quadOut(float t) {
    return -t * (t - 2);
  }

  public static float quadInOut(float t) {
    return t <= .5 ? t * t * 2 : 1 - (--t) * t * 2;
  }

  public static float cubeIn(float t) {
    return t * t * t;
  }

  public static float cubeOut(float t) {
    return 1 + (--t) * t * t;
  }

  public static float cubeInOut(float t) {
    return t <= .5 ? t * t * t * 4 : 1 + (--t) * t * t * 4;
  }

  public static float quartIn(float t) {
    return t * t * t * t;
  }

  public static float quartOut(float t) {
    return 1 - (t -= 1) * t * t * t;
  }

  public static float quartInOut(float t) {
    return t <= .5 ? t * t * t * t * 8 : (float) ((1 - (t = t * 2 - 2) * t * t * t) / 2 + .5);
  }

  public static float quintIn(float t) {
    return t * t * t * t * t;
  }

  public static float quintOut(float t) {
    return (t = t - 1) * t * t * t * t + 1;
  }

  public static float quintInOut(float t) {
    return ((t *= 2) < 1) ? (t * t * t * t * t) / 2 : ((t -= 2) * t * t * t * t + 2) / 2;
  }

  public static float smoothStepIn(float t) {
    return 2 * smoothStepInOut(t / 2);
  }

  public static float smoothStepOut(float t) {
    return 2 * smoothStepInOut((float) (t / 2 + 0.5)) - 1;
  }

  public static float smoothStepInOut(float t) {
    return t * t * (t * -2 + 3);
  }

  public static float smootherStepIn(float t) {
    return 2 * smootherStepInOut(t / 2);
  }

  public static float smootherStepOut(float t) {
    return 2 * smootherStepInOut((float) (t / 2 + 0.5)) - 1;
  }

  public static float smootherStepInOut(float t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
  }

  public static float sineIn(float t) {
    return (float) (-Math.cos(PI2 * t) + 1);
  }

  public static float sineOut(float t) {
    return (float) Math.sin(PI2 * t);
  }

  public static float sineInOut(float t) {
    return (float) (-Math.cos(Math.PI * t) / 2 + .5);
  }

  public static float bounceIn(float t) {
    return 1 - bounceOut(1 - t);
  }

  public static float bounceOut(float t) {
    if (t < B1) return (float) (7.5625 * t * t);
    if (t < B2) return (float) (7.5625 * (t - B3) * (t - B3) + .75);
    if (t < B4) return (float) (7.5625 * (t - B5) * (t - B5) + .9375);
    return (float) (7.5625 * (t - B6) * (t - B6) + .984375);
  }

  public static float bounceInOut(float t) {
    return t < 0.5 ? (1 - bounceOut(1 - 2 * t)) / 2 : (1 + bounceOut(2 * t - 1)) / 2;
  }

  public static float circIn(float t) {
    return (float) -(Math.sqrt(1 - t * t) - 1);
  }

  public static float circOut(float t) {
    return (float) Math.sqrt(1 - (t - 1) * (t - 1));
  }

  public static float circInOut(float t) {
    return (float)
      (t <= .5
        ? (Math.sqrt(1 - t * t * 4) - 1) / -2
        : (Math.sqrt(1 - (t * 2 - 2) * (t * 2 - 2)) + 1) / 2);
  }

  public static float expoIn(float t) {
    return (float) Math.pow(2, 10 * (t - 1));
  }

  public static float expoOut(float t) {
    return (float) (-Math.pow(2, -10 * t) + 1);
  }

  public static float expoInOut(float t) {
    return (float)
      (t < .5 ? Math.pow(2, 10 * (t * 2 - 1)) / 2 : (-Math.pow(2, -10 * (t * 2 - 1)) + 2) / 2);
  }

  public static float backIn(float t) {
    return (float) (t * t * (2.70158 * t - 1.70158));
  }

  public static float backOut(float t) {
    return (float) (1 - (--t) * (t) * (-2.70158 * t - 1.70158));
  }

  public static float backInOut(float t) {
    t *= 2;
    if (t < 1) return (float) (t * t * (2.70158 * t - 1.70158) / 2);
    t--;
    return (float) ((1 - (--t) * (t) * (-2.70158 * t - 1.70158)) / 2 + .5);
  }

  public static float elasticIn(float t) {
    return (float)
      -(ELASTIC_AMPLITUDE
        * Math.pow(2, 10 * (t -= 1))
        * Math.sin(
        (t - (ELASTIC_PERIOD / (2 * Math.PI) * Math.asin(1 / ELASTIC_AMPLITUDE)))
          * (2 * Math.PI)
          / ELASTIC_PERIOD));
  }

  public static float elasticOut(float t) {
    return (float)
      (ELASTIC_AMPLITUDE
        * Math.pow(2, -10 * t)
        * Math.sin(
        (t - (ELASTIC_PERIOD / (2 * Math.PI) * Math.asin(1 / ELASTIC_AMPLITUDE)))
          * (2 * Math.PI)
          / ELASTIC_PERIOD)
        + 1);
  }

  public static float elasticInOut(float t) {
    if (t < 0.5) {
      return (float)
        (-0.5
          * (Math.pow(2, 10 * (t -= 0.5f))
          * Math.sin((t - (ELASTIC_PERIOD / 4)) * (2 * Math.PI) / ELASTIC_PERIOD)));
    }
    return (float)
      (Math.pow(2, -10 * (t -= 0.5f))
        * Math.sin((t - (ELASTIC_PERIOD / 4)) * (2 * Math.PI) / ELASTIC_PERIOD)
        * 0.5
        + 1);
  }

  @FunctionalInterface
  public interface FunkinEaseFunction {
    float compute(float t);
  }

  @FunctionalInterface
  public interface FunkinEaseStartCallback {
    void run(FlixelTween tween);
  }

  @FunctionalInterface
  public interface FunkinEaseUpdateCallback {
    void run(FlixelTween tween);
  }

  @FunctionalInterface
  public interface FunkinEaseCompleteCallback {
    void run(FlixelTween tween);
  }
}
