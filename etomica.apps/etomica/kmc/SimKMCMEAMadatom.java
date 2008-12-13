package etomica.kmc;

import etomica.action.CalcVibrationalModes;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.MoleculeArrayList;
import etomica.box.Box;
import etomica.chem.elements.Tin;
import etomica.config.Configuration;
import etomica.config.ConfigurationFile;
import etomica.config.ConfigurationLattice;
import etomica.dimer.IntegratorDimerRT;
import etomica.dimer.PotentialMasterListDimer;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.SimulationGraphic;
import etomica.lattice.BravaisLatticeCrystal;
import etomica.lattice.crystal.BasisBetaSnA5;
import etomica.lattice.crystal.PrimitiveTetragonal;
import etomica.meam.ParameterSetMEAM;
import etomica.meam.PotentialMEAM;
import etomica.nbr.CriterionSimple;
import etomica.nbr.CriterionTypesCombination;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.space3d.Vector3D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.numerical.CalcGradientDifferentiable;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom of Sn on a surface, modeled with MEAM.
 * 
 * @author msellers
 *
 */

public class SimKMCMEAMadatom extends Simulation{

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DimerMEAMadatomSn";
    public final PotentialMasterListDimer potentialMasterD;
    public IBox box;
    public SpeciesSpheresMono fixed, potentialSpecies, movable;
    public PotentialMEAM potential;
    public ActivityIntegrate activityIntegrateKMC, activityIntegrateDimer, activityIntegrateKMCCluster;
    public IntegratorKMC integratorKMC;
    public IntegratorKMCCluster integratorKMCCluster;
    public IntegratorDimerRT integratorDimer;
    public CalcGradientDifferentiable calcGradientDifferentiable;
    public CalcVibrationalModes calcVibrationalModes;
    public double [][] dForces;
    public int [] d, modeSigns;
    public double [] positions;
    public double [] lambdas, frequencies;
    public IVector adAtomPos;
    public IMoleculeList movableSet;
    //public Boolean saddleFine, calcModes, minSearch, normalDir;
    
