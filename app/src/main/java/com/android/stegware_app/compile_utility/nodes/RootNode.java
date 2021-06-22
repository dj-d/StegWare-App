package com.android.stegware_app.compile_utility.nodes;

import com.android.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class RootNode extends AbstractNode {
    public RootNode() {
        super(null);
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < children.size(); i++) {
            stringBuilder.append(children.get(i).toString()).append(" ");
        }
        return stringBuilder.toString();
    }
}
