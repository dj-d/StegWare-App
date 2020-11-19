package com.android.stegware_app.compile_utility.nodes;

import androidx.annotation.NonNull;

import com.android.stegware_app.compile_utility.nodes.abstracts.AbstractNode;


public class ClassNode extends AbstractNode {
    public static final String CLASS_KEYWORD = "class ";

    private String signature;

    private String modifier;

    public String className;
    private String extendsClassName;
    private String implementsClassName;

    public ClassNode(AbstractNode _parent, String _signature) {
        super(_parent);

        String[] signatureWords = _signature.split(" ");

        // Find class word
        int i = 0;

        while (i < signatureWords.length && !signatureWords[i].equals("class")) {
            i++;
        }

        // Read all before class word - public or private
        this.modifier = "";

        for (int j = 0; j < i; j++) {
            this.modifier += signatureWords[j];
        }

        this.className = signatureWords[i + 1];

        // After class name there could be extends
        int k = i + 1;

        while (k < signatureWords.length && !signatureWords[k].equals("extends")) {
            k++;
        }

        if (k < signatureWords.length) {
            this.extendsClassName = signatureWords[k + 1];
        }

        // After class name there could be implements
        k = i + 1;

        while (k < signatureWords.length && !signatureWords[k].equals("implements")) {
            k++;
        }

        if (k < signatureWords.length) {
            this.implementsClassName = signatureWords[k + 1];
        }
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();

        stringBuilder.append(this.modifier);
        stringBuilder.append(" ");
        stringBuilder.append(CLASS_KEYWORD);
        stringBuilder.append(" ");
        stringBuilder.append(this.className);
        stringBuilder.append("{");

        for (int i = 0; i < children.size(); i++) {
            stringBuilder.append(children.get(i).toString()).append(" ");
        }

        stringBuilder.append("}");

        return stringBuilder.toString();
    }
}
