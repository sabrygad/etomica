package etomica.virial.simulations;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IListener;
import etomica.api.ISpecies;
import etomica.data.AccumulatorRatioAverage;
import etomica.data.DataPump;
import etomica.data.types.DataGroup;
import etomica.exception.ConfigurationOverlapException;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveBoxStep;
import etomica.integrator.mcmove.MCMoveManager;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.species.SpeciesSpheresMono;
import etomica.species.SpeciesSpheresRotating;
import etomica.virial.BoxCluster;
import etomica.virial.ClusterAbstract;
import etomica.virial.ClusterWeight;
import etomica.virial.ClusterWeightAbs;
import etomica.virial.ConfigurationCluster;
import etomica.virial.MCMoveClusterAtomMulti;
import etomica.virial.MCMoveClusterAtomRotateMulti;
import etomica.virial.MCMoveClusterMoleculeMulti;
import etomica.virial.MCMoveClusterRotateMoleculeMulti;
import etomica.virial.MCMoveClusterWiggleMulti;
import etomica.virial.MeterVirialRejected;
import etomica.virial.P0Cluster;
import etomica.virial.SpeciesFactory;
import etomica.virial.overlap.DataSourceVirialOverlapRejected;
import etomica.virial.overlap.IntegratorOverlapRejected;

/**
 * @author kofke
 *
 * Simulation implementing the overlap-sampling approach to evaluating a cluster
 * diagram.
 */
public class SimulationVirialOverlapRejected extends Simulation {

	/* If this constructor is used to instantiate the simulation, then doWiggle is set to false, and 
	 * ClusterAbstract[] is set to {refCluster,targetCluster}
	 */
	
    public SimulationVirialOverlapRejected(Space aSpace, SpeciesFactory speciesFactory, 
            double temperature, ClusterAbstract refCluster, ClusterAbstract targetCluster) {
        this(aSpace,speciesFactory,temperature,refCluster, targetCluster, false);
    }

    public SimulationVirialOverlapRejected(Space aSpace, SpeciesFactory speciesFactory, 
			double temperature, ClusterAbstract refCluster, ClusterAbstract targetCluster, boolean doWiggle) {
		this(aSpace,speciesFactory,temperature,new ClusterAbstract[]{refCluster,targetCluster},
                new ClusterWeight[]{ClusterWeightAbs.makeWeightCluster(refCluster),ClusterWeightAbs.makeWeightCluster(targetCluster)}, doWiggle);
	}
	
