package syntax;

import org.tweetyproject.arg.dung.semantics.ArgumentStatus;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.logics.pl.syntax.*;

import java.util.Collection;
import java.util.HashSet;

public class ClausalAttackConstraint implements AttackConstraint<Collection<PlFormula>> {
    private Argument argument;

    private Collection<PlFormula> clauses;

    public ClausalAttackConstraint(Argument arg) {
        this.argument = arg;
        this.clauses = new HashSet<>();
    }

    public ClausalAttackConstraint(Argument arg, Input input) {
        this.argument = arg;
        this.clauses = this.getConditionForArgument(arg, input);
    }

    public ClausalAttackConstraint(ClausalAttackConstraint condition1, ClausalAttackConstraint condition2) {
        if (condition1.getArgument() != condition2.getArgument()) {
            throw new IllegalArgumentException("Should not happen");
        }
        this.argument = condition1.getArgument();
        this.clauses = new HashSet<>();
        this.clauses.addAll(condition1.getCondition());
        this.clauses.addAll(condition2.getCondition());
    }

    public Collection<PlFormula> getCondition() {
        return this.clauses;
    }

    public Argument getArgument() {
        return this.argument;
    }

    private Collection<PlFormula> getConditionForArgument(Argument arg, Input input) {
        return switch (input.getSemantics()) {
            case CF -> this.getConditionForArgumentCF(arg, input);
            case ADM -> this.getConditionForArgumentADM(arg, input);
            case CO -> this.getConditionForArgumentCO(arg, input);
            case ST -> this.getConditionForArgumentST(arg, input);
            default -> throw new IllegalArgumentException("Unsupported Semantics");
        };
    }

    private Collection<PlFormula> getConditionForArgumentCF(Argument arg, Input input) {
        Collection<PlFormula> formula = new HashSet<>();
        switch (input.get(arg)) {
            case UNDECIDED:
            case IN:
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                break;
            case OUT:
                Collection<PlFormula> poss_attackers = new HashSet<>();
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    poss_attackers.add(new Proposition(b.getName()));
                }
                formula.add(new Disjunction(poss_attackers));
                break;
        }
        return formula;
    }

    private Collection<PlFormula> getConditionForArgumentADM(Argument arg, Input input) {
        Collection<PlFormula> formula = new HashSet<>();
        switch (input.get(arg)) {
            case IN:
                // an IN argument can not be attacked by another IN argument or a UNDECIDED argument (i.e. then it would not be defended)
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.UNDECIDED)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                break;
            case OUT:
                Collection<PlFormula> poss_attackers = new HashSet<>();
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    poss_attackers.add(new Proposition(b.getName()));
                }
                formula.add(new Disjunction(poss_attackers));
                break;
            case UNDECIDED:
                // an UNDECIDED argument can not be attacked by an IN argument (i.e. then it would be OUT)
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                break;
        }
        return formula;
    }

    private Collection<PlFormula> getConditionForArgumentCO(Argument arg, Input input) {
        Collection<PlFormula> formula = new HashSet<>();
        switch (input.get(arg)) {
            case IN:
                // an IN argument can not be attacked by another IN argument or a UNDECIDED argument (i.e. then it would not be defended)
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.UNDECIDED)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                break;
            case OUT:
                Collection<PlFormula> poss_attackers = new HashSet<>();
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    poss_attackers.add(new Proposition(b.getName()));
                }
                formula.add(new Disjunction(poss_attackers));
                break;
            case UNDECIDED:
                // an UNDECIDED argument can not be attacked by an IN argument (i.e. then it would be OUT)
                for (Argument b: input.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    formula.add(new Negation(new Proposition(b.getName())));
                }
                Collection<PlFormula> und_arguments = new HashSet<>();
                for (Argument c: input.getArgumentsOfStatus(ArgumentStatus.UNDECIDED)) {
                    und_arguments.add(new Proposition(c.getName()));
                }
                AssociativePlFormula sub_formula = new Disjunction(und_arguments);
                formula.add(sub_formula);
                break;
        }
        return formula;
    }

    private Collection<PlFormula> getConditionForArgumentST(Argument arg, Input input) {
        if (!input.getArgumentsOfStatus(ArgumentStatus.UNDECIDED).isEmpty())
            throw new IllegalArgumentException("Labeling is not stable");
        return getConditionForArgumentCF(arg, input);
    }

    @Override
    public String toString() {
        return argument + ":\t\t" + clauses;
    }

}
