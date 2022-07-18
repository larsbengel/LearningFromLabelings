
package eval;

import org.tweetyproject.arg.dung.reasoner.SimpleAdmissibleReasoner;
import org.tweetyproject.arg.dung.reasoner.SimpleCompleteReasoner;
import org.tweetyproject.arg.dung.reasoner.SimpleStableReasoner;
import org.tweetyproject.arg.dung.semantics.ArgumentStatus;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import learning.SimpleAFLearner;
import syntax.Entity;
import syntax.Input;
import org.tweetyproject.arg.dung.util.DefaultDungTheoryGenerator;
import org.tweetyproject.arg.dung.util.DungTheoryGenerationParameters;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluation Setup for Experiment 2
 *
 * @author Lars Bengel
 */
public class EvaluationAdCoSt {
    public static void main(String[] args) {
        // number of arguments of each theory and attack probability
        DungTheoryGenerationParameters params = new DungTheoryGenerationParameters();
        params.numberOfArguments = 4;
        params.attackProbability = 1.0/params.numberOfArguments;

        // number of frameworks that will be learned
        int numAFs = 1;

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

            // set up learning instance and get all st, co and adm inputs
            SimpleAFLearner learner = new SimpleAFLearner(entity.getArguments());

            Collection<Input> inputs = new ArrayList<>();
            inputs.addAll(entity.getAllLabelings(Semantics.ST));
            inputs.addAll(entity.getAllLabelings(Semantics.CO));

            for (Input input : entity.getAllLabelings(Semantics.ADM)) {
                // skip empty set
                if (input.getArgumentsOfStatus(ArgumentStatus.IN).isEmpty())
                    continue;
                inputs.add(input);
            }
            long setup_end = System.nanoTime();
            System.out.println("done");

            // Logging
            evaluationData[0] = (long) params.numberOfArguments;
            evaluationData[1] = (long) inputs.size();

            // Learn all st, co and adm labelings
            System.out.print("Learning...");
            int num_learned = 0;
            long learning_start = System.nanoTime();
            for (Input input : inputs) {
                boolean result = learner.learnLabeling(input);
                num_learned++;
            }
            long learning_end = System.nanoTime();
            long learning_time = learning_end - learning_start;
            System.out.println("done");

            // Logging
            evaluationData[2] = learning_time;

            // Construct all AFs, only if there are less than 1000
            System.out.print("Constructing...");
            long constructing_start = System.nanoTime();
            evaluationData[3] = learner.getNumberOfFrameworks();
            long constructing_end = System.nanoTime();
            System.out.println("done");

            evaluationData[4] = constructing_end-constructing_start;

            /*
            Collection<DungTheory> afs = new HashSet<>();
            if (0 < num_afs_learned && num_afs_learned < 1000) {
                constructing_start = System.nanoTime();
                afs = learner.getLearnedFrameworks();
                constructing_end = System.nanoTime();
                for (DungTheory af : afs) {
                    if (!entity.verifyFramework(af, inputs)) {
                        throw new IllegalArgumentException("Should not happen");
                    }
                }
            }

             */
            System.out.println(learner.getNumberOfFrameworks());
            System.out.println(theory.prettyPrint());
            System.out.println(new SimpleAdmissibleReasoner().getModels(theory));
            System.out.println(new SimpleCompleteReasoner().getModels(theory));
            System.out.println(new SimpleStableReasoner().getModels(theory));


            // Write performance data to file
            System.out.print("Saving...");
            String evaluation_string = i + "," + Arrays.stream(evaluationData).map(String::valueOf).collect(Collectors.joining(",")) + "\n";

            try {
                Files.write(Paths.get("results_" + params.numberOfArguments + ".csv"), evaluation_string.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
            }
            System.out.println("done");
        }
    }
}
