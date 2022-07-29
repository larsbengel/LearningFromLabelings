package learning;


import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.Attack;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.logics.pl.sat.Sat4jSolver;
import org.tweetyproject.logics.pl.sat.SatSolver;
import org.tweetyproject.logics.pl.semantics.PossibleWorld;
import org.tweetyproject.logics.pl.syntax.*;
import syntax.Input;
import syntax.SimpleAttackConstraint;
import util.ModelComputation;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the iterative algorithm for learning argumentation frameworks from labelings
 * @author Lars Bengel
 */
public class ParallelAFLearner implements AFLearner {

    /* the set of all arguments */
    private Collection<Argument> args;
    /* structure for storing the acceptance condition of each argument */
    private Map<Argument, SimpleAttackConstraint> conditions;

    /**
     * initialize the Learner with a set of arguments
     * @param args a set of arguments
     */
    public ParallelAFLearner(Collection<Argument> args) {
        this.args = args;
        this.conditions = new ConcurrentHashMap<>();
        for (Argument a: args) {
            this.conditions.put(a, new SimpleAttackConstraint(a));
        }

    }

    /**
     * learn a single labeling
     * i.e. compute the acceptance condition for each argument a wrt to the given labeling and combine it with the
     * previous acceptance condition of a
     * @param labeling some labeling of the set of arguments
     * @return true if the labeling was learned successfully
     */
    public boolean learnLabeling(Input labeling) {
        this.conditions.keySet().parallelStream().forEach(arg -> {
            SimpleAttackConstraint old_condition = this.conditions.get(arg);
            SimpleAttackConstraint new_condition = new SimpleAttackConstraint(arg, labeling);
            this.conditions.put(arg, new SimpleAttackConstraint(old_condition, new_condition));
        });
        return true;
    }

    /**
     * compute the set of argumentation frameworks that satisfy all acceptance conditions
     * 1. for each acceptance condition (argument) compute all models
     * 2. for each model compute a corresponding attack relation
     * 3. compute all possible combinations of the attack relations and we have all argumentation frame works satisfying the conditions
     * @return the set of argumentation frameworks obtained from learning
     */
    public Collection<DungTheory> getModels() {
        Collection<DungTheory> theories = new HashSet<>();
        theories.add(new DungTheory());
        for (Argument arg: this.args) {
            // for every argument get acceptance condition and combine with optional condition
            SimpleAttackConstraint attackConstraint = this.conditions.get(arg);
            AssociativePlFormula condition = attackConstraint.getCondition();
            AssociativePlFormula optionalCondition = attackConstraint.getOptionalCondition(this.args);
            // combine and transform to DNF
            AssociativePlFormula overallAcceptanceCondition = (AssociativePlFormula) new Conjunction(condition, new Disjunction(new Tautology(), optionalCondition)).toDnf().trim();
            // get all models of the condition
            //Collection<PossibleWorld> models = overallAcceptanceCondition.getModels();
            Collection<PossibleWorld> models = ModelComputation.getModelsOfDNF(overallAcceptanceCondition);
            //System.out.println("Arg: " + arg + "; Models: " + models.size());

            // create attack relations and assemble into all frameworks
            Collection<DungTheory> new_theories = new HashSet<>();
            for (PossibleWorld world: models) {
                DungTheory theory = new DungTheory();
                theory.addAll(this.args);
                for (Proposition p: world) {
                    Argument attacker = new Argument(p.getName());
                    theory.addAttack(attacker, arg);
                }
                for (DungTheory theory1: theories) {
                    DungTheory new_theory = new DungTheory(theory1);
                    new_theory.add(theory);
                    new_theories.add(new_theory);
                }
            }
            theories = new_theories;
        }
        return theories;
    }

    public DungTheory getModel() {
        DungTheory theory = new DungTheory();
        theory.addAll(this.args);
        Map<Argument, Collection<Attack>> partialAttackRelations = new ConcurrentHashMap<>();

        this.args.parallelStream().forEach(a -> {
            // use Sat4j solver included in Java. TODO should be optimized
            SatSolver solver = new Sat4jSolver();
            PlFormula condition = this.conditions.get(a).getCondition();
            PossibleWorld witness = (PossibleWorld) solver.getWitness(condition);
            Collection<Attack> partialAttackRelation = this.interpretationToAttacks(witness, a);
            partialAttackRelations.put(a, partialAttackRelation);
        });
        for (Collection<Attack> attacks: partialAttackRelations.values()) {
            theory.addAllAttacks(attacks);
        }
        return theory;
    }

    /**
     * compute the set of attacks that correspond to the given interpretation of an attack constraint
     * @param itp an interpretation
     * @param a the argument associated with the attack constraint the interpretation is coming from
     * @return the set of attacks corresponding to itp
     */
    private Collection<Attack> interpretationToAttacks(PossibleWorld itp, Argument a) {
        Collection<Attack> attacks = new HashSet<>();
        for (Proposition b: itp) {
            Attack attack = new Attack(new Argument(b.getName()), a);
            attacks.add(attack);
        }
        return attacks;
    }

    public void printStatus() {
        this.printStatus(false);
    }

    /**
     * print the current status of the acceptance conditions
     */
    public void printStatus(boolean dnf) {
        for (Argument arg: this.args) {
            SimpleAttackConstraint condition = this.conditions.get(arg);
            System.out.print(arg + "\t\t");
            if (!dnf) {
                System.out.print(condition.getCondition() + "\t\t\t");
            } else {
                System.out.print(condition.getCondition().toDnf() + "\t\t\t");
                System.out.println(condition.getCondition().getModels());
            }
            System.out.println(condition.getOptionalCondition(this.args));
        }
    }

    public long getNumberOfFrameworks() {
        return getNumberOfFrameworks(false);
    }

    public long getNumberOfFrameworks(boolean shortcut) {
        long total = 1;
        for (Argument arg: this.args) {
            SimpleAttackConstraint attackConstraint = this.conditions.get(arg);
            AssociativePlFormula condition = attackConstraint.getCondition();

            // optional attackers are all arguments which do not occur in the necessary condition
            int numOptionalArgs = this.args.size() - condition.getAtoms().size();

            // if there is any optional attacker, then there is still more than one framework possible
            if ( shortcut && numOptionalArgs > 0) {
                return -1;
            }

            if (shortcut && ModelComputation.existsMoreThanOneModelOfDNF(condition)) {
                return -1;
            }

            // number of frameworks is the number of models that the necessary condition has
            // i.e. all configurations of attacks that satisfy the conditions
            long numModelsCondition = ModelComputation.getModelsOfDNF(condition).size();

            // for the optional attacks we have 2^n possibilities
            long numModelsOptional = (long) Math.pow(2, numOptionalArgs);

            // the product of both above values is the number of partial attack relations of the argument
            long numModelsArg = numModelsCondition * numModelsOptional;

            //System.out.println(arg + ": " + numModelsArg);

            // in total the number of frameworks is the product of the value for each argument
            total = total * numModelsArg;
        }
        return total;
    }
}
