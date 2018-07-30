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
package eu.mihosoft.vmftext.tests.notop;

import eu.mihosoft.vmftext.tests.notop.NotOpModel;
import eu.mihosoft.vmftext.tests.notop.parser.NotOpModelParser;
import eu.mihosoft.vmftext.tests.notop.unparser.NotOpModelUnparser;
import org.junit.Assert;
import org.junit.Test;

public class NotOpTest {
    @Test
    public void notOpTest() {

        NotOpModelParser parser = new NotOpModelParser();
        NotOpModelUnparser unparser = new NotOpModelUnparser();

        // rule 1 alt 0
        NotOpModel model1 = parser.parse("def altOne");
        String s1 = unparser.unparse(model1).trim();
        System.out.println("CASE1: " + s1);

        Assert.assertEquals("def altOne r1", s1);

        // rule 1 alt 1
        NotOpModel model2 = parser.parse("abc altTwo");
        String s2 = unparser.unparse(model2).trim();
        System.out.println("CASE2: " + s2);
        Assert.assertEquals("abc altTwo r1", s2);

        // rule 2 alt 0
        NotOpModel model3 = parser.parse("abc altOne");
        String s3 = unparser.unparse(model3).trim();
        System.out.println("CASE3: " + s3);
        Assert.assertEquals("abc altOne r2",s3);

        // rule 2 alt 1
        NotOpModel model4 = parser.parse("def altTwo");
        String s4 = unparser.unparse(model4).trim();
        System.out.println("CASE4: " + s4);
        Assert.assertEquals("def altTwo r2", s4);

        // rule 3 alt 0
        NotOpModel model5 = parser.parse("this altOneR3");
        String s5 = unparser.unparse(model5).trim();
        System.out.println("CASE5: " + s5);
        Assert.assertEquals("this altOneR3", s5);
    }
}
