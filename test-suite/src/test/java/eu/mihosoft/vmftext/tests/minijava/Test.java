/*
 * Copyright 2017-2018 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Copyright 2017-2018 Goethe Center for Scientific Computing, University Frankfurt. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * If you use this software for scientific research then please cite the following publication(s):
 *
 * M. Hoffer, C. Poliwoda, & G. Wittum. (2013). Visual reflection library:
 * a framework for declarative GUI programming on the Java platform.
 * Computing and Visualization in Science, 2013, 16(4),
 * 181–192. http://doi.org/10.1007/s00791-014-0230-y
 */
package eu.mihosoft.vmftext.tests.minijava;

import eu.mihosoft.vmftext.tests.expressionlang.Expr;
import eu.mihosoft.vmftext.tests.expressionlang.NumberExpr;
import eu.mihosoft.vmftext.tests.expressionlang.ParanExpr;
import eu.mihosoft.vmftext.tests.expressionlang.PlusMinusOpExpr;
import eu.mihosoft.vmftext.tests.expressionlang.parser.ExpressionLangModelParser;
import eu.mihosoft.vmftext.tests.minijava.parser.MiniJavaModelParser;
import eu.mihosoft.vmftext.tests.minijava.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.minijava.unparser.Formatter;
import eu.mihosoft.vmftext.tests.minijava.unparser.IdentifierExpressionUnparser;
import eu.mihosoft.vmftext.tests.minijava.unparser.MiniJavaModelUnparser;
import groovy.lang.GroovyShell;
import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNConfigSet;
import org.antlr.v4.runtime.dfa.DFA;
import org.junit.Assert;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;


public class Test {