	public SimulationVirialOverlapRejected(Space aSpace, SpeciesFactory speciesFactory, 
            double temperature, final ClusterAbstract[] aValueClusters, final ClusterWeight[] aSampleClusters, boolean doWiggle) {
		super(aSpace,false);
		PotentialMaster potentialMaster = new PotentialMaster(space);
        sampleClusters = aSampleClusters;
        int nMolecules = sampleClusters[0].pointCount();
        species = speciesFactory.makeSpecies(this, space);
        getSpeciesManager().addSpecies(species);
        accumulators = new AccumulatorRatioAverage[sampleClusters.length];
        accumulatorPumps = new DataPump[sampleClusters.length];
        box = new BoxCluster[sampleClusters.length];
        integrators = new IntegratorMC[sampleClusters.length];
        meters = new MeterVirialRejected[sampleClusters.length];
        mcMoveTranslate = new MCMoveBoxStep[sampleClusters.length];
        if (!(species instanceof SpeciesSpheresMono)) {
            mcMoveRotate = new MCMoveBoxStep[sampleClusters.length];
        }
        if (doWiggle) {
            mcMoveWiggle = new MCMoveClusterWiggleMulti[sampleClusters.length];
        }
        
        P0Cluster p0 = new P0Cluster(space);
        potentialMaster.addPotential(p0,new ISpecies[]{});
        
        blockSize = 1000;
        
        for (int iBox=0; iBox<sampleClusters.length; iBox++) {
            // integrator for iBox samples based on iBox cluster
            box[iBox] = new BoxCluster(this,sampleClusters[iBox], space);
            addBox(box[iBox]);
            box[iBox].setNMolecules(species, nMolecules);
            
            integrators[iBox] = new IntegratorMC(this, potentialMaster);
            integrators[iBox].setTemperature(temperature);
            integrators[iBox].setBox(box[iBox]);
            integrators[iBox].getMoveManager().setEquilibrating(false);
            
            MCMoveManager moveManager = integrators[iBox].getMoveManager();
            
            if (species instanceof SpeciesSpheresMono || species instanceof SpeciesSpheresRotating) {
                mcMoveTranslate[iBox] = new MCMoveClusterAtomMulti(this, potentialMaster, space);
                mcMoveTranslate[iBox].setStepSize(0.41);
                ((MCMoveStepTracker)mcMoveTranslate[iBox].getTracker()).setAcceptanceTarget(0.4);
                moveManager.addMCMove(mcMoveTranslate[iBox]);
                
                if (species instanceof SpeciesSpheresRotating) {
                    mcMoveRotate[iBox] = new MCMoveClusterAtomRotateMulti(random, potentialMaster, space, nMolecules-1);
                    moveManager.addMCMove(mcMoveRotate[iBox]);
                }
            }
            else {
                mcMoveRotate[iBox] = new MCMoveClusterRotateMoleculeMulti(potentialMaster,getRandom(), space);
                mcMoveRotate[iBox].setStepSize(Math.PI);
                moveManager.addMCMove(mcMoveRotate[iBox]);
                mcMoveTranslate[iBox] = new MCMoveClusterMoleculeMulti(this, potentialMaster, space);
                moveManager.addMCMove(mcMoveTranslate[iBox]);
                if (doWiggle && species.getNumLeafAtoms() > 2) {
                    mcMoveWiggle[iBox] = new MCMoveClusterWiggleMulti(this, potentialMaster, nMolecules, space);
                    moveManager.addMCMove(mcMoveWiggle[iBox]);
                }
            }
            
            ConfigurationCluster configuration = new ConfigurationCluster(space);
            configuration.initializeCoordinates(box[iBox]);
            MeterVirialRejected meter = new MeterVirialRejected(new ClusterAbstract[]{aValueClusters[iBox],aSampleClusters[1-iBox].makeCopy()},
                    11, iBox==0);
            setMeter(meter,iBox);
            AccumulatorRatioAverage acc = new AccumulatorRatioAverage();
            setAccumulator(acc,iBox);
              
        }
        
        setRefPref(1,5);
        integratorOS = new IntegratorOverlapRejected(getRandom(), integrators);
        integratorOS.setNumSubSteps(1000);
        integratorOS.setEventInterval(1);
        ai = new ActivityIntegrate(integratorOS);
        getController().addAction(ai);
		
		dsvo = new DataSourceVirialOverlapRejected(accumulators[0],accumulators[1],meters[0], meters[1]);
        integratorOS.setDSVO(dsvo);
	}

    public void setRefPref(double refPrefCenter, double span) {
        refPref = refPrefCenter;
        meters[0].setBennetParam(refPrefCenter,span);
        meters[1].setBennetParam(refPrefCenter,span);
        accumulators[0].reset();
        accumulators[1].reset();
    }
    
    public void setMeter(MeterVirialRejected newMeter, int iBox) {
        // we need a new accumulator so nuke the old one now.
        if (accumulatorPumps[iBox] != null) {
            integrators[iBox].removeIntervalAction(accumulatorPumps[iBox]);
            accumulatorPumps[iBox] = new DataPump(newMeter, accumulators[iBox]);
            integrators[iBox].addIntervalAction(accumulatorPumps[iBox]);
        }
        if (meters[iBox] != null) {
            integrators[iBox].getMoveEventManager().removeListener(meters[iBox]);
        }
        meters[iBox] = newMeter;
        integrators[iBox].getMoveEventManager().addListener(meters[iBox]);
        if (integratorOS != null) {
            dsvo = new DataSourceVirialOverlapRejected(accumulators[0],accumulators[1], meters[0], meters[1]);
            integratorOS.setDSVO(dsvo);
        }
    }

    public void setAccumulator(AccumulatorRatioAverage newAccumulator, int iBox) {
        accumulators[iBox] = newAccumulator;
        accumulators[iBox].setBlockSize(blockSize);
        if (accumulatorPumps[iBox] == null) {
            accumulatorPumps[iBox] = new DataPump(meters[iBox],newAccumulator);
            integrators[iBox].addIntervalAction(accumulatorPumps[iBox]);
        }
        else {
            accumulatorPumps[iBox].setDataSink(newAccumulator);
        }
        if (integratorOS != null) {
            dsvo = new DataSourceVirialOverlapRejected(accumulators[0],accumulators[1], meters[0], meters[1]);
            integratorOS.setDSVO(dsvo);
        }
    }
    
