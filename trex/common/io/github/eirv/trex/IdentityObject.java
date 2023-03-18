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
 * @date 2023/2/11 11:01
 */
class IdentityObject<T> {
    public T obj;
    private int hash = -1;

    public IdentityObject(T obj) {
        this.obj = obj;
    }

    @Override
    public int hashCode() {
        if (hash == -1) {
            hash = System.identityHashCode(obj);
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IdentityObject)) return false;
        IdentityObject<?> peer = (IdentityObject<?>) obj;
        return this.obj == peer.obj;
    }
}
