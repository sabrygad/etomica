package etomica.modules.chainequilibrium;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;

import etomica.action.SimulationRestart;
import etomica.api.IAction;
import etomica.api.ISpecies;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorHistory;
import etomica.data.DataFork;
import etomica.data.DataPump;
import etomica.data.DataSinkTable;
import etomica.data.DataSourceCountTime;
import etomica.data.DataTag;
import etomica.data.meter.MeterTemperature;
import etomica.graphics.DeviceBox;
import etomica.graphics.DeviceDelaySlider;
import etomica.graphics.DeviceNSelector;
import etomica.graphics.DeviceSlider;
import etomica.graphics.DeviceThermoSlider;
import etomica.graphics.DisplayPlot;
import etomica.graphics.DisplayTable;
import etomica.graphics.DisplayTextBox;
import etomica.graphics.DisplayTextBoxesCAE;
import etomica.graphics.DisplayTimer;
import etomica.graphics.SimulationGraphic;
import etomica.graphics.SimulationPanel;
import etomica.modifier.Modifier;
import etomica.modifier.ModifierGeneral;
import etomica.modifier.ModifierNMolecule;
import etomica.space.ISpace;
import etomica.units.Dimension;
import etomica.units.Joule;
import etomica.units.Kelvin;
import etomica.units.Mole;
import etomica.units.Pixel;
import etomica.units.Prefix;
import etomica.units.PrefixedUnit;
import etomica.units.Quantity;
import etomica.units.UnitRatio;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.Constants.CompassDirection;

/**
 * Module for chain reaction (polymerization) using ChainEquilibriumSim as the
 * simulation class.  Original module by William Scharmach and Matt Moynihan.
 * Later revamped based on module redesign by William M. Chirdon.
 * 
 * @author William Scharmach
 * @author Matt Moynihan
 * @author Andrew Schultz
 */
public class FreeRadicalPolymerizationGraphic extends SimulationGraphic {

	private static final String APP_NAME = "Free Radical Polymerization";
	private static final int REPAINT_INTERVAL = 2;

    protected final FreeRadicalPolymerizationSim sim;

