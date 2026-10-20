package py3trial;

import py3trial.parser.Python3ModelParser;
import py3trial.parser.Python3TriviaSupport;
import py3trial.unparser.Formatter;
import py3trial.unparser.Python3ModelUnparser;

/**
 * Edit experiment: move a top-level statement into a function body. Python's
 * indentation is structural (INDENT/DEDENT tokens), so a moved statement that
 * keeps its own trivia lands at the wrong depth.
 */
public class Edit {
    public static void main(String[] args) {
        String nl = String.valueOf((char) 10);
        Python3Model target = new Python3ModelParser().parse("def f():" + nl + "    return 1" + nl + "print(f())" + nl);
        Python3Model donor = new Python3ModelParser().parse("x = 2" + nl);

        Funcdef fd = target.getRoot().vmf().content().stream(Funcdef.class).findFirst().get();
        BlockAlt2 block = (BlockAlt2) fd.getBlock();
        Stmt moved = ((File_inputChoiceAlt2) donor.getRoot().getFile_inputChoices().get(0)).getStmt();
        block.getStmts().add(moved);

        System.out.println("moved as is (default formatter):");
        System.out.println(show(new Python3ModelUnparser().unparse(target)));
        // the moved statement came from depth 0; the function body is at depth 1
        Python3TriviaSupport.reindent(moved, "", "    ");
        System.out.println("after Python3TriviaSupport.reindent(moved, \"\", \"    \"):");
        System.out.println(show(new Python3ModelUnparser().unparse(target)));
        Python3ModelUnparser u = new Python3ModelUnparser();
        u.setFormatter(Formatter.newBlockIndentFormatter());
        System.out.println("programmatic ranges with the block-indent formatter: " + u.getFormatter().programmaticRanges().size());
    }

    static String show(String s) {
        StringBuilder sb = new StringBuilder();
        for (String line : s.split(String.valueOf((char) 10), -1)) {
            sb.append("    |").append(line.replace("\t", "<TAB>")).append("|").append((char) 10);
        }
        return sb.toString();
    }
}
