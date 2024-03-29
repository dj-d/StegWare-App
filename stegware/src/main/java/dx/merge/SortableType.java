/*
 * Copyright (C) 2011 The Android Open Source Project
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

package dx.merge;

import dx.io.ClassDef;
import dx.io.DexBuffer;
import java.util.Comparator;

/**
 * Name and structure of a type. Used to order types such that each type is
 * preceded by its supertype and implemented interfaces.
 */
final class SortableType {
    public static final Comparator<SortableType> NULLS_LAST_ORDER = new Comparator<SortableType>() {
        public int compare(SortableType a, SortableType b) {
            if (a == b) {
                return 0;
            }
            if (b == null) {
                return -1;
            }
            if (a == null) {
                return 1;
            }
            if (a.depth != b.depth) {
                return a.depth - b.depth;
            }
            return a.getTypeIndex() - b.getTypeIndex();
        }
    };

    private final DexBuffer buffer;
    private ClassDef classDef;
    private int depth = -1;

    public SortableType(DexBuffer buffer, ClassDef classDef) {
        this.buffer = buffer;
        this.classDef = classDef;
    }

    public DexBuffer getBuffer() {
        return buffer;
    }

    public ClassDef getClassDef() {
        return classDef;
    }

    public int getTypeIndex() {
        return classDef.getTypeIndex();
    }

    /**
     * Assigns this type's depth if the depths of its supertype and implemented
     * interfaces are known. Returns false if the depth couldn't be computed
     * yet.
     */
    public boolean tryAssignDepth(SortableType[] types) {
        int max;
        if (classDef.getSupertypeIndex() == ClassDef.NO_INDEX) {
            max = 0; // this is Object.class or an interface
        } else {
            SortableType sortableSupertype = types[classDef.getSupertypeIndex()];
            if (sortableSupertype == null) {
                max = 1; // unknown, so assume it's a root.
            } else if (sortableSupertype.depth == -1) {
                return false;
            } else {
                max = sortableSupertype.depth;
            }
        }

        for (short interfaceIndex : classDef.getInterfaces()) {
            SortableType implemented = types[interfaceIndex];
            if (implemented == null) {
                max = Math.max(max, 1); // unknown, so assume it's a root.
            } else if (implemented.depth == -1) {
                return false;
            } else {
                max = Math.max(max, implemented.depth);
            }
        }

        depth = max + 1;
        return true;
    }

    public boolean isDepthAssigned() {
        return depth != -1;
    }
}