    public FreeRadicalPolymerizationGraphic(FreeRadicalPolymerizationSim simulation, ISpace _space) {

		super(simulation, TABBED_PANE, APP_NAME, REPAINT_INTERVAL, _space);
        this.sim = simulation;
        
        int dataInterval = (int) (.04 / sim.integratorHard.getTimeStep());
        
        ArrayList<DataPump> dataStreamPumps = getController().getDataStreamPumps();
        
        getDisplayBox(sim.box).setPixelUnit(new Pixel(10));

        GridBagConstraints vertGBC = SimulationPanel.getVertGBC();

        // ********* Data Declaration Section *******	
        int eMin = 0, eMax = 40;

        // **** Stuff that Modifies the Simulation

        final IAction resetAction = getController().getSimRestart().getDataResetAction();
        
        DeviceDelaySlider delaySlider = new DeviceDelaySlider(sim.controller1, sim.activityIntegrate);

        // Sliders on Well depth page
        final DeviceSlider AASlider = sliders(eMin, eMax/2, "initiator-initiatior", sim.p2AA);
        final DeviceSlider BSlider = sliders(eMin, eMax, "radical reaction", new P2SquareWellRadical[]{sim.p2AB,sim.p2BB});
        AASlider.setPostAction(resetAction);
        BSlider.setPostAction(resetAction);
        
        DeviceBox solventThermoFrac = new DeviceBox();
        solventThermoFrac.setController(sim.getController());
        solventThermoFrac.setModifier(new ModifierGeneral(new P2SquareWellBonded[]{sim.p2AA}, "solventThermoFrac"));
        solventThermoFrac.setLabel("fraction heat transfer to solvent");
        DisplayTextBox tBox = new DisplayTextBox();

        DisplayTimer displayTimer = new DisplayTimer(sim.integratorHard);
        add(displayTimer);
        
        DataSourceCountTime timer = new DataSourceCountTime(sim.integratorHard);

        DataFork tFork = new DataFork();
        final DataPump tPump = new DataPump (new MeterTemperature(sim, sim.box, space.D()), tFork);
        tFork.addDataSink(tBox);
        add(tBox);
        dataStreamPumps.add(tPump);
        AccumulatorHistory tHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
        tHistory.setTimeDataSource(timer);
        tFork.addDataSink(tHistory);
        DisplayPlot tPlot = new DisplayPlot();
        tHistory.addDataSink(tPlot.getDataSet().makeDataSink());
        tPlot.setUnit(Kelvin.UNIT);
        tPlot.setLabel("Temperature");
        tPlot.getPlot().setYLabel("Temperature (K)");
        tPlot.setDoLegend(false);
        add(tPlot);
        sim.integratorHard.addIntervalAction(tPump);
        sim.integratorHard.setActionInterval(tPump, dataInterval);

        final MeterChainLength molecularCount = new MeterChainLength(sim.agentManager);
        molecularCount.setIgnoredAtomType(sim.speciesA.getLeafType());
        molecularCount.setBox(sim.box);
        AccumulatorAverage accumulator = new AccumulatorAverageFixed(10);
        accumulator.setPushInterval(10);
        DataFork mwFork = new DataFork();
        final DataPump mwPump = new DataPump(molecularCount,mwFork);
        mwFork.addDataSink(accumulator);
        dataStreamPumps.add(mwPump);
        sim.integratorHard.addIntervalAction(mwPump);
        sim.integratorHard.setActionInterval(mwPump, dataInterval);
        
        MolecularWeightAvg molecularWeightAvg = new MolecularWeightAvg();
        mwFork.addDataSink(molecularWeightAvg);
        DataFork mwAvgFork = new DataFork();
        molecularWeightAvg.setDataSink(mwAvgFork);
        AccumulatorAverageCollapsing mwAvg = new AccumulatorAverageCollapsing();
        mwAvg.setPushInterval(1);
        mwAvgFork.addDataSink(mwAvg);
        final AccumulatorHistory mwHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
        mwHistory.setTimeDataSource(timer);
        mwAvgFork.addDataSink(mwHistory);

        MolecularWeightAvg2 molecularWeightAvg2 = new MolecularWeightAvg2();
        mwFork.addDataSink(molecularWeightAvg2);
        DataFork mwAvg2Fork = new DataFork();
        molecularWeightAvg2.setDataSink(mwAvg2Fork);
        AccumulatorAverageCollapsing mwAvg2 = new AccumulatorAverageCollapsing();
        mwAvg2.setPushInterval(1);
        mwAvg2Fork.addDataSink(mwAvg2);
        final AccumulatorHistory mw2History = new AccumulatorHistory(new HistoryCollapsingAverage());
        mw2History.setTimeDataSource(timer);
        mwAvg2Fork.addDataSink(mw2History);

        MeterConversion reactionConversion = new MeterConversion(sim.box, sim.agentManager);
        reactionConversion.setSpecies(new ISpecies[]{sim.speciesB});
        final HistoryCollapsingAverage conversionHistory = new HistoryCollapsingAverage();
        AccumulatorHistory conversionHistoryAcc = new AccumulatorHistory(conversionHistory);
        conversionHistoryAcc.setTimeDataSource(timer);
        final DataPump conversionPump = new DataPump(reactionConversion, conversionHistoryAcc);
        sim.integratorHard.addIntervalAction(conversionPump);
        sim.integratorHard.setActionInterval(conversionPump, dataInterval);
        dataStreamPumps.add(conversionPump);

        final IAction resetData = new IAction() {
            public void actionPerformed() {
                sim.integratorHard.resetTime();
                molecularCount.reset();
                conversionPump.actionPerformed();
                mwPump.actionPerformed();
                tPump.actionPerformed();
            }
        };
        getController().getResetAveragesButton().setLabel("Reset");
        getController().getResetAveragesButton().setPostAction(resetData);

        DataSinkTable dataTable = new DataSinkTable();
        accumulator.addDataSink(dataTable.makeDataSink(),new AccumulatorAverage.StatType[]{AccumulatorAverage.StatType.AVERAGE});
        DisplayTable THING = new DisplayTable(dataTable);
        THING.setTransposed(false);
        THING.setShowingRowLabels(false);
        THING.setPrecision(7);

        DisplayPlot compositionPlot = new DisplayPlot();
        accumulator.addDataSink(compositionPlot.getDataSet().makeDataSink(),new AccumulatorAverage.StatType[]{AccumulatorAverage.StatType.AVERAGE});
        compositionPlot.setDoLegend(false);

        DisplayPlot mwPlot = new DisplayPlot();
        mwPlot.setLabel("Degree of polymerization");
        mwHistory.addDataSink(mwPlot.getDataSet().makeDataSink());
        mwPlot.setLegend(new DataTag[]{mwHistory.getTag()}, "Number Avg");
        mw2History.addDataSink(mwPlot.getDataSet().makeDataSink());
        mwPlot.setLegend(new DataTag[]{mw2History.getTag()}, "Weight Avg");

        DisplayPlot conversionPlot = new DisplayPlot();
        conversionHistoryAcc.addDataSink(conversionPlot.getDataSet().makeDataSink());
        conversionPlot.setDoLegend(false);
//        conversionPlot.setLegend(new DataTag[]{reactionConversion.getTag()}, "reaction conversion");

        DisplayTextBoxesCAE mwBox = new DisplayTextBoxesCAE();
        mwBox.setAccumulator(mwAvg);
        mwBox.setLabel("Number avg degree of polymerization");
        add(mwBox);

        DisplayTextBoxesCAE mw2Box = new DisplayTextBoxesCAE();
        mw2Box.setAccumulator(mwAvg2);
        mw2Box.setLabel("Weight avg degree of polymerization");
        add(mw2Box);

        ((SimulationRestart)getController().getReinitButton().getAction()).setConfiguration(sim.config);
        getController().getReinitButton().setPostAction(new IAction() {
            public void actionPerformed() {
                sim.resetBonds();
                getDisplayBox(sim.box).repaint();
                resetData.actionPerformed();
            }
        });

        DeviceThermoSlider temperatureSelect = new DeviceThermoSlider(sim.controller1);
        temperatureSelect.setUnit(Kelvin.UNIT);
        temperatureSelect.setIntegrator(sim.integratorHard);
        temperatureSelect.setMaximum(1200);
        temperatureSelect.setIsothermal();
        temperatureSelect.setSliderPostAction(resetAction);
        temperatureSelect.addRadioGroupActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                resetAction.actionPerformed();
            }
        });
        
        ColorSchemeRadical colorScheme = new ColorSchemeRadical(sim, sim.agentManager);
        colorScheme.setFreeRadicalColor(sim.speciesA.getLeafType(), Color.GREEN);
