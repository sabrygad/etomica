package etomica.junit.atom.iterator;

import java.util.LinkedList;

import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.atom.AtomArrayList;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.junit.UnitTestUtil;

/**
 * Unit test for ApiIntraspeciesAA
 * 
 * @author David Kofke
 *  
 */
public class AtomIteratorAllMoleculesTest extends IteratorTestAbstract {

    public void testIterator() {

        int[] n0 = new int[] { 10, 1, 0, 0, 0};
        int nA0 = 5;
        int[] n1 = new int[] { 5, 1, 6, 0, 1};
        ISimulation sim = UnitTestUtil.makeStandardSpeciesTree(n0, nA0, n1);

        ISpecies[] species = new ISpecies[sim.getSpeciesManager().getSpeciesCount()];
        for(int i = 0; i < sim.getSpeciesManager().getSpeciesCount(); i++) {
        	species[i] = sim.getSpeciesManager().getSpecie(i);
        }
        for(int i=0; i<n0.length; i++) {
            boxTest(sim.getBox(i), species);
        }

    }

    /**
     * Performs tests on different species combinations in a particular box.
     */
    private void boxTest(IBox box, ISpecies[] species) {
        AtomIteratorAllMolecules iterator = new AtomIteratorAllMolecules();

        iterator.setBox(box);
        
        AtomArrayList moleculeList = new AtomArrayList();
        for(int i=0; i<species.length; i++) {
            IAtomSet molecules = box.getMoleculeList(species[i]);
            for (int j=0; j<molecules.getAtomCount(); j++) {
                moleculeList.add(molecules.getAtom(j));
            }
        }
        
        LinkedList list = testIterates(iterator, moleculeList.toArray());
        assertEquals(list.size(), box.getMoleculeList().getAtomCount());
    }
}
