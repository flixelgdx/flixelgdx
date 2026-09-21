/*
 * FlixelGDX packagr native launcher stub.
 *
 * This tiny program is the executable a packaged game runs as. It does not contain any game
 * code. At startup it reads the "packagr.cfg" file sitting next to itself, loads the bundled
 * runtime's libjvm, creates a JVM in this very process, and invokes the game's main class. Running
 * the JVM in-process (instead of spawning a child "java" process) means the packaged game shows up
 * as a single process under its own name in the task manager, exactly like a native application.
 *
 * The stub is deliberately dependency-free: it links only against the system C runtime and resolves
 * "JNI_CreateJavaVM" dynamically from the bundled runtime, so one build works with any JDK version.
 *
 * See the README next to this file for how each platform's binary is produced.
 */

#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <jni.h>

#ifdef _WIN32
#include <windows.h>
#define PATH_SEP '\\'
#define CLASSPATH_SEP ";"
#else
#include <dlfcn.h>
#include <dirent.h>
#include <unistd.h>
#include <sys/stat.h>
#define PATH_SEP '/'
#define CLASSPATH_SEP ":"
#endif

#ifdef __APPLE__
#include <mach-o/dyld.h>
#endif

#define MAX_PATH_LEN 4096
#define MAX_VM_ARGS 256

typedef jint(JNICALL *CreateJavaVM_t)(JavaVM **, void **, void *);

/*
 * Reports a fatal startup error to the user. On Windows the stub is a windowless (GUI subsystem)
 * program so it never flashes a console; errors are shown in a message box there instead of being
 * written to a standard stream nobody would see. Every other platform writes to standard error.
 */
static void reportError(const char *msg) {
#ifdef _WIN32
  MessageBoxA(NULL, msg, "FlixelGDX packagr", MB_OK | MB_ICONERROR);
#else
  fprintf(stderr, "packagr: %s\n", msg);
#endif
}

/* Configuration parsed from packagr.cfg. */
typedef struct {
  char mainClass[512];
  char jreDir[256];
  char libsDir[256];
  char *vmArgs[MAX_VM_ARGS];
  int vmArgCount;
} Config;

/* Fills "out" with the directory the running executable lives in (no trailing separator). */
static int exeDir(char *out, size_t cap) {
  char full[MAX_PATH_LEN];
#ifdef _WIN32
  DWORD n = GetModuleFileNameA(NULL, full, (DWORD) sizeof(full));
  if (n == 0 || n >= sizeof(full)) {
    return 0;
  }
#elif defined(__APPLE__)
  uint32_t size = sizeof(full);
  if (_NSGetExecutablePath(full, &size) != 0) {
    return 0;
  }
#else
  ssize_t n = readlink("/proc/self/exe", full, sizeof(full) - 1);
  if (n <= 0) {
    return 0;
  }
  full[n] = '\0';
#endif
  char *slash = strrchr(full, PATH_SEP);
  if (slash == NULL) {
    return 0;
  }
  *slash = '\0';
  if (strlen(full) + 1 > cap) {
    return 0;
  }
  strcpy(out, full);
  return 1;
}

/* Joins a directory and a child name into "out" using the platform separator. */
static void joinPath(char *out, size_t cap, const char *dir, const char *child) {
  snprintf(out, cap, "%s%c%s", dir, PATH_SEP, child);
}

/* Trims a trailing carriage return or newline left by the config file's line endings. */
static void trimEol(char *s) {
  size_t len = strlen(s);
  while (len > 0 && (s[len - 1] == '\n' || s[len - 1] == '\r')) {
    s[--len] = '\0';
  }
}

/* Reads packagr.cfg from "dir" into "cfg". Returns 1 on success, 0 on failure. */
static int readConfig(const char *dir, Config *cfg) {
  memset(cfg, 0, sizeof(*cfg));
  strcpy(cfg->jreDir, "jre");
  strcpy(cfg->libsDir, "lib");

  char cfgPath[MAX_PATH_LEN];
  joinPath(cfgPath, sizeof(cfgPath), dir, "packagr.cfg");
  FILE *file = fopen(cfgPath, "r");
  if (file == NULL) {
    char msg[MAX_PATH_LEN + 64];
    snprintf(msg, sizeof(msg), "cannot open %s", cfgPath);
    reportError(msg);
    return 0;
  }

  char line[2048];
  while (fgets(line, sizeof(line), file) != NULL) {
    trimEol(line);
    if (line[0] == '\0' || line[0] == '#') {
      continue;
    }
    char *eq = strchr(line, '=');
    if (eq == NULL) {
      continue;
    }
    *eq = '\0';
    const char *key = line;
    const char *value = eq + 1;
    if (strcmp(key, "main") == 0) {
      snprintf(cfg->mainClass, sizeof(cfg->mainClass), "%s", value);
    } else if (strcmp(key, "jre") == 0) {
      snprintf(cfg->jreDir, sizeof(cfg->jreDir), "%s", value);
    } else if (strcmp(key, "libs") == 0) {
      snprintf(cfg->libsDir, sizeof(cfg->libsDir), "%s", value);
    } else if (strcmp(key, "vmarg") == 0) {
      if (cfg->vmArgCount < MAX_VM_ARGS) {
        cfg->vmArgs[cfg->vmArgCount++] = strdup(value);
      }
    }
  }
  fclose(file);

  if (cfg->mainClass[0] == '\0') {
    reportError("no 'main' entry in packagr.cfg");
    return 0;
  }
  return 1;
}

