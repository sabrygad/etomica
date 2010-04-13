package etomica.modules.colloid;

import etomica.api.IAtom;
import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialAtomic;
import etomica.api.ISpecies;
import etomica.nbr.list.AtomNeighborLists;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.space.ISpace;

public class NeighborListManagerColloid extends NeighborListManager {

    public NeighborListManagerColloid(PotentialMasterList potentialMasterList,
            double range, IBox box, ISpace space) {
        super(potentialMasterList, range, box, space);
    }
    
    public void setSpeciesColloid(ISpecies newSpeciesColloid) {
        speciesColloid = newSpeciesColloid;
    }
    
    public void setPotentialMC(IPotentialAtomic newPotentailMC) {
        p2mc = newPotentailMC;
    }
    
    public void setSpeciesMonomer(ISpecies newSpeciesMonomer) {
        speciesMonomer = newSpeciesMonomer;
    }
    
    public void setPotentialPseudo(IPotentialAtomic newPotentailPseudo) {
        p2pseudo = newPotentailPseudo;
    }
    
    public void setChainLength(int newChainLength) {
        chainLength = newChainLength;
    }
    
    protected void neighborSetup() {
        super.neighborSetup();
        
        IAtom colloidAtom = box.getMoleculeList(speciesColloid).getMolecule(0).getChildList().getAtom(0);
        int p2idx = potentialMaster.getRangedPotentials(colloidAtom.getType()).getPotentialIndex(p2mc);
        IMoleculeList monomers = box.getMoleculeList(speciesMonomer);
        if (monomers.getMoleculeCount() == 0) {
            return;
        }
        int p2idx2 = potentialMaster.getRangedPotentials(monomers.getMolecule(0).getChildList().getAtom(0).getType()).getPotentialIndex(p2mc);

        for (int i=0; i<monomers.getMoleculeCount(); i++) {
            IAtom atom = monomers.getMolecule(i).getChildList().getAtom(0);
            ((AtomNeighborLists)agentManager2Body.getAgent(colloidAtom)).addUpNbr(atom,p2idx);
            ((AtomNeighborLists)agentManager2Body.getAgent(atom)).addDownNbr(colloidAtom,p2idx2);
        }
        
        p2idx = potentialMaster.getRangedPotentials(monomers.getMolecule(0).getChildList().getAtom(0).getType()).getPotentialIndex(p2pseudo);
        for (int i=0; i<monomers.getMoleculeCount(); i+=chainLength) {
            IAtom iAtom = monomers.getMolecule(i).getChildList().getAtom(0);
            for (int j=i+chainLength; j<monomers.getMoleculeCount(); j+=chainLength) {
                IAtom jAtom = monomers.getMolecule(j).getChildList().getAtom(0);
                // iAtom and jAtom are both bonded to the colloid.  we need to keep them apart with p2seudo
                ((AtomNeighborLists)agentManager2Body.getAgent(iAtom)).addUpNbr(jAtom,p2idx);
                ((AtomNeighborLists)agentManager2Body.getAgent(jAtom)).addDownNbr(iAtom,p2idx2);
            }
        }
    }

    protected ISpecies speciesColloid, speciesMonomer;
    protected IPotentialAtomic p2mc, p2pseudo;
    protected int chainLength;

    public static class NeighborListAgentSourceColloid extends PotentialMasterList.NeighborListAgentSource {
        public NeighborListAgentSourceColloid(double range, ISpace space) {
            super(range, space);
        }

        public Object makeAgent(IBox box) {
            return new NeighborListManagerColloid(potentialMaster, range, box, space);
        }
    }
}