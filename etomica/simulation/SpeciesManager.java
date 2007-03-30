package etomica.simulation;

import java.util.HashMap;
import java.util.LinkedList;

import etomica.atom.AtomAddressManager;
import etomica.atom.AtomType;
import etomica.atom.AtomTypeGroup;
import etomica.atom.AtomTypeLeaf;
import etomica.atom.AtomTypePhase;
import etomica.chem.elements.Element;
import etomica.phase.Phase;
import etomica.species.Species;
import etomica.util.Arrays;

/**
 * The SpeciesManager manages Species and AtomTypes on behalf of the
 * Simulation.
 * 
 * @author Andrew Schultz
 */
public class SpeciesManager implements java.io.Serializable {

    public SpeciesManager(Simulation sim, int[] bitLength) {
        this.sim = sim;
        speciesList = new Species[0];
        numAtomTypes = 0;
        elementSymbolHash = new HashMap();
        elementAtomTypeHash = new HashMap();
        typeReservoirCount = -1;
        
        speciesMasterType = new AtomTypePhase(AtomAddressManager.makeRootIndexManager(bitLength), this);
    }

    /**
     * Adds species to the list of all species in the simulation, and
     * adds new species agent to every phase currently in simulation.
     * This is called by the Species constructor.
     * 
     * @return the index assigned to the new species
     */
    public void addSpecies(Species species) {
        for (int i=0; i<speciesList.length; i++) {
            if (speciesList[i] == species) {
                throw new IllegalArgumentException("Species already exists");
            }
        }
        speciesList = (Species[])Arrays.addObject(speciesList,species);
        Phase[] phaseList = sim.getPhases();
        for (int i=0; i<phaseList.length; i++) {
            species.makeAgent(phaseList[i].getSpeciesMaster());
        }

        sim.getEventManager().fireEvent(new SimulationSpeciesAddedEvent(species));
    }

    /**
     * Removes the given AtomTypes associated with the given Species from the 
     * Simulation and does cleanup, including renumbering indices and firing 
     * AtomType-related event notifications.
     */
    public boolean removeSpecies(Species removedSpecies) {
        boolean success = false;
        for (int i=0; i<speciesList.length; i++) {
            if (speciesList[i] == removedSpecies) {
                success = true;
                break;
            }
        }
        if (!success) {
            return false;
        }

        sim.getEventManager().fireEvent(new SimulationSpeciesRemovedEvent(removedSpecies));
        
        speciesList = (Species[])Arrays.removeObject(speciesList,removedSpecies);
        Phase[] phaseList = sim.getPhases();
        for (int i=0; i<phaseList.length; i++) {
            phaseList[i].getSpeciesMaster().removeSpecies(removedSpecies);
        }
        
        AtomType[] agentTypes = speciesMasterType.getChildTypes();
        
        for (int i=0; i<agentTypes.length; i++) {
            if (agentTypes[i].getSpecies() == removedSpecies) {
                AtomTypeGroup removedType = (AtomTypeGroup)agentTypes[i];
                speciesMasterType.removeChildType(removedType);
                break;
            }
        }
        
        sim.getEventManager().fireEvent(new SimulationAtomTypeMaxIndexEvent(numAtomTypes));
        return true;
    }

    /**
     * Returns an array of the Species in the Simulation.
     */
    public Species[] getSpecies() {
        return speciesList;
    }

    /**
     * This method notifies the SpeciesManager that the give atomType was added
     * to the system.  This method should be called by the AtomType at the top
     * of the AtomType hierarchy whenver it receives notification of a new
     * AtomType.
     */
    public void atomTypeAddedNotify(AtomType newChildType) {
        if (newChildType instanceof AtomTypeLeaf) {
            Element newElement = ((AtomTypeLeaf)newChildType).getElement();
            Element oldElement = (Element)elementSymbolHash.get(newElement.getSymbol());
            if (oldElement != null && oldElement != newElement) {
                // having two AtomTypes with the same Element is OK, but having
                // two Elements with the same symbol is not allowed.
                throw new IllegalStateException("Element symbol "+newElement.getSymbol()+" already exists in this simulation as a different element");
            }
            // remember the element so we can check for future duplication
            elementSymbolHash.put(newElement.getSymbol(), newElement);
            LinkedList atomTypeList = (LinkedList)elementAtomTypeHash.get(newElement);
            if (atomTypeList == null) {
                atomTypeList = new LinkedList();
                elementAtomTypeHash.put(newElement, atomTypeList);
            }
            atomTypeList.add(newChildType);
        }

        sim.getEventManager().fireEvent(new SimulationAtomTypeAddedEvent(newChildType));
    }

