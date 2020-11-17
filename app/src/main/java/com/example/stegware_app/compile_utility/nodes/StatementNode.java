package com.example.stegware_app.compile_utility.nodes;

import androidx.annotation.NonNull;

import com.example.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class StatementNode extends AbstractNode {
    private String code;

    public StatementNode(AbstractNode _parent, String _code) {
        super(_parent);

        this.code = _code;
    }

    @NonNull
    @Override
    public String toString() {
        return this.code + " ";
    }
}
