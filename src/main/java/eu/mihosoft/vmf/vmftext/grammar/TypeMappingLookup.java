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
package eu.mihosoft.vmf.vmftext.grammar;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

import java.util.Objects;
import java.util.Optional;

public class TypeMappingLookup implements DelegatedBehavior<TypeMapping>{
    private TypeMapping caller;

    @Override
    public void setCaller(TypeMapping caller) {
        this.caller = caller;
    }

    /**
     * Returns the specified mapping by rule name.
     *
     * @param paramRuleName name of the lexer rule that parses the parameter (conversion is applied to the lexer rule. e.g. ANTLR token instance)
     * @return the specified mapping
     */
    public Optional<Mapping> mappingByRuleName(String paramRuleName) {
        return caller.getEntries().stream().filter(mE-> Objects.equals(mE.getRuleName(), paramRuleName)).findFirst();
    }
}
