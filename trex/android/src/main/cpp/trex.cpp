/*
 * Copyright (C) 2023 Wanli Zhu
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "trex.h"

#include <string>

#include "log.h"

FUNC(ArtObjPtr<void>, DecodeJObject, void*, jobject);
FUNC(std::string, Throwable_Dump, void*);
FUNC(const char*, GetSourceFile, void*);

static int Hook(void* address, void* replace, void** origin) {
    _make_rwx(address, _page_size);
    return DobbyHook(address, reinterpret_cast<dobby_dummy_func_t>(replace),
                     reinterpret_cast<dobby_dummy_func_t*>(origin));
}

static void* GetObjectPtr(JNIEnv* env, jobject obj) {
    ArtJNIEnvExt* env_ext = reinterpret_cast<ArtJNIEnvExt*>(env);
    void* thread = env_ext->GetSelf();
    ArtObjPtr<void> objptr = DecodeJObject(thread, obj);
    return objptr.Ptr();
}

HOOK(jobjectArray, NativeGetStackTrace, JNIEnv* env, jclass clazz,
     jobject java_stack_state) {
    jobjectArray result =
        OriginNativeGetStackTrace(env, clazz, java_stack_state);
    jobject proxy = env->CallStaticObjectMethod(trex_android_class,
                                                get_proxy_stack_trace_mid,
                                                java_stack_state, result);
    return reinterpret_cast<jobjectArray>(proxy);
}

JNIFUNC(jstring, nDumpThrowable, jthrowable throwable) {
    if (!DecodeJObject || !Throwable_Dump || !throwable) return nullptr;
    void* throwable_ptr = GetObjectPtr(env, throwable);
    std::string throwable_dumped = Throwable_Dump(throwable_ptr);
    return env->NewStringUTF(throwable_dumped.c_str());
}

JNIFUNCS(void, nHookNativeFunc) {
    if (native_get_stack_trace_func) {
        INLINE_HOOK(NativeGetStackTrace, native_get_stack_trace_func);
        native_get_stack_trace_func = nullptr;
    }
}

JNIFUNC(jstring, nGetSourceFile, jclass clazz) {
    if (!GetSourceFile || !DecodeJObject || !clazz) return nullptr;
    void* class_ptr = GetObjectPtr(env, clazz);
    const char* source_file = GetSourceFile(class_ptr);
    return env->NewStringUTF(source_file);
}

#ifndef __LP64__
FUNC(DvmObject*, dvmDecodeIndirectRef, void*, jobject);
FUNC(char*, dexProtoCopyMethodDescriptor, DvmDexProto*);

JNIFUNC(jstring, nGetDvmDescriptor, jint dvm_method,
        jboolean boot_method_type_visible,
        jboolean synthesized_method_type_visible) {
    DvmMethod* method = reinterpret_cast<DvmMethod*>(dvm_method);

    bool type_visible = dexProtoCopyMethodDescriptor;
    if (type_visible && boot_method_type_visible == JNI_FALSE) {
        DvmObject* class_loader = method->clazz->classLoader;
        if (!class_loader || strcmp(class_loader->clazz->descriptor,
                                    "Ljava/lang/BootClassLoader;") == 0) {
            type_visible = false;
        }
    }
    if (type_visible && synthesized_method_type_visible == JNI_FALSE) {
        uint32_t access_flags = method->clazz->accessFlags;
        if ((access_flags & ACC_SYNTHETIC) != 0) {
            type_visible = false;
        } else {
            access_flags = method->accessFlags;
            if ((access_flags & ACC_BRIDGE) != 0 ||
                (access_flags & ACC_SYNTHETIC) != 0) {
                type_visible = false;
            }
        }
    }

    const char* clazz = method->clazz->descriptor;
    const char* name = method->name;
    const char* signature =
        type_visible ? dexProtoCopyMethodDescriptor(&method->prototype)
                     : "(?)?";

    char descriptor[1024] = {0};
    sprintf(descriptor, "%s->%s%s", clazz, name, signature);

    return env->NewStringUTF(descriptor);
}

JNIFUNC(jobject, nGetDvmMethod, jint dvm_method) {
    jmethodID mid = reinterpret_cast<jmethodID>(dvm_method);
    return env->ToReflectedMethod(this_class, mid, JNI_FALSE);
}
#endif

static const JNINativeMethod gMethods[] = {
    JNIMETHOD(nHookNativeFunc, "()V"),
    JNIMETHOD(nDumpThrowable, "(Ljava/lang/Throwable;)Ljava/lang/String;"),
    JNIMETHOD(nGetSourceFile, "(Ljava/lang/Class;)Ljava/lang/String;"),
#ifndef __LP64__
    JNIMETHOD(nGetDvmDescriptor, "!(IZZ)Ljava/lang/String;"),
    JNIMETHOD(nGetDvmMethod, "!(I)Ljava/lang/reflect/Member;"),
#endif
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {
    JNIEnv* env;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    char sdk_tmp[10];
    __system_property_get("ro.build.version.sdk", sdk_tmp);
    sdk = atoi(sdk_tmp);

    bool art_vm = true;

#ifndef __LP64__
    if (sdk < SDK_L) {
        if (sdk >= SDK_K) {
            if (!env->FindClass("java/lang/reflect/ArtMethod")) {
                env->ExceptionClear();
                art_vm = false;
            }
        } else
            art_vm = false;
    }
#endif

    pine::ElfImg art;
    if (art_vm) {
        art.Open(sdk >= SDK_R   ? LIBART_PATH_R
                 : sdk >= SDK_Q ? LIBART_PATH_Q
                                : LIBART_PATH);
    }

    jclass native_class = nullptr;
    JNINativeMethod native_method = gMethods[0];
    const char* name = native_method.name;
    const char* signature = native_method.signature;

#ifdef __LP64__
    auto Throwable_nativeFillInStackTrace = reinterpret_cast<jobjectArray (*)(
        JNIEnv*, jclass)>(
        art.GetSymbolAddress(
            "_ZN3artL32Throwable_nativeFillInStackTraceEP7_JNIEnvP7_jclass"));
    if (!Throwable_nativeFillInStackTrace) return JNI_ERR;

    jfieldID declaring_class_fid = nullptr;
    bool lollipop = sdk < SDK_M;
    if (lollipop) {
        jclass art_method_class = env->FindClass("java/lang/reflect/ArtMethod");
        declaring_class_fid = env->GetFieldID(
            art_method_class, "declaringClass", "Ljava/lang/Class;");
    }

    jobjectArray backtrace = Throwable_nativeFillInStackTrace(env, nullptr);
    jsize len = env->GetArrayLength(backtrace);
    if (lollipop) len--;

    for (jsize i = lollipop ? 0 : 1; len > i; i++) {
        jobject element = env->GetObjectArrayElement(backtrace, i);
        if (lollipop) {
            element = env->GetObjectField(element, declaring_class_fid);
        }
        native_class = reinterpret_cast<jclass>(element);
        if (!env->GetStaticMethodID(native_class, name, signature)) {
            env->ExceptionClear();
            native_class = nullptr;
        } else
            break;
    }

    env->DeleteLocalRef(backtrace);
#else
    jclass throwable_class = env->FindClass("java/lang/Throwable");
    jmethodID throwable_cid =
        env->GetMethodID(throwable_class, "<init>", "()V");
    jobject throwable = env->NewObject(throwable_class, throwable_cid);
    jmethodID get_stack_trace_mid = env->GetMethodID(
        throwable_class, "getStackTrace", "()[Ljava/lang/StackTraceElement;");
    jobjectArray stack_trace = reinterpret_cast<jobjectArray>(
        env->CallObjectMethod(throwable, get_stack_trace_mid));
    jsize stack_trace_length = env->GetArrayLength(stack_trace);
    jclass element_class = env->FindClass("java/lang/StackTraceElement");
    jmethodID get_class_name_mid =
        env->GetMethodID(element_class, "getClassName", "()Ljava/lang/String;");

    for (jsize i = 0; stack_trace_length > i; i++) {
        jobject element = env->GetObjectArrayElement(stack_trace, i);
        jstring class_name_jstr = reinterpret_cast<jstring>(
            env->CallObjectMethod(element, get_class_name_mid));
        const char* class_name =
            env->GetStringUTFChars(class_name_jstr, nullptr);
        std::string class_name_str(class_name);
        env->ReleaseStringUTFChars(class_name_jstr, class_name);
        std::replace(class_name_str.begin(), class_name_str.end(), '.', '/');
        native_class = env->FindClass(class_name_str.c_str());
        if (!native_class) {
            env->ExceptionClear();
        } else {
            if (!env->GetStaticMethodID(native_class, name, signature)) {
                env->ExceptionClear();
                native_class = nullptr;
            } else
                break;
        }
    }

    env->DeleteLocalRef(throwable);
    env->DeleteLocalRef(stack_trace);
#endif

    jint native_method_count = sizeof(gMethods) / sizeof(JNINativeMethod);
#ifndef __LP64__
    if (art_vm) {
        native_method_count -= 2;
    }
#endif
    if (!native_class || env->RegisterNatives(native_class, gMethods,
                                              native_method_count) != JNI_OK) {
        ALOGE("RegisterNatives failed");
        return JNI_ERR;
    }

#ifndef __LP64__
    if (!art_vm) {
        void* dvm = dlopen("/system/lib/libdvm.so", RTLD_NOW);
        FUNC_REGH(dvmDecodeIndirectRef, dvm,
                  "_Z20dvmDecodeIndirectRefP6ThreadP8_jobject");
        FUNC_REGH(dexProtoCopyMethodDescriptor, dvm,
                  "_Z28dexProtoCopyMethodDescriptorPK8DexProto");
        dlclose(dvm);

        if (dvmDecodeIndirectRef) {
            jclass element_class =
                env->FindClass("java/lang/StackTraceElement");
            jobject element_jobj = env->AllocObject(element_class);
            DvmJNIEnvExt* env_ext = reinterpret_cast<DvmJNIEnvExt*>(env);
            void* thread = env_ext->self;
            DvmObject* element_obj = dvmDecodeIndirectRef(thread, element_jobj);
            DvmClassObject* element_class_obj = element_obj->clazz;
            element_class_obj->accessFlags &= ~ACC_FINAL;
        }
    }
#endif

    if (art_vm) {
        FUNC_REGE(DecodeJObject, art,
                  "_ZNK3art6Thread13DecodeJObjectEP8_jobject");
        FUNC_REGE(Throwable_Dump, art, "_ZN3art6mirror9Throwable4DumpEv");
        FUNC_REGE(GetSourceFile, art, "_ZN3art6mirror5Class13GetSourceFileEv");
        native_get_stack_trace_func = art.GetSymbolAddress(
            "_ZN3artL29Throwable_nativeGetStackTraceEP7_JNIEnvP7_jclassP8_"
            "jobject");

        trex_android_class =
            reinterpret_cast<jclass>(env->NewGlobalRef(native_class));
        get_proxy_stack_trace_mid = env->GetStaticMethodID(
            native_class, "proxy",
            "(Ljava/lang/Object;[Ljava/lang/StackTraceElement;)[Ljava/lang/"
            "StackTraceElement;");
    }

    return JNI_VERSION_1_6;
}
