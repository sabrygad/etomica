/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.models.nitrogen;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.config.IConformation;
import etomica.space.ISpace;
import etomica.units.Electron;

 /**
  *  Conformation for Nitrogen
  *  Reference paper: Etters RD. et al, Phys. Rev B 33(12) 1986
  * 
 * @author Tai Boon Tan
 *
 */
public class ConformationNitrogen implements IConformation, java.io.Serializable{
	
	public ConformationNitrogen(ISpace space){
		this.space = space;
		vector = space.makeVector();
	}

	public void initializePositions(IAtomList atomList) {
			
		IAtom n1 = atomList.getAtom(SpeciesN2.indexN1);
		n1.getPosition().E(new double[] {-bondOrigN, 0, 0});
		
		IAtom n2 = atomList.getAtom(SpeciesN2.indexN2);
		n2.getPosition().E(new double[] {bondOrigN, 0, 0});
		
		IAtom p1Left = atomList.getAtom(SpeciesN2.indexP1left);
		p1Left.getPosition().E(new double[] {-bondOrigP1, 0, 0});
		
		IAtom p2Left = atomList.getAtom(SpeciesN2.indexP2left);
		p2Left.getPosition().E(new double[] {-bondOrigP2, 0, 0});
		
		IAtom p1Right = atomList.getAtom(SpeciesN2.indexP1right);
		p1Right.getPosition().E(new double[] {bondOrigP1, 0, 0});
		
		IAtom p2Right = atomList.getAtom(SpeciesN2.indexP2right);
		p2Right.getPosition().E(new double[] {bondOrigP2, 0, 0});
		 
		
	}
	
	public void initializePositions(IAtomList atomList, IVector v) {
        
        IAtom n1 = atomList.getAtom(SpeciesN2.indexN1);
        n1.getPosition().Ea1Tv1(-bondOrigN, v);
        
        IAtom n2 = atomList.getAtom(SpeciesN2.indexN2);
        n2.getPosition().Ea1Tv1(bondOrigN, v);
        
        IAtom p1Left = atomList.getAtom(SpeciesN2.indexP1left);
        p1Left.getPosition().Ea1Tv1(-bondOrigP1, v);
        
        IAtom p2Left = atomList.getAtom(SpeciesN2.indexP2left);
        p2Left.getPosition().Ea1Tv1(-bondOrigP2, v);
        
        IAtom p1Right = atomList.getAtom(SpeciesN2.indexP1right);
        p1Right.getPosition().Ea1Tv1(bondOrigP1, v);
        
        IAtom p2Right = atomList.getAtom(SpeciesN2.indexP2right);
        p2Right.getPosition().Ea1Tv1(bondOrigP2, v);
         
    }
	
    public final static double [] Echarge = new double [6];
    static {
        ConformationNitrogen.Echarge[SpeciesN2.indexN1] = Electron.UNIT.toSim( 0.0);
        ConformationNitrogen.Echarge[SpeciesN2.indexN2] = Electron.UNIT.toSim( 0.0);
        ConformationNitrogen.Echarge[SpeciesN2.indexP1left] = Electron.UNIT.toSim(-0.373);
        ConformationNitrogen.Echarge[SpeciesN2.indexP2left] = Electron.UNIT.toSim( 0.373);
        ConformationNitrogen.Echarge[SpeciesN2.indexP1right] = Electron.UNIT.toSim(-0.373);
        ConformationNitrogen.Echarge[SpeciesN2.indexP2right] = Electron.UNIT.toSim( 0.373);
    }
	
	protected final ISpace space;
	protected static final double bondOrigN = 0.547;
	protected static final double bondOrigP2 = 0.847;
	protected static final double bondOrigP1 = 1.044;
	
	protected IVectorMutable vector;
	
	private static final long serialVersionUID = 1L;
}
