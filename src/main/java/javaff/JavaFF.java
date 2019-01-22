/************************************************************************
 * Strathclyde Planning Group,
 * Department of Computer and Information Sciences,
 * University of Strathclyde, Glasgow, UK
 * http://planning.cis.strath.ac.uk/
 * 
 * Copyright 2007, Keith Halsey
 * Copyright 2008, Andrew Coles and Amanda Smith
 *
 * (Questions/bug reports now to be sent to Andrew Coles)
 *
 * This file is part of JavaFF.
 * 
 * JavaFF is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JavaFF is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JavaFF.  If not, see <http://www.gnu.org/licenses/>.
 * 
 ************************************************************************/

package javaff;

import java.io.PrintStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import Game.*;
import IPlanner.IPlanner;
import javaff.data.GroundProblem;
import javaff.data.Plan;
import javaff.data.TotalOrderPlan;
import javaff.data.Type;
import javaff.data.UngroundProblem;
import javaff.data.metric.BinaryComparator;
import javaff.data.metric.FunctionSymbol;
import javaff.data.metric.NamedFunction;
import javaff.data.metric.NumberFunction;
import javaff.data.strips.AND;
import javaff.data.strips.NOT;
import javaff.data.strips.OperatorName;
import javaff.data.strips.PDDLObject;
import javaff.data.strips.Predicate;
import javaff.data.strips.PredicateSymbol;
import javaff.data.strips.Proposition;
import javaff.data.strips.STRIPSInstantAction;
import javaff.data.strips.SimpleType;
import javaff.data.strips.UngroundInstantAction;
import javaff.data.strips.Variable;
import javaff.planning.HelpfulFilter;
import javaff.planning.MetricState;
import javaff.planning.NullFilter;
import javaff.planning.State;
import javaff.search.BestFirstSearch;
import javaff.search.EnforcedHillClimbingSearch;

public class JavaFF implements IPlanner
{
    public static BigDecimal EPSILON = new BigDecimal(0.01);
	public static BigDecimal MAX_DURATION = new BigDecimal("100000"); //maximum duration in a duration constraint
	public static PrintStream planOutput = System.out;
	public static PrintStream parsingOutput = System.out;
	public static PrintStream infoOutput = System.out;
	public static PrintStream errorOutput = System.err;

    public JavaFF() {

    }

    public IPlanner reset() {
        return new JavaFF();
    }

    boolean isWorked = false;

    //A* algorithm should come here
    public List<Action> makePlan(BoardState state) {

        /*This part is to make sure that the code below will work for only one time
        It is for debug purposes, normally this kind of control will not be there as it
        does not make any sense*/
        if (isWorked)
        {
            List<Action> result = new ArrayList<>();
            result.add(Action.STOP);
            return result;
        }

        isWorked = true;


        EPSILON = EPSILON.setScale(2,BigDecimal.ROUND_HALF_EVEN);
        int timeLimit = state.colAmount * state.rowAmount * 2;
        Plan plan = plan(state, timeLimit);

		List<Position> solutionPath = new ArrayList<Position>();
		Iterator pit = ((TotalOrderPlan)plan).iterator();
		boolean isFirst = true;
		while (pit.hasNext())
		{
			STRIPSInstantAction move = (STRIPSInstantAction) pit.next();
			
			if(isFirst){
				String posCurr = move.params.get(0).toString();
				solutionPath.add(new Position(posCurr));
				isFirst = false;
			}

			String posNext = (String) move.params.get(1).toString();
			solutionPath.add(new Position(posNext));
		}

		List<Action> result = new ArrayList<>();

		for (int t = 1; t < solutionPath.size(); t++) {
			Position curPos = solutionPath.get(t);
			Position oldPos = solutionPath.get(t-1);

			if (curPos.x > oldPos.x){
				result.add(Action.RIGHT);
			}else if (curPos.x < oldPos.x){
				result.add(Action.LEFT);
			}else if (curPos.y > oldPos.y){
				result.add(Action.DOWN);
			}else if (curPos.y < oldPos.y){
				result.add(Action.UP);
			}else{
				result.add(Action.STOP);
			}
		}

		return result;
    }

