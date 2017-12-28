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

    public static void generateUnparser(GrammarModel gModel, ReadOnlyUnparserModel roModel, String unparserGrammarPath, ResourceSet resourceSet) {

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

    private static void generateUPGrammarCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, Writer w) throws IOException {
        w.append("grammar ").append(gModel.getGrammarName()+"ModelUnparserGrammar;").append('\n');
        w.append("import ").append(gModel.getGrammarName()).append(";\n");
        w.append('\n');

        for(UPRule rule :rules) {
            String ruleName = StringUtil.firstToLower(rule.getName());
            generateAltGrammarCode(gModel, model, rules, rule, ruleName, w);
        }
    }

    private static void generateAltGrammarCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, UPRuleBase rule, String parentName, Writer w) throws IOException {
        for(AlternativeBase a : rule.getAlternatives()) {
            String aName = parentName + "Alt"+a.getId();

            // filter alt name elements (have to be removed to produce a valid grammar)
            String aText = a.getElements().stream().filter(e->!e.getText().
                    startsWith("#")).map(e->e.getText() + " ").collect(Collectors.joining());

            w.append(aName+": ").append(aText + ";\n");

            generateElementGrammarCode(gModel,model,rules,a,aName,w);
        }
    }

    private static void generateElementGrammarCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, AlternativeBase alt, String parentName, Writer w) throws IOException {
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

    private static void generateUPParentUnparserCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, Writer w) throws IOException {

        w.append("package " + gModel.getPackageName()+".unparser;").append('\n').append('\n');

        w.append("// Java API imports").append('\n');
        w.append("import java.io.UnsupportedEncodingException;").append('\n');
        w.append("import java.io.ByteArrayOutputStream;").append('\n');
        w.append("import java.io.Writer;").append('\n');
        w.append("import java.io.PrintWriter;").append('\n').append('\n');

        w.append("// Model API imports").append('\n');
        w.append("import "+gModel.getPackageName()+"." + gModel.getGrammarName() + "Model;").append('\n').append('\n');

        w.append("// rule imports (from model api)").append('\n');

        for (UPRule rImport : rules) {

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
        }

        w.append("  public void unparse(Object o, Writer w) throws java.io.IOException {").append('\n');
        w.append("    if(!(o instanceof eu.mihosoft.vmf.runtime.core.VObject) && o!=null) {").append('\n');
        w.append("      w.append(o.toString() + \" \"); // TODO introduce type mapping from rule type to string").append('\n');
        w.append("    } else ");

        for (UPRule r : rules) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            w.append("if ( o instanceof " + ruleName + " ) {").append('\n');
            w.append("      unparse( ("+ruleName+")o, w );").append('\n');
            w.append("    }");
        }

        w.append('\n');
        w.append("    w.flush();").append('\n');

        w.append("  }").append('\n');
        w.append("  public void unparse(Object o, PrintWriter w) {").append('\n');
        w.append("    if(!(o instanceof eu.mihosoft.vmf.runtime.core.VObject) && o!=null) {").append('\n');
        w.append("      w.print(o.toString() + \" \"); // TODO introduce type mapping from rule type to string").append('\n');
        w.append("    } else ");

        for (UPRule r : rules) {
            String ruleName = StringUtil.firstToUpper(r.getName());
            w.append("if ( o instanceof " + ruleName + " ) {").append('\n');
            w.append("      unparse( ("+ruleName+")o, w );").append('\n');
            w.append("    }");
        }

        w.append('\n');

        w.append("    w.flush();").append('\n');

        w.append("  }").append('\n');
        w.append('\n');
        w.append('\n');
        w.append(" static class IntValue { private int value; public void set(int v) {this.value = v;} public int get() { return this.value; } int getAndInc() {int result = this.value;this.value++; return result;} }").append('\n');
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
                w.append('\n');
                w.append('\n');

                w.append("// rule imports (from model api)").append('\n');

                for (UPRule rImport : rules) {

                    String ruleImportName = StringUtil.firstToUpper(rImport.getName());

                    w.append("import " + gModel.getPackageName() + "." + ruleImportName + ";").append('\n');
                }

                w.append('\n');

                w.append("/*package private*/ class " + ruleName + "Unparser {").append('\n');

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

                w.append("  private final " + gModel.getGrammarName() + "ModelUnparser unparser;").append('\n');

                w.append('\n');

                w.append("  private void pushState() {").append('\n');
                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("    prop"+ prop.nameWithUpper() + "State.push( prop" + prop.nameWithUpper() + "ListIndex );").append('\n');
                }
                w.append("  }").append('\n');

                w.append("  private void popState() {").append('\n');
                for (Property prop : gRule.getProperties()) {

                    if (!prop.getType().isArrayType()) {
                        continue;
                    }

                    w.append("    prop" + prop.nameWithUpper() + "ListIndex = prop"+ prop.nameWithUpper() + "State.pop();").append('\n');
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

                w.append("    // try to unparse alternatives of this rule").append('\n');

                for (AlternativeBase a : r.getAlternatives()) {

                    String altName = ruleName + "Alt" + a.getId();

                    w.append("    if( unparse" + altName + "( obj, w ) ) { popState(); return; }").append('\n').append('\n');

                }
                w.append("    popState();").append('\n').append('\n');
                w.append("  }").append('\n');

                for (AlternativeBase a : r.getAlternatives()) {
                    generateAltCode(w, gModel, r, gRule, ruleName, ruleName, a);
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

    private static void generateSubRuleCode(String ruleName, String altName, String objName, SubRule sr, Writer w) throws IOException {

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
            generateAltCodeForSubRulesWithMultiplier(altName, sr, w);
        } else {
            for(AlternativeBase a : sr.getAlternatives()) {

                String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

                w.append("    if( unparse" + altNameSub + "( obj, w ) ) { return; }").append('\n');
            }
        }

        w.append("  }").append('\n');
    }

    private static void generateAltCodeForSubRulesWithMultiplier(String altName, SubRule sr, Writer w) throws IOException {
        w.append('\n');
        w.append("    // begin handling sub-rule with zeroToMany or oneToMany").append('\n');
        w.append("    while(true) { ").append('\n');
        w.append("      boolean matchedAnyAlt = false;").append('\n');

        boolean first = true;
        String elseStr = "";
        for(AlternativeBase a : sr.getAlternatives()) {

            String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

            if(!first) {
                elseStr = " else";
            } else {
                first=false;
            }

            w.append("     " + elseStr + " if( unparse" + altNameSub + "( obj, w ) ) { matchedAnyAlt = true; }").append('\n');

        }

        w.append("      if( matchedAnyAlt == false ) break;").append('\n');
        w.append("    }").append('\n');
        w.append("    // end   handling sub-rule with zeroToMany or oneToMany").append('\n').append('\n');
    }

    private static void generateAltCode(Writer w, GrammarModel gModel, UPRule r, RuleClass gRule, String ruleName, String objName, AlternativeBase a) throws IOException {
        String altName = ruleName + "Alt" + a.getId();
        w.append('\n');
        w.append("  private boolean unparse"+ altName + "( " + objName + " obj, PrintWriter w ) {").append('\n');

        w.append('\n');
        w.append("    if(obj==null) return false;").append('\n');
        w.append('\n');

        w.append("    // begin check whether unused properties are set").append('\n');

        // alternatives of grammar rules need to check whether they use all properties that are set and do
        // not use any unset property
        if(a.getParentRule() == r) {
            generateUnusedPropertiesCheck(a, r, w);
        }

        w.append("    // end   check whether unused properties are set").append('\n');

        w.append('\n');

        w.append("    ByteArrayOutputStream output = new ByteArrayOutputStream();").append('\n');

        w.append("    PrintWriter internalW = new PrintWriter(output);").append('\n');

        w.append('\n');
        w.append("    // begin preparing local list indices/iterators").append('\n');


        for(Property prop : gRule.getProperties()) {
            if(!prop.getType().isArrayType()) {
                continue;
            }
            w.append("    int prevProp" + prop.nameWithUpper() + "ListIndex = prop" + prop.nameWithUpper() + "ListIndex.get();").append('\n');
        }

        w.append("    // end   preparing local list indices/iterators").append('\n');

        generateElements(w, gRule, a, altName);

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

        w.append("\n    if( match"+altName+"(s) ) {").append('\n');
        w.append("         w.print(s /*+ \" \"*/);").append('\n');

        w.append('\n');
        w.append("        return true;").append('\n');
        w.append("    } else {").append('\n');
        w.append('\n');
        w.append("        // begin update global list indices/iterators since we consumed this alt successfully").append('\n');

        for(Property prop : gRule.getProperties()) {
            if(!prop.getType().isArrayType()) {
                continue;
            }
            w.append("        prop" + prop.nameWithUpper() + "ListIndex.set(prevProp" + prop.nameWithUpper() + "ListIndex);").append('\n');
        }
        w.append("        // end   update global list indices/iterators since we consumed this alt successfully").append('\n');
        w.append("        return false;").append('\n');
        w.append("    }").append('\n');
        w.append("\n  }").append('\n').append('\n');

        generateMatchAltMethod(gModel, altName, w);

        a.getElements().stream().filter(el->el instanceof SubRule).filter(el->!(el instanceof UPNamedSubRuleElement)).
                map(el->(SubRule)el).forEach(sr-> {

            for(AlternativeBase sa : sr.getAlternatives()) {
                try {
                    generateAltCode(w, gModel, r, gRule, altName + "SubRule" + sr.getId(), objName, sa);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                generateSubRuleCode(ruleName, altName, objName, sr, w);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void _getPropertiesUsedInAlternative(AlternativeBase a, List<String> propertyNames, Map<String, Boolean> listTypeMap) {
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
                    _getPropertiesUsedInAlternative(subA,propertyNames, listTypeMap);
                }
            }
        }

    }

    private static List<String> getPropertiesUsedInAlternative(AlternativeBase a) {

        List<String> propertyNames = new ArrayList<>();

        _getPropertiesUsedInAlternative(a, propertyNames, null);

        return propertyNames;
    }

    private static void generateUnusedPropertiesCheck(AlternativeBase a, UPRule r, Writer w) throws IOException {
        List<String> propertyNamesUsed = getPropertiesUsedInAlternative(a);
        Map<String,Boolean> propertyNamesInRule = getPropertyNamesOfRule(r);

        List<String> propertiesNotUsedInAlt = new ArrayList<>();
        propertiesNotUsedInAlt.addAll(propertyNamesInRule.keySet());
        propertiesNotUsedInAlt.removeAll(propertyNamesUsed);

        for(String pName : propertiesNotUsedInAlt) {

            if(propertyNamesInRule.get(pName)) {
                w.append("    if(!obj.get" + StringUtil.firstToUpper(pName) + "().isEmpty()) return false;").append('\n');
            } else {
                w.append("    if(obj.get" + StringUtil.firstToUpper(pName) + "() !=null) return false;").append('\n');
            }
        }
    }

    private static Map<String,Boolean> getPropertyNamesOfRule(UPRule r) {
        List<String> propertyNames = new ArrayList<>();
        Map<String,Boolean> listType = new HashMap<>();

        for(AlternativeBase a : r.getAlternatives()) {
            _getPropertiesUsedInAlternative(a, propertyNames, listType);
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

    private static void generateElements(Writer w, RuleClass gRule, AlternativeBase a, String altName) throws IOException {
        String indent = "";

        for(UPElement e : a.getElements()) {
            w.append('\n');
            if(e instanceof UPSubRuleElement) {
                generateSubRuleElementCode(w, altName, indent, (UPSubRuleElement) e);
            } else if(e instanceof UPNamedSubRuleElement) {
                generateNamedSubRuleElementCode(w, indent, (UPNamedSubRuleElement) e);
            } else if(e instanceof UPNamedElement) {
                UPNamedElement sre = (UPNamedElement) e;
                generateNamedElementCode(w, indent, sre);
            } else {
                generateUnnamedElementCode(w, indent, e);
            }
        }
    }

    private static void generateUnnamedElementCode(Writer w, String indent, UPElement e) throws IOException {

        // ignore EOF element
        if("EOF".equals(e.getText().trim())){
            return;
        }

        // remove ebnf multiplicity, optional and greedy characters
        String eText = e.getText();
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

        if(eText.startsWith("'")) {
            // remove '
            eText = eText.substring(1,eText.length()-1);
        }



        w.append(indent+"    // handling unnamed element  '"+eText+"'").append('\n');
        w.append(indent+"    internalW.print( \""+StringUtil.escapeJavaStyleString(eText,true) + "\" + \" \" );").append('\n');
    }

    private static void generateNamedElementCode(Writer w, String indent, UPNamedElement sre) throws IOException {
        w.append(indent+"    // handling element with name '"+sre.getName()+"'").append('\n');
        if(sre.isListType()) {
            String indexName = "prop" + StringUtil.firstToUpper(sre.getName()) + "ListIndex";
            String propName = "obj.get" + StringUtil.firstToUpper(sre.getName()+"()");
            if(sre.ebnfOne()) {
                if(sre.ebnfOptional()) {
                    w.append(indent+"    if(" + indexName+ ".get()" +" < " +propName+ ".size() ) {").append('\n');
                    w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc())" + ", internalW );").append('\n');
                    w.append(indent+"      internalW.print(\" \"); // insert whitespace").append('\n');
                    w.append(indent+"    }").append('\n');
                } else {

                    String breakOrReturn = " /*non optional case*/ return false;";

                    w.append(indent+"    if(" + indexName + ".get()" +" > " +propName+ ".size() -1 || " + propName + ".isEmpty()) { " +breakOrReturn + " }").append('\n');
                    w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc())" + ", internalW );").append('\n');
                    w.append(indent+"      // internalW.print(\" \"); // insert whitespace").append('\n');
                }
            } else if (sre.ebnfOneMany() || sre.ebnfZeroMany()) {

                if(sre.ebnfOptional()||sre.ebnfZeroMany()) {
                    w.append(indent+"    while(" + indexName+ ".get()" +" < " +propName+ ".size() ) {").append('\n');
                    w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc())" + ", internalW );").append('\n');
                    w.append(indent+"      // internalW.print(\" \"); // insert whitespace").append('\n');
                    w.append(indent+"    }").append('\n');
                } else {
                    w.append(indent+"    boolean matched"+StringUtil.firstToUpper(sre.getName()) +" = false;").append('\n');
                    w.append(indent+"    while(" + indexName+ ".get()" +" < " +propName+ ".size() || " + propName + ".isEmpty()) {").append('\n');
                    w.append(indent+"      getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +".getAndInc())" + ", internalW );").append('\n');
                    w.append(indent+"      // internalW.print(\" \"); // insert whitespace").append('\n');
                    w.append(indent+"      matched"+StringUtil.firstToUpper(sre.getName()) +" = true;").append('\n');
                    w.append(indent+"    }").append('\n');
                    w.append(indent+"    // we are in the non-optional case and return early if we didn't match").append('\n');
                    w.append(indent+"    if(!matched"+StringUtil.firstToUpper(sre.getName())+")").append('\n');
                    w.append(indent+"      return false;").append('\n');
                }
            }

        } else {
            w.append(indent+"    getUnparser().unparse( obj.get" + StringUtil.firstToUpper(sre.getName()) + "()" + ", internalW );").append('\n');
            w.append(indent+"    // internalW.print(\" \"); // insert whitespace").append('\n');
        }
    }

    private static void generateNamedSubRuleElementCode(Writer w, String indent, UPNamedSubRuleElement e) throws IOException {
        UPNamedSubRuleElement sre = e;
        w.append(indent+"    // handling sub-rule " + sre.getId() + " with name '"+sre.getName()+"'").append('\n');
        w.append(indent+"    getUnparser().unparse( obj.get"+ StringUtil.firstToUpper(sre.getName())+"(), internalW);").append('\n');
        w.append(indent+"    // internalW.print(\" \"); // insert whitespace").append('\n');
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