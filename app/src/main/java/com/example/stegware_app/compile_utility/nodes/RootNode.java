package com.example.stegware_app.compile_utility.nodes;

import androidx.annotation.NonNull;

import com.example.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class RootNode extends AbstractNode {

    public RootNode() {
        super(null);
    }

    public RootNode(AbstractNode _parent) {
        super(_parent);
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        for (int i = 0; i < children.size(); i++) {
            stringBuilder.append(children.get(i).toString()).append(" ");
        }

        return stringBuilder.toString();
    }
}
