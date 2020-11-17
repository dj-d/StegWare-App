package com.example.stegware_app.compile_utility;

import com.example.stegware_app.compile_utility.exceptions.InvalidSourceCodeException;
import com.example.stegware_app.compile_utility.exceptions.NotBalancedParenthesisException;
import com.example.stegware_app.compile_utility.nodes.ClassNode;
import com.example.stegware_app.compile_utility.nodes.ConstructorNode;
import com.example.stegware_app.compile_utility.nodes.ImportNode;
import com.example.stegware_app.compile_utility.nodes.MethodNode;
import com.example.stegware_app.compile_utility.nodes.StatementNode;
import com.example.stegware_app.compile_utility.nodes.abstracts.AbstractNode;

import java.util.Stack;

public class JavaParser {
    private String sourceCode;

    private SyntaxTree parsedFile;

    public JavaParser(String _sourceCode) throws InvalidSourceCodeException, NotBalancedParenthesisException {
        if (!JavaParser.areParenthesisBalanced(_sourceCode)) {
            throw new NotBalancedParenthesisException();
        }

        this.sourceCode = _sourceCode;
        this.parsedFile = new SyntaxTree();
        this.buildAST();
    }

    /**
     *
     *
     * @param c1
     * @param c2
     * @return
     */
    private static boolean isMatchingPair(char c1, char c2) {
        return (c1 == '(' && c2 == ')') || (c1 == '[' && c2 == ']') || (c1 == '{' && c2 == '}');
    }

    /**
     *
     *
     * @param sourceCode
     * @return
     */
    private static boolean areParenthesisBalanced(String sourceCode) {
        Stack<Character> stack = new Stack<>();

        for (int i = 0; i < sourceCode.length(); i++) {
            char c = sourceCode.charAt(i);

            if (c == '{' || c == '[' || c == '(') {
                stack.push(c);
            }

            if (c == '}' || c == ']' || c == ')') {
                if (stack.empty()) {
                    return false;
                }

                char prv = stack.pop();

                if (!isMatchingPair(prv, c)) {
                    return false;
                }
            }
        }

        return stack.empty();
    }

    /**
     *
     *
     * @param code
     * @param start
     * @return
     */
    private static int findEndOfBlock(String code, int start) {
        Stack<Character> stack = new Stack<>();

        while (code.charAt(start) != '{') {
            start++;
        }

        do {
            char c = code.charAt(start);

            if (c == '{' || c == '[' || c == '(') {
                stack.push(c);
            }

            if (c == '}' || c == ']' || c == ')') {
                stack.pop();
            }

            start++;
        } while (!stack.empty());

        return start;
    }

    /**
     *
     *
     * @param code
     * @param start
     * @param end
     * @param root
     * @return
     * @throws InvalidSourceCodeException
     */
    private AbstractNode parser(String code, int start, int end, AbstractNode root) throws InvalidSourceCodeException {
        Stack<Character> stack = new Stack<>();
        int i = start;

        while (i < end) {
            char c = code.charAt(i);

            if (c == ';') {
                StringBuilder a = new StringBuilder();

                while (!stack.empty()) {
                    a.append(stack.pop());
                }

                String statement = a.reverse().toString().trim();

                if (statement.startsWith("import ")) {
                    AbstractNode importNode = new ImportNode(root, statement);
                    root.addChild(importNode);
                } else {
                    AbstractNode statementNode = new StatementNode(root, statement);
                    root.addChild(statementNode);
                }
            } else if (c == '{') {
                StringBuilder signatureBuilder = new StringBuilder();

                while (!stack.empty()) {
                    signatureBuilder.append(stack.pop());
                }

                String signature = signatureBuilder.reverse().toString().trim();

                String[] signatureWords = signature.split(" ");
                int j = 0;

                while (j < signatureWords.length && !signatureWords[j].equals("class")) {
                    j++;
                }

                int endOfBlock = findEndOfBlock(code, i);

                if (j < signatureWords.length) {
                    AbstractNode classNode = new ClassNode(root, signature);
                    root.addChild(parser(code, i + 1, endOfBlock, classNode));
                } else if (root instanceof ClassNode) {
                    if (signature.contains(" " + ((ClassNode) root).className + "(") || signature.contains(" " + ((ClassNode) root).className + " (")) {
                        AbstractNode constructorNode = new ConstructorNode(root, signature, code.substring(i, endOfBlock));
                        root.children.add(constructorNode);
                    } else {
                        AbstractNode methodNode = new MethodNode(root, signature, code.substring(i, endOfBlock));
                        root.children.add(methodNode);
                    }
                } else {
                    throw new InvalidSourceCodeException();
                }

                i = endOfBlock;
            } else {
                stack.push(c);
            }

            i++;
        }

        return root;
    }

    /**
     *
     *
     * @throws InvalidSourceCodeException
     */
    private void buildAST() throws InvalidSourceCodeException {
        this.parsedFile.root = parser(this.sourceCode, 0, this.sourceCode.length(), this.parsedFile.getRoot());
    }
}
