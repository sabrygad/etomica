package etomica.modules.adsorption;

import etomica.integrator.IntegratorBox;
import etomica.modifier.Modifier;
import etomica.potential.P2SquareWell;
import etomica.units.Dimension;
import etomica.units.Null;

public class ModifierPMu implements Modifier {

    protected final EOSSW eos;
    protected final MyMCMove move;
    protected final P2SquareWell p2;
    protected final MeterExcessAdsorbed meterCountAtoms;
    protected double oldValue = 0;
    
    protected IntegratorBox integrator;
    
    public ModifierPMu(P2SquareWell p2, IntegratorBox integrator, EOSSW eos, MyMCMove move, MeterExcessAdsorbed meterCountAtoms) {
        this.p2 = p2;
        this.integrator = integrator;
        this.move = move;
        this.eos = eos;
        this.meterCountAtoms = meterCountAtoms;
        eos.setTemperature(integrator.getTemperature());
        reset();
    }
    
    public void setValue(double newValue) {
//        System.out.println("newValue "+newValue);
        double temperature = integrator.getTemperature();
        eos.setTemperature(temperature);
//        double sigma = p2.getCoreDiameter();
        double p = Math.pow(10,newValue); // * eos.pSat() / (sigma*sigma*sigma);
        double mu = eos.muForPressure(p);
        meterCountAtoms.setPressure(p);
//        System.out.println(temperature+" "+p+" "+mu);
        move.setMu(mu);
        oldValue = newValue;
    }
    
    public void reset() {
        eos.setSigma(p2.getCoreDiameter());
        eos.setEpsilon(p2.getEpsilon());
        eos.setLambda(p2.getLambda());
    }

    public double getValue() {
        // cheat.
        return oldValue;
    }

    public Dimension getDimension() {
        return Null.DIMENSION;
    }

    public String getLabel() {
        return "pressure";
    }
}