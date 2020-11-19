package com.android.stegware_app.compile_utility.nodes.abstracts;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractNode {
    protected AbstractNode parent;
    public List<AbstractNode> children;

    public AbstractNode(AbstractNode _parent) {
        this.parent = _parent;
        this.children = new ArrayList<>();
    }

    /**
     *
     *
     * @param child
     */
    public void addChild(AbstractNode child) {
        this.children.add(child);
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