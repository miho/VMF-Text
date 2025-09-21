package eu.mihosoft.vmftext.tests.java24.unparser.antlr4;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.TokenStream;

import java.util.List;

public abstract class JavaParserBase extends Parser {

    public JavaParserBase(TokenStream input){
        super(input);
    }

    public boolean DoLastRecordComponent() {
        ParserRuleContext ctx = this.getContext();
        if (!(ctx instanceof Java24Parser.RecordComponentListContext)) {
            return true; // or throw if this is an unexpected state
        }

        Java24Parser.RecordComponentListContext tctx = (Java24Parser.RecordComponentListContext) ctx;
        List<Java24Parser.RecordComponentContext> rcs = tctx.recordComponent();
        if (rcs.isEmpty()) return true;

        int count = rcs.size();
        for (int c = 0; c < count; ++c) {
            Java24Parser.RecordComponentContext rc = rcs.get(c);
            if (rc.ELLIPSIS() != null && c + 1 < count)
                return false;
        }
        return true;
    }

    public boolean IsNotIdentifierAssign()
    {
        var la = this._input.LA(1);
        // If not identifier, return true because it can't be
        // "identifier = ..."
        switch (la) {
            case Java24Parser.IDENTIFIER:
            case Java24Parser.MODULE:
            case Java24Parser.OPEN:
            case Java24Parser.REQUIRES:
            case Java24Parser.EXPORTS:
            case Java24Parser.OPENS:
            case Java24Parser.TO:
            case Java24Parser.USES:
            case Java24Parser.PROVIDES:
            case Java24Parser.WHEN:
            case Java24Parser.WITH:
            case Java24Parser.TRANSITIVE:
            case Java24Parser.YIELD:
            case Java24Parser.SEALED:
            case Java24Parser.PERMITS:
            case Java24Parser.RECORD:
            case Java24Parser.VAR:
                break;
            default:
                return true;
        }
        var la2 = this._input.LA(2);
        if (la2 != Java24Parser.ASSIGN) return true;
        return false;
    }
}