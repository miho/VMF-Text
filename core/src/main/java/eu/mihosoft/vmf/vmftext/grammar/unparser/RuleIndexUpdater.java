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
package eu.mihosoft.vmf.vmftext.grammar.unparser;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;
import eu.mihosoft.vmf.vmftext.grammar.UPRule;
import eu.mihosoft.vmf.vmftext.grammar.UnparserModel;

public class RuleIndexUpdater implements DelegatedBehavior<UnparserModel>{
    private UnparserModel caller;

    @Override
    public void setCaller(UnparserModel caller) {
        this.caller = caller;
    }

    public void onUnparserModelInstantiated() {
        caller.getRules().addChangeListener(evt -> {

            // update id of every rule that has been added to the list
            for(int i = 0; i < evt.added().indices().length;i++) {
                UPRule r =evt.added().elements().get(i);
                r.setRuleId(evt.added().indices()[i]);
            }

        });
    }
}
