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

#ifndef TREX_TREX_H
#define TREX_TREX_H

#include <dlfcn.h>
#include <dobby.h>
#include <jni.h>
#include <sys/mman.h>

#include "elf_img.h"

#define SDK_K 19
#define SDK_L 21
#define SDK_M 23
#define SDK_O 26
#define SDK_Q 29
#define SDK_R 30

#ifdef __LP64__
#define LIBART_PATH_R "/apex/com.android.art/lib64/libart.so"
#define LIBART_PATH_Q "/apex/com.android.runtime/lib64/libart.so"
#define LIBART_PATH "/system/lib64/libart.so"
#else
#define LIBART_PATH_R "/apex/com.android.art/lib/libart.so"
#define LIBART_PATH_Q "/apex/com.android.runtime/lib/libart.so"
#define LIBART_PATH "/system/lib/libart.so"
#endif

#define _uintval(p) reinterpret_cast<uintptr_t>(p)
#define _ptr(p) reinterpret_cast<void*>(p)
#define _align_up(x, n) (((x) + ((n)-1)) & ~((n)-1))
#define _align_down(x, n) ((x) & -(n))
#define _page_size 4096
#define _page_align(n) _align_up(static_cast<uintptr_t>(n), _page_size)
#define _ptr_align(x) \
    _ptr(_align_down(reinterpret_cast<uintptr_t>(x), _page_size))
#define _make_rwx(p, n)                                                 \
    ::mprotect(_ptr_align(p),                                           \
               _page_align(_uintval(p) + n) != _page_align(_uintval(p)) \
                   ? _page_align(n) + _page_size                        \
                   : _page_align(n),                                    \
               PROT_READ | PROT_WRITE | PROT_EXEC)

#define JNIFUNC(ret, name, ...)                      \
    extern "C" /*JNIEXPORT*/ ret JNICALL             \
        Java_io_github_eirv_trex_TrexAndroid_##name( \
            JNIEnv* env, jclass this_class, __VA_ARGS__)
#define JNIFUNCS(ret, name)                                      \
    extern "C" /*JNIEXPORT*/ ret JNICALL                         \
        Java_io_github_eirv_trex_TrexAndroid_##name(JNIEnv* env, \
                                                    jclass this_class)
#define JNIMETHOD(name, signature)                                           \
    {                                                                        \
#name, signature, (void*)Java_io_github_eirv_trex_TrexAndroid_##name \
    }

#define FUNC(ret, name, ...)                 \
    using Type##name = ret (*)(__VA_ARGS__); \
    static Type##name name = nullptr;
#define FUNC_REGH(name, handle, sym) \
    name = reinterpret_cast<Type##name>(dlsym(handle, sym));
#define FUNC_REGE(name, elf, sym) \
    name = reinterpret_cast<Type##name>(elf.GetSymbolAddress(sym));

#define HOOK(ret, name, ...)                           \
    static ret (*Origin##name)(__VA_ARGS__) = nullptr; \
    static ret Replace##name(__VA_ARGS__)
#define INLINE_HOOK(name, addr)                        \
    Hook(addr, reinterpret_cast<void*>(Replace##name), \
         reinterpret_cast<void**>(&Origin##name)) == RT_SUCCESS

#define ACC_FINAL 0x10
#define ACC_BRIDGE 0x40
#define ACC_SYNTHETIC 0x1000

#define DVM_CLASS_FIELD_SLOTS 4

int sdk = 0;
void* native_get_stack_trace_func = nullptr;
jclass trex_android_class = nullptr;
jmethodID get_proxy_stack_trace_mid = nullptr;

template <typename MirrorType>
class ArtObjPtr {
   public:
    inline MirrorType* Ptr() const {
        return reference_;
    }

   private:
    MirrorType* reference_;
};

class ArtJNIEnvExt : public JNIEnv {
   public:
    void* GetSelf() const {
        return self_;
    }

   private:
    ArtJNIEnvExt(void* self) : self_(self) {
    }

    void* const self_;
};

struct DvmClassObject;

struct DvmObject {
    DvmClassObject* clazz;
    uint32_t lock;
};

struct DvmClassObject : DvmObject {
    uint32_t instanceData[DVM_CLASS_FIELD_SLOTS];
    const char* descriptor;
    char* descriptorAlloc;
    uint32_t accessFlags;
    uint32_t serialNumber;
    void* pDvmDex;
    uint32_t status;
    DvmClassObject* verifyErrorClass;
    uint32_t initThreadId;
    size_t objectSize;
    DvmClassObject* elementClass;
    int arrayDim;
    uint32_t primitiveType;
    DvmClassObject* super;
    DvmObject* classLoader;
};

struct DvmDexProto {
    const void* dexFile;
};

struct DvmMethod {
    DvmClassObject* clazz;
    uint32_t accessFlags;
    uint16_t methodIndex;
    uint16_t registersSize;
    uint16_t outsSize;
    uint16_t insSize;
    const char* name;
    DvmDexProto prototype;
};

struct DvmJNIEnvExt {
    const struct JNINativeInterface* funcTable;
    const struct JNINativeInterface* baseFuncTable;
    uint32_t envThreadId;
    void* self;
};

#endif  // TREX_TREX_H
