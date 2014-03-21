package etomica.nbr.cell;

import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.atom.IAtomPositionDefinition;
import etomica.box.BoxAgentManager.BoxAgentSource;
import etomica.space.ISpace;

/**
 * BoxAgentSource responsible for creating a NeighborCellManager.
 */
public class BoxAgentSourceCellManager implements BoxAgentSource<NeighborCellManager> {

    public BoxAgentSourceCellManager(ISimulation sim, IAtomPositionDefinition positionDefinition, ISpace _space) {
        this.sim = sim;
        this.positionDefinition = positionDefinition;
        this.space = _space;
    }
    
    public void setRange(double d) {
        range = d;
    }

    public NeighborCellManager makeAgent(IBox box) {
        NeighborCellManager cellManager = new NeighborCellManager(sim, box,range,positionDefinition, space);
        box.getBoundary().getEventManager().addListener(cellManager);
        return cellManager;
    }
    
    public void releaseAgent(NeighborCellManager agent) {
    }
    
    protected final ISimulation sim;
    protected double range;
    protected final IAtomPositionDefinition positionDefinition;
    protected final ISpace space;
}