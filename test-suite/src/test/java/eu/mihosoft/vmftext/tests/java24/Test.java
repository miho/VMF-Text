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
package eu.mihosoft.vmftext.tests.java24;

import eu.mihosoft.vmftext.tests.java24.*;
import eu.mihosoft.vmftext.tests.java24.BlockStatement;
import eu.mihosoft.vmftext.tests.java24.ClassBody;
import eu.mihosoft.vmftext.tests.java24.ClassBodyDeclaration;
import eu.mihosoft.vmftext.tests.java24.ClassDeclaration;
import eu.mihosoft.vmftext.tests.java24.ClassOrInterfaceModifier;
import eu.mihosoft.vmftext.tests.java24.CompilationUnit;
import eu.mihosoft.vmftext.tests.java24.DoubleType;
import eu.mihosoft.vmftext.tests.java24.FieldDeclaration;
import eu.mihosoft.vmftext.tests.java24.FloatLiteral;
import eu.mihosoft.vmftext.tests.java24.LiteralExpr;
import eu.mihosoft.vmftext.tests.java24.MemberDeclaration;
import eu.mihosoft.vmftext.tests.java24.MethodCall;
import eu.mihosoft.vmftext.tests.java24.MethodDeclaration;
import eu.mihosoft.vmftext.tests.java24.Modifier;
import eu.mihosoft.vmftext.tests.java24.PackageDeclaration;
import eu.mihosoft.vmftext.tests.java24.QualifiedName;
import eu.mihosoft.vmftext.tests.java24.TypeDeclaration;
import eu.mihosoft.vmftext.tests.java24.TypeType;
import eu.mihosoft.vmftext.tests.java24.VariableDeclarator;
import eu.mihosoft.vmftext.tests.java24.parser.Java24ModelParser;
import eu.mihosoft.vmftext.tests.java24.unparser.BaseFormatter;
import eu.mihosoft.vmftext.tests.java24.unparser.Java24ModelUnparser;
import org.junit.Assert;
import org.mdkt.compiler.InMemoryJavaCompiler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

public class Test {

    @org.junit.Test
    public void testParseModifyUnparseBenchmark() throws Exception {

        String code = new String(Files.readAllBytes(Paths.get("test-code/Java8RungeKuttaBenchmark.java")));

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(code);

        Java24ModelUnparser unparser = new Java24ModelUnparser();

        // unparser.setFormatter(new MyFormatter());

        long timeStampBegin = System.nanoTime();

        for (int i = 0; i < 1000; i++) {
            String out = unparser.unparse(model);
        }

        long timeStampEnd = System.nanoTime();

        // TODO replace with proper JMH
        System.out.println("Duration: " + (timeStampEnd - timeStampBegin) * 1e-9);

    }

    @org.junit.Test
    public void testParseModifyUnparseCompileAndRun() throws Exception {

        System.out.println("START");

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(new FileInputStream("test-code/Java8RungeKutta.java"));

        System.out.println("PARSED");

        model.vmf().content().stream().filter(e -> e instanceof eu.mihosoft.vmftext.tests.java24.ClassDeclaration).map(e -> (eu.mihosoft.vmftext.tests.java24.ClassDeclaration) e).
                filter(cls -> "Java8RungeKutta".equals(cls.getClassName())).forEach(cls -> cls.setClassName(cls.getClassName() + "Out"));

        Java24ModelUnparser unparser = new Java24ModelUnparser();

        // unparser.setFormatter(new MyFormatter());

        try (FileWriter fw = new FileWriter(new File("test-code/Java8RungeKuttaOut.java"))) {
            unparser.unparse(model, fw);
        }

        InMemoryJavaCompiler compiler = InMemoryJavaCompiler.newInstance();

        Class<?> originalClass = compiler.compile("Java8RungeKutta",
                new String(Files.readAllBytes(Paths.get("test-code/Java8RungeKutta.java"))));

        Class<?> unparsedClass = compiler.compile("Java8RungeKuttaOut",
                new String(Files.readAllBytes(Paths.get("test-code/Java8RungeKuttaOut.java"))));


        System.out.println("\nRUNNING original version:");

        originalClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);

