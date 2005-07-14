package etomica.space;


/**
 * Interface for classes that make an atom's coordinate field.  This is
 * required by the constructor of Atom.
 *
 * @author David Kofke
 *
 */

/*
 * History
 * Created on Jul 13, 2005 by kofke
 */
public interface CoordinateFactory {

    public ICoordinate makeCoordinate();
    
    public void setKinetic(boolean kinetic);
    
    public boolean isKinetic();
    
    /**
     * Defines a CoordinateFactory that always returns null from makeCoordinate.
     */
    public static final CoordinateFactory NULL = new CoordinateFactory() {
        public ICoordinate makeCoordinate() {
            return null;
        }
        
        public void setKinetic(boolean kinetic) {}
        
        public boolean isKinetic() {
            return false;
        }
    };
}
