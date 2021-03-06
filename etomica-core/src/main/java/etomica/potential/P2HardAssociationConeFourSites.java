/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.potential;
import etomica.api.IAtomList;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.IAtomOriented;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.space3d.IOrientationFull3D;
import etomica.units.Angle;
import etomica.units.Dimension;
import etomica.units.Energy;
import etomica.units.Length;
import etomica.units.Null;

/**
 * Lennard-Jones potential with a square-well cone of attraction. 
 * Four association sites
 *
 * @author Hye Min Kim
 */

public class P2HardAssociationConeFourSites extends Potential2 implements Potential2Soft {
    private static final long serialVersionUID = 1L;
    public static boolean FLAG = false;
    private double wellcutoffFactor;
    private double wellCutoffSquared;
    private double sigma, sigmaSquared;
    private double epsilon, epsilon4, wellEpsilon;
    private double cutoffLJSquared, cutoffFactor;
    private double ec1, ec2;
    private final IVectorMutable dr;
    private IBoundary boundary;
    
    public P2HardAssociationConeFourSites(ISpace space, double sigma, double epsilon, double cutoffFactorLJ, double wellConstant) {
        super(space);
        dr = space.makeVector();

        setSigma(sigma);
        setEpsilon(epsilon);
        setCutoffFactorLJ(cutoffFactorLJ);
        setWellCutoffFactor(1.0);
        setWellEpsilon(wellConstant*getEpsilon());
        setTheta(etomica.units.Degree.UNIT.toSim(27.0));
    }

    /**
     * Returns infinity.
     */
    public double getRange() {
        return sigma*cutoffFactor;
    }


    /**
     * Returns the pair potential energy.
     */
    public double energy(IAtomList atoms) {
        IAtomOriented atom0 = (IAtomOriented)atoms.getAtom(0);
        IAtomOriented atom1 = (IAtomOriented)atoms.getAtom(1);
        dr.Ev1Mv2(atom1.getPosition(),atom0.getPosition());
        boundary.nearestImage(dr);
        double r2 = dr.squared();
        double eTot = 0.0;
                 
       // FLAG = false;
        if(r2 > cutoffLJSquared) {
            eTot = 0.0;
        }
        else {
            double s2 = sigmaSquared/r2;
            double s6 = s2*s2*s2;
            eTot = epsilon4*s6*(s6 - 1.0);
        }
                  
        if (r2 < wellCutoffSquared) {
        	double angle1 = Math.acos(-1.0/3.0);//theta
        	double cosAngle1 = -1.0/3.0;
        	double sinAngle1 = Math.sin(angle1);
        	double angle2 = (Math.PI-Math.acos(-1.0/3.0))/2.0;//(pi-theta)/2
        	double length = 2*Math.cos(angle2);//length of tetrahedron
        	double coordX = 0.5*(2-length*length);
            double coordY = coordX*(1-cosAngle1)/sinAngle1;
            double coordZ = Math.sqrt(1-coordX*coordX-coordY*coordY);
        	IVector e1A = atom0.getOrientation().getDirection();
            double er1Aa = e1A.dot(dr);//vector of site Aa on atom0  
            IVector e1AaY = ((IOrientationFull3D) atom0.getOrientation()).getSecondaryDirection();//Perpendicular direction of e1A
            IVectorMutable e1AaZ = space.makeVector();
            e1AaZ.E(e1A);
            ((IVectorMutable) e1AaZ).XE(e1AaY);//crossproduct
            IVectorMutable e1Ab = space.makeVector();
            e1Ab.Ea1Tv1(cosAngle1, e1A);
            e1Ab.PEa1Tv1(sinAngle1, e1AaY);
            double er1Ab = e1Ab.dot(dr);//vector of site Ab on atom0
            IVectorMutable e1Ba = space.makeVector();
            e1Ba.Ea1Tv1(coordX, e1A);
            e1Ba.PEa1Tv1(coordY, e1AaY);
            e1Ba.PEa1Tv1(coordZ, e1AaZ);
            double er1Ba = e1Ba.dot(dr);//vector of site Ba on atom0
            IVectorMutable e1Bb = space.makeVector();
            e1Bb.Ea1Tv1(coordX, e1A);
            e1Bb.PEa1Tv1(coordY, e1AaY);
            e1Bb.PEa1Tv1(-coordZ, e1AaZ);
            double er1Bb = e1Bb.dot(dr);//vector of site Bb on atom0
            IVector e2A = atom1.getOrientation().getDirection();
            double er2Aa = e2A.dot(dr);//vector of site Aa on atom1
            IVector e2AaY = ((IOrientationFull3D) atom1.getOrientation()).getSecondaryDirection();//Perpendicular direction of e2A         
            IVectorMutable e2AaZ = space.makeVector();
            e2AaZ.E(e2A);
            ((IVectorMutable) e2AaZ).XE(e2AaY);//crossproduct
            IVectorMutable e2Ab = space.makeVector();
            e2Ab.Ea1Tv1(cosAngle1, e2A);
            e2Ab.PEa1Tv1(sinAngle1, e2AaY);
            double er2Ab = e2Ab.dot(dr);//vector of site Ab on atom1
            IVectorMutable e2Ba = space.makeVector();
            e2Ba.Ea1Tv1(coordX, e2A);
            e2Ba.PEa1Tv1(coordY, e2AaY);
            e2Ba.PEa1Tv1(coordZ, e2AaZ);
            double er2Ba = e2Ba.dot(dr);//vector of site Ba on atom1
            IVectorMutable e2Bb = space.makeVector();
            e2Bb.Ea1Tv1(coordX, e2A);
            e2Bb.PEa1Tv1(coordY, e2AaY);
            e2Bb.PEa1Tv1(-coordZ, e2AaZ);
            double er2Bb = e2Bb.dot(dr);//vector of site Bb on atom0
            
            if (er1Aa*er2Aa < 0.0 ||er1Aa*er2Ab < 0.0||er1Ab*er2Aa < 0.0||er1Ab*er2Ab < 0.0||er1Ba*er2Ba < 0.0 ||er1Ba*er2Bb < 0.0||er1Bb*er2Ba < 0.0||er1Bb*er2Bb < 0.0 ){
            	eTot -= 0.0;
            } 
            if((er1Aa*er2Ba < 0.0 && er1Aa*er1Aa > ec2*r2 && er2Ba*er2Ba > ec2*r2)|| (er1Aa*er2Bb < 0.0 && er1Aa*er1Aa > ec2*r2 && er2Bb*er2Bb > ec2*r2)
            	||(er1Ab*er2Ba < 0.0 && er1Ab*er1Ab > ec2*r2 && er2Ba*er2Ba > ec2*r2)||(er1Ab*er2Bb < 0.0 && er1Ab*er1Ab > ec2*r2 && er2Bb*er2Bb > ec2*r2)
            	||(er1Ba*er2Aa < 0.0 && er1Ba*er1Ba > ec2*r2 && er2Aa*er2Aa > ec2*r2)|| (er1Ba*er2Ab < 0.0 && er1Ba*er1Ba > ec2*r2 && er2Ab*er2Ab > ec2*r2)
            	||(er1Bb*er2Aa < 0.0 && er1Bb*er1Bb > ec2*r2 && er2Aa*er2Aa > ec2*r2)||(er1Bb*er2Ab < 0.0 && er1Bb*er1Bb > ec2*r2 && er2Ab*er2Ab > ec2*r2)) eTot -= wellEpsilon;
                //if(er2 < 0.0 && er2*er2 > ec2*r2) {
                	//System.out.println ("haha " + eTot);
                	//if (eTot < -2) {
                		//FLAG = true;
                	//}
                //}
        }
        return eTot;
    }
    