    @org.junit.Test
    public void miniJavaParseTest() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model = parser.parse(new File("test-code/MiniJavaTestParseUnparseCode.java"));

    }

    @org.junit.Test
    public void miniJavaParseLongCodeTest() throws IOException {

        MiniJavaModelParser parser = new MiniJavaModelParser();

        // generate the model instance by parsing a code file

        MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest1.java"));
        MiniJavaModel model2 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest2.java"));

    }

    @org.junit.Test
    public void miniJavaParseUnparseLongCodeTest() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest1.java"));
        MiniJavaModel model2 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest2.java"));

        MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();
        unparser.setFormatter(new MyFormatter());

        // unparse the current model
        String s1 = unparser.unparse(model1);

        // parse the model from the previously unparsed model
        MiniJavaModel modelup1 = parser.parse(s1);

        Assert.assertEquals(model1, modelup1);

        // unparse the current model
        String s2 = unparser.unparse(model2);

        // parse the model from the previously unparsed model
        MiniJavaModel modelup2 = parser.parse(s2);

        Assert.assertEquals(model2, modelup2);

        // compile and execute original code as well as unparsed code and compare the results
        String contentOrig = compileAndRunMiniJava(
                new File("test-code/MiniJavaLongCodeFileTest1.java")
        );
        String contentUP = compileAndRunMiniJava(s1);
        Assert.assertTrue(!contentOrig.isEmpty());
        Assert.assertTrue(!contentUP.isEmpty());
        Assert.assertEquals(contentOrig, contentUP);

    }

    private String compileAndRunMiniJava(String code) throws IOException {

        GroovyShell shell = new GroovyShell();
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "utf-8");
        System.setOut(ps);
        shell.evaluate(code);
        ps.flush();
        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        ps.close();
        System.setOut(originalOut);
        return content;

    }

    private String compileAndRunMiniJava(File f) throws IOException {

        GroovyShell shell = new GroovyShell();
        PrintStream originalOut = System.out;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos, true, "utf-8");
        System.setOut(ps);
        shell.evaluate(f);
        ps.flush();
        String content = new String(baos.toByteArray(), StandardCharsets.UTF_8);
        ps.close();
        System.setOut(originalOut);
        return content;

    }

    @org.junit.Test
    public void miniJavaParseUnparseLongCodeTestBenchmarkTwoMiniJavaSampleFiles() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest1.java"));
        MiniJavaModel model2 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest2.java"));

        MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();
        unparser.setFormatter(new MyFormatter());

        for (int i = 0; i < 100; i++) {
            // unparse the current model
            String s1 = unparser.unparse(model1);
            // unparse the current model
            String s2 = unparser.unparse(model2);
        }

    }

    @org.junit.Test
    public void miniJavaParseLongCodeTestBenchmarkTwoMiniJavaSampleFiles() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();

        for (int i = 0; i < 100; i++) {
            MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest1.java"));
            MiniJavaModel model2 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest2.java"));
        }

    }

    @org.junit.Test
    public void miniJavaParseUnparseLongCodeTestBenchmark() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest3.java"));


        MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();
        unparser.setFormatter(new MyFormatter());

        for (int i = 0; i < 100; i++) {
            // unparse the current model
            String s1 = unparser.unparse(model1);
        }
    }

    @org.junit.Test
    public void miniJavaParseLongCodeTestBenchmark() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();

        for (int i = 0; i < 100; i++) {
            MiniJavaModel model1 = parser.parse(new File("test-code/MiniJavaLongCodeFileTest3.java"));
        }

    }

    @org.junit.Test
    public void miniJavaParseUnparseTest() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model = parser.parse(new File("test-code/MiniJavaTestParseUnparseCode.java"));

        // unparse the current model
        MiniJavaModelUnparser unparser = new MiniJavaModelUnparser();
        unparser.setFormatter(new MyFormatter());
        String s = unparser.unparse(model);

        // parse the model from the previously unparsed model
        MiniJavaModel modelup = parser.parse(s);

        Assert.assertEquals(model, modelup);


        String code = new MiniJavaModelUnparser().unparse(model);

    }

    @org.junit.Test
    public void miniJavaModifyModelParseUnparseTest() throws IOException {

        // generate the model instance by parsing a code file
        MiniJavaModelParser parser = new MiniJavaModelParser();
        MiniJavaModel model = parser.parse(
                new File("test-code/MiniJavaTestModifyModelParseUnparseCodeInput.java"));

        // adding a custom class declaration via api
        ClassDeclaration classDeclaration = ClassDeclaration.newInstance();
        classDeclaration.setName("MyGeneratedClass");

        classDeclaration.getExtend().add("Main");
        classDeclaration.getExtend().add("MyClass");

        // add a variable declaration
        classDeclaration.getVarDeclarations().add(FieldDeclaration.newBuilder().withDecl(
                VarDeclaration.newBuilder().withVarType(
                        Type.newBuilder().withTypeName("int").
                                withArrayType(true).build()).
                        withName("myGeneratedVar").build()).
                build()
        );

        classDeclaration.getVarDeclarations().
                add(FieldDeclaration.newBuilder().withDecl(
                        VarDeclaration.newBuilder().withVarType(
                                Type.newBuilder().withTypeName("int").
                                        build()).
                                withName("myGeneratedVar2").build()).
                        build()
                );

        // add a custom method
        MethodDeclaration mDecl = MethodDeclaration.newBuilder().withName("myMethod").build();

        // with a parameter a
        ParameterList pList = ParameterList.newInstance();
        pList.getParams().add(Parameter.newBuilder().withName("a").
                withParamType(Type.newBuilder().withTypeName("int").build()).build());
        mDecl.setArgs(pList);
        mDecl.setReturnType(Type.newBuilder().withTypeName("int").build());
        MethodBody methodBody = MethodBody.newInstance();
        mDecl.setBody(methodBody);

        // add a local variable declaration
        LocalDeclaration lDecl = LocalDeclaration.newBuilder().withDecl(VarDeclaration.newBuilder().
                withVarType(Type.newBuilder().withTypeName("boolean").build()).
                withName("myGenLocalV").build()).build();
        methodBody.getVarDeclarations().add(lDecl);

        // add an assignment
        BooleanLitExpression booleanExpression = BooleanLitExpression.newBuilder().withValue(false).build();
        VariableAssignmentStatement assignment = VariableAssignmentStatement.newBuilder().
                withVarName("myGenLocalV").
                withAssignmentExpression(NotExpression.newBuilder().
                        withOperatorExpression(booleanExpression).build()).build();
        methodBody.getBlock().add(assignment);

        // create a multiply expression
        IntLitExpression left = IntLitExpression.newBuilder().withValue(20).build();
        IntLitExpression right = IntLitExpression.newBuilder().withValue(30).build();
        MulExpression mulExpression = MulExpression.newBuilder().
                withLeft(left).
                withRight(right).build();

        // print the result
        methodBody.getBlock().add(PrintStatement.newBuilder().withPrintExpression(mulExpression).build());

        // reuse the mult expression as return statement
        methodBody.setReturnExpr(mulExpression);
        classDeclaration.getMethodDeclarations().add(mDecl);

        // finally add our custom class to the model
        model.getRoot().getClasses().add(classDeclaration);

        // compare the model that has been modified via API with the model directly parsed from code
        MiniJavaModelParser parserup = new MiniJavaModelParser();
        MiniJavaModel modelup = parserup.parse(
                new File("test-code/MiniJavaTestModifyModelParseUnparseCode.java"));
        Assert.assertEquals(model, modelup);

    }


    @org.junit.Test
    public void testParsePerRule() {
        MiniJavaModelParser parser = new MiniJavaModelParser();

        ClassDeclaration classDeclaration = parser.parseClassDeclaration("class MyClass { }");

        Assert.assertTrue("Expected class-decl., got " + classDeclaration.getClass(), classDeclaration instanceof ClassDeclaration);
        Assert.assertEquals("MyClass", classDeclaration.getName());

        Statement statement = parser.parseStatement("a = 2;");

        Assert.assertTrue("Expected variable-assignment-statement, got " + statement.getClass(), statement instanceof VariableAssignmentStatement);

        VariableAssignmentStatement variableAssignmentStatement = (VariableAssignmentStatement) statement;

        Assert.assertEquals("a", variableAssignmentStatement.getVarName());

        PrintStatement printStatement = parser.parsePrintStatement("System.out.println(12);");

        Assert.assertEquals(printStatement.getPrintExpression(), parser.parseIntLitExpression("12"));


        final boolean[] didThrow = {false};

        try {
            Statement s = parser.parsePrintStatement("a = 2;");
        } catch (Exception ex) {
            didThrow[0] = true;
        }

        Assert.assertTrue("Trying to parse a print-statement and specifying variable-assignment code should throw an exception.", didThrow[0]);

        didThrow[0] = false;
        final boolean[] didError = {false};
        try {
            parser.getErrorListeners().add(new ANTLRErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                    didError[0] = true;
                }

                @Override
                public void reportAmbiguity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, boolean exact, BitSet ambigAlts, ATNConfigSet configs) {
                }

                @Override
                public void reportAttemptingFullContext(Parser recognizer, DFA dfa, int startIndex, int stopIndex, BitSet conflictingAlts, ATNConfigSet configs) {
                }

                @Override
                public void reportContextSensitivity(Parser recognizer, DFA dfa, int startIndex, int stopIndex, int prediction, ATNConfigSet configs) {
                }
            });
            ClassDeclaration cDecl = parser.parseClassDeclaration("a = 2;");
            System.out.println("cDecl: " + cDecl);
        } catch (Exception ex) {
            didThrow[0] = true;
        }

        Assert.assertTrue("Trying to parse a class-decl. Should cause the error listeners to trigger.", didError[0]);
        Assert.assertTrue("Trying to parse a class-decl. Should not throw an exception but cause the error listeners to trigger since class-decl ia not a rule with labeled alts", !didThrow[0]);

    }

