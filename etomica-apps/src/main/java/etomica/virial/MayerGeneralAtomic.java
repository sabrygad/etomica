package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;
import etomica.api.IPotentialAtomic;
import etomica.atom.AtomPair;

/**
 * @author kofke
 *
 * General Mayer function, which wraps the Mayer potential around an instance of
 * a Potential2 object.
 */
public class MayerGeneralAtomic implements MayerFunction {

    /**
     * Constructor Mayer function using given potential.
     */
    public MayerGeneralAtomic(IPotentialAtomic potential) {
        this.potential = potential;
        aPair = new AtomPair();
    }

    public double f(IMoleculeList pair, double r2, double beta) {
        aPair.atom0 = pair.getMolecule(0).getChildList().getAtom(0);
        aPair.atom1 = pair.getMolecule(1).getChildList().getAtom(0);
        double x = -beta*potential.energy(aPair);
        if (Math.abs(x) < 0.01) {
            return x + x*x/2.0 + x*x*x/6.0 + x*x*x*x/24.0 + x*x*x*x*x/120.0;
        }
        return Math.exp(x) - 1;
    }

    public IPotential getPotential() {
        return potential;
    }

    public void setBox(IBox newBox) {
        potential.setBox(newBox);
    }

    protected final IPotentialAtomic potential;
    protected final AtomPair aPair;
}
