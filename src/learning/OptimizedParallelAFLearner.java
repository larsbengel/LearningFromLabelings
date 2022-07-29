package learning;


import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.arg.dung.syntax.Attack;
import org.tweetyproject.arg.dung.syntax.DungTheory;
import org.tweetyproject.commons.Interpretation;
import org.tweetyproject.logics.pl.sat.Sat4jSolver;
import org.tweetyproject.logics.pl.sat.SatSolver;
import org.tweetyproject.logics.pl.semantics.PossibleWorld;
import org.tweetyproject.logics.pl.syntax.*;
import syntax.ClausalAttackConstraint;
import syntax.Input;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of the iterative algorithm for learning argumentation frameworks from labelings
 * @author Lars Bengel
 */
public class OptimizedParallelAFLearner implements AFLearner {

    /* the set of all arguments */
    private Collection<Argument> args;
    /* structure for storing the acceptance condition of each argument */
    private Map<Argument, ClausalAttackConstraint> conditions;

    /**
     * initialize the Learner with a set of arguments
     * @param args a set of arguments
     */
    public OptimizedParallelAFLearner(Collection<Argument> args) {
        this.args = args;
        this.conditions = new ConcurrentHashMap<>();
        for (Argument a: args) {
            this.conditions.put(a, new ClausalAttackConstraint(a));
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
        this.args.parallelStream().forEach(arg -> {
            ClausalAttackConstraint old_condition = this.conditions.get(arg);
            ClausalAttackConstraint new_condition = new ClausalAttackConstraint(arg, labeling);
            ClausalAttackConstraint combined_condition = new ClausalAttackConstraint(old_condition, new_condition);
            this.conditions.put(arg, combined_condition);
        });
        return true;
    }

    @Override
    public Collection<DungTheory> getModels() {
        return null;
    }

    // TODO: doesnt work like this probably. need an add function for the ClausalAttackConstraint
    public boolean learnLabelings(Collection<Input> inputs) {
        //inputs.parallelStream().forEach(this::learnLabeling);
        return true;
    }

    /**
     * compute a single model that satisfies all attack constraints
     * computation for each argument constraint is independent and thus can be done in parallel
     * @return a dung theory for which all attack constraints are satisfied
     */
    public DungTheory getModel() {
        DungTheory theory = new DungTheory();
        theory.addAll(this.args);

        Map<Argument, Collection<Attack>> partialAttackRelations = new ConcurrentHashMap<>();

        SatSolver solver = new Sat4jSolver();
        this.args.parallelStream().forEach(a -> {
            Collection<PlFormula> kb = this.conditions.get(a).getCondition();
            PossibleWorld witness = (PossibleWorld) solver.getWitness(kb);
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
            ClausalAttackConstraint condition = this.conditions.get(arg);
            System.out.print(arg + "\t\t");
            if (!dnf) {
                System.out.print(condition.getCondition() + "\t\t\t");
            } else {
                System.out.print(condition.getCondition() + "\t\t\t");
                System.out.println(condition.getCondition());
            }
        }
    }

    public long getNumberOfFrameworks() {
        return getNumberOfFrameworks(false);
    }

    public long getNumberOfFrameworks(boolean shortcut) {
        return 0;
    }
}
