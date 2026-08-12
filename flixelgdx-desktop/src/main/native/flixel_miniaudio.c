/*
* MIT License
*
* Copyright (c) 2026 stringdotjar
*
* JNI wrapper over the single-file miniaudio engine (miniaudio.h). Each Java handle is a raw
* pointer into native memory. The framework's FlixelMiniAudio class declares the matching native
* methods; this file implements them and is compiled into the desktop module's bundled natives.
*/
#include <jni.h>
#include <stdlib.h>
#include <string.h>

#define MINIAUDIO_IMPLEMENTATION
#include "miniaudio.h"

// miniaudio has built-in decoders for WAV, MP3, and FLAC, but not Ogg Vorbis. We add Vorbis by
// fully decoding it to PCM with stb_vorbis and playing that back through an in-memory audio buffer.
// Decoding up front is a good fit here (these are game sound effects loaded whole into memory
// anyway) and keeps length, cursor, and seeking exact, which miniaudio's push-mode Vorbis path
// cannot guarantee. We only decode from memory, so the file-based stb_vorbis API is left out. */
#define STB_VORBIS_NO_STDIO
#include "stb_vorbis.c"

// A loaded sound owns whichever data source is live for the voice's lifetime. WAV, MP3, and FLAC
// play from a miniaudio decoder over a private copy of the encoded bytes (the memory decoder
// references that buffer rather than copying it). Ogg Vorbis plays from an audio buffer over the
// PCM that stb_vorbis decoded. In both cases audioData holds the allocation that must outlive the
// sound: the encoded bytes for the decoder path, or the decoded PCM for the Vorbis path.
typedef struct {
  ma_sound        sound;
  ma_decoder      decoder;
  ma_audio_buffer buffer;
  void*           audioData;
  ma_uint32       sampleRate;
  int             isVorbis;
} flixel_sound;