    public void setAccumulatorBlockSize(int newBlockSize) {
        blockSize = newBlockSize;
        for (int i=0; i<2; i++) {
            accumulators[i].setBlockSize(newBlockSize);
        }
        try {
            // reset the integrator so that it will re-adjust step frequency
            // and ensure it will take enough data for both ref and target
            integratorOS.reset();
        }
        catch (ConfigurationOverlapException e) { /* meaningless */ }
    }

    public void setRefPref(double newRefPref) {
        setMeter(new MeterVirialRejected(meters[0].getClusters(), 1, true), 0);
        setMeter(new MeterVirialRejected(meters[1].getClusters(), 1, false), 1);
        accumulators[0].reset();
        accumulators[1].reset();
        setRefPref(newRefPref,1);
    }
    
    public void initRefPref(String fileName, long initSteps) {
        // use the old refpref value as a starting point so that an initial
        // guess can be provided
        double oldRefPref = refPref;
        // refPref = -1 indicates we are searching for an appropriate value
        refPref = -1.0;
        if (fileName != null) {
            try { 
                FileReader fileReader = new FileReader(fileName);
                BufferedReader bufReader = new BufferedReader(fileReader);
                String refPrefString = bufReader.readLine();
                refPref = Double.parseDouble(refPrefString);
                bufReader.close();
                fileReader.close();
                System.out.println("setting ref pref (from file) to "+refPref);
                setRefPref(refPref);
            }
            catch (IOException e) {
                // file not there, which is ok.
            }
        }
        
        if (refPref == -1) {
            for (int i=0; i<2; i++) {
                integrators[i].getMoveManager().setEquilibrating(true);
            }

            setMeter(new MeterVirialRejected(meters[0].getClusters(), 21, true), 0);
            setMeter(new MeterVirialRejected(meters[1].getClusters(), 21, false), 1);
            setRefPref(oldRefPref,30);
            ai.setMaxSteps(initSteps);
            ai.actionPerformed();

            int newMinDiffLoc = dsvo.minDiffLocation();
            refPref = ((DataGroup)accumulators[0].getData()).getData(AccumulatorRatioAverage.StatType.AVERAGE.index).getValue(newMinDiffLoc+1)
                /((DataGroup)accumulators[1].getData()).getData(AccumulatorRatioAverage.StatType.AVERAGE.index).getValue(newMinDiffLoc+1);
            System.out.println("setting ref pref to "+refPref);
            setMeter(new MeterVirialRejected(meters[0].getClusters(), 15, true), 0);
            setMeter(new MeterVirialRejected(meters[1].getClusters(), 15, false), 1);
            setRefPref(refPref,4);
            for (int i=0; i<2; i++) {
                try {
                    integrators[i].reset();
                }
                catch (ConfigurationOverlapException e) {}
            }
            // set refPref back to -1 so that later on we know that we've been looking for
            // the appropriate value
            refPref = -1;
        }

    }
    
    public void equilibrate(String fileName, long initSteps) {
        // run a short simulation to get reasonable MC Move step sizes and
        // (if needed) narrow in on a reference preference
        ai.setMaxSteps(initSteps);

        for (int i=0; i<2; i++) {
            integrators[i].getMoveManager().setEquilibrating(true);
        }
        ai.actionPerformed();

        if (refPref == -1) {
            int newMinDiffLoc = dsvo.minDiffLocation();
            refPref = ((DataGroup)accumulators[0].getData()).getData(AccumulatorRatioAverage.StatType.AVERAGE.index).getValue(newMinDiffLoc+1)
                    /((DataGroup)accumulators[1].getData()).getData(AccumulatorRatioAverage.StatType.AVERAGE.index).getValue(newMinDiffLoc+1);
            System.out.println("setting ref pref to "+refPref+" ("+newMinDiffLoc+")");
//            int n = sim.accumulators[0].getNBennetPoints();
//            for (int i=0; i<n; i++) {
//                System.out.println(i+" "+sim.accumulators[0].getBennetBias(i)+" "+sim.accumulators[0].getBennetAverage(i)/sim.accumulators[1].getBennetAverage(i));
//            }
            setRefPref(refPref);
            if (fileName != null) {
                try {
                    FileWriter fileWriter = new FileWriter(fileName);
                    BufferedWriter bufWriter = new BufferedWriter(fileWriter);
                    bufWriter.write(String.valueOf(refPref)+"\n");
                    bufWriter.close();
                    fileWriter.close();
                }
                catch (IOException e) {
                    throw new RuntimeException("couldn't write to refpref file");
                }
            }
        }
        else {
            dsvo.reset();
        }
        for (int i=0; i<2; i++) {
            integrators[i].getMoveManager().setEquilibrating(false);
        }
    }
    
