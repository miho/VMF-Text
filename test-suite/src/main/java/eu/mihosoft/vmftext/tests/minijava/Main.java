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
package eu.mihosoft.vmftext.tests.minijava;

import eu.mihosoft.vmftext.tests.minijava.parser.MiniJavaModelParser;
import eu.mihosoft.vmftext.tests.minijava.unparser.MiniJavaModelUnparser;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;

public class Main {
    public static void main(String[] args) throws IOException {
        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest1.java"));
        MiniJavaModel model2 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest2.java"));

        MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();
        //unparser.setFormatter(new MyFormatter());

        StringWriter w = new StringWriter();

        for (int i = 0; i < 10000; i++) {
            // unparse the current model
            //String s1 = unparser.unparse(model1);

            unparser.unparse(model1,w);

            // parse the model from the previously unparsed model
            //MiniJavaModel modelup1 = parser.parse(s1);

            //Assert.assertEquals(model1, modelup1);

            // unparse the current model
            //String s2 = unparser.unparse(model2);

            unparser.unparse(model2,w);

            // parse the model from the previously unparsed model
            //MiniJavaModel modelup2 = parser.parse(s2);

            //Assert.assertEquals(model2, modelup2);
        }

        w.close();
    }
}
