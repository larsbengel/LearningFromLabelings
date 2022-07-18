/*
 *  This file is part of "TweetyProject", a collection of Java libraries for
 *  logical aspects of artificial intelligence and knowledge representation.
 *
 *  TweetyProject is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License version 3 as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2021 The TweetyProject Team <http://tweetyproject.org/contact/>
 */

package old;

import org.tweetyproject.arg.dung.reasoner.AbstractExtensionReasoner;
import org.tweetyproject.arg.dung.semantics.ArgumentStatus;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;

import java.util.*;

/**
 * Class representing an entity with an underlying argumentation framework. A user can ask the entity for labelings
 *
 * @author Lars Bengel
 */
public class Entity {

    private DungTheory hiddenFramework;

    private Map<Semantics, List<Example>> examples;

    /**
     * initialize the entity with the given AF
     * @param theory a dung theory
     */
    Entity(DungTheory theory) {
        hiddenFramework = theory;

        this.examples = new HashMap<>();
        this.computeExamplesForSemantics(Semantics.CF);
        this.computeExamplesForSemantics(Semantics.ADM);
        this.computeExamplesForSemantics(Semantics.CO);
        this.computeExamplesForSemantics(Semantics.ST);

        System.out.println("CF: " + examples.get(Semantics.CF).size());
        System.out.println("ADM: " + examples.get(Semantics.ADM).size());
        System.out.println("CO: " + examples.get(Semantics.CO).size());
        System.out.println("ST: " + examples.get(Semantics.ST).size());
    }

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
     * @param sem a semantics
     * @return a labeling
     */
    public Example getLabeling(Semantics sem) {
        List<Example> examplesSem = this.examples.get(sem);
        Random rnd = new Random(0);
        int id = rnd.nextInt(examplesSem.size());

        return examplesSem.remove(id);
    }

    /**
     * shortcut for getting all labelings wrt a semantics
     * @param sem a semantics
     * @return all labelings of the hidden AF wrt the given semantics
     */
    public Collection<Example> getAllLabelings(Semantics sem) {
        return this.examples.get(sem);
    }

    /**
     * verify if the given theory is equivalent to the hidden framework w.r.t. the given labelings
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
                    throw new IllegalArgumentException("Algorithm failed");
                }
            }
        }
        return true;
    }

    /**
     * get the arguments of the hidden AF
     * @return the set of arguments
     */
    public Collection<Argument> getArguments() {
        return new HashSet<>(this.hiddenFramework);
    }

    public DungTheory getHiddenAF() {
        return this.hiddenFramework;
    }
}
