package etomica.modules.interfacial;
import java.util.Arrays;

import etomica.api.IAtomPositioned;
import etomica.api.IVector;
import etomica.data.Data;
import etomica.data.DataSource;
import etomica.data.DataSourceIndependent;
import etomica.data.DataSourceTensorVirialHard;
import etomica.data.DataTag;
import etomica.data.IDataInfo;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataFunction;
import etomica.data.types.DataGroup;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.data.types.DataFunction.DataInfoFunction;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.integrator.IntegratorHard;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.units.Energy;
import etomica.units.Length;

/**
 * A MeterTensor that returns the virial component of the pressure tensor for a hard potential.  
 * This is the counterpart to MeterVelocityTensor, also called by the surface tension meter.
 *
 * @author Rob Riggleman
 */

public class DataSourceTensorVirialHardProfile extends DataSourceTensorVirialHard implements DataSourceIndependent {
    
    public DataSourceTensorVirialHardProfile(ISpace space) {
        super(space);
        profileData = new DataGroup(new Data[0]);
        profileDataInfo = new DataInfoGroup("Virial profiles", Energy.DIMENSION, new IDataInfo[0]);
        profileDataInfo.addTag(tag);
        binSize = 0.1;
        virialProfile = new double[space.D()][0];
    }
    
    /**
     * Current value of the meter, obtained by dividing sum of collision virial contributions by time elapsed since last call.
     * If elapsed-time interval is zero, returns the value reported at the last call to the method.
     */
    public DataGroup getVirialProfile() {
        double currentTime = integratorHard.getCurrentTime();
        double elapsedTime = currentTime - lastProfileTime;
        lastProfileTime = currentTime;
        
        IVector boxDim = integratorHard.getBox().getBoundary().getDimensions();
        if (L != boxDim.x(0)) {
            setupProfileData();
            profileData.E(Double.NaN);
            return profileData;
        }
        
        if(elapsedTime == 0.0) {
            profileData.E(Double.NaN);
            return profileData;
        }

        for (int i=0; i<virialProfile.length; i++) {
            System.arraycopy(virialProfileWork[i], 0, virialProfile[i], 0, virialProfile[i].length);
            Arrays.fill(virialProfileWork[i], 0);
        }
        profileData.TE(-1.0/elapsedTime);
        return profileData;
    }
    
    public DataInfoGroup getProfileDataInfo() {
        return profileDataInfo;
    }
    
    /**
     * Sums contribution to virial for each collision.
     */
    public void collisionAction(IntegratorHard.Agent agent) {
        super.collisionAction(agent);

        Tensor virialTensor = agent.collisionPotential.lastCollisionVirialTensor();

        double x0 = ((IAtomPositioned)agent.atom).getPosition().x(0);
        // wrap around PBC
        x0 -= Math.round(x0*Li) * L;
        int iBin0 = (int) ((x0 + halfL) / binSize);
        double x1 = ((IAtomPositioned)agent.collisionPartner).getPosition().x(0);
        // wrap around PBC
        x1 -= Math.round(x1*Li) * L;
        int iBin1 = (int) ((x1 + halfL) / binSize);
        if (iBin0 == iBin1) {
            addTensor(iBin0, virialTensor);
        }
        else if (Math.abs(iBin1-iBin0) > nBins/2) {
            if (iBin0 > nBins/2) {
                //swap so iBin1 is on the right side, iBin0 on the left
                int foo = iBin0;
                iBin0 = iBin1;
                iBin1 = foo;
            }
            int nBetween = (nBins-1)-iBin1+1 + iBin0+1;
            virialTensor.TE(1.0/nBetween);
            for (int i=iBin1; i<nBins; i++) {
                addTensor(i, virialTensor);
            }
            for (int i=0; i<iBin0+1; i++) {
                addTensor(i, virialTensor);
            }
        }
        else {
            if (iBin0 > iBin1) {
                //swap so iBin1 is on the right side, iBin0 on the left
                int foo = iBin0;
                iBin0 = iBin1;
                iBin1 = foo;
            }
            int nBetween = iBin1 - iBin0 + 1;
            virialTensor.TE(1.0/nBetween);
            for (int i=iBin0; i<iBin1+1; i++) {
                addTensor(i, virialTensor);
            }
        }
    }
    
    protected void addTensor(int iBin, Tensor virialTensor) {
        for (int i=0; i<virialTensor.D(); i++) {
            virialProfileWork[i][iBin] += virialTensor.component(i,i);
        }
    }
    
    public void setBinSize(double newBinSize) {
        binSize = newBinSize;
        if (integratorHard != null) {
            setupProfileData();
        }
    }
    
    public double getBinSize() {
        return binSize;
    }
    
    public void setIntegrator(IntegratorHard newIntegrator) {
        super.setIntegrator(newIntegrator);
        setupProfileData();
    }

    protected void setupProfileData() {
        IVector boxDim = integratorHard.getBox().getBoundary().getDimensions();
        L = boxDim.x(0);
        Li = 1.0/L;
        halfL = 0.5*L;
        nBins = (int)Math.round(L/binSize);
        binSize = boxDim.x(0) / nBins;
        
        xData = new DataDoubleArray(nBins);
        double[] x = xData.getData();
        for (int i=0; i<nBins; i++) {
            x[i] = -0.5 * boxDim.x(0) + (i+0.5) * binSize;
        }
        xDataInfo = new DataInfoDoubleArray("x", Length.DIMENSION, new int[] {nBins});
        
        DataFunction[] virialData = new DataFunction[boxDim.getD()];
        DataInfoFunction[] virialDataInfo = new DataInfoFunction[boxDim.getD()];
        for (int i=0; i<virialData.length; i++) {
            virialData[i] = new DataFunction(new int[]{nBins});
            virialProfile[i] = virialData[i].getData();
            virialDataInfo[i] = new DataInfoFunction("virial", Energy.DIMENSION, this);
        }
        profileData = new DataGroup(virialData);
        profileDataInfo = new DataInfoGroup("virial", Energy.DIMENSION, virialDataInfo);
        
        virialProfileWork = new double[boxDim.getD()][nBins];
    }

    public int getIndependentArrayDimension() {
        return 1;
    }

    public DataDoubleArray getIndependentData(int i) {
        return xData;
    }

    public DataInfoDoubleArray getIndependentDataInfo(int i) {
        return xDataInfo;
    }

    private static final long serialVersionUID = 1L;
    protected double lastProfileTime;
    protected DataGroup profileData;
    protected double[][] virialProfile, virialProfileWork;
    protected DataInfoGroup profileDataInfo;
    protected DataDoubleArray xData;
    protected DataInfoDoubleArray xDataInfo;
    protected double binSize;
    protected double L, Li, halfL;
    protected int nBins;
    
    public static class DataSourceVirialProfile implements DataSource {

        public DataSourceVirialProfile(DataSourceTensorVirialHardProfile meter) {
            this.meter = meter;
            tag = new DataTag();
        }
        
        public Data getData() {
            return meter.getVirialProfile();
        }

        public IDataInfo getDataInfo() {
            return meter.getProfileDataInfo();
        }

        public DataTag getTag() {
            return tag;
        }
        
        protected final DataSourceTensorVirialHardProfile meter;
        protected final DataTag tag;
    }
}