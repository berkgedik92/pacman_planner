package Game;

import Main.Config;
import lombok.Getter;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Getter
public class Monster extends GameCreature {

    private Action[] policy = null;   // a set of predefined deterministic moves
    public int item = 0;    // current move index of a predefined policy
    public boolean seqUp = true;   //if moves are deterministic, next move will be policy[item+1] or policy[item-1]
    private Config config;

    public Monster(Position pos) {
        this.currentPosition = pos;
        this.oldPosition = new Position(pos.y, pos.x);
        this.config = Config.getInstance();
    }

    public void setPolicy(Action[] actions)
    {
        this.policy = actions;
    }

    // OLD: Creates a new instance of monster and makes the first move in queue (needed for A*)
    // NEW: Just a copy constructor. if you need to make a move, just call makeDecision function
    public Monster(Monster old) {
        this.currentPosition = new Position(old.currentPosition.y, old.currentPosition.x);
        this.oldPosition = new Position(old.oldPosition.y, old.oldPosition.x);
        this.policy = old.policy;
        this.item = old.item;
        this.seqUp = old.seqUp;
    }

    public void makeDecision(BoardState state) throws Exception {

        Action decidedAction = (config.getBoolean("deterministic_monsters")) ? makeDeterministicDecision() : makeRandomDecision(state);

        if (!state.checkActionValidity(currentPosition, decidedAction))
            throw new Exception("Monster object tries to make invalid move!");

        oldPosition.y = currentPosition.y;
        oldPosition.x = currentPosition.x;

        switch (decidedAction) {
            case UP: currentPosition.y--; break;
            case DOWN: currentPosition.y++; break;
            case LEFT: currentPosition.x--; break;
            case RIGHT: currentPosition.x++; break;
            case STOP: break;
        }
    }

    public Action[] giveActionsToDo(int length) {
        int itemL = item;
        boolean seqUpL = seqUp;
        Action[] result = new Action[length];

        for (int i = 0; i < length; i++) {
            if (seqUpL && itemL == policy.length) {
                seqUpL = false;
                itemL--;
            }
            else if (!seqUpL && itemL == -1) {
                seqUpL = true;
                itemL++;
            }

            if (seqUpL)
                result[i] = policy[itemL++];
            else
                result[i] = Action.opposite(policy[itemL--]);
        }

        return result;
    }

    /*We assume here that policy array is not null*/
    private Action makeDeterministicDecision() {

        /*Decide should we change or direction (go left-to-right or right-to-left in the "policy")*/
        if (seqUp && item == policy.length) {
            seqUp = false;
            item--;
        }
        else if (!seqUp && item == -1) {
            seqUp = true;
            item++;
        }

        if (seqUp)
            return policy[item++];
        else
            return Action.opposite(policy[item--]);
    }

    private Action makeRandomDecision(BoardState state) {
        int pos = currentPosition.x + state.colAmount * currentPosition.y;

        List<Action> possibleActions = new ArrayList<>();

        //Let's not allow monsters to STOP for now (for debug purposes)
        //possibleActions.add(Game.Action.STOP);

        //if there is no left wall on the cell and monster is not in the leftmost column
        if ((state.boardData[pos] & 1) == 0 && (pos % state.colAmount > 0))
            possibleActions.add(Action.LEFT);

        //if there is no right wall on the cell and monster is not in the rightmost column
        if ((state.boardData[pos] & 4) == 0 && (pos % state.colAmount) < (state.colAmount - 1))
            possibleActions.add(Action.RIGHT);

        //if there is no bottom wall on the cell and monster is not in the bottom row
        if ((state.boardData[pos] & 8) == 0 && (pos / state.colAmount) < (state.rowAmount - 1))
            possibleActions.add(Action.DOWN);

        //if there is no top wall on the cell and monster is not in the top column
        if ((state.boardData[pos] & 2) == 0 && (pos / state.colAmount) > 0)
            possibleActions.add(Action.UP);

        //Select an action among possible actions
        return possibleActions.get(new Random().nextInt(possibleActions.size()));
    }
}
