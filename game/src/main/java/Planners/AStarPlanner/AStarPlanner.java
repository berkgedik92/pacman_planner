package Planners.AStarPlanner;

import Game.*;
import Planners.AbstractPlanner;
import java.util.*;

public class AStarPlanner extends AbstractPlanner {

	private Queue<Action> plannedActions = null;

	private static final Action[] actions = new Action[] {Action.STOP, Action.LEFT, Action.RIGHT, Action.UP, Action.DOWN};

    public AStarPlanner() {}

    //Starter function...
	@Override
	public Action getNextAction(BoardState state) {
		if (plannedActions == null) {
			plannedActions = new LinkedList<>();
			List<Action> actions = makePlan(state);
			plannedActions.addAll(actions);
			if (plannedActions.size() == 0)
				System.err.println("Cannot make a plan! Pacman will stop all time");
		}

		if (plannedActions.size() == 0)
			return Action.STOP;

		return plannedActions.poll();
    }

	@Override
	public boolean isTrained() {
        return true;
    }

	public List<Action> makePlan(BoardState state) {

        long startTime = System.currentTimeMillis();

        // A Queue to keep Nodes to process
        PriorityQueue<Node> nodeQueue = new PriorityQueue<>();

        //We have this to remember what states we visited so we will not visit them again.
		HashSet<String> hashOfVisitedStates = new HashSet<>();

		// Add initial node to the queue
		nodeQueue.add(new Node(state));
		Node solution = null;
		while (!nodeQueue.isEmpty()) {

			Node currentNode = nodeQueue.remove();

			// Check if this node is a solution, if so just return it.
            if (currentNode.getBoardState().remainingDotAmount == 0) {
                solution = currentNode;
                break;
            }

            // Add the state of this node to "hashOfVisitedStates" so we will never visit again this state.
            hashOfVisitedStates.add(currentNode.getStateHash());

			for (Action nextAction : actions) {

			    // On the state of the currentNode, try all possible actions... (i.e actions do not cause collision)
				if (currentNode.getBoardState().checkResult(nextAction) == ActionConsequence.FREE) {
					BoardState newState = new BoardState(currentNode.getBoardState(), nextAction);
                    Node nextNode = new Node(currentNode, nextAction, newState);

					/*
					    Check if the state of Node "next" is already visited, if so there is no point in adding
					    it to the "nodeQueue". Otherwise, add it to the priorityQueue.
					 */

					if (!hashOfVisitedStates.contains(nextNode.getStateHash()))
					    nodeQueue.add(nextNode);
				}
			}
		}

        List<Action> plan = new ArrayList<>();

		//If we cannot find a plan, just return empty action list as plan
		if (solution == null)
		    return plan;

        try {
            Node pointer = solution;
            while (pointer.getParent() != null) {
                plan.add(0, pointer.getAction());
                pointer = pointer.getParent();
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }

        long stopTime = System.currentTimeMillis();
        long elapsedTime = stopTime - startTime;
        System.out.println("Solved in " + elapsedTime + "ms amount of move is " + plan.size());
        return plan;
	}
}
