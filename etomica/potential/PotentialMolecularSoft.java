package etomica.potential;

import etomica.api.IMoleculeList;
import etomica.api.IPotentialMolecular;
import etomica.api.IVector;
import etomica.space.Tensor;


/**
 * Methods for properties obtained for a soft, differentiable pair potential.
 *
 * @author David Kofke
 */
public interface PotentialMolecularSoft extends IPotentialMolecular {

    public double virial(IMoleculeList atoms);

    /**
	 * Returns the gradient of the potential as it applies to each atom in the 
     * given AtomSet, indicating how the energy would change as the position of 
     * the first atom is varied.  The method is allowed to return an array of
     * Vectors with fewer elements than the number of atoms in the AtomSet.
	 * @param atoms
	 * @return
	 */
	public IVector[] gradient(IMoleculeList atoms);
    
    /**
     * Returns the same gradient as gradient(AtomSet) and also adds in the
     * contribution of the AtomSet to the pressureTensor.  Their
     * contribution is added to the given Tensor.  This combined method exists
     * for computational efficiency.  Calculating the pressureTensor is
     * generally trivial once the gradient is known but often requires
     * intermediate information.
     */
    public IVector[] gradient(IMoleculeList atoms, Tensor pressureTensor);

}