/*
 * Builds the "-Djava.class.path=..." option by listing every .jar in "libsAbs". The returned string
 * is heap-allocated and owned by the caller. Returns NULL on allocation failure.
 */
static char *buildClasspath(const char *libsAbs) {
  size_t cap = 8192;
  size_t used = 0;
  char* classPath = (char *) malloc(cap);
  if (classPath == NULL) {
    return NULL;
  }
  classPath[0] = '\0';

#ifdef _WIN32
  char pattern[MAX_PATH_LEN];
  snprintf(pattern, sizeof(pattern), "%s\\*.jar", libsAbs);
  WIN32_FIND_DATAA find;
  HANDLE handle = FindFirstFileA(pattern, &find);
  if (handle != INVALID_HANDLE_VALUE) {
    do {
      char entry[MAX_PATH_LEN];
      int n = snprintf(entry, sizeof(entry), "%s%s%c%s", used > 0 ? CLASSPATH_SEP : "", libsAbs,
                       PATH_SEP, find.cFileName);
      if (n < 0) {
        continue;
      }
      if (used + (size_t) n + 1 > cap) {
        cap = (used + (size_t) n + 1) * 2;
        char *grown = (char *) realloc(classPath, cap);
        if (grown == NULL) {
          FindClose(handle);
          free(classPath);
          return NULL;
        }
        classPath = grown;
      }
      strcpy(classPath + used, entry);
      used += (size_t) n;
    } while (FindNextFileA(handle, &find) != 0);
    FindClose(handle);
  }
#else
  DIR *dir = opendir(libsAbs);
  if (dir != NULL) {
    struct dirent *ent;
    while ((ent = readdir(dir)) != NULL) {
      size_t nameLen = strlen(ent->d_name);
      if (nameLen < 4 || strcmp(ent->d_name + nameLen - 4, ".jar") != 0) {
        continue;
      }
      char entry[MAX_PATH_LEN];
      int n = snprintf(entry, sizeof(entry), "%s%s%c%s", used > 0 ? CLASSPATH_SEP : "", libsAbs,
                       PATH_SEP, ent->d_name);
      if (n < 0) {
        continue;
      }
      if (used + (size_t) n + 1 > cap) {
        cap = (used + (size_t) n + 1) * 2;
        char *grown = (char *) realloc(classPath, cap);
        if (grown == NULL) {
          closedir(dir);
          free(classPath);
          return NULL;
        }
        classPath = grown;
      }
      strcpy(classPath + used, entry);
      used += (size_t) n;
    }
    closedir(dir);
  }
#endif
  return classPath;
}

/* Resolves "JNI_CreateJavaVM" from the bundled runtime's shared library. */
static CreateJavaVM_t loadCreateJavaVM(const char *dir, const char *jreDir) {
  char lib[MAX_PATH_LEN];
#ifdef _WIN32
  snprintf(lib, sizeof(lib), "%s%c%s%cbin%cserver%cjvm.dll", dir, PATH_SEP, jreDir, PATH_SEP,
           PATH_SEP, PATH_SEP);
  HMODULE handle = LoadLibraryA(lib);
  if (handle == NULL) {
    char msg[MAX_PATH_LEN + 64];
    snprintf(msg, sizeof(msg), "cannot load runtime library %s", lib);
    reportError(msg);
    return NULL;
  }
  return (CreateJavaVM_t) GetProcAddress(handle, "JNI_CreateJavaVM");
#else
#ifdef __APPLE__
  snprintf(lib, sizeof(lib), "%s%c%s%clib%cserver%clibjvm.dylib", dir, PATH_SEP, jreDir, PATH_SEP,
           PATH_SEP, PATH_SEP);
#else
  snprintf(lib, sizeof(lib), "%s%c%s%clib%cserver%clibjvm.so", dir, PATH_SEP, jreDir, PATH_SEP,
           PATH_SEP, PATH_SEP);
#endif
  void *handle = dlopen(lib, RTLD_NOW | RTLD_GLOBAL);
  if (handle == NULL) {
    char msg[MAX_PATH_LEN + 128];
    snprintf(msg, sizeof(msg), "cannot load runtime library %s (%s)", lib, dlerror());
    reportError(msg);
    return NULL;
  }
  return (CreateJavaVM_t) dlsym(handle, "JNI_CreateJavaVM");
#endif
}

