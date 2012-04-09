package etomica.integrator.mcmove;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.space.ISpace;

/**
 * An MC Move for cluster simulations that performs torsion moves on acetic acid.
 * The move is performed on all molecules in the Box.  
 * The move needs the torsion potential in order to choose appropriate
 * torsion angles.
 * Only the hydrogen site is rotating by 180 degree. 
 * 
 * @author Hye Min Kim
 */
public class MCMoveTorsionAceticAcid extends MCMoveMolecule {
   
    /**
     * Constructor for MCMoveAtomMulti.
     * @param parentIntegrator
     * @param nAtoms number of atoms to move in a trial.  Number of atoms in
     * box should be at least one greater than this value (greater
     * because first atom is never moved)
     */
    public MCMoveTorsionAceticAcid(IPotentialMaster potentialMaster, ISpace space,
            IRandom random) {
        super(potentialMaster,random,space,1,Double.POSITIVE_INFINITY);//we don't need stepsize-> put 1
        ((MCMoveStepTracker)getTracker()).setTunable(false);
        vCO = space.makeVector();
        vOH = space.makeVector();
    }

    //note that total energy is calculated
    public boolean doTrial() {
        uOld = energyMeter.getDataAsScalar();
        molecule = moleculeSource.getMolecule();

        IAtomList childList = molecule.getChildList();
        int numChildren = childList.getAtomCount();//5

        IAtom c = childList.getAtom(1);
        IAtom sBO = childList.getAtom(3);
        IAtom h = childList.getAtom(4);
        vCO.Ev1Mv2(sBO.getPosition(), c.getPosition());//vector CO
        vOH.Ev1Mv2(h.getPosition(), sBO.getPosition());//vector OH
        double lengthdr13 = vCO.squared();
        
        IVectorMutable project = space.makeVector();
        IVectorMutable secondaryDirection = space.makeVector();
        project.E(vCO);
        project.TE(vCO.dot(vOH)/lengthdr13);
        secondaryDirection.Ev1Mv2(project,vOH);
        secondaryDirection.TE(2);
        h.getPosition().PE(secondaryDirection);
        for (int k=0; k<numChildren; k++) {
            // shift the whole molecule so that the center of mass (or whatever
            // the position definition uses) doesn't change
        	//newCenter is needed to be changed to oldCenter
            IAtom atomk = childList.getAtom(k);
            atomk.getPosition().PEa1Tv1(-0.2, secondaryDirection);
        }
        uNew = energyMeter.getDataAsScalar();
        return true;
    }

    public void rejectNotify() {
        IAtomList childList = molecule.getChildList();
        int numChildren = childList.getAtomCount();//5
        IAtom c = childList.getAtom(1);
        IAtom sBO = childList.getAtom(3);
        IAtom h = childList.getAtom(4);
        vCO.Ev1Mv2(sBO.getPosition(), c.getPosition());//vector CO
        vOH.Ev1Mv2(h.getPosition(), sBO.getPosition());//vector OH
        double lengthdr13 = vCO.squared();
        
        IVectorMutable project = space.makeVector();
        IVectorMutable secondaryDirection = space.makeVector();
        project.E(vCO);
        project.TE(vCO.dot(vOH)/lengthdr13);
        secondaryDirection.Ev1Mv2(project,vOH);
        secondaryDirection.TE(2);
        h.getPosition().PE(secondaryDirection);
        for (int k=0; k<numChildren; k++) {
            IAtom atomk = childList.getAtom(k);
            atomk.getPosition().PEa1Tv1(-0.2, secondaryDirection);
        }
    }
    
    public double getB() {
        return -(uNew - uOld);
    }

	
    private static final long serialVersionUID = 1L;
    protected final IVectorMutable vCO,vOH;
    
}