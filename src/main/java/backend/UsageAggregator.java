package backend;

import com.intellij.ide.scratch.RootType;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.lang.LangBundle;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.CompositePsiElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;
import com.intellij.usages.impl.rules.UsageType;
import com.intellij.usages.impl.rules.UsageTypeProvider;
import com.intellij.util.containers.ConcurrentFactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentMap;

public class UsageAggregator {

    // If we set the similarity threshold to 0.25, usages that are more than 75% similar would be assigned to the same group.
    private final static double MIN_SIMLIAR_THRESHOLD = 0.70;
    private List<UsageGroupAst> astSimilarityList = new LinkedList<>();
    private int groupCount = 1;
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
//        String parentType = codeBlockPsiElement.getContext()
//        @NotNull IElementType parentType=  ((CompositePsiElement) codeBlockPsiElement.getContext()).getElementType();
        String parentType = findParentType(codeBlockPsiElement);
        CodeBlock codeBlock = new CodeBlock(codeBlockPsiElement, parentType);
//        return cluster(codeBlock);
        return clusterBasedOnStatement(codeBlock);
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

            UniqueUsageGroup newAstKey = new UniqueUsageGroup(codeBlock.getParentType());
            astSimilarityList.add(new UsageGroupAst(codeBlock, newAstKey));
            return newAstKey;
        }
    }

    private synchronized PsiElement getFirstCodeBlockParent(PsiElement element) {
//        while (element != null && !element.toString().contains("PsiCodeBlock")) {
        while (element != null && !element.toString().contains("PsiCodeBlock")) {
            if ("IF_STATEMENT".equals(element.getNode().getElementType().toString())) {
                return element;
            }
            if(element.toString().contains("PsiJavaFile")) break;
            element = element.getContext();
        }
//        PsiElement codeBlockPsiElement = PsiTreeUtil.getParentOfType(usageInfo.getElement(), PsiCodeBlock.class);
        return element;
    }


    private String findParentType(PsiElement codeBlockPsiElement) {
        @NotNull IElementType parentType;
        if(codeBlockPsiElement.getNode().getElementType().toString().equals("java.FILE")) return "in class";
        while (codeBlockPsiElement != null
                && !codeBlockPsiElement.getNode().getElementType().toString().equals("IF_STATEMENT")
                && !codeBlockPsiElement.getNode().getElementType().toString().equals("TRY_STATEMENT")
                && !codeBlockPsiElement.getNode().getElementType().toString().equals("FOR_STATEMENT")
                && !codeBlockPsiElement.getNode().getElementType().toString().equals("WHILE_STATEMENT")
        ) {
            codeBlockPsiElement = codeBlockPsiElement.getContext();
        }
        if (codeBlockPsiElement == null) return "in method body";
        parentType = codeBlockPsiElement.getNode().getElementType();
        switch (parentType.toString()) {
            case "IF_STATEMENT":
                return "in IF/ELSE";
            case "FOR_STATEMENT":
            case "WHILE_STATEMENT":
                return "in loop";
            case "METHOD":
                return "in method body";


            case "TRY_STATEMENT":
                return "In try catch";
        }
        return parentType.toString();
    }

    private synchronized UniqueUsageGroup clusterBasedOnStatement(CodeBlock codeBlock) {
        String usageType = codeBlock.getParentType();
        UsageGroupAst usageAst = astSimilarityList.stream().filter(usagetGroupAst -> usagetGroupAst.getGroup().getUsageDisplayed().equals(usageType)).
                findAny().orElse(null);

        // it found the cluster which can be added.
        if (usageAst !=null) {
            usageAst.getElements().add(codeBlock);
            usageAst.getGroup().incrementUsageCount();
            return usageAst.getGroup();
        } else {
            // Create and return a code block usage key

            UniqueUsageGroup newAstKey = new UniqueUsageGroup(codeBlock.getParentType());
            astSimilarityList.add(new UsageGroupAst(codeBlock, newAstKey));
            return newAstKey;
        }
    }


}
