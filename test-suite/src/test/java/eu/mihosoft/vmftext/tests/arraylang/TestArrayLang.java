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
package eu.mihosoft.vmftext.tests.arraylang;

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vmftext.tests.arraylang.parser.ArrayLangModelParser;
import eu.mihosoft.vmftext.tests.arraylang.unparser.ArrayLangModelUnparser;
import org.junit.Assert;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import org.junit.Test;


public class TestArrayLang {
    @Test
    public void parseModelTest() {
        ArrayLangModelParser parser = new ArrayLangModelParser();
        ArrayLangModel model1 = parser.parse("(1.0)");
        Array array1 = model1.getRoot();
        Assert.assertEquals(array1.getValues().size(),1);
        Assert.assertEquals(array1.getValues().get(0),1.0,1e-12);

        ArrayLangModel model2 = parser.parse("(1.0, 1.2, 2.3)");
        Array array2 = model2.getRoot();
        Assert.assertEquals(3, array2.getValues().size());
        Assert.assertEquals(VList.newInstance(Arrays.asList(1.0,1.2,2.3)), array2.getValues());
    }

    @Test
    public void unparseModelTest() {

        ArrayLangModel model1 = ArrayLangModel.newInstance();
        model1.setRoot(Array.newBuilder().withValues(VList.newInstance(Arrays.asList(1.0))).build());
        ArrayLangModelUnparser unparser1 = new ArrayLangModelUnparser();
        String s1 = unparser1.unparse(model1).trim(); // for the default formatter we are ok with leading & trailing spaces
        Assert.assertEquals("( 1.0 )", s1);

        ArrayLangModel model2 = ArrayLangModel.newInstance();
        model2.setRoot(Array.newBuilder().withValues(VList.newInstance(Arrays.asList(1.0, 2.1, 100.7))).build());
        ArrayLangModelUnparser unparser2 = new ArrayLangModelUnparser();
        String s2 = unparser2.unparse(model2).trim(); // for the default formatter we are ok with leading & trailing spaces
        Assert.assertEquals("( 1.0 , 2.1 , 100.7 )", s2);

    }

    @Test
    public void parseUnparseModelTest() {

        ArrayLangModelParser parser1 = new ArrayLangModelParser();
        ArrayLangModel model1 = parser1.parse("(1.0)");
        ArrayLangModelUnparser unparser1 = new ArrayLangModelUnparser();
        String s1 = unparser1.unparse(model1);
        ArrayLangModel model1up = parser1.parse(s1);

        Assert.assertEquals(model1, model1up);

        ArrayLangModelParser parser2 = new ArrayLangModelParser();
        ArrayLangModel model2 = parser2.parse("(1.1,2.3,100.7)");
        ArrayLangModelUnparser unparser2 = new ArrayLangModelUnparser();
        String s2 = unparser2.unparse(model2);
        ArrayLangModel model2up = parser2.parse(s2);

        Assert.assertEquals(model2, model2up);
    }
}
