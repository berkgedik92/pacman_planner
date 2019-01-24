package Game;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class GameCreature {
	public Position currentPosition;
    public Position oldPosition;
}