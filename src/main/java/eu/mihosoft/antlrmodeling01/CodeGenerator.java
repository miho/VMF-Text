package eu.mihosoft.antlrmodeling01;

import eu.mihosoft.vmf.commons.io.Resource;
import eu.mihosoft.vmf.lang.grammar.GrammarModel;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;

import java.io.InputStream;


/**
 * Created by miho on 30.08.17.
 */
public final class CodeGenerator {

    /**
     *
     * @param model
     * @param dest
     */
    public static void generate(GrammarModel model, Resource dest, String packageName) {

        VelocityEngine engine = createDefaultEngine();

        engine.setProperty("model", model);
        engine.setProperty("packageName", packageName);
    }

    /**
     * Creates a velocity engine with all necessary defaults required by this code generator.
     * @return new velocity engine
     */
    static VelocityEngine createDefaultEngine()  {
        VelocityEngine engine = new VelocityEngine();

        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "vmflang");
        engine.setProperty("vmflang.resource.loader.instance", new VMFResourceLoader());

        return engine;
    }



    static class VMFResourceLoader extends ClasspathResourceLoader {

        /**
         * Get an InputStream so that the Runtime can build a template with it.
         *
         * @param name name of template to get
         * @return InputStream containing the template
         * @throws ResourceNotFoundException if template not found in classpath.
         */
        public InputStream getResourceStream(String name)
                throws ResourceNotFoundException {
            InputStream input = CodeGenerator.class.getResourceAsStream(name);

            return input;
        }

    }
}
