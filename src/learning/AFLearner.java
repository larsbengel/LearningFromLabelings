
package org.tweetyproject.arg.dung.thesis.learning;

import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.arg.dung.thesis.syntax.Example;

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
    boolean learnLabeling(Example labeling);

    /**
     * compute all argumentation frameworks that satisfy the internal acceptance conditions
     * @return the set of computed argumentation frameworks
     */
    Collection<DungTheory> getLearnedFrameworks();

    /**
     * compute an argumentation framework that satisfy the internal acceptance conditions
     * @return some argumentation framework that satisfies the internal conditions
     */
    DungTheory getLearnedFramework();

    /**
     * print the internal acceptance conditions
     */
    void printStatus();

    /**
     * compute the number of argumentation frameworks that satisfy the internal acceptance conditions
     * @return number of afs that produce all processed input labelings
     */
    long getNumberOfFrameworks();
}