    public SimKMCMEAMadatom() {
        super(Space3D.getInstance(), true);    	

        potentialMasterD = new PotentialMasterListDimer(this, space);
        
      //SIMULATION BOX
        box = new Box(new BoundaryRectangularSlit(random, 0, 5, space), space);
        addBox(box);
     
      //SPECIES
        
        //Sn
        Tin tinFixed = new Tin("SnFix", Double.POSITIVE_INFINITY);  
        fixed = new SpeciesSpheresMono(this, space, tinFixed);
        movable = new SpeciesSpheresMono(this, space, Tin.INSTANCE);
        potentialSpecies = new SpeciesSpheresMono(this, space, tinFixed);
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        getSpeciesManager().addSpecies(potentialSpecies);
        ((IAtomTypeSphere)fixed.getLeafType()).setDiameter(3.022); 
        ((IAtomTypeSphere)movable.getLeafType()).setDiameter(3.022);
        ((IAtomTypeSphere)potentialSpecies.getLeafType()).setDiameter(3.022);
        box.setNMolecules(fixed, 180);
        
        potential = new PotentialMEAM(space);
        potential.setParameters(fixed.getLeafType(), ParameterSetMEAM.Sn);
        potential.setParameters(movable.getLeafType(), ParameterSetMEAM.Sn);
        potential.setParameters(potentialSpecies.getLeafType(), ParameterSetMEAM.Sn);
        
        
        
        //Sn
        //beta-Sn box
        
        //The dimensions of the simulation box must be proportional to those of
        //the unit cell to prevent distortion of the lattice.  The values for the 
        //lattice parameters for tin's beta box (a = 5.8314 angstroms, c = 3.1815 
        //angstroms) are taken from the ASM Handbook. 
              
        double a = 5.92; 
        double c = 3.23;
        box.getBoundary().setDimensions(new Vector3D(a*3, a*3, c*5));
        PrimitiveTetragonal primitive = new PrimitiveTetragonal(space, a, c);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisBetaSnA5());

                
        //Ag
        /**
        Silver silverFixed = new Silver("AgFix", Double.POSITIVE_INFINITY);
        fixed = new SpeciesSpheresMono(this, Silver.INSTANCE);
        movable = new SpeciesSpheresMono(this, Silver.INSTANCE);
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        ((AtomTypeSphere)fixed.getLeafType()).setDiameter(2.8895); 
        ((AtomTypeSphere)movable.getLeafType()).setDiameter(2.8895);
        potential = new PotentialMEAM(space);
        potential.setParameters(agFix, ParameterSetMEAM.Ag);
        potential.setParameters(ag, ParameterSetMEAM.Ag);
        potential.setParameters(agAdatom, ParameterSetMEAM.Ag);
        potential.setParameters(movable, ParameterSetMEAM.Ag);
        
        double a = 4.0863;
        PrimitiveCubic primitive = new PrimitiveCubic(space, a);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisCubicFcc());
        
        */
    
        
        //Cu
        /**
        //Copper copperFixed = new Copper("CuFix", Double.POSITIVE_INFINITY);
        fixed = new SpeciesSpheresMono(this, space, Copper.INSTANCE);
        movable = new SpeciesSpheresMono(this, space, Copper.INSTANCE);
        dimer = new SpeciesSpheresMono(this, space, Copper.INSTANCE);
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        getSpeciesManager().addSpecies(dimer);
        ((AtomTypeSphere)fixed.getLeafType()).setDiameter(2.5561); 
        ((AtomTypeSphere)dimer.getLeafType()).setDiameter(2.5561); 
        ((AtomTypeSphere)movable.getLeafType()).setDiameter(2.5561);
        potential = new PotentialMEAM(space);
        potential.setParameters(fixed.getLeafType(), ParameterSetMEAM.Cu);
        potential.setParameters(movable.getLeafType(), ParameterSetMEAM.Cu);
        potential.setParameters(dimer.getLeafType(), ParameterSetMEAM.Cu);
        
        double a = 3.6148;
        PrimitiveCubic primitive = new PrimitiveCubic(space, a);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisCubicFcc());
        */
        
        Configuration config = new ConfigurationLattice(crystal, space);
        config.initializeCoordinates(box);

        this.potentialMasterD.addPotential(potential, new IAtomTypeLeaf[]{movable.getLeafType(), potentialSpecies.getLeafType()});
        potentialMasterD.setSpecies(new ISpecies []{potentialSpecies, movable});
        potentialMasterD.setRange(potential.getRange()*1.1);
        CriterionSimple criteria2 = new CriterionSimple(this, space, potential.getRange(), potential.getRange()*1.1);
        potentialMasterD.setCriterion(potential, new CriterionTypesCombination(criteria2, new IAtomTypeLeaf[] {movable.getLeafType(), potentialSpecies.getLeafType()}));
        
    //ADATOM CREATION AND PLACEMENT
        // Sn
        
        IMolecule iMolecule = movable.makeMolecule();
        box.addMolecule(iMolecule);
        adAtomPos = ((IAtomPositioned)iMolecule.getChildList().getAtom(0)).getPosition();
        //adAtomPos = getSpace().makeVector();
        adAtomPos.setX(0, 9.8);
        adAtomPos.setX(1, 0.2);
        adAtomPos.setX(2, -1.0);
        IVector newBoxLength = space.makeVector();
        newBoxLength.E(box.getBoundary().getDimensions());
        newBoxLength.setX(0, 2.0*adAtomPos.x(0)+2.0);
        box.getBoundary().setDimensions(newBoxLength);
        
