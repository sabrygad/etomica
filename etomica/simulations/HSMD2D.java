package etomica.simulations;
import etomica.Default;
import etomica.IntegratorHard;
import etomica.IteratorDirective;
import etomica.P2HardSphere;
import etomica.Phase;
import etomica.Potential2;
import etomica.Simulation;
import etomica.Space2D;
import etomica.Species;
import etomica.SpeciesSpheresMono;
import etomica.action.activity.ActivityIntegrate;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceTrioControllerButton;
import etomica.graphics.DisplayPhase;
import etomica.graphics.SimulationGraphic;
import etomica.nbr.PotentialCalculationNbrSetup;
import etomica.nbr.PotentialMasterNbr;

/**
 * Simple hard-sphere molecular dynamics simulation in 2D.
 *
 * @author David Kofke
 */
 
public class HSMD2D extends Simulation {
    
    public IntegratorHard integrator;
    public SpeciesSpheresMono species, species2;
    public Phase phase;
    public Potential2 potential;

    public HSMD2D() {
    	this(new Space2D());
    }
    
    public HSMD2D(Space2D space) {
        super(space, new PotentialMasterNbr(space));
        Default.makeLJDefaults();
  //can't use cell list until integrator is updated for it      setIteratorFactory(new IteratorFactoryCell(this));
//        Default.BOX_SIZE = 30.0;
        Default.ATOM_SIZE = 0.4;
        integrator = new IntegratorHard(potentialMaster);
        integrator.addIntervalListener(((PotentialMasterNbr)potentialMaster).getNeighborManager());
        integrator.setTimeStep(0.01);
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheresMono(this);
	    species2 = new SpeciesSpheresMono(this);
	    species.setNMolecules(500);
	    species2.setNMolecules(5);
	    phase = new Phase(space);
	    potential = new P2HardSphere(space);
	    this.potentialMaster.setSpecies(potential,new Species[]{species,species});
	    this.potentialMaster.setSpecies(potential,new Species[]{species2,species2});
	    this.potentialMaster.setSpecies(potential,new Species[]{species,species2});
        
//		elementCoordinator.go();
        //explicit implementation of elementCoordinator activities
        phase.speciesMaster.addSpecies(species);
        phase.speciesMaster.addSpecies(species2);
        integrator.addPhase(phase);
    }
    
    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
        HSMD2D sim = new HSMD2D();
		sim.getController().start();
    }//end of main
    
}