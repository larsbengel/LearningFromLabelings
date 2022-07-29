import learning.AFLearner;
import learning.OptimizedParallelAFLearner;
import learning.ParallelAFLearner;
import learning.SimpleAFLearner;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.arg.dung.util.DefaultDungTheoryGenerator;
import org.tweetyproject.arg.dung.util.DungTheoryGenerationParameters;
import syntax.Entity;
import syntax.Input;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Lars Bengel
 */
public class Main {
    public static void main(String[] args) throws IOException {
        /*
        DungTheory theory = new DungTheory();
        Argument a = new Argument("a");
        Argument b = new Argument("b");
        Argument c = new Argument("c");
        Argument d = new Argument("d");
        Argument e = new Argument("e");

        theory.add(a);
        theory.add(b);
        theory.add(c);
        theory.add(d);
        theory.add(e);


        //PodlaszewskiCaminadaDungTheoryGenerator theoGen = new PodlaszewskiCaminadaDungTheoryGenerator(3);
        //DungTheory theory = theoGen.next();



        System.out.println(theory.prettyPrint());


        Input l1 = new Input();
        l1.setSemantics(Semantics.ADM);
        l1.put(a, ArgumentStatus.IN);
        l1.put(b, ArgumentStatus.OUT);
        l1.put(c, ArgumentStatus.OUT);
        l1.put(d, ArgumentStatus.UNDECIDED);
        l1.put(e, ArgumentStatus.UNDECIDED);

        Input l2 = new Input();
        l2.setSemantics(Semantics.CO);
        l2.put(a, ArgumentStatus.OUT);
        l2.put(b, ArgumentStatus.IN);
        l2.put(c, ArgumentStatus.IN);
        l2.put(d, ArgumentStatus.OUT);
        l2.put(e, ArgumentStatus.UNDECIDED);

        Input l3 = new Input();
        l3.setSemantics(Semantics.CF);
        l3.put(a, ArgumentStatus.UNDECIDED);
        l3.put(b, ArgumentStatus.UNDECIDED);
        l3.put(c, ArgumentStatus.UNDECIDED);
        l3.put(d, ArgumentStatus.IN);
        l3.put(e, ArgumentStatus.OUT);

        Input l4 = new Input();
        l4.setSemantics(Semantics.ADM);
        l4.put(a, ArgumentStatus.IN);
        l4.put(b, ArgumentStatus.IN);
        l4.put(c, ArgumentStatus.IN);
        l4.put(d, ArgumentStatus.OUT);
        l4.put(e, ArgumentStatus.OUT);

         */

        String learner_type = args[0];

        DungTheoryGenerationParameters params = new DungTheoryGenerationParameters();
        params.numberOfArguments = 20;
        params.attackProbability = 0.12;

        int numLearn = 1000;
        int numAFs = 10;


        List<Long> setup_time_list = new ArrayList<>();
        List<Long> learning_time_list = new ArrayList<>();
        List<Long> constructing_time_list = new ArrayList<>();


        DefaultDungTheoryGenerator theoryGenerator = new DefaultDungTheoryGenerator(params);
        theoryGenerator.setSeed(0);

        for (int i = 0; i < numAFs; i++) {
            System.out.print("Setup...");
            DungTheory theory = theoryGenerator.next();

            Long[] evaluationData = new Long[6];

            long setup_start = System.nanoTime();
            Entity entity = new Entity(theory);
            long setup_end = System.nanoTime();

            AFLearner learner = null;
            switch (learner_type) {
                case "simple" -> learner = new SimpleAFLearner(entity.getArguments());
                case "para" -> learner = new ParallelAFLearner(entity.getArguments());
                case "opti" -> learner = new OptimizedParallelAFLearner(entity.getArguments());
            }

            Collection<Input> inputs = new ArrayList<>();
            while (inputs.size() < numLearn) {
                try {
                    inputs.add(entity.getLabeling(Semantics.ST));
                } catch (IllegalArgumentException e1) {
                    try {
                        inputs.add(entity.getLabeling(Semantics.CO));
                    } catch (IllegalArgumentException e2) {
                        try {
                            inputs.add(entity.getLabeling(Semantics.ADM));
                        } catch (IllegalArgumentException e3) {
                            try {
                                inputs.add(entity.getLabeling(Semantics.CF));
                            } catch (IllegalArgumentException e4) {
                                break;
                            }
                        }
                    }
                }
            }

            System.out.println("done");

            evaluationData[0] = (long) params.numberOfArguments;
            evaluationData[1] = (long) inputs.size();


            System.out.print("Learning...");
            long learning_time = 0;
            int num_learned = 0;
            for (Input input : inputs) {
                long learning_start = System.nanoTime();
                boolean result = learner.learnLabeling(input);
                num_learned++;

                long learning_end = System.nanoTime();
                learning_time += learning_end - learning_start;
            }

            System.out.println("done");

            evaluationData[2] = (long) num_learned;
            evaluationData[4] = learning_time;

            System.out.print("Constructing...");

            //System.out.println("=== "+ i +" ===========");
            //System.out.println("Learned Labelings: " + num_learned);
            //System.out.println("Num of AFs: " + num_afs_learned);
            //learner.computePartialAttackRelations();
            long constructing_start = System.nanoTime();
            DungTheory learned_theory = learner.getModel();
            long constructing_end = System.nanoTime();
            System.out.println("done");

            System.out.print("Verifying...");
            if (!entity.verifyFramework(learned_theory, inputs)) {
                System.out.println("ERROR!");
            } else {
                System.out.println("done.");
            }

            long setup_time = setup_end - setup_start;
            long constructing_time = constructing_end - constructing_start;
            evaluationData[3] = setup_time;
            evaluationData[5] = constructing_time;


            System.out.print("Saving...");
            String evaluation_string = i + "," + Arrays.stream(evaluationData).map(String::valueOf).collect(Collectors.joining(",")) + "\n";

            try {
                Files.write(Paths.get("results_" + learner_type + ".csv"), evaluation_string.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
                System.out.println("ERROR");
            }

            setup_time_list.add(setup_time);
            learning_time_list.add(learning_time);
            constructing_time_list.add(constructing_time);

            //System.out.println("Setup: " + TimeUnit.NANOSECONDS.toMillis(setup_time) + " ms");
            //System.out.println("Learning: " + TimeUnit.NANOSECONDS.toMillis(learning_time) + " ms");
            //System.out.println("Constructing: " + TimeUnit.NANOSECONDS.toMillis(constructing_time) + " ms");
            //System.out.println("Labelings learned: " + num_learned);
            //System.out.println("AFs constructed: " + afs.size());

            System.out.println("done");
        }


        // write results to file
        FileWriter writer = new FileWriter("results_list_" + learner_type + ".csv");

        String collect_setup = setup_time_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        String collect_learning = learning_time_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        String collect_constructing = constructing_time_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        writer.write(collect_setup);
        writer.write("\n");
        writer.write(collect_learning);
        writer.write("\n");
        writer.write(collect_constructing);
        writer.write("\n");
        writer.write("\n");
        writer.close();

    }
}
