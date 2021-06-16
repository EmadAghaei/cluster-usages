package backend;

import com.intellij.psi.PsiElement;
import com.intellij.usages.PsiElementUsageTarget;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.jetbrains.annotations.NotNull;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

public class CodeBlock {
    private PsiElement psiElement;
    private String parentType= "Usage Group ";
    private CtClass<?> ctClass;
    private final static AstComparator AST_COMPARATOR = new AstComparator();


    public CodeBlock(PsiElement psiElement, String parentType) {
        this.psiElement = psiElement;
        this.parentType = parentType;
        String fakeBeginStub = "class clazz {";
        String fakeEndStub = "}";
        try {
            this.ctClass = Launcher.parseClass(fakeBeginStub + psiElement.getText() + fakeEndStub);
        } catch (Exception ex) {
            ex.printStackTrace();
            System.out.println("\n ---" + psiElement.getText());
        }

    }

    public String getParentType() {
        return parentType;
    }
    public void setParentType(String parentType) {
        this.parentType = parentType;
    }
    public PsiElement getPsiElement() {
        return psiElement;
    }

    public void setPsiElement(PsiElement psiElement) {
        this.psiElement = psiElement;
    }

    public String getCode() {
        return psiElement.getText();
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
