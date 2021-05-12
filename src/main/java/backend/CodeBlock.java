package backend;

import com.intellij.psi.PsiElement;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.jetbrains.annotations.NotNull;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

public class CodeBlock {
    private PsiElement codeBlock;
    private CtClass<?> ctClass;
    private final static AstComparator AST_COMPARATOR = new AstComparator();

    public CodeBlock(PsiElement codeBlock) {
        this.codeBlock = codeBlock;
        String fakeBeginStub = "class clazz {";
        String fakeEndStub = "}";
        try {
            this.ctClass = Launcher.parseClass(fakeBeginStub + codeBlock.getText() + fakeEndStub);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("\n ---" + codeBlock.getText());
        }

    }

    public String getCode() {
        return codeBlock.getText();
    }

    @Override
    public String toString() {
        return "CodeBlockUsage{" +
                "codeBlock=" + getCode() +
                ", ctClass=" + ctClass +
                '}';
    }


    public double calculateSimilarityScore(@NotNull CodeBlock codeBlock) {
        try {
//            int thisNodesCount = this.ctClass.filterChildren(null).list().size();
//            int codeBlockNodesCount = codeBlock.ctClass.filterChildren(null).list().size();
            Diff astDiff = AST_COMPARATOR.compare(this.ctClass, codeBlock.ctClass);
            int diffCount = astDiff.getAllOperations().size();
            int similarityCount = astDiff.getMappingsComp().asSet().size();
            // normalize similarity
            return similarityCount / (double) (diffCount +similarityCount);
        } catch (Exception e) {
            System.out.println(codeBlock);
            e.printStackTrace();
            throw new RuntimeException();
        }
    }

}
