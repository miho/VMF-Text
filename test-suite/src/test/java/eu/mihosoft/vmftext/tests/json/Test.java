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
package eu.mihosoft.vmftext.tests.json;


import eu.mihosoft.vmftext.tests.json.parser.JSONModelParser;
import eu.mihosoft.vmftext.tests.json.unparser.JSONModelUnparser;
import org.junit.Assert;

public class Test {

    @org.junit.Test
    public void testJSON() {
        JSONModelParser parser = new JSONModelParser();

        JSONModel model = parser.parse(
                    "{ \"version\" : 1.0 ,"
                        +  " \"data\" : {"
                        +  " \"sampleArray\" : [ \"mystring\" , true , false , { \"name\" : \"my name\" } ]"
                        +  " }"
                        + "} ");

        // output unchanged json document
        JSONModelUnparser unparser = new JSONModelUnparser();
        System.out.println(unparser.unparse(model));

        // iterate over all pairs and print them and their type
        System.out.println("Pairs:");
        model.vmf().content().stream(Pair.class).
                forEach(p-> System.out.println(" -> key: " + p.getKey() +
                        ", type: " + p.getValue().getClass().getSimpleName()));

        // change version number
        // we check both, name and type
        model.vmf().content().stream(Pair.class).
                filter(p->"version".equals(p.getKey())).map(p->p.getValue()).
                filter(v->v instanceof NumberValue).map(v->(NumberValue)v).forEach(v->

                // Automatic conversion! We use double here. No need to convert to String.
                v.setValue(2.0)
        );

        // invert all boolean values
        model.vmf().content().stream(BooleanValue.class).
                forEach(v->v.setValue(!v.getValue()));

        // insert pair
        Pair myPair = Pair.newInstance();
        myPair.setKey("my number");

        // number value
        NumberValue value = NumberValue.newInstance();

        // no conversion necessary! we can use double directly.
        value.setValue(1.2345);

        // add value to pair
        myPair.setValue(value);

        ((ObjectValue)model.getRoot().getValue()).getValue().getPairs().add(myPair);

        System.out.println("--- Unparse modified model ---");

        // output changed json document
        String changedJson = unparser.unparse(model);

        String expectedJson = "{ \"version\" : 2.0 , \"data\" : { \"sampleArray\" : [ \"mystring\" , false , true , { \"name\" : \"my name\" } ] }, \"my number\" : 1.2345 }";


        Assert.assertEquals(expectedJson, changedJson.trim());

    }

}
