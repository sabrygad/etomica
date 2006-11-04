package etomica.normalmode;

import etomica.action.PhaseImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomFactoryMono;
import etomica.atom.AtomType;
import etomica.atom.AtomTypeSphere;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.Data;
import etomica.data.DataHistogram;
import etomica.data.DataInfo;
import etomica.data.DataProcessor;
import etomica.data.DataPump;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.types.DataArithmetic;
import etomica.data.types.DataDouble;
import etomica.data.types.DataGroup;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayBoxesCAE;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntegratorMD;
import etomica.integrator.IntervalActionAdapter;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.crystal.PrimitiveFcc;
import etomica.phase.Phase;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularPeriodic;
import etomica.space.Space;
import etomica.space.Vector;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Null;
import etomica.util.DoubleRange;
import etomica.util.HistogramSimple;

/**
 * Simulation to run sampling with the hard sphere potential, but measuring
 * the harmonic potential based on normal mode data from a previous simulation.
 * @author Andrew Schultz
 */
public class TestFccHarmonic extends Simulation {

    public TestFccHarmonic(Space space, int numAtoms) {
        super(space, true, new PotentialMaster(space));

        defaults.makeLJDefaults();
        defaults.atomSize = 1.0;
        defaults.ignoreOverlap = true;

        SpeciesSpheresMono species = new SpeciesSpheresMono(this);

        phase = new Phase(this);
        phase.getAgent(species).setNMolecules(numAtoms);

        integrator = new IntegratorHard(this);

        integrator.setIsothermal(false);
        activityIntegrate = new ActivityIntegrate(this,
                integrator);
        double timeStep = 0.04;
        integrator.setTimeStep(timeStep);
        getController().addAction(activityIntegrate);
        // activityIntegrate.setMaxSteps(nSteps);

        Potential potential = new P2HardSphere(space, defaults.atomSize, false);
        AtomTypeSphere sphereType = (AtomTypeSphere) ((AtomFactoryMono) species
                .moleculeFactory()).getType();
        potentialMaster.addPotential(potential, new AtomType[] { sphereType,
                sphereType });

        bdry = new BoundaryRectangularPeriodic(this);
        phase.setBoundary(bdry);
        phase.setDensity(1.04);

        PhaseImposePbc makeperiodic = new PhaseImposePbc(phase);
        integrator.addListener(makeperiodic);

        lattice = new LatticeCubicFcc();
        ConfigurationLattice config = new ConfigurationLattice(lattice);
        // config.setRescalingToFitVolume(false);

        config.initializeCoordinates(phase);

        // nan this section is a patch
        // first we find out the scaling used in
        // ConfigurationLattice/LatticeCubicFcc
        // then, we create a primitive fcc lattice, and scale it so we can use
        // it in pri.
        primitive = lattice.getPrimitiveFcc();
        ConfigurationLattice.MyLattice myLattice = (ConfigurationLattice.MyLattice) config
                .getLatticeMemento();
        Vector scaling = myLattice.latticeScaling;
        primitive.setCubicSize(primitive.getCubicSize()*scaling.x(0));

        // nan phase.setDensity(1.04);
        integrator.setPhase(phase);

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        int nA = 108;
        boolean graphic = true;
        TestFccHarmonic sim = new TestFccHarmonic(Space3D.getInstance(), nA);
        sim.getDefaults().blockSize = 50;
        String filename = "normal_modes400";
        if (args.length > 0) {
            filename = args[0];
        }
        
        double harmonicFudge = 1;
        
        double[][] omegaSquared = ArrayReader1D.getFromFile(filename+".val");
        for (int i=0; i<omegaSquared.length; i++) {
            for (int j=0; j<omegaSquared[i].length; j++) {
                // omega is sqrt(kT)/eigenvalue
                omegaSquared[i][j] = 1/omegaSquared[i][j]/harmonicFudge;
            }
        }
        Vector[] q = ArrayReader1D.getVectorsFromFile(filename+".Q");
        double[][][] eigenvectors = ArrayReader2D.getFromFile(filename+".vec");
        MeterHarmonicEnergy harmonicEnergy = new MeterHarmonicEnergy();
        harmonicEnergy.setEigenvectors(eigenvectors);
        harmonicEnergy.setOmegaSquared(omegaSquared);
        harmonicEnergy.setWaveVectors(q);
        harmonicEnergy.setPhase(sim.phase);
        AccumulatorAverage harmonicAvg = new AccumulatorAverage(sim);
        DataPump pump = new DataPump(harmonicEnergy, harmonicAvg);
        IntervalActionAdapter adapter = new IntervalActionAdapter(pump);
        adapter.setActionInterval(2);
        sim.integrator.addListener(adapter);
        
        MeterHarmonicSingleEnergy harmonicSingleEnergy = new MeterHarmonicSingleEnergy();
        harmonicSingleEnergy.setEigenvectors(eigenvectors);
        harmonicSingleEnergy.setOmegaSquared(omegaSquared);
        harmonicSingleEnergy.setWaveVectors(q);
        harmonicSingleEnergy.setPhase(sim.phase);
        harmonicSingleEnergy.setTemperature(1.0);
        AccumulatorAverage harmonicSingleAvg = new AccumulatorAverage(sim);
        pump = new DataPump(harmonicSingleEnergy, harmonicSingleAvg);
        adapter = new IntervalActionAdapter(pump);
        adapter.setActionInterval(2);
        sim.integrator.addListener(adapter);
        DataProcessorFoo fooer = new DataProcessorFoo();
        harmonicSingleAvg.addDataSink(fooer, new StatType[]{StatType.AVERAGE});

        if(graphic){
            SimulationGraphic simG = new SimulationGraphic(sim);
            
            DisplayBoxesCAE harmonicBoxes = new DisplayBoxesCAE();
            harmonicBoxes.setAccumulator(harmonicAvg);
            simG.add(harmonicBoxes);

            DataHistogram harmonicSingleHistogram = new DataHistogram(new HistogramSimple.Factory(20, new DoubleRange(0, 1)));
            harmonicSingleAvg.addDataSink(harmonicSingleHistogram, new StatType[]{StatType.AVERAGE});
            DisplayPlot harmonicPlot = new DisplayPlot();
            harmonicPlot.setDoLegend(false);
            harmonicSingleHistogram.setDataSink(harmonicPlot.getDataSet().makeDataSink());
            simG.add(harmonicPlot);
            
            DisplayBox diffA = new DisplayBox();
            fooer.setDataSink(diffA);
            simG.add(diffA);
            
            simG.makeAndDisplayFrame();
        } else {
            double simTime = 400.0;
            int nSteps = (int) (simTime / sim.integrator.getTimeStep());

            sim.activityIntegrate.setMaxSteps(nSteps);
            
            sim.getController().actionPerformed();

            double avgHarmonicEnergy = ((DataDouble)((DataGroup)harmonicAvg.getData()).getData(AccumulatorAverage.StatType.AVERAGE.index)).x;
            double errorHarmonicEnergy = ((DataDouble)((DataGroup)harmonicAvg.getData()).getData(AccumulatorAverage.StatType.ERROR.index)).x;
            System.out.println("avg harmonic energy: "+avgHarmonicEnergy+" +/- "+errorHarmonicEnergy);
        }
    }

    private static final long serialVersionUID = 1L;
    public IntegratorMD integrator;
    public ActivityIntegrate activityIntegrate;
    public Phase phase;
    public BoundaryRectangularPeriodic bdry;
    public LatticeCubicFcc lattice;
    public PrimitiveFcc primitive;

    /**
     * DataProcessor that sums up the logs of all incoming values
     */
    public static class DataProcessorFoo extends DataProcessor {
        public DataProcessor getDataCaster(DataInfo incomingDataInfo) {
            return null;
        }
        
        public DataInfo processDataInfo(DataInfo incomingDataInfo) {
            dataInfo = new DataInfoDouble("free energy difference", Null.DIMENSION);
            data = new DataDouble();
            return dataInfo;
        }
            
        
        public Data processData(Data incomingData) {
            data.x = 0;
            int nData = ((DataArithmetic)incomingData).getLength();
            for (int i=0; i<nData; i++) {
                data.x += Math.log(((DataArithmetic)incomingData).getValue(i));
            }
            return data;
        }
        
        private DataDouble data;
    }
}