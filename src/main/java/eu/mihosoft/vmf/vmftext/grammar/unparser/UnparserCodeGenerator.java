package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.core.io.Resource;
import eu.mihosoft.vmf.vmftext.StringUtil;
import eu.mihosoft.vmf.vmftext.grammar.AlternativeBase;
import eu.mihosoft.vmf.vmftext.grammar.UPRule;
import eu.mihosoft.vmf.vmftext.grammar.UnparserModel;

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
                w.println("  public void "+"match"+ altName + "( obj, PrintWriter w ) {");

                w.println();

                w.println("  }");
            }


            w.println("}");
        }
    }

    String getAlternativeCode(){return null;}

}
