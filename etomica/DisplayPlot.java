package etomica;

import etomica.units.*;
import ptolemy.plot.Plot;
import java.awt.event.*;

/**
 * Class for creating a plot of simulation data.
 *
 * @author David Kofke
 */
 
public class DisplayPlot extends DisplayDataSources implements EtomicaElement {
    
    public String getVersion() {return "DisplayPlot:01.05.29/"+Display.VERSION;}

    private Plot plot;
    private javax.swing.JPanel panel = new javax.swing.JPanel();
   
    public DisplayPlot() {
        this(Simulation.instance);
    }
    public DisplayPlot(Simulation sim) {
        super(sim);
        plot = new Plot();
        panel.add(plot);
        setName("Data Plot");
//        new Thread(this).start();
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("X-Y graphical plot of data");
        return info;
    }
    
    /**
     * Accessor method to plot class so that its properties can be edited.
     */
    public final Plot getPlot() {return plot;}
        
    /**
     * Overrides superclass method to return a JPanel holding the plot.
     */
    public java.awt.Component graphic(Object obj) {return panel;}

    /**
     * Performs actions appropriate to addition or change of data source.
     * Implementation of abstract method from parent class.
     */
    public void setupDisplay() {
        panel.remove(plot);
        plot = new Plot();
        panel.add(plot);
        if(ySource.length > 1) {
            for(int i=0; i<ySource.length; i++) plot.addLegend(i,ySource[i].getLabel());
        }
        setLabel(ySource[0].getLabel());
        plot.setYLabel(ySource[0].getLabel());
        //change unit if dimension of new source is different from current source        
        if(yUnit.dimension() != ySource[0].getDimension()) 
            setYUnit(ySource[0].getDimension().defaultIOUnit());

        if(xSource == null) {
            setXUnit(Unit.NULL);
            plot.setXLabel("");
        }
        else {
            if(xUnit.dimension() != xSource.getDimension())
                setXUnit(xSource.getDimension().defaultIOUnit());
            plot.setXLabel(xSource.getLabel()+" ("+xUnit.symbol()+")");
        }
    }
            
    public void doUpdate() {
        super.doUpdate();
        if(ySource == null) return;
        int nSource = ySource.length;
        plot.clear(false);
        ///new stuff
        for(int k=0; k<nSource; k++) {
            for(int i=0; i<y.length; i++) {
              plot.addPoint(k, xUnit.fromSim(x[i]), yUnit.fromSim(y[k][i]), true);
            }
        }
        plot.repaint();
        ///end of new stuff
/*        if(xSource != null) {
            for(int k=0; k<nSource; k++) {
                double[] x = xSource.values(whichValueX);
                double[] y = ySource[k].values(whichValue);
                for(int i=0; i<y.length; i++) {
                    plot.addPoint(k,xUnit.fromSim(x[i]),yUnit.fromSim(y[i]),true);
                }//for i
            }//for k
        }//end if
        else {//xSource is null
            for(int k=0; k<nSource; k++) {
                double[] y = ySource[k].values(whichValue);
                for(int i=0; i<y.length; i++) {
                    plot.addPoint(k,(double)i,yUnit.fromSim(y[i]),true);
                }//for i
            }//for k
        }//end else
       
        plot.repaint(); */
    }//end doUpdate method
    
 /**
  * Define inner class as extension of ptolemy.plot.Plot
  * Does not override anything, but may want to later
  */
    public class Plot extends ptolemy.plot.Plot {
    }
}