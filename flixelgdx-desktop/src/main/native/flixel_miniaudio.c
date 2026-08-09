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

/* A loaded sound keeps its decoder and a private copy of the encoded bytes alive for the voice's
 * lifetime, since miniaudio's memory decoder references the buffer rather than copying it. */
typedef struct {
    ma_sound   sound;
    ma_decoder decoder;
    void*      audioData;
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
    return (jlong) (intptr_t) s;
}

JNIEXPORT void JNICALL
Java_org_flixelgdx_backend_desktop_audio_FlixelMiniAudio_soundUninit(JNIEnv* env, jclass clazz, jlong soundPtr) {
    (void) env;
    (void) clazz;
    flixel_sound* s = (flixel_sound*) (intptr_t) soundPtr;
    if (s != NULL) {
        ma_sound_uninit(&s->sound);
        ma_decoder_uninit(&s->decoder);
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
    ma_uint32 sampleRate = s->decoder.outputSampleRate;
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
