package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.core.TypeUtil;
import eu.mihosoft.vmf.vmftext.grammar.*;
import eu.mihosoft.vmf.vmftext.grammar.custommodeldef.CustomModelDefinitionsBaseListener;
import eu.mihosoft.vmf.vmftext.grammar.custommodeldef.CustomModelDefinitionsLexer;
import eu.mihosoft.vmf.vmftext.grammar.custommodeldef.CustomModelDefinitionsListener;
import eu.mihosoft.vmf.vmftext.grammar.custommodeldef.CustomModelDefinitionsParser;
import eu.mihosoft.vmf.vmftext.grammar.typemapping.TypeMappingBaseListener;
import eu.mihosoft.vmf.vmftext.grammar.typemapping.TypeMappingLexer;
import eu.mihosoft.vmf.vmftext.grammar.typemapping.TypeMappingParser;
import eu.mihosoft.vmf.vmftext.grammar.vmftextcomments.VMFTextCommentsBaseListener;
import eu.mihosoft.vmf.vmftext.grammar.vmftextcomments.VMFTextCommentsLexer;
import eu.mihosoft.vmf.vmftext.grammar.vmftextcomments.VMFTextCommentsParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import javax.swing.text.html.Option;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public final class GrammarMetaInformationUtil {
    private GrammarMetaInformationUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static TypeMappings getTypeMapping(TypeMappings mappings,String code) {


        TypeMappingBaseListener l = new TypeMappingBaseListener() {

            TypeMapping currentMapping;

            @Override
            public void enterTypeMapping(TypeMappingParser.TypeMappingContext ctx) {

                TypeMapping typeMapping = TypeMapping.newInstance();

                typeMapping.getApplyToNames().addAll(ctx.applyTo.stream().
                        map(t->t.getText()).collect(Collectors.toList()));

                currentMapping = typeMapping;

                mappings.getTypeMappings().add(typeMapping);

                super.enterTypeMapping(ctx);
            }

            @Override
            public void enterMapping(TypeMappingParser.MappingContext ctx) {

                Mapping m = Mapping.newBuilder().withRuleName(ctx.ruleName.getText()).
                        withTypeName(ctx.typeName.getText()).withMappingCode(ctx.embeddedCode.getText().
                        substring(1, ctx.embeddedCode.getText().length()-1)).build();

                currentMapping.getEntries().add(m);

                super.enterMapping(ctx);
            }
        };

        CharStream input = CharStreams.fromString(code);

        TypeMappingLexer lexer = new TypeMappingLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeMappingParser parser = new TypeMappingParser(tokens);

        ParserRuleContext tree = parser.typeMappingCode();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

        return mappings;
    }

    public static List<String> extractVMFTextCommentsFromCode(String code) throws IOException {

        List<String> result = new ArrayList<>();

        VMFTextCommentsBaseListener l = new VMFTextCommentsBaseListener() {

            @Override
            public void enterVmfTextComment(VMFTextCommentsParser.VmfTextCommentContext ctx) {
                String comment = ctx.text.getText();
                result.add(comment.substring("/*<!vmf-text!>".length(), comment.length()-2));
                super.enterVmfTextComment(ctx);
            }
        };

        CharStream input = CharStreams.fromString(code);

        TypeMappingLexer lexer = new TypeMappingLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeMappingParser parser = new TypeMappingParser(tokens);

        ParserRuleContext tree = parser.typeMapping();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

        return result;

    }

    public static List<String> extractVMFTextCommentsFromCode(InputStream codeStream) throws IOException {

        List<String> result = new ArrayList<>();

        VMFTextCommentsBaseListener l = new VMFTextCommentsBaseListener() {

            @Override
            public void enterVmfTextComment(VMFTextCommentsParser.VmfTextCommentContext ctx) {
                String comment = ctx.text.getText();
                result.add(comment.substring("/*<!vmf-text!>".length(), comment.length()-2));
                super.enterVmfTextComment(ctx);
            }
        };

        CharStream input = CharStreams.fromStream(codeStream);

        VMFTextCommentsLexer lexer = new VMFTextCommentsLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        VMFTextCommentsParser parser = new VMFTextCommentsParser(tokens);

        ParserRuleContext tree = parser.program();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

        return result;

    }

    public static void getCustomAnnotations(String code, GrammarModel model) {

        CharStream input = CharStreams.fromString(code);

        CustomModelDefinitionsLexer lexer = new CustomModelDefinitionsLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        CustomModelDefinitionsParser parser = new CustomModelDefinitionsParser(tokens);

        CustomModelDefinitionsListener l = new CustomModelDefinitionsBaseListener() {

            private String currentRuleName;

            @Override
            public void exitParameterMethod(CustomModelDefinitionsParser.ParameterMethodContext ctx) {

                String propName = ctx.name.getText();


                if(!propName.startsWith("get")) {
                  // we are converting to delegation method
                    Optional<RuleClass> ruleClass = model.ruleClassByName(currentRuleName);

                    if(!ruleClass.isPresent()) {
                        throw new RuntimeException("RuleClass '"+currentRuleName+"' referenced in line ["
                                + ctx.name.getLine()+":"+ctx.name.getCharPositionInLine()+"] " +
                                "does not exist in the grammar.");
                    }

                    System.out.println("DelegationMethod: \n" + tokens.getText(ctx));

                    ruleClass.get().getDelegationMethods().add(
                            DelegationMethod.newBuilder().withText(tokens.getText(ctx)).build()
                    );

                } else {

                    // convert property name from getter to pure
                    propName = propName.substring(3, propName.length());
                    propName = StringUtil.firstToLower(propName);

                    Optional<Property> prop = model.propertyByName(currentRuleName, propName);

                    if (!prop.isPresent()) {
//                        throw new RuntimeException("Property '" + propName + "' referenced in line ["
//                                + ctx.name.getLine() + ":" + ctx.name.getCharPositionInLine() + "] " +
//                                "does not exist in the grammar.");

                        Optional<RuleClass> ruleClass = model.ruleClassByName(currentRuleName);

                        if(!ruleClass.isPresent()) {
                            throw new RuntimeException("RuleClass '"+currentRuleName+"' referenced in line ["
                                    + ctx.name.getLine()+":"+ctx.name.getCharPositionInLine()+"] " +
                                    "does not exist in the grammar.");
                        }

                        String packageName = TypeUtil.getPackageNameFromFullClassName(ctx.returnType.getText());
                        String typeName = TypeUtil.getShortNameFromFullClassName(ctx.returnType.getText());

                        Property newProp = Property.newBuilder().
                                withName(propName).
                                withType(Type.newBuilder().withPackageName(packageName).withName(typeName).build()).
                                build();

                        newProp.getAnnotations().addAll(
                                ctx.annotations.stream().
                                        map(aCtx-> PropertyAnnotation.newBuilder().withText(tokens.getText(aCtx)).build()).
                                        collect(Collectors.toList()));

                        ruleClass.get().getProperties().add(newProp);

                    } else {


                        // convert annotations
                        prop.get().getAnnotations().addAll(
                                ctx.annotations.stream().map(aCtx -> PropertyAnnotation.newBuilder().
                                        withText(tokens.getText(aCtx)).build()).collect(Collectors.toList())
                        );
                    }
                }

                super.exitParameterMethod(ctx);
            }

            @Override
            public void exitDelegationMethod(CustomModelDefinitionsParser.DelegationMethodContext ctx) {

                Optional<RuleClass> ruleClass = model.ruleClassByName(currentRuleName);

                if(!ruleClass.isPresent()) {
                    throw new RuntimeException("RuleClass '"+currentRuleName+"' referenced in line ["
                            + ctx.name.getLine()+":"+ctx.name.getCharPositionInLine()+"] " +
                            "does not exist in the grammar.");
                }

                System.out.println("DelegationMethod: \n" + tokens.getText(ctx));

                ruleClass.get().getDelegationMethods().add(
                        DelegationMethod.newBuilder().withText(tokens.getText(ctx)).build()
                );

                super.exitDelegationMethod(ctx);
            }

            @Override
            public void enterModelDefinition(CustomModelDefinitionsParser.ModelDefinitionContext ctx) {
                super.enterModelDefinition(ctx);

                currentRuleName = ctx.ruleName.getText();

                System.out.println("entering " + ctx.ruleName.getText());
            }
        };



        ParserRuleContext tree = parser.modelDefinitionCode();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

    }
}