JNIEXPORT jlong JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_engineInit(JNIEnv* env, jclass clazz) {
  (void) env;
  (void) clazz;
  ma_engine* engine = (ma_engine*) malloc(sizeof(ma_engine));
  if (engine == NULL) {
    return 0;
  }
  if (ma_engine_init(NULL, engine) != MA_SUCCESS) {
    free(engine);
    return 0;
  }
  return (jlong) (intptr_t) engine;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_engineUninit(JNIEnv* env, jclass clazz, jlong enginePtr) {
  (void) env;
  (void) clazz;
  ma_engine* engine = (ma_engine*) (intptr_t) enginePtr;
  if (engine != NULL) {
    ma_engine_uninit(engine);
    free(engine);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_engineSetVolume(JNIEnv* env, jclass clazz, jlong enginePtr, jfloat volume) {
  (void) env;
  (void) clazz;
  ma_engine* engine = (ma_engine*) (intptr_t) enginePtr;
  if (engine != NULL) {
    ma_engine_set_volume(engine, volume);
  }
}

JNIEXPORT jlong JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_groupInit(JNIEnv* env, jclass clazz, jlong enginePtr) {
  (void) env;
  (void) clazz;
  ma_engine* engine = (ma_engine*) (intptr_t) enginePtr;
  if (engine == NULL) {
    return 0;
  }
  ma_sound_group* group = (ma_sound_group*) malloc(sizeof(ma_sound_group));
  if (group == NULL) {
    return 0;
  }
  if (ma_sound_group_init(engine, 0, NULL, group) != MA_SUCCESS) {
    free(group);
    return 0;
  }
  return (jlong) (intptr_t) group;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_groupUninit(JNIEnv* env, jclass clazz, jlong groupPtr) {
  (void) env;
  (void) clazz;
  ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
  if (group != NULL) {
    ma_sound_group_uninit(group);
    free(group);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_groupStop(JNIEnv* env, jclass clazz, jlong groupPtr) {
  (void) env;
  (void) clazz;
  ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
  if (group != NULL) {
    ma_sound_group_stop(group);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_groupStart(JNIEnv* env, jclass clazz, jlong groupPtr) {
  (void) env;
  (void) clazz;
  ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
  if (group != NULL) {
    ma_sound_group_start(group);
  }
}

JNIEXPORT jlong JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundLoad(JNIEnv* env, jclass clazz, jlong enginePtr, jbyteArray data, jint length, jlong groupPtr) {
  (void) clazz;
  ma_engine* engine = (ma_engine*) (intptr_t) enginePtr;
  ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
  if (engine == NULL || data == NULL || length <= 0) {
    return 0;
  }

  flixel_sound* s = (flixel_sound*) calloc(1, sizeof(flixel_sound));
  if (s == NULL) {
    return 0;
  }
  s->audioData = malloc((size_t) length);
  if (s->audioData == NULL) {
    free(s);
    return 0;
  }
  (*env)->GetByteArrayRegion(env, data, 0, length, (jbyte*) s->audioData);

  // Ogg streams begin with the "OggS" capture pattern. Route those through stb_vorbis, since
  // miniaudio cannot decode Vorbis on its own; everything else goes to the built-in decoders.
  if (length >= 4 && memcmp(s->audioData, "OggS", 4) == 0) {
    int channels = 0;
    int sampleRate = 0;
    short* pcm = NULL;
    int frameCount = stb_vorbis_decode_memory((const unsigned char*) s->audioData, length,
                                              &channels, &sampleRate, &pcm);
    free(s->audioData);
    s->audioData = NULL;
    if (frameCount < 0 || pcm == NULL) {
      free(s);
      return 0;
    }
    s->audioData = pcm;
    s->sampleRate = (ma_uint32) sampleRate;
    s->isVorbis = 1;

    ma_audio_buffer_config cfg = ma_audio_buffer_config_init(
        ma_format_s16, (ma_uint32) channels, (ma_uint64) frameCount, pcm, NULL);
    // The config initializer does not take a sample rate, so it defaults to zero. Without the
    // real rate the engine would not resample the decoded PCM to its own rate, and the sound
    // would play back at the wrong speed.
    cfg.sampleRate = (ma_uint32) sampleRate;
    if (ma_audio_buffer_init(&cfg, &s->buffer) != MA_SUCCESS) {
      free(pcm);
      free(s);
      return 0;
    }
    if (ma_sound_init_from_data_source(engine, &s->buffer, 0, group, &s->sound) != MA_SUCCESS) {
      ma_audio_buffer_uninit(&s->buffer);
      free(pcm);
      free(s);
      return 0;
    }
    return (jlong) (intptr_t) s;
  }

  if (ma_decoder_init_memory(s->audioData, (size_t) length, NULL, &s->decoder) != MA_SUCCESS) {
    free(s->audioData);
    free(s);
    return 0;
  }
  if (ma_sound_init_from_data_source(engine, &s->decoder, 0, group, &s->sound) != MA_SUCCESS) {
    ma_decoder_uninit(&s->decoder);
    free(s->audioData);
    free(s);
    return 0;
  }
  s->sampleRate = s->decoder.outputSampleRate;
  return (jlong) (intptr_t) s;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundUninit(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_uninit(&s->sound);
    if (s->isVorbis) {
      ma_audio_buffer_uninit(&s->buffer);
    } else {
      ma_decoder_uninit(&s->decoder);
    }
    free(s->audioData);
    free(s);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundStart(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_start(&s->sound);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundStop(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_stop(&s->sound);
  }
}

JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundIsPlaying(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  return (s != NULL && ma_sound_is_playing(&s->sound)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundIsAtEnd(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  return (s == NULL || ma_sound_at_end(&s->sound)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT jfloat JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundGetVolume(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  return (s != NULL) ? ma_sound_get_volume(&s->sound) : 0.0f;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundSetVolume(JNIEnv* env, jclass clazz, jlong soundPtr, jfloat volume) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_set_volume(&s->sound, volume);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundSetPitch(JNIEnv* env, jclass clazz, jlong soundPtr, jfloat pitch) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_set_pitch(&s->sound, pitch);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundSetPan(JNIEnv* env, jclass clazz, jlong soundPtr, jfloat pan) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_set_pan(&s->sound, pan);
  }
}

JNIEXPORT jfloat JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundGetCursor(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s == NULL) {
    return 0.0f;
  }
  float cursor = 0.0f;
  ma_sound_get_cursor_in_seconds(&s->sound, &cursor);
  return cursor;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundSeek(JNIEnv* env, jclass clazz, jlong soundPtr, jfloat seconds) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s == NULL) {
    return;
  }
  ma_uint32 sampleRate = s->sampleRate;
  if (sampleRate == 0) {
    sampleRate = 48000;
  }
  ma_uint64 frame = (ma_uint64) (seconds * (float) sampleRate);
  ma_sound_seek_to_pcm_frame(&s->sound, frame);
}

JNIEXPORT jfloat JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundGetLength(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s == NULL) {
    return 0.0f;
  }
  float length = 0.0f;
  ma_sound_get_length_in_seconds(&s->sound, &length);
  return length;
}

JNIEXPORT jboolean JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundIsLooping(JNIEnv* env, jclass clazz, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  return (s != NULL && ma_sound_is_looping(&s->sound)) ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundSetLooping(JNIEnv* env, jclass clazz, jlong soundPtr, jboolean looping) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_set_looping(&s->sound, looping == JNI_TRUE ? MA_TRUE : MA_FALSE);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundSetPosition(JNIEnv* env, jclass clazz, jlong soundPtr, jfloat x, jfloat y, jfloat z) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  if (s != NULL) {
    ma_sound_set_position(&s->sound, x, y, z);
  }
}
