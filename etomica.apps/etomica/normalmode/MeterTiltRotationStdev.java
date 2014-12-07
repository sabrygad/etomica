package etomica.normalmode;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.ISpecies;
import etomica.api.IVectorMutable;
import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.space.ISpace;
import etomica.units.Angle;

/**
 * Meter that measures the average tilt angle (not the angle of average tilt!)
 *
 * @author Andrew Schultz
 */
public class MeterTiltRotationStdev implements IEtomicaDataSource {

    public MeterTiltRotationStdev(ISpace space, ISpecies species, int nPlanes) {
        this.species = species;
        dr = space.makeVector();
        phiSum = new double[nPlanes+1];
        phi2Sum = new double[nPlanes+1];
        data = new DataDoubleArray(nPlanes+1);
        dataInfo = new DataInfoDoubleArray("tilt", Angle.DIMENSION, new int[]{nPlanes+1});
        tag = new DataTag();
        dataInfo.addTag(tag);
    }
    
    public void setBox(IBox newBox) {
        box = newBox;
    }

    public IData getData() {
        IMoleculeList molecules = box.getMoleculeList(species);
        int nMolecules = molecules.getMoleculeCount();
        for (int i=0; i<phiSum.length; i++) {
            phiSum[i] = 0;
            phi2Sum[i] = 0;
        }
        for (int i=0; i<nMolecules; i++) {
            IMolecule molecule = molecules.getMolecule(i);
            IAtomList atomList = molecule.getChildList();
            int leafCount = atomList.getAtomCount();
            dr.E(atomList.getAtom(leafCount-1).getPosition());
            dr.ME(atomList.getAtom(0).getPosition());
            double phi = Math.atan2(dr.getX(1), dr.getX(0));
            phiSum[0] += phi;
            phi2Sum[0] += phi*phi;
            int iPlane = (i/2)%(phiSum.length-1);
            phiSum[iPlane+1] += phi;
            phi2Sum[iPlane+1] += phi*phi;
        }
        double[] x = data.getData();
        for (int i=0; i<x.length; i++) {
            int n = i==0 ? nMolecules : (nMolecules/(phiSum.length-1));
            x[i] = Math.sqrt((n*phi2Sum[i]-phiSum[i]*phiSum[i])/(n*(n-1)));
        }
        return data;
    }

    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }

    public DataTag getTag() {
        return tag;
    }

    private static final long serialVersionUID = 1L;
    protected final ISpecies species;
    protected IBox box;
    protected final IVectorMutable dr;
    protected final DataDoubleArray data;
    protected final DataInfoDoubleArray dataInfo;
    protected final DataTag tag;
    protected final double[] phiSum, phi2Sum;
}