        double valueOrig = originalClass.getField("value").getDouble(null);

        System.out.println("\nRUNNING unparsed version:");

        unparsedClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);

        double valueUnparsed = originalClass.getField("value").getDouble(null);

        Assert.assertTrue(valueOrig != 0);
        Assert.assertEquals(valueOrig, valueUnparsed, 1e-12);

    }

    @org.junit.Test
    public void testParseUnparseComplexCode() throws Exception {

        String code = new String(Files.readAllBytes(Paths.get("test-code/Java8ComplexCode01.java")));

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(code);

        Java24ModelUnparser unparser = new Java24ModelUnparser();

        //unparser.setFormatter(new MyFormatter());

        String out = unparser.unparse(model);

    }

    @org.junit.Test
    public void testParseUnparseComplexCodeBenchmark() throws Exception {

        String code = new String(Files.readAllBytes(Paths.get("test-code/Java8ComplexCode01.java")));

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(code);


        Java24ModelUnparser unparser = new Java24ModelUnparser();

        // unparser.setFormatter(new MyFormatter());

        long timeStampBegin = System.nanoTime();

        for (int i = 0; i < 1000; i++) {


            String out = unparser.unparse(model);

        }

        long timeStampEnd = System.nanoTime();

        // TODO replace with JMH
        System.out.println("Duration: " + (timeStampEnd - timeStampBegin) * 1e-9);

    }

    @org.junit.Test
    public void testParsePerRuleFieldDecl() {

        Java24ModelParser parser = new Java24ModelParser();
        Java24ModelUnparser unparser = new Java24ModelUnparser();

        // unparser.setFormatter(new MyFormatter());


        eu.mihosoft.vmftext.tests.java24.FieldDeclaration fieldDecl = eu.mihosoft.vmftext.tests.java24.FieldDeclaration.newInstance();
        fieldDecl.setFieldType(eu.mihosoft.vmftext.tests.java24.TypeType.newBuilder().withSimpleType(
                eu.mihosoft.vmftext.tests.java24.DoubleType.newBuilder().build()).build());


        eu.mihosoft.vmftext.tests.java24.VariableDeclarator varDecl = VariableDeclaratorWithExprInit.newBuilder().
                withVarName("myVar1").withInitializer(
                eu.mihosoft.vmftext.tests.java24.LiteralExpr.newBuilder().withLit(
                        eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.5).build()
                ).build()
        ).build();

        fieldDecl.getVarDecls().add(varDecl);

        eu.mihosoft.vmftext.tests.java24.VariableDeclarator varDecl1 = VariableDeclaratorWithExprInit.newBuilder().
                withVarName("myVar2").withInitializer(
                eu.mihosoft.vmftext.tests.java24.LiteralExpr.newBuilder().withLit(
                        eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.6).build()
                ).build()
        ).build();

        fieldDecl.getVarDecls().add(varDecl1);

        eu.mihosoft.vmftext.tests.java24.FieldDeclaration fieldDeclFromParser = parser.parseFieldDeclaration("double myVar1 = 2.5, myVar2 = 2.6;");

        Assert.assertEquals(fieldDecl, fieldDeclFromParser);
    }

    @org.junit.Test
    public void testParsePerRuleClassDecl() {

        Java24ModelParser parser = new Java24ModelParser();
        Java24ModelUnparser unparser = new Java24ModelUnparser();

        // unparser.setFormatter(new MyFormatter());

        eu.mihosoft.vmftext.tests.java24.ClassDeclaration cDecl = eu.mihosoft.vmftext.tests.java24.ClassDeclaration.newInstance();
        eu.mihosoft.vmftext.tests.java24.ClassBody clsBody = eu.mihosoft.vmftext.tests.java24.ClassBody.newInstance();

        eu.mihosoft.vmftext.tests.java24.ClassBodyDeclaration clsBodyDecl = eu.mihosoft.vmftext.tests.java24.ClassBodyDeclaration.newInstance();
        clsBodyDecl.getModifiers().add(eu.mihosoft.vmftext.tests.java24.Modifier.newBuilder().withTypeModifier(
                eu.mihosoft.vmftext.tests.java24.ClassOrInterfaceModifier.newBuilder().withPrivateModifier("private").build()).build());

        eu.mihosoft.vmftext.tests.java24.MemberDeclaration mDecl = eu.mihosoft.vmftext.tests.java24.MemberDeclaration.newInstance();

        eu.mihosoft.vmftext.tests.java24.FieldDeclaration fieldDecl = eu.mihosoft.vmftext.tests.java24.FieldDeclaration.newInstance();
        fieldDecl.setFieldType(eu.mihosoft.vmftext.tests.java24.TypeType.newBuilder().withSimpleType(
                eu.mihosoft.vmftext.tests.java24.DoubleType.newBuilder().build()).build());

        eu.mihosoft.vmftext.tests.java24.VariableDeclarator varDecl = VariableDeclaratorWithExprInit.newBuilder().
                withVarName("myVar1").withInitializer(
                eu.mihosoft.vmftext.tests.java24.LiteralExpr.newBuilder().withLit(
                        eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.5).build()
                ).build()
        ).build();

        fieldDecl.getVarDecls().add(varDecl);

        eu.mihosoft.vmftext.tests.java24.VariableDeclarator varDecl1 = VariableDeclaratorWithExprInit.newBuilder().
                withVarName("myVar2").withInitializer(
                eu.mihosoft.vmftext.tests.java24.LiteralExpr.newBuilder().withLit(
                        eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.6).build()
                ).build()
        ).build();

        fieldDecl.getVarDecls().add(varDecl1);

        mDecl.setFieldDecl(fieldDecl);

        clsBodyDecl.setDeclaration(mDecl);
        clsBody.getDeclarations().add(clsBodyDecl);

        cDecl.setBody(clsBody);
        cDecl.setClassName("MyClass");

        eu.mihosoft.vmftext.tests.java24.ClassDeclaration cDeclFromParser = parser.parseClassDeclaration(
                "class MyClass { private double myVar1 = 2.5, myVar2 = 2.6;}");

        Assert.assertEquals(cDecl, cDeclFromParser);
    }

    public void testCustomClassDefinition() {

        Java24ModelParser parser = new Java24ModelParser();
        Java24ModelUnparser unparser = new Java24ModelUnparser();

        eu.mihosoft.vmftext.tests.java24.CompilationUnit unit = CompilationUnit.newInstance();

        eu.mihosoft.vmftext.tests.java24.PackageDeclaration pDecl = eu.mihosoft.vmftext.tests.java24.PackageDeclaration.newInstance();
        pDecl.defPackageNameFromString("eu.mihosoft.v123");

        unit.setPackageDecl(pDecl);

        eu.mihosoft.vmftext.tests.java24.TypeDeclaration tDecl = TypeDeclaration.newInstance();
        eu.mihosoft.vmftext.tests.java24.ClassDeclaration cDecl = ClassDeclaration.newInstance();
        eu.mihosoft.vmftext.tests.java24.ClassBody clsBody = ClassBody.newInstance();

        eu.mihosoft.vmftext.tests.java24.ClassBodyDeclaration clsBodyDecl = ClassBodyDeclaration.newInstance();
        clsBodyDecl.getModifiers().add(Modifier.newBuilder().withTypeModifier(
                ClassOrInterfaceModifier.newBuilder().withPrivateModifier("private").build()).build());

        eu.mihosoft.vmftext.tests.java24.MemberDeclaration mDecl = MemberDeclaration.newInstance();

        eu.mihosoft.vmftext.tests.java24.FieldDeclaration fieldDecl = FieldDeclaration.newInstance();
        fieldDecl.setFieldType(TypeType.newBuilder().withSimpleType(
                DoubleType.newBuilder().build()).build());

        eu.mihosoft.vmftext.tests.java24.VariableDeclarator varDecl = VariableDeclaratorWithExprInit.newBuilder().
                withVarName("myVar1").withInitializer(
                eu.mihosoft.vmftext.tests.java24.LiteralExpr.newBuilder().withLit(
                        eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.5).build()
                ).build()
        ).build();

        fieldDecl.getVarDecls().add(varDecl);

        VariableDeclarator varDecl1 = VariableDeclaratorWithExprInit.newBuilder().
                withVarName("myVar2").withInitializer(
                LiteralExpr.newBuilder().withLit(
                        eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.6).build()
                ).build()
        ).build();

        fieldDecl.getVarDecls().add(varDecl1);

        mDecl.setFieldDecl(fieldDecl);

        clsBodyDecl.setDeclaration(mDecl);
        clsBody.getDeclarations().add(clsBodyDecl);

        cDecl.setBody(clsBody);
        cDecl.setClassName("MyClass");
        tDecl.setClassDecl(cDecl);

        unit.getTypeDeclarations().add(tDecl);

        System.out.println(unparser.unparse(unit));

        eu.mihosoft.vmftext.tests.java24.FloatLiteral f1 = eu.mihosoft.vmftext.tests.java24.FloatLiteral.newBuilder().withFloatValue(2.5).build();
        eu.mihosoft.vmftext.tests.java24.FloatLiteral f2 = FloatLiteral.newBuilder().withHexValue(2.5).build();

        System.out.println("F1: " + unparser.unparse(f1));
        System.out.println("F2: " + unparser.unparse(f2));
    }


    @org.junit.Test
    public void testChangeVoidMethods() throws Exception {

        String code = "" +
                "class MyClass {\n" +
                "  public int m1() {return 0;}\n" +
                "  public void m2(int a, int b) {\n" +
                "    // do something\n" +
                "  }\n" +
                "  public java.util.List<String> m3(String a, String b) {\n" +
                "    class InnerClass {\n" +
                "      private void innerMethod() {}\n" +
                "    }\n" +
                "    return null;\n" +
                "  }\n" +
                "}\n";

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(code);

        model.vmf().content().stream(eu.mihosoft.vmftext.tests.java24.MethodDeclaration.class).
                filter(eu.mihosoft.vmftext.tests.java24.MethodDeclaration::returnsVoid).
                forEach(m ->
                        m.getParams().getParams().add(
                                parser.parseFormalParameter("int insP")
                        )
                );

        Java24ModelUnparser unparser = new Java24ModelUnparser();
        // unparser.setFormatter(new MyFormatter());

        String transformed = unparser.unparse(model);

        System.out.println("Original:    " + code);
        System.out.println("Transformed: " + transformed);

        Java24Model transformedModel = parser.parse(transformed);

        Assert.assertEquals(model, transformedModel);

    }

    @org.junit.Test
    public void testChangeOnParamMethods() throws Exception {

        String code = "" +
                "class MyClass {\n" +
                "  public int m1(String a) {return 0;}\n" +
                "  public void m2(int a, int b) {\n" +
                "    // do something\n" +
                "  }\n" +
                "  public java.util.List<String> m3(String a, String b) {\n" +
                "    class InnerClass {\n" +
                "      private void innerMethod1(String b) {}\n" +
                "      private void innerMethod2() {}\n" +
                "    }\n" +
                "    return null;\n" +
                "  }\n" +
                "}\n";

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(code);

        model.vmf().content().stream(eu.mihosoft.vmftext.tests.java24.MethodDeclaration.class).
                filter(m-> m.getParams().getParams().size()==1).
                forEach(m ->
                        m.getParams().getParams().add(
                                parser.parseFormalParameter("int insP")
                        )
                );

        Java24ModelUnparser unparser = new Java24ModelUnparser();
        // unparser.setFormatter(new MyFormatter());

        String transformed = unparser.unparse(model);

        System.out.println("Original:    " + code);
        System.out.println("Transformed: " + transformed);

        Java24Model transformedModel = parser.parse(transformed);

        Assert.assertEquals(model, transformedModel);

    }

    @org.junit.Test
    public void testScopeInstrumentations() throws Exception {

        String code = "" +
                "class MyClass {\n" +
                "  public int m1(String a) {return 0;}\n" +
                "  public void m2(int a, int b) {\n" +
                "    // do something\n" +
                "  }\n" +
                "  public java.util.List<String> m3(String a, String b) {\n" +
                "    class InnerClass {\n" +
                "      private void innerMethod1(String b) {}\n" +
                "      private void innerMethod2() {}\n" +
                "    }\n" +
                "    return null;\n" +
                "  }\n" +
                "}\n";

        Java24ModelParser parser = new Java24ModelParser();
        Java24Model model = parser.parse(code);

        model.vmf().content().stream(MethodDeclaration.class).
                forEach(m -> {
                    BlockStatement statement =
                            parser.parseBlockStatement(
                                    "System.out.println(\"> entering '"
                                            + m.getMethodName() + "()'\");"
                            );
                    m.getBody().getMethodBlock().getStatements().add(0,statement);
                });

        long numInstrumentationCalls = model.vmf().content().stream(MethodCall.class).filter(
                mC->"println".equals(mC.getMethodName())).count();

        // the instrumentation is expected to add 5 method calls
        Assert.assertEquals(5, numInstrumentationCalls);

        Java24ModelUnparser unparser = new Java24ModelUnparser();
        // unparser.setFormatter(new MyFormatter());

        String transformed = unparser.unparse(model);

        System.out.println("Original:    " + code);
        System.out.println("Transformed: " + transformed);

        Java24Model transformedModel = parser.parse(transformed);

        Assert.assertEquals(model, transformedModel);

    }


}

