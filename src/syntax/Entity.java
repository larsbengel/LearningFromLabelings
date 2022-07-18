
package org.tweetyproject.arg.dung.thesis.syntax;

import org.tweetyproject.arg.dung.reasoner.AbstractExtensionReasoner;
import org.tweetyproject.arg.dung.semantics.ArgumentStatus;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import java.util.*;

/**
 * Class representing an entity with an underlying hidden argumentation framework. An agent can ask the entity for labelings
 *
 * @author Lars Bengel
 */
public class Entity {

    /** the hidden AF that only this entity has access to */
    private DungTheory hiddenFramework;

    /** structure to store the labeling of the AF */
    private Map<Semantics, List<Example>> examples;

    private List<Example> allExamples;

    /**
     * initialize the entity with the given AF
     * @param theory some argumentation framework
     */
    public Entity(DungTheory theory) {
        hiddenFramework = theory;

        // compute the labelings for all supported semantics
        this.examples = new HashMap<>();
        this.computeExamplesForSemantics(Semantics.CF);
        this.computeExamplesForSemantics(Semantics.ADM);
        this.computeExamplesForSemantics(Semantics.CO);
        this.computeExamplesForSemantics(Semantics.ST);

        this.allExamples = new LinkedList<>();
        this.allExamples.addAll(this.getAllLabelings(Semantics.CF));
        this.allExamples.addAll(this.getAllLabelings(Semantics.ADM));
        this.allExamples.addAll(this.getAllLabelings(Semantics.CO));
        this.allExamples.addAll(this.getAllLabelings(Semantics.ST));

        //System.out.println("CF: " + examples.get(Semantics.CF).size());
        //System.out.println("ADM: " + examples.get(Semantics.ADM).size());
        //System.out.println("CO: " + examples.get(Semantics.CO).size());
        //System.out.println("ST: " + examples.get(Semantics.ST).size());
    }

    /**
     * helper method to compute all labelings for the given semantics
     * @param sem some semantics
     */
    private void computeExamplesForSemantics(Semantics sem) {
        Collection<Extension> exts = AbstractExtensionReasoner.getSimpleReasonerForSemantics(sem).getModels(this.hiddenFramework);
        List<Example> examples_sem = new LinkedList<>();
        for (Extension ext: exts) {
            Example ex = new Example(this.hiddenFramework, ext, sem);
            examples_sem.add(ex);
        }
        this.examples.put(sem, examples_sem);
    }

    /**
     * Ask for a random labeling w.r.t. the given semantics
     * The labeling is also removed from the internal storage so it can ot be given out again
     * @param sem a semantics
     * @return a labeling
     */
    public Example getLabeling(Semantics sem) {
        List<Example> examplesSem = this.examples.get(sem);
        Random rnd = new Random(0);
        int id = rnd.nextInt(examplesSem.size());

        return examplesSem.remove(id);
    }

    public Example getAnyLabeling() {
        Random rnd = new Random();
        int id = rnd.nextInt(this.allExamples.size());

        return this.allExamples.remove(id);
    }

    /**
     * shortcut for getting all labelings wrt a semantics
     * @param sem a semantics
     * @return all labelings of the hidden AF wrt. the given semantics
     */
    public Collection<Example> getAllLabelings(Semantics sem) {
        return this.examples.get(sem);
    }

    /**
     * verify if the given theory is equivalent to the hidden framework w.r.t. the given labelings
     * very inefficiently implemented right now
     *
     * @param theory a dung theory
     * @param examples a set of labelings
     * @return "true" if both frameworks are equivalent
     */
    public boolean verifyFramework(DungTheory theory, Collection<Example> examples) {
        for (Example example: examples) {
            if (this.examples.get(example.getSemantics()).contains(example)) {
                Collection<Extension> exts = AbstractExtensionReasoner.getSimpleReasonerForSemantics(example.getSemantics()).getModels(theory);
                Extension ext = new Extension(example.getArgumentsOfStatus(ArgumentStatus.IN));
                if (!exts.contains(ext)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * get the set of arguments of the hidden AF
     * @return the set of arguments
     */
    public Collection<Argument> getArguments() {
        return new HashSet<>(this.hiddenFramework);
    }

    /**
     * return the hidden AF
     * should not be used normally
     * @return the hidden AF
     */
    public DungTheory getHiddenAF() {
        return this.hiddenFramework;
    }
}
