package etomica;

/**
 * Methods for a hard (impulsive), spherically-symmetric pair potential.
 * Subclasses must provide a concrete definition for the energy (method u(double)).
 */

public abstract class Potential2HardSpherical extends Potential2 implements PotentialHard, Potential2Spherical {
   
	public Potential2HardSpherical(Space space) {
		super(space);
	}
	
	/**
    * The pair energy u(r^2) with no truncation applied.
    * @param the square of the distance between the particles.
    */
    public abstract double u(double r2);

    /**
     * Energy of the pair as given by the u(double) method, with application
     * of any PotentialTruncation that may be defined for the potential.
     */
    public double energy(Atom[] pair) {
    	cPair.reset(pair[0].coord,pair[1].coord);
    	double r2 = cPair.r2();
    	return u(r2);
    }
    
}
