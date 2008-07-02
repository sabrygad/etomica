package etomica.normalmode;

import etomica.data.Data;
import etomica.data.DataSource;
import etomica.data.DataTag;
import etomica.data.IDataInfo;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.integrator.IntegratorBox;
import etomica.units.Null;

/**
 * Meter used for direct sampling in the harmonic-sampled system.  The meter
 * measures the energy difference between the Umbrella's sampling region and harmonic system
 * 
 * @author Andrew Schultz & Tai Boon Tan
 */
public class MeterWorkUmbrellaHarmonic implements DataSource {
    
    public MeterWorkUmbrellaHarmonic(IntegratorBox integrator, MeterHarmonicEnergy meterHarmonic) {
        meterTarget= new MeterPotentialEnergyFromIntegrator(integrator);
        this.integrator = integrator;
    	this.meterHarmonic = meterHarmonic;
    	
        data = new DataDouble();
        dataInfo = new DataInfoDouble("Scaled Harmonic and soft sphere Energies", Null.DIMENSION);

        tag = new DataTag();
    }

    public Data getData() {
    	
    	double uTarget = meterTarget.getDataAsScalar();
    	double uHarmonic = meterHarmonic.getDataAsScalar();
    	double exp_uTarget = Math.exp(-(uTarget-latticeEnergy) /integrator.getTemperature());
    	double exp_uHarmonic = Math.exp(-uHarmonic /integrator.getTemperature());
    	double exp_2uTarget = exp_uTarget*exp_uTarget;
    	double exp_2uHarmonic = exp_uHarmonic*exp_uHarmonic;
    	
    	double gamma = Math.sqrt(exp_2uTarget + refPref*refPref*exp_2uHarmonic);
    	double umbrellaEnergy = -Math.log(gamma)*integrator.getTemperature();
        
//    	System.out.println("\nBennetHarmonic");
//    	System.out.println("uTarget-ulattice: "+ (uTarget - latticeEnergy));
//    	System.out.println("uHarmonic: "+ uHarmonic);
//    	System.out.println("uOverlap: "+overlapEnergy);
//    	System.out.println("uBennetHarmonic: "+(uHarmonic - overlapEnergy));
    	
    	data.x = (uHarmonic- umbrellaEnergy)/integrator.getTemperature();
        
        return data;
    }

    public void setLatticeEnergy(double newLatticeEnergy) {
        latticeEnergy = newLatticeEnergy;
    }
    
    public IDataInfo getDataInfo() {
        return dataInfo;
    }

    public DataTag getTag() {
        return tag;
    }

	public double getRefPref() {
		return refPref;
	}

	public void setRefPref(double refPref) {
		this.refPref = refPref;
	}
    
    protected final MeterPotentialEnergyFromIntegrator meterTarget;
    protected final MeterHarmonicEnergy meterHarmonic;
    protected final IntegratorBox integrator;
    protected final DataDouble data;
    protected final DataInfoDouble dataInfo;
    protected final DataTag tag;
    protected double latticeEnergy;
    protected double refPref;



}
