package etomica;

/**
 * Iterates over the child atoms (which are not necessarily 
 * the leaf atoms) of a sequence of atoms.
 * Parent atoms themselves are not returned in the sequence.
 * Used by Phase to iterate over molecules (direct children of
 * species agents).
 *
 * @author David Kofke
 */
 
public final class AtomIteratorChildren implements AtomIterator {
    
    private AtomIterator parentIterator;
    private boolean hasNext;
    private final IteratorDirective directive = new IteratorDirective();
    private boolean setAsNeighbor = false;
    private AtomIterator currentIterator;
    
    /**
     * @param iterator the iterator of the parent atoms.
     */      
    public AtomIteratorChildren(AtomGroup group) {
        parentIterator = new AtomIteratorSequential(group);
    }
    
    public boolean hasNext() {return hasNext;}
    
    public void setBasis(Atom a) {
        parentIterator.setBasis(a);
    }
    
    public boolean contains(Atom atom) {
        parentIterator.reset();
        while(parentIterator.hasNext()) {
            if(atom.parentGroup() == parentIterator.next()) return true;
        }
        return false;
    }
    
    public Atom reset(IteratorDirective id) {
        directive.copy(id);
        parentIterator.reset(id);
        return reset();
    }
    
    public void setAsNeighbor(boolean b) {
        parentIterator.setAsNeighbor(b);
        setAsNeighbor = b;
    }
    
    public Atom reset() {
        Atom atom = null;
        parentIterator.setAsNeighbor(setAsNeighbor);
        parentIterator.reset();
        while(parentIterator.hasNext()) {
            currentIterator.setBasis(parentIterator.next());
            currentIterator.setAsNeighbor(setAsNeighbor);
            atom = currentIterator.reset(directive);
            if(currentIterator.hasNext()) {hasNext = true; break;}
        }
        return atom;
    }
    
    public Atom next() {
        Atom next = currentIterator.next();
        while(!currentIterator.hasNext()) {
            if(parentIterator.hasNext()) {
                currentIterator.setBasis(parentIterator.next());
                currentIterator.reset(directive);
            }
            else {
                hasNext = false;
                break;
            }
        }
        return next;
    }//end of next
    
    //not implemented
    public void allAtoms(AtomAction act) {
    }
}//end of AtomIteratorChildren