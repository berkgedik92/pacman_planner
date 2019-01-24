package Game;

import java.util.List;

public interface IBoardStateObserver {

    void initialize(int rowAmount, int colAmount);
    void stateUpdateSignal(Position pacmanPosition, List<Position> monsterPositions, short[] boardData);
    void finishSignal();
}
