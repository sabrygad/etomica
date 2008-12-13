package etomica.atom;

import etomica.api.IMolecule;


/**
 * Interface for class that associates an integer with an atom.
 */
public interface MoleculeToIndex {

    /**
     * Returns an integer that this instance associates with the given atom.
     * Should return -1 if an appropriate index can not be determined.
     */
    public int getIndex(IMolecule atom);
}