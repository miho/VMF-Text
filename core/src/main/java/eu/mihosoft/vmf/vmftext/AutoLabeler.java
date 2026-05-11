package eu.mihosoft.vmf.vmftext;

import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4ParserBaseListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Rewrites unlabeled parser/lexer references into deterministic ANTLR labels.
 * Explicit labels and string literals are left unchanged.
 */
final class AutoLabeler {

    private AutoLabeler() {
        throw new AssertionError("Don't instantiate me!");
    }

    static File rewrite(File grammar, boolean emitReport) throws IOException {
        try(FileInputStream codeStream = new FileInputStream(grammar)) {
            CharStream input = CharStreams.fromStream(codeStream);

            ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);

            ParserRuleContext tree = parser.grammarSpec();
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            AutoLabelListener listener = new AutoLabelListener(tokens, rewriter);

            ParseTreeWalker.DEFAULT.walk(listener, tree);

            if(emitReport && listener.hasEntries()) {
                System.out.println(listener.report());
            }

            Path dir = Files.createTempDirectory("vmf-text-autolabel");
            File grammarOut = new File(dir.toFile(), grammar.getName());
            Files.write(grammarOut.toPath(), rewriter.getText().getBytes(StandardCharsets.UTF_8));

            return grammarOut;
        }
    }

    private static final class AutoLabelListener extends ANTLRv4ParserBaseListener {
        private final CommonTokenStream tokens;
        private final TokenStreamRewriter rewriter;
        private final Map<String,Integer> nameCountsInRule = new HashMap<>();
        private final Map<String,Map<String,String>> entriesByRule = new LinkedHashMap<>();
        private String currentRuleName;
        private int currentRuleIndex = -1;
        private int currentAltIndex;
        private int currentElementIndex;

        AutoLabelListener(CommonTokenStream tokens, TokenStreamRewriter rewriter) {
            this.tokens = tokens;
            this.rewriter = rewriter;
        }

        @Override
        public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            currentRuleName = ctx.RULE_REF().getText();
            currentRuleIndex++;
            currentAltIndex = -1;
            currentElementIndex = 0;
            nameCountsInRule.clear();
        }

        @Override
        public void enterAlternative(ANTLRv4Parser.AlternativeContext ctx) {
            currentAltIndex++;
            currentElementIndex = 0;
        }

        @Override
        public void enterElement(ANTLRv4Parser.ElementContext ctx) {
            if(currentRuleName == null) {
                return;
            }

            int elementIndex = currentElementIndex++;

            if(ctx.labeledElement() != null || ctx.atom() == null || ctx.actionBlock() != null) {
                return;
            }

            boolean parserRuleReference = isParserRuleReference(ctx.atom());
            String referencedName = referencedRuleName(ctx.atom());
            if(referencedName == null || "EOF".equals(referencedName)) {
                return;
            }

            String baseName = toPropertyBaseName(referencedName);
            if(parserRuleReference && baseName.equals(referencedName)) {
                baseName = baseName + "Node";
            }
            if(isRepeated(ctx)) {
                baseName = pluralize(baseName);
            }

            String labelName = uniqueName(baseName);
            String assignment = isRepeated(ctx) ? "+=" : "=";
            rewriter.insertBefore(ctx.start, labelName + assignment);

            String path = "/r" + currentRuleIndex + "/a" + Math.max(currentAltIndex, 0) + "/e" + elementIndex;
            entriesByRule.computeIfAbsent(currentRuleName, key -> new LinkedHashMap<>()).put(path, labelName);
        }

        private String referencedRuleName(ANTLRv4Parser.AtomContext atom) {
            if(atom.ruleref() != null) {
                return atom.ruleref().getText();
            }

            if(atom.terminal() != null && atom.terminal().TOKEN_REF() != null) {
                return atom.terminal().TOKEN_REF().getText();
            }

            return null;
        }

        private boolean isParserRuleReference(ANTLRv4Parser.AtomContext atom) {
            return atom.ruleref() != null;
        }

        private boolean isRepeated(ANTLRv4Parser.ElementContext ctx) {
            if(ctx.ebnfSuffix() == null) {
                return false;
            }

            String text = tokens.getText(ctx.ebnfSuffix());
            return text.startsWith("*") || text.startsWith("+");
        }

        private String uniqueName(String baseName) {
            int count = nameCountsInRule.getOrDefault(baseName, 0) + 1;
            nameCountsInRule.put(baseName, count);

            if(count == 1) {
                return baseName;
            }

            return baseName + count;
        }

        private String toPropertyBaseName(String referencedName) {
            String result;
            if(referencedName.equals(referencedName.toUpperCase())) {
                StringBuilder builder = new StringBuilder();
                for(String part : referencedName.toLowerCase().split("_+")) {
                    if(part.isEmpty()) {
                        continue;
                    }
                    if(builder.length() == 0) {
                        builder.append(part);
                    } else {
                        builder.append(StringUtil.firstToUpper(part));
                    }
                }
                result = builder.length() == 0 ? StringUtil.firstToLower(referencedName) : builder.toString();
            } else {
                result = StringUtil.firstToLower(referencedName);
            }

            if(isJavaKeyword(result)) {
                result = result + "Value";
            }
            return result;
        }

        private boolean isJavaKeyword(String value) {
            return "abstract assert boolean break byte case catch char class const continue default do double else enum "
                    .concat("extends final finally float for goto if implements import instanceof int interface long native ")
                    .concat("new package private protected public return short static strictfp super switch synchronized ")
                    .concat("this throw throws transient try void volatile while true false null")
                    .contains(" " + value + " ");
        }

        private String pluralize(String baseName) {
            if(baseName.endsWith("s")) {
                return baseName + "List";
            }
            return baseName + "s";
        }

        boolean hasEntries() {
            return !entriesByRule.isEmpty();
        }

        String report() {
            StringBuilder result = new StringBuilder("AutoLabel report:\n");
            entriesByRule.forEach((rule, entries) -> {
                result.append("  rule ").append(rule).append(":\n");
                entries.forEach((path, name) ->
                        result.append("    element ").append(path).append(" -> ").append(name).append('\n'));
            });
            return result.toString();
        }
    }
}
