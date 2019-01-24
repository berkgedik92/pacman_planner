package Game;

import Main.Config;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class BoardState {

    //How many monsters we have in total
    public int monsterAmount;

    //Score
    public int score = 0;

    /*How many columns and rows are there in the board*/
    public int colAmount;
    public int rowAmount;

    /*Board status (state) will be kept here*/
    public short[] boardData;

    /* Helper fields */
    public int remainingDotAmount;
    public boolean isPacmanDead;

    /*GameCreatures*/
    public Pacman pacman;
    public List<Monster> monsters = new ArrayList<>();
    private List<IBoardStateObserver> observers = new ArrayList<>();

    public BoardState(int colAmount, int rowAmount, short[] boardData, List<Position> initialPos) {
        this.boardData = boardData;
        this.rowAmount = rowAmount;
        this.colAmount = colAmount;

        this.remainingDotAmount = 0;
        int cellAmount = rowAmount * colAmount;
        for (int i = 0; i < cellAmount; i++)
            if ((boardData[i] & 16) > 0)
                remainingDotAmount++;

        this.isPacmanDead = false;

        monsterAmount = initialPos.size() - 1;
        //Set initial positions of creatures
        pacman = new Pacman(initialPos.get(0));

        for (int i = 0; i < monsterAmount; i++)
            monsters.add(new Monster(initialPos.get(i + 1)));
    }

    public void initSignalToObservers() {
        for (IBoardStateObserver observer : observers)
            observer.initialize(rowAmount, colAmount);
    }

    public void pushToObservers() {
        Position pacmanPosition = new Position(pacman.currentPosition.y, pacman.currentPosition.x);
        List<Position> monsterPositions = new ArrayList<>();
        for (Monster m : monsters)
            monsterPositions.add(new Position(m.getCurrentPosition().y, m.getCurrentPosition().x));
        for (IBoardStateObserver observer : observers)
            observer.stateUpdateSignal(pacmanPosition, monsterPositions, boardData);
    }

    public void pushFinishSignal() {
        for (IBoardStateObserver observer : observers)
            observer.finishSignal();
    }

    public void attachObserver(IBoardStateObserver observer) {
        this.observers.add(observer);
        Position pacmanPosition = new Position(pacman.currentPosition.y, pacman.currentPosition.x);
        List<Position> monsterPositions = new ArrayList<>();
        for (Monster m : monsters)
            monsterPositions.add(new Position(m.getCurrentPosition().y, m.getCurrentPosition().x));
        observer.initialize(rowAmount, colAmount);
        observer.stateUpdateSignal(pacmanPosition, monsterPositions, boardData);
    }

    public void setMonsterMoves(List<Action[]> monsterActions) {
        for (int i = 0; i < monsterAmount; i++)
            monsters.get(i).setPolicy(monsterActions.get(i));
    }

    public BoardState(BoardState state, Action action) {

        this.colAmount = state.colAmount;
        this.rowAmount = state.rowAmount;
        this.remainingDotAmount = state.remainingDotAmount;
        Position curPacmanPos = state.pacman.getCurrentPosition();
        this.pacman = new Pacman(curPacmanPos);
        this.isPacmanDead = state.isPacmanDead;
        this.score = state.score;
        this.monsterAmount = state.monsterAmount;

        this.boardData = state.getBoardDataCopy();

        this.monsters = new ArrayList<>();
        for (int i = 0; i < state.monsters.size(); i++)
            this.monsters.add(new Monster(state.monsters.get(i)));


        if (action != null) {
            ActionConsequence cons = checkResult(action);

            pacman.setCurrentPosition(Position.giveConsequence(curPacmanPos, action));
            pacman.setOldPosition(curPacmanPos);

            System.err.println("OLD: " + pacman.getOldPosition() + " NEW: " + pacman.getCurrentPosition());

            //Check if collision happened
            if (cons == ActionConsequence.MONSTER)
                this.isPacmanDead = true;

            /*Check if pacman collected something*/
            int pos = pacman.getCurrentPosition().x + colAmount * pacman.getCurrentPosition().y;
            int ch = boardData[pos];

            if ((ch & 16) != 0) {
                boardData[pos] = (short) (ch & 15);
                this.remainingDotAmount--;
            }

            for (Monster m : this.monsters) {
                try {
                    m.makeDecision(this);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private short[] getBoardDataCopy() {
        int totalCell = colAmount * rowAmount;
        short[] copyData = new short[boardData.length];
        if (totalCell >= 0)
            System.arraycopy(boardData, 0, copyData, 0, totalCell);
        return copyData;
    }

    public ActionConsequence checkResult(Action action) {
        Position pacmanPos = pacman.getCurrentPosition();
        /*Check if there is a wall*/
        int pos = pacmanPos.x + colAmount * pacmanPos.y;

        switch (action) {
            case LEFT:
                if ((boardData[pos] & 1) != 0 || (pacmanPos.x == 0))
                    return ActionConsequence.WALL;
                break;

            case RIGHT:
                if ((boardData[pos] & 4) != 0 || (pacmanPos.x == colAmount - 1))
                    return ActionConsequence.WALL;
                break;

            case UP:
                if ((boardData[pos] & 2) != 0 || (pacmanPos.y == 0))
                    return ActionConsequence.WALL;
                break;

            case DOWN:
                if ((boardData[pos] & 8) != 0 || (pacmanPos.y == (rowAmount - 1)))
                    return ActionConsequence.WALL;
        }

        Position pacmanNextPos = Position.giveConsequence(pacmanPos, action);

        //Check if there is a monster
        for (Monster m : monsters) {
            Action[] actions = m.giveActionsToDo(1);

            Position currentMonsterPos = new Position(m.getCurrentPosition().y, m.getCurrentPosition().x);
            Position nextMonsterPos = Position.giveConsequence(currentMonsterPos, actions[0]);

            //Two cases: 1) MonsterPosNextTime = PacmanPosNextTime
            if (Position.isEqual(nextMonsterPos, pacmanNextPos))
                return ActionConsequence.MONSTER;

            //2) MonsterCurrentPos = PacmanNextPos AND MonsterNextPos = PacmanCurrentPos
            if (Position.isEqual(currentMonsterPos, pacmanNextPos) && Position.isEqual(nextMonsterPos, pacmanPos))
                return ActionConsequence.MONSTER;
        }
        return ActionConsequence.FREE;
    }

    public boolean isFoodAt(Position p) {
        return isFoodAt(p.x, p.y);
    }

    private boolean isFoodAt(int x, int y) {
        int pos = x + colAmount * y;
        return (boardData[pos] & 16) > 0;
    }

    //////////////////////////////////////////////////////
    //          Helper methods for ease of use          //
    // These are to be used for planner implementations //
    //////////////////////////////////////////////////////

    public boolean checkActionValidity(Position pos, Action act) {
        short curPosValue = boardData[pos.x + colAmount * pos.y];
        return (curPosValue & act.getCode()) == 0;
    }

    public List<Position> getValidNeighborCells(Position pos) {
        List<Position> moves = new ArrayList<>();
        short curPosValue = boardData[pos.x + colAmount * pos.y];
        for (Action a : Action.values()) {
            if ((curPosValue & a.getCode()) == 0) {
                moves.add(Position.giveConsequence(pos, a));
            }
        }
        return moves;
    }

    public List<Action> getValidActions(Position pos) {
        List<Action> moves = new ArrayList<>();

        short curPosValue = boardData[pos.x + colAmount * pos.y];
        for (Action a : Action.values()) {
            if ((curPosValue & a.getCode()) == 0) {
                moves.add(a);
            }
        }
        return moves;
    }

    public short closestFood(Position pos) {
        short distance = 0;
        List<Position> options = getValidNeighborCells(pos);
        HashSet<Position> expanded = new HashSet<>();

        while (!options.isEmpty()) {
            distance++;
            List<Position> possible = new ArrayList<>();
            for (Position p : options) {
                expanded.add(p);
                if (isFoodAt(p))
                    return distance;
                else
                    for (Position cell : getValidNeighborCells(p))
                        if (!expanded.contains(cell))
                            possible.add(cell);
            }
            options.clear();
            options.addAll(possible);
        }
        return Short.MAX_VALUE;
    }

    public short foodWithinOneStep(Position pos) {
        short dots = 0;
        List<Position> neighbours = getValidNeighborCells(pos);
        for (Position p : neighbours)
            if (isFoodAt(p))
                dots++;
        return dots;
    }

    public boolean isGameOver() {
        return isPacmanDead || remainingDotAmount == 0;
    }

    public short closestMonster(Position pos) {
        short minDistance = Short.MAX_VALUE, dist;

        for (Monster m : monsters) {
            dist = (short)pos.manhattanDistance(m.getCurrentPosition());
            if (dist < minDistance)
                minDistance = dist;
        }
        return minDistance;
    }

    public String hashFunction() {

        StringBuilder b = new StringBuilder();

        //Consider remaining dots
        int bitAmount = colAmount * rowAmount;
        int remaining = bitAmount % 8;
        if (remaining != 0)
            bitAmount += (8 - remaining);

        byte[] bitsForDots = new byte[bitAmount / 8];
        for (int i = 0; i < colAmount * rowAmount; i++)
        {
            int currentByte = i / 8;
            int currentBit = i % 8;
            if ((boardData[i] & 16) > 0)
                bitsForDots[currentByte] |= 1 << currentBit;
        }

        b.append(new String(bitsForDots));

        int pacmanIndex = pacman.currentPosition.y * colAmount + pacman.currentPosition.x;
        b.append(pacmanIndex).append("_");

        //Consider the monsters
        for (Monster monster : monsters) {
            int monsterPos = monster.currentPosition.y * colAmount + monster.currentPosition.x;
            String mons = String.valueOf(monsterPos) + "*" +
                    monster.item + "*" + monster.seqUp + "#";
            b.append(mons);
        }

        return b.toString();
    }

    public void checkMaze() {

        /*Check if a collision happen:
        (i.e either Game.Pacman and a monster is in the same cell or a monster was
        in the cell Game.Pacman was and Game.Pacman was in the cell this monster was*/

        boolean collision = false;

        int pacmanCurX = pacman.getCurrentPosition().x;
        int pacmanCurY = pacman.getCurrentPosition().y;
        int pacmanOldX = pacman.getOldPosition().x;
        int pacmanOldY = pacman.getOldPosition().y;

        for (int i = 0; i < monsterAmount; i++) {
            int monsterCurX = monsters.get(i).getCurrentPosition().x;
            int monsterCurY = monsters.get(i).getCurrentPosition().y;
            int monsterOldX = monsters.get(i).getOldPosition().x;
            int monsterOldY = monsters.get(i).getOldPosition().y;

            if (pacmanCurX == monsterCurX && pacmanCurY == monsterCurY) {
                collision = true;
                break;
            }

            if (pacmanCurX == monsterOldX && pacmanCurY == monsterOldY && pacmanOldX == monsterCurX && pacmanOldY == monsterCurY) {
                collision = true;
                break;
            }
        }

        Config config = Config.getInstance();

        if (collision) {
            if (pacman.getPlanner().isTrained())
                pushFinishSignal();
            score -= 500;
            isPacmanDead = true;
            System.err.println("Pacman is dead!");
            return;
        }

        /*Check if pacman collected something*/
        int pos = pacman.getCurrentPosition().x + colAmount * pacman.getCurrentPosition().y;

        int ch = boardData[pos];

        if ((ch & 16) != 0) {
            boardData[pos] = (short) (ch & 15);
            score += 10;
            remainingDotAmount--;
        }

        /*Check if everything is collected, so the game is finished*/
        boolean finished = true;

        for (int i = 0; i < rowAmount * colAmount && finished; i++)
            if ((boardData[i] & 48) != 0)
                finished = false;

        if (finished) {
            if (pacman.getPlanner().isTrained())
                pushFinishSignal();
            System.err.println(remainingDotAmount);
            System.err.println("Pacman won the game!");
        }
    }
}
