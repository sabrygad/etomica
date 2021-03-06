package etomica.virial;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.AtomOrientedQuaternion;
import etomica.atom.AtomSource;
import etomica.atom.iterator.AtomIterator;
import etomica.integrator.mcmove.MCMoveAtom;
import etomica.space.ISpace;

public class MCMoveClusterAtomQ extends MCMoveAtom {

    protected final MCMoveAtom mcMove;
    
    public MCMoveClusterAtomQ(IRandom random, ISpace space, MCMoveAtom mcMove) {
        super(random, null, space);
        this.mcMove = mcMove;
    }

    public boolean doTrial() {
        boolean b = mcMove.doTrial();
        if (!b) return false;
        IAtomList leafAtoms = mcMove.getBox().getLeafList();
        int n = leafAtoms.getAtomCount();
        for (int i=0; i<n; i++) {
            double u1 = random.nextDouble();
            double u2 = 2*Math.PI*random.nextDouble();
            double u3 = 2*Math.PI*random.nextDouble();
            double s1 = Math.sqrt(u1);
            double s2 = Math.sqrt(1-u1);
            IVectorMutable q = ((AtomOrientedQuaternion)leafAtoms.getAtom(i)).getQuaternion();
            q.setX(0, s1*Math.sin(u2));
            q.setX(1, s1*Math.cos(u2));
            q.setX(2, s2*Math.sin(u3));
            q.setX(3, s2*Math.cos(u3));
        }
        return true;
    }

    public void setBox(IBox p) {
        mcMove.setBox(p);
    }

    public double getB() {
        return mcMove.getB();
    }

    public double getA() {
        return mcMove.getA();
    }

    public void rejectNotify() {
        mcMove.rejectNotify();
    }

    public void acceptNotify() {
        mcMove.acceptNotify();
    }

    public double energyChange() {
        return mcMove.energyChange();
    }

    public AtomIterator affectedAtoms() {
        return mcMove.affectedAtoms();
    }

    public AtomSource getAtomSource() {
        return mcMove.getAtomSource();
    }

    public void setAtomSource(AtomSource source) {
        mcMove.setAtomSource(source);
    }

    public IBox getBox() {
        return mcMove.getBox();
    }

    public AtomIterator affectedAtoms(IBox aBox) {
        return mcMove.affectedAtoms(aBox);
    }

    public double energyChange(IBox aBox) {
        return mcMove.energyChange(aBox);
    }

    public String toString() {
        return mcMove.toString()+" with random quaternion rotation";
    }
    
    
}
