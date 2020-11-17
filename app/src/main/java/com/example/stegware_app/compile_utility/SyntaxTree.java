package com.example.stegware_app.compile_utility;

import androidx.annotation.NonNull;

import com.example.stegware_app.compile_utility.nodes.RootNode;
import com.example.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class SyntaxTree {
    public AbstractNode root;

    public SyntaxTree() {
        this.root = new RootNode();
    }

    public AbstractNode getRoot() {
        return root;
    }

    @NonNull
    @Override
    public String toString() {
        return root.toString();
    }
}
