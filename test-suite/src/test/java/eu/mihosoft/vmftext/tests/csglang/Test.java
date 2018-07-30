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
package eu.mihosoft.vmftext.tests.csglang;

import eu.mihosoft.vmftext.tests.csglang.parser.CSGLangModelParser;
import eu.mihosoft.vmftext.tests.csglang.unparser.CSGLangModelUnparser;
import eu.mihosoft.vmftext.tests.csglang.unparser.PointUnparser;
import org.junit.Assert;

public class Test {
    @org.junit.Test
    public void csgLangUnparsePointTest() {

        CSGLangModelUnparser unparser = new CSGLangModelUnparser();

        Point p1 = Point.newBuilder().withAnchor("p0").withName("p1").withX(2.3).withY(53.2).build();

        String p1String = unparser.unparse(p1).trim();
        System.out.println(p1String);
        Assert.assertEquals("point p1 ( anchor: p0 , x: 2.30 , y: 53.20 )", p1String);

        Point p2 = Point.newBuilder().withName("p2").withX(47.12).withY(9.84).build();

        String p2String = unparser.unparse(p2).trim();
        System.out.println(p2String);
        Assert.assertEquals("point p2 ( x: 47.12 , y: 9.84 )", p2String);

        Point p3 = Point.newBuilder().withName("p3").withX(-8.0).withY(-100452.3).build();

        String p3String = unparser.unparse(p3).trim();
        System.out.println(p3String);
        Assert.assertEquals("point p3 ( x: -8.00 , y: -100452.30 )", p3String);

    }

    @org.junit.Test
    public void csgLangParsePointTest() {

        CSGLangModelParser parser = new CSGLangModelParser();

        Point p1 = Point.newBuilder().withAnchor("p0").withName("p1").withX(2.3).withY(53.2).build();
        Point p1Parsed = parser.parse("point p1 ( anchor: p0 , x: 2.30 , y: 53.20 )").vmf().content().
                stream(Point.class).findFirst().get();

        Assert.assertEquals(p1,p1Parsed);

        Point p2 = Point.newBuilder().withName("p2").withX(47.12).withY(9.84).build();
        Point p2Parsed = parser.parse("point p2 ( x: 47.12 , y: 9.84 )").vmf().content().
                stream(Point.class).findFirst().get();

        Assert.assertEquals(p2,p2Parsed);

        Point p3 = Point.newBuilder().withName("p3").withX(-8.0).withY(-100452.3).build();
        Point p3Parsed = parser.parse("point p3 ( x: -8.00 , y: -100452.30 )").vmf().content().
                stream(Point.class).findFirst().get();

        Assert.assertEquals(p3,p3Parsed);

    }
}
