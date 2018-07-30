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

import eu.mihosoft.vcollections.VList;
import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.runtime.core.VObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ControlFlowChildNodeDelegate implements DelegatedBehavior<ControlFlowChildNode> {
    private ControlFlowChildNode scope;

    @Override
    public void setCaller(ControlFlowChildNode caller) {
        this.scope = caller;
    }

    /**
     * Returns a list of all ancestors of this element.
     *
     * @return a list of all ancestors of this element
     */
    public VList<ControlFlowScope> parentScopes() {
        List<ControlFlowScope> ancestors = new ArrayList<>();

        ControlFlowScope parent = getParentScope(scope).orElse(null);

        while (parent != null) {
            ancestors.add(parent);
            parent = getParentScope(parent).orElse(null);
        }

        Collections.reverse(ancestors);

        return VList.newInstance(ancestors);
    }

    /**
     * Returns parent scope of the current code element.
     * @param cF code element
     * @return parent scope if present; empty optional otherwise
     */
    private static Optional<ControlFlowScope> getParentScope(VObject cF) {
        VObject obj = cF;
        for (VObject parent : obj.vmf().content().referencedBy()) {
            if (parent instanceof ControlFlowScope) {
                return Optional.of((ControlFlowScope) parent);
            }
            Optional<ControlFlowScope> pScope = getParentScope(parent);
            if (pScope.isPresent()) {
                return pScope;
            }
        }

        return Optional.empty();
    }
    
    
}
