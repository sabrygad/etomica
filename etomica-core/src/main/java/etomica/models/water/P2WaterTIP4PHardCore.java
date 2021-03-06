
package etomica.models.water;

import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IVectorMutable;
import etomica.potential.PotentialMolecular;
import etomica.space.ISpace;
import etomica.units.Calorie;
import etomica.units.Electron;
import etomica.units.Mole;

/** 
 * 4-point potential for water.  Potential parameters are typically defined
 * by a convenience subclass.
 * 
 * add a hard core for dielectric virial calculation
 * 
 * @author David Kofke, Andrew Schultz
 */
public class P2WaterTIP4PHardCore extends PotentialMolecular {

	private static double A = 600e3; // kcal A^12 / mol
	private static double C = 610; // kcal A^6 / mol
	private static double s6 = A/C;
	private static double s = Math.pow(s6, 1.0/6.0);
	private static double e = Mole.UNIT.fromSim(Calorie.UNIT.toSim(C/s6*1000))/4.0;

	private static final long serialVersionUID = 1L;
	public double sigma , sigma2;
	protected double epsilon, epsilon4;
	public double hardCore;
	protected IBoundary boundary;
	protected final double chargeH;
	protected final double chargeM;
	protected final double chargeMM, chargeMH, chargeHH;
	private IVectorMutable P1r,P2r;//dipole position
	private IVectorMutable O1P1r, O2P2r;
	private IVectorMutable P1P2r;//vector between dipoles
	private IVectorMutable mu1Normalized, mu2Normalized;
	private double mu;
	private IVectorMutable r;
	private IVectorMutable dipole1, dipole2;

	public P2WaterTIP4PHardCore(ISpace space, double hardCore, double sigma, double epsilon, double chargeM, double chargeH) {
		super(2, space);
		this.hardCore=hardCore;
		this.sigma = sigma;
        sigma2 = sigma*sigma;
        this.epsilon = epsilon;
        epsilon4 = 4*epsilon;
        chargeMM = chargeM * chargeM;
        chargeMH = chargeM * chargeH;
        chargeHH = chargeH * chargeH;
        this.chargeM = chargeM;
        this.chargeH = chargeH;
        P1r=space.makeVector();
        P2r=space.makeVector();
        O1P1r=space.makeVector();
        O2P2r=space.makeVector();
        P1P2r=space.makeVector();
        mu1Normalized=space.makeVector();
        mu2Normalized=space.makeVector();
        mu=-chargeM * 0.43588227661829493;//magnitude of dipole moment
        r=space.makeVector();
        dipole1 = space.makeVector();
        dipole2 = space.makeVector();
	}

    public void setBox(IBox box) {
        boundary = box.getBoundary();
    }

    public double energy(IMoleculeList pair){
		double sum = 0.0;
		IMolecule water1 = pair.getMolecule(0);
		IMolecule water2 = pair.getMolecule(1);
		
        IVectorMutable O1r = (water1.getChildList().getAtom(2)).getPosition();//H-H-O-M, so O is the third atom
        IVectorMutable O2r = (water2.getChildList().getAtom(2)).getPosition();
        IVectorMutable H11r = water1.getChildList().getAtom(0).getPosition();// 1st H on water 1
        IVectorMutable H12r = water1.getChildList().getAtom(1).getPosition();// 2nd H on water 1
        IVectorMutable H21r = water2.getChildList().getAtom(0).getPosition();// 1st H on water 2
        IVectorMutable H22r = water2.getChildList().getAtom(1).getPosition();// 2nd H on water 2
        IVectorMutable M1r = water1.getChildList().getAtom(3).getPosition();
        IVectorMutable M2r = water2.getChildList().getAtom(3).getPosition();

        r.Ev1Mv2(O1r, O2r);//distance between lj sites
        double r2 = r.squared();
		if(r2<hardCore*hardCore) return Double.POSITIVE_INFINITY;

		double s2 = sigma2/(r2);
		double s6 = s2*s2*s2;
		sum += epsilon4*s6*(s6 - 1.0);// LJ energy
		double ulj = sum;
		boolean printme = r2 > 400;
       
        double offset = 0.36794113830914743;	
        // get O1P1 vector
        O1P1r.Ev1Mv2(M1r, O1r);
        O1P1r.normalize();
        mu1Normalized.E(O1P1r);
        O1P1r.TE(offset);
        //get P1
        P1r.Ev1Pv2(O1P1r, O1r);
        
        O2P2r.Ev1Mv2(M2r, O2r);
        O2P2r.normalize();
        mu2Normalized.E(O2P2r);
        O2P2r.TE(offset);
        //get P2
        P2r.Ev1Pv2(O2P2r, O2r);
        //get P1P2r
        P1P2r.Ev1Mv2(P2r, P1r);

        double dipoleDistance2=P1P2r.squared();
        
        P1P2r.normalize();
        double dipoleDistance=Math.sqrt(dipoleDistance2);
        double u_dipole=mu*mu*(mu1Normalized.dot(mu2Normalized)-3.0*mu1Normalized.dot(P1P2r)*P1P2r.dot(mu2Normalized))/dipoleDistance/dipoleDistance2;
        r2 = M1r.Mv1Squared(M2r);
        sum += chargeMM/Math.sqrt(r2);
        r2 = M1r.Mv1Squared(H21r);
        sum += chargeMH/Math.sqrt(r2);
        r2 = M1r.Mv1Squared(H22r);
        sum += chargeMH/Math.sqrt(r2);
        r2 = H11r.Mv1Squared(M2r);
        sum += chargeMH/Math.sqrt(r2);
        r2 = H11r.Mv1Squared(H21r);
        sum += chargeHH/Math.sqrt(r2);
        r2 = H11r.Mv1Squared(H22r);
        sum += chargeHH/Math.sqrt(r2);
        r2 = H12r.Mv1Squared(M2r);
        sum += chargeMH/Math.sqrt(r2);
        r2 = H12r.Mv1Squared(H21r);
        sum += chargeHH/Math.sqrt(r2);
        r2 = H12r.Mv1Squared(H22r);
        sum += chargeHH/Math.sqrt(r2);
       // if (printme) System.out.println("P2WaterTIP4PHardCore, distance between dipoles:"+dipoleDistance);        
      //  if (printme) System.out.println("P2WaterTIP4PHardCore, u_dipole from eqn:"+u_dipole);
      //  if (printme) System.out.println("P2WaterTIP4PHardCore, dipole from electrostatics:"+(sum-ulj));
      //  if (printme) System.out.println("P2WaterTIP4PHardCore, ulj:"+ulj);

        double uDipole = sum - ulj;
		return sum;																					        
	}
    public IVectorMutable getDipole1(){
		dipole1.E(mu1Normalized);
		dipole1.TE(mu);
		return dipole1;
	}
	
	public IVectorMutable getDipole2(){
		dipole2.E(mu2Normalized);
		dipole2.TE(mu);
		return dipole2;
	}
    public double getRange() {return Double.POSITIVE_INFINITY;}
	public double getSigma() {return sigma;}
	public double getEpsilon() {return epsilon;}
}
