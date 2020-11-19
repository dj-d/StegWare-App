package com.android.stegware_app.compile_utility.nodes;

import androidx.annotation.NonNull;

import com.android.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class ConstructorNode extends AbstractNode {
    private String signature;
    public String body;

    public ConstructorNode(AbstractNode _parent, String _signature, String _body) {
        super(_parent);

        this.signature = _signature;
        this.body = _body;
    }

    @NonNull
    @Override
    public String toString() {
        return this.signature + this.body;
    }
}