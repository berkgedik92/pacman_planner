package Game;

import java.util.*;

import IPlanner.IPlanner;
import Main.Config;
import OnlinePlanner.ApproximateQPlanner;

public class Pacman extends GameCreature {

    private boolean planFailed = false;
    private Queue<Action> plannedActions = new LinkedList<>();
    private Config config;

    private IPlanner planner;

    public Pacman(Position pos) {
        this.currentPosition = new Position(pos);
        this.oldPosition = new Position(pos.y, pos.x);
        this.planner = new ApproximateQPlanner();
        this.config = Config.getInstance();
    }

    public IPlanner getPlanner() {
        return planner;
    }

    public void makeDecision(Action chosenAction, BoardState state) throws Exception {
        if (chosenAction == null)
            chosenAction = this.planner.getNextAction(state);
        
        if (!state.checkActionValidity(currentPosition, chosenAction))
            throw new Exception("Pacman planner tries to make invalid move!");

        oldPosition.y = currentPosition.y;
        oldPosition.x = currentPosition.x;

        switch (chosenAction) {
            case UP: currentPosition.y--; break;
            case DOWN: currentPosition.y++; break;
            case LEFT: currentPosition.x--; break;
            case RIGHT: currentPosition.x++; break;
            case STOP: break;
        }
    }
}
