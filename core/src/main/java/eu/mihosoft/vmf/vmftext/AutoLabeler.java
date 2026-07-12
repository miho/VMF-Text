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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Rewrites unlabeled parser/lexer references into deterministic ANTLR labels.
 *
 * <p>The rewriter targets grammars without explicit labels and derives a
 * predictable public API from grammar structure alone:</p>
 * <ul>
 *   <li>unlabeled parser-rule and token references become {@code name=}
 *       assignments;</li>
 *   <li>elements that can occur more than once (either via a {@code *}/{@code +}
 *       suffix or because they are nested inside a repeated block) become
 *       {@code name+=} list assignments;</li>
 *   <li>unnamed operator/separator literals inside a repeated block (e.g. the
 *       {@code ('+' | '-')} in {@code (('+' | '-') term)*} or the {@code ','} in
 *       {@code (',' item)*}) are captured as ordered list properties so the
 *       text round-trips instead of being dropped when unparsing;</li>
 *   <li>parser rules with two or more unlabeled top-level alternatives receive
 *       deterministic {@code # AltLabel} alternative labels so every
 *       alternative becomes its own typed sub class.</li>
 * </ul>
 *
 * <p>Manual labeling always wins and mixes consistently with auto-labeling:
 * explicitly labeled elements and alternatives are kept unchanged, generated
 * element names never collide with manually chosen ones, and a rule that only
 * labels <em>some</em> of its alternatives (which ANTLR would reject) has the
 * remaining alternatives labeled automatically so the grammar stays valid.
 * String literals outside of repeated blocks are left unchanged.</p>
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

            // collect the names that are already taken (parser rule names and
            // any explicit alternative labels) so generated alternative labels
            // never collide with existing model types.
            ReservedNameCollector reserved = new ReservedNameCollector();
            ParseTreeWalker.DEFAULT.walk(reserved, tree);

            AutoLabelListener listener = new AutoLabelListener(tokens, rewriter, reserved.reservedNamesLower());

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

    /**
     * Collects the names that generated alternative labels must not collide
     * with: all parser rule names and any explicit alternative labels.
     */
    private static final class ReservedNameCollector extends ANTLRv4ParserBaseListener {
        private final Set<String> reservedLower = new HashSet<>();

        @Override
        public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            if(ctx.RULE_REF() != null) {
                reservedLower.add(ctx.RULE_REF().getText().toLowerCase());
            }
        }

        @Override
        public void enterLabeledAlt(ANTLRv4Parser.LabeledAltContext ctx) {
            if(ctx.identifier() != null) {
                reservedLower.add(ctx.identifier().getText().toLowerCase());
            }
        }

        Set<String> reservedNamesLower() {
            return reservedLower;
        }
    }

    private static final class AutoLabelListener extends ANTLRv4ParserBaseListener {
        private static final String LITERAL_BLOCK_NAME = "operator";
        private static final String LITERAL_ATOM_NAME = "symbol";

        private final CommonTokenStream tokens;
        private final TokenStreamRewriter rewriter;
        private final Set<String> reservedNamesLower;
        private final Map<String,Integer> nameCountsInRule = new HashMap<>();
        private final Map<String,Map<String,String>> entriesByRule = new LinkedHashMap<>();
        // blocks that were labeled as a single unit: their interior must not be
        // labeled again (avoids double labeling of operator/separator literals).
        private final Set<ParserRuleContext> suppressedSubTrees = new HashSet<>();
        private String currentRuleName;
        private int currentRuleIndex = -1;
        private int currentAltIndex;
        private int currentElementIndex;

        AutoLabelListener(CommonTokenStream tokens, TokenStreamRewriter rewriter, Set<String> reservedNamesLower) {
            this.tokens = tokens;
            this.rewriter = rewriter;
            this.reservedNamesLower = reservedNamesLower;
        }

        @Override
        public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            currentRuleName = ctx.RULE_REF().getText();
            currentRuleIndex++;
            currentAltIndex = -1;
            currentElementIndex = 0;
            nameCountsInRule.clear();

            // reserve the names of manually labeled elements so auto-generated
            // element labels never collide with the manual ones.
            seedManualElementLabels(ctx);

            labelAlternatives(ctx);
        }

        /**
         * Adds deterministic {@code # AltLabel} labels to a rule's top-level
         * alternatives.
         *
         * <p>Manual alternative labels always win and are preserved. When a rule
         * mixes manually labeled and unlabeled alternatives (which ANTLR rejects
         * on its own, because alternative labeling is all-or-none), the missing
         * labels are filled in so the grammar becomes valid while keeping the
         * manual labels intact. For fully unlabeled rules the labels are only
         * added when there are at least two non-empty alternatives.</p>
         */
        private void labelAlternatives(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            if(ctx.ruleBlock() == null || ctx.ruleBlock().ruleAltList() == null) {
                return;
            }

            List<ANTLRv4Parser.LabeledAltContext> alts = ctx.ruleBlock().ruleAltList().labeledAlt();

            if(alts.size() < 2) {
                return;
            }

            int labeledCount = 0;
            boolean hasEmptyUnlabeled = false;
            for(ANTLRv4Parser.LabeledAltContext alt : alts) {
                if(alt.identifier() != null) {
                    labeledCount++;
                } else if(isEmptyAlternative(alt)) {
                    hasEmptyUnlabeled = true;
                }
            }

            if(labeledCount == alts.size()) {
                return; // fully manual labeling: nothing to complete
            }

            boolean completingManualLabels = labeledCount > 0;

            // For fully unlabeled rules we stay conservative and skip rules that
            // contain an empty alternative (labeling it would introduce an empty
            // sub class). When completing manual labels we must label every
            // alternative (including empty ones) to keep the grammar valid.
            if(!completingManualLabels && hasEmptyUnlabeled) {
                return;
            }

            for(int i = 0; i < alts.size(); i++) {
                ANTLRv4Parser.LabeledAltContext alt = alts.get(i);
                if(alt.identifier() != null) {
                    continue; // keep the manual label
                }

                String label = uniqueAltLabel(currentRuleName, i + 1);

                if(isEmptyAlternative(alt)) {
                    // an empty alternative has no tokens of its own; its context
                    // starts at the following separator, so inserting before it
                    // places the label right after the empty alternative.
                    rewriter.insertBefore(alt.start, "# " + label + " ");
                } else {
                    rewriter.insertAfter(alt.alternative().stop, " # " + label);
                }

                entriesByRule.computeIfAbsent(currentRuleName, key -> new LinkedHashMap<>())
                        .put("/r" + currentRuleIndex + "/alt" + (i + 1), "# " + label);
            }
        }

        private boolean isEmptyAlternative(ANTLRv4Parser.LabeledAltContext alt) {
            return alt.alternative() == null || alt.alternative().element().isEmpty();
        }

        private void seedManualElementLabels(ParserRuleContext ctx) {
            if(ctx instanceof ANTLRv4Parser.LabeledElementContext) {
                ANTLRv4Parser.LabeledElementContext le = (ANTLRv4Parser.LabeledElementContext) ctx;
                if(le.identifier() != null) {
                    nameCountsInRule.putIfAbsent(le.identifier().getText(), 1);
                }
            }
            for(int i = 0; i < ctx.getChildCount(); i++) {
                if(ctx.getChild(i) instanceof ParserRuleContext) {
                    seedManualElementLabels((ParserRuleContext) ctx.getChild(i));
                }
            }
        }

        private String uniqueAltLabel(String ruleName, int altNumber) {
            String base = StringUtil.firstToUpper(ruleName) + "Alt" + altNumber;
            String candidate = base;
            int suffix = 1;
            while(reservedNamesLower.contains(candidate.toLowerCase())) {
                suffix++;
                candidate = base + "_" + suffix;
            }
            reservedNamesLower.add(candidate.toLowerCase());
            return candidate;
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

            if(ctx.labeledElement() != null || ctx.actionBlock() != null || isInsideSuppressedSubTree(ctx)) {
                return;
            }

            if(ctx.atom() != null) {
                labelAtomElement(ctx, elementIndex);
            } else if(ctx.ebnf() != null && ctx.ebnf().block() != null) {
                labelLiteralBlockElement(ctx, elementIndex);
            }
        }

        private void labelAtomElement(ANTLRv4Parser.ElementContext ctx, int elementIndex) {
            boolean repeated = isRepeated(ctx);
            boolean parserRuleReference = isParserRuleReference(ctx.atom());
            String referencedName = referencedRuleName(ctx.atom());

            String baseName;
            if(referencedName != null) {
                if("EOF".equals(referencedName)) {
                    return;
                }
                baseName = toPropertyBaseName(referencedName);
                if(parserRuleReference && baseName.equals(referencedName)) {
                    baseName = baseName + "Node";
                }
            } else if(repeated && isStringLiteralAtom(ctx.atom())) {
                // unnamed string literals inside a repeated block (e.g. the ','
                // in '(',' item)*') would be dropped when unparsing. Capturing
                // them as an ordered list keeps the text round-trippable.
                baseName = LITERAL_ATOM_NAME;
            } else {
                return;
            }

            if(repeated) {
                baseName = pluralize(baseName);
            }

            String labelName = uniqueName(baseName);
            String assignment = repeated ? "+=" : "=";
            rewriter.insertBefore(ctx.start, labelName + assignment);
            recordEntry(elementIndex, labelName);
        }

        /**
         * Labels an unlabeled block that forms a token set (e.g. an operator
         * group like {@code ('+' | '-')}) as a single {@code +=} list element
         * when it can occur more than once. ANTLR only permits labels on
         * blocks whose alternatives are single tokens, so all other blocks
         * (multi-token alternatives like {@code ('[' ']')} or blocks with rule
         * references) are left untouched and their elements are labeled
         * individually instead.
         */
        private void labelLiteralBlockElement(ANTLRv4Parser.ElementContext ctx, int elementIndex) {
            ANTLRv4Parser.EbnfContext ebnf = ctx.ebnf();

            boolean repeated = isStarOrPlus(blockSuffix(ebnf)) || isRepeated(ctx);
            if(!repeated || !isTokenSetBlock(ebnf.block())) {
                return;
            }

            String labelName = uniqueName(pluralize(LITERAL_BLOCK_NAME));
            rewriter.insertBefore(ctx.start, labelName + "+=");
            suppressedSubTrees.add(ebnf);
            recordEntry(elementIndex, labelName);
        }

        private void recordEntry(int elementIndex, String labelName) {
            String path = "/r" + currentRuleIndex + "/a" + Math.max(currentAltIndex, 0) + "/e" + elementIndex;
            entriesByRule.computeIfAbsent(currentRuleName, key -> new LinkedHashMap<>()).put(path, labelName);
        }

        private boolean isInsideSuppressedSubTree(ANTLRv4Parser.ElementContext ctx) {
            if(suppressedSubTrees.isEmpty()) {
                return false;
            }
            ParserRuleContext parent = ctx.getParent();
            while(parent != null) {
                if(suppressedSubTrees.contains(parent)) {
                    return true;
                }
                parent = parent.getParent();
            }
            return false;
        }

        private boolean isStringLiteralAtom(ANTLRv4Parser.AtomContext atom) {
            return atom.terminal() != null && atom.terminal().STRING_LITERAL() != null;
        }

        private ANTLRv4Parser.EbnfSuffixContext blockSuffix(ANTLRv4Parser.EbnfContext ebnf) {
            return ebnf.blockSuffix() == null ? null : ebnf.blockSuffix().ebnfSuffix();
        }

        /**
         * A block can carry a label only if ANTLR considers it a token set:
         * every alternative consists of exactly one unsuffixed terminal
         * (token reference or string literal). Anything else labeled as a unit
         * is rejected by ANTLR with "label assigned to a block which is not a
         * set" (error 130).
         */
        private boolean isTokenSetBlock(ANTLRv4Parser.BlockContext block) {
            if(block.altList() == null || block.optionsSpec() != null || !block.ruleAction().isEmpty()) {
                return false;
            }
            for(ANTLRv4Parser.AlternativeContext alt : block.altList().alternative()) {
                if(alt.elementOptions() != null || alt.element().size() != 1) {
                    return false;
                }
                ANTLRv4Parser.ElementContext element = alt.element().get(0);
                if(element.ebnfSuffix() != null || element.atom() == null
                        || element.atom().terminal() == null) {
                    return false;
                }
            }
            return true;
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

        /**
         * An element yields a list property if it carries a {@code *}/{@code +}
         * suffix directly, or if it is nested inside a block that repeats via
         * {@code *}/{@code +}. Optional ({@code ?}) suffixes do not imply lists.
         */
        private boolean isRepeated(ANTLRv4Parser.ElementContext ctx) {
            if(isStarOrPlus(ctx.ebnfSuffix())) {
                return true;
            }

            ParserRuleContext parent = ctx.getParent();
            while(parent != null && !(parent instanceof ANTLRv4Parser.ParserRuleSpecContext)) {
                if(parent instanceof ANTLRv4Parser.EbnfContext) {
                    ANTLRv4Parser.EbnfContext ebnf = (ANTLRv4Parser.EbnfContext) parent;
                    if(ebnf.blockSuffix() != null && isStarOrPlus(ebnf.blockSuffix().ebnfSuffix())) {
                        return true;
                    }
                }
                parent = parent.getParent();
            }

            return false;
        }

        private boolean isStarOrPlus(ANTLRv4Parser.EbnfSuffixContext ebnfSuffix) {
            if(ebnfSuffix == null) {
                return false;
            }

            String text = tokens.getText(ebnfSuffix);
            return text.startsWith("*") || text.startsWith("+");
        }

        private String uniqueName(String baseName) {
            int count = nameCountsInRule.getOrDefault(baseName, 0) + 1;
            String candidate = count == 1 ? baseName : baseName + count;

            // a suffixed candidate may still collide with a name that is
            // already taken, e.g. a manual 'identifier2' label next to an
            // auto-labeled 'identifier': bump the counter until it is free.
            while(count > 1 && nameCountsInRule.containsKey(candidate)) {
                count++;
                candidate = baseName + count;
            }

            nameCountsInRule.put(baseName, count);
            if(count > 1) {
                // reserve the suffixed name itself so a later base name equal
                // to it (e.g. base 'identifier2' after 'identifier2' was
                // generated from base 'identifier') cannot produce it again.
                nameCountsInRule.putIfAbsent(candidate, 1);
            }
            return candidate;
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
