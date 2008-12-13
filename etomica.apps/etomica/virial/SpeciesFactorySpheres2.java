package etomica.virial;

import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.chem.elements.ElementSimple;
import etomica.config.ConformationChainZigZag2;
import etomica.space.ISpace;


public class SpeciesFactorySpheres2 implements SpeciesFactory, java.io.Serializable {

    public SpeciesFactorySpheres2(ISpace space, int nA, ElementSimple CH3element, ElementSimple CH2element) {
        this(space, nA, CH3element,CH2element, nominalBondL, nominalBondTheta);
    }
    
    public SpeciesFactorySpheres2(ISpace space, int nA, ElementSimple CH3element, ElementSimple CH2element, double bondL, double bondTheta) {
        this.nA = nA;
        this.CH3element = CH3element;
        this.CH2element = CH2element;
        this.bondL = bondL;
        this.bondTheta = bondTheta;
        this.space = space;
                init();
    }
    
    public void setBondL(double newBondL) {
        bondL = newBondL;
        init();
    }
    
    public double getBondL() {
        return bondL;
    }
    
    public void setBondTheta(double newBondTheta) {
        bondTheta = newBondTheta;
        init();
    }
    
    public double getBondTheta() {
        return bondTheta;
    }
    
    public void init() {
        IVector vector1 = space.makeVector();
        vector1.setX(0, bondL);
        IVector vector2 = space.makeVector();
        vector2.setX(0, bondL*Math.cos(bondTheta));
        vector2.setX(1, bondL*Math.sin(bondTheta));
        conformation = new ConformationChainZigZag2(space, vector1, vector2);
    }
    
    public ISpecies makeSpecies(ISimulation sim, ISpace _space) {
        SpeciesAlkane species = new SpeciesAlkane(sim, _space, nA,CH3element, CH2element);
        species.setConformation(conformation);
        return species;
    }
    
    private static final long serialVersionUID = 1L;
    protected static final double nominalBondL = 1.54;
    protected static final double nominalBondTheta = Math.PI*114/180;
    protected final ISpace space;
    protected double bondL;
    protected double bondTheta;
    private final int nA;
    private final ElementSimple CH3element;
    private final ElementSimple CH2element;
    //private final int element2;
    private ConformationChainZigZag2 conformation;

}