    /**
     * Reassigns indices from the reservoir to the given AtomTypes.
     */
    private void recycleIndices(AtomType[] atomTypes) {
        // now iterate over remaining AtomTypes and re-use old indices that are
        // less than remaining indices
        for (int i=0; i<atomTypes.length; i++) {
            if (atomTypes[i].getIndex() >= numAtomTypes) {
                int oldIndex = atomTypes[i].getIndex();
                // this triggers a call back to our requestIndex method, which will
                // return an index from the reservoir
                atomTypes[i].resetIndex();
                sim.getEventManager().fireEvent(new SimulationAtomTypeIndexChangedEvent(atomTypes[i], oldIndex));
                if (typeReservoirCount == typeIndexReservoir.length) {
                    // we ran out of indices to recycle
                    return;
                }
            }
            if (atomTypes[i] instanceof AtomTypeGroup) {
                recycleIndices(((AtomTypeGroup)atomTypes[i]).getChildTypes());
                if (typeReservoirCount == typeIndexReservoir.length) {
                    // we ran out of indices to recycle
                    return;
                }
            }
        }
    }

    /**
     * Returns an array of indices for the give parent AtomType.  The array
     * of indices does not include the given parent's index.
     */
    private static int[] getChildIndices(AtomTypeGroup atomType) {
        int[] childIndices = new int[0];
        for (int i=0; i<atomType.getChildTypes().length; i++) {
            AtomType childType = atomType.getChildTypes()[i];
            if (childType instanceof AtomTypeGroup) {
                // recursion!
                int[] iChildIndices = getChildIndices((AtomTypeGroup)childType);
                int oldSize = childIndices.length;
                int newSize = oldSize + iChildIndices.length + 1;
                childIndices = Arrays.resizeArray(childIndices, newSize);
                childIndices[oldSize] = childType.getIndex();
                System.arraycopy(iChildIndices, 0, childIndices, oldSize+1, iChildIndices.length);
            }
            else {
                childIndices = Arrays.resizeArray(childIndices, childIndices.length+1);
                childIndices[childIndices.length-1] = childType.getIndex();
            }
        }
        return childIndices;
    }

    public int requestTypeIndex() {
        if (typeIndexReservoir == null) {
            // if no reservoir, just return the next index
            return ++numAtomTypes;
        }
        // if we have a reservoir, it's because we're in the middle of a
        // recycling pass
        return typeIndexReservoir[typeReservoirCount++];
    }

    public void atomTypeRemovedNotify(AtomType removedType) {
        typeReservoirCount = 0;
        // put the removed AtomType indices in a reservoir
        if (removedType instanceof AtomTypeLeaf) {
            Element oldElement = ((AtomTypeLeaf)removedType).getElement();
            elementSymbolHash.remove(oldElement.getSymbol());
            typeIndexReservoir = new int[]{removedType.getIndex()};
        }
        else if (removedType instanceof AtomTypeGroup) {
            removeElements((AtomTypeGroup)removedType);
            int[] childIndices = null;
            childIndices = getChildIndices((AtomTypeGroup)removedType);
            typeIndexReservoir = new int[childIndices.length+1];
            numAtomTypes -= typeIndexReservoir.length;
            typeIndexReservoir[0] = removedType.getIndex();
            System.arraycopy(childIndices, 0, typeIndexReservoir, 1, childIndices.length);
            java.util.Arrays.sort(typeIndexReservoir);
        }
        // now replace any AtomType index that's higher than a removed index
        // with the removed index.
        
        // we can start at the agent types (our child's childTypes) because 
        // types for the SpeciesMaster never go away
        typeReservoirCount = 0;
        recycleIndices(speciesMasterType.getChildTypes());
        typeIndexReservoir = null;
        typeReservoirCount = -1;
    }

    /**
     * Removes all elements from the element hash which are children of the
     * given parent AtomType
     */
    private void removeElements(AtomTypeGroup oldParentType) {
        AtomType[] oldChildTypes = oldParentType.getChildTypes();
        for (int i=0; i<oldChildTypes.length; i++) {
            if (oldChildTypes[i] instanceof AtomTypeLeaf) {
                Element oldElement = ((AtomTypeLeaf)oldChildTypes[i]).getElement();
                elementSymbolHash.remove(oldElement.getSymbol());
            }
            else if (oldChildTypes[i] instanceof AtomTypeGroup) {
                removeElements((AtomTypeGroup)oldChildTypes[i]);
            }
        }
    }

    /**
     * Returns an Element symbol starting with symbolBase that does not yet 
     * exist in the Simulation.  Return values will be like "base0, base1, base2..." 
     */
    public String makeUniqueElementSymbol(String symbolBase) {
        int n = 0;
        while (elementSymbolHash.containsKey(symbolBase+n)) {
            n++;
        }
        // reserve this symbol so future calls to makeUniqueElementSymbol won't return it
        // this will get repalced by the actual Element when it gets added via childTypeAddedNotify
        elementSymbolHash.put(symbolBase+n, null);
        return symbolBase+n;
    }

    /**
     * @return Returns the AtomType of the SpeciesMaster
     */
    public AtomTypePhase getSpeciesMasterType() {
        return speciesMasterType;
    }

    private static final long serialVersionUID = 1L;
    private Species[] speciesList;
    private final HashMap elementSymbolHash;
    private final HashMap elementAtomTypeHash;
    private int numAtomTypes;
    private int[] typeIndexReservoir;
    private int typeReservoirCount;
    private final AtomTypePhase speciesMasterType;
    private final Simulation sim;
}
