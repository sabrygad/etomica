/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.virial;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IVectorMutable;
import etomica.config.IConformation;
import etomica.space.ISpace;

 /**
  *  Conformation for Anthracene
  *  Reference paper: Iwai
  * 3 site linear, group H to the two side benzene rings 
 * @author Shu Yang
 * date: March,7,2011
 */
public class ConformationAnthracene3site implements IConformation, java.io.Serializable{
	
	public ConformationAnthracene3site(ISpace space){
		this.space = space;
		vector = space.makeVector();
	}

	public void initializePositions(IAtomList atomList) {
			
		IAtom n1 = atomList.getAtom(SpeciesAnthracene3site545.indexC1);
		n1.getPosition().E(new double[] {0, 0, 0});
		
		IAtom n2 = atomList.getAtom(SpeciesAnthracene3site545.indexCH1);
		n2.getPosition().E(new double[] {-bondlength, 0, 0});
		
		IAtom n3 = atomList.getAtom(SpeciesAnthracene3site545.indexCH2);
		n3.getPosition().E(new double[] {bondlength, 0, 0});
		
		
	}
		
	protected final ISpace space;
	protected static final double bondlength = 2.42;//converst nm to angstrom

	
	protected IVectorMutable vector;
	
	private static final long serialVersionUID = 1L;
}
