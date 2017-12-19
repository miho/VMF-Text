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

        // List<Alter>

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
                String altName = ruleName + "Alt" + a.getId();
                w.println("  public boolean unparse"+ altName + "( " + ruleName + " obj, PrintWriter w) {");

                w.println("    ");

                w.println( "    java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();" );
                w.println( "    PrintWriter internalW = new PrintWriter(output);" );

                for(UPElement e : a.getElements()) {
                    if(e instanceof UPSubRuleElement) {
                        UPSubRuleElement sre = (UPSubRuleElement) e;
                        w.println("    unparse"+ altName + "SubRule" + sre.getId() + "( obj, internalW );");
                    } else if(e instanceof UPNamedSubRuleElement) {
                        UPNamedSubRuleElement sre = (UPNamedSubRuleElement) e;
                        w.println("    unparse"+ altName + "SubRule" + sre.getId() + "( obj, internalW );");
                    } else if(e instanceof  UPNamedElement) {
                        UPNamedElement ne = (UPNamedElement) e;
                        w.println("    internalW.print( convertToString( obj.get"+StringUtil.firstToUpper(ne.getName())+"() ) );");
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

                w.println("    return match"+altName+"(s);");

                w.println("\n  }");

                a.getElements().stream().filter(el->el instanceof SubRule).map(el->(SubRule)el).forEach(sr-> {
                    w.println("  public void unparse" + altName + "SubRule" + sr.getId() + "( " + ruleName + " obj, PrintWriter w) {");

                    for(AlternativeBase sa : sr.getAlternatives()) {

                        String altSubName = ruleName + "SubRule" + "Alt" + sa.getId();

                        w.println("    if( unparse" + altSubName + "( obj, w )) { return; }");

                    }
                    w.println("  }");
                });
            }


            w.println("}");
        }
    }

    String getAlternativeCode(){return null;}

}
