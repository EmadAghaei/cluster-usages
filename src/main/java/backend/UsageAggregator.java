package backend;

import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageInfo;
import com.intellij.usages.Usage;
import com.intellij.usages.UsageGroup;
import com.intellij.usages.UsageInfo2UsageAdapter;

import java.util.*;

public class UsageAggregator {


    private final static double SIMILIAR_THRESHOLD = 0.15; // lower means combine more
    private List<AstSimilarityNode> astSimilarityList = new LinkedList<>();
    private Set<String> codeBlockSet = new HashSet<>();

//    private Map<String, UniqueUsageGroup> usageToUniqueUsageGroup = new HashMap<String, UniqueUsageGroup>();
//    private Map<String, UsageInfo> usageToUsageInfo = new HashMap<String, UsageInfo>();
//    private Map<UniqueUsageGroup, CodeBlockUsage> usageInfoToCodeBlockMap = new HashMap<>();
//    private SortedMap<CodeBlockUsage, UniqueUsageGroup> codeBlockIntegerSortedMap = new TreeMap<>();

    public synchronized UsageGroup getAggregateUsage(Usage usage) {
        UsageInfo usageInfo = ((UsageInfo2UsageAdapter) usage).getUsageInfo();
        PsiElement currentElement = usageInfo.getElement();


//        while (currentElement != null && !currentElement.toString().contains("PsiMethod:")) {
//            currentElement = currentElement.getContext();
//            System.out.println(currentElement.getContext().toString());
//        }
        currentElement = currentElement.getContext().getParent();
//        System.out.println(currentElement.toString());

        PsiElement codeBlockElement = currentElement;
        CodeBlockUsage codeBlockUsage;

        if (codeBlockElement != null) {
            if (codeBlockSet.contains(codeBlockElement.getText())) {
                return null;
            }
            codeBlockSet.add(codeBlockElement.getText());

            String codeBlockElementClazzName = codeBlockElement.getContext().toString().replaceFirst(".*:", "");
            if (codeBlockElementClazzName.equals("@link")) {
                System.out.println(codeBlockElementClazzName +"---"+ codeBlockElement.getText());
                return null;
            }

            codeBlockUsage = new CodeBlockUsage(codeBlockElement, codeBlockElementClazzName);

            Optional<AstSimilarityNode> mostSimilarAstKey = Optional.empty();
            double highestSimilarityRating = 0.0;

            mostSimilarAstKey = astSimilarityList.parallelStream()
                    .max((a, b) ->
                            (int) (1000 * (a.getSimilarityTo(codeBlockUsage) - b.getSimilarityTo(codeBlockUsage))));


//            for (AstSimilarityNode astSimilarityNode: astSimilarityList) {
//                double maxSimilarityTo = astSimilarityNode.getSimilarityTo(codeBlockUsage);
//                if (maxSimilarityTo > highestSimilarityRating) {
//                    mostSimilarAstKey = Optional.of(astSimilarityNode);
//                    highestSimilarityRating = maxSimilarityTo;
//                }
//            }

            if (mostSimilarAstKey.isPresent()) {
                highestSimilarityRating = mostSimilarAstKey.get().getSimilarityTo(codeBlockUsage);
            }

            if (highestSimilarityRating > SIMILIAR_THRESHOLD) {
//                System.out.println("Aggregation occurred");
//                System.out.println(highestSimilarityRating);
                // Check if returning exact match because classic find usages is weird.
                if (highestSimilarityRating >= 1.0) {
                    throw new RuntimeException("This should never happen!");
                }
                mostSimilarAstKey.get().getElements().add(codeBlockUsage);
                mostSimilarAstKey.get().getGroup().incrementUsageCount();
                return mostSimilarAstKey.get().getGroup();
            } else {
//                System.out.println("new group occurred");
//                System.out.println(highestSimilarityRating);
                // Create and return a codeblock usage key
                UniqueUsageGroup newAstKey = new UniqueUsageGroup("Similar Usage Group");
                astSimilarityList.add(new AstSimilarityNode(codeBlockUsage, newAstKey));
                return newAstKey;
            }
        } else {
            return null;
//            return usageToUniqueUsageGroup.get(key);
        }
    }
}
