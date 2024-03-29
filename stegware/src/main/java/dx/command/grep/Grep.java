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

package dx.command.grep;

import dx.io.ClassData;
import dx.io.ClassDef;
import dx.io.CodeReader;
import dx.io.DexBuffer;
import dx.io.EncodedValueReader;
import dx.io.MethodId;
import dx.io.instructions.DecodedInstruction;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public final class Grep {
    private final DexBuffer dex;
    private final CodeReader codeReader = new CodeReader();
    private final Set<Integer> stringIds;

    private final PrintWriter out;
    private int count = 0;

    private ClassDef currentClass;
    private ClassData.Method currentMethod;

    public Grep(final DexBuffer dex, Pattern pattern, final PrintWriter out) {
        this.dex = dex;
        this.out = out;

        stringIds = getStringIds(dex, pattern);

        codeReader.setStringVisitor(new CodeReader.Visitor() {
            public void visit(DecodedInstruction[] all, DecodedInstruction one) {
                encounterString(one.getIndex());
            }
        });
    }

    private EncodedValueReader newEncodedValueReader(DexBuffer.Section section) {
        return new EncodedValueReader(section) {
            @Override protected void visitString(int type, int index) {
                encounterString(index);
            }
        };
    }

    private void encounterString(int index) {
        if (stringIds.contains(index)) {
            out.println(location() + " " + dex.strings().get(index));
            count++;
        }
    }

    private String location() {
        String className = dex.typeNames().get(currentClass.getTypeIndex());
        if (currentMethod != null) {
            MethodId methodId = dex.methodIds().get(currentMethod.getMethodIndex());
            return className + "." + dex.strings().get(methodId.getNameIndex());
        } else {
            return className;
        }
    }

    /**
     * Prints usages to out. Returns the number of matches found.
     */
    public int grep() {
        for (ClassDef classDef : dex.classDefs()) {
            currentClass = classDef;
            currentMethod = null;

            if (classDef.getClassDataOffset() == 0) {
                continue;
            }

            ClassData classData = dex.readClassData(classDef);

            // find the strings in encoded constants
            int staticValuesOffset = classDef.getStaticValuesOffset();
            if (staticValuesOffset != 0) {
                newEncodedValueReader(dex.open(staticValuesOffset)).readArray();
            }

            // find the strings in method bodies
            for (ClassData.Method method : classData.allMethods()) {
                currentMethod = method;
                if (method.getCodeOffset() != 0) {
                    codeReader.visitAll(dex.readCode(method).getInstructions());
                }
            }
        }

        currentClass = null;
        currentMethod = null;
        return count;
    }

    private Set<Integer> getStringIds(DexBuffer dex, Pattern pattern) {
        Set<Integer> stringIds = new HashSet<Integer>();
        int stringIndex = 0;
        for (String s : dex.strings()) {
            if (pattern.matcher(s).find()) {
                stringIds.add(stringIndex);
            }
            stringIndex++;
        }
        return stringIds;
    }
}
