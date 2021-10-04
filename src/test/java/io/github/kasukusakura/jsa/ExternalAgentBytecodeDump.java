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

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

import java.util.Base64;

public class ExternalAgentBytecodeDump {
    public static void main(String[] args) throws Throwable {
        try (var code = ExternalAgentBytecodeDump.class.getResourceAsStream("ExternalAgent.class")) {
            var cn = new ClassNode();
            new ClassReader(code).accept(cn, 0);

            cn.version = Opcodes.V1_8;
            cn.module = null;
            cn.innerClasses.clear();
            cn.nestHostClass = null;
            cn.nestMembers = null;
            cn.permittedSubclasses = null;
            cn.outerClass = null;
            cn.outerMethod = null;
            cn.outerMethodDesc = null;


            byte[] bc;
            {
                var cw = new ClassWriter(0);
                cn.accept(cw);
                bc = cw.toByteArray();
            }

            var str = Base64.getEncoder().encodeToString(bc);
            System.out.println(
                    "\"" + str.replace("\n", "") + "\""
            );
        }
    }
}
