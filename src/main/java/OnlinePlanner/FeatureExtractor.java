/*
* @Author: Dmytro Kalpakchi
* @Date:   2016-10-07 21:13:51
* @Last Modified by:   Dmytro Kalpakchi
* @Last Modified time: 2016-10-14 15:11:12
*/

package OnlinePlanner;

import Game.*;

class FeatureExtractor {
	/**
	 * Array of features for pacman boards with the following elements:
	 * 0 - bias feature
	 * 1 - whether food will be eaten
	 * 2 - how far away the next food is
	 * 3 - number of monsters one step away
	 * 4 - is it a repeated action for pacman (small loop of length 2)
	 */
	
	static final int FEATURES_NUM = 5;

	static double[] getFeatures(BoardState state, Action nextAction) {
		double[] features = new double[FEATURES_NUM];
		features[0] = 1;

		Position pacmanCurPos = state.pacman.currentPosition;
		Position pacmanNextPos = Position.giveConsequence(pacmanCurPos, nextAction);

		for (Monster m : state.monsters) {
			Position monsterCurPos = m.currentPosition;
			for (Position mp : state.getValidNeighborCells(monsterCurPos)) {
				if (Position.isEqual(mp, pacmanNextPos))
					features[3]++;
			}
		}

		features[3] /= state.monsterAmount;

		// cannot convert from boolean to short normally in java - don't really know the reason
		features[1] = (state.isFoodAt(pacmanNextPos) ? 1 : 0);
		features[2] = (double)(state.closestFood(pacmanNextPos)) / (state.rowAmount* state.colAmount);

		// should be general cycle avoidance
		features[4] = (Position.isEqual(state.pacman.oldPosition, pacmanNextPos) ? 1 : 0);
		return features;
	}
}
