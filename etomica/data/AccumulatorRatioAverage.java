package etomica.data;

import etomica.data.types.DataArithmetic;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataGroup;
import etomica.units.Dimension;
import etomica.util.Constants;
import etomica.util.Function;

/**
 * Accumulator for calculating ratio between two sums
 */
public class AccumulatorRatioAverage extends AccumulatorAverage {
    
    public AccumulatorRatioAverage() {
        super();
    }
    
    public Data getData() {
        if (mostRecent == null) return null;
        if(count > 0) {
            super.getData();
            double average0 = ((DataDoubleArray)average).getData()[0];
            if (average0 == 0) {
                ratio.E(Double.NaN);
                ratioError.E(Double.NaN);
                ratioStandardDeviation.E(Double.NaN);
                return dataGroup;
            }

            ratio.E((Data)sum);
            ratio.TE(1/((DataDoubleArray)sum).getData()[0]);

            double errorRatio0 = ((DataDoubleArray)error).getData()[0]/average0;
            errorRatio0 *= errorRatio0;
            ratioError.E((Data)error);
            ratioError.DE(average);
            ratioError.TE(ratioError);
            ratioError.PE(errorRatio0);
            ratioError.map(Function.Sqrt.INSTANCE);
            ratioError.TE(ratio);
            ratioError.map(Function.Abs.INSTANCE);

            double stdevRatio0 = ((DataDoubleArray)standardDeviation).getData()[0]/average0;
            ratioStandardDeviation.E((Data)standardDeviation);
            ratioStandardDeviation.DE(average);
            ratioStandardDeviation.TE(ratioStandardDeviation);
            ratioStandardDeviation.PE(stdevRatio0);
            ratioStandardDeviation.map(Function.Sqrt.INSTANCE);
            ratioStandardDeviation.TE(ratio);
            ratioStandardDeviation.map(Function.Abs.INSTANCE);
        }
        return dataGroup;
    }

    public void reset() {
        super.reset();
        ratio.E(Double.NaN);
        ratioError.E(Double.NaN);
        ratioStandardDeviation.E(Double.NaN);
    }
    
    public DataInfo processDataInfo(DataInfo dataInfo) {
        DataFactory factory = dataInfo.getDataFactory();
        
        sum = (DataArithmetic)factory.makeData(dataInfo.getLabel()+"(blkAvg sum)", dataInfo.getDimension());

        ratio = (DataArithmetic)factory.makeData("Ratio", Dimension.NULL);
        ratioError = (DataArithmetic)factory.makeData("Ratio error", Dimension.NULL);
        ratioStandardDeviation = (DataArithmetic)factory.makeData("Ratio stddev", Dimension.NULL);
        super.processDataInfo(dataInfo);
        Data[] dataGroups = new Data[dataGroup.getNData()+3];
        int i;
        for (i=0; i<dataGroup.getNData(); i++) {
            dataGroups[i] = dataGroup.getData(i);
        }
        dataGroups[i++] = (Data)ratio;
        dataGroups[i++] = (Data)ratioError;
        dataGroups[i++] = (Data)ratioStandardDeviation;
        dataGroup = new DataGroup("Group", dataGroups);
        return dataGroup.getDataInfo();
    }
    
    public static class Type extends AccumulatorAverage.Type {
        protected Type(String label, int index) {super(label,index);}       
        public Constants.TypedConstant[] choices() {return VIRIAL_CHOICES;}
    }
    //XXX such an ugly hack!!!!
    protected static final AccumulatorAverage.Type[] VIRIAL_CHOICES = 
        new AccumulatorAverage.Type[] {
            CHOICES[0], CHOICES[1], CHOICES[2], CHOICES[3], CHOICES[4],
            new Type("Ratio",5),
            new Type("Ratio error",6), new Type("Ratio standard deviation",7)};
    public static final AccumulatorAverage.Type RATIO = VIRIAL_CHOICES[5];
    public static final AccumulatorAverage.Type RATIO_ERROR = VIRIAL_CHOICES[6];
    public static final AccumulatorAverage.Type RATIO_STANDARD_DEVIATION = VIRIAL_CHOICES[7];

    //need separate fields because ratio values are calculated from the non-ratio values.
    protected DataArithmetic ratio, ratioStandardDeviation, ratioError;
}