    public Plan plan(BoardState state, int timeLimit)
    {
		// ********************************
		// Parse and Ground the Problem
		// ********************************
		long startTime = System.currentTimeMillis();
		
		UngroundProblem problem = createDomain();
		createProblem(problem, state, timeLimit);
		
		if (problem == null)
		{
			System.out.println("Parsing error - see console for details");
			return null;
		}

		GroundProblem ground = problem.ground();

		long afterGrounding = System.currentTimeMillis();

		// ********************************
		// Search for a plan
		// ********************************

		// Get the initial state
		MetricState initialState = ground.getMetricInitialState();
		
        State goalState = performFFSearch(initialState);
                
		long afterPlanning = System.currentTimeMillis();

        TotalOrderPlan top = null;
		if (goalState != null) top = (TotalOrderPlan) goalState.getSolution();
		if (top != null) top.print(planOutput);

		double groundingTime = (afterGrounding - startTime)/1000.00;
		double planningTime = (afterPlanning - afterGrounding)/1000.00;

		infoOutput.println("Instantiation Time =\t\t"+groundingTime+"sec");
		infoOutput.println("Planning Time =\t"+planningTime+"sec");

		return top;
	}
    
    public State performFFSearch(MetricState initialState) {

        // Implementation of standard FF-style search
        infoOutput.println("Performing search as in FF - first considering EHC with only helpful actions");

        // Now, initialise an EHC searcher
        EnforcedHillClimbingSearch EHCS = new EnforcedHillClimbingSearch(initialState);

        EHCS.setFilter(HelpfulFilter.getInstance()); // and use the helpful actions neighbourhood

        // Try and find a plan using EHC
        State goalState = EHCS.search();

        if (goalState == null) // if we can't find one
        {
            infoOutput.println("EHC failed, using best-first search, with all actions");

            // create a Best-First Searcher
            BestFirstSearch BFS = new BestFirstSearch(initialState);
            //BreadthFirstSearch BFS = new BreadthFirstSearch(initialState);

            // ... change to using the 'all actions' neighbourhood (a null filter, as it removes nothing)
            BFS.setFilter(NullFilter.getInstance());

            // and use that
            goalState = BFS.search();
        }

        return goalState; // return the plan
    }
    
