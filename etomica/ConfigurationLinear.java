package etomica;
import etomica.units.Dimension;

/**
 * Places atoms in a straight line.  Does not zero total momentum.
 *
 * @author David Kofke
 */

/* History
 * 08/12/03 (DAK) added constructor that takes angles
 */

public class ConfigurationLinear extends Configuration {
          
    private double bondLength;
    private Space.Vector orientation;
    private double[] angle;
    
    public ConfigurationLinear(Space space) {
        this(space, 0.55*Default.ATOM_SIZE);
    }
    public ConfigurationLinear(Space space, double bondLength) {
    	this(space, bondLength, new double[] {etomica.units.Degree.UNIT.toSim(45.), 0.0});
    }
    public ConfigurationLinear(Space space, double bondLength, double[] initAngles) {
        super(space);
        this.bondLength = bondLength;
        orientation = space.makeVector();
        angle = new double[space.D()];
        for(int i=0; i<initAngles.length; i++) setAngle(i,initAngles[i]);
        zeroTotalMomentum = false;
    }
    //remove these constructors when possible
    public ConfigurationLinear(Simulation sim) {
        this(sim, 0.55*Default.ATOM_SIZE);
    }
    public ConfigurationLinear(Simulation sim, double bondLength) {
        super(sim);
        this.bondLength = bondLength;
        orientation = space.makeVector();
        angle = new double[space.D()];
        setAngle(0,etomica.units.Degree.UNIT.toSim(45.));
        zeroTotalMomentum = false;
    }
              
    public void setBondLength(double b) {
        bondLength = b;
    }
    public double getBondLength() {return bondLength;}
    public Dimension getBondLengthDimension() {return Dimension.LENGTH;}
    
    //need to re-express this in terms of a Space.Orientation object
    public void setAngle(int i, double t) {//t in radians
        angle[i] = t;
        switch(angle.length) {
            case 1:
                return;
            case 2:
                setOrientation(new Space2D.Vector(Math.cos(angle[0]),Math.sin(angle[0])));
                return;
            case 3:
                setOrientation(new Space3D.Vector(Math.sin(angle[1])*Math.cos(angle[0]),
                                                  Math.sin(angle[1])*Math.sin(angle[0]),
                                                  Math.cos(angle[1])));
                return;
        }
    }
    public double getAngle(int i) {return angle[i];}
    public void setOrientation(Space.Vector e) {orientation.E(e);}
    
    public void setOffset(Space.Vector v) {
        orientation.E(v);
        bondLength = Math.sqrt(v.squared());
        orientation.DE(bondLength);
    }
              
    /**
    * Sets all atoms coordinates to lie on a straight line along the x-axis, with the
    * center of mass unchanged from the value before method was called
    */
    public void initializePositions(AtomIterator[] iterators) {
        
        AtomIteratorCompound iterator = new AtomIteratorCompound(iterators);
        int size = iterator.size();
        if(iterator.size() == 0) return;
            
        double xNext = -bondLength*0.5*(double)(size-1);
        iterator.reset();
        while(iterator.hasNext()) {
            Atom a = iterator.next();
            try {//may get null pointer exception when beginning simulation
                a.creator().getConfiguration().initializePositions(a);
            } catch(NullPointerException e) {}
            a.coord.translateTo(space.origin());
            a.coord.translateBy(xNext,orientation);  //move xNext distance in direction orientation
            xNext += bondLength;
        }
    }//end of initializeCoordinates
}//end of ConfigurationLinear
      
