package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.Mapping;
import eu.mihosoft.vmf.vmftext.grammar.TypeMapping;
import eu.mihosoft.vmf.vmftext.grammar.TypeMappings;
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

import java.io.IOException;
import java.io.InputStream;
import java.util.stream.Collectors;

public final class GrammarMetaInformationUtil {
    private GrammarMetaInformationUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static TypeMappings getTypeMapping(String code) {

        TypeMappings mappings = TypeMappings.newInstance();



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
                        withTypeName(ctx.typeName.getText()).build();

                currentMapping.getEntries().add(m);

                super.enterMapping(ctx);
            }
        };

        CharStream input = CharStreams.fromString(code);

        TypeMappingLexer lexer = new TypeMappingLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        TypeMappingParser parser = new TypeMappingParser(tokens);

        ParserRuleContext tree = parser.typeMapping();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

        return mappings;
    }

    public static String extractVMFTextCommentsFromCode(String code) throws IOException {

        StringBuilder sb = new StringBuilder();

        VMFTextCommentsBaseListener l = new VMFTextCommentsBaseListener() {

            @Override
            public void enterVmfTextComment(VMFTextCommentsParser.VmfTextCommentContext ctx) {
                String comment = ctx.text.getText();
                sb.append(comment.substring("/*<!vmf-text!>".length(), comment.length()-2));
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

        String result = sb.toString();

        return result;

    }

    public static String extractVMFTextCommentsFromCode(InputStream codeStream) throws IOException {

        StringBuilder sb = new StringBuilder();

        VMFTextCommentsBaseListener l = new VMFTextCommentsBaseListener() {

            @Override
            public void enterVmfTextComment(VMFTextCommentsParser.VmfTextCommentContext ctx) {
                String comment = ctx.text.getText();
                sb.append(comment.substring("/*<!vmf-text!>".length(), comment.length()-2));
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

        String result = sb.toString();

        return result;

    }
}
