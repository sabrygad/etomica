package etomica.modules.interfacial;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.ArrayList;

import javax.swing.JPanel;

import etomica.api.IAction;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.config.ConfigurationLattice;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorHistory;
import etomica.data.Data;
import etomica.data.DataFork;
import etomica.data.DataGroupSplitter;
import etomica.data.DataPipe;
import etomica.data.DataProcessor;
import etomica.data.DataProcessorInterfacialTension;
import etomica.data.DataPump;
import etomica.data.DataSink;
import etomica.data.DataSourceCountTime;
import etomica.data.DataSplitter;
import etomica.data.DataTag;
import etomica.data.IDataInfo;
import etomica.data.meter.MeterDensity;
import etomica.data.meter.MeterNMolecules;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.meter.MeterProfileByVolume;
import etomica.data.meter.MeterTemperature;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataTensor;
import etomica.exception.ConfigurationOverlapException;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceButton;
import etomica.graphics.DeviceNSelector;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceThermoSlider;
import etomica.graphics.DisplayPlot;
import etomica.graphics.DisplayTextBox;
import etomica.graphics.DisplayTextBoxesCAE;
import etomica.graphics.DisplayTimer;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.modifier.Modifier;
import etomica.modules.interfacial.DataSourceTensorVirialHardProfile.DataSourceVirialProfile;
import etomica.nbr.list.PotentialMasterList;
import etomica.space.Space;
import etomica.space2d.Space2D;
import etomica.space3d.Space3D;
import etomica.units.Dimension;
import etomica.units.Energy;
import etomica.units.Length;
import etomica.units.Null;
import etomica.units.Pixel;
import etomica.units.Unit;
import etomica.units.systems.LJ;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.Constants.CompassDirection;

/**
 * Graphic UI for interfacial tension module.  Design by Heath Turner.
 *
 * @author Andrew Schultz
 */
public class InterfacialSWGraphic extends SimulationGraphic {

    private final static String APP_NAME = "Interfacial Tension";
    private final static int REPAINT_INTERVAL = 20;
    private DeviceThermoSlider temperatureSelect;
    protected InterfacialSW sim;
    protected final DeviceNSelector nSlider;
    protected final DeviceSlider xSlider, yzSlider;
    protected final MeterProfileByVolume densityProfileMeter;
    protected boolean isExpanded;
    
