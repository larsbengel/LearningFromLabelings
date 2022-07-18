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

package old;

import org.tweetyproject.arg.dung.semantics.ArgumentStatus;
import org.tweetyproject.arg.dung.syntax.Argument;
import org.tweetyproject.logics.pl.syntax.*;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Lars Bengel
 */
public class AcceptanceCondition {

    private Argument argument;
    private AssociativePlFormula condition;

    public AcceptanceCondition(Argument arg) {
        this.argument = arg;
        this.condition = new Conjunction();
    }

    public AcceptanceCondition(Argument arg, Example example) {
        this.argument = arg;
        this.condition = getConditionForArgument(arg, example);
    }

    public AcceptanceCondition(AcceptanceCondition condition1, AcceptanceCondition condition2) {
        if (condition1.getArgument() != condition2.getArgument()) {
            throw new IllegalArgumentException("Should not happen");
        }
        this.argument = condition1.getArgument();
        this.condition = (AssociativePlFormula) new Conjunction(condition1.getCondition(), condition2.getCondition()).collapseAssociativeFormulas().toDnf().trim();

    }

    public AssociativePlFormula getCondition() {
        return condition;
    }

    private Argument getArgument() {
        return this.argument;
    }

    public AssociativePlFormula getConditionForArgument(Argument arg, Example example) {
        switch (example.getSemantics()) {
            case CF:
                return this.getConditionForArgumentCF(arg, example);
            case ADM:
                return this.getConditionForArgumentADM(arg, example);
            case CO:
                return this.getConditionForArgumentCO(arg, example);
            case ST:
                return this.getConditionForArgumentST(arg, example);
            default:
                throw new IllegalArgumentException("Unsupported Semantics");
        }
    }

    private AssociativePlFormula getConditionForArgumentCF(Argument arg, Example example) {
        AssociativePlFormula formula = new Conjunction();
        switch (example.get(arg)) {
            case UNDECIDED:
            case IN:
                Collection<PlFormula> cf_arguments = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    cf_arguments.add(new Negation(new Proposition(b.getName())));
                }
                formula = new Conjunction(cf_arguments);
                break;
            case OUT:
                Collection<PlFormula> poss_attackers = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    poss_attackers.add(new Proposition(b.getName()));
                }
                formula = new Disjunction(poss_attackers);
                break;
        }
        return formula;
    }

    private AssociativePlFormula getConditionForArgumentADM(Argument arg, Example example) {
        AssociativePlFormula formula = new Conjunction();
        switch (example.get(arg)) {
            case IN:
                // an IN argument can not be attacked by another IN argument or a UNDECIDED argument (i.e. then it would not be defended)
                Collection<PlFormula> cf_arguments = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    cf_arguments.add(new Negation(new Proposition(b.getName())));
                }
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.UNDECIDED)) {
                    cf_arguments.add(new Negation(new Proposition(b.getName())));
                }
                formula = new Conjunction(cf_arguments);
                break;
            case OUT:
                Collection<PlFormula> poss_attackers = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    poss_attackers.add(new Proposition(b.getName()));
                }
                formula = new Disjunction(poss_attackers);
                break;
            case UNDECIDED:
                // an UNDECIDED argument can not be attacked by an IN argument (i.e. then it would be OUT)
                Collection<PlFormula> in_arguments = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    in_arguments.add(new Negation(new Proposition(b.getName())));
                }
                formula = new Conjunction(in_arguments);
                break;


        }
        return formula;
    }

    private AssociativePlFormula getConditionForArgumentCO(Argument arg, Example example) {
        AssociativePlFormula formula = new Conjunction();
        switch (example.get(arg)) {
            case IN:
                // an IN argument can not be attacked by another IN argument or a UNDECIDED argument (i.e. then it would not be defended)
                Collection<PlFormula> cf_arguments = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    cf_arguments.add(new Negation(new Proposition(b.getName())));
                }
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.UNDECIDED)) {
                    cf_arguments.add(new Negation(new Proposition(b.getName())));
                }
                formula = new Conjunction(cf_arguments);
                break;
            case OUT:
                Collection<PlFormula> poss_attackers = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    poss_attackers.add(new Proposition(b.getName()));
                }
                formula = new Disjunction(poss_attackers);
                break;
            case UNDECIDED:
                // an UNDECIDED argument can not be attacked by an IN argument (i.e. then it would be OUT)
                Collection<PlFormula> in_arguments = new HashSet<>();
                for (Argument b: example.getArgumentsOfStatus(ArgumentStatus.IN)) {
                    in_arguments.add(new Negation(new Proposition(b.getName())));
                }
                formula = new Conjunction(in_arguments);
                Collection<PlFormula> und_arguments = new HashSet<>();
                for (Argument c: example.getArgumentsOfStatus(ArgumentStatus.UNDECIDED)) {
                    und_arguments.add(new Proposition(c.getName()));
                }
                AssociativePlFormula sub_formula = new Disjunction(und_arguments);

                formula = new Conjunction(formula, sub_formula);
                break;
        }
        return formula;
    }

    private AssociativePlFormula getConditionForArgumentST(Argument arg, Example example) {
        if (!example.getArgumentsOfStatus(ArgumentStatus.UNDECIDED).isEmpty())
            throw new IllegalArgumentException("Labeling is not stable");
        return getConditionForArgumentCF(arg, example);
    }

    /**
     * compute the optional acceptance condition for this argument based on its acceptance condition
     * @param arguments the set of all arguments
     * @return the optional acceptance condition of this argument
     */
    public AssociativePlFormula getOptionalCondition(Collection<Argument> arguments) {
        Collection<Argument> args = new HashSet<>();
        for (Proposition atom: this.condition.getAtoms()) {
            args.add(new Argument(atom.getName()));
        }
        Collection<Argument> optionalArguments = new HashSet<>(arguments);
        optionalArguments.removeAll(args);
        Collection<PlFormula> optionalAtoms = new HashSet<>();
        for (Argument a: optionalArguments) {
            optionalAtoms.add(new Proposition(a.getName()));
        }
        return new Disjunction(optionalAtoms);
    }

    @Override
    public String toString() {
        return argument + ":\t\t" + condition;
    }
}
