// Generated from /Users/miho/Documents/GitHub/VMF-Text/core/src/main/resources/eu/mihosoft/vmf/vmftext/antlr/RuleMatcher.g4 by ANTLR 4.7.1
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class RuleMatcherLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, DOUBLE=15, IDENTIFIER=16, 
		WS=17, LINE_COMMENT=18;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	public static final String[] ruleNames = {
		"T__0", "T__1", "T__2", "T__3", "T__4", "T__5", "T__6", "T__7", "T__8", 
		"T__9", "T__10", "T__11", "T__12", "T__13", "DOUBLE", "SIGN", "DIGIT", 
		"DOT", "IDENTIFIER", "WS", "LINE_COMMENT"
	};

	private static final String[] _LITERAL_NAMES = {
		null, "'*'", "'/'", "'+'", "'-'", "'('", "')'", "'id'", "'name'", "';'", 
		"'otherName'", "'a'", "'b'", "'c'", "','"
	};
	private static final String[] _SYMBOLIC_NAMES = {
		null, null, null, null, null, null, null, null, null, null, null, null, 
		null, null, null, "DOUBLE", "IDENTIFIER", "WS", "LINE_COMMENT"
	};
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}


	public RuleMatcherLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "RuleMatcher.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\24\u0096\b\1\4\2"+
		"\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
		"\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
		"\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\3\2\3\2\3\3\3\3\3\4\3\4"+
		"\3\5\3\5\3\6\3\6\3\7\3\7\3\b\3\b\3\b\3\t\3\t\3\t\3\t\3\t\3\n\3\n\3\13"+
		"\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16"+
		"\3\17\3\17\3\20\5\20W\n\20\3\20\6\20Z\n\20\r\20\16\20[\3\20\3\20\7\20"+
		"`\n\20\f\20\16\20c\13\20\3\20\5\20f\n\20\3\20\3\20\6\20j\n\20\r\20\16"+
		"\20k\3\20\5\20o\n\20\3\20\6\20r\n\20\r\20\16\20s\5\20v\n\20\3\21\3\21"+
		"\3\22\3\22\3\23\3\23\3\24\3\24\7\24\u0080\n\24\f\24\16\24\u0083\13\24"+
		"\3\25\6\25\u0086\n\25\r\25\16\25\u0087\3\25\3\25\3\26\3\26\3\26\3\26\7"+
		"\26\u0090\n\26\f\26\16\26\u0093\13\26\3\26\3\26\2\2\27\3\3\5\4\7\5\t\6"+
		"\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\2#\2%\2\'"+
		"\22)\23+\24\3\2\7\3\2\62;\4\2C\\c|\5\2\62;C\\c|\5\2\13\f\17\17\"\"\4\2"+
		"\f\f\17\17\2\u009e\2\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13"+
		"\3\2\2\2\2\r\3\2\2\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2"+
		"\2\2\27\3\2\2\2\2\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2"+
		"\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\3-\3\2\2\2\5/\3\2\2\2\7\61\3\2\2\2\t"+
		"\63\3\2\2\2\13\65\3\2\2\2\r\67\3\2\2\2\179\3\2\2\2\21<\3\2\2\2\23A\3\2"+
		"\2\2\25C\3\2\2\2\27M\3\2\2\2\31O\3\2\2\2\33Q\3\2\2\2\35S\3\2\2\2\37u\3"+
		"\2\2\2!w\3\2\2\2#y\3\2\2\2%{\3\2\2\2\'}\3\2\2\2)\u0085\3\2\2\2+\u008b"+
		"\3\2\2\2-.\7,\2\2.\4\3\2\2\2/\60\7\61\2\2\60\6\3\2\2\2\61\62\7-\2\2\62"+
		"\b\3\2\2\2\63\64\7/\2\2\64\n\3\2\2\2\65\66\7*\2\2\66\f\3\2\2\2\678\7+"+
		"\2\28\16\3\2\2\29:\7k\2\2:;\7f\2\2;\20\3\2\2\2<=\7p\2\2=>\7c\2\2>?\7o"+
		"\2\2?@\7g\2\2@\22\3\2\2\2AB\7=\2\2B\24\3\2\2\2CD\7q\2\2DE\7v\2\2EF\7j"+
		"\2\2FG\7g\2\2GH\7t\2\2HI\7P\2\2IJ\7c\2\2JK\7o\2\2KL\7g\2\2L\26\3\2\2\2"+
		"MN\7c\2\2N\30\3\2\2\2OP\7d\2\2P\32\3\2\2\2QR\7e\2\2R\34\3\2\2\2ST\7.\2"+
		"\2T\36\3\2\2\2UW\5!\21\2VU\3\2\2\2VW\3\2\2\2WY\3\2\2\2XZ\5#\22\2YX\3\2"+
		"\2\2Z[\3\2\2\2[Y\3\2\2\2[\\\3\2\2\2\\]\3\2\2\2]a\5%\23\2^`\5#\22\2_^\3"+
		"\2\2\2`c\3\2\2\2a_\3\2\2\2ab\3\2\2\2bv\3\2\2\2ca\3\2\2\2df\5!\21\2ed\3"+
		"\2\2\2ef\3\2\2\2fg\3\2\2\2gi\5%\23\2hj\5#\22\2ih\3\2\2\2jk\3\2\2\2ki\3"+
		"\2\2\2kl\3\2\2\2lv\3\2\2\2mo\5!\21\2nm\3\2\2\2no\3\2\2\2oq\3\2\2\2pr\5"+
		"#\22\2qp\3\2\2\2rs\3\2\2\2sq\3\2\2\2st\3\2\2\2tv\3\2\2\2uV\3\2\2\2ue\3"+
		"\2\2\2un\3\2\2\2v \3\2\2\2wx\7/\2\2x\"\3\2\2\2yz\t\2\2\2z$\3\2\2\2{|\7"+
		"\60\2\2|&\3\2\2\2}\u0081\t\3\2\2~\u0080\t\4\2\2\177~\3\2\2\2\u0080\u0083"+
		"\3\2\2\2\u0081\177\3\2\2\2\u0081\u0082\3\2\2\2\u0082(\3\2\2\2\u0083\u0081"+
		"\3\2\2\2\u0084\u0086\t\5\2\2\u0085\u0084\3\2\2\2\u0086\u0087\3\2\2\2\u0087"+
		"\u0085\3\2\2\2\u0087\u0088\3\2\2\2\u0088\u0089\3\2\2\2\u0089\u008a\b\25"+
		"\2\2\u008a*\3\2\2\2\u008b\u008c\7\61\2\2\u008c\u008d\7\61\2\2\u008d\u0091"+
		"\3\2\2\2\u008e\u0090\n\6\2\2\u008f\u008e\3\2\2\2\u0090\u0093\3\2\2\2\u0091"+
		"\u008f\3\2\2\2\u0091\u0092\3\2\2\2\u0092\u0094\3\2\2\2\u0093\u0091\3\2"+
		"\2\2\u0094\u0095\b\26\3\2\u0095,\3\2\2\2\16\2V[aeknsu\u0081\u0087\u0091"+
		"\4\2\3\2\b\2\2";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}