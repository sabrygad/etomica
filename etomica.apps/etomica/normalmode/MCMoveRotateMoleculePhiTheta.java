package etomica.normalmode;

import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.AtomPositionGeometricCenter;
import etomica.atom.IAtomPositionDefinition;
import etomica.integrator.mcmove.MCMoveRotateMolecule3D;
import etomica.space.ISpace;

public class MCMoveRotateMoleculePhiTheta extends MCMoveRotateMolecule3D {

    protected final IVectorMutable[] drSum;
    protected double maxAngle;
    protected boolean rotatePhi;
    protected final IVectorMutable dr, com;
    protected IAtomPositionDefinition pos;
    protected double delta;
    
    public MCMoveRotateMoleculePhiTheta(IPotentialMaster potentialMaster,
            IRandom random, ISpace _space, IVectorMutable[] drSum, boolean doPhi) {
        super(potentialMaster, random, _space);
        this.drSum = drSum;
        dr = _space.makeVector();
        rotatePhi = doPhi;
        com = _space.makeVector();
        pos = new AtomPositionGeometricCenter(_space);
    }
    
    public void setBox(IBox p) {
        super.setBox(p);
        IMoleculeList molecules = p.getMoleculeList();
        int nPlanes = drSum.length;
        for (int iPlane=0; iPlane<nPlanes; iPlane++) {
            drSum[iPlane].E(0);
        }
        for (int i=0; i<molecules.getMoleculeCount(); i++) {
            IAtomList atoms = molecules.getMolecule(i).getChildList();
            int iPlane = (i/2)%nPlanes;
            drSum[iPlane].PE(atoms.getAtom(1).getPosition());
            drSum[iPlane].ME(atoms.getAtom(0).getPosition());
        }
    }

    public void setMaxPhi(double newMaxAngle) {
        maxAngle = newMaxAngle;
    }
    
    public double getConstraint() {
        return maxAngle;
    }
    
    public boolean doTrial() {
//      System.out.println("doTrial MCMoveRotateMolecule called");
      
        if(box.getMoleculeList().getMoleculeCount()==0) {molecule = null; return false;}
          
        molecule = moleculeSource.getMolecule();
        energyMeter.setTarget(molecule);
        uOld = energyMeter.getDataAsScalar();
      
        if(Double.isInfinite(uOld)) {
            throw new RuntimeException("Overlap in initial state");
        }
        
        int nPlanes = drSum.length;
        int iPlane = (molecule.getIndex()/2)%nPlanes;
        IVectorMutable drSumi = drSum[iPlane];
        IAtomList atoms = molecule.getChildList();
        // subtract the original orientation
        double planePhi = Math.atan2(drSumi.getX(1), drSumi.getX(0));
        if (Math.abs(planePhi) > maxAngle) {
            throw new RuntimeException("oops "+planePhi);
        }
        drSumi.ME(atoms.getAtom(1).getPosition());
        drSumi.PE(atoms.getAtom(0).getPosition());
        
        if (rotatePhi) {
            delta = (2*random.nextDouble() - 1.0)*stepSize;
            rotationTensor.setAxial(2,delta);

            r0.E(positionDefinition.position(molecule));
            doTransform();
        }
        else {
            dr.E(atoms.getAtom(1).getPosition());
            dr.ME(atoms.getAtom(0).getPosition());
            dr.normalize();
            double sintheta = Math.sqrt(dr.getX(0)*dr.getX(0) + dr.getX(1)*dr.getX(1));
            double costheta = dr.getX(2);
            delta = (2*random.nextDouble() - 1.0)*stepSize;

            double x = dr.getX(0);
            double y = dr.getX(1);
            double phi = Math.atan2(y, x);
            
            double newCosTheta = costheta + delta;
            double newSinTheta = Math.sqrt(1-newCosTheta*newCosTheta);

            double newPhi = phi * sintheta / newSinTheta;

            if (newCosTheta < 1e-8 || newCosTheta > 1 - 1e-8 || Math.abs(newPhi) > Math.PI - 1e-8) {
                // we don't rotate past vertical or horizontal, also can't rotate backwards
                drSumi.PE(atoms.getAtom(1).getPosition());
                drSumi.ME(atoms.getAtom(0).getPosition());
                return false;
            }
//            System.out.println(sintheta+" "+costheta+"  =>  "+newSinTheta+" "+newCosTheta);

            double a = newSinTheta/sintheta;
            double b = newCosTheta - newSinTheta/sintheta*costheta;

            doRotationTransform(a, b);
            
            rotationTensor.setAxial(2,newPhi-phi);

            r0.E(positionDefinition.position(molecule));
            doTransform();
        }

      
        energyMeter.setTarget(molecule);
        return true;
    }

    protected void doRotationTransform(double a, double b) {
        com.E(pos.position(molecule));
        IAtomList childList = molecule.getChildList();
        for (int i=0; i<childList.getAtomCount(); i++) {
            IVectorMutable p = childList.getAtom(i).getPosition();
            p.ME(com);
            double l = Math.sqrt(p.squared());
            if (p.getX(2) < 0) l = -l;
            p.TE(a);
            p.setX(2, p.getX(2) + b*l);
            p.PE(com);
        }
    }

    
    public double getA() {
        int nPlanes = drSum.length;
        int iPlane = (molecule.getIndex()/2)%nPlanes;
        IVectorMutable drSumi = drSum[iPlane];
        IAtomList atoms = molecule.getChildList();
        // add the trial orientation
        drSumi.PE(atoms.getAtom(1).getPosition());
        drSumi.ME(atoms.getAtom(0).getPosition());
        double phi = Math.atan2(drSumi.getX(1), drSumi.getX(0));

        if (Math.abs(phi) > maxAngle) {
            return 0;
        }
        return 1;
    }

    public void acceptNotify() {
    }

    public void rejectNotify() {
        int nPlanes = drSum.length;
        int iPlane = (molecule.getIndex()/2)%nPlanes;
        IVectorMutable drSumi = drSum[iPlane];
        IAtomList atoms = molecule.getChildList();
        // subtract the trial orientation
        drSumi.ME(atoms.getAtom(1).getPosition());
        drSumi.PE(atoms.getAtom(0).getPosition());

        if (rotatePhi) {
            rotationTensor.setAxial(2,-delta);
            r0.E(positionDefinition.position(molecule));
            doTransform();
        }
        else {
            dr.E(atoms.getAtom(1).getPosition());
            dr.ME(atoms.getAtom(0).getPosition());
            dr.normalize();

            double x = dr.getX(0);
            double y = dr.getX(1);
            double phi = Math.atan2(y, x);

            double sintheta = Math.sqrt(dr.getX(0)*dr.getX(0) + dr.getX(1)*dr.getX(1));
            double costheta = dr.getX(2);
            double newCosTheta = costheta - delta;
            double newSinTheta = Math.sqrt(1-newCosTheta*newCosTheta);
            double newPhi = phi * sintheta / newSinTheta;

            double a = newSinTheta/sintheta;
            double b = newCosTheta - newSinTheta/sintheta*costheta;

            doRotationTransform(a, b);

            rotationTensor.setAxial(2,newPhi-phi);

            r0.E(positionDefinition.position(molecule));
            doTransform();
        }

        // add the original orientation back
        drSumi.PE(atoms.getAtom(1).getPosition());
        drSumi.ME(atoms.getAtom(0).getPosition());
    }
    
    public String toString() {
        return super.toString()+" "+(rotatePhi ? "phi" : "theta");
    }
}
