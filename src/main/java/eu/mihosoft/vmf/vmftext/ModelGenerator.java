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


import eu.mihosoft.vmf.core.TypeUtil;
import eu.mihosoft.vmf.core.io.FileResourceSet;
import eu.mihosoft.vmf.core.io.Resource;
import eu.mihosoft.vmf.core.io.ResourceSet;
import eu.mihosoft.vmf.vmftext.grammar.*;
import eu.mihosoft.vmf.vmftext.grammar.unparser.UnparserCodeGenerator;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

import java.io.File;
import java.io.IOException;
import java.io.Writer;

public class ModelGenerator {

    public ModelGenerator() {
        //
    }

    private VelocityEngine engine;
    private static final String TEMPLATE_PATH="/eu/mihosoft/vmf/vmftext/vmtemplates/";

    /**
     * Creates a velocity engine with all necessary defaults required by this code generator.
     * @return new velocity engine
     */
    private static VelocityEngine createDefaultEngine() {
        VelocityEngine engine = new VelocityEngine();

        engine.setProperty(RuntimeConstants.RESOURCE_LOADER, "vmflang");
        engine.setProperty("vmflang.resource.loader.instance", new VMFResourceLoader());

        return engine;
    }

    private static void generateModelDefinition(
            Writer out, VelocityEngine engine, String packageName, GrammarModel model) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);
        context.put("packageName", packageName);
        context.put("Util", StringUtil.class);

        mergeTemplate("model-parser", engine, context, out);
    }

    private static void generateModelParser(
            Writer out, VelocityEngine engine, String modelPackageName, String packageName, GrammarModel model) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);
        context.put("modelPackageName", modelPackageName);
        context.put("packageName", packageName);
        context.put("Util", StringUtil.class);

        mergeTemplate("model-converter", engine, context, out);
    }



    /**
     * Generates template code for the specified template.
     * @param templateName template to use for code generation
     * @param engine engine used for rendering
     * @param ctx velocity context (contains model instance etc.)
     * @param out writer to use for code generation
     * @throws IOException if the code generation fails due to I/O related problems
     */
    private static void mergeTemplate(
            String templateName, VelocityEngine engine, VelocityContext ctx, Writer out) throws IOException {
        String path = resolveTemplatePath(templateName);
        engine.mergeTemplate(path, "UTF-8", ctx, out);
    }

    private static String resolveTemplatePath(String path) {

        return TEMPLATE_PATH+path+".vm";
    }

    public void generateModel(GrammarModel model, ResourceSet fileset) {

        if(engine==null) {
            engine = createDefaultEngine();
        }

        try (Resource resource =
                     fileset.open(TypeUtil.computeFileNameFromJavaFQN(
                             model.getPackageName()+".vmfmodel."+model.getGrammarName() + "Model"));

             Writer w = resource.open()) {

            generateModelDefinition(w, engine, model.getPackageName()+".vmfmodel", model);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //
    }

    public void generateModelParser(GrammarModel model, ResourceSet fileset) {

        if(engine==null) {
            engine = createDefaultEngine();
        }

        try (Resource resource =
                     fileset.open(TypeUtil.computeFileNameFromJavaFQN(
                             model.getPackageName()+".parser."+model.getGrammarName() + "ModelParser"));

             Writer w = resource.open()) {

            generateModelParser(w, engine, model.getPackageName(),
                    model.getPackageName()+".parser", model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void generateModelUnparser(GrammarModel model, UnparserModel unparserModel, File grammarFile, ResourceSet fileset) {

        if (!(fileset instanceof FileResourceSet)) {
            throw new UnsupportedOperationException("FIXME: implement support for other resource sets " +
                    "(currently only file-resource-sets are supported)");
        }

        FileResourceSet resourceSet = (FileResourceSet) fileset;

        String unparserGrammarFile = (model.getPackageName()+".unparser.antlr4."+model.getGrammarName()).
                replace('.','/') + "ModelUnparserGrammar.g4";

        UnparserCodeGenerator.generateUnparser(model, unparserModel.asReadOnly(), unparserGrammarFile, fileset);

        VMFText.AntlrTool.setOutput(fileset);

        // we assume that each grammar and its dependencies are located in a single directory
        String libFolder=grammarFile.getAbsoluteFile().getParent();

        VMFText.AntlrTool.main(
                new String[]{
                        ((FileResourceSet) fileset).getRootSrcFolder()+"/"+unparserGrammarFile,
                        grammarFile.getAbsolutePath(),
                        "-listener",
                        "-package", model.getPackageName()+".unparser.antlr4",
                        "-lib", libFolder==null?"":libFolder,
                        "-o", ""
                }
        );

        try (Resource resource =
                     fileset.open(TypeUtil.computeFileNameFromJavaFQN(
                             model.getPackageName()+".unparser.Formatter"));

             Writer w = resource.open()) {

            generateUnparserFormatter(w, engine,model.getPackageName(), model);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Resource resource =
                     fileset.open(TypeUtil.computeFileNameFromJavaFQN(
                             model.getPackageName()+".unparser.BaseFormatter"));

             Writer w = resource.open()) {

            generateUnparserBaseFormatter(w, engine,model.getPackageName(), model);
        } catch (IOException e) {
            e.printStackTrace();
        }

        try (Resource resource =
                     fileset.open(TypeUtil.computeFileNameFromJavaFQN(
                             model.getPackageName()+".unparser.TypeToStringConverter"));

             Writer w = resource.open()) {

            generateTypeToStringConverter(w, engine,model.getPackageName(), model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateUnparserFormatter(
            Writer out, VelocityEngine engine, String modelPackageName, GrammarModel model) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);
        context.put("modelPackageName", modelPackageName);
        context.put("Util", StringUtil.class);

        mergeTemplate("unparser-formatter", engine, context, out);
    }

    private static void generateUnparserBaseFormatter(
            Writer out, VelocityEngine engine, String modelPackageName, GrammarModel model) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);
        context.put("modelPackageName", modelPackageName);
        context.put("Util", StringUtil.class);

        mergeTemplate("unparser-base-formatter", engine, context, out);
    }

    private static void generateTypeToStringConverter(
            Writer out, VelocityEngine engine, String packageName, GrammarModel model) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);
        context.put("packageName", packageName);
        context.put("Util", StringUtil.class);

        mergeTemplate("type-to-string-converter", engine, context, out);
    }
}
