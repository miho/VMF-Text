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
package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.core.TypeUtil;
import eu.mihosoft.vmf.core.io.Resource;
import eu.mihosoft.vmf.core.io.ResourceSet;
import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.vmftext.TemplateEngine;
import eu.mihosoft.vmf.vmftext.grammar.*;
import org.apache.velocity.VelocityContext;

import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class UnparserCodeGenerator {

    public static void generateUnparser(GrammarModel gModel, ReadOnlyUnparserModel roModel,
                                        String unparserGrammarPath, ResourceSet resourceSet) {

        // ensure we work on our own modifiable copy of the model
        UnparserModel model = roModel.asModifiable();

        List<UPRule> rules = computeFinalUnparserRuleList(model);

        try (Resource resource =
                     resourceSet.open(TypeUtil.computeFileNameFromJavaFQN(
                             gModel.getPackageName()+".unparser."+gModel.getGrammarName() + "ModelUnparser"));

             Writer w = resource.open()) {

            generateUPParentUnparserCode(gModel, model,rules, w);

        } catch (IOException e) {
            e.printStackTrace();
        }

        generateUPCode(gModel, model,rules, resourceSet);

        try (Resource resource =
                     resourceSet.open(unparserGrammarPath);
             Writer w = resource.open()) {

            generateUPGrammarCode(gModel, model, rules, w);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateUPGrammarCode(GrammarModel gModel, UnparserModel model,
                                              List<UPRule> rules, Writer w) throws IOException {
        w.append("grammar ").append(gModel.getGrammarName()+"ModelUnparserGrammar;").append('\n');
        w.append("import ").append(gModel.getGrammarName()).append(";\n");
        w.append('\n');

        for(UPRule rule :rules) {
            String ruleName = StringUtil.firstToLower(rule.getName());
            generateAltGrammarCode(gModel, model, rules, rule, ruleName, w);
        }
    }

    private static void generateAltGrammarCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules,
                                               UPRuleBase rule, String parentName, Writer w) throws IOException {
        for(AlternativeBase a : rule.getAlternatives()) {
            String aName = parentName + "Alt"+a.getAltId();

            // filter alt name elements (have to be removed to produce a valid grammar)
            String aText = a.getElements().stream().filter(e->!e.getText().
                    startsWith("#")).map(e->e.getText() + " ").collect(Collectors.joining());

            w.append(aName+": ").append(aText + " " + (aText.endsWith("EOF")?"":"EOF")+" ;\n");

            generateElementGrammarCode(gModel,model,rules,a,aName,w);
        }
    }

    private static void generateElementGrammarCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules,
                                                   AlternativeBase alt, String parentName, Writer w) throws IOException {
        for(UPElement e : alt.getElements()) {
            if(e instanceof SubRule) {
                SubRule sr = (SubRule) e;
                String srName = parentName+"SubRule"+sr.getRuleId();

                String eText = e.getText(); // TODO 27.12.2017 maybe simplify expression by removing all labels and substitute sub-rules?

                w.append(srName+": ").append(eText+";\n");

                generateAltGrammarCode(gModel, model, rules, sr, srName, w);
            }
        }
    }

    private static List<UPRule> computeFinalUnparserRuleList(UnparserModel model) {
        // convert labeled alts to rule classes
        Map<String,List<LabeledAlternative>> labeledAlternatives = model.getRules().stream().
                flatMap(r->r.getAlternatives().stream()).filter(a->a instanceof LabeledAlternative).
                map(a->(LabeledAlternative)a).collect(Collectors.groupingBy(LabeledAlternative::getName));

        // add all directly specified rules
        List<UPRule> rules = new ArrayList(model.getRules());

        // find parents of labeled alternatives
        List<UPRuleBase> delList = labeledAlternatives.values().stream().
                flatMap(las->las.stream()).map(la->la.getParentRule()).collect(Collectors.toList());

        // and remove them from unparser generation since they are interface-only types and thus
        // don't need/support direct implementations
        rules.removeAll(delList);

        // convert labeled alternatives to rules (which are added to rules list)
        labeledAlternatives.values().stream().
                map(la->labeledAltToRule(la.get(0).getName(),la)).collect(Collectors.toCollection(()->rules));
        return rules;
    }

    private static List<UPRule> computeParentsOfLabeledAlts(UnparserModel model) {
        // convert labeled alts to rule classes
        Map<String,List<LabeledAlternative>> labeledAlternatives = model.getRules().stream().
                flatMap(r->r.getAlternatives().stream()).filter(a->a instanceof LabeledAlternative).
                map(a->(LabeledAlternative)a).collect(Collectors.groupingBy(LabeledAlternative::getName));


        // find parents of labeled alternatives
        List<UPRule> parentsOfLabeledAlts = labeledAlternatives.values().stream().
                flatMap(las->las.stream()).map(la->(UPRule)la.getParentRule()).distinct().
                collect(Collectors.toList());

        return parentsOfLabeledAlts;
    }

    private static List<UPRule> computeRulesThatHaveSpecifiedParent(UnparserModel model, UPRule rParent) {

        // find children of specified rule
        List<LabeledAlternative> labeledAlternatives = rParent.getAlternatives().stream().
                filter(a->a instanceof LabeledAlternative).
                map(a->(LabeledAlternative)a).collect(Collectors.toList());


        // convert labeled alts to rule classes
        List<UPRule> children = labeledAlternatives.stream().
                 map(la->labeledAltToRule(la.getName(),
                         new ArrayList<LabeledAlternative>(Arrays.asList(la)))).
                collect(Collectors.toList());

        return children;
    }

    private static void generateUPParentUnparserCode(GrammarModel gModel, UnparserModel model,
                                                     List<UPRule> rules, Writer w) throws IOException {

        w.append("package " + gModel.getPackageName()+".unparser;").append('\n').append('\n');

        w.append("// Java API imports").append('\n');
        w.append("import java.io.UnsupportedEncodingException;").append('\n');
        w.append("import java.io.ByteArrayOutputStream;").append('\n');
        w.append("import java.io.Writer;").append('\n');
        w.append("import java.io.PrintWriter;").append('\n').append('\n');
        w.append("import java.util.function.BiFunction;").append('\n');

        w.append("// Model API imports").append('\n');
        w.append("import "+gModel.getPackageName()+"." + gModel.getGrammarName() + "Model;").append('\n').append('\n');
        w.append("import " + gModel.getPackageName() + ".CodeElement;").append('\n');

        w.append("// rule imports (from model api)").append('\n');

        w.append("import " + gModel.getPackageName() + ".*;").append('\n');

        // We use star-import instead of importing each rule individually
//        for (UPRule rImport : rules) {
//
//            String ruleImportName = StringUtil.firstToUpper(rImport.getName());
//
//            w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
//        }

        List<UPRule> parentsOfLabeledAlts = computeParentsOfLabeledAlts(model);
        w.append('\n');
        w.append("// alt parents imports (from model api)").append('\n');
        for (UPRule rImport : parentsOfLabeledAlts) {

            String ruleImportName = StringUtil.firstToUpper(rImport.getName());

            w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
        }

        w.append('\n');
        w.append("import " + gModel.getPackageName()+ ".unparser." +gModel.getGrammarName()+ "ModelUnparser.__VMF__IntValue;").append('\n');
        w.append('\n');
        w.append('\n');

        w.append("public class "+gModel.getGrammarName()+"ModelUnparser {").append('\n');

        w.append('\n');
        w.append("  // rule unparsers").append('\n');

        for (UPRule r : rules) {

            String ruleName = StringUtil.firstToUpper(r.getName());

            w.append("  private final " + ruleName + "Unparser " + StringUtil.firstToLower(ruleName) + "Unparser;").append('\n');
        }

        w.append('\n');
        w.append("  private Formatter formatter = Formatter.newDefaultFormatter();").append('\n');
        w.append("  public Formatter getFormatter() {return formatter;};").append('\n');
        w.append("  public void setFormatter(Formatter formatter) { if(formatter==null) {formatter = Formatter.newDefaultFormatter();} this.formatter = formatter; };").append('\n');
        w.append('\n');

        w.append("  public "+gModel.getGrammarName()+"ModelUnparser() {").append('\n');

        for (UPRule r : rules) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            w.append("    " + StringUtil.firstToLower(ruleName) + "Unparser = new " + ruleName + "Unparser(this);").append('\n');
        }

        w.append("  }").append('\n');

        String rootClassNameUpperCase = gModel.rootClass().nameWithUpper();
        String rootClassNameLowerCase = gModel.rootClass().nameWithLower();

        String rootClassUnparserName = rootClassNameLowerCase + "Unparser";

        w.append('\n');
        w.append("  public void unparse(" + gModel.getGrammarName()+"Model model, Writer w) {").append('\n');
        w.append("    "+rootClassUnparserName+".unparse(model.getRoot(), new PrintWriter(w));").append('\n');
        w.append("  }").append('\n');
        w.append('\n');
        w.append('\n');
        w.append("  public void unparse(" + gModel.getGrammarName()+"Model model, PrintWriter w) {").append('\n');
        w.append("    "+rootClassUnparserName+".unparse(model.getRoot(), w);").append('\n');
        w.append("  }").append('\n');
        w.append('\n');

        w.append('\n');
        w.append("  public String unparse(" + gModel.getGrammarName()+"Model model) {").append('\n');
        w.append("    ByteArrayOutputStream output = new ByteArrayOutputStream();\n" +
                 "    PrintWriter pw = new PrintWriter(output);\n" +
                 "    unparse(model,pw);\n" +
                 "    pw.close();\n" +
                 "    return output.toString();").append('\n');
        w.append("  }").append('\n');
        w.append('\n');

        for (UPRule r : parentsOfLabeledAlts) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            String ruleUnparserInstanceName = StringUtil.firstToLower(r.getName())+"Unparser";
            w.append('\n');
            w.append("  public String unparse(" + ruleName+" rule) {").append('\n');
            w.append("    ByteArrayOutputStream output = new ByteArrayOutputStream();\n" +
                     "    PrintWriter pw = new PrintWriter(output);\n" +
                     "    unparse(rule,pw);\n" +
                     "    pw.close();\n" +
                     "    return output.toString();\n"+
                     "  }").append('\n');
            w.append('\n');
            w.append("  public void unparse(" + ruleName + " rule, Writer w) throws java.io.IOException {").append('\n');

            List<UPRule> parents = computeRulesThatHaveSpecifiedParent(model, r);

            boolean first = true;
            for (UPRule r1 : parents) {
                String ruleName1 = StringUtil.firstToUpper(r1.getName());
                if(first) {
                    first=false;
                    w.append("    ");
                } else {
                    w.append(" else ");
                }
                w.append("if ( rule instanceof " + ruleName1 + " ) {").append('\n');
                w.append("      unparse( ("+ruleName1+")rule, w );").append('\n');
                w.append("    }");
            }

            w.append('\n');
            w.append("    w.flush();").append('\n');
            w.append("  }").append('\n');

            w.append('\n');
            w.append('\n');
            w.append("  public void unparse(" + ruleName + " rule, PrintWriter w) {").append('\n');

            first = true;
            for (UPRule r1 : parents) {
                String ruleName1 = StringUtil.firstToUpper(r1.getName());
                if(first) {
                    first=false;
                    w.append("    ");
                } else {
                    w.append(" else ");
                }
                w.append("if ( rule instanceof " + ruleName1 + " ) {").append('\n');
                w.append("      unparse( ("+ruleName1+")rule, w );").append('\n');
                w.append("    }");
            }

            w.append('\n');
            w.append("    w.flush();").append('\n');
            w.append("  }").append('\n');
            w.append('\n');
        }

        for (UPRule r : rules) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            String ruleUnparserInstanceName = StringUtil.firstToLower(r.getName())+"Unparser";
            w.append('\n');
            w.append("  public void unparse(" + ruleName + " rule, Writer w) {").append('\n');
            w.append("    "+ruleUnparserInstanceName+".unparse(rule, new PrintWriter(w));").append('\n');
            w.append("  }").append('\n');
            w.append('\n');
            w.append('\n');
            w.append("  public void unparse(" + ruleName + " rule, PrintWriter w) {").append('\n');
            w.append("    "+ruleUnparserInstanceName+".unparse(rule, w);").append('\n');
            w.append("  }").append('\n');
            w.append('\n');
            w.append("  public String unparse(" + ruleName+" rule) {").append('\n');
            w.append("    ByteArrayOutputStream output = new ByteArrayOutputStream();\n" +
                     "    PrintWriter pw = new PrintWriter(output);\n" +
                     "    unparse(rule,pw);\n" +
                     "    pw.close();\n" +
                     "    return output.toString();\n"+
                     "  }").append('\n');
            w.append('\n');
        }


        w.append("  public void unparse(CodeElement rule, Writer w) throws java.io.IOException {").append('\n');

        boolean first = true;
        for (UPRule r : rules) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            if(first) {
                first=false;
                w.append("    ");
            } else {
                w.append(" else ");
            }
            w.append("if ( rule instanceof " + ruleName + " ) {").append('\n');
            w.append("      unparse( ("+ruleName+")rule, w );").append('\n');
            w.append("    }");
        }


        w.append('\n');
        w.append("    w.flush();").append('\n');
        w.append("  }").append('\n');
        w.append("  public void unparse(CodeElement rule, PrintWriter w) {").append('\n');

        first = true;
        for (UPRule r : rules) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            if(first) {
                first=false;
                w.append("    ");
            } else {
                w.append(" else ");
            }
            w.append("if ( rule instanceof " + ruleName + " ) {").append('\n');
            w.append("      unparse( ("+ruleName+")rule, w );").append('\n');
            w.append("    }");
        }

        w.append('\n');

        w.append("    w.flush();").append('\n');

        w.append("  }").append('\n');
        w.append('\n');

        // Unparse methods for Strings

        w.append('\n');
        w.append("  /*package private*/ void unparse(String s, Writer w) throws java.io.IOException {").append('\n');
        w.append("    w.append(s==null?\"\":s);").append('\n');
        w.append("  }").append('\n');
        w.append('\n');
        w.append("  /*package private*/ void unparse(String s, PrintWriter w) {").append('\n');
        w.append("    w.print(s==null?\"\":s);").append('\n');
        w.append("  }").append('\n');
        w.append('\n');

        // State object classes

        w.append('\n');
        w.append(" static class __VMF__IntValue { private int value; public void set(int v) {this.value = v;} public int get() { return this.value; } int getAndInc() {int result = this.value;this.value++; return result;} }").append('\n');
        w.append('\n');
        w.append('\n');
        w.append(" static class __VMF__BoolValue { private boolean value; public void set(boolean v) {this.value = v;} public boolean is() { return this.value; } boolean getAndInvert() {boolean result = this.value;this.value=!this.value; return result;} }").append('\n');
        w.append('\n');
        w.append("} // end class").append('\n');
        w.append('\n');
    }

    private static void generateUPCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, ResourceSet resourceSet) {

        for (UPRule r : rules) {

            String ruleName = StringUtil.firstToUpper(r.getName());


            try (Resource resource =
                         resourceSet.open(TypeUtil.computeFileNameFromJavaFQN(
                                 gModel.getPackageName() + ".unparser." + ruleName + "Unparser"))) {

                Writer w = resource.open();

                w.append("package " + gModel.getPackageName()+".unparser;").append('\n').append('\n');

                w.append("// Java API imports").append('\n');
                w.append("import java.io.UnsupportedEncodingException;").append('\n');
                w.append("import java.io.ByteArrayOutputStream;").append('\n');
                w.append("import java.io.Writer;").append('\n');
                w.append("import java.io.PrintWriter;").append('\n');
                w.append("import java.util.Deque;").append('\n');
                w.append("import java.util.ArrayDeque;").append('\n').append('\n');

                w.append("// ANTLR4 imports").append('\n');
                w.append("import org.antlr.v4.runtime.BailErrorStrategy;").append('\n');
                w.append("import org.antlr.v4.runtime.CharStream;").append('\n');
                w.append("import org.antlr.v4.runtime.CharStreams;").append('\n');
                w.append("import org.antlr.v4.runtime.ParserRuleContext;").append('\n');
                w.append("import org.antlr.v4.runtime.CommonTokenStream;").append('\n');
                w.append("import org.antlr.v4.runtime.tree.ErrorNode;").append('\n');
                w.append("import org.antlr.v4.runtime.tree.ParseTreeWalker;").append('\n').append('\n');

                w.append("// ANTLR4 generated parser imports").append('\n');
                w.append("import "+ gModel.getPackageName()+ ".unparser.antlr4."+gModel.getGrammarName()+"ModelUnparserGrammarLexer;").append('\n');
                w.append("import "+ gModel.getPackageName()+ ".unparser.antlr4."+gModel.getGrammarName()+"ModelUnparserGrammarParser;").append('\n');
                w.append("import "+ gModel.getPackageName()+ ".unparser.antlr4."+gModel.getGrammarName()+"ModelUnparserGrammarBaseListener;").append('\n').append('\n');

                w.append("// Model API imports").append('\n');
                w.append("import "+gModel.getPackageName()+"." + gModel.getGrammarName() + "Model;").append('\n').append('\n');

                w.append('\n');
                w.append("import " + gModel.getPackageName()+ ".unparser." +gModel.getGrammarName()+ "ModelUnparser.__VMF__IntValue;").append('\n');
                w.append("import " + gModel.getPackageName()+ ".unparser." +gModel.getGrammarName()+ "ModelUnparser.__VMF__BoolValue;").append('\n');
                w.append('\n');
                w.append('\n');

                w.append("// rule imports (from model api)").append('\n');

                w.append("import " + gModel.getPackageName() + ".*;").append('\n');


//                for (UPRule rImport : rules) {
//
//                    String ruleImportName = StringUtil.firstToUpper(rImport.getName());
//
//                    w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
//                }

                w.append('\n');

                w.append("/*package private*/ public class " + ruleName + "Unparser {").append('\n');

                // find rule
                RuleClass gRule = gModel.getRuleClasses().stream().filter(
                        gRcl -> Objects.equals(StringUtil.firstToUpper(r.getName()), gRcl.nameWithUpper())).findFirst().get();

                w.append('\n');
                w.append("  // begin declare list property indices/iterators").append('\n');

                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("  __VMF__IntValue prop" + prop.nameWithUpper() + "ListIndex = new __VMF__IntValue();").append('\n');
                    w.append("  final Deque<__VMF__IntValue> prop"+ prop.nameWithUpper() + "State = new ArrayDeque<>();").append('\n');
                }

                w.append("  // end   declare list property indices/iterators").append('\n');

                w.append('\n');
                w.append('\n');
                w.append("  // begin declare property usage flags").append('\n');

                for (Property prop : gRule.getProperties()) {

                    if (prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("  __VMF__BoolValue prop" + prop.nameWithUpper() + "Used = new __VMF__BoolValue();").append('\n');
                    w.append("  final Deque<__VMF__BoolValue> prop"+ prop.nameWithUpper() + "State = new ArrayDeque<>();").append('\n');
                }

                w.append("  // end   declare property usage flags").append('\n');

                w.append('\n');

                w.append("  private final " + gModel.getGrammarName() + "ModelUnparser unparser;").append('\n');

                w.append('\n');

                w.append("  private void pushState() {").append('\n');
                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("    prop"+ prop.nameWithUpper() + "State.push( prop" + prop.nameWithUpper() + "ListIndex );").append('\n');

                }

                for (Property prop : gRule.getProperties()) {

                    if (prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("    prop"+ prop.nameWithUpper() + "State.push( prop" + prop.nameWithUpper() + "Used );").append('\n');

                }

                w.append("  }").append('\n');

                w.append("  private void popState() {").append('\n');
                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("    prop" + prop.nameWithUpper() + "ListIndex = prop"+ prop.nameWithUpper() + "State.pop();").append('\n');
                }
                for (Property prop : gRule.getProperties()) {

                    if (prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("    prop" + prop.nameWithUpper() + "Used = prop"+ prop.nameWithUpper() + "State.pop();").append('\n');
                }
                w.append("  }").append('\n');

                w.append('\n');

                w.append("  /*package private*/ " + ruleName + "Unparser(" + gModel.getGrammarName() + "ModelUnparser unparser" + ") {").append('\n');
                w.append("    this.unparser = unparser;").append('\n');
                w.append("  }").append('\n');
                w.append('\n');

                w.append("  private " + gModel.getGrammarName() + "ModelUnparser getUnparser() { return this.unparser; }").append('\n').append('\n');

                w.append("  public void " + "unparse(" + ruleName + " obj, PrintWriter w ) {").append('\n');
                w.append('\n');
                w.append("    // ignore null objects").append('\n');
                w.append("    if(obj==null) return;").append('\n');
                w.append('\n');
                w.append("    pushState();").append('\n');
                w.append('\n');
                w.append("    // begin reset list property indices/iterators").append('\n');

                for (Property prop : gRule.getProperties()) {

                    if (prop.getType().isArrayType()) {
                        w.append("    prop" + prop.nameWithUpper() + "ListIndex = new __VMF__IntValue();").append('\n');
                    }
                }
                w.append("    // end   reset list property indices/iterators").append('\n');
                w.append('\n');

                w.append("    // begin reset property usage flags").append('\n');
                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        w.append("    prop" + prop.nameWithUpper() + "Used = new __VMF__BoolValue();").append('\n');
                    }
                }
                w.append("    // end   reset property usage flags").append('\n');
                w.append('\n');

                w.append("    // try to unparse alternatives of this rule").append('\n');

                // optimize alts (check the one with most elements first)
                List<AlternativeBase> alts = new ArrayList<>(r.getAlternatives());
                //sortAltsMostElementsFirst(alts, rules);

                for (AlternativeBase a : alts) {

                    String altName = ruleName + "Alt" + a.getAltId();

                    w.append("    if( unparse" + altName + "( obj, w ) ) { popState(); getUnparser().getFormatter().done(obj,true,w); return; }").append('\n').append('\n');

                }
                w.append("    // TODO: 29.12.2017 introduce unparser error handler etc.").append('\n');
                w.append("    getUnparser().getFormatter().done(obj,false,w);").append("\n");
                w.append("    throw new RuntimeException(\"Cannot unparse rule '" + ruleName + "'. Language model is invalid!\");").append('\n');
                w.append("    // popState();").append('\n').append('\n');
                w.append("  } // end unparse").append('\n');


                int altIndex = 0;
                for (AlternativeBase a : r.getAlternatives()) {
                    altIndex++;
                    boolean noCheck = altIndex >= r.getAlternatives().size();

                    // TODO 06.01.2018 introduce switch for enabling/disabling full validation
                    // - remove false&& below to remove expensive checks from last alternative to speed up performance
                    // - do this only if you KNOW that your model is VALID
                    // generateAltCode(w, model, gModel, r, gRule, ruleName, ruleName, a,
                    //        false&&noCheck);
                    //
                    // [UPDATE:] 22.06.2018 we can go even further and skip match... calls in almost all cases
                    // (only cases where properties are used in multiple alts need to be resolved via match...)
                    generateAltCode(w, model, gModel, r, gRule, ruleName, ruleName, a,
                            true, r.getAlternatives().size());
                }

                w.append("}").append('\n').append('\n');
            }  catch(IOException ex) {
                ex.printStackTrace();
            }
        } // end for each rule
    }


    private static UPRule labeledAltToRule(String name, List<LabeledAlternative> la) {
        UPRule r = UPRule.newBuilder().withName(name).build();

        List<Alternative> alts = la.stream().map(UnparserCodeGenerator::labeledAltToAlt).collect(Collectors.toList());
        r.getAlternatives().addAll(alts);

        return r;
    }

    private static Alternative labeledAltToAlt(LabeledAlternative la) {
        Alternative a = Alternative.newBuilder().applyFrom(la).build();
        return a;
    }

    private static void generateSubRuleCode(String ruleName, String altName, String objName, SubRule sr, List<UPRule> rules, RuleClass gRule, Writer w) throws IOException {

        w.append('\n');
        w.append("  private boolean unparse" + altName + "SubRule" + sr.getRuleId() + "( " + objName + " obj, PrintWriter w ) { boolean valid = false;").append('\n');

        boolean multiplierCase = false;
        if(sr instanceof UPElement) {
            UPElement ruleElement = (UPElement) sr;
            if(ruleElement.ebnfZeroMany() || ruleElement.ebnfOneMany()) {
                multiplierCase = true;
            }
        }

        if(multiplierCase) {
            generateAltCodeForSubRulesWithMultiplier(altName, sr, rules, gRule, w);
        } else {

            // optimize alts (check the one with most elements first)
            List<AlternativeBase> alts = new ArrayList<>(sr.getAlternatives());
            //sortAltsMostElementsFirst(alts, rules);

            for(AlternativeBase a : sr.getAlternatives()) {

                String altNameSub = altName + "SubRule" + sr.getRuleId() + "Alt" + a.getAltId();

                w.append("    if( unparse" + altNameSub + "( obj, w ) ) { return true; }").append('\n');
            }
        }

        w.append("  return valid;}").append('\n');
    }

    private static int getTotalNumberOfElementsInAlt(AlternativeBase a, List<UPRule> rules) {

        // TODO: 06.01.2018 does not fully work (some rule references, e.g., labeled rules cannot be resolved).

        int eCount = 0;
        for(UPElement e : a.getElements()) {
            if(e instanceof SubRule) {
                for(AlternativeBase subAlt : ((SubRule)e).getAlternatives()) {
                    eCount += getTotalNumberOfElementsInAlt(subAlt, rules);
                }
            } else if(e.isParserRule()) {
                Optional<UPRule> ruleOpt = rules.stream().filter(
                        r->r.getName().equals(StringUtil.firstToUpper(e.getRuleName()))).findFirst();

                if(ruleOpt.isPresent()) {
                    for(AlternativeBase ruleAlt : ruleOpt.get().getAlternatives()) {
                        eCount += getTotalNumberOfElementsInAlt(ruleAlt, rules);
                    }
                } else {
                    eCount++;
                }
            } else {
                eCount++;
            }
        }

        return eCount;
    }


    /**
     * Sorts the specified alternative list according to the total number of elements matched by the alternatives. This can
     * improve unparsing performance. BUT: it does not work if the grammar contains ambiguities (will change the result!).
     * Currently (as of 06.01.2018) we think it is better to leave this kind of optimization to the developer of the grammar.
     * But we will keep this functionality for future benchmarks.
     * @param alts
     * @param rules
     */
    private static void sortAltsMostElementsFirst(List<AlternativeBase> alts, List<UPRule> rules) {

        System.out.println("BEFORE:");
        alts.forEach(alternativeBase -> {
            System.out.println("a: " + "#elements: "  + getTotalNumberOfElementsInAlt(alternativeBase, rules) + " " + alternativeBase.getText());
        });

        Collections.sort(alts, (a1, a2) -> Integer.compare(getTotalNumberOfElementsInAlt(a2, rules),getTotalNumberOfElementsInAlt(a1, rules)));

        System.out.println("AFTER:");
        alts.forEach(alternativeBase -> {
            System.out.println("a: " + "#elements: "  + getTotalNumberOfElementsInAlt(alternativeBase, rules) + " " + alternativeBase.getText());
        });
    }

    private static void generateAltCodeForSubRulesWithMultiplier(String altName, SubRule sr, List<UPRule> rules, RuleClass gRule, Writer w) throws IOException {
        w.append('\n');
        w.append("    // begin declaring can-consume variables").append('\n');
        for(AlternativeBase a : sr.getAlternatives()) {
            String altNameSub = altName + "SubRule" + sr.getRuleId() + "Alt" + a.getAltId();
            String canConsumeVarName = StringUtil.firstToLower(altNameSub + "CanConsume");
            w.append("    boolean " + canConsumeVarName + " = true;").append('\n');
        }
        w.append("    // end declaring can-consume variables").append('\n');
        w.append('\n');
        w.append("    // begin handling sub-rule with zeroToMany or oneToMany").append('\n');
        w.append("    while(true) { ").append('\n');
        w.append("      boolean matchedAnyAlt = false;").append('\n');

        boolean first = true;
        String elseStr = "";

        // optimize alts (check the one with most elements first)
        List<AlternativeBase> alts = new ArrayList<>(sr.getAlternatives());
        //sortAltsMostElementsFirst(alts, rules);

        for(AlternativeBase a : alts) {

            String altNameSub = altName + "SubRule" + sr.getRuleId() + "Alt" + a.getAltId();

            String canConsumeVarName = StringUtil.firstToLower(altNameSub + "CanConsume");

            if(!first) {
                elseStr = " else";
            } else {
                first=false;
            }

            w.append("     " + elseStr + " if( "
                    + canConsumeVarName + " && unparse" + altNameSub + "( obj, w ) ) { valid = true;").append('\n').append('\n');
            w.append("        // We matched this alternative").append('\n');
            w.append("        matchedAnyAlt = true;").append('\n').append('\n');

            w.append("        // We unparsed this alt once. For unparsing multiple times there has to be something").append('\n');
            w.append("        // to be consumed/unparsed. See below...").append('\n');
            w.append("        " + canConsumeVarName + " = false;").append('\n').append('\n');

            List<Property> propertiesInAlt = getPropertiesUsedInAlt(gRule,a);

            if(propertiesInAlt.isEmpty()) {
                w.append("        // We don't unparse this alt again since there is nothing to consume (no properties).").append('\n').append('\n');
            } else {
                w.append("        // We check whether at least one of the rule properties/elements of list properties can").append('\n');
                w.append("        // be consumed/unparsed. If one of the checks below is positive we unparse again.").append('\n').append('\n');
            }

            // array properties
            for(Property p : propertiesInAlt) {
                if(p.getType().isArrayType()) {
                    w.append("        // check whether elements from list property '"+ p.nameWithLower() + "' can be consumed").append('\n');
                    w.append("        " +canConsumeVarName + " = " + canConsumeVarName + "\n" +
                             "          || prop" + p.nameWithUpper()+ "ListIndex.get() < obj.get" + p.nameWithUpper() +"().size();").append('\n');
                }
            }

            // non-array properties
            for(Property p : propertiesInAlt) {
                if(!p.getType().isArrayType()) {
                    w.append("        // check whether non-list property '"+ p.nameWithLower() + "' can be consumed").append('\n');
                    w.append("        " +canConsumeVarName + " = " + canConsumeVarName + "\n" +
                             "          || ( obj.get" + p.nameWithUpper() + "()!=null && !prop" + p.nameWithUpper()+ "Used.is());").append('\n');
                }
            }

            w.append("      }").append('\n');

        }

        w.append("      if( matchedAnyAlt == false ) break;").append('\n');
        w.append("    }").append('\n');
        w.append("    // end   handling sub-rule with zeroToMany or oneToMany").append('\n').append('\n');
    }

    private static void generateAltCode(Writer w, UnparserModel model, GrammarModel gModel, UPRule r, RuleClass gRule,
                                        String ruleName, String objName, AlternativeBase a, boolean noCheck, int numAltsPerRule) throws IOException {

        boolean userNoCheck = noCheck;
        boolean lastRuleAlt = a.getAltId() == numAltsPerRule-1;
        boolean negationOperatorUsedInAlt = propertiesOfAltUseNegateOperator(a, r, gRule);
        boolean propertiesUsedInMultipleAlts = propertiesUsedInMultipleRuleAlts(a, r, gRule);

        // we do need to check cases where properties are used in multiple alts of the current rule to ensure we
        // do a valid unparse
        if(noCheck == true) {
            if(lastRuleAlt) {
                // if we are the last rule we never need a check
            } else {
                // if not, it depends...
                noCheck = !propertiesUsedInMultipleAlts && !negationOperatorUsedInAlt;
            }
        }
        w.append('\n');
        w.append("  // ").append('\n');
        w.append("  //  --------------------------------------------------------------------------------").append('\n');
        w.append("  //  -- FLAGS:").append('\n');
        w.append("  //  --------------------------------------------------------------------------------").append('\n');
        w.append("  // ").append('\n');
        w.append("  //    ------------------------------------------------------------------------------").append('\n');
        w.append("  //    -- rule and noCheck info:").append('\n');
        w.append("  //    ------------------------------------------------------------------------------").append('\n');
        w.append("  //      -> rule-alt-text:         ").append(a.getText().replace('\n',' ').
                                                                            replace('\r',' ')).append('\n');
        w.append("  //      -> no-check:              " + userNoCheck).append('\n');
        w.append("  //    ------------------------------------------------------------------------------").append('\n');
        w.append("  //    -- properties which determine whether we can unparse without matchAlt-calls:").append('\n');
        w.append("  //    ------------------------------------------------------------------------------").append('\n');
        w.append("  //      -> no-operator:           " + negationOperatorUsedInAlt).append('\n');
        w.append("  //      -> used-in-multiple-alts: " + propertiesUsedInMultipleAlts).append('\n');
        w.append("  //      -> last-rule-alt:         " + lastRuleAlt).append('\n');
        w.append("  //").append('\n');
        w.append("  //  --------------------------------------------------------------------------------").append('\n');
        w.append("  //  -- EVALUATION:").append('\n');
        w.append("  //  --------------------------------------------------------------------------------").append('\n');
        w.append("  // ").append('\n');
        if(userNoCheck==false) {
            w.append("  //    noCheck is disabled which forces us to do all checks.").append('\n');
            w.append("  //    FIXME: TODO: consider disabling checks and do full validation prior to unparsing.").append('\n');
            w.append("  //    FIXME: TODO: this code will run up to ~70 times slower than with noCheck=true").append('\n');
        } else if(lastRuleAlt) {
            w.append("  //    We are the last alt in this rule and don't do any checks (matchAlt-calls)").append('\n');
            w.append("  //    since checking was not enforced.").append('\n');
        } else if(negationOperatorUsedInAlt) {
            w.append("  //    Negation operator '~' is used in this alt.").append('\n');
            w.append("  //    That's why we do checks (matchAlt-calls). Otherwise we can't make a valid decision.").append('\n');
            w.append("  //    FIXME: TODO: using the not-operator in parser-rule properties has a negative performance impact.").append('\n');
        } else if(propertiesUsedInMultipleAlts) {
            w.append("  //    Properties used in this alt are used in other alts (with different terminals/lexer rules).").append('\n');
            w.append("  //    That's why we do checks (matchAlt-calls). Otherwise we can't make a valid decision.").append('\n');
            w.append("  //    FIXME: TODO: using properties with different lexer rules in multiple alts has a negative performance impact.").append('\n');
        } else {
            w.append("  //    Well done! Nothing prevents us from skipping checks (matchAlt-calls). We can decide by checking").append('\n');
            w.append("  //    whether properties in this alt are consumable and/or if properties used in other alts").append('\n');
            w.append("  //    are defined etc.").append('\n');
        }
        w.append("  // ");
        String altName = ruleName + "Alt" + a.getAltId();
        w.append('\n');
        w.append("  private boolean unparse"+ altName + "( " + objName + " obj, PrintWriter w ) {").append('\n');

        w.append('\n');
        w.append("    if(obj==null) return false;").append('\n');
        w.append('\n');

        w.append("    // begin check whether unused properties are set").append('\n');

        // alternatives of grammar rules need to check whether they use all properties that are set and do
        // not use any unset property (only they do, no sub-rules, hence the if statement)
        if(a.getParentRule() == r) {
            generateUnusedPropertiesCheck(a, r, w);
        }

        w.append("    // end   check whether unused properties are set").append('\n');

        w.append('\n');

        w.append("    // begin check whether non-optional properties are available (not used/consumed)").append('\n');
        // TODO 21.01.2018 enable if we want to avoid unnecessary matchAlt... calls but still do validation
        generateConsumedPropertiesCheck(a, altName, r, gRule, w);
        w.append("    // end   check whether non-optional properties are available (not used/consumed)").append('\n');
        w.append('\n');

        if(!noCheck) {
            w.append("    getUnparser().getFormatter().pushState();").append('\n');
        }

        if(noCheck) {
            w.append("    PrintWriter internalW = w;").append('\n');
        } else {

            w.append("    ByteArrayOutputStream output = new ByteArrayOutputStream();").append('\n');
            w.append("    PrintWriter internalW = new PrintWriter(output);").append('\n');

        }

        w.append('\n');
        w.append("    // begin preparing local list indices/iterators").append('\n');

        for(Property prop : gRule.getProperties()) {
            if(!prop.getType().isArrayType()) {
                continue;
            }
            w.append("    int prevProp" + prop.nameWithUpper() + "ListIndex = prop"
                    + prop.nameWithUpper() + "ListIndex.get();").append('\n');
        }

        w.append("    // end   preparing local list indices/iterators").append('\n');

        w.append('\n');
        w.append("    // begin preparing local property usage flags").append('\n');

        for(Property prop : gRule.getProperties()) {
            if(prop.getType().isArrayType()) {
                continue;
            }
            w.append("    boolean prevProp" + prop.nameWithUpper() + "Used = prop"
                    + prop.nameWithUpper() + "Used.is();").append('\n');
        }

        w.append("    // end   preparing local property usage flags").append('\n');

        generateElements(model, gModel, w, gRule, r, a, altName, noCheck);

        if(noCheck) {
            w.append("    return true;");
            w.append("\n  }").append('\n').append('\n');
        } else {
            w.append('\n');
            w.append("    internalW.close();");
            w.append('\n');


            w.append("\n    String s;").append('\n');

            w.append("    try {").append('\n');
            w.append("        s = output.toString(\"UTF-8\");").append('\n');
            w.append("    } catch(UnsupportedEncodingException ex) {").append('\n');
            w.append("        s = output.toString();").append("\n");
            w.append("        ex.printStackTrace();").append('\n');
            w.append("    }").append('\n');

            w.append("\n    if( match" + altName + "(s) ) {").append('\n');
            w.append("        w.print(s /*+ \" \"*/);").append('\n');

            w.append('\n');
            w.append("        getUnparser().getFormatter().acceptState();").append('\n');
            w.append('\n');
            w.append("        return true;").append('\n');
            w.append("    } else {").append('\n');
            w.append('\n');
            generateRejectStateCode("    ",gRule, altName, noCheck, w);
            w.append('\n');
            w.append("      return false;").append('\n');
            w.append("    }").append('\n');
            w.append("\n  }").append('\n').append('\n');

        } // end if !noCheck

        generateMatchAltMethod(gModel, altName, w);

        final boolean noCheckFinal = noCheck;
        a.getElements().stream().filter(el->el instanceof SubRule).filter(el->!(el instanceof UPNamedSubRuleElement)).
                map(el->(SubRule)el).forEach(sr-> {

            for(AlternativeBase sa : sr.getAlternatives()) {
                try {
                    generateAltCode(w, model, gModel, r, gRule, altName + "SubRule" + sr.getRuleId(), objName, sa, /*we check based on parent alts preferences*/noCheckFinal,sr.getAlternatives().size());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                generateSubRuleCode(ruleName, altName, objName, sr, model.getRules(), gRule, w);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void generateRejectStateCode(String indent, RuleClass gRule, String altName, boolean noCheck, Writer w) throws IOException {
        w.append(indent + "// begin revert global list indices/iterators since we didn't consume this alt").append('\n');

        for (Property prop : gRule.getProperties()) {
            if (!prop.getType().isArrayType()) {
                continue;
            }
            w.append(indent + "prop" + prop.nameWithUpper() + "ListIndex.set(prevProp" + prop.nameWithUpper() + "ListIndex);").append('\n');
        }
        w.append(indent + "// end   revert global list indices/iterators since we didn't consume this alt").append('\n');
        w.append('\n');
        w.append('\n');
        w.append(indent + "// begin revert global property usage flags since we didn't consume this alt").append('\n');

        for (Property prop : gRule.getProperties()) {
            if (prop.getType().isArrayType()) {
                continue;
            }
            w.append(indent + "prop" + prop.nameWithUpper() + "Used.set(prevProp" + prop.nameWithUpper() + "Used);").append('\n');
        }
        w.append(indent + "// end   update global property usage flags since we consume this alt").append('\n');
        w.append('\n');
        if(!noCheck) {
            w.append(indent + "getUnparser().getFormatter().rejectState();").append('\n');
        }
    }

    private static void _getNamesOfPropertiesUsedInAlternative(AlternativeBase a, List<String> propertyNames, Map<String, Boolean> listTypeMap) {
        for(UPElement e : a.getElements()) {
            if(e instanceof WithName) {
                String pName = ((WithName)e).getName();
                propertyNames.add(pName);
                if(listTypeMap!=null) {
                    listTypeMap.put(pName, e.isListType());
                }
            }

            if(e instanceof SubRule) {
                SubRule sr = (SubRule) e;
                for(AlternativeBase subA : sr.getAlternatives()) {
                    _getNamesOfPropertiesUsedInAlternative(subA,propertyNames, listTypeMap);
                }
            }
        }
    }

    private static List<String> getNamesOfPropertiesUsedInAlternative(AlternativeBase a) {

        List<String> propertyNames = new ArrayList<>();

        _getNamesOfPropertiesUsedInAlternative(a, propertyNames, null);

        return propertyNames;
    }


    private static void _getPropertiesElementsUsedInAlternative(AlternativeBase a, List<UPElement> elements, boolean includeSubRules) {
        for(UPElement e : a.getElements()) {
            if(e instanceof WithName) {
                elements.add(e);
            }

            if(includeSubRules && e instanceof SubRule) {
                SubRule sr = (SubRule) e;
                for(AlternativeBase subA : sr.getAlternatives()) {
                    _getPropertiesElementsUsedInAlternative(subA,elements, includeSubRules);
                }
            }
        }
    }

    private static List<UPElement> getPropertiesElementsUsedInAlternative(AlternativeBase a, boolean includeSubRules) {

        List<UPElement> elements = new ArrayList<>();

        _getPropertiesElementsUsedInAlternative(a, elements, includeSubRules);

        return elements;
    }

    private static List<Property> getPropertiesUsedInSubRule(RuleClass parent, SubRule sr) {
        List<String> propNames = new ArrayList<>();

        for(AlternativeBase subA : sr.getAlternatives()) {
            propNames.addAll(getNamesOfPropertiesUsedInAlternative(subA));
        }

        return parent.getProperties().stream().filter(
                p->propNames.contains(p.nameWithLower())).collect(Collectors.toList());
    }

    private static List<Property> getPropertiesUsedInAlt(RuleClass parent, AlternativeBase a) {
        List<String> propNames = new ArrayList<>();

        propNames.addAll(getNamesOfPropertiesUsedInAlternative(a));

        return parent.getProperties().stream().filter(
                p->propNames.contains(p.nameWithLower())).collect(Collectors.toList());
    }


    private static void generateUnusedPropertiesCheck(AlternativeBase a, UPRule r, Writer w) throws IOException {
        List<String> propertyNamesUsed = getNamesOfPropertiesUsedInAlternative(a);
        Map<String,Boolean> propertyNamesInRuleWithListFlag = getPropertyNamesOfRule(r);

        List<String> propertiesNotUsedInAlt = new ArrayList<>();
        propertiesNotUsedInAlt.addAll(propertyNamesInRuleWithListFlag.keySet());
        propertiesNotUsedInAlt.removeAll(propertyNamesUsed);

        for(String pName : propertiesNotUsedInAlt) {

// TODO 19.06.2018 check for set/unset
//            if(propertyNamesInRuleWithListFlag.get(pName)) {
//                w.append("    if( !obj.get" + StringUtil.firstToUpper(pName) + "().isEmpty() ) return false;").append('\n');
//            } else {
//                w.append("    if( obj.get" + StringUtil.firstToUpper(pName) + "() !=null ) return false;").append('\n');
//            }

            w.append("    if( obj.vmf().reflect().propertyByName(\"" + StringUtil.firstToLower(pName) + "\").get().isSet() ) return false;").append('\n');
        }
    }

    private static void generateConsumedPropertiesCheck(AlternativeBase a, String aName, UPRule r, RuleClass gRule, Writer w) throws IOException {
        //List<Property> propertiesInAlt = getPropertiesUsedInAlt(gRule, a);

        List<UPElement> propertyElements = getPropertiesElementsUsedInAlternative(a, false);

        String canConsumeVarName = StringUtil.firstToLower(aName + "CanConsume");

        boolean hasArrayPropertiesThatNeedToBeChecked = propertyElements.stream().
                filter(p->p.isListType() && !(p.ebnfOptional() || p.ebnfZeroMany())).count() > 0;

        boolean hasNonArrayPropertiesThatNeedToBeChecked = propertyElements.stream().
                filter(p->!p.isListType() && !p.ebnfOptional()).count() > 0;

        boolean hasToCheck = hasArrayPropertiesThatNeedToBeChecked || hasNonArrayPropertiesThatNeedToBeChecked;

        if(hasToCheck) {
            w.append("        boolean " + canConsumeVarName + " = true;").append('\n');
        } else {
            w.append("        // no non-optional properties to check").append('\n');
        }

        // array properties
        for(UPElement p : propertyElements) {
            if(p.isListType() && !(p.ebnfOptional() || p.ebnfZeroMany())) {
                String pName = StringUtil.firstToLower(((WithName)p).getName());
                String pNameUpper = StringUtil.firstToUpper(((WithName)p).getName());

                w.append("        // check whether elements from list property '"+ pName + "' can be consumed").append('\n');
                w.append("        " +canConsumeVarName + " = " + canConsumeVarName + "\n" +
                        "          && prop" + pNameUpper+ "ListIndex.get() < obj.get" + pNameUpper +"().size();").append('\n');
            }
        }

        // non-array properties
        for(UPElement p : propertyElements) {
            if(!p.isListType() && !(p.ebnfOptional() || p.ebnfZeroMany())) {
                String pName = StringUtil.firstToLower(((WithName)p).getName());
                String pNameUpper = StringUtil.firstToUpper(((WithName)p).getName());

                w.append("        // check whether non-list property '"+ pName + "' can be consumed").append('\n');
                w.append("        " +canConsumeVarName + " = " + canConsumeVarName + "\n" +
                        "          && ( obj.get" + pNameUpper + "()!=null /*&& !prop" + pNameUpper+ "Used.is()*/);").append('\n');
            }
        }

        if(hasToCheck) {
            w.append("        if(!" + canConsumeVarName + ") return false;").append('\n');
        }
    }

    private static boolean propertiesOfAltUseNegateOperator(AlternativeBase a, UPRule r, RuleClass gRule) {

        // negation can only occur in lexer rules and terminal rules
        Stream<UPElement> propertyElemsUsedInAlt = getPropertiesElementsUsedInAlternative(a, true).
                stream().distinct();

        // check whether we find a rule that is negated
        return propertyElemsUsedInAlt.filter(e->e.isNegated()).count() > 0;
    }

    private static boolean propertiesUsedInMultipleRuleAlts(AlternativeBase a, UPRule r, RuleClass gRule) {
        // find overlapping properties, i.e., properties that are used in multiple alternatives of the same rule
        List<UPElement> rulePropertyElements = new ArrayList<>();
        for(AlternativeBase ruleAlt: r.getAlternatives()) {
            List<UPElement> propertyElemsUsedInAlt = getPropertiesElementsUsedInAlternative(ruleAlt, true).
                    stream().distinct().filter(e->e.isLexerRule() || e.isTerminal()).collect(Collectors.toList());

            // for each element we check whether to add the element to the list of property elements to count
            for(UPElement upElement : propertyElemsUsedInAlt) {
                String name = "";
                String referencedRuleName;

                if(upElement.isTerminal()) {
                    referencedRuleName = upElement.getText();
                } else {
                    referencedRuleName = upElement.getRuleName();
                }

                // add the element (if not already added)
                if(!rulePropertyElements.contains(upElement)) {
                    rulePropertyElements.add(upElement);
                }

                // filter all elements from the list that have the same name, reference a lexer rule or terminal
                // which is different from the current element (upElement).
                // we don't count parser-rule-elements. They have their own unparser class and do their checks there
                // and the same property cannot be of two different parser rule types
                List<UPElement> elementsToAdd = rulePropertyElements.stream().filter(e -> e instanceof WithName).
                        filter(n -> Objects.equals(((WithName)n).getName(), name)).
                        filter(n->!Objects.equals(n.isTerminal()?n.getText():n.getRuleName(),referencedRuleName)).
                        collect(Collectors.toList());

                // finally add the elements
                rulePropertyElements.addAll(elementsToAdd);
            }
        }

        // this map contains the properties and the number of occurrences
        Map<String, Integer> numberOfOccurrences = new HashMap<>();

        // do the counting
        for(Property p : gRule.getProperties()) {
            String pName = p.nameWithLower();

            int numOcc = (int)rulePropertyElements.stream().filter(e->e instanceof WithName).map(e->(WithName)e).
                    filter(n-> Objects.equals(pName,n.getName())).count();

            numberOfOccurrences.put(pName, numOcc);
        }

        // check whether for at least one property we have multiple occurrences (as explained above, we only check
        // lexer rule properties for now)
        boolean overlappingProperties = numberOfOccurrences.values().stream().filter(v->v > 1).count() > 0;

        return overlappingProperties;
    }

    private static Map<String,Boolean> getPropertyNamesOfRule(UPRule r) {
        List<String> propertyNames = new ArrayList<>();
        Map<String,Boolean> listType = new HashMap<>();

        for(AlternativeBase a : r.getAlternatives()) {
            _getNamesOfPropertiesUsedInAlternative(a, propertyNames, listType);
        }

        return listType;
    }

    private static TemplateEngine tEngine;

    private static void generateMatchAltMethod(GrammarModel gModel, String altName, Writer w) throws IOException {

        if(tEngine==null) {
            tEngine = new TemplateEngine();
        }

        TemplateEngine.Engine engine = tEngine.getEngine();
        VelocityContext context = engine.context;
        context.put("model", gModel);
        context.put("altName", altName);
        context.put("grammarName", gModel.getGrammarName());
        context.put("packageName", gModel.getPackageName());
        context.put("Util", StringUtil.class);

        tEngine.mergeTemplate("model-unparser-match-alt",w);
    }

    private static void generateElements(UnparserModel model, GrammarModel gModel, Writer w, RuleClass gRule,
                                         UPRule rule, AlternativeBase a, String altName, boolean noCheck) throws IOException {
        String indent = "";

        for(UPElement e : a.getElements()) {
            w.append('\n');
            if(e instanceof UPSubRuleElement) {
                generateSubRuleElementCode(w, altName, indent, (UPSubRuleElement) e, gRule, noCheck);
            } else if(e instanceof UPNamedSubRuleElement) {
                generateNamedSubRuleElementCode(w, indent, (UPNamedSubRuleElement) e, gRule, altName, noCheck);
            } else if(e instanceof UPNamedElement) {
                UPNamedElement sre = (UPNamedElement) e;
                generateNamedElementCode(w, indent, model, sre, rule, gRule, gModel, altName, noCheck);
            } else {
                generateUnnamedElementCode(model, w, indent, e);
            }
        }
    }

    private static void generateUnnamedElementCode(UnparserModel model, Writer w, String indent, UPElement e) throws IOException {

        // ignore EOF element
        if("EOF".equals(e.getText().trim())) {
            return;
        }

        // remove ebnf multiplicity, optional and greedy characters
        String eText = e.getText();
        eText = removeEBNFModifierFromElementText(eText);

        if(eText.startsWith("'")) {
            // terminal element
            // remove '
            eText = eText.substring(1,eText.length()-1);
            w.append(indent + "    // handling unnamed terminal element  '"+eText+"'").append('\n');
            String ruleString = StringUtil.escapeJavaStyleString(eText,true);

            String rulePath = UPRuleUtil.getPath(e);
            boolean unnamedRule = true;
            boolean optionalRule=e.ebnfOptional() || e.ebnfZeroMany();
            String grammarText = e.getText();
            String ruleInfoString = "Formatter.RuleInfo.newRuleInfo(obj, Formatter.RuleType.TERMINAL, null, \"" + ruleString + "\", \"" + rulePath + "\", " + unnamedRule + ", "+ optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText,false) + "\")";

            w.append(indent + "    getUnparser().getFormatter().pre( unparser, " + ruleInfoString + ", internalW);").append('\n');
            w.append(indent + "    // internalW.print( \""+StringUtil.escapeJavaStyleString(eText,true) + "\");").append('\n');
            w.append(indent + "    getUnparser().getFormatter().render( unparser, " + ruleInfoString + ", internalW, " + "\""+StringUtil.escapeJavaStyleString(eText,true) + "\");").append('\n');
            w.append(indent + "    getUnparser().getFormatter().post(unparser, " + ruleInfoString + ", internalW);").append('\n');
            return;
        } else if(Character.isUpperCase(eText.charAt(0))){
            // we are a lexer rule ref
            final String lexerRuleName = eText;

            Optional<UPLexerRule> lexerRuleOptional =
                    model.getLexerRules().stream().
                            filter(lr->Objects.equals(lr.getName(),lexerRuleName)).findFirst();


            boolean lexerRuleToTerminalPossible = lexerRuleOptional.isPresent();

            if(lexerRuleToTerminalPossible) {
                UPLexerRule lexerRule = lexerRuleOptional.get();

                String lexerRuleString = removeEBNFModifierFromElementText(lexerRule.getText());

                if(lexerRuleString.startsWith("'")) {
                    // terminal element
                    // remove '
                    // TODO 18.01.2018 improve terminal extraction from lexer rule
                    lexerRuleString = lexerRuleString.substring(1, lexerRuleString.length() - 1);
                    lexerRuleString = lexerRuleString.replaceAll("'\\s*'","");

                    w.append(indent + "    // handling unnamed lexer rule ref '" + eText + "'").append('\n');
                    w.append(indent + "    // we could successfully find terminal text of the rule").append('\n');
                    String ruleName = eText;

                    String rulePath = UPRuleUtil.getPath(e);
                    boolean unnamedRule = true;
                    boolean optionalRule=e.ebnfOptional() || e.ebnfZeroMany();
                    String grammarText = e.getText();
                    String ruleInfoString = "Formatter.RuleInfo.newRuleInfo(obj, Formatter.RuleType.LEXER_RULE, \"" + ruleName + "\", \"" + StringUtil.escapeJavaStyleString(lexerRuleString, true) + "\", \"" + rulePath + "\", " + unnamedRule + ", "+ optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText,false) + "\")";
                    // String ruleInfoString = "Formatter.RuleInfo.newRuleInfo(obj, Formatter.RuleType.LEXER_RULE, \"" + ruleName + "\", \"" + StringUtil.escapeJavaStyleString(lexerRuleString, true) + "\")";
                    w.append(indent + "    getUnparser().getFormatter().pre( unparser, " + ruleInfoString + ", internalW);").append('\n');
                    w.append(indent + "    // internalW.print( \"" + StringUtil.escapeJavaStyleString(lexerRuleString, true) + "\" /*+ \" \" */);").append('\n');
                    w.append(indent + "    getUnparser().getFormatter().render( unparser, " + ruleInfoString + ", internalW, " + "\""+StringUtil.escapeJavaStyleString(lexerRuleString,true) + "\");").append('\n');
                    w.append(indent + "    getUnparser().getFormatter().post(unparser, " + ruleInfoString + ", internalW);").append('\n');
                    return;
                }  else {
                    w.append(indent+"    // handling unnamed lexer rule ref '"+eText+"'").append('\n');
                    w.append(indent+"    // FIXME: cannot process rule since it is not terminal only (that's why we ignore it)").append('\n');
                    w.append(indent+"    // RULE-TEXT: " + lexerRuleString).append('\n');
                    w.append(indent+"    // TODO SOLUTION: specify a property name, e.g., 'myProperty = " +eText+ "'").append('\n');
                    w.append(indent+"    // getUnparser().getFormatter().pre( unparser, obj, \""+StringUtil.escapeJavaStyleString(eText,true)+"\", internalW);").append('\n');
                    w.append(indent+"    // internalW.print( \""+StringUtil.escapeJavaStyleString(eText,true) + "\" );").append('\n');
                    w.append(indent+"    // getUnparser().getFormatter().post(unparser, obj, \""+StringUtil.escapeJavaStyleString(eText,true)+"\", internalW);").append('\n');
                    return;
                }
            }
        }

        w.append(indent+"    // handling unrecognized element  '"+eText.replace('\n', ' ')+"'").append('\n');
        w.append(indent+"    // FIXME: cannot recognize element (that's why we ignore it)").append('\n');
        w.append(indent+"    // getUnparser().getFormatter().pre( unparser, obj, \""+StringUtil.escapeJavaStyleString(eText,true)+"\", internalW);").append('\n');
        w.append(indent+"    // internalW.print( \""+StringUtil.escapeJavaStyleString(eText,true) + "\" );").append('\n');
        w.append(indent+"    // getUnparser().getFormatter().post(unparser, obj, \""+StringUtil.escapeJavaStyleString(eText,true)+"\", internalW);").append('\n');

    }

    private static String removeEBNFModifierFromElementText(String eText) {
        if(eText.endsWith("?")) {
            eText = eText.substring(0,eText.length()-1);
        }
        if(eText.endsWith("*")) {
            eText = eText.substring(0,eText.length()-1);
        }
        if(eText.endsWith("+")) {
            eText = eText.substring(0,eText.length()-1);
        }
        if(eText.endsWith(")")) {
            // remove ( )
            eText = eText.substring(1,eText.length()-1);
        }
        return eText;
    }

    private static void generateNamedElementCode(Writer w, String indent, UnparserModel model, UPNamedElement sre,
                                                 UPRule rule, RuleClass gRule, GrammarModel gModel, String altName,
                                                 boolean noCheck) throws IOException {

        String lexerRuleName = sre.getRuleName()!=null?sre.getRuleName():"";

        w.append(indent+"    // handling element with name '"+sre.getName()+"'").append('\n');
        String ruleType = "/*FIXME: TYPE IS UNDEFINED! ruleText='" + sre.getText() + "' */";
        if(sre.isLexerRule()) {
            ruleType = "Formatter.RuleType.LEXER_RULE";
        } else if(sre.isTerminal()) {
            ruleType = "Formatter.RuleType.TERMINAL";
        }
        String ruleInfoString;

        if(sre.isListType()) {
            String indexName = "prop" + StringUtil.firstToUpper(sre.getName()) + "ListIndex";
            String propName = "obj.get" + StringUtil.firstToUpper(sre.getName()+"()");
            if(sre.ebnfOne()) {
                if(sre.ebnfOptional()) {
                    w.append(indent+"    if(" + indexName+ ".get()" +" < " +propName+ ".size() ) {").append('\n');
                    if(sre.isParserRule()) {
                        w.append(indent+"      " + sre.getRuleName() + " listElemObj = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc());").append('\n');
                        w.append(indent+"      getUnparser().unparse(listElemObj, internalW );").append('\n');
                    } else if(sre.isLexerRule()) {
                        w.append(indent+"      {").append('\n');
                        String targetTypeOfMapping = gModel.getTypeMappings().targetTypeNameOfMapping(rule.getName(), lexerRuleName);
                        boolean mappingExists = gModel.getTypeMappings().mappingByRuleNameExists(rule.getName(), lexerRuleName);
                        w.append(indent+"        " + targetTypeOfMapping + " listElemObj = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc());").append('\n');
                        w.append(indent+"        String s = TypeToStringConverterForRule"+ StringUtil.firstToUpper(rule.getName()) + ".convertToString" + (mappingExists?"ForRule"+lexerRuleName:"") + "( listElemObj )").append('\n');
                        w.append(indent+"        if(s!=null) {").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        //w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"            // internalW.print(s);").append('\n');
                        w.append(indent + "          getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    } else {
                        w.append(indent+"      String listElemObj = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName + ".getAndInc());").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"      Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", listElemObj /*TERMINAL String conversion*/, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        //w.append(indent+"      Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", listElemObj /*TERMINAL String conversion*/" + ")");

                        w.append(indent+"      getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"      if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"        // internalW.print(s);").append('\n');
                        w.append(indent+"        getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"      }").append('\n');
                        w.append(indent+"      getUnparser().getFormatter().post( unparser, ruleInfo, internalW);").append('\n');
                    }
                    w.append(indent + "    }").append('\n');
                } else {

                    String breakOrReturn = " /*non optional case*/ return false;";

                    w.append(indent+"    if(" + indexName + ".get()" +" > " +propName+ ".size() -1 || " + propName + ".isEmpty()) {").append('\n');
                    generateRejectStateCode(indent+"      ",gRule, altName, noCheck, w);
                    w.append(indent+"      " +breakOrReturn).append('\n');
                    w.append(indent+"    }").append('\n');
                    if(sre.isParserRule()) {
                        w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()), internalW );").append('\n');
                    } else if(sre.isLexerRule()) {
                        w.append(indent+"      {").append('\n');
                        boolean mappingExists = gModel.getTypeMappings().mappingByRuleNameExists(rule.getName(), lexerRuleName);
                        w.append(indent+"        String s = TypeToStringConverterForRule"+ StringUtil.firstToUpper(rule.getName()) + ".convertToString"+(mappingExists?"ForRule"+lexerRuleName:"")+"( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) );").append('\n');
                        w.append(indent+"        if(s!=null) {").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        // w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"            // internalW.print(s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    } else {
                        w.append(indent+"      {").append('\n');
                        w.append(indent+"        String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) /*TERMINAL String conversion*/;").append('\n');
                        w.append(indent+"        if(s !=null) {").append('\n');


                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", listElemObj.toString() /*TERMINAL String conversion*/, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        // w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", listElemObj.toString() /*TERMINAL String conversion*/" + ")");

                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"            // internalW.print(s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"      }").append('\n');
                    }

                }
            } else if (sre.ebnfOneMany() || sre.ebnfZeroMany()) {

                if(sre.ebnfOptional()||sre.ebnfZeroMany()) {
                    w.append(indent+"    while(" + indexName+ ".get()" +" < " +propName+ ".size() ) {").append('\n');
                    if(sre.isParserRule()) {
                        w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()), internalW );").append('\n');
                    } else if(sre.isLexerRule()) {
                        w.append(indent+"      {").append('\n');
                        boolean mappingExists = gModel.getTypeMappings().mappingByRuleNameExists(rule.getName(), lexerRuleName);
                        w.append(indent+"        String s = TypeToStringConverterForRule"+ StringUtil.firstToUpper(rule.getName()) + ".convertToString"+(mappingExists?"ForRule"+lexerRuleName:"")+"( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) );").append('\n');
                        w.append(indent+"        if(s!=null) {").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        // w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"            // internalW.print(s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    } else {
                        w.append(indent+"      {").append('\n');
                        w.append(indent+"        String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) /*TERMINAL String conversion*/;").append('\n');
                        w.append(indent+"        if(s!=null) {").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        // w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"            // internalW.print(s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    }
                    w.append(indent + "    }").append('\n');
                } else {
                    w.append(indent+"    boolean matched"+StringUtil.firstToUpper(sre.getName()) +" = false;").append('\n');
                    w.append(indent+"    while(" + indexName+ ".get()" +" < " +propName+ ".size() && !" + propName + ".isEmpty()) {").append('\n');
                    if(sre.isParserRule()) {
                        w.append(indent + "      matched"+StringUtil.firstToUpper(sre.getName()) +" = true;").append('\n');
                        w.append(indent + "      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()), internalW );").append('\n');
                    } else if(sre.isLexerRule()) {
                        w.append(indent+"        {").append('\n');
                        boolean mappingExists = gModel.getTypeMappings().mappingByRuleNameExists(rule.getName(), lexerRuleName);
                        w.append(indent+"          String s = TypeToStringConverterForRule"+ StringUtil.firstToUpper(rule.getName()) + ".convertToString"+(mappingExists?"ForRule"+lexerRuleName:"")+"( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) );").append('\n');
                        w.append(indent+"          if(s!=null) {").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"            Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        // w.append(indent+"            Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                        w.append(indent+"            getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"            if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"              // internalW.print(s);").append('\n');
                        w.append(indent+"              getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"            }").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"        }").append('\n');
                    } else {
                        w.append(indent+"        {").append('\n');
                        w.append(indent+"          String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()).toString() /*TERMINAL String conversion*/;").append('\n');
                        w.append(indent+"          if(s!=null) {").append('\n');

                        String rulePath = UPRuleUtil.getPath(sre);
                        boolean unnamedRule = false;
                        boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                        String grammarText = sre.getText();

                        w.append(indent+"            Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                        // w.append(indent+"            Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                        w.append(indent+"            getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"            if(!ruleInfo.isConsumed()) {").append('\n');
                        w.append(indent+"              // internalW.print(s);").append('\n');
                        w.append(indent+"              getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                        w.append(indent+"            }").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"        }").append('\n');
                    }
                    w.append(indent+"    }").append('\n');
                    w.append(indent+"    // we are in the non-optional case and return early if we didn't match").append('\n');
                    w.append(indent+"    if(!matched"+StringUtil.firstToUpper(sre.getName())+") { ").append('\n');
                    generateRejectStateCode("    ",gRule, altName, noCheck, w);
                    w.append(indent+"      return false;").append('\n');
                    w.append(indent+"    }").append('\n');
                }
            }

        } else {
            if(sre.isParserRule()) {
                w.append(indent+"    if(obj.get" + StringUtil.firstToUpper(sre.getName()) + "() !=null) {").append('\n');
                w.append(indent+"        getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "(), internalW );").append('\n');
                w.append(indent+"    }").append('\n');
            } else if(sre.isLexerRule()) {
                w.append(indent + "    if(!prop" + StringUtil.firstToUpper(sre.getName()) + "Used.is())").append('\n');
                w.append(indent + "    {").append('\n');
                w.append(indent + "      prop" + StringUtil.firstToUpper(sre.getName()) + "Used.set(true);").append('\n');
                boolean mappingExists = gModel.getTypeMappings().mappingByRuleNameExists(rule.getName(), lexerRuleName);
                w.append(indent + "      String s = TypeToStringConverterForRule"+ StringUtil.firstToUpper(rule.getName()) + ".convertToString"+(mappingExists?"ForRule"+lexerRuleName:"")+"( obj.get" + StringUtil.firstToUpper(sre.getName()) + "() );").append('\n');
                w.append(indent + "      if(s!=null) {").append('\n');

                String rulePath = UPRuleUtil.getPath(sre);
                boolean unnamedRule = false;
                boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                String grammarText = sre.getText();

                w.append(indent + "        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                //w.append(indent + "        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                w.append(indent + "        getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "          if(!ruleInfo.isConsumed()) {").append('\n');
                w.append(indent + "            // internalW.print(s);").append('\n');
                w.append(indent + "            getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                w.append(indent + "          }").append('\n');
                w.append(indent + "        getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "      }").append('\n');
                w.append(indent + "    }").append('\n');
            } else {

                w.append(indent + "    if(!prop" + StringUtil.firstToUpper(sre.getName()) + "Used.is())").append('\n');
                w.append(indent + "    {").append('\n');
                w.append(indent + "      prop" + StringUtil.firstToUpper(sre.getName()) + "Used.set(true);").append('\n');
                w.append(indent + "      String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "() /*TERMINAL String conversion*/;").append('\n');
                w.append(indent + "      if(s!=null) {").append('\n');

                String rulePath = UPRuleUtil.getPath(sre);
                boolean unnamedRule = false;
                boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                String grammarText = sre.getText();

                w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                //w.append(indent + "        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');

                w.append(indent + "        getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "        if(!ruleInfo.isConsumed()) {").append('\n');
                w.append(indent + "          // internalW.print(s);").append('\n');
                w.append(indent + "          getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                w.append(indent + "        }").append('\n');
                w.append(indent + "        getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                if(sre.ebnfOptional()) {
                w.append(indent + "      }").append('\n');
                } else {
                    w.append(indent + "      } else {").append('\n');
                    w.append(indent + "        // non optional case, we return early since the property object is null").append('\n');
                    generateRejectStateCode(indent+ "      ", gRule,altName, noCheck, w);
                    w.append(indent + "        return false;").append('\n');
                    w.append(indent + "      }").append('\n');
                }
                w.append(indent + "    }").append('\n');
            }
        }
    }

    private static void generateNamedSubRuleElementCode(Writer w, String indent, UPNamedSubRuleElement sre,
                                                        RuleClass gRule, String altName, boolean noCheck) throws IOException {

        w.append(indent+"    // handling sub-rule " + sre.getRuleId() + " with name '"+sre.getName()+"'").append('\n');

        // if the sub-rule is a string or a string list, we need to manually consume the property.
        Type subRuleType = gRule.getModel().propertyByName(gRule.nameWithUpper(), sre.getName()).get().getType();
        String propName = "obj.get" + StringUtil.firstToUpper(sre.getName()) + "()";
        if("String".equals(subRuleType.getName())) {
            w.append(indent + "    // we consume this sub-rule-property manually since it is a string or string list").append('\n');
            String ruleType = "Formatter.RuleType.TERMINAL";
            if(subRuleType.isArrayType()) {
                String indexName = "prop" + StringUtil.firstToUpper(sre.getName()) + "ListIndex";
                w.append(indent+"    // list type: we set the current element index to it's maximum value so there won't be any consumable elements left").append('\n');
                w.append(indent+"    if(" + indexName+ ".get()" +" < " +propName+ ".size() ) {").append('\n');
                w.append(indent+"      String s = " + propName + ".get(" + indexName +".getAndInc()) /*TERMINAL String conversion*/;").append('\n');
                w.append(indent+"      if(s!=null) {").append('\n');

                String rulePath = UPRuleUtil.getPath((SubRule)sre);
                boolean unnamedRule = false;
                boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                String grammarText = sre.getText();

                w.append(indent+"        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + sre.getName() + "\", s, " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                //w.append(indent+"        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + sre.getName() + "\", s);").append('\n');

                w.append(indent+"        getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"        if(!ruleInfo.isConsumed()) {").append('\n');
                w.append(indent+"          // internalW.print(s);").append('\n');
                w.append(indent+"          getUnparser().getFormatter().render( unparser, ruleInfo, internalW, s);").append('\n');
                w.append(indent+"        }").append('\n');
                w.append(indent+"        getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"      }").append('\n');
                w.append(indent+"    }").append('\n');
            } else {
                String propStateName = "prop" + StringUtil.firstToUpper(sre.getName()) + "Used";
                w.append(indent+"    if(!" + propStateName + ".is()) { ").append('\n');

                String rulePath = UPRuleUtil.getPath((SubRule)sre);
                boolean unnamedRule = false;
                boolean optionalRule=sre.ebnfOptional() || sre.ebnfZeroMany();
                String grammarText = sre.getText();

                w.append(indent+"      Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + sre.getName() + "\", " + propName + ", " + "\"" + rulePath + "\", " + unnamedRule  + "," + optionalRule + ", \"" + StringUtil.escapeJavaStyleString(grammarText, true) + "\");").append('\n');
                //w.append(indent+"      Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + sre.getName() + "\", " + propName + ");").append('\n');

                w.append(indent+"      getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"      if(!ruleInfo.isConsumed()) {").append('\n');
                w.append(indent+"        // internalW.print(" + propName + ");").append('\n');
                w.append(indent+"        getUnparser().getFormatter().render( unparser, ruleInfo, internalW, " + propName + ");").append('\n');
                w.append(indent+"      }").append('\n');
                w.append(indent+"      getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"      // string type: we define the property as used/consumed").append('\n');
                w.append(indent+"      " + propStateName+".set(true);").append('\n');

                if(!sre.ebnfOptional()) {
                    w.append(indent+"    } else { // non-optional case, we return early").append('\n');
                    generateRejectStateCode("    ",gRule, altName, noCheck, w);
                    w.append(indent+"      return false;").append('\n');
                    w.append(indent+"    } ").append('\n');
                } else {
                    w.append(indent+"    } // optional case, we do not return early").append('\n');
                }
            }
        } else {
            // this is a normal sub-rule
            w.append(indent+"    getUnparser().unparse( " + propName +", internalW);").append('\n');
        }
    }

    private static void generateSubRuleElementCode(Writer w, String altName, String indent, UPSubRuleElement sre, RuleClass gRule, boolean noCheck) throws IOException {
        w.append(indent+"    // handling sub-rule " + sre.getRuleId()).append('\n');
        if(!sre.ebnfOptional()&&!sre.ebnfZeroMany()) {
            w.append(indent+"    // this rule is not optional. we skip the rest of this alt if we can't match it.").append('\n');
            w.append(indent+"    if(!unparse" + altName + "SubRule" + sre.getRuleId() + "( obj, internalW )) {").append('\n');
            generateRejectStateCode(indent + "      ", gRule, altName, noCheck, w);
            w.append(indent+"      return false;").append('\n');
            w.append(indent+"    }").append('\n');
        } else {
            w.append(indent+"    // this rule is optional. we continue with the rest of this alt even if we can't match it.").append('\n');
            w.append(indent + "  unparse" + altName + "SubRule" + sre.getRuleId() + "( obj, internalW );").append('\n');
        }
    }
}

//                w.println(":type: unnamed-sub-rule");
//
//                if(sre.ebnfOneMany()) {
//                    w.println("one-many:  " + sre.ebnfOneMany());
//                } else if(sre.ebnfZeroMany()) {
//                    w.println("zero-many: " + sre.ebnfZeroMany());
//                } else if(sre.ebnfOne()) {
//                    w.println("one:       " + sre.ebnfOne());
//                } else if(sre.ebnfOptional()) {
//                    w.println("optional:  " + sre.ebnfOptional());
//                }