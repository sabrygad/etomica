package etomica.config;

import etomica.api.IVector;
import etomica.space.ISpace;

public class ConformationChainZigZag2 extends ConformationChain {
	
    public ConformationChainZigZag2(ISpace space){
		super(space);
		
		v1 = space.makeVector();
		v2 = space.makeVector();
		isVectorOne = true;
	}
	public ConformationChainZigZag2(ISpace space, IVector vect1, IVector vect2){
		super(space);
		v1 = space.makeVector();
		v2 = space.makeVector();
		
		v1.E(vect1);
		v2.E(vect2);
		
		isVectorOne = true;
	}
	
	/* (non-Javadoc)
	 * @see etomica.ConformationChain#reset()
	 */
	protected void reset() {
		isVectorOne = true;
	}

	/* (non-Javadoc)
	 * @see etomica.ConformationChain#nextVector()
	 */
	protected IVector nextVector() {
		if(isVectorOne){
			isVectorOne = false;
			return v1;
		}
		isVectorOne = true;
		return v2;
	}
	
	public IVector getFirstVector() {
	    return v1;
	}
	
	public IVector getSecondVector() {
	    return v2;
	}

    private static final long serialVersionUID = 1L;
	protected IVector v1;
	protected IVector v2;
	boolean isVectorOne;
   }

