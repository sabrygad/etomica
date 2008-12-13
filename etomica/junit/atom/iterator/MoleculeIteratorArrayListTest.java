package etomica.junit.atom.iterator;

import java.util.LinkedList;

import etomica.api.IAtomTypeLeaf;
import etomica.api.ISpecies;
import etomica.atom.Molecule;
import etomica.atom.MoleculeArrayList;
import etomica.atom.iterator.MoleculeIteratorArrayListSimple;
import etomica.junit.UnitTestUtil;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresHetero;

/**
 * Unit test for AtomIteratorArrayList.
 */
public class MoleculeIteratorArrayListTest extends MoleculeIteratorTestAbstract {

    public MoleculeIteratorArrayListTest() {
        super();
        UnitTestUtil.VERBOSE = false;
    }
    
    public void testListVariations() {
        ISpecies species = new SpeciesSpheresHetero(Space3D.getInstance(), false, new IAtomTypeLeaf[0]);
        MoleculeIteratorArrayListSimple iterator = new MoleculeIteratorArrayListSimple();
        
        //make sure new iterator gives no iterates
        LinkedList list = generalIteratorMethodTests(iterator);
        assertEquals(list.size(), 0);
        
        // make empty list to start
        MoleculeArrayList atomList = new MoleculeArrayList();
        iterator.setList(atomList);
        
        //add some atoms and check each time
        for(int i=0; i<=10; i++) {
            list = generalIteratorMethodTests(iterator);
            assertEquals(list.size(), i);
            atomList.add(new Molecule(species));
        }
        list = generalIteratorMethodTests(iterator);
        
        //check that setList changes list
        MoleculeArrayList arrayList = new MoleculeArrayList();
        iterator.setList(arrayList);
        list = generalIteratorMethodTests(iterator);
        assertEquals(list.size(), 0);
        arrayList.add(new Molecule(species));
        list = generalIteratorMethodTests(iterator);
        assertEquals(list.size(), 1);
        
        //check handling of null list
        iterator.setList(null);
        boolean exceptionThrown = false;
        try {
            list = generalIteratorMethodTests(iterator);
        }
        catch (RuntimeException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        
    }

}
