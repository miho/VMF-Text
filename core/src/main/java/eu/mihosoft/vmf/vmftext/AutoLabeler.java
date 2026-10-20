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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.tinylog.Logger;

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
 *       text round-trips instead of being dropped when unparsing. A single
 *       literal is named after its text ({@code ','} → {@code commas},
 *       {@code '['} → {@code lBrackets}, {@code 'static'} →
 *       {@code staticKeywords}); a token-set block is {@code operators}
 *       ({@code operator} when not repeated);</li>
 *   <li>parser rules with two or more unlabeled top-level alternatives receive
 *       deterministic {@code # AltLabel} alternative labels so every
 *       alternative becomes its own typed sub class;</li>
 *   <li>a <em>nested</em> block with two or more alternatives (e.g. the
 *       {@code ('continue' | 'break' | 'return' expression?)} in a C
 *       {@code jumpStatement}) is lifted into a synthetic parser rule
 *       {@code <rule>Choice} whose alternatives then become typed sub classes
 *       like top-level alternatives do. Without this, alternatives that
 *       consist only of literals leave nothing in the model and the choice is
 *       lost when unparsing ({@code return 1;} came back as {@code ;}). Token
 *       sets such as {@code ('+' | '-')} keep their ordered literal capture,
 *       blocks with an empty alternative (an optional in disguise) and blocks
 *       containing manual labels, actions or predicates are left untouched.</li>
 * </ul>
 *
 * <p>Manual labeling always wins and mixes consistently with auto-labeling:
 * explicitly labeled elements and alternatives are kept unchanged, generated
 * element names never collide with manually chosen ones, and a rule that only
 * labels <em>some</em> of its alternatives (which ANTLR would reject) has the
 * remaining alternatives labeled automatically so the grammar stays valid.
 * String literals outside of repeated blocks are left unchanged.</p>
 *
 * <p>Duplicate element names prefer a keyword-context name when the nearest
 * preceding sibling is a keyword-like string literal (e.g. {@code 'else'
 * statement} becomes {@code elseStatement}); otherwise they receive stable
 * numeric suffixes. Names that would collide with a parser rule or alternative
 * label (ANTLR error 69) are emitted in PascalCase (e.g. {@code Statement})
 * so getters stay {@code getStatement()} without a {@code Node} suffix. The
 * numbering is scoped to the generated type: in rules whose alternatives
 * become separate typed sub classes each top-level alternative numbers its
 * names independently, everywhere else names are unique per rule.</p>
 */
final class AutoLabeler {

    private AutoLabeler() {
        throw new AssertionError("Don't instantiate me!");
    }

    static File rewrite(File grammar, boolean emitReport) throws IOException {
        return rewrite(grammar, emitReport, null);
    }

    static File rewrite(File grammar, boolean emitReport, File reportFile) throws IOException {
        return rewrite(grammar, null, emitReport, reportFile);
    }

    /**
     * @param lexerGrammar the lexer grammar of a split grammar (#39), whose
     *                     single-literal rules are the keywords; {@code null}
     *                     for a combined grammar
     */
    static File rewrite(File grammar, File lexerGrammar, boolean emitReport, File reportFile) throws IOException {
        // pass A: lift nested alternative blocks into synthetic rules (repeat so
        // blocks nested inside lifted blocks are lifted too), then pass B labels
        // the result like any other grammar.
        List<String> liftedEntries = new ArrayList<>();
        grammar = liftBlocks(grammar, false, liftedEntries);
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
            if(lexerGrammar != null) {
                try(FileInputStream lexerStream = new FileInputStream(lexerGrammar)) {
                    ANTLRv4Parser lexerParser = new ANTLRv4Parser(new CommonTokenStream(
                            new ANTLRv4Lexer(CharStreams.fromStream(lexerStream))));
                    ParseTreeWalker.DEFAULT.walk(reserved, lexerParser.grammarSpec());
                }
            }

            AutoLabelListener listener = new AutoLabelListener(tokens, rewriter, reserved.reservedNamesLower(),
                    reserved.keywordTokens());

            ParseTreeWalker.DEFAULT.walk(listener, tree);

            if(emitReport && (listener.hasEntries() || !liftedEntries.isEmpty())) {
                String report = listener.report(grammar.getName(), liftedEntries);
                Logger.info(report);
                if(reportFile != null) {
                    File parent = reportFile.getParentFile();
                    if(parent != null) {
                        Files.createDirectories(parent.toPath());
                    }
                    Files.write(reportFile.toPath(), report.getBytes(StandardCharsets.UTF_8));
                }
            }

            Path dir = Files.createTempDirectory("vmf-text-autolabel");
            File grammarOut = new File(dir.toFile(), grammar.getName());
            Files.write(grammarOut.toPath(), rewriter.getText().getBytes(StandardCharsets.UTF_8));

            return grammarOut;
        }
    }

    private static final class BlockLiftResult {
        final File grammar;
        final List<String> entries;
        BlockLiftResult(File grammar, List<String> entries) {
            this.grammar = grammar;
            this.entries = entries;
        }
    }

    /**
     * For grammars that are <em>not</em> auto-labeled: lifts only the nested
     * alternative blocks whose alternatives consist of literals / token
     * references and are not a plain token set, e.g. {@code ('[' ']' | '[]')}
     * or {@code ('<' '<' | '>' '>' '>' | '>' '>')}. Such a block has no model
     * property, so without lifting its text is dropped on unparse (issue #37).
     * Manual labels inside a block disable lifting for that block.
     */
    static File liftLiteralOnlyBlocks(File grammar) throws IOException {
        List<String> entries = new ArrayList<>();
        File result = liftBlocks(grammar, true, entries);
        if(!entries.isEmpty()) {
            StringBuilder report = new StringBuilder("Lifted literal-only alternative blocks in ")
                    .append(grammar.getName()).append(" (each alternative is a typed sub class):\n");
            entries.forEach(e -> report.append("  ").append(e).append('\n'));
            Logger.info(report.toString());
        }
        return result;
    }

    private static File liftBlocks(File grammar, boolean literalOnly, List<String> entriesOut) throws IOException {
        // repeat so blocks nested inside lifted blocks are lifted too
        for(int round = 0; round < 8; round++) {
            BlockLiftResult lifted = liftAlternativeBlocks(grammar, literalOnly);
            if(lifted.entries.isEmpty()) {
                break;
            }
            entriesOut.addAll(lifted.entries);
            grammar = lifted.grammar;
        }
        return grammar;
    }

    /**
     * Pass A of auto-labeling: rewrites every liftable nested alternative block
     * into a reference to a new synthetic parser rule appended to the grammar.
     * Returns the input file unchanged when nothing was lifted.
     */
    private static BlockLiftResult liftAlternativeBlocks(File grammar, boolean literalOnly) throws IOException {
        try(FileInputStream codeStream = new FileInputStream(grammar)) {
            CharStream input = CharStreams.fromStream(codeStream);
            ANTLRv4Lexer lexer = new ANTLRv4Lexer(input);
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            ANTLRv4Parser parser = new ANTLRv4Parser(tokens);
            ParserRuleContext tree = parser.grammarSpec();
            TokenStreamRewriter rewriter = new TokenStreamRewriter(tokens);
            ReservedNameCollector reserved = new ReservedNameCollector();
            ParseTreeWalker.DEFAULT.walk(reserved, tree);
            BlockLifter lifter = new BlockLifter(tokens, rewriter, reserved.reservedNamesLower(), literalOnly);
            ParseTreeWalker.DEFAULT.walk(lifter, tree);
            if(lifter.entries.isEmpty()) {
                return new BlockLiftResult(grammar, lifter.entries);
            }
            // Place the synthetic rules right after the last parser rule. A parser
            // rule that follows the lexer rules would confuse the grammar lexer's
            // '[' handling (ARG_ACTION vs LEXER_CHAR_SET) for the locals [...]
            // that a later rewrite pass injects into every parser rule.
            org.antlr.v4.runtime.Token lastParserRuleEnd = null;
            if(tree instanceof ANTLRv4Parser.GrammarSpecContext) {
                ANTLRv4Parser.GrammarSpecContext spec = (ANTLRv4Parser.GrammarSpecContext) tree;
                if(spec.rules() != null) {
                    for(ANTLRv4Parser.RuleSpecContext rs : spec.rules().ruleSpec()) {
                        if(rs.parserRuleSpec() != null) {
                            lastParserRuleEnd = rs.parserRuleSpec().stop;
                        }
                    }
                }
            }
            if(lastParserRuleEnd != null) {
                rewriter.insertAfter(lastParserRuleEnd, "\n" + lifter.syntheticRules.toString());
            } else {
                rewriter.insertBefore(tree.stop, lifter.syntheticRules.toString());
            }
            Path dir = Files.createTempDirectory("vmf-text-autolabel-lift");
            File grammarOut = new File(dir.toFile(), grammar.getName());
            Files.write(grammarOut.toPath(), rewriter.getText().getBytes(StandardCharsets.UTF_8));
            return new BlockLiftResult(grammarOut, lifter.entries);
        }
    }

    /**
     * Finds nested alternative blocks whose choice would be lost in the model
     * and replaces each with a labeled reference to a synthetic rule
     * {@code <rule>Choice[N]} that carries the block's alternatives as
     * labeled top-level alternatives ({@code # <Rule>ChoiceAltN}), i.e. typed
     * sub classes. The labels are emitted here so the result is the same with
     * and without the auto-label pass. The block's EBNF suffix moves to the
     * reference, so {@code (a | b)*} becomes a single ordered list of the
     * synthetic type instead of two lists that lose their interleaving.
     *
     * <p>In {@code literalOnly} mode (grammars that are not auto-labeled) only
     * blocks whose alternatives consist purely of literals / token references
     * and that are not a plain token set are lifted; everything else is left
     * exactly as the author wrote it.</p>
     */
    private static final class BlockLifter extends ANTLRv4ParserBaseListener {
        private final CommonTokenStream tokens;
        private final TokenStreamRewriter rewriter;
        private final Set<String> reservedNamesLower;
        private final boolean literalOnly;
        final StringBuilder syntheticRules = new StringBuilder();
        final List<String> entries = new ArrayList<>();
        private final Set<ParserRuleContext> liftedBlocks = new HashSet<>();
        private String currentRuleName;
        private int currentRuleIndex = -1;

        BlockLifter(CommonTokenStream tokens, TokenStreamRewriter rewriter, Set<String> reservedNamesLower,
                    boolean literalOnly) {
            this.tokens = tokens;
            this.rewriter = rewriter;
            this.reservedNamesLower = reservedNamesLower;
            this.literalOnly = literalOnly;
        }

        @Override
        public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            currentRuleName = ctx.RULE_REF().getText();
            currentRuleIndex++;
        }

        @Override
        public void exitParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            currentRuleName = null;
        }

        @Override
        public void enterElement(ANTLRv4Parser.ElementContext ctx) {
            if(currentRuleName == null || ctx.ebnf() == null || ctx.ebnf().block() == null) {
                return;
            }
            if(isInside(ctx, liftedBlocks)) {
                return; // moves verbatim with the enclosing lifted block
            }
            ANTLRv4Parser.BlockContext block = ctx.ebnf().block();
            String suffix = ctx.ebnf().blockSuffix() == null ? "" : tokens.getText(ctx.ebnf().blockSuffix());
            boolean choice = isLiftable(block);
            boolean perItem = !choice && !literalOnly && isPerItemLiftable(block, suffix);
            if(!choice && !perItem) {
                return;
            }
            String ruleName = uniqueRuleName(camelCase(currentRuleName) + (choice ? "Choice" : "Item"));
            boolean repeated = suffix.startsWith("*") || suffix.startsWith("+") || isInsideRepetition(ctx);

            StringBuilder ruleText = new StringBuilder();
            List<ANTLRv4Parser.AlternativeContext> alts = block.altList().alternative();
            if(choice) {
                // the synthetic rule: one labeled alternative per block alternative
                for(int i = 0; i < alts.size(); i++) {
                    String altLabel = uniqueAltLabel(StringUtil.firstToUpper(ruleName) + "Alt" + (i + 1));
                    ruleText.append(i == 0 ? "    : " : "    | ")
                            .append(tokens.getText(alts.get(i))).append(" # ").append(altLabel).append('\n');
                }
                syntheticRules.append("\n// lifted by VMF-Text from rule '").append(currentRuleName)
                        .append("' so each alternative becomes its own typed sub class\n")
                        .append(ruleName).append('\n').append(ruleText).append("    ;\n");
            } else {
                // the synthetic rule: the block's single alternative, so every
                // iteration becomes one element of a typed list and an optional
                // property inside it stays attached to its iteration
                ruleText.append("    : ").append(tokens.getText(alts.get(0))).append('\n');
                syntheticRules.append("\n// lifted by VMF-Text from rule '").append(currentRuleName)
                        .append("' so each iteration becomes its own typed element\n")
                        .append(ruleName).append('\n').append(ruleText).append("    ;\n");
            }

            // the reference: a labeled (list) property of the enclosing rule. PascalCase
            // for the singular form because a label equal to a rule name is ANTLR error 69.
            String label = repeated ? ruleName + "s" : StringUtil.firstToUpper(ruleName);
            String assignment = repeated ? "+=" : "=";
            rewriter.replace(ctx.ebnf().start, ctx.ebnf().stop, label + assignment + ruleName + suffix);
            liftedBlocks.add(ctx.ebnf());
            entries.add("/r" + currentRuleIndex + " " + currentRuleName + ": block "
                    + abbreviate(tokens.getText(block)) + suffix + " -> " + label + assignment + ruleName);
        }

        private String uniqueAltLabel(String base) {
            String candidate = base;
            int n = 1;
            while(reservedNamesLower.contains(candidate.toLowerCase())) {
                n++;
                candidate = base + "_" + n;
            }
            reservedNamesLower.add(candidate.toLowerCase());
            return candidate;
        }

        /** true when the element sits inside a block that repeats via '*' / '+' */
        private boolean isInsideRepetition(ANTLRv4Parser.ElementContext ctx) {
            for(ParserRuleContext p = ctx.getParent(); p != null && !(p instanceof ANTLRv4Parser.ParserRuleSpecContext); p = p.getParent()) {
                if(p instanceof ANTLRv4Parser.EbnfContext) {
                    ANTLRv4Parser.EbnfContext ebnf = (ANTLRv4Parser.EbnfContext) p;
                    if(ebnf.blockSuffix() != null) {
                        String t = tokens.getText(ebnf.blockSuffix());
                        if(t.startsWith("*") || t.startsWith("+")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        /**
         * Liftable: two or more alternatives, none empty, not a token set
         * (those keep their literal capture / round-trip via recorded states),
         * no options/actions, and no manual labels, actions or predicates
         * anywhere inside. In literal-only mode every element of every
         * alternative must additionally be an unsuffixed literal or token
         * reference.
         */
        private boolean isLiftable(ANTLRv4Parser.BlockContext block) {
            if(block.altList() == null || block.optionsSpec() != null || !block.ruleAction().isEmpty()) {
                return false;
            }
            List<ANTLRv4Parser.AlternativeContext> alts = block.altList().alternative();
            if(alts.size() < 2) {
                return false;
            }
            for(ANTLRv4Parser.AlternativeContext alt : alts) {
                if(alt.elementOptions() != null || alt.element().isEmpty()) {
                    return false;
                }
                if(literalOnly) {
                    for(ANTLRv4Parser.ElementContext el : alt.element()) {
                        if(el.atom() == null || el.atom().terminal() == null || el.ebnfSuffix() != null) {
                            return false;
                        }
                    }
                }
            }
            if(isTokenSetBlock(block)) {
                return false;
            }
            return !containsManualLabelOrAction(block);
        }

        /**
         * Per-item liftable: a repeated block ({@code *} or {@code +}) with a
         * single non-empty alternative that contains a property-bearing element
         * whose count per iteration is not exactly one — an optional or repeated
         * rule / token reference, or an optional / repeated sub-block with such a
         * reference inside. Captured as parallel lists ({@code tfpdefs+=…},
         * {@code tests+=…}) the optional property loses its iteration:
         * {@code (',' tfpdef ('=' test)?)*} drifted Python's default arguments to
         * the first parameters. Literal-only optionals are fine (their presence
         * is recorded per occurrence) and so are plain nested choices (one choice
         * object per iteration). Manual labels disable lifting as for choices.
         */
        private boolean isPerItemLiftable(ANTLRv4Parser.BlockContext block, String suffix) {
            if(!(suffix.startsWith("*") || suffix.startsWith("+"))) {
                return false;
            }
            if(block.altList() == null || block.optionsSpec() != null || !block.ruleAction().isEmpty()) {
                return false;
            }
            List<ANTLRv4Parser.AlternativeContext> alts = block.altList().alternative();
            if(alts.size() != 1 || alts.get(0).elementOptions() != null || alts.get(0).element().isEmpty()) {
                return false;
            }
            if(containsManualLabelOrAction(block)) {
                return false;
            }
            return hasVariableArityProperty(alts.get(0));
        }

        private boolean hasVariableArityProperty(ANTLRv4Parser.AlternativeContext alt) {
            for(ANTLRv4Parser.ElementContext el : alt.element()) {
                boolean suffixed = el.ebnfSuffix() != null;
                if(el.atom() != null) {
                    if(suffixed && isPropertyAtom(el.atom())) {
                        return true;
                    }
                } else if(el.ebnf() != null && el.ebnf().block() != null) {
                    ANTLRv4Parser.BlockContext inner = el.ebnf().block();
                    boolean innerSuffixed = el.ebnf().blockSuffix() != null;
                    if(innerSuffixed) {
                        if(containsPropertyAtom(inner)) {
                            return true;
                        }
                    } else if(inner.altList() != null && inner.altList().alternative().size() == 1) {
                        if(hasVariableArityProperty(inner.altList().alternative().get(0))) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }

        private boolean containsPropertyAtom(ANTLRv4Parser.BlockContext block) {
            if(block.altList() == null) {
                return false;
            }
            for(ANTLRv4Parser.AlternativeContext alt : block.altList().alternative()) {
                for(ANTLRv4Parser.ElementContext el : alt.element()) {
                    if(el.atom() != null && isPropertyAtom(el.atom())) {
                        return true;
                    }
                    if(el.ebnf() != null && el.ebnf().block() != null && containsPropertyAtom(el.ebnf().block())) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** A rule reference or a token reference (not a string literal) — becomes a model property. */
        private static boolean isPropertyAtom(ANTLRv4Parser.AtomContext atom) {
            if(atom.ruleref() != null) {
                return true;
            }
            return atom.terminal() != null && atom.terminal().TOKEN_REF() != null
                    && !"EOF".equals(atom.terminal().TOKEN_REF().getText());
        }

        private boolean containsManualLabelOrAction(ParserRuleContext ctx) {
            if(ctx instanceof ANTLRv4Parser.LabeledElementContext
                    || ctx instanceof ANTLRv4Parser.ActionBlockContext) {
                return true;
            }
            for(int i = 0; i < ctx.getChildCount(); i++) {
                if(ctx.getChild(i) instanceof ParserRuleContext
                        && containsManualLabelOrAction((ParserRuleContext) ctx.getChild(i))) {
                    return true;
                }
            }
            return false;
        }

        private String uniqueRuleName(String base) {
            String candidate = base;
            int n = 1;
            while(reservedNamesLower.contains(candidate.toLowerCase())) {
                n++;
                candidate = base + n;
            }
            reservedNamesLower.add(candidate.toLowerCase());
            return candidate;
        }

        private static boolean isInside(ParserRuleContext ctx, Set<ParserRuleContext> ancestors) {
            for(ParserRuleContext p = ctx.getParent(); p != null; p = p.getParent()) {
                if(ancestors.contains(p)) {
                    return true;
                }
            }
            return false;
        }

        private static String abbreviate(String text) {
            String oneLine = text.replaceAll("\\s+", " ");
            return oneLine.length() > 60 ? oneLine.substring(0, 57) + "..." : oneLine;
        }
    }

    /**
     * A block can carry a label only if ANTLR considers it a token set:
     * every alternative consists of exactly one unsuffixed terminal
     * (token reference or string literal). Anything else labeled as a unit
     * is rejected by ANTLR with "label assigned to a block which is not a
     * set" (error 130).
     */
    /**
     * {@code simple_stmt} → {@code simpleStmt}: property names and synthetic
     * rule names derived from snake_case rule names drop the underscores
     * (#47). The model class of the rule itself keeps ANTLR's spelling.
     */
    static String camelCase(String name) {
        if(name == null || name.indexOf('_') < 0) {
            return name;
        }
        StringBuilder sb = new StringBuilder();
        for(String part : name.split("_+")) {
            if(part.isEmpty()) {
                continue;
            }
            sb.append(sb.length() == 0 ? part : StringUtil.firstToUpper(part));
        }
        return sb.length() == 0 ? name : sb.toString();
    }

    private static boolean isTokenSetBlock(ANTLRv4Parser.BlockContext block) {
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

    /**
     * Collects the names that generated alternative labels must not collide
     * with: all parser rule names and any explicit alternative labels.
     */
    private static final class ReservedNameCollector extends ANTLRv4ParserBaseListener {
        private final Set<String> reservedLower = new HashSet<>();
        // lexer rules that are a single literal (Var: 'var';): keywords, see #47
        private final Map<String,String> keywordTokens = new LinkedHashMap<>();

        @Override
        public void enterLexerRuleSpec(ANTLRv4Parser.LexerRuleSpecContext ctx) {
            if(ctx.FRAGMENT() != null || ctx.TOKEN_REF() == null || ctx.lexerRuleBlock() == null
                    || ctx.lexerRuleBlock().lexerAltList() == null) {
                return;
            }
            List<ANTLRv4Parser.LexerAltContext> alts = ctx.lexerRuleBlock().lexerAltList().lexerAlt();
            if(alts.size() != 1 || alts.get(0).lexerCommands() != null || alts.get(0).lexerElements() == null) {
                return;
            }
            List<ANTLRv4Parser.LexerElementContext> elements = alts.get(0).lexerElements().lexerElement();
            if(elements.size() != 1 || elements.get(0).ebnfSuffix() != null || elements.get(0).lexerAtom() == null
                    || elements.get(0).lexerAtom().terminal() == null
                    || elements.get(0).lexerAtom().terminal().STRING_LITERAL() == null) {
                return;
            }
            String literal = elements.get(0).lexerAtom().terminal().STRING_LITERAL().getText();
            keywordTokens.put(ctx.TOKEN_REF().getText(), literal.substring(1, literal.length() - 1));
        }

        Map<String,String> keywordTokens() {
            return keywordTokens;
        }

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
        private final Map<String,String> keywordTokens;
        private final Map<String,Integer> nameCountsInRule = new HashMap<>();
        private final Map<String,Map<String,String>> entriesByRule = new LinkedHashMap<>();
        // operand names planned per alternative (expr op expr -> left / right, #47)
        private final Map<ParserRuleContext,String> plannedNames = new HashMap<>();
        // blocks that were labeled as a single unit: their interior must not be
        // labeled again (avoids double labeling of operator/separator literals).
        private final Set<ParserRuleContext> suppressedSubTrees = new HashSet<>();
        private String currentRuleName;
        private int currentRuleIndex = -1;
        private int currentAltIndex;
        private int currentElementIndex;
        // true while inside a rule whose top-level alternatives each become
        // their own typed sub class: element names are then numbered per
        // alternative instead of per rule.
        private boolean altTypedRule;

        AutoLabelListener(CommonTokenStream tokens, TokenStreamRewriter rewriter, Set<String> reservedNamesLower,
                          Map<String,String> keywordTokens) {
            this.tokens = tokens;
            this.rewriter = rewriter;
            this.reservedNamesLower = reservedNamesLower;
            this.keywordTokens = keywordTokens;
        }

        @Override
        public void enterParserRuleSpec(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            currentRuleName = ctx.RULE_REF().getText();
            currentRuleIndex++;
            currentAltIndex = -1;
            currentElementIndex = 0;
            nameCountsInRule.clear();

            altTypedRule = labelAlternatives(ctx);

            if(!altTypedRule) {
                // reserve the names of manually labeled elements so auto-generated
                // element labels never collide with the manual ones. (Alt-typed
                // rules seed per top-level alternative instead, see
                // enterAlternative.)
                seedManualElementLabels(ctx);
            }
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
         *
         * @return {@code true} if every top-level alternative carries its own
         *         distinct label after this pass, i.e. each alternative becomes
         *         a separate typed sub class with its own element-name
         *         namespace
         */
        private boolean labelAlternatives(ANTLRv4Parser.ParserRuleSpecContext ctx) {
            if(ctx.ruleBlock() == null || ctx.ruleBlock().ruleAltList() == null) {
                return false;
            }

            List<ANTLRv4Parser.LabeledAltContext> alts = ctx.ruleBlock().ruleAltList().labeledAlt();

            if(alts.size() < 2) {
                return false;
            }

            int labeledCount = 0;
            boolean hasEmptyUnlabeled = false;
            boolean duplicateManualLabels = false;
            Set<String> manualLabels = new HashSet<>();
            for(ANTLRv4Parser.LabeledAltContext alt : alts) {
                if(alt.identifier() != null) {
                    labeledCount++;
                    if(!manualLabels.add(alt.identifier().getText())) {
                        duplicateManualLabels = true;
                    }
                } else if(isEmptyAlternative(alt)) {
                    hasEmptyUnlabeled = true;
                }
            }

            // the same manual label on several alternatives merges them into a
            // single type, so element names must stay unique across the whole
            // rule in that case.
            boolean altTyped = !duplicateManualLabels;

            if(labeledCount == alts.size()) {
                return altTyped; // fully manual labeling: nothing to complete
            }

            boolean completingManualLabels = labeledCount > 0;

            // For fully unlabeled rules we stay conservative and skip rules that
            // contain an empty alternative (labeling it would introduce an empty
            // sub class). When completing manual labels we must label every
            // alternative (including empty ones) to keep the grammar valid.
            if(!completingManualLabels && hasEmptyUnlabeled) {
                return false;
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

            return altTyped;
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

            // each top-level alternative of an alt-typed rule is its own type,
            // so it gets a fresh element-name namespace seeded with its own
            // manual labels. Nested block alternatives (parent is not a
            // labeledAlt) share the namespace of their top-level alternative.
            if(altTypedRule && ctx.getParent() instanceof ANTLRv4Parser.LabeledAltContext) {
                nameCountsInRule.clear();
                seedManualElementLabels(ctx);
            }
            planBinaryOperands(ctx);
        }

        /**
         * {@code expr op expr}: the operands become {@code left} and
         * {@code right} instead of {@code expr} / {@code expr2} (#47). Only the
         * exact three-element shape qualifies: the same rule on both sides,
         * unlabeled and unsuffixed, and an operator in the middle (a literal, a
         * keyword token, a token reference, a token set or a different rule).
         */
        private void planBinaryOperands(ANTLRv4Parser.AlternativeContext ctx) {
            List<ANTLRv4Parser.ElementContext> els = ctx.element();
            if(els.size() != 3) {
                return;
            }
            String left = unlabeledRuleRef(els.get(0));
            String right = unlabeledRuleRef(els.get(2));
            if(left == null || !left.equals(right) || !isOperatorElement(els.get(1), left)) {
                return;
            }
            if(nameCountsInRule.containsKey("left") || nameCountsInRule.containsKey("right")
                    || reservedNamesLower.contains("left") || reservedNamesLower.contains("right")) {
                return;
            }
            plannedNames.put(els.get(0), "left");
            plannedNames.put(els.get(2), "right");
        }

        private String unlabeledRuleRef(ANTLRv4Parser.ElementContext el) {
            if(el.labeledElement() != null || el.ebnfSuffix() != null || el.atom() == null
                    || el.atom().ruleref() == null) {
                return null;
            }
            return el.atom().ruleref().RULE_REF().getText();
        }

        private boolean isOperatorElement(ANTLRv4Parser.ElementContext el, String operandRule) {
            if(el.labeledElement() != null || el.ebnfSuffix() != null) {
                return false;
            }
            if(el.atom() != null) {
                if(isStringLiteralAtom(el.atom())) {
                    return true;
                }
                if(el.atom().terminal() != null && el.atom().terminal().TOKEN_REF() != null) {
                    return !"EOF".equals(el.atom().terminal().TOKEN_REF().getText());
                }
                return el.atom().ruleref() != null && !operandRule.equals(el.atom().ruleref().RULE_REF().getText());
            }
            return el.ebnf() != null && el.ebnf().block() != null && el.ebnf().blockSuffix() == null
                    && isTokenSetBlock(el.ebnf().block());
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
            String referencedName = referencedRuleName(ctx.atom());

            String baseName;
            if(referencedName != null) {
                if("EOF".equals(referencedName)) {
                    return;
                }
                String keyword = keywordTokens.get(referencedName);
                if(keyword != null) {
                    // a lexer rule that is a single literal (Var: 'var';) is a
                    // keyword, not a value: no property unless it repeats, then a
                    // list named after the literal, like a string literal (#47)
                    if(!repeated) {
                        return;
                    }
                    baseName = literalBaseName(keyword);
                } else {
                    String planned = plannedNames.get(ctx);
                    baseName = planned != null ? planned : toPropertyBaseName(referencedName);
                }
            } else if(repeated && isStringLiteralAtom(ctx.atom())) {
                // unnamed string literals inside a repeated block (e.g. the ','
                // in '(',' item)*') would be dropped when unparsing. Capturing
                // them as an ordered list keeps the text round-trippable; the
                // property is named after the literal (',' -> commas).
                baseName = literalBaseName(unquoteStringLiteral(
                        ctx.atom().terminal().STRING_LITERAL().getText()));
            } else {
                return;
            }

            if(repeated) {
                baseName = pluralize(baseName);
            }

            // Prefer keyword-context names over numeric suffixes when a duplicate
            // would otherwise become baseName2 and a preceding keyword literal
            // is available (e.g. 'else' statement → elseStatement). Also treat
            // ANTLR-reserved rule/alt names as taken so we try context first.
            String labelBase = baseName;
            boolean taken = nameCountsInRule.containsKey(baseName)
                    || reservedNamesLower.contains(baseName.toLowerCase());
            if(taken) {
                String keyword = precedingKeywordLiteral(ctx);
                if(keyword != null) {
                    String contextual = StringUtil.firstToLower(keyword)
                            + StringUtil.firstToUpper(baseName);
                    if(!nameCountsInRule.containsKey(contextual)
                            && !reservedNamesLower.contains(contextual.toLowerCase())) {
                        labelBase = contextual;
                    }
                }
            }

            // ANTLR error 69: a label must not match a parser rule (or alt) name.
            // Prefer PascalCase of the same word over the old "Node" suffix so the
            // public getter stays getValue()/getStatement() while the grammar stays
            // valid (label Value vs rule value).
            if(reservedNamesLower.contains(labelBase.toLowerCase())) {
                labelBase = StringUtil.firstToUpper(labelBase);
            }

            String labelName = uniqueName(labelBase);
            String assignment = repeated ? "+=" : "=";
            rewriter.insertBefore(ctx.start, labelName + assignment);
            recordEntry(elementIndex, labelName);
        }

        private static final Map<String,String> LITERAL_NAMES = new HashMap<>();
        static {
            String[][] table = {
                {",", "comma"}, {";", "semicolon"}, {".", "dot"}, {":", "colon"},
                {"(", "lParen"}, {")", "rParen"}, {"[", "lBracket"}, {"]", "rBracket"},
                {"{", "lBrace"}, {"}", "rBrace"}, {"<", "lt"}, {">", "gt"}, {"=", "eq"},
                {"+", "plus"}, {"-", "minus"}, {"*", "star"}, {"/", "slash"}, {"%", "percent"},
                {"!", "bang"}, {"?", "question"}, {"&", "amp"}, {"|", "pipe"}, {"^", "caret"},
                {"~", "tilde"}, {"#", "hash"}, {"@", "at"}, {"$", "dollar"}, {"\\", "backslash"},
                {"'", "quote"}, {"\"", "doubleQuote"}, {"`", "backtick"},
                {"->", "arrow"}, {"=>", "fatArrow"}, {"::", "doubleColon"}, {"...", "ellipsis"},
                {"==", "eqEq"}, {"!=", "notEq"}, {"<=", "le"}, {">=", "ge"}, {"&&", "andAnd"},
                {"||", "orOr"}, {"++", "plusPlus"}, {"--", "minusMinus"}, {"<<", "shiftLeft"},
                {">>", "shiftRight"}, {">>>", "unsignedShiftRight"}, {"**", "starStar"},
                {"+=", "plusEq"}, {"-=", "minusEq"}, {"*=", "starEq"}, {"/=", "slashEq"},
                {"%=", "percentEq"}, {"&=", "ampEq"}, {"|=", "pipeEq"}, {"^=", "caretEq"},
                {"<<=", "shiftLeftEq"}, {">>=", "shiftRightEq"}, {">>>=", "unsignedShiftRightEq"},
            };
            for(String[] e : table) {
                LITERAL_NAMES.put(e[0], e[1]);
            }
        }

        /**
         * Property base name for a captured literal: a known punctuation token
         * by name ({@code ','} → {@code comma}), an unknown punctuation
         * sequence by its characters ({@code '<-'} → {@code ltMinus}), a
         * keyword-like literal as {@code <keyword>Keyword}, anything else
         * {@code symbol}.
         */
        private String literalBaseName(String literal) {
            if(literal == null || literal.isEmpty()) {
                return LITERAL_ATOM_NAME;
            }
            if(literal.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                return StringUtil.firstToLower(literal) + "Keyword";
            }
            String known = LITERAL_NAMES.get(literal);
            if(known != null) {
                return known;
            }
            StringBuilder composed = new StringBuilder();
            for(int i = 0; i < literal.length(); i++) {
                String part = LITERAL_NAMES.get(String.valueOf(literal.charAt(i)));
                if(part == null) {
                    return LITERAL_ATOM_NAME;
                }
                composed.append(composed.length() == 0 ? part : StringUtil.firstToUpper(part));
            }
            return composed.toString();
        }

        /**
         * Walks left among sibling {@code element()}s of the parent alternative
         * (rule or block) and returns the unquoted text of the nearest earlier
         * string-literal atom when it looks like a keyword
         * ({@code [a-zA-Z_][a-zA-Z0-9_]*}), otherwise {@code null}.
         */
        private String precedingKeywordLiteral(ANTLRv4Parser.ElementContext ctx) {
            ParserRuleContext parent = ctx.getParent();
            if(!(parent instanceof ANTLRv4Parser.AlternativeContext)) {
                return null;
            }

            ANTLRv4Parser.AlternativeContext alt = (ANTLRv4Parser.AlternativeContext) parent;
            List<ANTLRv4Parser.ElementContext> siblings = alt.element();
            int index = siblings.indexOf(ctx);
            if(index <= 0) {
                return null;
            }

            for(int i = index - 1; i >= 0; i--) {
                ANTLRv4Parser.ElementContext sibling = siblings.get(i);
                if(sibling.atom() != null && sibling.atom().terminal() != null
                        && sibling.atom().terminal().TOKEN_REF() != null) {
                    String keyword = keywordTokens.get(sibling.atom().terminal().TOKEN_REF().getText());
                    if(keyword != null && keyword.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                        return keyword;
                    }
                    return null;
                }
                if(sibling.atom() == null || !isStringLiteralAtom(sibling.atom())) {
                    continue;
                }
                String literal = sibling.atom().terminal().STRING_LITERAL().getText();
                String unquoted = unquoteStringLiteral(literal);
                if(unquoted != null && unquoted.matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                    return unquoted;
                }
                return null;
            }
            return null;
        }

        private String unquoteStringLiteral(String literal) {
            if(literal == null || literal.length() < 2) {
                return null;
            }
            char first = literal.charAt(0);
            char last = literal.charAt(literal.length() - 1);
            if((first == '\'' && last == '\'') || (first == '"' && last == '"')) {
                return literal.substring(1, literal.length() - 1);
            }
            return literal;
        }

        /**
         * Labels an unlabeled block that forms a token set (e.g. an operator
         * group like {@code ('+' | '-')}) as a single element: a {@code +=}
         * list when it can occur more than once, a plain {@code =} property
         * otherwise. A choice between literals must always be captured or the
         * text cannot be reproduced ({@code ('.' | '->') Identifier} unparsed
         * as {@code onesize} instead of {@code one->size} before). ANTLR only
         * permits labels on blocks whose alternatives are single tokens, so all
         * other blocks are left to alternative lifting / per-element labeling.
         * A parenthesized single literal that is not repeated has no choice to
         * capture and stays untouched.
         */
        private void labelLiteralBlockElement(ANTLRv4Parser.ElementContext ctx, int elementIndex) {
            ANTLRv4Parser.EbnfContext ebnf = ctx.ebnf();

            boolean repeated = isStarOrPlus(blockSuffix(ebnf)) || isRepeated(ctx);
            if(!isTokenSetBlock(ebnf.block())) {
                return;
            }
            boolean isChoice = ebnf.block().altList().alternative().size() > 1;
            if(!repeated && !isChoice) {
                return;
            }

            String base = repeated ? pluralize(LITERAL_BLOCK_NAME) : LITERAL_BLOCK_NAME;
            if(reservedNamesLower.contains(base.toLowerCase())) {
                base = StringUtil.firstToUpper(base); // ANTLR error 69: label == rule name
            }
            String labelName = uniqueName(base);
            rewriter.insertBefore(ctx.start, labelName + (repeated ? "+=" : "="));
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


        private String referencedRuleName(ANTLRv4Parser.AtomContext atom) {
            if(atom.ruleref() != null) {
                return atom.ruleref().getText();
            }

            if(atom.terminal() != null && atom.terminal().TOKEN_REF() != null) {
                return atom.terminal().TOKEN_REF().getText();
            }

            return null;
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
                result = camelCase(StringUtil.firstToLower(referencedName));
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
            // entry → entries, property → properties (consonant + y)
            if(baseName.length() > 1
                    && baseName.endsWith("y")
                    && !isVowel(baseName.charAt(baseName.length() - 2))) {
                return baseName.substring(0, baseName.length() - 1) + "ies";
            }
            return baseName + "s";
        }

        private boolean isVowel(char c) {
            return "aeiouAEIOU".indexOf(c) >= 0;
        }

        boolean hasEntries() {
            return !entriesByRule.isEmpty();
        }

        String report(String grammarFileName) {
            return report(grammarFileName, java.util.Collections.emptyList());
        }

        String report(String grammarFileName, List<String> liftedBlocks) {
            StringBuilder result = new StringBuilder();
            result.append("AutoLabel report");
            if(grammarFileName != null && !grammarFileName.isEmpty()) {
                result.append(" for ").append(grammarFileName);
            }
            result.append(":\n");
            entriesByRule.forEach((rule, entries) -> {
                result.append("  rule ").append(rule).append(":\n");
                entries.forEach((path, name) ->
                        result.append("    element ").append(path).append(" -> ").append(name).append('\n'));
            });
            if(liftedBlocks != null && !liftedBlocks.isEmpty()) {
                result.append("  lifted alternative blocks (each alternative is a typed sub class):\n");
                liftedBlocks.forEach(e -> result.append("    ").append(e).append('\n'));
            }
            result.append("Note: explicit ANTLR labels always win over inferred names.\n");
            return result.toString();
        }
    }
}
