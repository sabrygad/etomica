package etomica.junit;

import junit.framework.TestCase;
import etomica.atom.Atom;
import etomica.atom.AtomGroup;
import etomica.atom.AtomLeaf;
import etomica.atom.SpeciesAgent;
import etomica.atom.SpeciesMaster;
import etomica.phase.Phase;
import etomica.simulation.Simulation;
import etomica.space2d.Space2D;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheres;
import etomica.species.SpeciesSpheresMono;


/**
 * Performs tests of AtomIndexManager, checking that methods to determine
 * if two atoms are in the same phase, species, molecule, etc., function 
 * correctly.
 */
public class AtomIndexManagerTest extends TestCase {

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        Simulation sim = new Simulation(Space2D.getInstance());
        SpeciesSpheresMono species0 = new SpeciesSpheresMono(sim);
        SpeciesSpheres species1 = new SpeciesSpheres(sim, 5);
        sim.getSpeciesManager().addSpecies(species0);
        sim.getSpeciesManager().addSpecies(species1);
        Phase phase0 = new Phase(sim);
        Phase phase1 = new Phase(sim);
        phase0.getAgent(species0).setNMolecules(20);
        phase0.getAgent(species1).setNMolecules(10);
        phase1.getAgent(species0).setNMolecules(20);
        phase1.getAgent(species1).setNMolecules(10);
        atoms = new Atom[26];
        int i = 0;
        atoms[i++] = master0 = phase0.getSpeciesMaster();//0
        atoms[i++] = master1 = phase1.getSpeciesMaster();//1
        atoms[i++] = agent00 = phase0.getAgent(species0);//2
        atoms[i++] = agent01 = phase0.getAgent(species1);//3
        atoms[i++] = agent10 = phase1.getAgent(species0);//4
        atoms[i++] = agent11 = phase1.getAgent(species1);//5
        AtomGroup[][] node = new AtomGroup[2][2];
        node[0][0] = agent00;
        node[0][1] = agent01;
        node[1][0] = agent10;
        node[1][1] = agent11;
        phaseIndex =    new int[] {0,0,0,0,1,1,1,1, 0,0,0,0,0,0,1,1,1,1,1,1};
        speciesIndex =  new int[] {0,0,1,1,0,0,1,1, 1,1,1,1,1,1,1,1,1,1,1,1};
        moleculeIndex = new int[] {0,5,0,3,0,5,0,4, 0,0,3,3,4,4,0,0,3,3,4,4};
        atomIndex =     new int[] {0,5,0,3,0,5,0,4, 1,4,0,4,0,1,1,4,0,4,0,1};
        i0 = i;
        int del = 8;
        for(int j=0; j<del; j++) {
            atoms[i++] = node[phaseIndex[j]][speciesIndex[j]].getChildList().get(moleculeIndex[j]);//7
        }
        for(int j=del; j<phaseIndex.length; j++) {
            atoms[i++] = node[phaseIndex[j]][speciesIndex[j]].getDescendant(new int[] {moleculeIndex[j], atomIndex[j]});
        }
        atom = new AtomLeaf(Space3D.getInstance());
    }

    public void testGetOrdinal() {
        for(int i=0; i<atomIndex.length; i++) {
//          System.out.println("AtomIndex: "+atoms[i+i0].node.getOrdinal()+" "+(1+atomIndex[i]));
//          System.out.println("Bits: "+Integer.toBinaryString(atoms[i+i0].node.index()));
//          System.out.println(atoms[i+i0].toString());
          assertEquals(atoms[i+i0].getIndex(), atomIndex[i]);
      }
    }

    public void testAncestry() {
        for(int i=0; i<atoms.length; i++) {
            assertFalse(atoms[i].isDescendedFrom(atom));
            assertFalse(atom.isDescendedFrom(atoms[i]));
            Atom speciesMasteri = atoms[i];
            while (!(speciesMasteri instanceof SpeciesMaster)) {
                speciesMasteri = speciesMasteri.getParentGroup();
            }
            for(int j=0; j<atoms.length; j++) {
                Atom speciesMasterj = atoms[j];
                while (!(speciesMasterj instanceof SpeciesMaster)) {
                    speciesMasterj = speciesMasterj.getParentGroup();
                }
                if (speciesMasteri != speciesMasterj) {
                    // different phases, so skip
                    continue;
                }
//                System.out.println(i+" "+j);
                if(isDescendedFrom(atoms[i],atoms[j])) {
                    assertTrue(atoms[i].isDescendedFrom(atoms[j]));
                } else {
                    assertFalse(atoms[i].isDescendedFrom(atoms[j]));
                }
            }
        }
    }
    private boolean isDescendedFrom(Atom a1, Atom a2) {
        if(a1.getType().getAddressManager().getDepth() < a2.getType().getAddressManager().getDepth()) return false;
        else if(a1 == a2) return true;
        else return isDescendedFrom(a1.getParentGroup(), a2);
    }

    
    public void testIsDescendedFrom() {
        for(int i=0; i<atoms.length; i++) {
            assertFalse(atoms[i].getType().isDescendedFrom(atom.getType()));
            assertFalse(atom.getType().isDescendedFrom(atoms[i].getType()));
            Atom speciesMasteri = atoms[i];
            while (!(speciesMasteri instanceof SpeciesMaster)) {
                speciesMasteri = speciesMasteri.getParentGroup();
            }
            for(int j=0; j<atoms.length; j++) {
                Atom speciesMasterj = atoms[j];
                while (!(speciesMasterj instanceof SpeciesMaster)) {
                    speciesMasterj = speciesMasterj.getParentGroup();
                }
                if (speciesMasteri != speciesMasterj) {
                    // different phases, so skip
                    continue;
                }
                boolean is = atoms[i].getType().isDescendedFrom(atoms[j].getType());
                if(typeIsDescendedFrom(atoms[i],atoms[j])) {
                    assertTrue(is);
                } else {
                    if(is) {
                        System.out.println("isDescendedFrom "+i+" "+j+" "+atoms[i]+" "+atoms[j]+" "+Integer.toBinaryString(atoms[i].getType().getAddress())+" "+Integer.toBinaryString(atoms[j].getType().getAddress()));
                        typeIsDescendedFrom(atoms[i],atoms[j]);
                    }
                    assertFalse(is);
                }
            }
        }
    }
    private boolean typeIsDescendedFrom(Atom a1, Atom a2) {
        if(a1.getType().getAddressManager().getDepth() < a2.getType().getAddressManager().getDepth()) return false;
        else if(a1.getType() == a2.getType()) return true;
        else return typeIsDescendedFrom(a1.getParentGroup(), a2);
    }
    
    public void testSameMolecule() {
        int trueCount = 0;
        int falseCount = 0;
        int undefinedCount = 0;
        for(int i=i0; i<atoms.length; i++) {
            if (atoms[i] instanceof SpeciesMaster || atoms[i] instanceof SpeciesAgent) {
                continue;
            }
            if(atoms[i].inSameMolecule(atom)) System.out.println(i+" "+atoms[i]+" "+atom);
            assertFalse(atoms[i].inSameMolecule(atom));
            assertFalse(atom.inSameMolecule(atoms[i]));
            Atom moleculeA = atoms[i];
            while (!(moleculeA.getParentGroup() instanceof SpeciesAgent)) {
                moleculeA = moleculeA.getParentGroup();
            }
            Atom speciesMasteri = atoms[i];
            while (!(speciesMasteri instanceof SpeciesMaster)) {
                speciesMasteri = speciesMasteri.getParentGroup();
            }
            for(int j=i0; j<atoms.length; j++) {
                if (atoms[j] instanceof SpeciesMaster || atoms[j] instanceof SpeciesAgent) {
                    continue;
                }
                Atom speciesMasterj = atoms[j];
                while (!(speciesMasterj instanceof SpeciesMaster)) {
                    speciesMasterj = speciesMasterj.getParentGroup();
                }
                if (speciesMasteri != speciesMasterj) {
                    // different phases, so skip
                    continue;
                }
                Atom moleculeB = atoms[j];
                while (!(moleculeB.getParentGroup() instanceof SpeciesAgent)) {
                    moleculeB = moleculeB.getParentGroup();
                }
                boolean inSameMolecule = atoms[i].inSameMolecule(atoms[j]);
                if(moleculeA == null || moleculeB == null) {
                    if(inSameMolecule) System.out.println(inSameMolecule+" "+i+" "+j+" "+atoms[i]+" "+atoms[j]);
                    assertFalse(inSameMolecule);
                    undefinedCount++;
                } else if(moleculeA == moleculeB) {
                    if(!inSameMolecule) System.out.println(inSameMolecule+" "+i+" "+j+" "+atoms[i]+" "+atoms[j]);
                    assertTrue(inSameMolecule);
                    trueCount++;
                } else {
                    if(inSameMolecule) System.out.println(inSameMolecule+" "+i+" "+j+" "+atoms[i]+" "+atoms[j]);
                    assertFalse(inSameMolecule);
                    falseCount++;
                }
            }
        }
        if(UnitTestUtil.VERBOSE) System.out.println("AtomIndexManagerTest(Molecule): trueCount = "+trueCount+"; falseCount = "+falseCount+"; undefinedCount = "+undefinedCount);
    }

    Atom atom;
    SpeciesAgent agent00, agent01, agent10, agent11;
    SpeciesMaster master0, master1;
    Atom[] atoms;
    int[] moleculeIndex, phaseIndex, speciesIndex, atomIndex;
    int i0;
}
