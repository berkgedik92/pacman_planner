package SATSolver;

public class SATClause {

    public int[] atoms;

    public SATClause(int[] atoms) {
        this.atoms = atoms;
    }

    public int getLength() {
        return this.atoms.length;
    }

    public void RemoveAtom(int atom) {

        if (atoms.length == 1)
        {
            atoms = new int[0];
            return;
        }

        int[] res = new int[atoms.length - 1];
        int pos = 0;

        for (int i = 0; i < atoms.length; i++)
            if (atoms[i] != atom)
                res[pos++] = atoms[i];

        atoms = res;
    }

    public SATClause(SATClause source) {
        atoms = new int[source.atoms.length];

        for (int i = 0; i < source.atoms.length; i++)
            atoms[i] = source.atoms[i];
    }
}