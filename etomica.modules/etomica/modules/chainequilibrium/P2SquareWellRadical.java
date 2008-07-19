package etomica.modules.chainequilibrium;

import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IRandom;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.IAtomKinetic;
import etomica.potential.P2SquareWell;
import etomica.space.ISpace;


/**
 * Similar to square-well potential, but considers and alters bonding states
 * with collisions based on free radical reactions.  Fully reacted atoms are
 * inert.  A radical can react (bond) with a monomer.  If two radicals meet,
 * they can bond or disproportionate (become unreactive without forming a bond)
 * based on the combinationProbability.
 * 
 * @author Andrew Schultz
 */
public class P2SquareWellRadical extends P2SquareWell {

    private static final long serialVersionUID = 1L;
    protected final AtomLeafAgentManager agentManager;
    protected final IRandom random;
    protected double combinationProbability;

	public P2SquareWellRadical(ISpace space, AtomLeafAgentManager aam,
	        double coreDiameter, double lambda, double epsilon, IRandom random) {
		super(space, coreDiameter, lambda, epsilon, true);
        agentManager = aam;
        this.random = random;
	}

	/**
	 * Sets the probability that two radicals will bond instead of becoming
	 * unreactive.
	 */
	public void setCombinationProbability(double newCombinationProbability) {
	    combinationProbability = newCombinationProbability;
	}

	public double getCombinationProbability() {
	    return combinationProbability;
	}

	/**
     * This function will tell the user, if passed an atom weither or not that atom can bond
	 */
	protected boolean isRadical(IAtom a) {
        IAtom[] nbrs = (IAtom[])agentManager.getAgent(a);
		for(int i=0; i < nbrs.length-1; ++i){
			if (nbrs[i] == null) {
				return false;
			}
		}
		return nbrs[nbrs.length-1] == null;
	}
	
    protected boolean isEmpty(IAtom a) {
        IAtom[] nbrs = (IAtom[])agentManager.getAgent(a);
        return nbrs[0] == null;
    }

