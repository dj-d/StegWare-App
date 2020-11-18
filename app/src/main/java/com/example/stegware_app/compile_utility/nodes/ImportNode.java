package com.example.stegware_app.compile_utility.nodes;

import androidx.annotation.NonNull;

import com.example.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

public class ImportNode extends AbstractNode {
    public static final String IMPORT_KEY_WORD = "import ";

    public String packagePath;
    private String packageName;

    public ImportNode(AbstractNode _parent, String _statement) {
        super(_parent);

        int indexImportKeyword = _statement.indexOf(IMPORT_KEY_WORD) + IMPORT_KEY_WORD.length();
        int lastDotIndex = _statement.lastIndexOf(".");

        this.packagePath = _statement.substring(indexImportKeyword, lastDotIndex);
        this.packageName = _statement.substring(lastDotIndex + 1);
    }

    @NonNull
    @Override
    public String toString() {
        return IMPORT_KEY_WORD + this.packagePath + "." + this.packageName + ";";
    }
}
