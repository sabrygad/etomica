/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.normalmode;

import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.meter.MeterPotentialEnergyFromIntegrator;
import etomica.data.types.DataDouble;
import etomica.data.types.DataDouble.DataInfoDouble;
import etomica.integrator.IntegratorBox;
import etomica.units.Null;

/**
 * Meter used for overlap sampling in the target-sampled system.  The meter
 * measures the energy difference for the harmonic and target
 * potentials.
 * 
 * @author Tai Boon Tan
 */
public class MeterWorkTargetPhaseSpace implements IEtomicaDataSource {
    
    public MeterWorkTargetPhaseSpace(IntegratorBox integrator, MeterHarmonicEnergy meterHarmonicEnergy) {
        meterEnergy = new MeterPotentialEnergyFromIntegrator(integrator);
        this.integrator = integrator;
        this.meterHarmonicEnergy = meterHarmonicEnergy;
        data = new DataDouble();
        dataInfo = new DataInfoDouble("Scaled Harmonic and hard sphere Energies", Null.DIMENSION);

        tag = new DataTag();
    }

    public IData getData() {
    	double uTarget = (meterEnergy.getDataAsScalar() - latticeEnergy);
    	double uHarmonic = meterHarmonicEnergy.getDataAsScalar();
    	data.x = ( uHarmonic - uTarget) / integrator.getTemperature();
    	
        return data;
    }

    public void setLatticeEnergy(double newLatticeEnergy) {
        latticeEnergy = newLatticeEnergy;
    }
    
    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }

    public DataTag getTag() {
        return tag;
    }

    protected final MeterPotentialEnergyFromIntegrator meterEnergy;
    protected final MeterHarmonicEnergy meterHarmonicEnergy;
    protected final IntegratorBox integrator;
    protected final DataDouble data;
    protected final DataInfoDouble dataInfo;
    protected final DataTag tag;
    protected double latticeEnergy;
}
