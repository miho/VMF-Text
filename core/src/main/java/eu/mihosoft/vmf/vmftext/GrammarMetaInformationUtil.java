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
import java.util.*;
import java.util.stream.Collectors;

public final class GrammarMetaInformationUtil {
    private GrammarMetaInformationUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static TypeMappings getTypeMapping(TypeMappings mappings, String code) {


        TypeMappingBaseListener l = new TypeMappingBaseListener() {

            private TypeMapping currentMapping;

            @Override
            public void enterTypeMapping(TypeMappingParser.TypeMappingContext ctx) {

                TypeMapping typeMapping = TypeMapping.newInstance();

                typeMapping.getApplyToNames().addAll(ctx.applyTo.stream().
                        map(t -> t.getText()).collect(Collectors.toList()));

                currentMapping = typeMapping;

                mappings.getTypeMappings().add(typeMapping);

                super.enterTypeMapping(ctx);
            }

            @Override
            public void enterMapping(TypeMappingParser.MappingContext ctx) {

                if(ctx.stringToTypeCode!=null && ctx.typeToStringCode!=null) {
                    Mapping m = Mapping.newBuilder().withRuleName(ctx.ruleName.getText()).
                            withTypeName(ctx.typeName.getText()).
                            withStringToTypeCode(ctx.stringToTypeCode.getText().
                                    substring(1, ctx.stringToTypeCode.getText().length() - 1)).
                            withTypeToStringCode(ctx.typeToStringCode.getText().
                                    substring(1, ctx.typeToStringCode.getText().length() - 1)).build();

                    if(ctx.defaultCode!=null) {
                        m.setDefaultValueCode(ctx.defaultCode.getText().
                                substring(1, ctx.defaultCode.getText().length() - 1));
                    }

                    currentMapping.getEntries().add(m);
                } else if(ctx.stringToTypeCode!=null) {
                    Mapping m = Mapping.newBuilder().withRuleName(ctx.ruleName.getText()).
                            withTypeName(ctx.typeName.getText()).
                            withStringToTypeCode(ctx.stringToTypeCode.getText().
                                    substring(1, ctx.stringToTypeCode.getText().length() - 1)).
                            withTypeToStringCode("entry.toString()").build();
                    currentMapping.getEntries().add(m);
                }

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
                result.add(comment.substring("/*<!vmf-text!>".length(), comment.length() - 2));
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
                result.add(comment.substring("/*<!vmf-text!>".length(), comment.length() - 2));
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
            private Set<String> definedCustomRules = new HashSet<>();

            @Override
            public void exitParameterMethod(CustomModelDefinitionsParser.ParameterMethodContext ctx) {

                if (currentRuleName == null) return;

                String propName = ctx.name.getText();

                // convert property name from getter to pure
                propName = propName.substring(3, propName.length());
                propName = StringUtil.firstToLower(propName);

                Optional<Property> prop = model.propertyByName(currentRuleName, propName);

                if (!prop.isPresent()) {
                    System.out.println(" -> adding custom parameter '" +
                            propName + "' to rule '" + currentRuleName + "'.");

                    Optional<RuleClass> ruleClass = model.ruleClassByName(currentRuleName);

                    String packageName = TypeUtil.getPackageNameFromFullClassName(ctx.returnType.getText());
                    String typeName = TypeUtil.getShortNameFromFullClassName(ctx.returnType.getText());

                    Property newProp = Property.newBuilder().
                            withName(propName).
                            withType(Type.newBuilder().withPackageName(packageName).withName(typeName).build()).
                            build();

                    newProp.getAnnotations().addAll(
                            ctx.annotations.stream().
                                    map(aCtx -> PropertyAnnotation.newBuilder().withText(tokens.getText(aCtx)).build()).
                                    collect(Collectors.toList()));

                    ruleClass.get().getCustomProperties().add(newProp);

                } else {
                    // convert annotations
                    prop.get().getAnnotations().addAll(
                            ctx.annotations.stream().map(aCtx -> PropertyAnnotation.newBuilder().
                                    withText(tokens.getText(aCtx)).build()).collect(Collectors.toList())
                    );
                }

                super.exitParameterMethod(ctx);
            }

            @Override
            public void exitDelegationMethod(CustomModelDefinitionsParser.DelegationMethodContext ctx) {

                if (currentRuleName == null) return;

                Optional<RuleClass> ruleClass = model.ruleClassByName(currentRuleName);

                if (!ruleClass.isPresent()) {
                    throw new RuntimeException("RuleClass '" + currentRuleName + "' referenced in line ["
                            + ctx.name.getLine() + ":" + ctx.name.getCharPositionInLine() + "] " +
                            "does not exist in the grammar.");
                }

                System.out.println(" -> adding delegation-method: " + tokens.getText(ctx.returnType) + " " +
                        tokens.getText(ctx.name, ctx.name) + "()");

                ruleClass.get().getDelegationMethods().add(
                        DelegationMethod.newBuilder().withText(tokens.getText(ctx)).build()
                );

                super.exitDelegationMethod(ctx);
            }

            @Override
            public void enterModelDefinition(CustomModelDefinitionsParser.ModelDefinitionContext ctx) {
                super.enterModelDefinition(ctx);

                currentRuleName = ctx.ruleName.getText();

                System.out.println("> Entering rule '" + ctx.ruleName.getText() + "'");

                if (!model.ruleClassByName(currentRuleName).isPresent()) {

                    if (!definedCustomRules.contains(currentRuleName)) {
                        System.out.println(" -> rule does not exist. Adding model class outside of the grammar spec.");
                        definedCustomRules.add(currentRuleName);
                        model.getCustomRules().add(CustomRule.newBuilder().withText(tokens.getText(ctx)).build());
                    }

                    currentRuleName = null;
                } else {
                    RuleClass ruleClass = model.ruleClassByName(currentRuleName).get();
                    if (ctx.identifierList() != null) {
                        ruleClass.getSuperInterfaces().addAll(
                                ctx.identifierList().elements.stream().map(token -> tokens.getText(token, token)).
                                        collect(Collectors.toList()));
                    }

                    if (ctx.annotations != null) {
                        System.out.println(" -> adding custom annotations");
                        ruleClass.getCustomRuleAnnotations().addAll(
                                ctx.annotations.stream().map(aCtx -> RuleAnnotation.newBuilder().
                                        withText(tokens.getText(aCtx)).build()).
                                        collect(Collectors.toList())
                        );
                    }

                }
            }
        };

        ParserRuleContext tree = parser.modelDefinitionCode();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

    }
}
