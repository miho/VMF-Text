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

import eu.mihosoft.vmf.core.io.Resource;
import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import eu.mihosoft.ext.velocity.legacy.app.VelocityEngine;
import eu.mihosoft.ext.velocity.legacy.exception.ResourceNotFoundException;
import eu.mihosoft.ext.velocity.legacy.runtime.RuntimeConstants;
import eu.mihosoft.ext.velocity.legacy.runtime.RuntimeServices;
import eu.mihosoft.ext.velocity.legacy.runtime.log.LogChute;
import eu.mihosoft.ext.velocity.legacy.runtime.resource.loader.ClasspathResourceLoader;

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
        VMFTextDefaultLogger.registerVelocityDefaultLogger(engine);

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

/**
 * Dummy logger to prevent velocity from crashing if it is used in combination with other
 * log4j configurations. We should consider updating from this old velocity release to 
 * either a more modern version of velocity or switching to stringtemplates4.
 */
class VMFTextDefaultLogger implements LogChute
{

    public static void registerVelocityDefaultLogger(VelocityEngine engine) {
        VMFTextDefaultLogger logger = new VMFTextDefaultLogger();
        engine.setProperty(RuntimeConstants.RUNTIME_LOG_LOGSYSTEM, logger );
    }

    private VMFTextDefaultLogger() {

    }

    /**
     *  This init() will be invoked once by the LogManager
     *  to give you the current RuntimeServices intance
     */
    public void init(RuntimeServices rsvc)
    {
        // do nothing
    }

    /**
     *  This is the method that you implement for Velocity to
     *  call with log messages.
     */
    public void log(int level, String message)
    {
        // Logger.getLogger(this.getClass().getName()).
        //             log(Level.SEVERE, null, message);
    }

    /**
     *  This is the method that you implement for Velocity to
     *  call with log messages.
     */
    public void log(int level, String message, Throwable t)
    {
        // Logger.getLogger(this.getClass().getName()).
        // log(Level.SEVERE, message, t);
    }

    /**
     *  This is the method that you implement for Velocity to
     *  check whether a specified log level is enabled.
     */
    public boolean isLevelEnabled(int level)
    {
        return false;
    }

}