int main(int argc, char **argv) {
  char dir[MAX_PATH_LEN];
  if (!exeDir(dir, sizeof(dir))) {
    reportError("cannot determine the executable directory");
    return 1;
  }

  Config cfg;
  if (!readConfig(dir, &cfg)) {
    return 1;
  }

  char libsAbs[MAX_PATH_LEN];
  joinPath(libsAbs, sizeof(libsAbs), dir, cfg.libsDir);
  char *classpath = buildClasspath(libsAbs);
  if (classpath == NULL) {
    reportError("could not assemble the classpath");
    return 1;
  }

  CreateJavaVM_t createJavaVM = loadCreateJavaVM(dir, cfg.jreDir);
  if (createJavaVM == NULL) {
    reportError("the bundled runtime does not export JNI_CreateJavaVM");
    free(classpath);
    return 1;
  }

  int optionCount = cfg.vmArgCount + 1;
  JavaVMOption *options = (JavaVMOption *) calloc((size_t) optionCount, sizeof(JavaVMOption));
  if (options == NULL) {
    free(classpath);
    return 1;
  }
  char cpOption[16384];
  snprintf(cpOption, sizeof(cpOption), "-Djava.class.path=%s", classpath);
  options[0].optionString = cpOption;
  int optionUsed = 1;
  for (int i = 0; i < cfg.vmArgCount; i++) {
    // -XstartOnFirstThread is understood by the macOS "java" launcher, not by the JVM, so passing it
    // to JNI_CreateJavaVM would be rejected as unrecognized. This launcher already runs the game's
    // main on the process's first thread, which is exactly what that option asks for, so it is
    // dropped here rather than forwarded.
    if (strcmp(cfg.vmArgs[i], "-XstartOnFirstThread") == 0) {
      continue;
    }
    options[optionUsed++].optionString = cfg.vmArgs[i];
  }

  JavaVMInitArgs vmArgs;
  vmArgs.version = JNI_VERSION_1_8;
  vmArgs.nOptions = optionUsed;
  vmArgs.options = options;
  vmArgs.ignoreUnrecognized = JNI_FALSE;

  JavaVM *jvm = NULL;
  JNIEnv *env = NULL;
  jint created = createJavaVM(&jvm, (void **) &env, &vmArgs);
  free(options);
  free(classpath);
  if (created != JNI_OK) {
    char msg[128];
    snprintf(msg, sizeof(msg), "failed to create the JVM (code %d)", (int) created);
    reportError(msg);
    return 1;
  }

  char className[512];
  snprintf(className, sizeof(className), "%s", cfg.mainClass);
  for (char *c = className; *c != '\0'; c++) {
    if (*c == '.') {
      *c = '/';
    }
  }

  int exitCode = 0;
  jclass mainClass = (*env)->FindClass(env, className);
  if (mainClass == NULL) {
    (*env)->ExceptionDescribe(env);
    char msg[600];
    snprintf(msg, sizeof(msg), "main class '%s' was not found", cfg.mainClass);
    reportError(msg);
    exitCode = 1;
  } else {
    jmethodID mainMethod =
        (*env)->GetStaticMethodID(env, mainClass, "main", "([Ljava/lang/String;)V");
    if (mainMethod == NULL) {
      (*env)->ExceptionDescribe(env);
      char msg[600];
      snprintf(msg, sizeof(msg), "no 'public static void main(String[])' in '%s'", cfg.mainClass);
      reportError(msg);
      exitCode = 1;
    } else {
      jclass stringClass = (*env)->FindClass(env, "java/lang/String");
      jobjectArray appArgs = (*env)->NewObjectArray(env, argc - 1, stringClass, NULL);
      for (int i = 1; i < argc; i++) {
        jstring value = (*env)->NewStringUTF(env, argv[i]);
        (*env)->SetObjectArrayElement(env, appArgs, i - 1, value);
      }
      (*env)->CallStaticVoidMethod(env, mainClass, mainMethod, appArgs);
      if ((*env)->ExceptionCheck(env)) {
        (*env)->ExceptionDescribe(env);
        exitCode = 1;
      }
    }
  }

  (*jvm)->DestroyJavaVM(jvm);
  return exitCode;
}
