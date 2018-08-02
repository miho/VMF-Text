package lexicalpreservationtest;

import arraylang.ArrayLangLexer;
import arraylang.ArrayLangParser;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        String code = "_(_1.0,__2,_3,4,5)_";

        CharStream input = CharStreams.fromString(code);

        ArrayLangLexer lexer = new ArrayLangLexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ArrayLangParser parser = new ArrayLangParser(tokens);

        ParserRuleContext tree = parser.array();
        ParseTreeWalker walker = new ParseTreeWalker();

        ParseTreeListener listener = new ParseTreeListener(tokens);
        walker.walk(listener,tree);

        System.out.println("\n");

        listener.getIgnoredPiecesOfText().forEach(s -> {
            System.out.print("{"+s+"}");
        });
    }
}

class ParseTreeListener implements org.antlr.v4.runtime.tree.ParseTreeListener {

    private final List<String> ignoredPiecesOfText = new ArrayList<>();
    private final TokenStream stream;
    private TerminalNode prevNode;

    public ParseTreeListener(TokenStream stream) {
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
                System.out.print("{" + piece + "}");
            }

            prevNode = node;

        } else {

            System.out.print("[" + stream.getText(prevNode.getSourceInterval()) + "]");

            int start = prevNode.getSymbol().getStopIndex() + 1;
            int stop = node.getSymbol().getStartIndex() - 1;

            if (stop - start > -1) {
                String piece = stream.getText(
                        stream.get(prevNode.getSymbol().getTokenIndex() + 1),
                        stream.get(node.getSymbol().getTokenIndex() - 1));
                ignoredPiecesOfText.add(piece);
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
}
