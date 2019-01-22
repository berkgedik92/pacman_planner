package SATSolver;

import java.util.*;

/**
 * Created by USER on 10/6/2016.
 */
public class SATSolver {

    //unit clauses will be kept here
    private Set<Integer> result = new HashSet<>();

    /* For each possible atoms, I will create a set, each set of atom A
    will keep all all clauses in which A exists */
    private Map<Integer, Set<SATClause>> atomToClause = new HashMap<>();

    private Map<Integer, Integer> twos = new HashMap<>();

    private int atomNum;

    private List<SATClause> unitClauses = new ArrayList<>();
    List<SATClause> currentClauses = new ArrayList<>(); //all clauses

    public SATSolver(List<SATClause> clauses, int atomNum) {
        this.atomNum = atomNum;

        for (int i = atomNum * -1; i < atomNum + 1; i++) {
            atomToClause.put(i, new HashSet<>());
            twos.put(i, 0);
        }

        for (int i = 0; i < clauses.size(); i++) {
            SATClause current = clauses.get(i);
            if (current.getLength() == 1)
                this.unitClauses.add(current);
            else if (current.getLength() == 2) {
                int e1 = current.atoms[0];
                int e2 = current.atoms[1];
                this.twos.put(e1, this.twos.get(e1) + 1);
                this.twos.put(e2, this.twos.get(e2) + 1);
            }

            for (int j = 0; j < current.atoms.length; j++){
                int curAtom = current.atoms[j];
                atomToClause.get(curAtom).add(current);
            }

            currentClauses.add(current);
        }
    }

    public SATSolver(SATSolver source) {
        this.atomNum = source.atomNum;

        for (Integer i : source.result)
            this.result.add(i);

        for (int i = 0; i < source.currentClauses.size(); i++)
            this.currentClauses.add(new SATClause(source.currentClauses.get(i)));

        for (int i = atomNum * -1; i < atomNum + 1; i++) {
            atomToClause.put(i, new HashSet<>());
            twos.put(i, 0);
        }

        for (int i = 0; i < currentClauses.size(); i++) {
            SATClause current = currentClauses.get(i);
            if (current.getLength() == 1)
                this.unitClauses.add(current);
            else if (current.getLength() == 2) {
                int e1 = current.atoms[0];
                int e2 = current.atoms[1];
                this.twos.put(e1, this.twos.get(e1) + 1);
                this.twos.put(e2, this.twos.get(e2) + 1);
            }

            for (int j = 0; j < current.atoms.length; j++){
                int curAtom = current.atoms[j];
                atomToClause.get(curAtom).add(current);
            }
        }
    }

    public boolean isFinished() {
        for (int i = atomNum * -1; i < atomNum + 1; i++)
            if (atomToClause.get(i).size() > 0)
                return false;

        return true;
    }

    public int heuristic2() {
        int bestCandidate = 1;
        int bestLength = atomToClause.get(1).size() + atomToClause.get(-1).size()
                + 3 * (twos.get(1) + twos.get(-1));

        for (int i = 2; i < atomNum + 1; i++) {
            int u = atomToClause.get(i).size() + atomToClause.get(i * -1).size()
                    + 3 * (twos.get(i) + twos.get(i * -1));

            if (u > bestLength) {
                bestLength = u;
                bestCandidate = i;
            }
        }

        if (twos.get(bestCandidate) < twos.get(bestCandidate * -1))
            return bestCandidate;
        return bestCandidate  * -1;
    }

    public int heuristic1() {
        int bestCandidate = 1;
        int bestLength = atomToClause.get(-1 * atomNum).size() + atomToClause.get(-1).size();

        for (int i = (-1 * atomNum) + 1; i < atomNum + 1; i++) {
            int u = atomToClause.get(i).size() + atomToClause.get(i * -1).size();

            if (u > bestLength) {
                bestLength = u;
                bestCandidate = i;
            }
        }

        return bestCandidate;
    }

    SATClause clauseToWork;

    public boolean UnitPropagate() {
        while (unitClauses.size() > 0) {
            SATClause c = unitClauses.get(0);
            int curAtom = c.atoms[0];
            unitClauses.remove(0);
            result.add(curAtom);

            //Delete all clauses that includes curAtom from everywhere
            try {
                for (SATClause clause : atomToClause.get(curAtom)) {

                    int[] atoms = clause.atoms;
                    boolean isTwo = atoms.length == 2;

                    for (int i = 0; i < atoms.length; i++)
                        if (atoms[i] != curAtom) {
                            atomToClause.get(atoms[i]).remove(clause);
                            if (isTwo)
                                twos.put(atoms[i], twos.get(atoms[i]) - 1);
                        }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

            atomToClause.put(curAtom, new HashSet<>());
            twos.put(curAtom, 0);

            /*Delete all clauses that includes !curAtom from everywhere
            and add C \ !curAtom to necessary places*/
            int negation = curAtom * -1;

            try {
                for (SATClause clause : atomToClause.get(negation)) {
                    clauseToWork = clause;
                    clause.RemoveAtom(negation);

                    if (clause.atoms.length == 0)
                        return false;
                    if (clause.atoms.length == 1) {
                        unitClauses.add(clause);
                        twos.put(clause.atoms[0], twos.get(clause.atoms[0]) - 1);
                    }
                    if (clause.atoms.length == 2) {
                        twos.put(clause.atoms[0], twos.get(clause.atoms[0]) + 1);
                        twos.put(clause.atoms[1], twos.get(clause.atoms[1]) + 1);
                    }
                }
            }catch (Exception e) {
                e.printStackTrace();
            }

            atomToClause.put(negation , new HashSet());
            twos.put(negation, 0);
        }
        return true;
    }


    public Set<Integer> DPLL() {

        try {
            if (!UnitPropagate())
                return null;
            if (isFinished())
                return result;
        }catch (Exception e) {
            e.printStackTrace();
        }

        try {
            int candidate = heuristic2();
            SATSolver copy = new SATSolver(this);

            SATClause newSatClause1 = new SATClause(new int[]{candidate});
            copy.unitClauses.add(newSatClause1);
            Set<Integer> trial = copy.DPLL();

            if (trial != null)
                return trial;

            SATClause newSatClause2 = new SATClause(new int[]{candidate * -1});
            unitClauses.add(newSatClause2);
            return DPLL();
        }catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
