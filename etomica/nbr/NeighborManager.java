/*
 * History
 * Created on Sep 22, 2004 by kofke
 */
package etomica.nbr;

import etomica.Atom;
import etomica.AtomIteratorTree;
import etomica.Debug;
import etomica.Integrator;
import etomica.IteratorDirective;
import etomica.Phase;
import etomica.Space;
import etomica.Integrator.IntervalEvent;
import etomica.Integrator.IntervalListener;
import etomica.action.AtomsetActionAdapter;

/**
 * Initiates the process of updating the neighbor lists.  Acts as a
 * listener of the integrator, and performs the update at regular intervals
 * upon receiving interval events.
 */
public class NeighborManager implements IntervalListener {

	/**
	 * 
	 */
	public NeighborManager(PotentialMasterNbr potentialMaster) {
		super();
		this.potentialMaster = potentialMaster;
		setUpdateInterval(1);
		iieCount = updateInterval;
		iterator = new AtomIteratorTree();
		iterator.setDoAllNodes(true);
		neighborCheck = new NeighborCheck();
		neighborReset = new NeighborReset();
        setPriority(200);
	}

	/* (non-Javadoc)
	 * @see etomica.Integrator.IntervalListener#intervalAction(etomica.Integrator.IntervalEvent)
	 */
	public void intervalAction(IntervalEvent evt) {
		if(evt.type() == IntervalEvent.START) {
			reset(((Integrator)evt.getSource()).getPhase());
		} else if(evt.type() == IntervalEvent.INTERVAL) {
			if (--iieCount == 0) {
				updateNbrsIfNeeded((Integrator)evt.getSource());
				iieCount = updateInterval;
			}
		}
	}

	/**
	 * @param 
	 */
	public void reset(Phase[] phase) {
		for (int i=0; i<phase.length; i++) {
			for (int j=0; j<criteria.length; j++) {
				criteria[j].setPhase(phase[i]);
			}
			boundary = phase[i].boundary();
			iterator.setRoot(phase[i].speciesMaster());
			iterator.allAtoms(neighborReset);
			potentialMaster.calculate(phase[i],id,potentialCalculationNbrSetup);
		}
	}

	public void updateNbrsIfNeeded(Integrator integrator) {
        boolean resetIntegrator = false;
        Phase[] phase = integrator.getPhase();
		for (int i=0; i<phase.length; i++) {
			neighborCheck.reset();
			for (int j=0; j<criteria.length; j++) {
				criteria[j].setPhase(phase[i]);
			}
			iterator.setRoot(phase[i].speciesMaster());
			iterator.allAtoms(neighborCheck);
			if (neighborCheck.needUpdate) {
				if (Debug.ON && Debug.DEBUG_NOW) {
					System.out.println("Updating neighbors");
				}
				if (neighborCheck.unsafe) {
					System.err.println("Atoms exceeded the safe neighbor limit");
				}
				boundary = phase[i].boundary();
				iterator.allAtoms(neighborReset);
				potentialMaster.calculate(phase[i],id,potentialCalculationNbrSetup);
				resetIntegrator = true;
			}
		}
        //TODO consider a reset(Phase) method for integrator to reset relative to just the affected phase
        if(resetIntegrator) integrator.reset();
	}
	
	public int getUpdateInterval() {
		return updateInterval;
	}
	
	public void setUpdateInterval(int updateInterval) {
		this.updateInterval = updateInterval;
	}
	
	public void addCriterion(NeighborCriterion criterion) {
		NeighborCriterion[] newCriteria = new NeighborCriterion[criteria.length+1];
		System.arraycopy(criteria, 0, newCriteria, 0, criteria.length);
		newCriteria[criteria.length] = criterion;
		criteria = newCriteria;
	}

    public boolean removeCriterion(NeighborCriterion criterion) {
    	for (int i=0; i<criteria.length; i++) {
    		if (criteria[i] == criterion) {
    	    	NeighborCriterion[] newCriteria = new NeighborCriterion[criteria.length-1];
    	    	System.arraycopy(criteria,0,newCriteria,0,i);
    	    	System.arraycopy(criteria,i+1,newCriteria,i,criteria.length-i-1);
    	    	criteria = newCriteria;
    	    	return true;
    		}
    	}
    	return false;
    }
    
    /**
     * @return Returns the interval-listener priority.
     */
    public int getPriority() {
        return priority;
    }
    /**
     * Sets the interval-listener priority.  Default value is 300, which
     * puts this after central-image enforcement.
     * @param priority The priority to set.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }


	private NeighborCriterion[] criteria = new NeighborCriterion[0];
	private int updateInterval;
	private int iieCount;
	private Space.Boundary boundary;
	private final PotentialMasterNbr potentialMaster;
	private final AtomIteratorTree iterator;
	private final NeighborCheck neighborCheck;
	private final NeighborReset neighborReset;
	private final IteratorDirective id = new IteratorDirective();
	private final PotentialCalculationNbrSetup potentialCalculationNbrSetup = new PotentialCalculationNbrSetup();
	private int priority;
    
	private static class NeighborCheck extends AtomsetActionAdapter {
		private boolean needUpdate = false, unsafe = false;
		public void actionPerformed(Atom[] atom) {
			NeighborCriterion criterion = atom[0].type.getNbrManagerAgent().getCriterion();
			if (criterion != null && criterion.needUpdate(atom[0])) {
				needUpdate = true;
				if (criterion.unsafe()) {
					if (Debug.DEBUG_NOW) {
						System.out.println("atom "+atom[0]+" exceeded safe limit");
					}
					unsafe = true;
				}
			}
		}
		
		public void reset() {
			needUpdate = false;
			unsafe = false;
		}

	}
	
	private class NeighborReset extends AtomsetActionAdapter {
		public void actionPerformed(Atom[] atom) {
            if(atom[0].node.depth() < 2) return;//don't want SpeciesMaster or SpeciesAgents
			NeighborCriterion criterion = atom[0].type.getNbrManagerAgent().getCriterion();
			((AtomSequencerNbr)atom[0].seq).clearNbrs();
			if (criterion != null) {
				boundary.centralImage(atom[0].coord);
				criterion.reset(atom[0]);
			}
		}
	}
}
