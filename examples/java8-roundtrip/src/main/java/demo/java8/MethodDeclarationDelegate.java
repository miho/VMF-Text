/*
 * Copyright 2017-2026 Michael Hoffer <info@michaelhoffer.de>. All rights reserved.
 * Licensed under the Apache License, Version 2.0.
 */
package demo.java8;

import eu.mihosoft.vmf.runtime.core.DelegatedBehavior;

public class MethodDeclarationDelegate implements DelegatedBehavior<MethodDeclaration> {

    private MethodDeclaration caller;

    @Override
    public void setCaller(MethodDeclaration caller) {
        this.caller = caller;
    }

    public boolean returnsVoid() {
        return caller.getType().getVoidType();
    }
}
