// Generated from /Users/miho/Documents/GitHub/VMF-Text/core/src/main/resources/eu/mihosoft/vmf/vmftext/antlr/RuleMatcher.g4 by ANTLR 4.7.1
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class RuleMatcherParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.7.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, T__2=3, T__3=4, T__4=5, T__5=6, T__6=7, T__7=8, T__8=9, 
		T__9=10, T__10=11, T__11=12, T__12=13, T__13=14, DOUBLE=15, IDENTIFIER=16, 
		WS=17, LINE_COMMENT=18;
	public static final int
		RULE_prog = 0, RULE_expr = 1, RULE_nested = 2, RULE_nested2 = 3, RULE_array = 4;
	public static final String[] ruleNames = {
		"prog", "expr", "nested", "nested2", "array"
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

	@Override
	public String getGrammarFileName() { return "RuleMatcher.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public RuleMatcherParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}
	public static class ProgContext extends ParserRuleContext {
		public ExprContext expr;
		public List<ExprContext> expressions = new ArrayList<ExprContext>();
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public ProgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_prog; }
	}

	public final ProgContext prog() throws RecognitionException {
		ProgContext _localctx = new ProgContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_prog);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(13);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__4 || _la==DOUBLE) {
				{
				{
				setState(10);
				((ProgContext)_localctx).expr = expr(0);
				((ProgContext)_localctx).expressions.add(((ProgContext)_localctx).expr);
				}
				}
				setState(15);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ExprContext extends ParserRuleContext {
		public ExprContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_expr; }
	 
		public ExprContext() { }
		public void copyFrom(ExprContext ctx) {
			super.copyFrom(ctx);
		}
	}
	public static class ParanExprContext extends ExprContext {
		public ExprContext expression;
		public ExprContext expr() {
			return getRuleContext(ExprContext.class,0);
		}
		public ParanExprContext(ExprContext ctx) { copyFrom(ctx); }
	}
	public static class NumberExprContext extends ExprContext {
		public Token value;
		public TerminalNode DOUBLE() { return getToken(RuleMatcherParser.DOUBLE, 0); }
		public NumberExprContext(ExprContext ctx) { copyFrom(ctx); }
	}
	public static class MultDivOpExprContext extends ExprContext {
		public ExprContext left;
		public Token operator;
		public ExprContext right;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public MultDivOpExprContext(ExprContext ctx) { copyFrom(ctx); }
	}
	public static class PlusMinusOpExprContext extends ExprContext {
		public ExprContext left;
		public Token operator;
		public ExprContext right;
		public List<ExprContext> expr() {
			return getRuleContexts(ExprContext.class);
		}
		public ExprContext expr(int i) {
			return getRuleContext(ExprContext.class,i);
		}
		public PlusMinusOpExprContext(ExprContext ctx) { copyFrom(ctx); }
	}

	public final ExprContext expr() throws RecognitionException {
		return expr(0);
	}

	private ExprContext expr(int _p) throws RecognitionException {
		ParserRuleContext _parentctx = _ctx;
		int _parentState = getState();
		ExprContext _localctx = new ExprContext(_ctx, _parentState);
		ExprContext _prevctx = _localctx;
		int _startState = 2;
		enterRecursionRule(_localctx, 2, RULE_expr, _p);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(22);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case DOUBLE:
				{
				_localctx = new NumberExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;

				setState(17);
				((NumberExprContext)_localctx).value = match(DOUBLE);
				}
				break;
			case T__4:
				{
				_localctx = new ParanExprContext(_localctx);
				_ctx = _localctx;
				_prevctx = _localctx;
				setState(18);
				match(T__4);
				setState(19);
				((ParanExprContext)_localctx).expression = expr(0);
				setState(20);
				match(T__5);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			_ctx.stop = _input.LT(-1);
			setState(32);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					if ( _parseListeners!=null ) triggerExitRuleEvent();
					_prevctx = _localctx;
					{
					setState(30);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
					case 1:
						{
						_localctx = new MultDivOpExprContext(new ExprContext(_parentctx, _parentState));
						((MultDivOpExprContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(24);
						if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
						setState(25);
						((MultDivOpExprContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__0 || _la==T__1) ) {
							((MultDivOpExprContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(26);
						((MultDivOpExprContext)_localctx).right = expr(5);
						}
						break;
					case 2:
						{
						_localctx = new PlusMinusOpExprContext(new ExprContext(_parentctx, _parentState));
						((PlusMinusOpExprContext)_localctx).left = _prevctx;
						pushNewRecursionContext(_localctx, _startState, RULE_expr);
						setState(27);
						if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
						setState(28);
						((PlusMinusOpExprContext)_localctx).operator = _input.LT(1);
						_la = _input.LA(1);
						if ( !(_la==T__2 || _la==T__3) ) {
							((PlusMinusOpExprContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
						}
						else {
							if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
							_errHandler.reportMatch(this);
							consume();
						}
						setState(29);
						((PlusMinusOpExprContext)_localctx).right = expr(4);
						}
						break;
					}
					} 
				}
				setState(34);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,3,_ctx);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			unrollRecursionContexts(_parentctx);
		}
		return _localctx;
	}

	public static class NestedContext extends ParserRuleContext {
		public int xyz;
		public double abc;
		public Token id;
		public Token name;
		public Token otherName;
		public TerminalNode DOUBLE() { return getToken(RuleMatcherParser.DOUBLE, 0); }
		public TerminalNode IDENTIFIER() { return getToken(RuleMatcherParser.IDENTIFIER, 0); }
		public NestedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nested; }
	}

	public final NestedContext nested() throws RecognitionException {
		NestedContext _localctx = new NestedContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_nested);
		int _la;
		try {
			setState(53);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,7,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(35);
				match(T__4);
				setState(40);
				_errHandler.sync(this);
				switch (_input.LA(1)) {
				case T__6:
					{
					setState(36);
					match(T__6);
					setState(37);
					((NestedContext)_localctx).id = match(DOUBLE);
					}
					break;
				case T__7:
					{
					setState(38);
					match(T__7);
					setState(39);
					((NestedContext)_localctx).name = match(IDENTIFIER);
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(42);
				match(T__5);
				setState(44);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__8) {
					{
					setState(43);
					match(T__8);
					}
				}

				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(46);
				match(T__4);
				setState(47);
				match(T__9);
				setState(48);
				((NestedContext)_localctx).otherName = match(IDENTIFIER);
				setState(49);
				match(T__5);
				setState(51);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==T__8) {
					{
					setState(50);
					match(T__8);
					}
				}

				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class Nested2Context extends ParserRuleContext {
		public Nested2Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_nested2; }
	}

	public final Nested2Context nested2() throws RecognitionException {
		Nested2Context _localctx = new Nested2Context(_ctx, getState());
		enterRule(_localctx, 6, RULE_nested2);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(55);
			match(T__4);
			setState(59);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__10:
				{
				setState(56);
				match(T__10);
				setState(57);
				match(T__11);
				}
				break;
			case T__12:
				{
				setState(58);
				match(T__12);
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArrayContext extends ParserRuleContext {
		public Token DOUBLE;
		public List<Token> values = new ArrayList<Token>();
		public List<TerminalNode> DOUBLE() { return getTokens(RuleMatcherParser.DOUBLE); }
		public TerminalNode DOUBLE(int i) {
			return getToken(RuleMatcherParser.DOUBLE, i);
		}
		public ArrayContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_array; }
	}

	public final ArrayContext array() throws RecognitionException {
		ArrayContext _localctx = new ArrayContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_array);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(61);
			match(T__4);
			setState(62);
			((ArrayContext)_localctx).DOUBLE = match(DOUBLE);
			((ArrayContext)_localctx).values.add(((ArrayContext)_localctx).DOUBLE);
			setState(67);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==T__13) {
				{
				{
				setState(63);
				match(T__13);
				setState(64);
				((ArrayContext)_localctx).DOUBLE = match(DOUBLE);
				((ArrayContext)_localctx).values.add(((ArrayContext)_localctx).DOUBLE);
				}
				}
				setState(69);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(70);
			match(T__5);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
		switch (ruleIndex) {
		case 1:
			return expr_sempred((ExprContext)_localctx, predIndex);
		}
		return true;
	}
	private boolean expr_sempred(ExprContext _localctx, int predIndex) {
		switch (predIndex) {
		case 0:
			return precpred(_ctx, 4);
		case 1:
			return precpred(_ctx, 3);
		}
		return true;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\24K\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\3\2\7\2\16\n\2\f\2\16\2\21\13\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\5\3\31\n\3\3\3\3\3\3\3\3\3\3\3\3\3\7\3!\n\3\f\3\16\3$\13"+
		"\3\3\4\3\4\3\4\3\4\3\4\5\4+\n\4\3\4\3\4\5\4/\n\4\3\4\3\4\3\4\3\4\3\4\5"+
		"\4\66\n\4\5\48\n\4\3\5\3\5\3\5\3\5\5\5>\n\5\3\6\3\6\3\6\3\6\7\6D\n\6\f"+
		"\6\16\6G\13\6\3\6\3\6\3\6\2\3\4\7\2\4\6\b\n\2\4\3\2\3\4\3\2\5\6\2O\2\17"+
		"\3\2\2\2\4\30\3\2\2\2\6\67\3\2\2\2\b9\3\2\2\2\n?\3\2\2\2\f\16\5\4\3\2"+
		"\r\f\3\2\2\2\16\21\3\2\2\2\17\r\3\2\2\2\17\20\3\2\2\2\20\3\3\2\2\2\21"+
		"\17\3\2\2\2\22\23\b\3\1\2\23\31\7\21\2\2\24\25\7\7\2\2\25\26\5\4\3\2\26"+
		"\27\7\b\2\2\27\31\3\2\2\2\30\22\3\2\2\2\30\24\3\2\2\2\31\"\3\2\2\2\32"+
		"\33\f\6\2\2\33\34\t\2\2\2\34!\5\4\3\7\35\36\f\5\2\2\36\37\t\3\2\2\37!"+
		"\5\4\3\6 \32\3\2\2\2 \35\3\2\2\2!$\3\2\2\2\" \3\2\2\2\"#\3\2\2\2#\5\3"+
		"\2\2\2$\"\3\2\2\2%*\7\7\2\2&\'\7\t\2\2\'+\7\21\2\2()\7\n\2\2)+\7\22\2"+
		"\2*&\3\2\2\2*(\3\2\2\2+,\3\2\2\2,.\7\b\2\2-/\7\13\2\2.-\3\2\2\2./\3\2"+
		"\2\2/8\3\2\2\2\60\61\7\7\2\2\61\62\7\f\2\2\62\63\7\22\2\2\63\65\7\b\2"+
		"\2\64\66\7\13\2\2\65\64\3\2\2\2\65\66\3\2\2\2\668\3\2\2\2\67%\3\2\2\2"+
		"\67\60\3\2\2\28\7\3\2\2\29=\7\7\2\2:;\7\r\2\2;>\7\16\2\2<>\7\17\2\2=:"+
		"\3\2\2\2=<\3\2\2\2>\t\3\2\2\2?@\7\7\2\2@E\7\21\2\2AB\7\20\2\2BD\7\21\2"+
		"\2CA\3\2\2\2DG\3\2\2\2EC\3\2\2\2EF\3\2\2\2FH\3\2\2\2GE\3\2\2\2HI\7\b\2"+
		"\2I\13\3\2\2\2\f\17\30 \"*.\65\67=E";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}