    private UngroundProblem createDomain(){
    	
    	UngroundProblem problem = new UngroundProblem();
    	problem.DomainName = "pacman";
    	problem.requirements.add(":typing");
    	problem.requirements.add(":fluents");
    	
    	SimpleType posType = new SimpleType("pos", SimpleType.rootType);
    	SimpleType timeType = new SimpleType("time", SimpleType.rootType);
    	problem.types.add(posType);
    	problem.types.add(timeType);
    	problem.typeMap.put("pos", posType);
    	problem.typeMap.put("time", timeType);
    	
    	Variable posVariable = new Variable("?pos", posType);
    	Variable pos2Variable = new Variable("?pos2", posType);
    	Variable timeVariable = new Variable("?time", timeType);
    	Variable time2Variable = new Variable("?time2", timeType);
    	
    	PredicateSymbol atSymbol = new PredicateSymbol("At");
    	atSymbol.addVar(posVariable);
    	
    	PredicateSymbol visitedSymbol = new PredicateSymbol("Visited");
    	visitedSymbol.addVar(posVariable);
    	
    	PredicateSymbol adjacentSymbol = new PredicateSymbol("Adjacent");
    	adjacentSymbol.addVar(posVariable);
    	adjacentSymbol.addVar(pos2Variable);
    	
    	PredicateSymbol timeSymbol = new PredicateSymbol("Time");
    	timeSymbol.addVar(timeVariable);
    	
    	PredicateSymbol timeSequenceSymbol = new PredicateSymbol("TimeSequence");
    	timeSequenceSymbol.addVar(timeVariable);
    	timeSequenceSymbol.addVar(time2Variable);
    	
    	problem.predSymbols.add(atSymbol);
    	problem.predSymbols.add(visitedSymbol);
    	problem.predSymbols.add(adjacentSymbol);
    	problem.predSymbols.add(timeSymbol);
    	problem.predSymbols.add(timeSequenceSymbol);
    	problem.predSymbolMap.put("At", atSymbol);
    	problem.predSymbolMap.put("Visited", visitedSymbol);
    	problem.predSymbolMap.put("Adjacent",adjacentSymbol);
    	problem.predSymbolMap.put("Time",timeSymbol);
    	problem.predSymbolMap.put("TimeSequence",timeSequenceSymbol);
    	
    	FunctionSymbol collisionSymbol = new FunctionSymbol("collision");
    	collisionSymbol.addVar(posVariable);
    	collisionSymbol.addVar(timeVariable);
    	
    	FunctionSymbol collision2Symbol = new FunctionSymbol("collision2");
    	collision2Symbol.addVar(posVariable);
    	collision2Symbol.addVar(pos2Variable);
    	collision2Symbol.addVar(timeVariable);
    	
    	problem.funcSymbols.add(collisionSymbol);
    	problem.funcSymbolMap.put("collision", collisionSymbol);
    	problem.funcSymbols.add(collision2Symbol);
    	problem.funcSymbolMap.put("collision2", collision2Symbol);
    	
    	UngroundInstantAction actionMove = new UngroundInstantAction();
    	actionMove.name = new OperatorName("move");
    	
    	Variable posCurr = new Variable("?posCurr", posType);
    	Variable posNext = new Variable("?posNext", posType);
    	Variable timeCurr = new Variable("?timeCurr", timeType);
    	Variable timeNext = new Variable("?timeNext", timeType);
    	
    	actionMove.params.add(posCurr);
    	actionMove.params.add(posNext);
    	actionMove.params.add(timeCurr);
    	actionMove.params.add(timeNext);
    	
    	AND moveCondition = new AND();
    	
    	Predicate atConditionPredicate = new Predicate(atSymbol);
    	atConditionPredicate.addParameter(posCurr);
    	
    	Predicate adjacentConditionPredicate = new Predicate(adjacentSymbol);
    	adjacentConditionPredicate.addParameter(posCurr);
    	adjacentConditionPredicate.addParameter(posNext);
    	
    	Predicate timeConditionPredicate = new Predicate(timeSymbol);
    	timeConditionPredicate.addParameter(timeCurr);
    	
    	Predicate timeSequenceConditionPredicate = new Predicate(timeSequenceSymbol);
    	timeSequenceConditionPredicate.addParameter(timeCurr);
    	timeSequenceConditionPredicate.addParameter(timeNext);
    	
    	moveCondition.add(atConditionPredicate);
    	moveCondition.add(adjacentConditionPredicate);
    	moveCondition.add(timeConditionPredicate);
    	moveCondition.add(timeSequenceConditionPredicate);
    	
    	NamedFunction collisionFunction = new NamedFunction(collisionSymbol);
    	collisionFunction.addParameter(posNext);
    	collisionFunction.addParameter(timeNext);
    	NumberFunction function1 = new NumberFunction(1);
    	BinaryComparator collisionComparator = new BinaryComparator("<", collisionFunction, function1);
    	
    	NamedFunction collision2Function = new NamedFunction(collision2Symbol);
    	collision2Function.addParameter(posCurr);
    	collision2Function.addParameter(posNext);
    	collision2Function.addParameter(timeCurr);
    	BinaryComparator collision2Comparator = new BinaryComparator("<", collision2Function, function1);
    	
    	moveCondition.add(collisionComparator);
    	moveCondition.add(collision2Comparator);
    	
    	actionMove.condition = moveCondition;
    	
    	AND moveEffect = new AND();
    	
    	Predicate atEffectNotPredicate = new Predicate(atSymbol);
    	atEffectNotPredicate.addParameter(posCurr);
    	NOT notAt = new NOT(atEffectNotPredicate);
    	
    	Predicate atEffectPredicate = new Predicate(atSymbol);
    	atEffectPredicate.addParameter(posNext);
    	
    	Predicate visitedEffectPredicate = new Predicate(visitedSymbol);
    	visitedEffectPredicate.addParameter(posCurr);
    	
    	Predicate visitedEffectPredicate2 = new Predicate(visitedSymbol);
    	visitedEffectPredicate2.addParameter(posNext);
    	
    	Predicate timeEffectNotPredicate = new Predicate(timeSymbol);
    	timeEffectNotPredicate.addParameter(timeCurr);
    	NOT notTime = new NOT(timeEffectNotPredicate);
    	
    	Predicate timeEffectPredicate = new Predicate(timeSymbol);
    	timeEffectPredicate.addParameter(timeNext);
    	
    	moveEffect.add(notAt);
    	moveEffect.add(atEffectPredicate);
    	moveEffect.add(visitedEffectPredicate);
    	moveEffect.add(visitedEffectPredicate2);
    	moveEffect.add(notTime);
    	moveEffect.add(timeEffectPredicate);
    	
    	actionMove.effect = moveEffect;
    	
    	problem.actions.add(actionMove);
    	
    	return problem;
    }
    
