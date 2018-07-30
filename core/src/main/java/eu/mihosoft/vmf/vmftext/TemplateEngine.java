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
package eu.mihosoft.vmf.vmftext;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.IOException;
import java.io.Writer;

public class TemplateEngine {

    private Engine engine;
    private static final String TEMPLATE_PATH="/eu/mihosoft/vmf/vmftext/vmtemplates/";

    /**
     * Creates a velocity engine with all necessary defaults required by this code generator.
     * @return new velocity engine
     */
    private static Engine createDefaultEngine() {
        VelocityEngine engine = new VelocityEngine();

        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "vmflang");
        engine.setProperty("vmflang.resource.loader.instance", new VMFResourceLoader());
        VelocityContext context = new VelocityContext();
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);

        return new Engine(context, engine);
    }

    public Engine getEngine() {

        if(engine==null) {
            engine = createDefaultEngine();
        }

        return engine;
    }

    public static class Engine {
        public final VelocityContext context;
        public final VelocityEngine engine;

        private Engine(VelocityContext context, VelocityEngine engine) {
            this.context = context;
            this.engine = engine;
        }
    }

    /**
     * Generates template code for the specified template.
     * @param templateName template to use for code generation
     * @param out writer to use for code generation
     * @throws IOException if the code generation fails due to I/O related problems
     */
    public void mergeTemplate(
            String templateName, Writer out) throws IOException {
        String path = resolveTemplatePath(templateName);
        engine.engine.mergeTemplate(path, "UTF-8", engine.context, out);
    }

    private static String resolveTemplatePath(String path) {

        return TEMPLATE_PATH+path+".vm";
    }
}
