package Game;

import lombok.Getter;

@Getter
@Setter
public abstract class GameCreature {
	public Position currentPos;
    public Position oldPos;
}