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
