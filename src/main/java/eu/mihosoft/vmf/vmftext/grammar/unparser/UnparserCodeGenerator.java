package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.vmftext.grammar.*;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class UnparserCodeGenerator {

    public static void generateUnparser(UnparserModel model, PrintWriter w) {
        generateUPCode(model,w);
    }

    private static void generateUPCode(UnparserModel model, PrintWriter w) {

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

        for (UPRule r : rules) {

            String ruleName = StringUtil.firstToUpper(r.getName());

            w.println("public class "+ruleName+"Unparser {");
            w.println("  public void "+"unparse("+ ruleName + " obj, PrintWriter w ) {");

            for(AlternativeBase a : r.getAlternatives()) {

                String altName = ruleName + "Alt" + a.getId();

                w.println("    if( unparse" + altName + "( obj, w ) ) { return; }");

            }
            w.println("  }");

            for(AlternativeBase a : r.getAlternatives()) {
                generateAltCode(w, ruleName, ruleName, a);
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

        w.println("  private void unparse" + altName + "SubRule" + sr.getId() + "( " + objName + " obj, PrintWriter w ) {");

        for(AlternativeBase a : sr.getAlternatives()) {

            String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

            w.println("    if( unparse" + altNameSub + "( obj, w ) ) { return; }");
        }

        w.println("  }");

        for(AlternativeBase a : sr.getAlternatives()) {
            generateAltCode(w, ruleName, objName, a);
        }
    }

    private static void generateAltCode(PrintWriter w, String ruleName, String objName, AlternativeBase a) {
        String altName = ruleName + "Alt" + a.getId();
        w.println("  private boolean unparse"+ altName + "( " + objName + " obj, PrintWriter w ) {");

        w.println("    ");

        w.println( "    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();\n" );
        w.println( "    PrintWriter internalW = new PrintWriter(output);" );

        for(UPElement e : a.getElements()) {
            if(e instanceof UPSubRuleElement) {
                UPSubRuleElement sre = (UPSubRuleElement) e;
                w.println(":type: unnamed-subrule");

                if(sre.ebnfOneMany()) {
                    w.println("one-many:  " + sre.ebnfOneMany());
                } else if(sre.ebnfZeroMany()) {
                    w.println("zero-many: " + sre.ebnfZeroMany());
                } else if(sre.ebnfOne()) {
                    w.println("one:       " + sre.ebnfOne());
                } else if(sre.ebnfOptional()) {
                    w.println("optional:  " + sre.ebnfOptional());
                }
                w.println("    unparse"+ altName + "SubRule" + sre.getId() + "( obj, internalW );");
            } else if(e instanceof UPNamedSubRuleElement) {
                UPNamedSubRuleElement sre = (UPNamedSubRuleElement) e;
                w.println("    internalW.print( convertToString( obj.get"+ StringUtil.firstToUpper(sre.getName())+"() ) + \" \" );");

                w.println(sre.getText());

                w.println(":type: named-subrule");

                if(sre.ebnfOneMany()) {
                    w.println("one-many:  " + sre.ebnfOneMany());
                } else if(sre.ebnfZeroMany()) {
                    w.println("zero-many: " + sre.ebnfZeroMany());
                } else if(sre.ebnfOne()) {
                    w.println("one:       " + sre.ebnfOne());
                } else if(sre.ebnfOptional()) {
                    w.println("optional:  " + sre.ebnfOptional());
                }

            } else if(e instanceof UPNamedElement) {
                UPNamedElement sre = (UPNamedElement) e;
                w.println("    internalW.print( convertToString( obj.get"+ StringUtil.firstToUpper(sre.getName())+"() ) + \" \");");

                w.println(sre.getText());

                w.println(":type: named-element");

                if(sre.ebnfOneMany()) {
                    w.println("one-many:  " + sre.ebnfOneMany());
                } else if(sre.ebnfZeroMany()) {
                    w.println("zero-many: " + sre.ebnfZeroMany());
                } else if(sre.ebnfOne()) {
                    w.println("one:       " + sre.ebnfOne());
                } else if(sre.ebnfOptional()) {
                    w.println("optional:  " + sre.ebnfOptional());
                }

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

                w.println("    internalW.print( "+eText + " + \" \" );");
            }
        }

        w.println("\n    String s = output.toString( \"UTF-8\" );");

        w.println("\n    if( match"+altName+"(s)) {");
        w.println("        w.print(s + \" \");");
        w.println("        return true;");
        w.println("    } else {");
        w.println("        return false;");
        w.println("    }");
        w.println("\n  }");

        a.getElements().stream().filter(el->el instanceof SubRule).filter(el->!(el instanceof UPNamedSubRuleElement)).
                map(el->(SubRule)el).forEach(sr-> {

            for(AlternativeBase sa : sr.getAlternatives()) {
                generateAltCode(w, altName + "SubRule" + sr.getId(), objName, sa);
            }

            generateSubRuleCode(ruleName, altName, objName, sr, w);
        });
    }
}
