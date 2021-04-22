package backend;

import com.github.gumtreediff.matchers.Mapping;
import com.intellij.psi.PsiElement;
import gumtree.spoon.AstComparator;
import gumtree.spoon.diff.Diff;
import org.jetbrains.annotations.NotNull;
import spoon.Launcher;
import spoon.reflect.declaration.CtClass;

import java.util.Set;

public class CodeBlockUsage {
    private PsiElement codeBlock;
    private CtClass<?> ctClass;
    private final static AstComparator AST_COMPARATOR = new AstComparator();

    public CodeBlockUsage(PsiElement codeBlock, String clazzName) {
        this.codeBlock = codeBlock;
        String fakeBeginStub = String.format("class %s { ", clazzName);
        String fakeEndStub = "\n}";
        try {
            this.ctClass = Launcher.parseClass(fakeBeginStub + codeBlock.getText() + fakeEndStub);
        } catch (Exception ex){
//            System.out.println(clazzName + "\n ---"+ codeBlock.getText());
        }

    }

    public String getCode() {
        return codeBlock.getText();
    }

    @Override
    public String toString() {
        return "CodeBlockUsage{" +
                "codeBlock=" + codeBlock +
                ", ctClass=" + ctClass +
                '}';
    }

    public double compareSimilarity(@NotNull CodeBlockUsage o) {
        try {
            int myFakeASTNodeCount = this.ctClass.filterChildren(null).list().size();
            int otherFakeASTNodeCount = o.ctClass.filterChildren(null).list().size();
            Diff astDiff = AST_COMPARATOR.compare(this.ctClass, o.ctClass);
            Set<Mapping> similiarities = astDiff.getMappingsComp().asSet();
            double simarility = 2 * similiarities.size() /
                    (double) (2 * similiarities.size() + myFakeASTNodeCount + otherFakeASTNodeCount);
            return simarility;
        }catch (Exception e) {
            System.out.println(o.toString());
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
