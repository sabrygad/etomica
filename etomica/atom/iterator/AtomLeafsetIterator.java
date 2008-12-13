package etomica.atom.iterator;

import etomica.api.IAtomList;

/**
 * Interface for classes that loop over a set of atoms. Permits
 * iteration via a next()!=null while loop (iterator returns
 * atoms to client) or via a call to allAtoms(AtomsetActive) (client gives
 * action to iterator).
 */

public interface AtomLeafsetIterator extends AtomsetIterator {
    
	/**
	 * Returns the next AtomSet iterate, or null if hasNext() is false.
	 */
    public IAtomList next();
}