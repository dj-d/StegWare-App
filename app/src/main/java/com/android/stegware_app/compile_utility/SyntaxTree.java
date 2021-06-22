package com.android.stegware_app.compile_utility;

import com.android.stegware_app.compile_utility.nodes.RootNode;
import com.android.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class SyntaxTree {
    public AbstractNode root;

    public SyntaxTree() {
        this.root = new RootNode();
    }

    public AbstractNode getRoot() {
        return root;
    }


    @Override
    public String toString() {
        return root.toString();
    }
}
