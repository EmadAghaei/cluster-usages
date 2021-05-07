package backend;

import com.github.gumtreediff.matchers.Mapping;
import com.intellij.psi.PsiElement;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.jetbrains.annotations.NotNull;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

import java.util.Set;

public class CodeBlock {
    private PsiElement codeBlock;
    private CtClass<?> ctClass;
    private final static AstComparator AST_COMPARATOR = new AstComparator();

    public CodeBlock(PsiElement codeBlock) {
        this.codeBlock = codeBlock;
//        String fakeBeginStub = String.format("class %s { ", clazzName);
        String fakeBeginStub = "class clazz {";
        String fakeEndStub = "\n}";
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
            int thisNodesCount = this.ctClass.filterChildren(null).list().size();
            int codeBlockNodesCount = codeBlock.ctClass.filterChildren(null).list().size();
            Diff astDiff = AST_COMPARATOR.compare(this.ctClass, codeBlock.ctClass);
            Set<Mapping> similiarities = astDiff.getMappingsComp().asSet();
            // normalize similarity
            return 2 * similiarities.size() /
                    (double) (2 * similiarities.size() + thisNodesCount + codeBlockNodesCount);
        } catch (Exception e) {
            System.out.println(codeBlock.toString());
            e.printStackTrace();
            throw new RuntimeException();
        }


//            double editScriptSize = astDiff.getRootOperations().size();
//          double normalizer = Double.max(myFakeASTNodeCount, otherFakeASTNodeCount);

//            double alpha = 1 - (editScriptSize / normalizer);

//            List<Operation> differences = astDiff.getRootOperations();


//            alpha = 1 - differences.size() / Double.max(myFakeAST.length())

//            double percentageSimilar = ((double) similiarities.size()) / (differences.size() + similiarities.size());
//            return percentageSimilar;

    }

}