    /**
     * Accessor method for Lennard-Jones size parameter
     */
    public double getSigma() {return sigma;}
    /**
     * Accessor method for Lennard-Jones size parameter
     */
    public void setSigma(double s) {
        sigma = s;
        sigmaSquared = s*s;
        setCutoffFactorLJ(cutoffFactor);
    }
    public static final Dimension getSigmaDimension() {return Length.DIMENSION;}

    /**
    * Accessor method for Lennard-Jones cutoff distance; divided by sigma
    * @return cutoff distance, divided by size parameter (sigma)
    */
    public double getCutoffFactorLJ() {return cutoffFactor;}
    /**
     * Accessor method for Lennard-Jones cutoff distance; divided by sigma
     * @param rc cutoff distance, divided by size parameter (sigma)
     */
    public void setCutoffFactorLJ(double rc) {  
        cutoffFactor = rc;
        double cutoffLJ = sigma*cutoffFactor;
        cutoffLJSquared = cutoffLJ*cutoffLJ;
    }
    public static final Dimension getCutoffFactorLJDimension() {return Null.DIMENSION;}
   
    /**
    * Accessor method for attractive-well diameter divided by sigma
    */
    public double getWellCutoffFactor() {return wellcutoffFactor;}
    /**
    * Accessor method for attractive-well diameter divided by sigma;
    */
    public void setWellCutoffFactor(double wcut) {
        wellcutoffFactor = wcut;
        double wellCutoff = sigma*wcut;
        wellCutoffSquared = wellCutoff*wellCutoff;
    }
          
    public static final Dimension getWellCutoffFactorDimension() {return Null.DIMENSION;}

    /**
    * Accessor method for Lennard-Jones energy parameter
    */ 
    public double getEpsilon() {return epsilon;}
    /**
    * Accessor method for depth of well
    */
    public void setEpsilon(double eps) {
        epsilon = eps;
        epsilon4 = 4.0 * eps;
    }
    public static final Dimension getEpsilonDimension() {return Energy.DIMENSION;}
    
    /**
    * Accessor method for attractive-well depth parameter.
    */
    public double getWellEpsilon() {return wellEpsilon;}
    /**
    * Accessor method for attractive-well depth parameter.
    */
    public void setWellEpsilon(double weps) {wellEpsilon = weps;}
          
    public static final Dimension getWellEpsilonDimension() {return Energy.DIMENSION;}
    
    /**
     * Accessor method for angle describing width of cone.
     */
    public double getTheta() {return Math.acos(ec1);}
    
    /**
     * Accessor method for angle (in radians) describing width of cone.
     */
    public void setTheta(double t) {
        ec1   = Math.cos(t);
        ec2   = ec1*ec1;
    }
    public Dimension getThetaDimension() {return Angle.DIMENSION;}

    public void setBox(IBox box) {
        boundary = box.getBoundary();
    }

	public double hyperVirial(IAtomList pair) {
		return 0;
	}

	public double integral(double rc) {
		return 0;
	}

	public double u(double r2) {
		return 0;
	}

    public double du(double r2) {
        return 0;
    }

	public IVector[] gradient(IAtomList atoms) {
		return null;
	}

	public IVector[] gradient(IAtomList atoms, Tensor pressureTensor) {
		return null;
	}

	public double virial(IAtomList atoms) {
		return 0;
	}
}
