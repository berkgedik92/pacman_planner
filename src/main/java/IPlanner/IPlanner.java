package IPlanner;

import Game.Action;
import Game.BoardState;
import java.util.List;

public interface IPlanner {
    Action getNextAction(BoardState state);
    IPlanner reset();   /*in order to get a clean planner (if we will call planner several times, we need a clear cache)
                        if, for some reason, you don't want the cache of the planner not be cleaned, then return the instance
                        (this), otherwise return new <IPlanner>();*/

    boolean isTrained();
}