class MyFormatter extends BaseFormatter {

    @Override
    public void pre(Java24ModelUnparser unparser, RuleInfo ruleInfo, PrintWriter w) {

        // TODO Allow rule consume() to provide manually
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
                || (Objects.equals(prevRuleText, ";"));

        if (insideFor(prevRuleText)) {
            lineBreak = false;
        }

        if ("else".equals(ruleText) && "}".equals(prevRuleText)) {
            lineBreak = false;
            w.append(" ");
        }

        if (lineBreak) {
            w.append('\n').append(getIndent());
//        } else if (IdentifierExprUnparser.
//                matchIdentifierExprAlt0(prevRuleText + ruleText)
//                && !ruleText.equals(";") && !ruleText.equals(")") && !ruleText.equals("(")
//                && !ruleText.equals(",")
//                || prevRuleText.equals(",")
//                || prevRuleText.equals("]")
//                || prevRuleText.equals("(")
//                || ruleText.equals(")")
//                || ruleText.equals("{")
//                || prevRuleText.equals("=")
//                ) {
//            w.append(" ");
        } else if (
                !(getPrevRuleInfo().getParentObject() instanceof QualifiedName)
                        && !(ruleInfo.getParentObject() instanceof PackageDeclaration)
                ) {
            w.append(" ");
        }

        if (prevRuleText.startsWith("//") && prevRuleText.endsWith("\n")) {
            w.append(getIndent());
        }

        super.pre(unparser, ruleInfo, w);

    }

    private boolean insideFor(String prevRuleText) {
        if (prevRuleText.startsWith("for")) {
            setBoolState("for-started", true);
        }

        if (getBoolState("for-started")) {
            String stringState = getStringState("for-;");
            if (stringState == null) {
                stringState = "";
            }
            if (stringState.length() == 2) {
                setStringState("for-;", "");
                setBoolState("for-started", false);
                return false;
            }
            if (";".equals(prevRuleText)) {
                setStringState("for-;", stringState + ";");
            }
        }
        return getBoolState("for-started");
    }

}