//    @org.junit.Test
//    public void lexicalPreservationTest() {
//        String code = "class MyClass {\n  public int m1() {return 0;}\n}";
//        ClassDeclaration cDecl = new MiniJavaModelParser().parseClassDeclaration(code);
//
//        List<CodeRange> ranges = cDecl.vmf().content().stream(CodeElement.class).
//                map(cE -> cE.getCodeRange()).collect(Collectors.toList());
//
//        List<CodeRange> newRanges = new ArrayList<>();
//
//        CodeRange prevRange = null;
//        for (CodeRange r : ranges) {
//
//            System.out.println(
//                    "-> " + r.getStart().getIndex() +
//                            ":" + r.getStop().getIndex() + "\n"
//                             + code.substring(r.getStart().getIndex(), r.getStop().getIndex()+1)+"");
//
//
//            if (prevRange == null) {
//                prevRange = r;
//                continue;
//            }
//
////            if (prevRange.getStop().getIndex() != r.getStart().getIndex()) {
////
////                CodeRange newR = CodeRange.newBuilder().
////                        withStart(prevRange.getStop()).
////                        withStop(r.getStart()).build();
////
////                newRanges.add(newR);
////
////                System.out.println(
////                        "-> " + newR.getStart().getIndex() +
////                                " -> " + newR.getStop().getIndex() + ": '"
////                               /* + code.substring(newR.getStart().getIndex(), newR.getStop().getIndex())*/+"'");
////            }
//
//            prevRange = r;
//        }
//
//
//    }
//
//
}


class MyFormatter extends BaseFormatter {

    @Override
    public void pre(MiniJavaModelUnparser unparser, Formatter.RuleInfo ruleInfo, PrintWriter w) {

        String ruleText = ruleInfo.getRuleText();
        String prevRuleText = getPrevRuleInfo().getRuleText();

        if (Objects.equals(prevRuleText, "{")) {
            incIndent();
        }

        if (Objects.equals(ruleText, "}")) {
            decIndent();
        }

        boolean lineBreak = Objects.equals(prevRuleText, "{")
                || Objects.equals(prevRuleText, "}")
                || Objects.equals(prevRuleText, ";");

        if (lineBreak) {
            w.append('\n').append(getIndent());
        } else if (IdentifierExpressionUnparser.
                matchIdentifierExpressionAlt0(prevRuleText + ruleText)
                && !ruleText.equals(";") && !ruleText.equals(")") && !ruleText.equals("(")
                && !ruleText.equals(",")
                || prevRuleText.equals(",")
                || prevRuleText.equals("]")
                || prevRuleText.equals("(")
                || ruleText.equals(")")
                || ruleText.equals("{")
                || prevRuleText.equals("=")) {
            w.append(" ");
        }

        super.pre(unparser, ruleInfo, w);

    }
}