package Game;

import java.util.*;

import Planners.AStarPlanner.AStarPlanner;
import Planners.IPlanner;
import Main.Config;
import Planners.OnlinePlanner.ApproximateQPlanner;
import Planners.SATSolver.SATPlanner;

public class Pacman extends GameCreature {

    private boolean planFailed = false;
    private Queue<Action> plannedActions = new LinkedList<>();

    private IPlanner planner;

    public Pacman(Position pos) {
        this.currentPosition = new Position(pos);
        this.oldPosition = new Position(pos.y, pos.x);

        String planner = Config.getInstance().getString("planner");

        switch (planner) {
            case "online":
                this.planner = new ApproximateQPlanner();
                break;
            case "astar":
                this.planner = new AStarPlanner();
                break;
            case "sat":
                this.planner = new SATPlanner();
                break;
            default:
                throw new RuntimeException("Invalid argument for 'planner' in the configuration file. " +
                        "(Written : " + planner + " , expected: online/astar/sat)");
        }
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
