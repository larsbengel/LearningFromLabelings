import learning.AFLearner;
import org.tweetyproject.arg.dung.semantics.Semantics;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import learning.SimpleAFLearner;
import syntax.Entity;
import syntax.Input;

/**
 * Showcasing how to use the implementation of the learning algorithm
 *
 * @author Lars Bengel
 */
public class ExampleUsage {
    public static void main(String[] args ) {

        // create a hidden argumentation framework that we will try to learn
        DungTheory hiddenAF = new DungTheory();
        Argument a = new Argument("a");
        Argument b = new Argument("b");
        Argument c = new Argument("c");
        hiddenAF.add(a);
        hiddenAF.add(b);
        hiddenAF.add(c);
        hiddenAF.addAttack(a,b);
        hiddenAF.addAttack(b,a);
        hiddenAF.addAttack(b,c);

        // create an entity that knows the hidden argumentation framework
        // we can then ask the entity for labelings
        Entity entity = new Entity(hiddenAF);


        // initialize instance of the learning algorithm for the set of arguments from above
        AFLearner learner = new SimpleAFLearner(entity.getArguments());

        // learn a stable labeling
        Input input1 = entity.getLabeling(Semantics.ST);
        learner.learnLabeling(input1);

        System.out.println("\nAcceptance conditions after learning the stable labeling: " + input1);
        learner.printStatus();
        System.out.println("Number of AFs that satisfy these conditions: " + learner.getNumberOfFrameworks());

        // learn a conflict-free labeling
        Input input2 = entity.getLabeling(Semantics.CF);
        learner.learnLabeling(input2);

        System.out.println("\nAcceptance conditions after learning the conflict-free labeling: " + input2);
        learner.printStatus();
        System.out.println("Number of AFs that satisfy these conditions: " + learner.getNumberOfFrameworks());

        // compute one argumentation framework that produces boh labelings;
        DungTheory learnedTheory = learner.getModel();
        System.out.println("\n\nLearned Framework: \n" + learnedTheory.prettyPrint());

    }
}
