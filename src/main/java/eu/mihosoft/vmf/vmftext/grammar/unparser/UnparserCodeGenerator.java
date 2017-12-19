package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.core.io.Resource;
import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.vmftext.grammar.*;

import java.io.PrintWriter;
import java.util.List;

public class UnparserCodeGenerator {

    public static void generateUnparser(UnparserModel model, PrintWriter w) {
        generateUPCode(model,w);
    }

    private static void generateUPCode(UnparserModel model, PrintWriter w) {

        for (UPRule r : model.getRules()) {

            String ruleName = StringUtil.firstToUpper(r.getName());

            w.println("public class "+ruleName+"Unparser("+ ruleName + " obj, PrintWriter w ) {");
            w.println("  public void "+"unparse("+ ruleName + " obj, PrintWriter w ) {");

            for(AlternativeBase a : r.getAlternatives()) {

                String altName = ruleName + "Alt" + a.getId();

                w.println("    if( unparse" + altName + "( obj, w ) ) { return; }");

            }
            w.println("  }");

            for(AlternativeBase a : r.getAlternatives()) {
                generateAltCode(w, ruleName, a);
            }


            w.println("}\n");
        }
    }

    private static void generateSubRuleCode(String ruleName, String altName, SubRule sr, PrintWriter w) {


        w.println("  public void unparse" + altName + "SubRule" + sr.getId() + "( " + ruleName + " obj, PrintWriter w ) {");

        for(AlternativeBase a : sr.getAlternatives()) {

            String altNameSub = altName + "SubRule" + sr.getId() + "Alt" + a.getId();

            w.println("    if( unparse" + altNameSub + "( obj, w ) ) { return; }");

        }
        w.println("  }");

        for(AlternativeBase a : sr.getAlternatives()) {
            generateAltCode(w, ruleName, a);
        }
    }

    private static void generateAltCode(PrintWriter w, String ruleName, AlternativeBase a) {
        String altName = ruleName + "Alt" + a.getId();
        w.println("  public boolean unparse"+ altName + "( " + ruleName + " obj, PrintWriter w ) {");

        w.println("    ");

        w.println( "    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();\n" );
        w.println( "    PrintWriter internalW = new PrintWriter(output);" );

        for(UPElement e : a.getElements()) {
            if(e instanceof UPSubRuleElement) {
                UPSubRuleElement sre = (UPSubRuleElement) e;
                w.println("    unparse"+ altName + "SubRule" + sre.getId() + "( obj, internalW );");
            } else if(e instanceof UPNamedSubRuleElement) {
                UPNamedSubRuleElement sre = (UPNamedSubRuleElement) e;
                w.println("    internalW.print( convertToString( obj.get"+ StringUtil.firstToUpper(sre.getName())+"() ) );");
            } else if(e instanceof UPNamedElement) {
                UPNamedElement ne = (UPNamedElement) e;
                w.println("    internalW.print( convertToString( obj.get"+ StringUtil.firstToUpper(ne.getName())+"() ) );");
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

                w.println("    internalW.println( "+eText + " + \" \" );");
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
                generateAltCode(w, altName + "SubRule" + sr.getId(), sa);
            }

            generateSubRuleCode(ruleName, altName, sr, w);
        });
    }

}
