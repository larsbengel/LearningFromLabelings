
package thesis;

import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Lars Bengel
 */
public class LabelingComputation {
    public static Collection<Extension> getConflictFreeSets(DungTheory bbase) {
        DungTheory restrictedTheory = new DungTheory(bbase);
        // remove all self-attacking arguments
        for (Argument argument: bbase) {
            if (restrictedTheory.isAttackedBy(argument, argument)) {
                restrictedTheory.remove(argument);
            }
        }
        return getConflictFreeSetsRecursive(bbase, restrictedTheory);
    }

    /**
     * computes all conflict-free sets of bbase
     * @param bbase an argumentation framework
     * @param candidates a set of arguments
     * @return conflict-free sets in bbase
     */
    private static Collection<Extension> getConflictFreeSetsRecursive(DungTheory bbase, Collection<Argument> candidates) {
        Collection<Extension> cfSubsets = new HashSet<>();
        if (candidates.size() == 0 || bbase.size() == 0) {
            cfSubsets.add(new Extension());
        } else {
            for (Argument element: candidates) {
                DungTheory remainingTheory = new DungTheory(bbase);
                remainingTheory.remove(element);
                remainingTheory.removeAll(bbase.getAttacked(element));

                Set<Argument> remainingCandidates = new HashSet<>(candidates);
                remainingCandidates.remove(element);
                remainingCandidates.removeAll(bbase.getAttacked(element));
                remainingCandidates.removeAll(bbase.getAttackers(element));

                Collection<Extension> subsubsets = getConflictFreeSetsRecursive(remainingTheory, remainingCandidates);

                for (Extension subsubset : subsubsets) {
                    cfSubsets.add(new Extension(subsubset));
                    subsubset.add(element);
                    cfSubsets.add(new Extension(subsubset));
                }
            }
        }
        return cfSubsets;
    }
}
