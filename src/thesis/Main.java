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

package thesis;

import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.arg.dung.util.DefaultDungTheoryGenerator;
import org.tweetyproject.arg.dung.util.DungTheoryGenerationParameters;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.TimeUnit;
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


        Example l1 = new Example();
        l1.setSemantics(Semantics.ADM);
        l1.put(a, ArgumentStatus.IN);
        l1.put(b, ArgumentStatus.OUT);
        l1.put(c, ArgumentStatus.OUT);
        l1.put(d, ArgumentStatus.UNDECIDED);
        l1.put(e, ArgumentStatus.UNDECIDED);

        Example l2 = new Example();
        l2.setSemantics(Semantics.CO);
        l2.put(a, ArgumentStatus.OUT);
        l2.put(b, ArgumentStatus.IN);
        l2.put(c, ArgumentStatus.IN);
        l2.put(d, ArgumentStatus.OUT);
        l2.put(e, ArgumentStatus.UNDECIDED);

        Example l3 = new Example();
        l3.setSemantics(Semantics.CF);
        l3.put(a, ArgumentStatus.UNDECIDED);
        l3.put(b, ArgumentStatus.UNDECIDED);
        l3.put(c, ArgumentStatus.UNDECIDED);
        l3.put(d, ArgumentStatus.IN);
        l3.put(e, ArgumentStatus.OUT);

        Example l4 = new Example();
        l4.setSemantics(Semantics.ADM);
        l4.put(a, ArgumentStatus.IN);
        l4.put(b, ArgumentStatus.IN);
        l4.put(c, ArgumentStatus.IN);
        l4.put(d, ArgumentStatus.OUT);
        l4.put(e, ArgumentStatus.OUT);

         */

        DungTheoryGenerationParameters params = new DungTheoryGenerationParameters();
        params.numberOfArguments = 10;
        params.attackProbability = 0.1;

        int numLearn = 200;
        int numAFs = 100;


        List<Long> setup_time_list = new ArrayList<>();
        List<Long> learning_time_list = new ArrayList<>();
        List<Long> constructing_time_list = new ArrayList<>();
        List<Long> num_af_list = new ArrayList<>();


        DefaultDungTheoryGenerator theoryGenerator = new DefaultDungTheoryGenerator(params);
        theoryGenerator.setSeed(0);

        for (int i = 0; i < numAFs; i++) {
            System.out.print("Setup...");
            DungTheory theory = theoryGenerator.next();

            Long[] evaluationData = new Long[25];

            long setup_start = System.nanoTime();
            Entity entity = new Entity(theory);
            long setup_end = System.nanoTime();

            AFLearner learner = new AFLearner(entity.getArguments());

            Collection<Example> examples = new ArrayList<>();
            while (examples.size() < numLearn) {
                try {
                    examples.add(entity.getLabeling(Semantics.ST));
                } catch (IllegalArgumentException e1) {
                    try {
                        examples.add(entity.getLabeling(Semantics.CO));
                    } catch (IllegalArgumentException e2) {
                        try {
                            examples.add(entity.getLabeling(Semantics.ADM));
                        } catch (IllegalArgumentException e3) {
                            try {
                                examples.add(entity.getLabeling(Semantics.CF));
                            } catch (IllegalArgumentException e4) {
                                break;
                            }
                        }
                    }
                }
            }

            System.out.println("done");

            evaluationData[0] = (long) params.numberOfArguments;
            evaluationData[1] = (long) examples.size();


            System.out.print("Learning...");
            List<Integer> cutoffs = Arrays.asList(10, 20, 30, 40, 50, 60, 80, 100, 150, 200);
            long learning_time = 0;
            int num_learned = 0;
            for (Example example : examples) {
                long learning_start = System.nanoTime();
                boolean result = learner.learnLabeling(example);
                //learner.printStatus();
                long numFrameworks = learner.getNumberOfFrameworks(true);
                num_learned++;
                if (numFrameworks == 1) {
                    break;
                }
                long learning_end = System.nanoTime();
                learning_time += learning_end - learning_start;
                // check if current status is relevant for evaluation
                if (cutoffs.contains(num_learned)) {
                    evaluationData[2 + cutoffs.indexOf(num_learned)] = learning_time;
                    evaluationData[13 + cutoffs.indexOf(num_learned)] = learner.getNumberOfFrameworks();
                }
            }

            System.out.println("done");

            long num_afs_learned = learner.getNumberOfFrameworks();
            evaluationData[12] = learning_time;
            evaluationData[23] = num_afs_learned;
            evaluationData[24] = (long) num_learned;

            System.out.print("Constructing...");

            //System.out.println("=== "+ i +" ===========");
            //System.out.println("Learned Labelings: " + num_learned);
            //System.out.println("Num of AFs: " + num_afs_learned);
            //learner.computePartialAttackRelations();
            long constructing_start = 0;
            long constructing_end = 0;
            Collection<DungTheory> afs = new HashSet<>();
            if (0 < num_afs_learned && num_afs_learned < 10000) {
                constructing_start = System.nanoTime();
                afs = learner.getLearnedFrameworks();
                constructing_end = System.nanoTime();
                for (DungTheory af : afs) {
                    //System.out.println(af.prettyPrint());
                    //System.out.println(new SimpleAdmissibleReasoner().getModels(af));
                    if (!entity.verifyFramework(af, examples)) {
                        System.out.println("Problem!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!");
                        break;
                    }
                }
            }
            System.out.println("done");

            System.out.print("Saving...");
            String evaluation_string = i + "," + Arrays.stream(evaluationData).map(String::valueOf).collect(Collectors.joining(",")) + "\n";

            try {
                Files.write(Paths.get("results.csv"), evaluation_string.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                //exception handling left as an exercise for the reader
            }


            long setup_time = setup_end - setup_start;
            long constructing_time = constructing_end - constructing_start;

            setup_time_list.add(setup_time);
            learning_time_list.add(learning_time);
            constructing_time_list.add(constructing_time);
            num_af_list.add(num_afs_learned);

            //System.out.println("Setup: " + TimeUnit.NANOSECONDS.toMillis(setup_time) + " ms");
            //System.out.println("Learning: " + TimeUnit.NANOSECONDS.toMillis(learning_time) + " ms");
            //System.out.println("Constructing: " + TimeUnit.NANOSECONDS.toMillis(constructing_time) + " ms");
            //System.out.println("Labelings learned: " + num_learned);
            //System.out.println("AFs constructed: " + afs.size());

            System.out.println("done");
        }

        /*
        // write results to file
        FileWriter writer = new FileWriter("results.csv");

        String collect_setup = setup_time_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        String collect_learning = learning_time_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        String collect_constructing = constructing_time_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        String collect_afs = num_af_list.stream().map(String::valueOf).collect(Collectors.joining(","));
        writer.write(collect_setup);
        writer.write("\n");
        writer.write(collect_learning);
        writer.write("\n");
        writer.write(collect_constructing);
        writer.write("\n");
        writer.write(collect_afs);
        writer.write("\n");
        writer.close();

         */
    }
}
