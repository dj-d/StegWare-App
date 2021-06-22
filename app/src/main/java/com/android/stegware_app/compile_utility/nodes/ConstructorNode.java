package com.android.stegware_app.compile_utility.nodes;

import com.android.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class ConstructorNode extends AbstractNode {
    public String signature;
    public String body;

    public ConstructorNode(AbstractNode parent, String signature, String body) {
        super(parent);

        this.signature = signature;
        this.body = body;
    }

    @Override
    public String toString() {
        return this.signature + this.body;
    }
}
