/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.data.meter;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISimulation;
import etomica.atom.AtomTypeOrientedSphere;
import etomica.atom.MoleculeOrientedDynamic;
import etomica.data.DataSourceScalar;
import etomica.species.ISpeciesOriented;
import etomica.units.Dimension;
import etomica.units.Temperature;

/**
 * Meter for measurement of the temperature based on kinetic-energy
 * equipartition.  The class uses a MeterKineticEnergy by default to calculate
 * the kinetic energy, but any DataSourceScalar can be used for this purpose by
 * calling setKineticEnergyMeter.
 * 
 * If the ISimulation is not given, the class will assume that all atoms have
 * only translational degrees of freedom.  If the ISimulation is given, this
 * class will examine the ISpecies and calculate the actual number of degrees
 * of freedom (more for oriented atoms or molecules).
 * 
 * @author Andrew Schultz
 */
public class MeterTemperature extends DataSourceScalar {

    public MeterTemperature(IBox box, int D) {
        this(null, box, D);
    }

    public MeterTemperature(ISimulation sim, IBox box, int D) {
		super("Temperature", Temperature.DIMENSION);
		dim = D;
		meterKE = new MeterKineticEnergy();
		((MeterKineticEnergy)meterKE).setBox(box);
		this.sim = sim;
		this.box = box;
	}
    
    public void setKineticEnergyMeter(DataSourceScalar meterKineticEnergy) {
        meterKE = meterKineticEnergy;
    }

	public double getDataAsScalar() {
	    int totalD = box.getLeafList().getAtomCount() * dim;
	    if (sim != null) {
	        totalD = 0;
//	        ISpecies[] species = sim.getSpeciesManager().getSpecies();
	        for (int i=0; i<sim.getSpeciesCount(); i++) {
	            int nMolecules = box.getNMolecules(sim.getSpecies(i));
	            if (nMolecules > 0) {
	                IMolecule molecule = box.getMoleculeList(sim.getSpecies(i)).getMolecule(0);
	                if (molecule instanceof MoleculeOrientedDynamic) {
	                    if (Double.isInfinite(((ISpeciesOriented)sim.getSpecies(i)).getMass())) {
	                        continue;
	                    }
                        totalD += 6*nMolecules;
	                }
	                else {
	                    IAtomList children = molecule.getChildList();
	                    if (children.getAtomCount() == 0 || 
	                        Double.isInfinite(children.getAtom(0).getType().getMass())) {
	                        continue;
	                    }
	                    if (children.getAtom(0).getType() instanceof AtomTypeOrientedSphere) {
	                        // oriented sphere at this point corresponds to cylindrical symmetry
	                        if (dim == 3) {
	                            totalD += 5*nMolecules*children.getAtomCount();
	                        }
	                        else { // dim = 2
	                            totalD += 3*nMolecules*children.getAtomCount();
	                        }
	                    }
	                    else {
	                        totalD += dim*nMolecules*children.getAtomCount();
	                    }
	                }
	            }
	        }
	    }
		return (2. / totalD) * meterKE.getDataAsScalar();
	}

	public Dimension getDimension() {
		return Temperature.DIMENSION;
	}

    private static final long serialVersionUID = 1L;
    protected IBox box;
	protected DataSourceScalar meterKE;
	protected final ISimulation sim;
	private final int dim;
}