        /**
        //Ag
        IAtom iAtom = agAdatom.getMoleculeFactory().makeAtom();
        box.getAgent(agAdatom).addChildAtom(iAtom);
        ((IAtomPositioned)iAtom).getPosition().setX(0, 7.5);
        ((IAtomPositioned)iAtom).getPosition().setX(1, 0.9477016722828758);
        ((IAtomPositioned)iAtom).getPosition().setX(2, 1.0709520701043456);
        */
        /**
        //Cu
        IAtom iAtom = cuAdatom.getMoleculeFactory().makeAtom();
        box.getAgent(cuAdatom).addChildAtom(iAtom);
        ((IAtomPositioned)iAtom).getPosition().setX(0, 6.0);
        ((IAtomPositioned)iAtom).getPosition().setX(1, 0.9477016722828758);
        ((IAtomPositioned)iAtom).getPosition().setX(2, 1.0709520701043456);
        */
    }
    
    public void setMovableAtoms(double distance, IVector center){
        //distance = distance*distance;
        IVector rij = space.makeVector();
        MoleculeArrayList movableList = new MoleculeArrayList();
        IMoleculeList loopSet = box.getMoleculeList();
        for (int i=0; i<loopSet.getMoleculeCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)loopSet.getMolecule(i).getChildList().getAtom(0)).getPosition());
            if(rij.x(0) > (box.getBoundary().getDimensions().x(0) - 3.0)){continue;}
            //box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               movableList.add(loopSet.getMolecule(i));
            } 
        }
        for (int i=0; i<movableList.getMoleculeCount(); i++){
            IMolecule newMolecule = movable.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)movableList.getMolecule(i).getChildList().getAtom(0)).getPosition());
            box.removeMolecule(movableList.getMolecule(i));
        }
        movableSet = box.getMoleculeList(movable);
    }
    
    public void setPotentialListAtoms(){
        MoleculeArrayList neighborList = new MoleculeArrayList();
        MoleculeArrayList fixedList = new MoleculeArrayList();
        IMoleculeList loopSet = box.getMoleculeList();
        IMoleculeList movableSet = box.getMoleculeList(movable);
        for(int i=0; i<loopSet.getMoleculeCount(); i++){
            if(loopSet.getMolecule(i).getType()==movable){
                continue;
            }
            if(((IAtomPositioned)loopSet.getMolecule(i).getChildList().getAtom(0)).getPosition().x(0) < -4.0){continue;}
            boolean fixedFlag = true;
            for(int j=0; j<movableSet.getMoleculeCount(); j++){
                IVector dist = space.makeVector();
                dist.Ev1Mv2(((IAtomPositioned)loopSet.getMolecule(i).getChildList().getAtom(0)).getPosition(),((IAtomPositioned)movableSet.getMolecule(j).getChildList().getAtom(0)).getPosition());
                box.getBoundary().nearestImage(dist);
                if(Math.sqrt(dist.squared())<potentialMasterD.getMaxPotentialRange()+2.0){
                    neighborList.add(loopSet.getMolecule(i));
                    fixedFlag = false;
                    break;
                }               
            
            }
            if(fixedFlag){
                fixedList.add(loopSet.getMolecule(i));
            }
        }
        for (int i=0; i<neighborList.getMoleculeCount(); i++){
            IMolecule newMolecule = potentialSpecies.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)neighborList.getMolecule(i).getChildList().getAtom(0)).getPosition());
            box.removeMolecule(neighborList.getMolecule(i));
         }
        for (int i=0; i<fixedList.getMoleculeCount(); i++){
            IMolecule newMolecule = fixed.makeMolecule();
            box.addMolecule(newMolecule);
            ((IAtomPositioned)newMolecule.getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)fixedList.getMolecule(i).getChildList().getAtom(0)).getPosition());
            box.removeMolecule(fixedList.getMolecule(i));
         }
        
    }
    
    public void randomizePositions(){
        IVector workVector = space.makeVector();
        IMoleculeList loopSet3 = box.getMoleculeList(movable);
        IVector [] currentPos = new IVector [loopSet3.getMoleculeCount()];
        double offset = 0;
        for(int i=0; i<currentPos.length; i++){
            currentPos[i] = space.makeVector();
            currentPos[i] = (((IAtomPositioned)loopSet3.getMolecule(i).getChildList().getAtom(0)).getPosition());
            for(int j=0; j<3; j++){
                offset = random.nextGaussian()/10.0;
                if(Math.abs(offset)>0.1){offset=0.1;}
                workVector.setX(j,offset);
            }
            currentPos[i].PE(workVector);
        }
    }
    
    //Must be run after setMovableAtoms
    public void removeAtoms(double distance, IVector center){
        distance = distance*distance;
        IVector rij = space.makeVector();
        
        IMoleculeList loopSet = box.getMoleculeList(movable);
        for (int i=0; i<loopSet.getMoleculeCount(); i++){
            rij.Ev1Mv2(center,((IAtomPositioned)loopSet.getMolecule(i).getChildList().getAtom(0)).getPosition());
            box.getBoundary().nearestImage(rij);
            if(rij.squared() < distance){
               box.removeMolecule(loopSet.getMolecule(i));
            } 
        }   
    }
    
    public void initializeConfiguration(String fileName){
        ConfigurationFile config = new ConfigurationFile(fileName);
        config.initializeCoordinates(box);
    }
    
    public void integratorKMC(){
        integratorKMC = new IntegratorKMC(this, potentialMasterD, 273.15, this.getRandom(), new ISpecies[]{movable}, this.getSpace());
        integratorKMC.setBox(box);
        activityIntegrateKMC = new ActivityIntegrate(integratorKMC);
        getController().addAction(activityIntegrateKMC);
    }
    
    public void integratorKMCCluster(double temp, int steps, int totalSearch){
        integratorKMCCluster = new IntegratorKMCCluster(this, potentialMasterD, temp, totalSearch, this.getRandom(), new ISpecies[]{movable}, this.getSpace());
        integratorKMCCluster.setBox(box);
        activityIntegrateKMCCluster = new ActivityIntegrate(integratorKMCCluster);
        activityIntegrateKMCCluster.setMaxSteps(steps);
        getController().addAction(activityIntegrateKMCCluster);
    }
    
