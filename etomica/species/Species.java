package etomica.species;

import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomType;
import etomica.api.IConformation;
import etomica.api.IMolecule;
import etomica.api.ISpecies;
import etomica.util.Arrays;

/**
 * Type for atom that is a group of other atoms, and for which its node is an
 * instance of AtomTreeNodeGroup.
 * 
 * @author andrew
 */
public abstract class Species implements ISpecies {

    protected int index;

    /* (non-Javadoc)
     * @see etomica.atom.IAtomType#setIndex(int)
     */
    public void setIndex(int newIndex) {
        index = newIndex;
    }

    /* (non-Javadoc)
     * @see etomica.atom.IAtomType#getIndex()
     */
    public int getIndex() {
        return index;
    }

    /**
     * Simple invokes parent constructor with same arguments.
     */
    public Species(IAtomPositionDefinition positionDefinition) {
        super();
        this.positionDefinition = positionDefinition;
    }

    /* (non-Javadoc)
     * @see etomica.atom.IAtomTypeMolecule#removeChildType(etomica.atom.AtomTypeLeaf)
     */
    public void removeChildType(IAtomType removedType) {
        boolean success = false;
        for (int i=0; i<childTypes.length; i++) {
            if (childTypes[i] == removedType) {
                success = true;
                break;
            }
        }
        if (!success) {
            throw new IllegalArgumentException("AtomType "+removedType+" is not my child!");
        }
        childTypes = (IAtomType[])Arrays.removeObject(childTypes,removedType);
        for (int i = 0; i < childTypes.length; i++) {
            childTypes[i].setChildIndex(i);
        }
    }
    
    /**
     * @return Returns the species.
     */
    public ISpecies getSpecies() {
        return this;
    }

    public IAtomType getAtomType(int index) {
    	return childTypes[index];
    }

    public int getAtomTypeCount() {
    	return childTypes.length;
    }

    /* (non-Javadoc)
     * @see etomica.atom.IAtomTypeMolecule#addChildType(etomica.atom.AtomTypeLeaf)
     */
    public void addChildType(IAtomType newChildType) {
        if (newChildType.getSpecies() != null) {
            throw new IllegalArgumentException(newChildType+" already has a parent");
        }
        newChildType.setSpecies(this);
        newChildType.setChildIndex(childTypes.length);
        childTypes = (IAtomType[]) Arrays.addObject(childTypes, newChildType);
    }
    
    /* (non-Javadoc)
     * @see etomica.atom.IAtomTypeMolecule#setConformation(etomica.config.Conformation)
     */
    public void setConformation(IConformation config) {
        conformation = config;
    }
    
    /* (non-Javadoc)
     * @see etomica.atom.IAtomTypeMolecule#getConformation()
     */
    public IConformation getConformation() {return conformation;}
 
    public void initializeConformation(IMolecule molecule) {
        conformation.initializePositions(molecule.getChildList());
    }
    
    /* (non-Javadoc)
     * @see etomica.atom.IAtomType#getPositionDefinition()
     */
    public IAtomPositionDefinition getPositionDefinition() {
        return positionDefinition;
    }

    /* (non-Javadoc)
     * @see etomica.atom.IAtomType#setPositionDefinition(etomica.atom.AtomPositionDefinition)
     */
    public void setPositionDefinition(IAtomPositionDefinition newPositionDefinition) {
        positionDefinition = newPositionDefinition;
    }

    private static final long serialVersionUID = 2L;
    protected IConformation conformation;
    protected IAtomPositionDefinition positionDefinition;
    protected IAtomType[] childTypes = new IAtomType[0];
}