package SATSolver;

class SATClause {

    int[] atoms;

    SATClause(int[] atoms) {
        this.atoms = atoms;
    }

    SATClause(SATClause source) {
        atoms = new int[source.atoms.length];
        System.arraycopy(source.atoms, 0, atoms, 0, source.atoms.length);
    }

    int getLength() {
        return this.atoms.length;
    }

    void RemoveAtom(int atom) {

        if (atoms.length == 1)
        {
            atoms = new int[0];
            return;
        }

        int[] res = new int[atoms.length - 1];
        int pos = 0;

        for (int current: this.atoms)
            if (current != atom)
                res[pos++] = current;

        atoms = res;
    }
}