package Game;

import java.util.*;
import Main.Config;
import OnlinePlanner.ApproximateQPlanner;

public class Pacman extends GameCreature {

    private boolean planFailed = false;
    private Queue<Action> plannedActions = new LinkedList<>();

    private ApproximateQPlanner onlinePlanner;

    public Pacman(Position pos) {
        this.currentPos = new Position(pos);
        this.oldPos = new Position(pos.y, pos.x);
        this.onlinePlanner = new ApproximateQPlanner();
    }

    public void makeDecision(Action chosenAction) throws Exception {
        BoardState state = Board.getInstance().state;
        if (chosenAction == null)
            chosenAction = Config.isOnlinePlanning ? onlinePlanner.getNextAction(state) : chooseNextAction(state);
        
        if (!state.checkActionValidity(currentPos, chosenAction))
            throw new Exception("Game.Pacman planner tries to make invalid move!");

        oldPos.y = currentPos.y;
        oldPos.x = currentPos.x;

        switch (chosenAction) {
            case UP: currentPos.y--; break;
            case DOWN: currentPos.y++; break;
            case LEFT: currentPos.x--; break;
            case RIGHT: currentPos.x++; break;
            case STOP: break;
        }
    }

    private Action chooseNextAction(BoardState state) {

        if (planFailed)
            return Action.STOP;

        if (plannedActions.size() == 0) {
            Config.planner = Config.planner.reset();
            List<Action> actions = Config.planner.makePlan(state);
            if (actions.size() == 0) {
                System.err.println("Cannot make a plan! Game.Pacman will stop all time");
                planFailed = true;
                return Action.STOP;
            }
            for (int i = 0; i < actions.size(); i++)
                plannedActions.add(actions.get(i));
        }

        return plannedActions.poll();
    }

    public ApproximateQPlanner getOnlinePlanner() {
        return onlinePlanner;
    }
}
