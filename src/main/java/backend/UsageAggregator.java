package backend;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.java.ImportStatementElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.UsageToPsiElementProvider;
import com.intellij.psi.*;

import java.util.*;

public class UsageAggregator {


    // If we set the similarity threshold to 0.25, usages that are more than 25% similar would be assigned to the same group.
    private final static double MIN_SIMLIAR_THRESHOLD = 0.75;
    private List<UsageGroupAst> astSimilarityList = new LinkedList<>();
    private volatile int groupCount = 1;
    private static final Language JAVA = Language.findLanguageByID("JAVA");
    private static final int MAX_HOPES = 17;

    public synchronized UsageGroup getAggregateUsage(Usage usage) {

        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        PsiElement codeBlockPsiElement = getFirstCodeBlockParent(usageInfo.getElement());
        if (codeBlockPsiElement == null) return null;
//        if (codeBlockSet.contains(codeBlockPsiElement.getText())) {
//            return null;
//        } else {
//            codeBlockSet.add(codeBlockPsiElement.getText());
//        }
        CodeBlock codeBlock = new CodeBlock(codeBlockPsiElement);
        return cluster(codeBlock);
    }

    public synchronized UsageGroup getAggregateUsage2(Usage usage) {

        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        final PsiElement psiElement = UsageToPsiElementProvider.findAppropriateParentFrom(usageInfo.getElement());
        if (psiElement == null) return null;
//        if (codeBlockSet.contains(codeBlockPsiElement.getText())) {
//            return null;
//        } else {
//            codeBlockSet.add(codeBlockPsiElement.getText());
//        }
        CodeBlock codeBlock = new CodeBlock(psiElement);
        return cluster(codeBlock);
    }


    public synchronized UsageGroup getAggregateUsageBasedOnStatement(Usage usage) {

        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        final PsiElement psiElement = findStatementFrom(usageInfo.getElement());
        if (psiElement == null) return null;
        CodeBlock codeBlock = new CodeBlock(psiElement);
        return cluster(codeBlock);
    }

    //    It first finds the minimum similarity between the usage and all members of all clusters separately and memoizes them.
//    Based on max of mins similarity, it chooses the best cluster.
    private synchronized UniqueUsageGroup cluster(CodeBlock codeBlock) {
        double highestSimilarityRating = 0.0;
        // find the most similar usage to current code block
        Optional<UsageGroupAst> mostSimilarGroup = astSimilarityList.stream()
                .max((a, b) ->
                        (int) (10000 * (a.getSimilarityToMostSimilarUsageOfThisGroup(codeBlock) - b.getSimilarityToMostSimilarUsageOfThisGroup(codeBlock))));
//                        (int) (10000 * (a.getSimilarityToAverageOfThisGroup(codeBlock) - b.getSimilarityToAverageOfThisGroup(codeBlock))));

        if (mostSimilarGroup.isPresent()) {
            // calculate similarity score to the most similar usage in all usages
//            highestSimilarityRating = mostSimilarGroup.get().getSimilarityToAverageOfThisGroup(codeBlock);
            highestSimilarityRating = mostSimilarGroup.get().getSimilarityToMostSimilarUsageOfThisGroup(codeBlock);
        }
        // Check if returning exact match because classic find usages is weird.
        if (highestSimilarityRating > 1.0) {
            throw new RuntimeException("This should never happen!");
        }
        // it found the cluster which can be added.
        if (highestSimilarityRating > MIN_SIMLIAR_THRESHOLD) {
            mostSimilarGroup.get().getElements().add(codeBlock);
            mostSimilarGroup.get().getGroup().incrementUsageCount();
            return mostSimilarGroup.get().getGroup();
        } else {
            // Create and return a code block usage key
            UniqueUsageGroup newAstKey = new UniqueUsageGroup("Usage Group #"+groupCount++);
            newAstKey.incrementUsageCount();
            astSimilarityList.add(new UsageGroupAst(codeBlock, newAstKey));
            return newAstKey;
        }
    }

    private synchronized PsiElement getFirstCodeBlockParent(PsiElement element) {
        while (element != null && !element.toString().contains("PsiCodeBlock")) {
            element = element.getContext();
        }
//        PsiElement codeBlockPsiElement = PsiTreeUtil.getParentOfType(usageInfo.getElement(), PsiCodeBlock.class);
        return element;
    }

    private synchronized PsiElement findStatementFrom(PsiElement element) {
        if (element.getLanguage() == JAVA) {
            int hopes = 0;
            while (hopes++ < MAX_HOPES && element != null) {
                if (element.toString().endsWith("Statement") ||
                        element instanceof PsiField ||
                        element instanceof PsiClassInitializer ||
                        element instanceof PsiMethod ||
                        element instanceof ImportStatementElement ||
                        element instanceof PsiClass
                ) return element;
                if(element instanceof PsiDocComment) return  null;

                element = element.getParent();
            }
        }
        return null;
    }
}
