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
            String aName = parentName + "Alt"+a.getId();

            // filter alt name elements (have to be removed to produce a valid grammar)
            String aText = a.getElements().stream().filter(e->!e.getText().
                    startsWith("#")).map(e->e.getText() + " ").collect(Collectors.joining());

            w.append(aName+": ").append(aText + " EOF ;\n");

            generateElementGrammarCode(gModel,model,rules,a,aName,w);
        }
    }

    private static void generateElementGrammarCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules,
                                                   AlternativeBase alt, String parentName, Writer w) throws IOException {
        for(UPElement e : alt.getElements()) {
            if(e instanceof SubRule) {
                SubRule sr = (SubRule) e;
                String srName = parentName+"SubRule"+sr.getId();

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

        for (UPRule rImport : rules) {

            String ruleImportName = StringUtil.firstToUpper(rImport.getName());

            w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
        }

        List<UPRule> parentsOfLabeledAlts = computeParentsOfLabeledAlts(model);
        w.append('\n');
        w.append("// alt parents imports (from model api)").append('\n');
        for (UPRule rImport : parentsOfLabeledAlts) {

            String ruleImportName = StringUtil.firstToUpper(rImport.getName());

            w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
        }

        w.append('\n');
        w.append("import " + gModel.getPackageName()+ ".unparser." +gModel.getGrammarName()+ "ModelUnparser.IntValue;").append('\n');
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
        w.append(" static class IntValue { private int value; public void set(int v) {this.value = v;} public int get() { return this.value; } int getAndInc() {int result = this.value;this.value++; return result;} }").append('\n');
        w.append('\n');
        w.append('\n');
        w.append(" static class BoolValue { private boolean value; public void set(boolean v) {this.value = v;} public boolean is() { return this.value; } boolean getAndInvert() {boolean result = this.value;this.value=!this.value; return result;} }").append('\n');
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
                w.append("import " + gModel.getPackageName()+ ".unparser." +gModel.getGrammarName()+ "ModelUnparser.IntValue;").append('\n');
                w.append("import " + gModel.getPackageName()+ ".unparser." +gModel.getGrammarName()+ "ModelUnparser.BoolValue;").append('\n');
                w.append('\n');
                w.append('\n');

                w.append("// rule imports (from model api)").append('\n');

                for (UPRule rImport : rules) {

                    String ruleImportName = StringUtil.firstToUpper(rImport.getName());

                    w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
                }

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

                    w.append("  IntValue prop" + prop.nameWithUpper() + "ListIndex = new IntValue();").append('\n');
                    w.append("  final Deque<IntValue> prop"+ prop.nameWithUpper() + "State = new ArrayDeque<>();").append('\n');
                }

                w.append("  // end   declare list property indices/iterators").append('\n');

                w.append('\n');
                w.append('\n');
                w.append("  // begin declare property usage flags").append('\n');

                for (Property prop : gRule.getProperties()) {

                    if (prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("  BoolValue prop" + prop.nameWithUpper() + "Used = new BoolValue();").append('\n');
                    w.append("  final Deque<BoolValue> prop"+ prop.nameWithUpper() + "State = new ArrayDeque<>();").append('\n');
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
                        w.append("    prop" + prop.nameWithUpper() + "ListIndex = new IntValue();").append('\n');
                    }
                }
                w.append("    // end   reset list property indices/iterators").append('\n');
                w.append('\n');

                w.append("    // begin reset property usage flags").append('\n');
                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        w.append("    prop" + prop.nameWithUpper() + "Used = new BoolValue();").append('\n');
                    }
                }
                w.append("    // end   reset property usage flags").append('\n');
                w.append('\n');

                w.append("    // try to unparse alternatives of this rule").append('\n');

                // optimize alts (check the one with most elements first)
                List<AlternativeBase> alts = new ArrayList<>(r.getAlternatives());
                //sortAltsMostElementsFirst(alts, rules);

                for (AlternativeBase a : alts) {

                    String altName = ruleName + "Alt" + a.getId();

                    w.append("    if( unparse" + altName + "( obj, w ) ) { popState(); return; }").append('\n').append('\n');

                }
                w.append("    // TODO: 29.12.2017 introduce unparser error handler etc.").append('\n');
                w.append("    throw new RuntimeException(\"Cannot unparse rule '" + ruleName + "'. Language model is invalid!\");").append('\n');
                w.append("    // popState();").append('\n').append('\n');
                w.append("  }").append('\n');


                int altIndex = 0;
                for (AlternativeBase a : r.getAlternatives()) {
                    altIndex++;
                    boolean noCheck = altIndex >= r.getAlternatives().size();

                    // TODO 06.01.2018 introduce switch for enabling/disabling full validation
                    // - remove false&& below to remove expensive checks from last alternative to speed up performance
                    // - do this only if you KNOW that your model is VALID
                    generateAltCode(w, model, gModel, r, gRule, ruleName, ruleName, a,
                            false&&noCheck);
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
        w.append("  private void unparse" + altName + "SubRule" + sr.getId() + "( " + objName + " obj, PrintWriter w ) {").append('\n');

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

                String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

                w.append("    if( unparse" + altNameSub + "( obj, w ) ) { return; }").append('\n');
            }
        }

        w.append("  }").append('\n');
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
            String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();
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

            String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

            String canConsumeVarName = StringUtil.firstToLower(altNameSub + "CanConsume");

            if(!first) {
                elseStr = " else";
            } else {
                first=false;
            }

            w.append("     " + elseStr + " if( "
                    + canConsumeVarName + " && unparse" + altNameSub + "( obj, w ) ) { ").append('\n').append('\n');
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
                                        String ruleName, String objName, AlternativeBase a, boolean noCheck) throws IOException {
        String altName = ruleName + "Alt" + a.getId();
        w.append('\n');
        w.append("  private boolean unparse"+ altName + "( " + objName + " obj, PrintWriter w ) {").append('\n');

        w.append('\n');
        w.append("    if(obj==null) return false;").append('\n');
        w.append('\n');

        if(!noCheck) {
            w.append("    getUnparser().getFormatter().pushState();").append('\n');
        }

        w.append("    // begin check whether unused properties are set").append('\n');

        // alternatives of grammar rules need to check whether they use all properties that are set and do
        // not use any unset property
        if(a.getParentRule() == r) {
            generateUnusedPropertiesCheck(a, r, w);
        }

        w.append("    // end   check whether unused properties are set").append('\n');

        w.append('\n');

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

        generateElements(model, gModel, w, gRule, r, a, altName);

        if(noCheck) {
            w.append('\n');
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
            w.append("        // begin revert global list indices/iterators since we didn't consume this alt").append('\n');

            for (Property prop : gRule.getProperties()) {
                if (!prop.getType().isArrayType()) {
                    continue;
                }
                w.append("        prop" + prop.nameWithUpper() + "ListIndex.set(prevProp" + prop.nameWithUpper() + "ListIndex);").append('\n');
            }
            w.append("        // end   revert global list indices/iterators since we didn't consume this alt").append('\n');
            w.append('\n');
            w.append('\n');
            w.append("        // begin revert global property usage flags since we didn't consume this alt").append('\n');

            for (Property prop : gRule.getProperties()) {
                if (prop.getType().isArrayType()) {
                    continue;
                }
                w.append("        prop" + prop.nameWithUpper() + "Used.set(prevProp" + prop.nameWithUpper() + "Used);").append('\n');
            }
            w.append("        // end   update global property usage flags since we consume this alt").append('\n');
            w.append('\n');
            w.append("        getUnparser().getFormatter().rejectState();").append('\n');
            w.append('\n');
            w.append("        return false;").append('\n');
            w.append("    }").append('\n');
            w.append("\n  }").append('\n').append('\n');

        } // end if !noCheck

        generateMatchAltMethod(gModel, altName, w);

        a.getElements().stream().filter(el->el instanceof SubRule).filter(el->!(el instanceof UPNamedSubRuleElement)).
                map(el->(SubRule)el).forEach(sr-> {

            for(AlternativeBase sa : sr.getAlternatives()) {
                try {
                    generateAltCode(w, model, gModel, r, gRule, altName + "SubRule" + sr.getId(), objName, sa, false);
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

            if(propertyNamesInRuleWithListFlag.get(pName)) {
                w.append("    if( !obj.get" + StringUtil.firstToUpper(pName) + "().isEmpty() ) return false;").append('\n');
            } else {
                w.append("    if( obj.get" + StringUtil.firstToUpper(pName) + "() !=null ) return false;").append('\n');
            }
        }
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

    private static void generateElements(UnparserModel model, GrammarModel gModel, Writer w, RuleClass gRule, UPRule rule, AlternativeBase a, String altName) throws IOException {
        String indent = "";

        for(UPElement e : a.getElements()) {
            w.append('\n');
            if(e instanceof UPSubRuleElement) {
                generateSubRuleElementCode(w, altName, indent, (UPSubRuleElement) e);
            } else if(e instanceof UPNamedSubRuleElement) {
                generateNamedSubRuleElementCode(w, indent, (UPNamedSubRuleElement) e, gRule);
            } else if(e instanceof UPNamedElement) {
                UPNamedElement sre = (UPNamedElement) e;
                generateNamedElementCode(w, indent, model, sre, rule, gModel);
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
            String ruleInfoString = "Formatter.RuleInfo.newRuleInfo(obj, Formatter.RuleType.TERMINAL, null, \"" + ruleString + "\")";
            w.append(indent + "    getUnparser().getFormatter().pre( unparser, " + ruleInfoString + ", internalW);").append('\n');
            w.append(indent + "    internalW.print( \""+StringUtil.escapeJavaStyleString(eText,true) + "\");").append('\n');
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
                    String ruleInfoString = "Formatter.RuleInfo.newRuleInfo(obj, Formatter.RuleType.LEXER_RULE, \"" + ruleName + "\", \"" + StringUtil.escapeJavaStyleString(lexerRuleString, true) + "\")";
                    w.append(indent + "    getUnparser().getFormatter().pre( unparser, " + ruleInfoString + ", internalW);").append('\n');
                    w.append(indent + "    internalW.print( \"" + StringUtil.escapeJavaStyleString(lexerRuleString, true) + "\" /*+ \" \" */);").append('\n');
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

    private static void generateNamedElementCode(Writer w, String indent, UnparserModel model, UPNamedElement sre, UPRule rule, GrammarModel gModel) throws IOException {

        String lexerRuleName = sre.getRuleName()!=null?sre.getRuleName():"";

        w.append(indent+"    // handling element with name '"+sre.getName()+"'").append('\n');
        String ruleType = "/*FIXME: TYPE IS UNDEFINED!*/";
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
                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          internalW.print(s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    } else {
                        w.append(indent+"      String listElemObj = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName + ".getAndInc());").append('\n');
                        w.append(indent+"      Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", listElemObj /*TERMINAL String conversion*/" + ")");
                        w.append(indent+"      getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"      internalW.print(s);").append('\n');
                        w.append(indent+"      getUnparser().getFormatter().post( unparser, ruleInfo, internalW);").append('\n');
                    }
                    w.append(indent + "    }").append('\n');
                } else {

                    String breakOrReturn = " /*non optional case*/ return false;";

                    w.append(indent+"    if(" + indexName + ".get()" +" > " +propName+ ".size() -1 || " + propName + ".isEmpty()) { " +breakOrReturn + " }").append('\n');
                    if(sre.isParserRule()) {
                        w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()), internalW );").append('\n');
                    } else if(sre.isLexerRule()) {
                        w.append(indent+"      {").append('\n');
                        boolean mappingExists = gModel.getTypeMappings().mappingByRuleNameExists(rule.getName(), lexerRuleName);
                        w.append(indent+"        String s = TypeToStringConverterForRule"+ StringUtil.firstToUpper(rule.getName()) + ".convertToString"+(mappingExists?"ForRule"+lexerRuleName:"")+"( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) );").append('\n');
                        w.append(indent+"        if(s!=null) {").append('\n');
                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          internalW.print(s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    } else {
                        w.append(indent+"      {").append('\n');
                        w.append(indent+"        String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) /*TERMINAL String conversion*/;").append('\n');
                        w.append(indent+"        if(s !=null) {").append('\n');
                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", listElemObj.toString() /*TERMINAL String conversion*/" + ")");
                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          internalW.print(s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
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
                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          internalW.print(s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"        }").append('\n');
                        w.append(indent+"      }").append('\n');
                    } else {
                        w.append(indent+"      {").append('\n');
                        w.append(indent+"        String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()) /*TERMINAL String conversion*/;").append('\n');
                        w.append(indent+"        if(s!=null) {").append('\n');
                        w.append(indent+"          Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                        w.append(indent+"          getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          internalW.print(s);").append('\n');
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
                        w.append(indent+"            Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"            internalW.print(s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"        }").append('\n');
                    } else {
                        w.append(indent+"        {").append('\n');
                        w.append(indent+"          String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc()).toString() /*TERMINAL String conversion*/;").append('\n');
                        w.append(indent+"          if(s!=null) {").append('\n');
                        w.append(indent+"            Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"            internalW.print(s);").append('\n');
                        w.append(indent+"            getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                        w.append(indent+"          }").append('\n');
                        w.append(indent+"        }").append('\n');
                    }
                    w.append(indent+"    }").append('\n');
                    w.append(indent+"    // we are in the non-optional case and return early if we didn't match").append('\n');
                    w.append(indent+"    if(!matched"+StringUtil.firstToUpper(sre.getName())+")").append('\n');
                    w.append(indent+"      return false;").append('\n');
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
                w.append(indent + "        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                w.append(indent + "        getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "        internalW.print(s);").append('\n');
                w.append(indent + "        getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "      }").append('\n');
                w.append(indent + "    }").append('\n');
            } else {

                w.append(indent + "    if(!prop" + StringUtil.firstToUpper(sre.getName()) + "Used.is())").append('\n');
                w.append(indent + "    {").append('\n');
                w.append(indent + "      prop" + StringUtil.firstToUpper(sre.getName()) + "Used.set(true);").append('\n');
                w.append(indent + "      String s = obj.get" + StringUtil.firstToUpper(sre.getName()) + "() /*TERMINAL String conversion*/;").append('\n');
                w.append(indent + "      if(s!=null) {").append('\n');
                w.append(indent + "        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + lexerRuleName + "\", s);").append('\n');
                w.append(indent + "        getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "        internalW.print(s);").append('\n');
                w.append(indent + "        getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent + "      }").append('\n');
                w.append(indent + "    }").append('\n');
            }
        }
    }

    private static void generateNamedSubRuleElementCode(Writer w, String indent, UPNamedSubRuleElement sre, RuleClass gRule) throws IOException {

        w.append(indent+"    // handling sub-rule " + sre.getId() + " with name '"+sre.getName()+"'").append('\n');

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
                w.append(indent+"        Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + sre.getName() + "\", s);").append('\n');
                w.append(indent+"        getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"        internalW.print(s);").append('\n');
                w.append(indent+"        getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"      }").append('\n');
                w.append(indent+"    }").append('\n');
            } else {
                String propStateName = "prop" + StringUtil.firstToUpper(sre.getName()) + "Used";
                w.append(indent+"    if(!" + propStateName + ".is()) { ").append('\n');
                w.append(indent+"      Formatter.RuleInfo ruleInfo = Formatter.RuleInfo.newRuleInfo(obj, " + ruleType + ", \"" + sre.getName() + "\", " + propName + ");").append('\n');
                w.append(indent+"      getUnparser().getFormatter().pre( unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"      internalW.print(" + propName + ");").append('\n');
                w.append(indent+"      getUnparser().getFormatter().post(unparser, ruleInfo, internalW);").append('\n');
                w.append(indent+"      // string type: we define the property as used/consumed").append('\n');
                w.append(indent+"      " + propStateName+".set(true);").append('\n');
                w.append(indent+"    }").append('\n');
            }
        } else {
            // this is a normal sub-rule
            w.append(indent+"    getUnparser().unparse( " + propName +", internalW);").append('\n');
        }
    }

    private static void generateSubRuleElementCode(Writer w, String altName, String indent, UPSubRuleElement sre) throws IOException {
        w.append(indent+"    // handling sub-rule " + sre.getId()).append('\n');
        w.append(indent+"    unparse" + altName + "SubRule" + sre.getId() + "( obj, internalW );").append('\n');
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