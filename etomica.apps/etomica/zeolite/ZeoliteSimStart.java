package etomica.zeolite;

import etomica.action.IntegratorActionAdapter;
import etomica.atom.iterator.AtomIteratorMolecule;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.meter.MeterEnergy;
import etomica.graphics.DisplayPlot;
import etomica.integrator.IntegratorMD;
import etomica.integrator.IntegratorBox;
import etomica.api.ISpecies;
import etomica.util.HistoryCollapsing;

public class ZeoliteSimStart extends IntegratorActionAdapter{

    public ZeoliteSimStart(ZeoliteSimulation sim,zeoliteSimGraphic graphic) {
        this(sim.integrator,false,sim.getInterval());
        this.sim = sim;
        this.graphic = graphic;
	}
    
    public ZeoliteSimStart(IntegratorMD integrator, boolean ignoreOverlap,int interval) {
        super(integrator);
        this.ignoreOverlap = ignoreOverlap;
        this.interval = interval;
        
    }

    public void actionPerformed() {
        if(integrator != null) {
        	//Turn off BoxDisplay
        	//graphic.remove(graphic.display);
        	
        	
        	//Set Max Steps Based on SimulationParameters.xls
        	sim.activityIntegrate.setMaxSteps(3270000/2);
        	sim.activityIntegrate.setSleepPeriod(0);
        	((IntegratorMD)integrator).setThermostatInterval(327000);
        	//Keeping another graphic of the total energy drift
        	MeterEnergy eMeter = new MeterEnergy(((IntegratorBox)integrator).getPotential(), sim.box);
        	AccumulatorHistory energyHistory = new AccumulatorHistory(new HistoryCollapsing());
        	energyHistory.getHistory().setHistoryLength(sim.getInterval()*500);
        	//AccumulatorAverage enAcc = new AccumulatorAverage(sim);
        	//enAcc.setPushInterval(20);
        	//DataFork enFork = new DataFork(new DataSink[]{energyHistory, enAcc});
        	DataPump energyPump = new DataPump(eMeter, energyHistory);
            sim.integrator.addIntervalAction(energyPump);
            sim.integrator.setActionInterval(energyPump, 10);
        	energyHistory.setPushInterval(50);
            //XXX BORK!  This class needs to be rewritten
        	//sim.register(eMeter,energyPump);
        	
        	DisplayPlot hisPlot = new DisplayPlot();
        	hisPlot.setLabel("Energy");
        	energyHistory.setDataSink(hisPlot.getDataSet().makeDataSink());
        	hisPlot.setDoLegend(true);
        	graphic.add(hisPlot);
        	
        	interval = 327;
        		
        	
        	filename = sim.getFileName();
        	sp = sim.getSpeciesRMS();
        	System.out.println(filename);
        	//sim.integrator.setTimeStep(0.00);
        	MSDCoordWriter coordWriter = new MSDCoordWriter(sim.getSpace(), filename);
        	coordWriter.setBox(sim.box);
            coordWriter.setIterator(new AtomIteratorMolecule(sp[0]));
            coordWriter.setIntegrator(sim.integrator);
            coordWriter.setWriteInterval(interval);
            coordWriter.openFile();
            System.out.println("created MSDCoordWriter");
        	
        }
    }

    /**
     * @return Returns the ignoreOverlap.
     */
    public boolean isIgnoreOverlap() {
        return ignoreOverlap;
    }

    /**
     * @param ignoreOverlap The ignoreOverlap to set.
     */
    public void setIgnoreOverlap(boolean ignoreOverlap) {
        this.ignoreOverlap = ignoreOverlap;
    }

    private static final long serialVersionUID = 1L;
    private boolean ignoreOverlap;
    private int interval;
    private ZeoliteSimulation sim;
    private String filename;
    private ISpecies[] sp;
    private zeoliteSimGraphic graphic;
}
