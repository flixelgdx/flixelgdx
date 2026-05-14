# Static reference copy (the Android module wires `consumerProguardFiles` to the generated file under
# `build/generated/flixel-reflection/consumer-rules.pro` from `generateReflectionConsumerRules`).
#
# Those rules keep FlixelGDX, libGDX, and optional dependency packages so R8 does not strip APIs that
# games still reach through reflection from their own code or from libraries. Adjust profiles via
# `flixelReflectionProfile` and `flixelReflectionExtraPackages` in `gradle.properties`.

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class me.stringdotjar.flixelgdx.** { *; }
