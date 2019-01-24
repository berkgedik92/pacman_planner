package Game;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
abstract class GameCreature {
	public Position currentPosition;
    public Position oldPosition;
}