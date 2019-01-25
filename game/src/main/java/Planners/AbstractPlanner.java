package Planners;

import Game.Action;
import Game.BoardState;
import Main.Config;

public abstract class AbstractPlanner {

    protected static Config config;

    public AbstractPlanner() {
        if (config == null)
            config = Config.getInstance();
    }

    public abstract Action getNextAction(BoardState state);
    public abstract boolean isTrained();
}
