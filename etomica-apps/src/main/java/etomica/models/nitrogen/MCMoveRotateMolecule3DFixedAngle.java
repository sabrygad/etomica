/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.models.nitrogen;
import etomica.action.MoleculeChildAtomAction;
import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVectorMutable;
import etomica.atom.AtomPositionGeometricCenter;
import etomica.atom.IAtomPositionDefinition;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.paracetamol.AtomActionTransformed;
import etomica.space.ISpace;
import etomica.space.RotationTensor;
import etomica.space3d.RotationTensor3D;
import etomica.space3d.Tensor3D;
import etomica.units.Degree;


/**
 * MCMoveRotate that moves the molecules according to pre-set constraint angle
 *  which is given by the parameter "angle"
 *  
 * This class does not consider rotational energy, so the variable "energyChange"
 *  always returns zero
 *  
 * getB() always returns positive one is to ensure the proposed move is always 
 *  accepted
 * 
 * the proposed angle criteria is based on u3 and u4 distribution
 * 
 * @author Tai Boon Tan
 *
 */
public class MCMoveRotateMolecule3DFixedAngle extends MCMoveMolecule {
    
    private static final long serialVersionUID = 2L;
    protected transient IVectorMutable r0;
    protected transient RotationTensor rotationTensor;
    protected IAtomPositionDefinition positionDefinition;
    protected double constraintAngle;
    protected CoordinateDefinitionNitrogen coordinateDef;
    protected IVectorMutable[][] initMolecOrientation;
    protected IVectorMutable molecOrientation, rotationAxis;
	protected RotationTensor3D rotation;
	protected Tensor3D tensor;
    protected final MoleculeChildAtomAction atomGroupAction;
    
    public MCMoveRotateMolecule3DFixedAngle(IPotentialMaster potentialMaster, IRandom random,
    		                      ISpace _space, double angle, CoordinateDefinitionNitrogen coordinateDef, IBox box) {
        super(potentialMaster, random, _space, 0.5*Math.PI, Math.PI);
        rotationTensor = _space.makeRotationTensor();
        r0 = _space.makeVector();
        positionDefinition = new AtomPositionGeometricCenter(space);
        constraintAngle = angle;
        this.coordinateDef = coordinateDef;
        
        int numMolec = box.getMoleculeList().getMoleculeCount();
     	initMolecOrientation = new IVectorMutable[numMolec][3];
     	molecOrientation = space.makeVector();
     	rotationAxis = space.makeVector();
     	
    	tensor = new Tensor3D(new double[][]{{1.0, 0.0, 0.0},{0.0, 1.0, 0.0},{0.0, 0.0, 1.0}});
		rotation = new RotationTensor3D();
		rotation.E(tensor);
    	/*
		 * initializing the initial orientation of the molecule
		 */
		for (int i=0; i<numMolec; i++){
			initMolecOrientation[i] = space.makeVectorArray(3);
			initMolecOrientation[i] = coordinateDef.getMoleculeOrientation(box.getMoleculeList().getMolecule(i));
		}
		atomGroupAction = new MoleculeChildAtomAction(new AtomActionTransformed(space));
	        
    }
     
    public boolean doTrial() {

        if(box.getMoleculeList().getMoleculeCount()==0) {molecule = null; return false;}
        int iMolecule = random.nextInt(box.getMoleculeList().getMoleculeCount());
        
        molecule = coordinateDef.getBox().getMoleculeList().getMolecule(iMolecule);
        r0.E(positionDefinition.position(molecule));
        
        energyMeter.setTarget(molecule);
        uOld = energyMeter.getDataAsScalar();
        if(Double.isInfinite(uOld)) {
            throw new RuntimeException("Overlap in initial state");
        }
        
        double u3 = 2.0; // set u3 and u4 to large values
        double u4 = 2.0;
        double theta = 361;
        while (theta > constraintAngle){
        	u3 = (2*random.nextDouble() - 1.0)*(constraintAngle/180.0) *Math.PI;
        	u4 = (2*random.nextDouble() - 1.0)*(constraintAngle/180.0) *Math.PI;
        	theta = Degree.UNIT.fromSim(Math.acos(1.0000000000000004 - (u3*u3 + u4*u4)*0.5));
        } 
        
        r0.E(positionDefinition.position(molecule));
        setToU(iMolecule, u3, u4);
		
	    IVectorMutable leafPos0 = molecule.getChildList().getAtom(0).getPosition();
		IVectorMutable leaftPos1 = molecule.getChildList().getAtom(1).getPosition();
		molecOrientation.Ev1Mv2(leaftPos1, leafPos0);
		molecOrientation.normalize();
		
		double angleMol = Math.acos(molecOrientation.dot(initMolecOrientation[iMolecule][0]));
		
        energyMeter.setTarget(molecule);
        
        if(Double.isNaN(energyMeter.getDataAsScalar())){
        	System.out.println("theta: " + theta+ " angleMol: "+ Degree.UNIT.fromSim(angleMol));
        	System.out.println("molecOrientation: " + molecOrientation.toString());
        	System.out.println("initMolecOrientation: " + initMolecOrientation[iMolecule][0].toString());
        	System.out.println("dot: " + molecOrientation.dot(initMolecOrientation[iMolecule][0]));
        	double energy = energyMeter.getDataAsScalar();
        	System.out.println("energy"+ energy);
        	throw new RuntimeException("<MCMoveRotate3DFixedAngle> energy is NaN!!!!!!!!!!!!");
        }
        return true;
    }

