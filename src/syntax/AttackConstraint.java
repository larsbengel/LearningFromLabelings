package syntax;

import org.tweetyproject.arg.dung.syntax.Argument;

public interface AttackConstraint<T> {
    public T getCondition();
    public Argument getArgument();
}
