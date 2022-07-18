
package old;

import org.tweetyproject.logics.pl.semantics.PossibleWorld;
import org.tweetyproject.logics.pl.syntax.*;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Lars Bengel
 */
public class ModelComputation {
    public static PossibleWorld getModelOfConjunction(Conjunction condition) {
        Collection<Proposition> accepted = new HashSet<>();
        for (PlFormula literal: condition.getLiterals()) {
            if (literal instanceof Negation) {
            } else {
                accepted.addAll(literal.getAtoms());
            }
        }
        return new PossibleWorld(accepted);
    }

    public static Collection<PossibleWorld> getModelsOfDNF(AssociativePlFormula condition) {
        Collection<PossibleWorld> models = new HashSet<>();
        if (condition instanceof Disjunction) {
            models.addAll(condition.getModels());
            //for (PlFormula conj: condition.getFormulas()) {
            //    models.add(ModelComputation.getModelOfConjunction((Conjunction) conj));
            //}
            return models;
        } else if (condition instanceof Conjunction) {
            models.add(ModelComputation.getModelOfConjunction((Conjunction) condition));
        }
        return models;
    }

    public static boolean existsMoreThanOneModelOfDNF(AssociativePlFormula condition) {
        if (condition instanceof Disjunction) {
            return true;
        } else {
            return !(condition instanceof Conjunction);
        }
    }

    public static PossibleWorld getAnyModelOfDNF(AssociativePlFormula condition) {
        return ModelComputation.getModelOfConjunction((Conjunction) condition.getFormulas().iterator().next());
    }
}
