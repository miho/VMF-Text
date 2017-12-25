package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.vmftext.grammar.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class UnparserCodeGenerator {

    public static void generateUnparser(GrammarModel gModel, UnparserModel model, PrintWriter w) {

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


        generateUPParentUnparserCode(gModel, model,rules, w);
        generateUPCode(gModel, model,rules, w);

        //generateUPGrammarCode(gModel, model, w);
    }

    private static void generateUPParentUnparserCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, PrintWriter w) {
        w.println("public class "+gModel.getGrammarName()+"Unparser {");


        for (UPRule r : rules) {

            String ruleName = StringUtil.firstToUpper(r.getName());

            w.println("  private " + StringUtil.firstToLower(ruleName) + "Unparser = new " + ruleName + "Unparser();");
        }

        String rootClassNameUpperCase = gModel.rootClass().nameWithUpper();
        String rootClassNameLowerCase = gModel.rootClass().nameWithLower();

        String rootClassUnparserName = rootClassNameLowerCase + "Unparser";

        w.println();
        w.println("  public void unparse(" + gModel.getGrammarName()+"Model model, PrintWriter w) {");
        w.println("    "+rootClassUnparserName+".unparse(model.getRoot(), w);");
        w.println("  }");
        w.println();
        w.println();
        w.println("}");
    }

    private static void generateUPCode(GrammarModel gModel, UnparserModel model, List<UPRule> rules, PrintWriter w) {


        for (UPRule r : rules) {

            String ruleName = StringUtil.firstToUpper(r.getName());

            w.println("/*package private*/ class "+ruleName+"Unparser {");

            // find rule
            RuleClass gRule = gModel.getRuleClasses().stream().filter(
                    gRcl -> Objects.equals(StringUtil.firstToUpper(r.getName()), gRcl.nameWithUpper())).findFirst().get();

            w.println();
            w.println("  // begin declare list property indices/iterators");

            for(Property prop : gRule.getProperties()) {

                if(!prop.getType().isArrayType()) {
                    continue;
                }

                w.println("  int prop" + prop.nameWithUpper() + "ListIndex;");
            }
            w.println("  // end   declare list property indices/iterators");

            w.println();

            w.println("  public void "+"unparse("+ ruleName + " obj, PrintWriter w ) {");
            w.println("    ");
            w.println("    // begin reset list property indices/iterators");

            for(Property prop : gRule.getProperties()) {

                if(prop.getType().isArrayType()) {
                    w.println("    prop" + prop.nameWithUpper() + "ListIndex = 0;");
                }
            }
            w.println("    // end   reset list property indices/iterators");

            w.println();

            w.println("    // try to unparse alternatives of this rule");

            for(AlternativeBase a : r.getAlternatives()) {

                String altName = ruleName + "Alt" + a.getId();

                w.println("    if( unparse" + altName + "( obj, w ) ) { return; }");

            }
            w.println("  }");

            for(AlternativeBase a : r.getAlternatives()) {
                generateAltCode(w, gModel, r, gRule, ruleName, ruleName, a);
            }

            w.println("}\n");
        }
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

    private static void generateSubRuleCode(String ruleName, String altName, String objName, SubRule sr, PrintWriter w) {

        w.println();
        w.println("  private void unparse" + altName + "SubRule" + sr.getId() + "( " + objName + " obj, PrintWriter w ) {");

        for(AlternativeBase a : sr.getAlternatives()) {

            String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

            w.println("    if( unparse" + altNameSub + "( obj, w ) ) { return; }");
        }

        w.println("  }");

        // TODO check whether we really don't need this 22.12.2017
//        for(AlternativeBase a : sr.getAlternatives()) {
//            generateAltCode(w, ruleName, objName, a);
//        }
    }

    private static void generateAltCode(PrintWriter w, GrammarModel gModel, UPRule r, RuleClass gRule, String ruleName, String objName, AlternativeBase a) {
        String altName = ruleName + "Alt" + a.getId();
        w.println();
        w.println("  private boolean unparse"+ altName + "( " + objName + " obj, PrintWriter w ) {");

        w.println("    ");

        w.println("    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();");

        w.println("    PrintWriter internalW = new PrintWriter(output);");


        w.println("    ");
        w.println("    // begin preparing local list indices/iterators");


        for(Property prop : gRule.getProperties()) {
            if(!prop.getType().isArrayType()) {
                continue;
            }
            w.println("    int localProp" + prop.nameWithUpper() + "ListIndex = prop" + prop.nameWithUpper() + "ListIndex;");
        }

        w.println("    // end   preparing local list indices/iterators");
        w.println("    ");

        generateElements(w, gRule, a, altName);

        w.println("\n    String s = output.toString( \"UTF-8\" );");

        w.println("\n    if( match"+altName+"(s)) {");
        w.println("        w.print(s + \" \");");
        w.println();
        w.println("        // begin update global list indices/iterators since we consumed this alt successfully");

        for(Property prop : gRule.getProperties()) {
            if(!prop.getType().isArrayType()) {
                continue;
            }
            w.println("        prop" + prop.nameWithUpper() + "ListIndex = localProp" + prop.nameWithUpper() + "ListIndex;");
        }
        w.println("        // end   update global list indices/iterators since we consumed this alt successfully");
        w.println("    ");
        w.println("        return true;");
        w.println("    } else {");
        w.println("        return false;");
        w.println("    }");
        w.println("\n  }");

        a.getElements().stream().filter(el->el instanceof SubRule).filter(el->!(el instanceof UPNamedSubRuleElement)).
                map(el->(SubRule)el).forEach(sr-> {

            for(AlternativeBase sa : sr.getAlternatives()) {
                generateAltCode(w, gModel, r, gRule, altName + "SubRule" + sr.getId(), objName, sa);
            }

            generateSubRuleCode(ruleName, altName, objName, sr, w);
        });
    }

    private static void generateElements(PrintWriter w, RuleClass gRule, AlternativeBase a, String altName) {
        boolean parentOfAisZeroToManyOrOneToMany = false;
        String indent = "";

        if(a.getParentRule() instanceof UPElement) {
            UPElement parentOfa = (UPElement) a.getParentRule();

            if(parentOfa.ebnfZeroMany()) {
                parentOfAisZeroToManyOrOneToMany = true;
                indent = "  ";
                w.println();
                w.println("    // begin handling sub-rule with zeroToMany or oneToMany");
                w.println("    while(true) { ");
            }
        }
        for(UPElement e : a.getElements()) {
            w.println();
            if(e instanceof UPSubRuleElement) {
                UPSubRuleElement sre = (UPSubRuleElement) e;
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
                w.println(indent+"    // handling sub-rule " + sre.getId());
                w.println(indent+"    unparse" + altName + "SubRule" + sre.getId() + "( obj, internalW );");
            } else if(e instanceof UPNamedSubRuleElement) {
                UPNamedSubRuleElement sre = (UPNamedSubRuleElement) e;
                w.println(indent+"    // handling sub-rule " + sre.getId() + " with name '"+sre.getName()+"'");
                // w.println(indent+"    internalW.print( convertToString( obj.get"+ StringUtil.firstToUpper(sre.getName())+"() ) + \" \" );");
                w.println(indent+"    convertToString( obj.get"+ StringUtil.firstToUpper(sre.getName())+"(), internalW);");
                w.println(indent+"    internalW.print(\" \"); // insert whitespace");

//                w.println(":text: " + sre.getText());
//
//                w.println(":type: named-sub-rule");
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

            } else if(e instanceof UPNamedElement) {
                UPNamedElement sre = (UPNamedElement) e;
                w.println(indent+"    // handling element with name '"+sre.getName()+"'");
                if(sre.isListType()) {
                    String indexName = "localProp" + StringUtil.firstToUpper(sre.getName()) + "ListIndex";
                    String propName = "obj.get" + StringUtil.firstToUpper(sre.getName()+"()");
                    if(sre.ebnfOne()) {
                        if(sre.ebnfOptional()) {
                            w.println(indent+"    if(" + indexName +" < " +propName+ ".size() -1 ) {");
//                            w.println(indent+"      internalW.print( convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + ") ) + \" \");");
                            w.println(indent+"      convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + "), internalW );");
                            w.println(indent+"      internalW.print(\" \"); // insert whitespace");
                            w.println(indent+"    }");
                        } else {
                            w.println(indent+"    if(" + indexName +" > " +propName+ ".size() -1 || " + propName + ".isEmty()) { /*non-optional case*/ return false; }");
//                            w.println(indent+"    internalW.print( convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + ") ) + \" \");");
                            w.println(indent+"      convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + "), internalW );");
                            w.println(indent+"      internalW.print(\" \"); // insert whitespace");
                        }
                    } else if (sre.ebnfOneMany() || sre.ebnfZeroMany()) {

                        if(sre.ebnfOptional()||sre.ebnfZeroMany()) {
                            w.println(indent+"    while(" + indexName +" < " +propName+ ".size() -1 ) ");
                            // w.println(indent+"      internalW.print( convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + ") ) + \" \");");
                            w.println(indent+"      convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + "), internalW );");
                            w.println(indent+"      internalW.print(\" \"); // insert whitespace");
                        } else {
                            w.println(indent+"    boolean matched"+StringUtil.firstToUpper(sre.getName()) +" = false;");
                            w.println(indent+"    while(" + indexName +" < " +propName+ ".size() -1 || " + propName + ".isEmty()) {");
                           // w.println(indent+"      internalW.print( convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + ") ) + \" \");");
                            w.println(indent+"      convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "().get(" + indexName +"++) )" + "), internalW );");
                            w.println(indent+"      internalW.print(\" \"); // insert whitespace");
                            w.println(indent+"      matched"+StringUtil.firstToUpper(sre.getName()) +" = true;");
                            w.println(indent+"    }");
                            w.println(indent+"    // we are in the non-optional case and return early if we didn't match");
                            w.println(indent+"    if(!matched"+StringUtil.firstToUpper(sre.getName())+")");
                            w.println(indent+"      return false;");
                        }
                    }

                } else {
                    //w.println(indent+"    internalW.print( convertToString( obj.get"+ StringUtil.firstToUpper(sre.getName())+"() ) + \" \");");
                    w.println(indent+"    convertToString( obj.get" + StringUtil.firstToUpper(sre.getName()) + "() )" + "), internalW );");
                    w.println(indent+"    internalW.print(\" \"); // insert whitespace");
                }

//                w.println(":text: " + sre.getText());
//
//                w.println(":type: named-element");
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

            } else {
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
                    // replace ' with "
                    eText = "\""+eText.substring(1,eText.length()-1)+"\"";
                }

                w.println(indent+"    // handling unnamed element  '"+eText+"'");
                w.println(indent+"    internalW.print( "+eText + " + \" \" );");
            }
        }

        if(parentOfAisZeroToManyOrOneToMany) {
            w.println("    }");
            w.println("    // end   handling sub-rule with zeroToMany or oneToMany");
        }

    }
}
