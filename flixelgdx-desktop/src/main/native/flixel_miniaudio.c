/*
 * MIT License
 *
 * Copyright (c) 2026 stringdotjar
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

/*
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

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundRestoreRouting(JNIEnv* env, jclass clazz, jlong enginePtr, jlong soundPtr, jlong groupPtr) {
  (void) env;
  (void) clazz;
  flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
  ma_engine*   engine = (ma_engine*) (intptr_t) enginePtr;
  if (s == NULL || engine == NULL) {
    return;
  }
  if (groupPtr != 0) {
    ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
    ma_node_attach_output_bus((ma_node*) &s->sound, 0, (ma_node*) group, 0);
  } else {
    ma_node_graph* graph    = ma_engine_get_node_graph(engine);
    ma_node*       endpoint = ma_node_graph_get_endpoint(graph);
    ma_node_attach_output_bus((ma_node*) &s->sound, 0, endpoint, 0);
  }
}

JNIEXPORT jfloat JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_groupGetVolume(JNIEnv* env, jclass clazz, jlong groupPtr) {
  (void) env;
  (void) clazz;
  ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
  return (group != NULL) ? ma_sound_group_get_volume(group) : 1.0f;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_groupSetVolume(JNIEnv* env, jclass clazz, jlong groupPtr, jfloat volume) {
  (void) env;
  (void) clazz;
  ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
  if (group != NULL) {
    ma_sound_group_set_volume(group, volume);
  }
}

// Effect node types mirror FlixelAudioNodeRegistry built-in IDs:
//   0 = LOW_PASS, 1 = HIGH_PASS, 2 = BAND_PASS, 3 = REVERB (unsupported), 4 = DELAY
typedef enum {
  EFFECT_TYPE_LOW_PASS  = 0,
  EFFECT_TYPE_HIGH_PASS = 1,
  EFFECT_TYPE_BAND_PASS = 2,
  EFFECT_TYPE_REVERB    = 3,
  EFFECT_TYPE_DELAY     = 4
} flixel_effect_type;

// Owns one miniaudio effect node. The engine pointer is stored for live-param reinit (filters
// need the engine's sample rate and channel count when reconfiguring the filter coefficients).
// Cached doubles keep the most recent param values so getParam can return them without a native query.
typedef struct {
  flixel_effect_type type;
  ma_engine*         engine;
  double             cutoffHz;
  double             q;
  ma_uint32          order;
  float              delay;
  float              decay;
  union {
    ma_lpf_node   lpf;
    ma_hpf_node   hpf;
    ma_bpf_node   bpf;
    ma_delay_node delay_node;
  } node;
} flixel_effect_node;

// Returns the ma_node* inside an effect regardless of type.
static ma_node* effect_to_node(flixel_effect_node* fn) {
  switch (fn->type) {
    case EFFECT_TYPE_LOW_PASS:  return (ma_node*) &fn->node.lpf;
    case EFFECT_TYPE_HIGH_PASS: return (ma_node*) &fn->node.hpf;
    case EFFECT_TYPE_BAND_PASS: return (ma_node*) &fn->node.bpf;
    case EFFECT_TYPE_DELAY:     return (ma_node*) &fn->node.delay_node;
    default:                    return NULL;
  }
}

JNIEXPORT jlong JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeCreate(JNIEnv* env, jclass clazz, jlong enginePtr, jlong soundPtr, jint typeId, jfloatArray paramsArray) {
  (void) clazz;
  ma_engine*   engine = (ma_engine*) (intptr_t) enginePtr;
  flixel_sound* s     = (flixel_sound*) (intptr_t) soundPtr;
  if (engine == NULL) {
    return 0;
  }

  jsize    paramLen = (*env)->GetArrayLength(env, paramsArray);
  jfloat*  params   = (*env)->GetFloatArrayElements(env, paramsArray, NULL);

  ma_uint32 channels   = ma_engine_get_channels(engine);
  ma_uint32 sampleRate = ma_engine_get_sample_rate(engine);

  flixel_effect_node* fn = (flixel_effect_node*) calloc(1, sizeof(flixel_effect_node));
  if (fn == NULL) {
    (*env)->ReleaseFloatArrayElements(env, paramsArray, params, JNI_ABORT);
    return 0;
  }
  fn->engine = engine;
  fn->type   = (flixel_effect_type) typeId;

  ma_node_graph* graph = ma_engine_get_node_graph(engine);
  ma_result result = MA_ERROR;

  switch ((flixel_effect_type) typeId) {
    case EFFECT_TYPE_LOW_PASS: {
      fn->cutoffHz = paramLen > 0 ? (double) params[0] : 500.0;
      fn->order    = paramLen > 1 ? (ma_uint32) params[1] : 2;
      ma_lpf_node_config cfg = ma_lpf_node_config_init(channels, sampleRate, fn->cutoffHz, fn->order);
      result = ma_lpf_node_init(graph, &cfg, NULL, &fn->node.lpf);
      break;
    }
    case EFFECT_TYPE_HIGH_PASS: {
      fn->cutoffHz = paramLen > 0 ? (double) params[0] : 500.0;
      fn->order    = paramLen > 1 ? (ma_uint32) params[1] : 2;
      ma_hpf_node_config cfg = ma_hpf_node_config_init(channels, sampleRate, fn->cutoffHz, fn->order);
      result = ma_hpf_node_init(graph, &cfg, NULL, &fn->node.hpf);
      break;
    }
    case EFFECT_TYPE_BAND_PASS: {
      fn->cutoffHz = paramLen > 0 ? (double) params[0] : 500.0;
      fn->q        = paramLen > 1 ? (double) params[1] : 1.0;
      fn->order    = paramLen > 2 ? (ma_uint32) params[2] : 2;
      ma_bpf_node_config cfg = ma_bpf_node_config_init(channels, sampleRate, fn->cutoffHz, fn->order);
      result = ma_bpf_node_init(graph, &cfg, NULL, &fn->node.bpf);
      break;
    }
    case EFFECT_TYPE_DELAY: {
      fn->delay = paramLen > 0 ? params[0] : 0.3f;
      fn->decay = paramLen > 1 ? params[1] : 0.5f;
      ma_uint32 delayFrames = (ma_uint32) (fn->delay * (float) sampleRate);
      ma_delay_node_config cfg = ma_delay_node_config_init(channels, sampleRate, delayFrames, fn->decay);
      result = ma_delay_node_init(graph, &cfg, NULL, &fn->node.delay_node);
      break;
    }
    default:
      break;
  }

  (*env)->ReleaseFloatArrayElements(env, paramsArray, params, JNI_ABORT);

  if (result != MA_SUCCESS) {
    free(fn);
    return 0;
  }
  (void) s;
  return (jlong) (intptr_t) fn;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeSetParam(JNIEnv* env, jclass clazz, jlong nodePtr, jint paramId, jfloat value) {
  (void) env;
  (void) clazz;
  flixel_effect_node* fn = (flixel_effect_node*) (intptr_t) nodePtr;
  if (fn == NULL) {
    return;
  }
  ma_uint32 channels   = ma_engine_get_channels(fn->engine);
  ma_uint32 sampleRate = ma_engine_get_sample_rate(fn->engine);

  switch (fn->type) {
    case EFFECT_TYPE_LOW_PASS:
      if (paramId == 0) {
        fn->cutoffHz = (double) value;
        ma_lpf_config cfg = ma_lpf_config_init(ma_format_f32, channels, sampleRate, fn->cutoffHz, fn->order);
        ma_lpf_node_reinit(&cfg, &fn->node.lpf);
      }
      break;
    case EFFECT_TYPE_HIGH_PASS:
      if (paramId == 0) {
        fn->cutoffHz = (double) value;
        ma_hpf_config cfg = ma_hpf_config_init(ma_format_f32, channels, sampleRate, fn->cutoffHz, fn->order);
        ma_hpf_node_reinit(&cfg, &fn->node.hpf);
      }
      break;
    case EFFECT_TYPE_BAND_PASS:
      if (paramId == 0) {
        fn->cutoffHz = (double) value;
        ma_bpf_config cfg = ma_bpf_config_init(ma_format_f32, channels, sampleRate, fn->cutoffHz, fn->order);
        ma_bpf_node_reinit(&cfg, &fn->node.bpf);
      }
      break;
    case EFFECT_TYPE_DELAY:
      if (paramId == 0) {
        fn->decay = value;
        ma_delay_node_set_decay(&fn->node.delay_node, value);
      }
      break;
    default:
      break;
  }
}

JNIEXPORT jfloat JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeGetParam(JNIEnv* env, jclass clazz, jlong nodePtr, jint paramId) {
  (void) env;
  (void) clazz;
  flixel_effect_node* fn = (flixel_effect_node*) (intptr_t) nodePtr;
  if (fn == NULL) {
    return 0.0f;
  }
  switch (fn->type) {
    case EFFECT_TYPE_LOW_PASS:
    case EFFECT_TYPE_HIGH_PASS:
    case EFFECT_TYPE_BAND_PASS:
      if (paramId == 0) return (jfloat) fn->cutoffHz;
      if (paramId == 1) return (jfloat) fn->q;
      break;
    case EFFECT_TYPE_DELAY:
      if (paramId == 0) return fn->decay;
      break;
    default:
      break;
  }
  return 0.0f;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeConnectToSound(JNIEnv* env, jclass clazz, jlong nodePtr, jlong soundPtr) {
  (void) env;
  (void) clazz;
  flixel_effect_node* fn = (flixel_effect_node*) (intptr_t) nodePtr;
  flixel_sound*        s  = (flixel_sound*) (intptr_t) soundPtr;
  if (fn == NULL || s == NULL) {
    return;
  }
  ma_node* effectNode = effect_to_node(fn);
  if (effectNode != NULL) {
    ma_node_attach_output_bus((ma_node*) &s->sound, 0, effectNode, 0);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeConnect(JNIEnv* env, jclass clazz, jlong downstreamPtr, jlong upstreamPtr) {
  (void) env;
  (void) clazz;
  flixel_effect_node* downstream = (flixel_effect_node*) (intptr_t) downstreamPtr;
  flixel_effect_node* upstream   = (flixel_effect_node*) (intptr_t) upstreamPtr;
  if (downstream == NULL || upstream == NULL) {
    return;
  }
  ma_node* downNode = effect_to_node(downstream);
  ma_node* upNode   = effect_to_node(upstream);
  if (downNode != NULL && upNode != NULL) {
    ma_node_attach_output_bus(upNode, 0, downNode, 0);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeRouteToOutput(JNIEnv* env, jclass clazz, jlong enginePtr, jlong nodePtr, jlong groupPtr) {
  (void) env;
  (void) clazz;
  flixel_effect_node* fn     = (flixel_effect_node*) (intptr_t) nodePtr;
  ma_engine*          engine = (ma_engine*) (intptr_t) enginePtr;
  if (fn == NULL || engine == NULL) {
    return;
  }
  ma_node* effectNode = effect_to_node(fn);
  if (effectNode == NULL) {
    return;
  }
  if (groupPtr != 0) {
    ma_sound_group* group = (ma_sound_group*) (intptr_t) groupPtr;
    ma_node_attach_output_bus(effectNode, 0, (ma_node*) group, 0);
  } else {
    ma_node_graph* graph    = ma_engine_get_node_graph(engine);
    ma_node*       endpoint = ma_node_graph_get_endpoint(graph);
    ma_node_attach_output_bus(effectNode, 0, endpoint, 0);
  }
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_nodeDestroy(JNIEnv* env, jclass clazz, jlong nodePtr) {
  (void) env;
  (void) clazz;
  flixel_effect_node* fn = (flixel_effect_node*) (intptr_t) nodePtr;
  if (fn == NULL) {
    return;
  }
  ma_node_detach_all_output_buses(effect_to_node(fn));
  switch (fn->type) {
    case EFFECT_TYPE_LOW_PASS:  ma_lpf_node_uninit(&fn->node.lpf,        NULL); break;
    case EFFECT_TYPE_HIGH_PASS: ma_hpf_node_uninit(&fn->node.hpf,        NULL); break;
    case EFFECT_TYPE_BAND_PASS: ma_bpf_node_uninit(&fn->node.bpf,        NULL); break;
    case EFFECT_TYPE_DELAY:     ma_delay_node_uninit(&fn->node.delay_node, NULL); break;
    default: break;
  }
  free(fn);
}
