package backend;

import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;

import java.util.*;

public class UsageAggregator {

    // If we set the similarity threshold to 0.25, usages that are less than 75% similar are unlikely to be assigned to the same group.
    // If we set the SIMILIAR_THRESHOLD =0 we can say usages less than 100% similar usages (identical usages) are placed to different groups.
    private final static double SIMILIAR_THRESHOLD = 0.15; // the threshold is inverted and means allowed difference between usages within one group.
    private List<UsageGroupAst> astSimilarityList = new LinkedList<>();
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
        Optional<UsageGroupAst> mostSimilarCodeBlock;
        double highestSimilarityRating = 0.0;
        // find the most similar usage to current code block
        mostSimilarCodeBlock = astSimilarityList.parallelStream()
                .max((a, b) ->
                        (int) (1000 * (a.getMinimumSimilarityTo(codeBlock) - b.getMinimumSimilarityTo(codeBlock))));

        if (mostSimilarCodeBlock.isPresent()) {
             // calculate similarity score to the most similar usage in all usages
            highestSimilarityRating = mostSimilarCodeBlock.get().getMinimumSimilarityTo(codeBlock);
        }
        // Check if returning exact match because classic find usages is weird.
        if (highestSimilarityRating > 1.0) {
            throw new RuntimeException("This should never happen!");
        }
        if (highestSimilarityRating > SIMILIAR_THRESHOLD) {
            mostSimilarCodeBlock.get().getElements().add(codeBlock);
            mostSimilarCodeBlock.get().getGroup().incrementUsageCount();
            return mostSimilarCodeBlock.get().getGroup();
        } else {
            // Create and return a codeblock usage key
            UniqueUsageGroup newAstKey = new UniqueUsageGroup("Similar Usage Group");
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