    protected void setToU(int iMolecule, double u3, double u4){
    	IAtomList childList = molecule.getChildList();
    	for (int iChild = 0; iChild<childList.getAtomCount(); iChild++) {
    		IAtom a = childList.getAtom(iChild);
    		IVectorMutable r = a.getPosition();
    		r.ME(r0);
    	}
    	
    	IVectorMutable rotationAxis = space.makeVector();
    	RotationTensor3D rotation = new RotationTensor3D();
    	rotation.E(tensor);
	         
    	IVectorMutable leafPos0 = molecule.getChildList().getAtom(0).getPosition();
    	IVectorMutable leafPos1 = molecule.getChildList().getAtom(1).getPosition();
		    	
    	/*
         * a.
         */
	   	IVectorMutable axis = space.makeVector();
	   	axis.Ev1Mv2(leafPos1, leafPos0);
	   	axis.normalize();
		    		    	
    	double angle = Math.acos(axis.dot(initMolecOrientation[iMolecule][0]));

    	if (Math.abs(angle) > 5e-8){ // make sure we DO NOT cross-product vectors with very small angle
    		rotationAxis.E(axis);
	    	rotationAxis.XE(initMolecOrientation[iMolecule][0]);
	    	rotationAxis.normalize();
			    	
	    	/*
	    	 * 	 c. rotating clockwise.
		 	 */
	    	rotation.setRotationAxis(rotationAxis, angle);
			    	
	    	if(rotation.isNaN()){	
	    		System.out.println("Step 1 Rotation tensor is BAD!");
				System.out.println("Rotation Angle is too small, angle: "+ angle);
	    		System.out.println("Rotation is not necessary");
	    		System.out.println(rotation);
	    		throw new RuntimeException();
	    	}
	        ((AtomActionTransformed)atomGroupAction.getAtomAction()).setTransformationTensor(rotation);
	        atomGroupAction.actionPerformed(molecule);
    	}

        if (Math.abs(u3)>1e-7 || Math.abs(u4)>1e-7){
		       
       	/*
         * a.	
         */
       	axis.E(0);
    	axis.Ea1Tv1(u3, initMolecOrientation[iMolecule][1]);
    	axis.PEa1Tv1(u4, initMolecOrientation[iMolecule][2]);
    	axis.normalize();
			    	
    	/*
    	 * b.
    	 */
    	angle = Math.acos(1.0000000000000004 - (u3*u3 + u4*u4)*0.5);
    	if(Math.abs(angle) > 1e-7){
	    	rotationAxis.E(0);
	    	rotationAxis.E(axis);
	    	rotationAxis.XE(initMolecOrientation[iMolecule][0]);
	    	rotationAxis.normalize();
				    	
	    	rotation.setRotationAxis(rotationAxis, -angle);
				    	
	    	if(rotation.isNaN()){
	    		System.out.println("Step 2 Rotation tensor is BAD!");
	    		System.out.println("Rotation Angle is too small, angle: "+ angle);
	    		System.out.println("Rotation is not necessary");
	    		System.out.println(rotation);
	    		throw new RuntimeException();
	    	}
				    	
	    	((AtomActionTransformed)atomGroupAction.getAtomAction()).setTransformationTensor(rotation);
	        atomGroupAction.actionPerformed(molecule);
	   		}
        }
    
        for (int iChild = 0; iChild<childList.getAtomCount(); iChild++) {
            IAtom a = childList.getAtom(iChild);
            IVectorMutable r = a.getPosition();
            r.PE(r0);
        }
    }
    
    public double getB() {
//    	double energy = energyMeter.getDataAsScalar();
//    	if(Double.isInfinite(energy)){
//    		return -1.0;
//    	}
        return 0.0; //always accept the rotational move, See the acceptance criteria in IntegratorMC
    }
    
    public double energyChange() { return 0.0;}
}
