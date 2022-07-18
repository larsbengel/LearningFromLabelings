
package old;

import org.tweetyproject.arg.dung.reasoner.SimpleConflictFreeReasoner;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.arg.dung.util.DefaultDungTheoryGenerator;
import org.tweetyproject.arg.dung.util.DungTheoryGenerationParameters;
import org.tweetyproject.arg.dung.util.DungTheoryGenerator;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Lars Bengel
 */
public class Test {
    public static void main(String[] args) {
        DungTheoryGenerator dtg = new DefaultDungTheoryGenerator(new DungTheoryGenerationParameters());
        dtg.setSeed(0);
        DungTheory theory = dtg.next();

        long start1 = System.nanoTime();
        Collection<Extension> exts1 = new SimpleConflictFreeReasoner().getModels(theory);
        long end1 = System.nanoTime();

        System.out.println("_____");

        long start2 = System.nanoTime();
        //Collection<Extension> exts2 = LabelingComputation.getConflictFreeSets(theory);
        Collection<Example> examples = new HashSet<>();
        for (Extension ext: exts1) {
            examples.add(new Example(theory, ext, Semantics.CF));
        }
        long end2 = System.nanoTime();

        System.out.println("Time 1: " + (end1 - start1));
        System.out.println("Time 2: " + (end2 - start2));

        //System.out.println(exts1.equals(exts2));
        System.out.println(exts1.size());
        //System.out.println(exts2.size());
        System.out.println(examples.size());
        System.out.println(examples.iterator().next());
    }
}
