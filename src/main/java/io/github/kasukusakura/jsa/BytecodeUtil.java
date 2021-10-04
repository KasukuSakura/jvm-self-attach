/*
 * Copyright 2021 KasukuSakura
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package io.github.kasukusakura.jsa;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

class BytecodeUtil {
    static byte[] replace(byte[] source, byte[] replace, byte[] target) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(source.length);
        int sourceLength = source.length,
                replaceLength = replace.length,
                targetLength = target.length,
                replaceLengthR1 = replaceLength - 1;
        root:
        for (int i = 0; i < sourceLength; i++) {
            if (i + replaceLength <= sourceLength) {
                for (int z = 0; z < replaceLength; z++) {
                    if (replace[z] != source[i + z]) {
                        outputStream.write(source[i]);
                        continue root;
                    }
                }
                outputStream.write(target, 0, targetLength);
                i += replaceLengthR1;
            } else {
                outputStream.write(source[i]);
            }
        }
        return outputStream.toByteArray();
    }

    static byte[] replace(byte[] classfile, String const1, String const2) {
        return replace(classfile, toJvm(const1), toJvm(const2));
    }

    static byte[] toJvm(String const0) {
        byte[] bytes = const0.getBytes(StandardCharsets.UTF_8);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(bytes.length + 2);
        try {
            new DataOutputStream(bos).writeShort(bytes.length);
        } catch (IOException ioException) {
            throw new AssertionError(ioException);
        }
        bos.write(bytes, 0, bytes.length);
        return bos.toByteArray();
    }
}
