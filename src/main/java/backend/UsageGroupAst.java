package backend;

import java.util.HashSet;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.stream.Stream;

public class UsageGroupAst {
    private CodeBlock representativeElement;
    private UniqueUsageGroup group;
    private Set<CodeBlock> elements = new HashSet<>();

    public UsageGroupAst(CodeBlock representativeElement, UniqueUsageGroup group) {
        this.representativeElement = representativeElement;
        this.group = group;
        elements.add(representativeElement);
    }

    public Set<CodeBlock> getElements() {
        return elements;
    }

    public void setElements(Set<CodeBlock> elements) {
        this.elements = elements;
    }

    public CodeBlock getRepresentativeElement() {
        return representativeElement;
    }

    public UniqueUsageGroup getGroup() {
        return group;
    }

    // finds a minimum similarity between a usage to be clustered and all members of a cluster
    public double getSimilarityToAverageOfThisGroup(CodeBlock o) {
        // it calculate similarity of codeblock to all usages we have
//        Stream<Double> similarityStream = elements.parallelStream().map(elem -> elem.calculateSimilarityScore(o));
        OptionalDouble lowestSimilarity = elements.stream().
                mapToDouble(elem -> elem.calculateSimilarityScore(o)).average();
        // find the most diffrent usage in this cluster
//        Optional<Double> lowestSimilarity = similarityStream.min(Double::compareTo);
//        Optional<Double> lowestSimilarity = similarityStream.min(Double::compareTo);
//        Optional<Double> lowestSimilarity = similarityStream.min(Double::compareTo);
//        if (!lowestSimilarity.isPresent()) { throw new RuntimeException("This should never happen");  }
        return lowestSimilarity.getAsDouble();
    }

    // finds a maximum similarity between a usage to be clustered and all members of a cluster
    public double getSimilarityToMostSimilarUsageOfThisGroup(CodeBlock o) {
        // it calculate similarity of codeblock to all usages we have
        OptionalDouble mostSimilarityScore = elements.stream().
                mapToDouble(elem -> elem.calculateSimilarityScore(o)).max();
        return mostSimilarityScore.getAsDouble();
    }

//    @Override
//    public String toString() {
//        return "UsageGroupAst{" +
//                "representativeElement=" + representativeElement.getCode() +
//                ", group=" + group +
//                ", elements=" + elements +
//                '}';
//    }
}
