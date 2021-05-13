package backend;

import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;

import java.util.*;

public class UsageAggregator {

    // If we set the similarity threshold to 0.25, usages that are more than 25% similar would be assigned to the same group.
    private final static double MIN_SIMLIAR_THRESHOLD = 0.25;
    private List<UsageGroupAst> astSimilarityList = new LinkedList<>();
    private int groupCount=1;
//    private Set<String> codeBlockSet = new HashSet<>();

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

//    It first finds the minimum similarity between the usage and all members of all clusters separately and memoizes them.
//    Based on max of mins similarity, it chooses the best cluster.
    private synchronized UniqueUsageGroup cluster(CodeBlock codeBlock) {
        double highestSimilarityRating = 0.0;
        // find the most similar usage to current code block
        Optional<UsageGroupAst>  mostSimilarGroup = astSimilarityList.parallelStream()
                .max((a, b) ->
                        (int) (10000 * (a.getMinimumSimilarityTo(codeBlock) - b.getMinimumSimilarityTo(codeBlock))));

        if (mostSimilarGroup.isPresent()) {
             // calculate similarity score to the most similar usage in all usages
            highestSimilarityRating = mostSimilarGroup.get().getMinimumSimilarityTo(codeBlock);
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
            UniqueUsageGroup newAstKey = new UniqueUsageGroup("Usage Group ");
            astSimilarityList.add(new UsageGroupAst(codeBlock, newAstKey));
            return newAstKey;
        }
    }

    private PsiElement getFirstCodeBlockParent(PsiElement element) {
        while (element != null && !element.toString().contains("PsiCodeBlock")) {
            element = element.getContext();
        }
//        PsiElement codeBlockPsiElement = PsiTreeUtil.getParentOfType(usageInfo.getElement(), PsiCodeBlock.class);
        return element;
    }
}
