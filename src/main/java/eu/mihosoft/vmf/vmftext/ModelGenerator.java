package eu.mihosoft.vmf.vmftext;


import eu.mihosoft.vmf.core.TypeUtil;
import eu.mihosoft.vmf.core.io.Resource;
import eu.mihosoft.vmf.core.io.ResourceSet;
import eu.mihosoft.vmf.vmftext.grammar.GrammarModel;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;

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

    private static void generateModelConverter(
            Writer out, VelocityEngine engine, String packageName, GrammarModel model) throws IOException {
        VelocityContext context = new VelocityContext();
        context.put("model", model);
        context.put("TEMPLATE_PATH",TEMPLATE_PATH);
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
                             model.getPackageName()+".vmfmodel."+model.getGrammarName() + "Model"))) {

            Writer w = resource.open();
            generateModelDefinition(w, engine, model.getPackageName()+".vmfmodel", model);
        } catch (IOException e) {
            e.printStackTrace();
        }


            //
    }

    public void generateModelConverter(GrammarModel model, ResourceSet fileset) {

        if(engine==null) {
            engine = createDefaultEngine();
        }

        try (Resource resource =
                     fileset.open(TypeUtil.computeFileNameFromJavaFQN(
                             model.getPackageName()+".parser."+model.getGrammarName() + "Converter"))) {

            Writer w = resource.open();
            generateModelConverter(w, engine,model.getPackageName()+".parser", model);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