    private static final long serialVersionUID = 1L;
//	protected DisplayPlot plot;
	public DataSourceVirialOverlapRejected dsvo;
    public AccumulatorRatioAverage[] accumulators;
    protected DataPump[] accumulatorPumps;
	protected final ClusterWeight[] sampleClusters;
    public BoxCluster[] box;
    public ISpecies species;
    public IntegratorMC[] integrators;
    public MCMoveBoxStep[] mcMoveRotate;
    public MCMoveBoxStep[] mcMoveTranslate;
    public MCMoveClusterWiggleMulti[] mcMoveWiggle;
    public MeterVirialRejected[] meters;
    public ActivityIntegrate ai;
    public IntegratorOverlapRejected integratorOS;
    public double refPref;
    protected int blockSize;

  /*  public static void main(String[] args) {

        int nPoints = 4;
        double temperature = 1.3; //temperature governing sampling of configurations
        double sigmaHSRef = 1.5;
        double sigmaLJ = 1.0;
        double[] HSB = new double[7];
        HSB[2] = Standard.B2HS(sigmaHSRef);
        HSB[3] = Standard.B3HS(sigmaHSRef);
        HSB[4] = Standard.B4HS(sigmaHSRef);
        HSB[5] = Standard.B5HS(sigmaHSRef);
        HSB[6] = Standard.B6HS(sigmaHSRef);
        System.out.println("sigmaHSRef: "+sigmaHSRef);
        System.out.println("B2HS: "+HSB[2]);
        System.out.println("B3HS: "+HSB[3]+" = "+(HSB[3]/(HSB[2]*HSB[2]))+" B2HS^2");
        System.out.println("B4HS: "+HSB[4]+" = "+(HSB[4]/(HSB[2]*HSB[2]*HSB[2]))+" B2HS^3");
        System.out.println("B5HS: "+HSB[5]+" = 0.110252 B2HS^4");
        System.out.println("B6HS: "+HSB[6]+" = 0.03881 B2HS^5");

        Space3D space = Space3D.getInstance();
        MayerHardSphere fRef = new MayerHardSphere(space,sigmaHSRef);
        MayerEHardSphere eRef = new MayerEHardSphere(space,sigmaHSRef);
        P2LennardJones p2LJ = new P2LennardJones(space,sigmaLJ,1.0);
        MayerGeneralSpherical fTarget = new MayerGeneralSpherical(space,p2LJ);
        MayerESpherical eTarget = new MayerESpherical(space,p2LJ);

        ClusterAbstract refCluster = Standard.virialCluster(nPoints,fRef,true,eRef,true);
        refCluster.setTemperature(temperature);
        ClusterAbstract targetCluster = Standard.virialCluster(nPoints,fTarget,true,eTarget,true);
        targetCluster.setTemperature(temperature);

        int maxSteps = 10000;

        SimulationVirialOverlap sim = new SimulationVirialOverlap(space, new SpeciesFactorySpheres(), temperature, refCluster, targetCluster);
        sim.ai.setMaxSteps(maxSteps);
        sim.ai.actionPerformed();
        System.out.println("average: "+sim.dsvo.getDataAsScalar()+", error: "+sim.dsvo.getError());
        DataGroup allYourBase = (DataGroup)sim.accumulators[0].getData(sim.dsvo.minDiffLocation());
        System.out.println("hard sphere ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("hard sphere   average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("hard sphere overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
        allYourBase = (DataGroup)sim.accumulators[1].getData(sim.dsvo.minDiffLocation());
        System.out.println("lennard jones ratio average: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorRatioAverage.StatType.RATIO_ERROR.index)).getData()[1]);
        System.out.println("lennard jones average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[0]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[0]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[0]);
        System.out.println("lennard jones overlap average: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.AVERAGE.index)).getData()[1]
                          +" stdev: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.STANDARD_DEVIATION.index)).getData()[1]
                          +" error: "+((DataDoubleArray)allYourBase.getData(AccumulatorAverage.StatType.ERROR.index)).getData()[1]);
    }*/
}