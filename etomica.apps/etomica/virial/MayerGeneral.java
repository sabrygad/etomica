package etomica.virial;

import etomica.AtomSet;
import etomica.Potential;

/**
 * @author kofke
 *
 * General Mayer function, which wraps the Mayer potential around an instance of
 * a Potential2 object.
 */
public class MayerGeneral implements MayerFunction {

	/**
	 * Constructor Mayer function using given potential.
	 */
	public MayerGeneral(Potential potential) {
		this.potential = potential;
	}

	public double f(AtomSet pair, double beta) {
		return Math.exp(-beta*potential.energy(pair)) - 1.0;
	}

	private final Potential potential;
}
