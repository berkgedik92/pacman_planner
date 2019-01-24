/*
* @Author: Dmytro Kalpakchi
* @Date:   2016-10-08 10:57:15
* @Last Modified by:   Dmytro Kalpakchi
* @Last Modified time: 2016-10-14 15:36:27
*/

package OnlinePlanner;

import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.util.HashMap;

import IPlanner.IPlanner;
import Game.Action;
import Game.BoardState;
import Game.Monster;
import Main.Config;

public class ApproximateQPlanner implements IPlanner {

	private HashMap<Short, Integer> actDistribution;

	private boolean trained = false;

	private boolean useStats = false;
	private double learningRate = 0.1;
	private double discount = 0.8;
	private double trainingEpsilon = 0.05;
	private double testEpsilon = 0;
	private double livingReward = -5.0;
	private double[] weights;

	public ApproximateQPlanner() {
		weights = new double[FeatureExtractor.FEATURES_NUM];
		if (useStats) {
			actDistribution = new HashMap<>();
			for (Action a : Action.values())
				actDistribution.put(a.getCode(), 0);
		}
	}

	private double computeQValue(double[] features) {
		double result = 0.0;
		for (int i = 0; i < weights.length; i++) {
			result += weights[i] * features[i];
		}
		return result;
	}

	private Action getRandomAction(BoardState b) {
		List<Action> actions = b.getValidActions(b.pacman.currentPosition);
		int rndInd = new Random().nextInt(actions.size());
		return actions.get(rndInd);
	}

	private Action getPolicyAction(BoardState b) {
		List<Action> validActions = b.getValidActions(b.pacman.currentPosition);
		Action winner = validActions.get(0);
		double maxQValue = Integer.MIN_VALUE, qValue;
		for (Action a : validActions) {
			double[] features = FeatureExtractor.getFeatures(b, a);
			// System.err.println(Arrays.toString(features));
			qValue = computeQValue(features);
			// System.err.println(a + " " + qValue);
			if (maxQValue < qValue || (useStats && maxQValue == qValue && 
				actDistribution.get(a.getCode()) < actDistribution.get(winner.getCode()))) {
				maxQValue = qValue;
				winner = a;
			}
		}
		// System.err.println("\n");
		if (useStats)
			actDistribution.put(winner.getCode(), actDistribution.get(winner.getCode()) + 1);
		return winner;
	}

	public Action getNextAction(BoardState b) {
		return new Random().nextDouble() < testEpsilon ? getRandomAction(b) : getPolicyAction(b);
	}

	@Override
	public IPlanner reset() {
		return this;
	}

	@Override
	public boolean isTrained() {
		return trained;
	}

	private void updateWeights(BoardState old, Action act, BoardState next) {
		// System.err.println("WEIGHTS " + Arrays.toString(weights));
		double[] features = FeatureExtractor.getFeatures(old, act);
		// System.err.println("FEATURES " + Arrays.toString(features));
		double reward = next.score - old.score + livingReward;

		// System.err.println("REWARD " + reward);
		double qValue = computeQValue(features);
		// might be substituted with just state policy lookup if we kept this policy
		double expectedQValue = next.isGameOver() ? 0 : next.getValidActions(
			next.pacman.currentPosition).stream().mapToDouble(x -> computeQValue(FeatureExtractor.getFeatures(next, x))).max().getAsDouble();
		double difference = reward + discount * expectedQValue - qValue;

		// System.err.println("GOT: " + reward + " EXPECT IN FUTURE: " + expectedQValue + " EXPECTED TO GET NOW: " + qValue);
		for (int i = 0; i < weights.length; i++) {
			weights[i] += learningRate * difference * features[i];
		}
		// System.err.println("WEIGHTS " + Arrays.toString(weights) + "\n\n");
	}

	public void train(BoardState state) throws Exception {
		Config config = Config.getInstance();
		int trainingEpisodes = (int)config.getConfig("training_episodes");
		System.err.println("=========== " + trainingEpisodes + " TRAINING EPISODES STARTED ==============");
		int episode = 0, victories = 0;
		int[] movesStat = new int[trainingEpisodes];
		while(episode < trainingEpisodes) {
			BoardState playBoard = new BoardState(state, null);
			Random rnd = new Random();
			int iterationAmount = 0;
			while (!playBoard.isGameOver()) {
				Action decision;

				// here need to try simulated annealing idea
				if (rnd.nextDouble() < trainingEpsilon)
					decision = getRandomAction(playBoard);
				else
					decision = getPolicyAction(playBoard);

				BoardState oldBoard = new BoardState(playBoard, null);
				playBoard.pacman.makeDecision(decision);
				for (Monster m : playBoard.monsters) {
					m.makeDecision(playBoard.boardData);
				}
				playBoard.checkMaze();
				updateWeights(oldBoard, decision, playBoard);
				iterationAmount++;
			}
			movesStat[episode] = iterationAmount;

			if (!playBoard.isPacmanDead)
				victories++;

			episode++;

			if (episode % 10 == 0)
				System.err.println("TRAINED " + episode + " EPISODES with " + iterationAmount + " ITERATIONS\n");
		}
		System.err.println("=========== TRAINING IS FINISHED ==============");
		System.err.println(victories * 100.0 / episode + "% of victories during training");
		System.err.println("Average number of moves per episode: " + Arrays.stream(movesStat).average().getAsDouble());
		System.err.println("\n");

		test(state);
		trained = true;
	}

	private void test(BoardState state) throws Exception {
		Config config = Config.getInstance();
		int testEpisodes = (int)config.getConfig("test_episodes");
        System.err.println("=========== " + testEpisodes + " TEST GAMES STARTED ==============");
        int episode = 0, victories = 0;
        int[] movesStat = new int[testEpisodes];
        while (episode < testEpisodes) {
            BoardState playBoard = new BoardState(state, null);
            Random rnd = new Random();
            int iter = 0;
            while (!playBoard.isGameOver()) {
                Action decision;

                // here need to try simulated annealing idea
                if (rnd.nextDouble() < trainingEpsilon)
                    decision = getRandomAction(playBoard);
                else
                    decision = getPolicyAction(playBoard);

                playBoard.pacman.makeDecision(decision);
                for (Monster m : playBoard.monsters)
                    m.makeDecision(playBoard.boardData);

                playBoard.checkMaze();
                iter++;
            }
            movesStat[episode] = iter;

            if (!playBoard.isPacmanDead)
                victories++;

            episode++;

            if (episode % 10 == 0)
                System.err.println("PLAYED " + episode + " TEST GAMES\n");
        }
        System.err.println("=========== TESTING IS FINISHED ==============");
        System.err.println(victories * 100.0 / episode + "% of victories during testing");
        System.err.println("Average number of moves per episode: " + Arrays.stream(movesStat).average().getAsDouble());
        System.err.println("\n");
    }
}