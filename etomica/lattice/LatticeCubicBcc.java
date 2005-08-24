package etomica.lattice;
import etomica.lattice.crystal.BasisCubicBcc;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.space3d.Space3D;
import etomica.util.Default;

/**
 * Cubic primitive with a 2-site bcc basis.
 */

public class LatticeCubicBcc extends LatticeCrystal implements CubicLattice {
    
	/**
	 * Cubic bcc crystal with a lattice constant that gives a
     * maximum-density structure for spheres of size Default.ATOM_SIZE. 
	 */
    public LatticeCubicBcc() {
        this(2.0/Math.sqrt(3.0)*Default.ATOM_SIZE);
    }
    
	public LatticeCubicBcc(double latticeConstant) {
		this(new PrimitiveCubic(Space3D.getInstance()));
        primitive = (PrimitiveCubic)crystal.getLattice().getPrimitive();
        primitive.setCubicSize(latticeConstant);
	}

	/**
	 * Auxiliary constructor needed to be able to pass new PrimitiveCubic and
	 * new BasisCubicBcc (which needs the new primitive) to super.
	 */	
	private LatticeCubicBcc(PrimitiveCubic primitive) {
		super(new Crystal(primitive, new BasisCubicBcc(primitive)));
	}
    
    /**
     * Returns the primitive the determines the lattice constant.
     * Set the lattice constant via primitive().setSize(value).
     */
    public PrimitiveCubic primitive() {
        return primitive;
    }
    
    /**
     * The lattice constant is the size of the cubic primitive vectors
     * of the lattice underlying this crystal.
     */
    public void setLatticeConstant(double latticeConstant) {
        primitive.setCubicSize(latticeConstant);
    }
    
    public double getLatticeConstant() {
        return primitive.getCubicSize();
    }

    /**
     * Returns "Bcc".
     */
    public String toString() {return "Bcc";}
    
    private PrimitiveCubic primitive;
    
}//end of CrystalBcc