
package org.tweetyproject.arg.dung.thesis.eval;

import org.tweetyproject.arg.dung.semantics.ArgumentStatus;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.arg.dung.thesis.learning.SimpleAFLearner;
import org.tweetyproject.arg.dung.thesis.syntax.Entity;
import org.tweetyproject.arg.dung.thesis.syntax.Example;
import org.tweetyproject.arg.dung.util.DefaultDungTheoryGenerator;
import org.tweetyproject.arg.dung.util.DungTheoryGenerationParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Evaluation Setup for Experiment 3
 *
 * @author Lars Bengel
 */
public class EvaluationOneAf {
    public static void main(String[] args) {
        // number of arguments of each theory and attack probability
        DungTheoryGenerationParameters params = new DungTheoryGenerationParameters();
        params.numberOfArguments = 16;
        params.attackProbability = 1.0/params.numberOfArguments;

        // number of frameworks that will be learned
        int numAFs = 100;

        // init theory generator with seed
        DefaultDungTheoryGenerator theoryGenerator = new DefaultDungTheoryGenerator(params);
        theoryGenerator.setSeed(132);

        // learn argumentation frameworks
        for (int i = 0; i < numAFs; i++) {
            // compute labelings of the hidden af
            System.out.print(i + ".Setup...");
            long setup_start = System.nanoTime();
            DungTheory theory = theoryGenerator.next();

            Long[] evaluationData = new Long[5];


            Entity entity = new Entity(theory);

            // set up learning instance and get all st, co and adm examples
            SimpleAFLearner learner = new SimpleAFLearner(entity.getArguments());

            Collection<Example> examples = new ArrayList<>();
            while (true) {
                try {
                    Example example = entity.getAnyLabeling();
                    if (example.getArgumentsOfStatus(ArgumentStatus.IN).isEmpty())
                        continue;
                    examples.add(example);
                } catch (IllegalArgumentException e) {
                    break;
                }
            }

            long setup_end = System.nanoTime();
            System.out.println("done");
            //System.out.println("Num Labs: " + examples.size());

            // Logging
            evaluationData[0] = (long) params.numberOfArguments;
            evaluationData[1] = (long) examples.size();

            // Learn all st, co and adm labelings
            System.out.print("Learning...");
            int num_learned = 0;
            long learning_start = System.nanoTime();
            for (Example example : examples) {
                boolean result = learner.learnLabeling(example);
                num_learned++;

                if (learner.getNumberOfFrameworks(true) == 1) {
                    break;
                }
            }
            long learning_end = System.nanoTime();
            long learning_time = learning_end - learning_start;
            System.out.println("done");

            // Logging
            long num_afs_learned = learner.getNumberOfFrameworks();
            evaluationData[2] = learning_time;
            evaluationData[4] = (long) num_learned;

            // Construct all AFs, only if there are less than 1000
            System.out.print("Constructing...");
            long constructing_start = 0;
            long constructing_end = 0;
            Collection<DungTheory> afs = new HashSet<>();
            if (1 == num_afs_learned) {
                constructing_start = System.nanoTime();
                DungTheory af = learner.getLearnedFramework();
                constructing_end = System.nanoTime();
                //System.out.println("NUM AFs: " + afs.size());
                if (!entity.getHiddenAF().equals(af)) {
                        throw new IllegalArgumentException("Should not happen");
                }
            } else {
                System.out.println(num_afs_learned);
                throw new RuntimeException("More than 1 af left");
            }
            System.out.println("done");

            evaluationData[3] = constructing_end-constructing_start;

            // Write performance data to file
            System.out.print("Saving...");
            String evaluation_string = i + "," + Arrays.stream(evaluationData).map(String::valueOf).collect(Collectors.joining(",")) + "\n";

            try {
                Files.write(Paths.get("results_"+params.numberOfArguments+".csv"), evaluation_string.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                throw new RuntimeException("Should not happen");
            }
            System.out.println("done");
        }
    }
}
