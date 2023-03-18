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

package io.github.eirv.trex;

/**
 * @author Eirv
 * @date 2023/2/7 12:52
 */
class ArtMethodM {
    public final Class<?> declaringClass;
    public final long artMethod;
    private final int hash;

    public ArtMethodM(Class<?> declaringClass, long artMethod) {
        this.declaringClass = declaringClass;
        this.artMethod = artMethod;
        hash = (int) (artMethod ^ (artMethod >>> 32));
    }

    public int getAccessFlags() {
        return TrexAndroidImpl.getInt(artMethod + TrexAndroidImpl.OFF_ACCESS_FLAGS);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof ArtMethodM)) return false;

        ArtMethodM peer = (ArtMethodM) obj;
        return declaringClass == peer.declaringClass && artMethod == peer.artMethod;
    }
}
