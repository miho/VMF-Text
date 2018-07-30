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
package eu.mihosoft.vmftext.tests.miniclang;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.List;
import java.util.Optional;

public class ControlFlowDelegate implements DelegatedBehavior<ControlFlowScope> {
    private ControlFlowScope scope;

    @Override
    public void setCaller(ControlFlowScope caller) {
        this.scope = caller;
    }

    /**
     * Resolves a variable by name.
     * @param name variable name
     * @return the variable if present; empty optional otherwise
     */
    public Optional<DeclStatement> resolveVariable(String name) {

        // list of scopes to visit (order is current to root)
        List<ControlFlowScope> scopes = scope.parentScopes();
        scopes.add(0, scope);

        // for each scope check whether it has a declaration that matches the
        // requested variable (by name) and return it if is present
        for (ControlFlowScope cF : scopes) {
            Optional<DeclStatement> variableDecl = cF.vmf().content().stream(DeclStatement.class).
                    filter(declStatement ->
                            name.equals(declStatement.getVarName())
                    ).findFirst();

            if (variableDecl.isPresent()) {
                return variableDecl;
            }
        }

        // if the root scope is not a function declaration we return early since there's
        // nothing to check
        ControlFlowScope rootScope = scopes.get(scopes.size() - 1);
        if (!(rootScope instanceof FunctionDecl)) return Optional.empty();

        // check function parameters
        FunctionDecl fDecl = (FunctionDecl) rootScope;
        Optional<? extends DeclStatement> paramDecl =
                fDecl.getParams().stream().filter(parameter -> name.equals(parameter.getVarName()))
                        .findFirst();

        // return if parameter matches (by name)
        if (paramDecl.isPresent()) {
            return (Optional<DeclStatement>) paramDecl;
        }

        // nothing found
        return Optional.empty();
    }

}