    public InterfacialSWGraphic(final InterfacialSW simulation, Space _space) {

    	super(simulation, TABBED_PANE, APP_NAME, _space.D() == 2 ? 10*REPAINT_INTERVAL : REPAINT_INTERVAL, _space);

        ArrayList dataStreamPumps = getController().getDataStreamPumps();

    	this.sim = simulation;

        LJ unitSystem = new LJ();
        Unit tUnit = Energy.DIMENSION.getUnit(unitSystem);

        final double expansionFac = 3;
        isExpanded = false;

        final DeviceButton expandButton = new DeviceButton(sim.getController());
        IAction expandAction = new IAction() {
            public void actionPerformed() {
                IVector dim = sim.box.getBoundary().getDimensions();
                dim.setX(0, expansionFac * dim.x(0));
                sim.box.getBoundary().setDimensions(dim);
                ((PotentialMasterList)sim.integrator.getPotential()).getNeighborManager(sim.box).reset();
                try {
                    sim.integrator.reset();
                }
                catch (ConfigurationOverlapException e) {
                    throw new RuntimeException(e);
                }
                nSlider.setEnabled(false);
                expandButton.getButton().setEnabled(false);
                getDisplayBox(sim.box).repaint();
                densityProfileMeter.reset();
                isExpanded = true;
            }
        };
        expandButton.setAction(expandAction);
        expandButton.setLabel("Expand");
        add(expandButton);

        // the reinitialize's preAction halts the integrator 
        final IAction oldPreAction = getController().getReinitButton().getPreAction();
        getController().getReinitButton().setPreAction(new IAction() {
            public void actionPerformed() {
                oldPreAction.actionPerformed();
                if (!isExpanded) return;
                IVector dim = sim.box.getBoundary().getDimensions();
                dim.setX(0, dim.x(0) / expansionFac);
                sim.box.getBoundary().setDimensions(dim);
                nSlider.setEnabled(true);
                expandButton.getButton().setEnabled(true);
                isExpanded = false;
            }
        });
        
        IAction recenterAction = new IAction() {
            public void actionPerformed() {
                double dx = 0.5;
                int leftN1 = 0, leftP1 = 0;
                int left0 = 0;
                IAtomSet leafAtoms = sim.box.getLeafList();
                int nTot = leafAtoms.getAtomCount();
                for (int i=0; i<nTot; i++) {
                    IVector pos = ((IAtomPositioned)leafAtoms.getAtom(i)).getPosition();
                    if (pos.x(0) < 0) {
                        left0++;
                    }
                    if (pos.x(0) < -dx) {
                        leftN1++;
                        leftP1++;
                    }
                    else if (pos.x(0) < dx) {
                        leftP1++;
                    }
                }
                double target = 0.5 * nTot; 
                // interpolate to find median
                double median = -dx + (target - leftN1) * (2*dx) / (leftP1 - leftN1);
                // make sure we didn't extrapolate
                if (median < -dx) {
                    median = -dx;
                }
                else if (median > dx) {
                    median = dx;
                }

                int newLeft0 = 0;
                for (int i=0; i<nTot; i++) {
                    IVector pos = ((IAtomPositioned)leafAtoms.getAtom(i)).getPosition();
                    pos.setX(0, pos.x(0) - median);
                    if (pos.x(0) < 0) {
                        newLeft0++;
                    }
                }
//                System.out.println((left0-target)+" "+(newLeft0-target));
                ((PotentialMasterList)sim.integrator.getPotential()).getNeighborManager(sim.box).reset();
                try {
                    sim.integrator.reset();
                }
                catch (ConfigurationOverlapException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        sim.integrator.addIntervalAction(recenterAction);
        sim.integrator.setActionInterval(recenterAction, 1000);

	    //display of box, timer
        ColorSchemeByType colorScheme = new ColorSchemeByType(sim);
        colorScheme.setColor(sim.species.getLeafType(),java.awt.Color.red);
        getDisplayBox(sim.box).setColorScheme(new ColorSchemeByType(sim));
//        sim.integrator.addListener(new IntervalActionAdapter(this.getDisplayBoxPaintAction(sim.box)));

        DataSourceCountTime timeCounter = new DataSourceCountTime(sim.integrator);
        DisplayTimer displayTimer = new DisplayTimer(sim.integrator);
        add(displayTimer);

        //add meter and display for current kinetic temperature

		MeterTemperature thermometer = new MeterTemperature(sim.box, space.D());
        DataFork temperatureFork = new DataFork();
        final DataPump temperaturePump = new DataPump(thermometer,temperatureFork);
        sim.integrator.addIntervalAction(temperaturePump);
        sim.integrator.setActionInterval(temperaturePump, 20);
        final AccumulatorHistory temperatureHistory = new AccumulatorHistory();
        temperatureHistory.setTimeDataSource(timeCounter);
		final DisplayTextBox tBox = new DisplayTextBox();
		temperatureFork.setDataSinks(new DataSink[]{tBox,temperatureHistory});
        tBox.setUnit(tUnit);
		tBox.setLabel("Measured Temperature");
		tBox.setLabelPosition(CompassDirection.NORTH);

		dataStreamPumps.add(temperaturePump);
        tBox.setUnit(tUnit);
		tBox.setLabel("Measured Temperature");
		tBox.setLabelPosition(CompassDirection.NORTH);

		// Number density box
	    MeterDensity densityMeter = new MeterDensity(sim.getSpace());
        densityMeter.setBox(sim.box);
	    final DisplayTextBox densityBox = new DisplayTextBox();
        final DataPump densityPump = new DataPump(densityMeter, densityBox);
        sim.integrator.addIntervalAction(densityPump);
        sim.integrator.setActionInterval(densityPump, 10);
        dataStreamPumps.add(densityPump);
	    densityBox.setLabel("Number Density");
	    
//	      MeterEnergy eMeter = new MeterEnergy(sim.integrator.getPotential(), sim.box);
//        AccumulatorHistory energyHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
//        energyHistory.setTimeDataSource(timeCounter);
//        DataPump energyPump = new DataPump(eMeter, energyHistory);
//        sim.integrator.addIntervalAction(energyPump);
//        sim.integrator.setActionInterval(energyPump, 60);
//        energyHistory.setPushInterval(5);
//        dataStreamPumps.add(energyPump);
		
		MeterPotentialEnergyFromIntegrator peMeter = new MeterPotentialEnergyFromIntegrator(sim.integrator);
        AccumulatorHistory peHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
        peHistory.setTimeDataSource(timeCounter);
        final AccumulatorAverageCollapsing peAccumulator = new AccumulatorAverageCollapsing();
        peAccumulator.setPushInterval(10);
        DataFork peFork = new DataFork(new DataSink[]{peHistory, peAccumulator});
        DataPump pePump = new DataPump(peMeter, peFork);
        sim.integrator.addIntervalAction(pePump);
        sim.integrator.setActionInterval(pePump, 10);
        peHistory.setPushInterval(1);
        dataStreamPumps.add(pePump);
		
        DisplayPlot ePlot = new DisplayPlot();
        peHistory.setDataSink(ePlot.getDataSet().makeDataSink());

        ePlot.getPlot().setTitle("Energy History");
		ePlot.setDoLegend(true);
		ePlot.setLabel("Energy");
		
        DataSourceTensorVirialHardProfile pMeter = new DataSourceTensorVirialHardProfile(space);
        pMeter.setIntegrator(sim.integrator);
        DataProcessorTensorSplitter tensorSplitter = new DataProcessorTensorSplitter();
        final DataPump pPump = new DataPump(pMeter, tensorSplitter);
        DataFork virialFork = new DataFork();
        tensorSplitter.setDataSink(virialFork);
        final DataSplitter splitter = new DataSplitter();
        virialFork.addDataSink(splitter);
        final AccumulatorAverageCollapsing[] pAccumulator = new AccumulatorAverageCollapsing[space.D()];
        final DisplayTextBoxesCAE[] pDisplay = new DisplayTextBoxesCAE[space.D()];
        String[] comp = new String[]{"x", "y", "z"};
        for (int i=0; i<pAccumulator.length; i++) {
            pAccumulator[i] = new AccumulatorAverageCollapsing();
            splitter.setDataSink(i, pAccumulator[i]);
            pAccumulator[i].setPushInterval(10);
            pDisplay[i] = new DisplayTextBoxesCAE();
            pDisplay[i].setLabel(comp[i]+" Virial");
            pDisplay[i].setAccumulator(pAccumulator[i]);
        }
        sim.integrator.addIntervalAction(pPump);
        sim.integrator.setActionInterval(pPump, 20);
        dataStreamPumps.add(pPump);
        
        DataProcessorInterfacialTension interfacialTension = new DataProcessorInterfacialTension(space);
        interfacialTension.setBox(sim.box);
        virialFork.addDataSink(interfacialTension);
        final AccumulatorAverageCollapsing tensionAvg = new AccumulatorAverageCollapsing();
        interfacialTension.setDataSink(tensionAvg);
        tensionAvg.setPushInterval(10);
        DisplayTextBoxesCAE tensionDisplay = new DisplayTextBoxesCAE();
        tensionDisplay.setAccumulator(tensionAvg);

        DataSourceVirialProfile virialProfileMeter = new DataSourceVirialProfile(pMeter);
        DataFork virialProfileFork = new DataFork();
        DataPump virialProfilePump = new DataPump(virialProfileMeter, virialProfileFork);
        DataGroupSplitter virialSplitter = new DataGroupSplitter();
        virialProfileFork.addDataSink(virialSplitter);
        sim.integrator.addIntervalAction(virialProfilePump);
        sim.integrator.setActionInterval(virialProfilePump, 20);
        AccumulatorAverageFixed[] virialProfileAvg = new AccumulatorAverageFixed[space.D()];
        DisplayPlot virialPlot = new DisplayPlot();
        for (int i=0; i<space.D(); i++) {
            virialProfileAvg[i] = new AccumulatorAverageFixed(10);
            virialProfileAvg[i].setPushInterval(10);
            virialSplitter.setDataSink(i, virialProfileAvg[i]);
            virialProfileAvg[i].addDataSink(virialPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{AccumulatorAverage.StatType.AVERAGE});
            virialPlot.setLegend(new DataTag[]{virialProfileAvg[i].getTag()}, comp[i]+" Virial");
        }
        virialPlot.setLabel("Virial Profile");
        add(virialPlot);
        dataStreamPumps.add(virialProfilePump);

        DataProcessorInterfacialTensionProfile interfacialTensionProfile = new DataProcessorInterfacialTensionProfile(space);
        interfacialTensionProfile.setBox(sim.box);
        virialProfileFork.addDataSink(interfacialTensionProfile);
        AccumulatorAverageFixed tensionProfileAvg = new AccumulatorAverageFixed(10);
        interfacialTensionProfile.setDataSink(tensionProfileAvg);
        tensionProfileAvg.setPushInterval(10);
        DisplayPlot tensionPlot = new DisplayPlot();
        tensionProfileAvg.addDataSink(tensionPlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{AccumulatorAverage.StatType.AVERAGE});
        tensionPlot.setLabel("Tension Profile");
        add(tensionPlot);

        densityProfileMeter = new MeterProfileByVolume(space);
        densityProfileMeter.setBox(sim.box);
        densityProfileMeter.setDataSource(new MeterNMolecules());
        AccumulatorAverageFixed densityProfileAvg = new AccumulatorAverageFixed(10);
        densityProfileAvg.setPushInterval(10);
        DataPump profilePump = new DataPump(densityProfileMeter, densityProfileAvg);
        sim.integrator.addIntervalAction(profilePump);
        sim.integrator.setActionInterval(profilePump, 10);
        dataStreamPumps.add(profilePump);
        
        final FitTanh fitTanh = new FitTanh();
        densityProfileAvg.addDataSink(fitTanh, new AccumulatorAverage.StatType[]{AccumulatorAverage.StatType.AVERAGE});

        DisplayPlot profilePlot = new DisplayPlot();
        densityProfileAvg.addDataSink(profilePlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{AccumulatorAverage.StatType.AVERAGE});
        fitTanh.setDataSink(profilePlot.getDataSet().makeDataSink());
        profilePlot.setLegend(new DataTag[]{densityProfileAvg.getTag()}, "density");
        profilePlot.setLegend(new DataTag[]{densityProfileAvg.getTag(), fitTanh.getTag()}, "fit");
        profilePlot.setDoLegend(true);
        profilePlot.setLabel("Density");
        GridBagConstraints vertGBC = SimulationPanel.getVertGBC();
        JPanel profilePanel = new JPanel(new GridBagLayout());
        getPanel().tabbedPane.add("Density", profilePanel);
        profilePanel.add(profilePlot.graphic(), vertGBC);
        final DisplayTextBox vaporDensityBox = new DisplayTextBox("Vapor density", Null.UNIT);
        final DisplayTextBox liquidDensityBox = new DisplayTextBox("Vapor density", Null.UNIT);
        final DisplayTextBox interfaceWidthBox = new DisplayTextBox("Interface width", Null.UNIT);
        final DisplayTextBox interfaceLocationBox = new DisplayTextBox("Interface location", Null.UNIT);
        JPanel fitParamsPanel = new JPanel();
        fitParamsPanel.add(vaporDensityBox.graphic());
        fitParamsPanel.add(liquidDensityBox.graphic());
        fitParamsPanel.add(interfaceWidthBox.graphic());
        fitParamsPanel.add(interfaceLocationBox.graphic());
        profilePanel.add(fitParamsPanel, vertGBC);
        IAction pullParams = new IAction() {
            public void actionPerformed() {
                double[] params = fitTanh.getLastBestParam();
                data.x = params[0];
                vaporDensityBox.putData(data);
                data.x = params[1];
                liquidDensityBox.putData(data);
                data.x = params[2];
                interfaceWidthBox.putData(data);
                data.x = params[3];
                interfaceLocationBox.putData(data);
            }
            DataDouble data = new DataDouble();
        };
        sim.integrator.addIntervalAction(pullParams);
        sim.integrator.setActionInterval(pullParams, 10);

        final DisplayTextBoxesCAE peDisplay = new DisplayTextBoxesCAE();
        peDisplay.setAccumulator(peAccumulator);

        nSlider = new DeviceNSelector(sim.getController());
        nSlider.setSpecies(sim.species);
        nSlider.setBox(sim.box);
        nSlider.setMinimum(0);
        nSlider.setMaximum(space.D() == 3 ? 2048 : 500);
        nSlider.setLabel("Number of Atoms");
        nSlider.setShowBorder(true);
        nSlider.setShowValues(true);
        // add a listener to adjust the thermostat interval for different
        // system sizes (since we're using ANDERSEN_SINGLE.  Smaller systems 
        // don't need as much thermostating.
        final ConfigurationLattice config = new ConfigurationLattice((space.D() == 2) ? new LatticeOrthorhombicHexagonal() : new LatticeCubicFcc(), space);
        nSlider.setPostAction(new IAction() {
            public void actionPerformed() {
                int n = (int)nSlider.getValue();
                if(n == 0) {
                	sim.integrator.setThermostatInterval(400);
                }
                else {
                    sim.integrator.setThermostatInterval((400+(n-1))/n);
                }
                
                if (oldN < n) {
                    config.initializeCoordinates(sim.box);
                }
                oldN = n;
                ((PotentialMasterList)sim.integrator.getPotential()).getNeighborManager(sim.box).reset();
                try {
                    sim.integrator.reset();
                }
                catch (ConfigurationOverlapException e) {
                    throw new RuntimeException(e);
                }
                getController().getSimRestart().actionPerformed();
                getDisplayBox(sim.box).repaint();
            }
            
            int oldN = sim.box.getMoleculeList().getAtomCount();
        });
        IAction reconfig = new IAction() {
            public void actionPerformed() {
                config.initializeCoordinates(sim.box);
                ((PotentialMasterList)sim.integrator.getPotential()).getNeighborManager(sim.box).reset();
                try {
                    sim.integrator.reset();
                }
                catch (ConfigurationOverlapException e) {
                    throw new RuntimeException(e);
                }
                getController().getSimRestart().actionPerformed();
                getDisplayBox(sim.box).repaint();
                densityProfileMeter.reset();
            }
        };
        xSlider = new DeviceSlider(sim.getController());
        xSlider.setMaximum(30);
        xSlider.setModifier(new ModifierBoxSize(sim.box, 0, reconfig));
        yzSlider = new DeviceSlider(sim.getController());
        yzSlider.setMaximum(15);
        yzSlider.setModifier(new ModifierBoxSize(sim.box, 1, reconfig));
        add(xSlider);
        add(yzSlider);
        
        //************* Lay out components ****************//


        getDisplayBox(sim.box).setScale(0.7);

        //temperature selector
        temperatureSelect = new DeviceThermoSlider(sim.getController());
        temperatureSelect.setPrecision(2);
        temperatureSelect.setMinimum(0.0);
        if (space.D() == 3) {
            // Tc = 1.312
            temperatureSelect.setMaximum(1.5);
        }
        else {
            // Tc = 0.515
            temperatureSelect.setMaximum(0.6);
        }
        temperatureSelect.setSliderMajorValues(3);
	    temperatureSelect.setUnit(tUnit);
	    temperatureSelect.setIntegrator(sim.integrator);
	    temperatureSelect.setIsothermalButtonsVisibility(false);

        IAction resetAction = new IAction() {
        	public void actionPerformed() {

                // Reset density (Density is set and won't change, but
        		// do this anyway)
        		densityPump.actionPerformed();
        		densityBox.repaint();

        		// Reset temperature (THIS IS NOT WORKING)
                temperaturePump.actionPerformed();
//                tBox.putData(temperatureHistory.getData());
                tBox.repaint();

                // IS THIS WORKING?
                pPump.actionPerformed();
                for (int i=0; i<space.D(); i++) {
                    pDisplay[i].putData(pAccumulator[i].getData());
                    pDisplay[i].repaint();
                }
                peDisplay.putData(peAccumulator.getData());
                peDisplay.repaint();

        		getDisplayBox(sim.box).graphic().repaint();
        	}
        };

        this.getController().getReinitButton().setPostAction(resetAction);
        this.getController().getResetAveragesButton().setPostAction(resetAction);

        getPanel().controlPanel.add(temperatureSelect.graphic(), vertGBC);
        add(nSlider);

    	add(ePlot);
    	add(densityBox);
    	add(tBox);
    	for (int i=0; i<space.D(); i++) {
    	    add(pDisplay[i]);
    	}
    	add(peDisplay);
        add(tensionDisplay);

    }

    public static void main(String[] args) {
        Space sp = null;
        if(args.length != 0) {
            try {
                int D = Integer.parseInt(args[0]);
                if (D == 3) {
                    sp = Space3D.getInstance();
                }
                else {
                	sp = Space2D.getInstance();
                }
            } catch(NumberFormatException e) {}
        }
        else {
        	sp = Space3D.getInstance();
        }

        InterfacialSW sim = new InterfacialSW(sp);
        InterfacialSWGraphic swGraphic = new InterfacialSWGraphic(sim, sp);
        swGraphic.getDisplayBox(sim.box).setPixelUnit(new Pixel(9));
		SimulationGraphic.makeAndDisplayFrame
		        (swGraphic.getPanel(), APP_NAME);
    }
    
    public static class Applet extends javax.swing.JApplet {

        public void init() {
	        getRootPane().putClientProperty(
	                        "defeatSystemEventQueueCheck", Boolean.TRUE);
	        Space sp = Space3D.getInstance();
	        InterfacialSW sim = new InterfacialSW(sp);
            InterfacialSWGraphic ljmdGraphic = new InterfacialSWGraphic(sim, sp);
            ljmdGraphic.getDisplayBox(sim.box).setPixelUnit(new Pixel(15));

		    getContentPane().add(ljmdGraphic.getPanel());
	    }

        private static final long serialVersionUID = 1L;
    }
    
    /**
     * Inner class to find the total pressure of the system from the pressure
     * tensor.
     */
    public static class DataProcessorTensorSplitter extends DataProcessor {

        public DataProcessorTensorSplitter() {
            data = new DataDoubleArray(3);
        }
        
        protected Data processData(Data inputData) {
            double[] x = data.getData();
            for (int i=0; i<x.length; i++) {
                x[i] = ((DataTensor)inputData).x.component(i,i);
            }
            return data;
        }

        protected IDataInfo processDataInfo(IDataInfo inputDataInfo) {
            dataInfo = new DataDoubleArray.DataInfoDoubleArray(inputDataInfo.getLabel(), inputDataInfo.getDimension(), new int[]{inputDataInfo.getLength()});
            return dataInfo;
        }

        public DataPipe getDataCaster(IDataInfo inputDataInfo) {
            if (!(inputDataInfo instanceof DataTensor.DataInfoTensor)) {
                throw new IllegalArgumentException("Gotta be a DataInfoTensor");
            }
            return null;
        }

        private static final long serialVersionUID = 1L;
        protected final DataDoubleArray data;
    }
    
    public static class ModifierBoxSize implements Modifier {
        public ModifierBoxSize(IBox box, int dim, IAction reconfig) {
            this.box = box;
            this.dim = dim;
            this.reconfig = reconfig;
        }
        
        public Dimension getDimension() {
            return Length.DIMENSION;
        }

        public String getLabel() {
            return "Box size";
        }

        public double getValue() {
            return box.getBoundary().getDimensions().x(dim);
        }

        public void setValue(double newValue) {
            if (newValue <= 0) {
                throw new IllegalArgumentException("Gotta be positive");
            }
            IVector sizeNow = box.getBoundary().getDimensions();
            double oldValue = sizeNow.x(dim);
            sizeNow.setX(dim, newValue);
            if (dim == 1 && sizeNow.getD() == 3) {
                sizeNow.setX(2, newValue);
            }
            box.getBoundary().setDimensions(sizeNow);
            try {
                reconfig.actionPerformed();
            }
            catch (RuntimeException e) {
                // box is too small.  restore to original size
                sizeNow.setX(dim, oldValue);
                // and reconfig.  this shouldn't throw.
                reconfig.actionPerformed();
            }
        }
        
        protected final IBox box;
        protected final int dim;
        protected final IAction reconfig;
    }
}

