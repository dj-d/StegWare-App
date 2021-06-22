package com.android.stegware_app.compile_utility.nodes;

import com.android.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class StatementNode extends AbstractNode {
    public String code;

    public StatementNode(AbstractNode parent, String code) {
        super(parent);
        this.code = code;
    }

    @Override
    public String toString() {
        return this.code + " ";
    }
}
