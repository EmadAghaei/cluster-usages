package backend;

import com.google.common.base.Joiner;
import com.intellij.lang.Language;
import com.intellij.openapi.util.Segment;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.java.ImportStatementElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.*;
import com.intellij.psi.*;
import com.intellij.usages.impl.rules.*;
import com.intellij.usages.rules.PsiElementUsage;
import gui.UsageGroupBundle;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class UsageAggregator {


    // If we set the similarity threshold to 0.25, usages that are more than 25% similar would be assigned to the same group.
    private final static double MIN_SIMLIAR_THRESHOLD = 0.85;
    private List<UsageGroupAst> astSimilarityList = new LinkedList<>();
    private volatile int groupNo = 1;
    private static final Language JAVA = Language.findLanguageByID("JAVA");
    private static final int MAX_HOPES = 17;

    public synchronized UsageGroup getAggregateUsage(Usage usage) {

        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
//        com.intellij.usageView.UsageInfo#getReference

//        JavaUsageTypeProvider usageTypeProvider = usageInfo.
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


//        UsageTypeGroupingRule usageTypeGroupingRule = new UsageTypeGroupingRule();
//        UsageGroup usageGroup = usageTypeGroupingRule.getParentGroupsFor(usage,UsageTarget.EMPTY_ARRAY).get(0);

        final PsiElement psiElement = findStatementFrom(usageInfo.getElement());
        UsageType usageType = findUsageType(usage, psiElement);


        if (psiElement == null) return null;
        CodeBlock codeBlock = new CodeBlock(psiElement, usageType);
        return cluster(codeBlock);
    }

    public synchronized UsageGroup getGroupBasedOnStatement(Usage usage) {

        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        final PsiElement psiElement = findStatementFrom(usageInfo.getElement());
        if (psiElement == null) return null;
        CodeBlock codeBlock = new CodeBlock(psiElement);
        return cluster2(codeBlock, usage, psiElement);
    }

    private UsageType findUsageType(@Nullable Usage usage, PsiElement parentBlockPsi) {

        UsageTypeGroupingRule usageTypeGroupingRule = new UsageTypeGroupingRule();
        List<UsageGroup> usageGroupList = usageTypeGroupingRule.getParentGroupsFor(usage, UsageTarget.EMPTY_ARRAY);
        if (usageGroupList == null || usageGroupList.size() == 0) return null;
        PsiElement element = null;
        if (usage instanceof PsiElementUsage) {

            PsiElementUsage elementUsage = (PsiElementUsage) usage;

            element = elementUsage.getElement();
        }
        if (!usageGroupList.get(0).getText(null).equals("Unclassified {0}"))
            return new UsageType(usageGroupList.get(0).getText(null));

        if (parentBlockPsi instanceof PsiIfStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.if"));
        if (parentBlockPsi instanceof PsiDeclarationStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.declaration"));
        if (parentBlockPsi instanceof PsiAssertStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.assert"));
        if (parentBlockPsi instanceof PsiTryStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.try"));
        if (parentBlockPsi instanceof PsiLoopStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.loop"));
        if (parentBlockPsi instanceof PsiReturnStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.return"));
        if (parentBlockPsi instanceof PsiExpressionStatement)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.expr"));
        if (parentBlockPsi instanceof PsiReferenceExpression || (element != null && element instanceof PsiReferenceExpression))
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.reference.expr"));
        if (parentBlockPsi instanceof PsiField)
            return new UsageType(UsageGroupBundle.messagePointer("usage.type.field"));

        return UsageType.UNCLASSIFIED;

    }

    //    It first finds the minimum similarity between the usage and all members of all clusters separately and memoizes them.
//    Based on max of mins similarity, it chooses the best cluster.
    private synchronized UniqueUsageGroup cluster2(final CodeBlock codeBlock, Usage usage, PsiElement parentPsiElement) {
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
//            int i = 0;
//            mostSimilarGroup.get().getGroup().setUsageDisplayed(mostSimilarGroup.get().getGroup().getUsageDisplayed() + (i++));
            return mostSimilarGroup.get().getGroup();
        } else {
            // Create and return a code block usage key
            StringBuilder stringBuilder = new StringBuilder("#");
            UsageType usageType = findUsageType(usage, parentPsiElement);

//            String usageTypeStr =usage.getPresentation().getTooltipText();

//            String usageTypeStr = ((UsageInfo2UsageAdapter) usage).getUsageInfo().getElement().getText();
//            String usageTypeStr = codeBlock.getCodeTokenized();
            String groupTypeAndSegment = "";
            if (!usageType.toString().equals("Unclassified {0}")) {
                groupTypeAndSegment = usageType.toString();
            }
            groupTypeAndSegment += " " + calculateGroupName(usage, parentPsiElement);

            if (groupNo <= 9) {
                stringBuilder.append(0);
            }
            stringBuilder.append(groupNo++).append(" ").append(groupTypeAndSegment);

            final UniqueUsageGroup newAstKey = new UniqueUsageGroup(stringBuilder.toString());
//            newAstKey.incrementUsageCount();
            astSimilarityList.add(new UsageGroupAst(codeBlock, newAstKey));
            return newAstKey;
        }
    }

    private String calculateGroupName(Usage usage, PsiElement parentPsiElement) {
        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        PsiElement element = usageInfo.getElement();
//        PsiElement element = parentPsiElement;
        Segment segment = usageInfo.getSegment();
        ChunkExtractor chunkExtractor = ChunkExtractor.getExtractor(usageInfo.getFile());
        TextRange range = element.getTextRange();
        TextChunk[] chunks = chunkExtractor.createTextChunks((UsageInfo2UsageAdapter) usage, usageInfo.getFile().getText(),
                range.getStartOffset(), range.getEndOffset(), false, new ArrayList<>());
        System.out.println(segment);
        return Arrays.toString(chunks).replaceAll(",", "").replaceAll(" ", "");

    }

    //    It first finds the minimum similarity between the usage and all members of all clusters separately and memoizes them.
//    Based on max of mins similarity, it chooses the best cluster.
    private synchronized UniqueUsageGroup cluster(final CodeBlock codeBlock) {
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
            String usageTypeStr = "Usage Group ";
            if (!codeBlock.getType().toString().equals("Unclassified {0}")) {
                usageTypeStr = codeBlock.getType().toString();
            }
            StringBuilder stringBuilder = new StringBuilder("#");
            if (groupNo <= 9) {
                stringBuilder.append(0);
            }
            stringBuilder.append(groupNo++).append(" ").append(usageTypeStr);

            final UniqueUsageGroup newAstKey = new UniqueUsageGroup(stringBuilder.toString());
//            newAstKey.incrementUsageCount();
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
                if (element instanceof PsiDocComment) return null;

                element = element.getParent();
            }
        }
        return null;
    }
}
