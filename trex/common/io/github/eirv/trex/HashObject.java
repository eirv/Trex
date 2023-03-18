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
 * @date 2023/2/7 12:22
 */
class HashObject {
    public final Object obj;
    private final int hash;

    public HashObject(Object obj) {
        this.obj = obj;
        hash = obj.hashCode();
    }

    public static boolean equals(Object a, Object b) {
        return a.hashCode() == b.hashCode() && a.equals(b);
    }

    @Override
    public int hashCode() {
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        assert obj instanceof HashObject;
        HashObject peer = (HashObject) obj;
        return this.obj.equals(peer.obj);
    }
}
