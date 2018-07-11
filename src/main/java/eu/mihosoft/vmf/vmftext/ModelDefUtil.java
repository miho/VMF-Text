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
package eu.mihosoft.vmf.vmftext;



import eu.mihosoft.vmf.vmftext.grammar.Mapping;
import eu.mihosoft.vmf.vmftext.grammar.TypeMapping;
import eu.mihosoft.vmf.vmftext.grammar.TypeMappings;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Lexer;
import eu.mihosoft.vmf.vmftext.grammar.antlr4.ANTLRv4Parser;
import eu.mihosoft.vmf.vmftext.grammar.java9.Java9BaseListener;
import eu.mihosoft.vmf.vmftext.grammar.java9.Java9Lexer;
import eu.mihosoft.vmf.vmftext.grammar.java9.Java9Parser;
import eu.mihosoft.vmf.vmftext.grammar.typemapping.TypeMappingBaseListener;
import eu.mihosoft.vmf.vmftext.grammar.typemapping.TypeMappingLexer;
import eu.mihosoft.vmf.vmftext.grammar.typemapping.TypeMappingParser;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

final class ModelDefUtil {
    private ModelDefUtil() {
        throw new AssertionError("Don't instantiate me!");
    }

    public static List<String> getNamesOfDefinedInterfaces(String code) {

        List<String> result = new ArrayList<>();

        Java9BaseListener l = new Java9BaseListener() {

            private String packageName = "";

            @Override
            public void enterPackageDeclaration(Java9Parser.PackageDeclarationContext ctx) {
                packageName = ctx.packageName().getText();
            }

            @Override
            public void enterInterfaceDeclaration(Java9Parser.InterfaceDeclarationContext ctx) {
                result.add(packageName + "."+ctx.normalInterfaceDeclaration().Identifier().getText());
            }
        };

        CharStream input = CharStreams.fromString(code);

        Java9Lexer lexer = new Java9Lexer(input);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        Java9Parser parser = new Java9Parser(tokens);

        ParserRuleContext tree = parser.compilationUnit();

        ParseTreeWalker walker = new ParseTreeWalker();

        walker.walk(l, tree);

        return result;
    }

}
