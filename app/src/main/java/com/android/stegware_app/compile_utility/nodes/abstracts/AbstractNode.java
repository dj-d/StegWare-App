package com.android.stegware_app.compile_utility.nodes.abstracts;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNode {
    AbstractNode parent;
    public List<AbstractNode> children;

    public AbstractNode(AbstractNode parent) {
        this.parent = parent;
        this.children = new ArrayList<>();
    }

    public void addChild(AbstractNode child) {
        this.children.add(child);
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