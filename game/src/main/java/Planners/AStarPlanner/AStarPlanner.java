package Planners.AStarPlanner;

import Game.*;
import Planners.AbstractPlanner;

import java.util.*;

public class AStarPlanner extends AbstractPlanner {

	private Queue<Action> plannedActions = null;

    public AStarPlanner() {}

	@Override
	public Action getNextAction(BoardState state) {
		if (plannedActions == null) {
			plannedActions = new LinkedList<>();
			List<Action> actions = makePlan(state);
			plannedActions.addAll(actions);
		}

		if (plannedActions.size() == 0) {
			System.err.println("Cannot make a plan! Pacman will stop all time");
			return Action.STOP;
		}
		return plannedActions.poll();
    }

	@Override
	public boolean isTrained() {
		return false;
	}

	public List<Action> makePlan(BoardState state)  {
		long startTime = System.currentTimeMillis();
		List<Action> plan = new ArrayList<>();
		try {
			Node n = search(state);
			while (n.parent != null) {
				plan.add(0, n.getAction());
				n = n.parent;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			return plan;
		}

		long stopTime = System.currentTimeMillis();
		long elapsedTime = stopTime - startTime;
		System.out.println("Solved in " + elapsedTime + "ms amount of move is " + plan.size());
		return plan;
	}

	private Node search(BoardState wrapper) {
		//two lists keep track of states traversed and future states to check
		ArrayList<Node> open = new ArrayList<>();
		HashSet<String> closed = new HashSet<>();
		open.add(new Node(wrapper.pacman.getCurrentPosition(), null, Action.STOP, wrapper));//add the first state to open
		while (!open.isEmpty()) {
			//sort list for faster runtime
		    open.sort((arg0, arg1) -> {
			    if (arg0.getF() - arg1.getF() == 0)
			        return arg0.getH() - arg1.getH();
			    return arg0.getF() - arg1.getF();
            });

			Node q = open.remove(0);
			Node next;
			for (int move = 1; move < 5; move++) {
				// check each action
				Action nextAction = Action.STOP;
				switch (move) {
				case 1:// Left
					nextAction = Action.LEFT;
					break;
				case 2:// Right
					nextAction = Action.RIGHT;
					break;
				case 3:// Up
					nextAction = Action.UP;
					break;
				case 4:// Down
					nextAction = Action.DOWN;
					break;
				}

				if (q.board.checkResult(nextAction) == ActionConsequence.FREE) {//only check heuristic if is a valid move
					BoardState n = new BoardState(q.board, nextAction);
					next = new Node(n.pacman.getCurrentPosition(), q, nextAction, n);
					if (n.remainingDotAmount == 0) {//solution is found
						return next;
					} else {
						//check to see if state has already been checked, if not add to open
						boolean canAdd = true;
						String hash = next.board.hashFunction();
						if (!closed.contains(hash)) {
							for (Node n2 : open) {
								if (!canAdd)
									break;
								if (n2.f < next.f && n2.p.x == next.p.x && n2.p.y == next.p.y) {
									canAdd = false;
									break;
								}
							}
							if (canAdd)
								open.add(next);
						}
					}
				}
			}
			closed.add(q.board.hashFunction());//keep track of states traversed
		}
		return null;//if no plan is found
	}

	class Node {
		Node parent;
		Position p;
		int f = 0, g = 0, h = 0;//f for full heuristic, g keeps track of length of plan,
								//h is heuristic of either null, remaining food, or remaining food and distance to closest food
		Action act;
		BoardState board;

		Node(Position pos, Node p, Action a, BoardState b) {
			this.p = new Position(pos);
			this.parent = p;
			this.act = a;
			this.h = b.remainingDotAmount;
			this.board = b;
		}

		int getF() {
			if (this.parent != null)
				this.g = this.parent.g + 1;
			else
				this.g = 1;
			this.f = this.g + this.h;
			return this.h;
		}

		int getH() {
			return this.h;
		}

		Action getAction() {
			return act;
		}

	}
}
