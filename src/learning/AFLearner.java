
package learning;

import org.tweetyproject.arg.dung.syntax.DungTheory;
import syntax.Input;

import java.util.Collection;

/**
 * Interface for the learning algorithm
 *
 * @author Lars Bengel
 */
public interface AFLearner {
    /**
     * learn a single input labeling and store in internal acceptance conditions
     * @param labeling some input labeling
     * @return true if the labeling has been processed successfully
     */
    boolean learnLabeling(Input labeling);

    /**
     * compute all argumentation frameworks that satisfy the internal acceptance conditions
     * @return the set of computed argumentation frameworks
     */
    Collection<DungTheory> getModels();

    /**
     * compute an argumentation framework that satisfy the internal acceptance conditions
     * @return some argumentation framework that satisfies the internal conditions
     */
    DungTheory getModel();

    /**
     * print the internal acceptance conditions
     */
    void printStatus();

    /**
     * compute the number of argumentation frameworks that satisfy the internal acceptance conditions
     * @return number of afs that produce all processed input labelings
     */
    long getNumberOfFrameworks();
    long getNumberOfFrameworks(boolean shortcut);
}
