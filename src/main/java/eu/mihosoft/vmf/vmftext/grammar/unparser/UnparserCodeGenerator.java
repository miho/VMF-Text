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
            int aCounter = 0;
            for(AlternativeBase a : r.getAlternatives()) {

                String altName = ruleName + "Alt" + a.getId();

                w.println("    if( match" + altName + "( obj, w ) ) {");

                w.println("      unparse" + altName + "( obj, w );");

                w.print("    }");

                if(aCounter < r.getAlternatives().size() -1 ) {
                    w.print(" else ");
                } else {
                    w.println();
                }

                aCounter++;
            }
            w.println("  }");

            for(AlternativeBase a : r.getAlternatives()) {
                String altName = ruleName + "Alt" + a.getId();
                w.println("  public String unparse"+ altName + "( " + ruleName + " obj, PrintWriter w) {");

                w.println("    ");

                w.print( "    return " );

                int eCount = 0;
                for(UPElement e : a.getElements()) {
                    if(e instanceof UPSubRuleElement) {
                        UPSubRuleElement sre = (UPSubRuleElement) e;
                        w.print("unparse"+ altName + "SubRule" + sre.getId() + "(obj, w) ");
                    } else if(e instanceof UPNamedSubRuleElement) {
                        UPNamedSubRuleElement sre = (UPNamedSubRuleElement) e;
                        w.print("unparse"+ altName + "SubRule" + sre.getId() + "(obj, w) ");
                    } else if(e instanceof  UPNamedElement) {
                        UPNamedElement ne = (UPNamedElement) e;
                        w.print("convertToString( obj.get"+StringUtil.firstToUpper(ne.getName())+"() )");
                    } else {
                        w.print(""+e.getText().replace('\'', '"') + " + \" \"");
                    }

                    if(eCount < a.getElements().size()-1) {
                        w.print(" + ");
                    } else {
                        w.println(";");
                    }

                    eCount++;
                }

                w.println("\n  }");

                a.getElements().stream().filter(el->el instanceof SubRule).map(el->(SubRule)el).forEach(sr-> {
                    w.println("  public void unparse" + altName + "SubRule" + sr.getId() + "( " + ruleName + " obj, PrintWriter w) {");
                    w.println();
                    w.println("  }");
                });
            }


            w.println("}");
        }
    }

    String getAlternativeCode(){return null;}

}
