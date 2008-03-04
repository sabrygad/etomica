package etomica.atom.iterator;

import etomica.api.IAtom;
import etomica.api.IAtomSet;

/**
 * Interface for an AtomIterator that can be conditioned with
 * target and basis atoms.
 */
public interface AtomsetIteratorBasisDependent extends AtomsetIteratorTargetable {

	/**
	 * Identifies the atoms that form the basis for iteration, such that
	 * the childList atoms of those given will form the iterates.
	 * @param atoms The basis atoms; a null basis will
	 * condition the iterator to give no iterates until a valid basis
	 * is specified via another call to this method.
	 */
    public void setBasis(IAtomSet atoms);
    
    /**
     * Indicates the size of the basis needed to set the iterator.
     * The length of the array given to setBasis should be this value.
     * @return the size of the basis for this iterator.
     */
    public int basisSize();
    
    /**
     * Returns true if the iterator with its current basis 
     * would return an iterate for the given target.
     */
    public boolean haveTarget(IAtom target);

}