    private void createProblem(UngroundProblem problem, BoardState state, int timeLimit) {
    	problem.ProblemName = "pacman-problem";
    	problem.ProblemDomainName = "pacman";
    	
    	Type posType = problem.typeMap.get("pos");
    	Type timeType = problem.typeMap.get("time");
    	
    	for (int c = 0; c < state.colAmount; c++) {
            for (int r = 0; r < state.rowAmount; r++) {
            	String name = "p_" + c + "_" + r;
                PDDLObject posObject = new PDDLObject(name, posType);
            	problem.objects.add(posObject);
            	problem.objectMap.put(name, posObject);
            }
    	}
    	
    	for (int t = 0; t < timeLimit; t++){
    		String name = "t_" + t;
            PDDLObject timeObject = new PDDLObject(name, timeType);
            problem.objects.add(timeObject);
            problem.objectMap.put(name, timeObject);
    	}
    	
    	// Create collisions
    	FunctionSymbol collisionSymbol = problem.funcSymbolMap.get("collision");
    	
    	Position[][] monsterPos = new Position[state.monsters.size()][timeLimit];

        for (int m = 0; m < state.monsters.size(); m++) {
            Monster currentMonster = state.monsters.get(m);
            Position cPos = new Position(currentMonster.getCurrentPos().y, currentMonster.getCurrentPos().x);
            Action[] actions = currentMonster.giveActionsToDo(timeLimit);

            for (int t = 0; t < timeLimit; t++) {
                monsterPos[m][t] = new Position(cPos.y, cPos.x);
                cPos = Position.giveConsequence(cPos, actions[t]);
            }
        }

        Set<String> collision = new HashSet<String>();
        Set<String> notCollision = new HashSet<String>();

		Set<String> collision2 = new HashSet<String>();
		Set<String> notCollision2 = new HashSet<String>();

        //Initially make all (pos, time) pairs equal to false
        for (int t = 0; t < timeLimit; t++) {
            for (int c = 0; c < state.colAmount; c++) {
                for (int r = 0; r < state.rowAmount; r++) {
                    notCollision.add("p_" + c + "_" + r + ":t_" + t);
                }
            }
        }

		for (int t = 0; t < timeLimit; t++){
			for (int c1 = 0; c1 < state.colAmount; c1++){
				for (int r1 = 0; r1 < state.rowAmount; r1++){
					for (int c2 = 0; c2 < state.colAmount; c2++){
						for (int r2 = 0; r2 < state.rowAmount; r2++){
							notCollision2.add("p_" + c1 + "_" + r1 + ":p_" + c2 + "_" + r2 + ":t_" + t);
						}
					}
				}
			}
		}

        //Now check all monster positions and convert necessary values to true
        for (int m = 0; m < state.monsters.size(); m++){
            for (int t = 0; t < timeLimit; t++) {
                Position curPos = monsterPos[m][t];
                collision.add("p_" + curPos.x + "_" + curPos.y + ":t_" + t);
                notCollision.remove("p_" + curPos.x + "_" + curPos.y + ":t_" + t);
            }
        }
        
        for(String position : collision){
        	String[] parsedPosition = position.split(":");
        	NamedFunction collisionFunction = new NamedFunction(collisionSymbol);
        	PDDLObject posObject = (PDDLObject) problem.objectMap.get(parsedPosition[0]);
        	collisionFunction.addParameter(posObject);
        	
        	PDDLObject timeObject = (PDDLObject) problem.objectMap.get(parsedPosition[1]);
        	collisionFunction.addParameter(timeObject);
        	problem.funcValues.put(collisionFunction, new BigDecimal(1));
        }
    	
        for(String position : notCollision){
        	String[] parsedPosition = position.split(":");
        	NamedFunction collisionFunction = new NamedFunction(collisionSymbol);
        	PDDLObject posObject = (PDDLObject) problem.objectMap.get(parsedPosition[0]);
        	collisionFunction.addParameter(posObject);
        	
        	PDDLObject timeObject = (PDDLObject) problem.objectMap.get(parsedPosition[1]);
        	collisionFunction.addParameter(timeObject);
        	problem.funcValues.put(collisionFunction, new BigDecimal(0));
        }

		//Now check all monster positions and convert necessary values to true
		for (int m = 0; m < state.monsters.size(); m++){
			for (int t = 0; t < timeLimit; t++) {
				Position curPos = monsterPos[m][t];
				collision.add("p_" + curPos.x + "_" + curPos.y + ":t_" + t);
				notCollision.remove("p_" + curPos.x + "_" + curPos.y + ":t_" + t);
			}
		}

		//There must be no collisions (I'm not sure if this part is correct, I need to check it later)
        for (int m = 0; m < state.monsters.size(); m++) {
            for (int t = 0; t < timeLimit - 1; t++) {
				Position curPos = monsterPos[m][t];
				Position nextPos = monsterPos[m][t+1];

				collision2.add("p_" + nextPos.x + "_" + nextPos.y + ":p_" + curPos.x + "_" + curPos.y + ":t_" + t);
				notCollision2.remove("p_" + nextPos.x + "_" + nextPos.y + ":p_" + curPos.x + "_" + curPos.y + ":t_" + t);
            }
		}

        FunctionSymbol collision2Symbol = problem.funcSymbolMap.get("collision2");
		for(String position : collision2){
			String[] parsedPosition = position.split(":");
			NamedFunction collisionFunction = new NamedFunction(collision2Symbol);
			PDDLObject posCurrObject = (PDDLObject) problem.objectMap.get(parsedPosition[0]);
			collisionFunction.addParameter(posCurrObject);
			PDDLObject posNextObject = (PDDLObject) problem.objectMap.get(parsedPosition[1]);
			collisionFunction.addParameter(posNextObject);

			PDDLObject timeObject = (PDDLObject) problem.objectMap.get(parsedPosition[2]);
			collisionFunction.addParameter(timeObject);
			problem.funcValues.put(collisionFunction, new BigDecimal(1));
		}

		for(String position : notCollision2){
			String[] parsedPosition = position.split(":");
			NamedFunction collisionFunction = new NamedFunction(collision2Symbol);
			PDDLObject posCurrObject = (PDDLObject) problem.objectMap.get(parsedPosition[0]);
			collisionFunction.addParameter(posCurrObject);
			PDDLObject posNextObject = (PDDLObject) problem.objectMap.get(parsedPosition[1]);
			collisionFunction.addParameter(posNextObject);

			PDDLObject timeObject = (PDDLObject) problem.objectMap.get(parsedPosition[2]);
			collisionFunction.addParameter(timeObject);
			problem.funcValues.put(collisionFunction, new BigDecimal(0));
		}

		PredicateSymbol timePredicateSymbol = problem.predSymbolMap.get("Time");
        Proposition timeProposition = new Proposition(timePredicateSymbol);
        PDDLObject initialTime = (PDDLObject) problem.objectMap.get("t_0");
        timeProposition.addParameter(initialTime);
        problem.initial.add(timeProposition);
        
        PredicateSymbol atPredicateSymbol = problem.predSymbolMap.get("At");
        Proposition atProposition = new Proposition(atPredicateSymbol);
        Position initialPacmanPosition = state.pacman.getCurrentPos();
        PDDLObject initialPosition = (PDDLObject) problem.objectMap.get("p_" + initialPacmanPosition.x + "_" + initialPacmanPosition.y);
        atProposition.addParameter(initialPosition);
        problem.initial.add(atProposition);
        
        PredicateSymbol timeSequencePredicateSymbol = problem.predSymbolMap.get("TimeSequence");
        
        /*3) Define TimeSequence predicates*/
        for (int t = 0; t < timeLimit - 1; t++){
        	Proposition timeSequenceProposition = new Proposition(timeSequencePredicateSymbol);
        	PDDLObject timeCurrent = (PDDLObject) problem.objectMap.get("t_" + t);
        	PDDLObject timeNext = (PDDLObject) problem.objectMap.get("t_" + (t+1));
        	timeSequenceProposition.addParameter(timeCurrent);
        	timeSequenceProposition.addParameter(timeNext);
        	problem.initial.add(timeSequenceProposition);
        }

        /*4) Define Adjacent predicates*/
        Set<String> adjacent = new HashSet<String>();
        short[] wallData = state.boardData;
        PredicateSymbol adjacentPredicateSymbol = problem.predSymbolMap.get("Adjacent");

        //Check top-neighbors
        for (int r = 1; r < state.rowAmount; r++){
            for (int c = 0; c < state.colAmount; c++){
                if ((wallData[r * state.colAmount + c] & 2) == 0){
                    adjacent.add("p_" + c + "_" + r + ":p_" + c + "_" + (r-1));
                }
            }
        }

        //Check bottom-neighbors
        for (int r = 0; r < state.rowAmount - 1; r++){
            for (int c = 0; c < state.colAmount; c++){
                if ((wallData[r * state.colAmount + c] & 8) == 0){
                    adjacent.add("p_" + c + "_" + r + ":p_" + c + "_" + (r+1));
                }
            }
        }

        //Check left-neighbors
        for (int r = 0; r < state.rowAmount; r++){
            for (int c = 1; c < state.colAmount; c++){
                if ((wallData[r * state.colAmount + c] & 1) == 0){
                    adjacent.add("p_" + c + "_" + r + ":p_" + (c-1) + "_" + r);
                }
            }
        }

        //Check right-neighbors
        for (int r = 0; r < state.rowAmount; r++){
            for (int c = 0; c < state.colAmount - 1; c++){
                if ((wallData[r * state.colAmount + c] & 4) == 0){
                    adjacent.add("p_" + c + "_" + r + ":p_" + (c+1) + "_" + r);
                }
            }
        }
        
        for(String str : adjacent){
        	String[] parsedPositions = str.split(":");
        	Proposition adjacentProposition = new Proposition(adjacentPredicateSymbol);
        	PDDLObject firstCell = (PDDLObject)problem.objectMap.get(parsedPositions[0]);
        	PDDLObject secondCell = (PDDLObject)problem.objectMap.get(parsedPositions[1]);
        	adjacentProposition.addParameter(firstCell);
        	adjacentProposition.addParameter(secondCell);
        	problem.initial.add(adjacentProposition);
        }

        /*4) Define goal predicates*/
        AND goalAnd = new AND();
        PredicateSymbol visitedPredicateSymbol = problem.predSymbolMap.get("Visited");

        for (int r = 0; r < state.rowAmount; r++){
            for (int c = 0; c < state.colAmount; c++){
                if ((wallData[r * state.colAmount + c] & 16) > 0){
                	Proposition visitedProposition = new Proposition(visitedPredicateSymbol);
                    PDDLObject visitedCell = (PDDLObject) problem.objectMap.get("p_" + c + "_" + r);
                    visitedProposition.addParameter(visitedCell);
                	goalAnd.add(visitedProposition);
                }
            }
        }
        problem.goal = goalAnd;
    	
    }
}
