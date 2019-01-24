package Game;

import java.util.*;
import Main.Config;
import OnlinePlanner.ApproximateQPlanner;

public class Pacman extends GameCreature {

    private boolean planFailed = false;
    private Queue<Action> plannedActions = new LinkedList<>();

    private ApproximateQPlanner onlinePlanner;

    public Pacman(Position pos) {
        this.currentPosition = new Position(pos);
        this.oldPosition = new Position(pos.y, pos.x);
        this.onlinePlanner = new ApproximateQPlanner();
    }

    public void makeDecision(Action chosenAction, BoardState state) throws Exception {
        Config config = Config.getInstance();

        if (chosenAction == null)
            chosenAction = config.isOnlinePlanning() ? onlinePlanner.getNextAction(state) : chooseNextAction(state);
        
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

    private Action chooseNextAction(BoardState state) {

        if (planFailed)
            return Action.STOP;

        Config config = Config.getInstance();

        if (plannedActions.size() == 0) {
            config.setPlanner(config.getPlanner().reset());
            List<Action> actions = config.getPlanner().makePlan(state);
            if (actions.size() == 0) {
                System.err.println("Cannot make a plan! Pacman will stop all time");
                planFailed = true;
                return Action.STOP;
            }
            plannedActions.addAll(actions);
        }

        return plannedActions.poll();
    }

    public ApproximateQPlanner getOnlinePlanner() {
        return onlinePlanner;
    }
}
