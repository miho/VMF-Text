package eu.mihosoft.vmf.vmftext.grammar;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects all pieces of text that are ignored by the grammar ({@code -> channel("Hidden")}). This class is intended
 * to be used for lexical preserving code generation. In contrast to previous designs it does not depend on the
 * grammar and can be applied to all ANTLR4 parse trees.
 */
public class IgnoredTextListener implements org.antlr.v4.runtime.tree.ParseTreeListener {

    private final List<String> ignoredPiecesOfText = new ArrayList<>();
    private final TokenStream stream;
    private TerminalNode prevNode;

    private static boolean debugOutputEnabled = false;

    /**
     * Constructor. Creates a new instance.
     * @param stream The token stream to process
     */
    public IgnoredTextListener(TokenStream stream) {
        this.stream = stream;
    }

    @Override
    public void visitTerminal(TerminalNode node) {

        if(prevNode==null) {

            int start = 0;
            int stop = node.getSymbol().getStartIndex()-1;

            if(stop - start > -1) {
                String piece = stream.getText(
                        stream.get(0),
                        stream.get(node.getSymbol().getTokenIndex()-1));
                ignoredPiecesOfText.add(piece);

                if(debugOutputEnabled)
                System.out.print("{" + piece + "}");
            }

            prevNode = node;

        } else {

            if(debugOutputEnabled)
            System.out.print("[" + stream.getText(prevNode.getSourceInterval()) + "]");

            int start = prevNode.getSymbol().getStopIndex() + 1;
            int stop = node.getSymbol().getStartIndex() - 1;

            if (stop - start > -1) {
                String piece = stream.getText(
                        stream.get(prevNode.getSymbol().getTokenIndex() + 1),
                        stream.get(node.getSymbol().getTokenIndex() - 1));
                ignoredPiecesOfText.add(piece);

                if(debugOutputEnabled)
                System.out.print("{" + piece + "}");
            }

            prevNode = node;
        }

    }

    @Override
    public void visitErrorNode(ErrorNode node) {

    }

    @Override
    public void enterEveryRule(ParserRuleContext ctx) {

    }

    @Override
    public void exitEveryRule(ParserRuleContext ctx) {

    }

    public List<String> getIgnoredPiecesOfText() {
        return ignoredPiecesOfText;
    }

    public static boolean isDebugOutputEnabled() {
        return debugOutputEnabled;
    }

    public static void setDebugOutputEnabled(boolean debugOutputEnabled) {
        IgnoredTextListener.debugOutputEnabled = debugOutputEnabled;
    }
}
