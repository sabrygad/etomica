package etomica.models.nitrogen;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

import etomica.action.IAction;
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
import etomica.data.types.DataGroup;
import etomica.data.types.DataGroup.DataInfoGroup;
import etomica.normalmode.CoordinateDefinition.BasisCell;
import etomica.space3d.Space3D;
import etomica.units.CompoundDimension;
import etomica.units.Degree;
import etomica.units.Dimension;
import etomica.units.Length;
import etomica.units.Null;
import etomica.util.HistogramExpanding;

/**
 * Meter that measures the spherical coordinate angles
 * angle[0]: is theta, angle between the projection vector (molecular axis vector onto xy plane)
 * 				and x-axis
 * angle[1]: is phi,   angle between the molecular axis vector and z-axis  
 * 
 * @author Tai Boon Tan
 *
 */
public class MeterOrientationDistribution implements IEtomicaDataSource, IAction, Serializable {

    public MeterOrientationDistribution(IBox newBox, CoordinateDefinitionNitrogen coordDef, ISpecies species) {
        tag = new DataTag();
        this.coordinateDefinition = coordDef;
        
        if (species==null){
        	throw new RuntimeException("Need to set species in MeterNormalizedCoord class");
        }
 
        angle = new double[2];
        axes = new IVectorMutable[3];
        molAxis = Space3D.makeVector(3);
        temp = Space3D.makeVector(3);
        b = Space3D.makeVector(3);
        c = Space3D.makeVector(3);
        
        for (int i=0; i<axes.length; i++){
        	axes[i] = Space3D.makeVector(3);
        }
        axes[0].E(new double[]{1.0, 0.0, 0.0});
        axes[1].E(new double[]{0.0, 1.0, 0.0});
        axes[2].E(new double[]{0.0, 0.0, 1.0});
        
       	dof = (coordDef.getCoordinateDim() /newBox.getNMolecules(species)) - 3;
        
        uDistributions = new DataDoubleArray[dof];
        uDistributionsInfo = new DataInfoDoubleArray[dof];
        
        histogramU = new HistogramExpanding[dof];
        for (int i=0; i<dof; i++){
        	histogramU[i] = new HistogramExpanding(0.05);
        }
    }

    public IEtomicaDataInfo getDataInfo() {
        return dataInfo;
    }
    
    /**
     * Assigning the histogram to data
     */
    public void actionPerformed() {
    
        BasisCell[] cells = coordinateDefinition.getBasisCells();
                
        for (int iCell = 0; iCell<cells.length; iCell++) {
      
            BasisCell cell = cells[iCell];
            IMoleculeList molecules = cell.molecules;
            int numMolecules = molecules.getMoleculeCount();
            
            for (int iMol=0; iMol<numMolecules; iMol++){
            	
	          	IMolecule molecule = molecules.getMolecule(iMol);
	          	IVectorMutable leafPos0 = molecule.getChildList().getAtom(0).getPosition();
		    	IVectorMutable leafPos1 = molecule.getChildList().getAtom(1).getPosition();
		
		    	molAxis.Ev1Mv2(leafPos1, leafPos0);
		       	molAxis.normalize();
	            angle[1] = Degree.UNIT.fromSim(Math.acos(molAxis.dot(axes[2])));
		       	
		       	temp.E(molAxis);
		       	temp.XE(axes[2]);
		       	temp.normalize();
		       	
		       	b.E(axes[2]);
		       	b.XE(temp);
		       	b.normalize();
		       	
		       	c.Ea1Tv1(Math.sqrt(molAxis.squared()), b);
		       	angle[0] = Degree.UNIT.fromSim(Math.acos(c.dot(axes[0])));
	            for (int i=0; i<dof; i++){ 
	            	histogramU[i].addValue(angle[i]);
	            }           
            }
        } //end of cell; there is only 1 cell
    }

    /**
     * Returns the DataGroup of uDistribtion[i]
     */
    public IData getData() {
 
    	CompoundDimension length = new CompoundDimension(new Dimension[]{Length.DIMENSION}, new double[]{1});
        
    	for(int i=0; i<dof; i++){
    		int nBin = histogramU[i].getNBins();
    		uDistributions[i] = new DataDoubleArray(new int[] {2, nBin});
    		uDistributionsInfo[i] = new DataInfoDoubleArray("u", length, new int[]{2, nBin});
    		
    		uDistributions[i].assignColumnFrom(0, histogramU[i].xValues());
    		uDistributions[i].assignColumnFrom(1, histogramU[i].getHistogram());
    		
    	}
    	
    	data = new DataGroup(uDistributions);
        dataInfo = new DataInfoGroup("all uDistributions", Null.DIMENSION, uDistributionsInfo);
      
    	return data;
    	
    }

    /**
     * Sets the tensor summation to 0.
     */
    public void reset() {
        data.E(0);
    }

    public DataTag getTag() {
        return tag;
    }
    
	public void writeUdistribution(String filename){
		DataGroup uData = (DataGroup)getData();
		
		for (int i=0; i<uData.getNData(); i++){
			String fName = filename+"U"+i+".orient";
			try {
				FileWriter fileWriter = new FileWriter(fName,false);
				
				DataDoubleArray uDistribution = (DataDoubleArray)uData.getData(i);
				
				for (int j=0; j<uDistribution.getLength()/uDistribution.getArrayDimension(); j++){
					fileWriter.write(uDistribution.getValue(new int[]{0,j})+" "+ uDistribution.getValue(new int[]{1,j}) + "\n");
				}
			
				fileWriter.close();
				
			} catch(IOException e){
				throw new RuntimeException("Failed to write coord data orientation U" + e);
			
			}
		}
		
	}
    private static final long serialVersionUID = 1L;
    protected CoordinateDefinitionNitrogen coordinateDefinition;
    private final DataTag tag;
    private IEtomicaDataInfo dataInfo;
    private DataGroup data;
    private HistogramExpanding[] histogramU;
    private DataDoubleArray[] uDistributions;
    private DataInfoDoubleArray[] uDistributionsInfo;
    private IVectorMutable[] axes;
    private IVectorMutable temp, b, c;
    private IVectorMutable molAxis;
    private double[] angle;
    private int dof;
    
}