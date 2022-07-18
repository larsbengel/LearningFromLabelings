
package eval;

import org.tweetyproject.arg.dung.parser.AbstractDungParser;
import org.tweetyproject.arg.dung.parser.ApxParser;
import org.tweetyproject.arg.dung.semantics.Extension;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import learning.AFLearner;
import learning.SimpleAFLearner;
import syntax.Input;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Evaluation Setup for Experiment 1
 *
 * @author Lars Bengel
 */
public class EvaluationICCMA {
    public static void main(String[] args) throws IOException {
        AbstractDungParser parser = new ApxParser();
        File folder = new File("instances");
        File[] listOfFiles = folder.listFiles();

        Collection<String> filenames = new HashSet<>();

        for (int i = 0; i < listOfFiles.length; i++) {
            String filename = listOfFiles[i].getName();
            if (filename.endsWith(".apx")) {
                filenames.add("instances/"+filename);
            }
        }

        System.out.println("Learning up to " + filenames.size() + " AFs...");
        int i = 0;
        for (String path: filenames) {
            System.out.print(i++ + ".Setup...");
            long setup_start = System.nanoTime();
            DungTheory theory = parser.parseBeliefBaseFromFile(path);
            Collection<Collection<Argument>> exts_co = AbstractDungParser.parseExtensionList(Files.readString(Paths.get( path + ".CO")));
            Collection<Collection<Argument>> exts_st = AbstractDungParser.parseExtensionList(Files.readString(Paths.get( path + ".ST")));

            if (theory.size() > 1000) {
                System.out.println("Skipped: " + path);
                System.out.println(theory.size() + ", " + (exts_co.size()+exts_st.size()));
                continue;
            }

            List<Input> inputs = new LinkedList<>();
            for (Collection<Argument> ext: exts_co) {
                Input input = new Input(theory, new Extension(ext), Semantics.CO);
                inputs.add(input);
            }
            for (Collection<Argument> ext: exts_st) {
                Input input = new Input(theory, new Extension(ext), Semantics.ST);
                inputs.add(input);
            }

            Collection<Input> examplesLearn = new ArrayList<>();
            while (examplesLearn.size() < 200) {
                try {
                    Random rnd = new Random();
                    int id = rnd.nextInt(inputs.size());
                    examplesLearn.add(inputs.remove(id));
                } catch (IllegalArgumentException e) {
                    break;

                }
            }



            Long[] evaluationData = new Long[5];

            AFLearner learner = new SimpleAFLearner(theory);

            long setup_end = System.nanoTime();
            System.out.println("done");

            System.out.println(path);
            System.out.println("Num Args: " + theory.size());
            System.out.println("Num exts co: " + exts_co.size());
            System.out.println("Num exts st: " + exts_st.size());

            // Logging
            evaluationData[0] = (long) theory.size();
            evaluationData[1] = (long) exts_co.size();

            // Learn all st, co labelings
            System.out.print("Learning...");
            int num_learned = 0;
            long learning_start = System.nanoTime();
            for (Input input : examplesLearn) {
                learner.learnLabeling(input);
                num_learned++;
            }
            long learning_end = System.nanoTime();
            System.out.println("done");

            // Construct all AFs, only if there are less than 1000
            System.out.print("Constructing...");
            long constructing_start = System.nanoTime();
            DungTheory learnedTheory = learner.getLearnedFramework();
            long constructing_end = System.nanoTime();
            System.out.println("done");

            // Logging
            System.out.print("Saving...");
            evaluationData[2] = (long) exts_st.size();
            evaluationData[3] = learning_end - learning_start;;
            evaluationData[4] = constructing_end - constructing_start;

            // Write performance data to file
            String evaluation_string = path + "," + Arrays.stream(evaluationData).map(String::valueOf).collect(Collectors.joining(",")) + "\n";

            try {
                Files.write(Paths.get("results_iccma.csv"), evaluation_string.getBytes(), StandardOpenOption.APPEND);
            }catch (IOException e) {
                throw new RuntimeException("Should never happen");
            }
            System.out.println("done");
        }
    }
}
