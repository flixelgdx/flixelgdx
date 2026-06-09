# Static reference copy (the Android module wires `consumerProguardFiles` to the generated file under
# `build/generated/flixel-reflection/consumer-rules.pro` from `generateReflectionConsumerRules`).
#
# Those rules keep FlixelGDX, libGDX, and optional dependency packages so R8 does not strip APIs that
# games or libraries may still need for reflective access, JNI, serialization, or similar. Adjust
# breadth via `flixelReflectionProfile` and `flixelReflectionExtraPackages` in `gradle.properties`.

-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*
-keep class me.stringdotjar.flixelgdx.** { *; }
