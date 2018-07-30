/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
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
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmftext.tests.miniclang;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public final class StringPrintStream extends PrintStream{
    private ByteArrayOutputStream baos;
    private PrintStream ps;


    private StringPrintStream(ByteArrayOutputStream outputStream) {
        super(outputStream);
        baos = outputStream;
        ps = this;
    }

    public static StringPrintStream newInstance() {
        ByteArrayOutputStream bs = new ByteArrayOutputStream();
        StringPrintStream ps = new StringPrintStream(bs);

        return ps;
    }

    public String toString() {
        ps.flush();

        String output = new String(baos.toByteArray(),
                StandardCharsets.UTF_8);

        return output;
    }
}
