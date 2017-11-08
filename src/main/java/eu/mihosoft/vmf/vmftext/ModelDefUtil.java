package eu.mihosoft.vmf.vmftext;



import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.java9.Java9BaseListener;
import eu.mihosoft.vmf.vmftext.grammar.java9.Java9Lexer;
import eu.mihosoft.vmf.vmftext.grammar.java9.Java9Parser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;

public class ModelDefUtil {
    private ModelDefUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static List<String> getNamesOfDefinedInterfaces(String code) {

        List<String> result = new ArrayList<>();

        Java9BaseListener l = new Java9BaseListener() {

            private String packageName = "";

            @Override
            public void enterPackageDeclaration(Java9Parser.PackageDeclarationContext ctx) {
                packageName = ctx.packageName().getText();
            }

            @Override
            public void enterInterfaceDeclaration(Java9Parser.InterfaceDeclarationContext ctx) {
                result.add(packageName + "."+ctx.normalInterfaceDeclaration().Identifier().getText());
            }
        };

        CharStream input = CharStreams.fromString(code);

        Java9Lexer lexer = new Java9Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java9Parser parser = new Java9Parser(tokens);

        ParserRuleContext tree = parser.compilationUnit();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

        return result;
    }
}