//        colorScheme.setColor(sim.speciesA.getLeafType(), Color.BLUE);
        colorScheme.setColor(sim.speciesB.getLeafType(), Color.BLACK);
        colorScheme.setFreeRadicalColor(sim.speciesB.getLeafType(), Color.RED);
        colorScheme.setFullColor(sim.speciesA.getLeafType(), Color.BLUE);
        colorScheme.setFullColor(sim.speciesB.getLeafType(), Color.GRAY);
        getDisplayBox(sim.box).setColorScheme(colorScheme);
        
        DeviceSlider combinationProbabilitySlider = new DeviceSlider(sim.getController());
        combinationProbabilitySlider.setLabel("combination probability");
        combinationProbabilitySlider.setShowBorder(true);
        combinationProbabilitySlider.setModifier(new ModifierGeneral(new Object[]{sim.p2BB, sim.p2AB}, "combinationProbability"));
        combinationProbabilitySlider.setNMajor(5);
        combinationProbabilitySlider.setPrecision(1);
        combinationProbabilitySlider.setMinimum(0);
        combinationProbabilitySlider.setMaximum(1);

        DeviceNSelector nSliderA = new DeviceNSelector(sim.getController());
        nSliderA.setSpecies(sim.speciesA);
        nSliderA.setBox(sim.box);
        nSliderA.setPrecision(1);
        nSliderA.setModifier(new ModifierNMolecule(sim.box, sim.speciesA) {
            public void setValue(double newValue) {
                super.setValue(newValue*2);
            }
            public double getValue() {
                return 0.5*super.getValue();
            }
        });
        nSliderA.setShowBorder(true);
        nSliderA.setLabel("Initiator (blue)");
        nSliderA.setNMajor(4);
        IAction reset = new IAction() {
            public void actionPerformed() {
                getController().getSimRestart().actionPerformed();
                sim.resetBonds();
                getDisplayBox(sim.box).repaint();
                resetData.actionPerformed();
            }
        };
        nSliderA.setResetAction(reset);
        nSliderA.setMaximum(1000);
        nSliderA.setShowValues(true);
        nSliderA.setShowSlider(false);
        nSliderA.setEditValues(true);
        DeviceNSelector nSliderB = new DeviceNSelector(sim.getController());
        nSliderB.setSpecies(sim.speciesB);
        nSliderB.setBox(sim.box);
        nSliderB.setShowBorder(true);
        nSliderB.setLabel("Monomers (black)");
        nSliderB.setResetAction(reset);
        nSliderB.setNMajor(4);
        nSliderB.setMaximum(1000);
        nSliderB.setShowValues(true);
        nSliderB.setShowSlider(false);
        nSliderB.setEditValues(true);

        tBox.setUnit(Kelvin.UNIT);
        tBox.setLabel("Measured Temperature");
        tBox.setLabelPosition(CompassDirection.NORTH);

        compositionPlot.setLabel("Composition");
        conversionPlot.setLabel("Conversion");

        add(compositionPlot);
        add(mwPlot);
        JPanel conversionPanel = new JPanel(new GridBagLayout());
        conversionPanel.add(conversionPlot.graphic(), vertGBC);
        
        DeviceBox conversionHistoryLength = new DeviceBox();
        conversionHistoryLength.setInteger(true);
        conversionHistoryLength.setController(sim.getController());
        conversionHistoryLength.setModifier(new Modifier() {

            public Dimension getDimension() {
                return Quantity.DIMENSION;
            }

            public String getLabel() {
                return "history length";
            }

            public double getValue() {
                return conversionHistory.getHistoryLength();
            }

            public void setValue(double newValue) {
                conversionHistory.setHistoryLength((int)newValue);
            }
        });
        conversionPanel.add(conversionHistoryLength.graphic(),vertGBC);
        
        getPanel().tabbedPane.add("Conversion" , conversionPanel);

        JPanel speciesEditors = new JPanel(new java.awt.GridLayout(0, 1));
        JPanel epsilonSliders = new JPanel(new java.awt.GridBagLayout());

        speciesEditors.add(nSliderA.graphic());
        speciesEditors.add(nSliderB.graphic());

        epsilonSliders.add(AASlider.graphic(null), vertGBC);
        epsilonSliders.add(BSlider.graphic(null), vertGBC);