public void enableDimerSearch(String fileName, long maxSteps){
        
        integratorDimer = new IntegratorDimerRT(this, potentialMasterD, new ISpecies[]{movable}, space);
        integratorDimer.setBox(box);
        integratorDimer.setOrtho(false, false);
        integratorDimer.setFileName(fileName);
        integratorDimer.addNonintervalListener(potentialMasterD.getNeighborManager(box));
        integratorDimer.addIntervalAction(potentialMasterD.getNeighborManager(box));  
        activityIntegrateDimer = new ActivityIntegrate(integratorDimer);
        integratorDimer.setActivityIntegrate(activityIntegrateDimer);
        getController().addAction(activityIntegrateDimer);
        activityIntegrateDimer.setMaxSteps(maxSteps);
    }
    
    public static void main(String[] args){
       
        final SimKMCMEAMadatom sim = new SimKMCMEAMadatom();
        IVector vect = sim.getSpace().makeVector();
        vect.setX(0, 9.8);
        vect.setX(1, -0.2);
        vect.setX(2, -0.2);
        
        sim.setMovableAtoms(100.0, vect);
        
        sim.setPotentialListAtoms();
        sim.initializeConfiguration("0-MEAM_saddle");
        sim.integratorKMCCluster(400.0, 800, 30);

        //energy: -3331480.584975273    Vib: 9.561284069712113E96
        sim.integratorKMCCluster.setInitialStateConditions(-3331480.584975273, 9.561284069712113E96);
        
        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME,1, sim.space, sim.getController());
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        
        //sim.integratorMD.addIntervalAction(simGraphic.getPaintAction(sim.box));
        sim.integratorKMCCluster.addIntervalAction(simGraphic.getPaintAction(sim.box));

    	ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
    	
    	colorScheme.setColor(sim.fixed.getLeafType(),java.awt.Color.gray);
        colorScheme.setColor(sim.movable.getLeafType(),java.awt.Color.red);
        colorScheme.setColor(sim.potentialSpecies.getLeafType(),java.awt.Color.PINK);

    	simGraphic.makeAndDisplayFrame(APP_NAME);
    }

}