    /**
     * This will tell you what the lowest open space is in atom a
	 */
	protected int lowest(IAtom a){
        IAtomLeaf[] nbrs = (IAtomLeaf[])agentManager.getAgent(a);
		int j = nbrs.length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (nbrs[i] == null) {
				return i;
			}
		}
		return j; 
	}
	
	/**
     * This function tells you if two atoms are bonded
     * This could probably be public, although a public version would
     * need to first re-retrieve agents
	 */
	protected boolean areBonded(IAtom a, IAtom b){
        IAtomLeaf[] nbrs = (IAtomLeaf[])agentManager.getAgent(a);
		int j = nbrs.length;	//check INDEXING
		for(int i=0; i != j; ++i){
			if (nbrs[i] == b){		
				return true;
			}
		}
		return false; 	
	}
    
	/**
     * this function will bond atoms a & b together
	 */
	protected void bond(IAtomLeaf a, IAtomLeaf b){
		int i = lowest(a);		// (0 is the First Space) 
		int j = lowest(b);
        ((IAtomLeaf[])agentManager.getAgent(a))[i] = b;
        ((IAtomLeaf[])agentManager.getAgent(b))[j] = a;
	}

	/**
     * this function will makes a and b unreactive by setting both to be bonded
     * to themselves (a-a, b-b).
     */
    protected void disproportionate(IAtom a, IAtom b){
        System.out.println("disproportionating "+a+" "+b);
        int i = lowest(a);      // (0 is the First Space) 
        int j = lowest(b);
        ((IAtom[])agentManager.getAgent(a))[i] = a;
        ((IAtom[])agentManager.getAgent(b))[j] = b;
    }

	/**
	 * Computes next time of collision of the two atoms, assuming free-flight
	 * kinematics.
	 */
	public double collisionTime(IAtomSet atoms, double falseTime) {
	
		if (ignoreOverlap) {
			
            IAtomKinetic atom0 = (IAtomKinetic)atoms.getAtom(0);
            IAtomKinetic atom1 = (IAtomKinetic)atoms.getAtom(1);
            dv.Ev1Mv2(atom1.getVelocity(), atom0.getVelocity());
            
            dr.Ev1Mv2(atom1.getPosition(), atom0.getPosition());
            dr.PEa1Tv1(falseTime,dv);
            nearestImageTransformer.nearestImage(dr);

			double r2 = dr.squared();
			if (r2 < wellDiameterSquared) {
        		boolean areBonded = areBonded(atom0, atom1);
                if (!areBonded) {
                    //inside well but not mutually bonded; collide now if approaching
                    return (dr.dot(dv) < 0) ? falseTime : Double.POSITIVE_INFINITY;
                }
			}
		}
		//mutually bonded, or outside well; collide as SW
		return super.collisionTime(atoms, falseTime);
	}

	
	public void bump(IAtomSet pair, double falseTime) {

        IAtomKinetic atom0 = (IAtomKinetic)pair.getAtom(0);
        IAtomKinetic atom1 = (IAtomKinetic)pair.getAtom(1);
        dv.Ev1Mv2(atom1.getVelocity(), atom0.getVelocity());
        
        dr.Ev1Mv2(atom1.getPosition(), atom0.getPosition());
        dr.PEa1Tv1(falseTime,dv);
        nearestImageTransformer.nearestImage(dr);

		double r2 = dr.squared();
		double bij = dr.dot(dv);
		double nudge = 0;
		double eps = 1.0e-10;
		
		// ke is kinetic energy due to components of velocity
		
		double reduced_m = 2.0 / (((IAtomTypeLeaf)atom0.getType()).rm() + ((IAtomTypeLeaf)atom1.getType()).rm());
		
		if (areBonded(atom0,atom1)) {		//atoms are bonded to each
            lastCollisionVirial = reduced_m * bij;
			if (2 * r2 > (coreDiameterSquared + wellDiameterSquared)) {															
				// there is no escape (Mu Ha Ha Ha!), nude back inside
				nudge = -eps;
			}
        }
		else { 	//not bonded to each other
			//well collision; decide whether to bond or have hard repulsion
		    boolean radical0 = isRadical(atom0);
		    boolean radical1 = isRadical(atom1);
            boolean empty0 = isEmpty(atom0);
            boolean empty1 = isEmpty(atom1);
			if (!radical0 && !radical1) {
				lastCollisionVirial = reduced_m * bij;
				nudge = eps;
			}
			else if (radical0 && radical1) {
                //radcial + radical.  terminate
			    // if we're here and empty, we're initiator radical.  at least one of the atoms
			    // has to be monomer radical
			    if ((!empty0 || !empty1) && random.nextDouble() < combinationProbability) {
			        // combine
                    lastCollisionVirial = 0.5* reduced_m* (bij + Math.sqrt(bij * bij + 4.0 * r2 *2* epsilon/ reduced_m));
                    bond((IAtomLeaf)atom0,(IAtomLeaf)atom1);
                    nudge = -eps;
			    }
			    else {
			        // disproportionate
	                lastCollisionVirial = reduced_m * bij;
			        disproportionate(atom0, atom1);
                    nudge = eps;
			    }
		    }
		    else if ((radical0 && empty1) || (radical1 && empty0)) {
			    //one is a radical, the other is a monomer.
                lastCollisionVirial = 0.5* reduced_m* (bij + Math.sqrt(bij * bij + 4.0 * r2 * epsilon/ reduced_m));
				bond((IAtomLeaf)atom0,(IAtomLeaf)atom1);
				nudge = -eps;
		    }
		    else {
		        // one of them is full
	             lastCollisionVirial = reduced_m * bij;
	             nudge = eps;
		    }
		} 

		lastCollisionVirialr2 = lastCollisionVirial / r2;
		dv.Ea1Tv1(lastCollisionVirialr2, dr);
		atom0.getVelocity().PEa1Tv1(((IAtomTypeLeaf)atom0.getType()).rm(), dv);
		atom1.getVelocity().PEa1Tv1(-((IAtomTypeLeaf)atom1.getType()).rm(), dv);
		atom0.getPosition().PEa1Tv1(-falseTime * ((IAtomTypeLeaf)atom0.getType()).rm(), dv);
		atom1.getPosition().PEa1Tv1(falseTime * ((IAtomTypeLeaf)atom1.getType()).rm(), dv);
		
		if (nudge != 0) 
		{
			atom0.getPosition().PEa1Tv1(-nudge, dr);
			atom1.getPosition().PEa1Tv1(nudge, dr);
		}
	}
}