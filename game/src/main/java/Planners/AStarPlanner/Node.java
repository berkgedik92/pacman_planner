package Planners.AStarPlanner;

import Game.Action;
import Game.BoardState;
import Game.Position;
import lombok.Getter;

@Getter
class Node implements Comparable<Node> {

    private Node parent;
	private BoardState boardState;

	private int currentCost = 0;

	//What action is performed to come to this node from its parent node
	private Action action;
    private String stateHash;

	Node(BoardState boardState) {
		this.boardState = boardState;
		this.stateHash = boardState.getHashfOfState();
	}

	Node(Node parent, Action action, BoardState boardState) {
		this.parent = parent;
		this.action = action;
		this.boardState = boardState;
        this.stateHash = boardState.getHashfOfState();
    }

	private int guessTotalCost() {
		if (this.parent != null)
			this.currentCost = this.parent.currentCost + 1;
		else
			this.currentCost = 1;
		return this.currentCost + guessRemainingCost();
	}

	private int guessRemainingCost() {
		return boardState.remainingDotAmount;
	}

    @Override
    public int compareTo(Node otherNode) {
        if (this.guessTotalCost() - otherNode.guessTotalCost() == 0)
            return this.guessRemainingCost() - otherNode.guessRemainingCost();
        return this.guessTotalCost() - otherNode.guessTotalCost();
	}
}
