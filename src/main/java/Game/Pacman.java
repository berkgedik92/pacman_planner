package Game;

import java.util.*;

import IPlanner.IPlanner;
import Main.Config;
import Main.YamlConfig;
import OnlinePlanner.ApproximateQPlanner;

public class Pacman extends GameCreature {

    private boolean planFailed = false;
    private Queue<Action> plannedActions = new LinkedList<>();
    private YamlConfig config;

    private IPlanner planner;

    public Pacman(Position pos) {
        this.currentPosition = new Position(pos);
        this.oldPosition = new Position(pos.y, pos.x);
        this.planner = new ApproximateQPlanner();
        this.config = YamlConfig.getInstance();
    }

    public IPlanner getPlanner() {
        return planner;
    }

    public void makeDecision(Action chosenAction) throws Exception {
        BoardState state = Board.getInstance().state;

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
