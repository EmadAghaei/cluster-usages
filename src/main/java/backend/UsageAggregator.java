package backend;

import com.intellij.lang.Language;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.java.ImportStatementElement;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.usageView.UsageInfo;
import com.intellij.usageView.UsageViewBundle;
import com.intellij.usages.*;
import com.intellij.psi.*;
import com.intellij.usages.impl.rules.*;
import com.intellij.usages.rules.PsiElementUsage;
import gui.UniqueUsageTypeProvider;
import gui.UsageGroupBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private UsageType findUsageType(@Nullable Usage usage, PsiElement parentBlockPsi) {
        if (usage instanceof PsiElementUsage) {
            PsiElementUsage elementUsage = (PsiElementUsage)usage;

            PsiElement element = elementUsage.getElement();
            UsageType usageType = getUsageType(element, UsageTarget.EMPTY_ARRAY);

            if (usageType == null && element instanceof PsiFile && elementUsage instanceof UsageInfo2UsageAdapter) {
                usageType = ((UsageInfo2UsageAdapter)elementUsage).getUsageType();
            }

            if (usageType != null) return usageType;

            if (usage instanceof ReadWriteAccessUsage) {
                ReadWriteAccessUsage u = (ReadWriteAccessUsage)usage;
                if (u.isAccessedForWriting()) return UsageType.WRITE;
                if (u.isAccessedForReading()) return UsageType.READ;
            }
            if (parentBlockPsi instanceof PsiIfStatement) return  new UsageType(UsageGroupBundle.messagePointer("usage.type.if"));
            if (parentBlockPsi instanceof PsiTryStatement) return  new UsageType(UsageGroupBundle.messagePointer("usage.type.try"));
            if (parentBlockPsi instanceof PsiLoopStatement) return  new UsageType(UsageGroupBundle.messagePointer("usage.type.loop"));
            if (parentBlockPsi instanceof PsiReturnStatement) return  new UsageType(UsageGroupBundle.messagePointer("usage.type.return"));
            if (parentBlockPsi instanceof PsiReferenceExpression) return  new UsageType(UsageGroupBundle.messagePointer("usage.type.reference.expr"));
            if (parentBlockPsi instanceof PsiExpressionStatement) return  new UsageType(UsageGroupBundle.messagePointer("usage.type.expr"));

            return UsageType.UNCLASSIFIED;
        }

        return null;
    }

    @Nullable
    private static UsageType getUsageType(PsiElement element, UsageTarget @NotNull [] targets) {
        if (element == null) return null;

        if (PsiTreeUtil.getParentOfType(element, PsiComment.class, false) != null) { return UsageType.COMMENT_USAGE; }

        for(UsageTypeProvider provider: UsageTypeProvider.EP_NAME.getExtensionList()) {
            UsageType usageType;
            if (provider instanceof UsageTypeProviderEx) {
                usageType = ((UsageTypeProviderEx) provider).getUsageType(element, targets);
            }
            else {
                usageType = provider.getUsageType(element);
            }
            if (usageType != null) {
                return usageType;
            }
        }

        return null;
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
            String usageTypeStr = "Usage Group ";
            if (!codeBlock.getType().toString().equals("Unclassified {0}")) {
                usageTypeStr = codeBlock.getType().toString();
            }

            UniqueUsageGroup newAstKey = new UniqueUsageGroup( " #" + groupCount++ +" "+usageTypeStr);
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
                if (element instanceof PsiDocComment) return null;

                element = element.getParent();
            }
        }
        return null;
    }
}
