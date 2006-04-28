package etomica.data.types;

import etomica.data.Data;
import etomica.data.DataFactory;
import etomica.data.DataInfo;
import etomica.data.DataProcessor;

/**
 * A DataProcessor that converts a Data instance into a DataDoubleArray. Copies
 * an element of the input data to the DataDouble's encapsulated value and
 * returns the DataDouble instance.
 * <p>
 * Casting for various types of Data is performed as follows:
 * <ul>
 * <li><u>DataDoubleArray</u>. Does nothing, and returns input data directly.
 * 
 * <li><u>DataDouble</u>. Casts to a 1-element array.
 * 
 * <li><u>DataInteger</u>. Casts to a 1-element array.
 * 
 * <li><u>DataVector</u>. Casts to a one-dimensional array of length equal to
 * the number of vector elements.
 * 
 * <li><u>DataTensor</u>. Casts to a two-dimension square array.
 * 
 * <li><u>DataFunction</u>. Casts only the dependent data, to an array of the
 * same shape. Independent data are discarded.
 * 
 * <li><u>DataGroup</u>. Uses DataExtractor to locate a DataDoubleArray in DataGroup.  Other 
 * Data in group are discarded. 
 * 
 * </ul>
 * Attempts to process a different type will result in an
 * IllegalArgumentException when encountered in the processDataInfo method.
 * <p>
 * @author David Kofke and Andrew Schultz
 *  
 */

/*
 * History Created on Jul 21, 2005 by kofke
 */
public class CastToDoubleArray extends DataProcessor {

    /**
     * Sole constructor.
     */
    public CastToDoubleArray() {
    }

    /**
     * Prepares processor to perform cast. Given DataInfo is examined to see
     * what data type will be given to processor.
     * 
     * @throws IllegalArgumentException
     *             if DataInfo is not one of the acceptable types, as described
     *             in general comments for this class
     */
    protected DataInfo processDataInfo(DataInfo inputDataInfo) {
        Class inputClass = inputDataInfo.getDataClass();
        DataFactory factory = inputDataInfo.getDataFactory();
        int[] arrayShape;
        if (inputClass == DataDoubleArray.class) {
            inputType = 0;
            return inputDataInfo;
        } else if (inputClass == DataDouble.class) {
            inputType = 1;
            arrayShape = new int[]{1};
        } else if (inputClass == DataInteger.class) {
            inputType = 2;
            arrayShape = new int[]{1};
        } else if (inputClass == DataVector.class) {
            inputType = 3;
            arrayShape = new int[]{((DataVector.Factory)factory).getSpace().D()};
        } else if (inputClass == DataTensor.class) {
            inputType = 4;
            int D = ((DataTensor.Factory) factory).getSpace().D();
            arrayShape = new int[]{D,D};
        } else {
            throw new IllegalArgumentException(
                    "Cannot cast to DataDoubleArray from " + inputClass);
        }
        outputData = new DataDoubleArray(arrayShape);
        return new DataInfo(inputDataInfo.getLabel(), inputDataInfo.getDimension(), DataDoubleArray.getFactory(arrayShape));
    }

    /**
     * Copies input Data to a DataDoubleArray and returns it (the DataDataDoubleArray).
     * 
     * @throws ClassCastException
     *             if input Data is not of the type indicated by the most recent
     *             call to processDataInfo
     *  
     */
    protected Data processData(Data data) {
        switch (inputType) {
        case 0:
            return data;
        case 1:
            outputData.E(((DataDouble) data).x);
            return outputData;
        case 2:
            outputData.E(((DataInteger) data).x);
            return outputData;
        case 3:
            ((DataVector) data).x.assignTo(outputData.getData());
            return outputData;
        case 4:
            ((DataTensor) data).x.assignTo(outputData.getData());//both Tensor and DataDoubleArray
                                                                 // sequence data by rows
            return outputData;
        default:
            throw new Error("Assertion error.  Input type out of range: "
                    + inputType);
        }
    }

    public DataProcessor getDataCaster(DataInfo info) {
        return null;
    }

    private DataDoubleArray outputData;
    private int inputType;
}
