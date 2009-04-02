package etomica.models.oneDHardRods;

import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorLeafAtoms;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveBoxStep;
import etomica.normalmode.CoordinateDefinition;
import etomica.normalmode.CoordinateDefinition.BasisCell;

/**
 * A Monte Carlo move which selects a wave vector, and changes the normal mode
 * associated with that wave vector.
 * 
 * harmonicWV are the wave vectors that cannot be changed by the doTrial() method.
 * 
 * @author cribbin
 *
 */
public class MCMoveChangeMultipleModes extends MCMoveBoxStep{

    private static final long serialVersionUID = 1L;
    protected CoordinateDefinition coordinateDefinition;
    private final AtomIteratorLeafAtoms iterator;
    protected double[][] uOld;
    protected double[] deltaU;
    protected final IRandom random;
    protected double energyOld, energyNew /*, latticeEnergy*/;
    protected final MeterPotentialEnergy energyMeter;
    private double[][][] eigenVectors;
    private IVectorMutable[] waveVectors;
    private double[] waveVectorCoefficients;
    int changedWV;
    int[] harmonicWaveVectors;  //all wvs from the harmonic wv are not changed.
    
    
//    MCMoveChangeSingleLEFT singleft;
    
    public MCMoveChangeMultipleModes(IPotentialMaster potentialMaster, IRandom random) {
        super(potentialMaster);
        
        this.random = random;
        iterator = new AtomIteratorLeafAtoms();
        energyMeter = new MeterPotentialEnergy(potentialMaster);
        
//        singleft = new MCMoveChangeSingleLEFT(potentialMaster, random);
    }

    public void setCoordinateDefinition(CoordinateDefinition newCoordinateDefinition) {
        coordinateDefinition = newCoordinateDefinition;
        deltaU = new double[coordinateDefinition.getCoordinateDim()];
        uOld = null;
        
//        singleft.setCoordinateDefinition(newCoordinateDefinition);
    }
    
    public CoordinateDefinition getCoordinateDefinition() {
        return coordinateDefinition;
    }
    
    /**
     * The harmonic wavevectors are not able to be changed by this MCMove.
     * 
     */
    public void setHarmonicWV(int[] hwv){
        harmonicWaveVectors = hwv;
        if(harmonicWaveVectors.length +1 == waveVectors.length){
            System.out.println("FEAR THE INFINiTE LOOP!!");
            throw new IllegalArgumentException("all wave vectors are harmonic!");
        }
        
//        singleft.setHarmonicWV(hwv[0]);
    }

    /**
     * Set the wave vectors used by the move.
     * 
     * @param wv
     */
    public void setWaveVectors(IVectorMutable[] wv){
        waveVectors = new IVectorMutable[wv.length];
        waveVectors = wv;
        
//        singleft.setWaveVectors(wv);
    }
    public void setWaveVectorCoefficients(double[] coeff){
        waveVectorCoefficients = coeff;
        
//        singleft.setWaveVectorCoefficients(coeff);
    }
    /**
     * Informs the move of the eigenvectors for the selected wave vector.  The
     * actual eigenvectors used will be those specified via setModes
     */
    public void setEigenVectors(double[][][] newEigenVectors) {
        eigenVectors = newEigenVectors;
        
//        singleft.setEigenVectors(newEigenVectors);
    }
    
    public void setBox(IBox newBox) {
        super.setBox(newBox);
        iterator.setBox(newBox);
        energyMeter.setBox(newBox);
        
//        singleft.setBox(newBox);
    }

    public AtomIterator affectedAtoms() {
        return iterator;
    }

//    public void setWaveVectorAndEigenVectorsChanged(int wv, int[] evectors){
//        //we will need some flag to indicate that these were set, and not
          //randomly assign them.
//    }
//    
    public boolean doTrial() {
        
//        singleft.doTrial();
        
        
        
//        System.out.println("MCMoveChangeMode doTrial");
        energyOld = energyMeter.getDataAsScalar();
        int coordinateDim = coordinateDefinition.getCoordinateDim();
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        
        //nan These lines make it a single atom-per-molecule class, and
        // assume that the first cell is the same as every other cell.
        BasisCell cell = cells[0];
//        double[] calcedU = coordinateDefinition.calcU(cell.molecules);
        uOld = new double[cells.length][coordinateDim];
        
        // Select the wave vector whose eigenvectors will be changed.
        // The zero wavevector is center of mass motion, and is rejected as
        // a possibility.
        boolean success = true;
        do{
            success = true;
            changedWV = random.nextInt(waveVectorCoefficients.length-1);
            changedWV += 1;
            for(int i = 0; i < harmonicWaveVectors.length; i++){
                if (changedWV == harmonicWaveVectors[i]) {
                    success = false;
                }
            }
        } while (!success);
        
//        System.out.println( changedWV );
        
        //calculate the new positions of the atoms.
        //loop over cells
        double delta1 = (2*random.nextDouble()-1) * stepSize;
        double delta2 = (2*random.nextDouble()-1) * stepSize;
        for(int iCell = 0; iCell < cells.length; iCell++){
            //store old positions.
            double[] uNow = coordinateDefinition.calcU(cells[iCell].molecules);
            System.arraycopy(uNow, 0, uOld[iCell], 0, coordinateDim);
            cell = cells[iCell];
            for(int i = 0; i< coordinateDim; i++){
                  deltaU[i] = 0;
            }
            
            //loop over the wavevectors, and sum contribution of each to the
            //generalized coordinates.  Change the selected wavevectors eigen-
            //vectors at the same time!
            double kR = waveVectors[changedWV].dot(cell.cellPosition);
            double coskR = Math.cos(kR);
            double sinkR = Math.sin(kR);
            for(int i = 0; i < coordinateDim; i++){
                for(int j = 0; j < coordinateDim; j++){
                    deltaU[j] += waveVectorCoefficients[changedWV]*
                        eigenVectors[changedWV][i][j]*2.0*(delta1*coskR - delta2*sinkR);
                }
            }
            double normalization = 1/Math.sqrt(cells.length);
            for(int i = 0; i < coordinateDim; i++){
                deltaU[i] *= normalization;
            }
            
            for(int i = 0; i < coordinateDim; i++) {
                uNow[i] += deltaU[i];
//                System.out.println(uNow[i]);
            }
            coordinateDefinition.setToU(cells[iCell].molecules, uNow);
            
        }
        
        energyNew = energyMeter.getDataAsScalar();
        return true;
    }
    
    public double getA() {
        return 1;
    }

    public double getB() {
        return -(energyNew - energyOld);
    }
    
    public void acceptNotify() {
//        System.out.println("accept");
//        iterator.reset();
//        for(int i = 0; i < 32; i++){
//            System.out.println(((AtomLeaf)iterator.nextAtom()).getPosition());
//        }
//        
    }

    public double energyChange() {
        return energyNew - energyOld;
    }

    public void rejectNotify() {
//        System.out.println("reject");
        // Set all the atoms back to the old values of u
        BasisCell[] cells = coordinateDefinition.getBasisCells();
        for (int iCell = 0; iCell<cells.length; iCell++) {
            BasisCell cell = cells[iCell];
            coordinateDefinition.setToU(cell.molecules, uOld[iCell]);
        }
    }


}