//        epsilonSliders.add(combinationProbabilitySlider.graphic(null));
        epsilonSliders.add(solventThermoFrac.graphic(), vertGBC);

        final JTabbedPane sliderPanel = new JTabbedPane();
        //panel for all the controls
        getPanel().controlPanel.add(delaySlider.graphic(), vertGBC);
        getPanel().controlPanel.add(temperatureSelect.graphic(), vertGBC);
        getPanel().controlPanel.add(sliderPanel, vertGBC);
        sliderPanel.add(epsilonSliders, "Reaction Energy (kJ/mol)");
        sliderPanel.add(speciesEditors, "Number of Molecules");
        getPanel().controlPanel.add(combinationProbabilitySlider.graphic(), vertGBC);

        //set the number of significant figures displayed on the table.
        javax.swing.table.DefaultTableCellRenderer numberRenderer = new javax.swing.table.DefaultTableCellRenderer() {
            java.text.NumberFormat formatter;
            {
                formatter = java.text.NumberFormat.getInstance();
                formatter.setMaximumFractionDigits(6);
            }

            public void setValue(Object value) {
                setText((value == null) ? "" : formatter.format(value));
            }
        };

        numberRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
    }

    public DeviceSlider sliders(int eMin, int eMax, String s, P2SquareWellBonded p){

        DeviceSlider AASlider = new DeviceSlider(sim.getController(), new ModifierGeneral(p, "epsilon"));
        AASlider.setUnit(new UnitRatio(new PrefixedUnit(Prefix.KILO, Joule.UNIT), Mole.UNIT));
        AASlider.doUpdate();
        AASlider.setShowBorder(true);
        AASlider.setLabel(s);
        AASlider.setMinimum(eMin);
        AASlider.setMaximum(eMax);
        AASlider.setNMajor(4);
        AASlider.getSlider().setSnapToTicks(true);

        return AASlider;
    }

    public DeviceSlider sliders(int eMin, int eMax, String s, P2SquareWellRadical[] potentials){

        DeviceSlider AASlider = new DeviceSlider(sim.getController(), new ModifierGeneral(potentials, "epsilon"));
        AASlider.setUnit(new UnitRatio(new PrefixedUnit(Prefix.KILO, Joule.UNIT), Mole.UNIT));
        AASlider.doUpdate();
        AASlider.setShowBorder(true);
        AASlider.setLabel(s);
        AASlider.setMinimum(eMin);
        AASlider.setMaximum(eMax);
        AASlider.setNMajor(4);
        AASlider.getSlider().setSnapToTicks(true);

        return AASlider;
    }

    public static void main(String[] args) {
        FreeRadicalPolymerizationSim sim = new FreeRadicalPolymerizationSim();
        FreeRadicalPolymerizationGraphic graphic = new FreeRadicalPolymerizationGraphic(sim, sim.getSpace());
        SimulationGraphic.makeAndDisplayFrame(graphic.getPanel(), APP_NAME);
    }

    public static class Applet extends javax.swing.JApplet {

        public void init() {
			getRootPane().putClientProperty("defeatSystemEventQueueCheck", Boolean.TRUE);
	        FreeRadicalPolymerizationSim sim = new FreeRadicalPolymerizationSim();
			getContentPane().add(new FreeRadicalPolymerizationGraphic(sim, sim.getSpace()).getPanel());
        }
    }
}