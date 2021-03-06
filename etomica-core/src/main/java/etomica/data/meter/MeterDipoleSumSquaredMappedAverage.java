package etomica.data.meter;

import etomica.api.IAtom;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IMoleculeList;
import etomica.api.IPotentialMaster;
import etomica.api.ISimulation;
import etomica.api.IVectorMutable;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.DipoleSource;
import etomica.atom.MoleculeAgentManager;
import etomica.atom.MoleculeAgentManager.MoleculeAgentSource;
import etomica.atom.iterator.IteratorDirective;
import etomica.data.DataTag;
import etomica.data.IData;
import etomica.data.IEtomicaDataInfo;
import etomica.data.IEtomicaDataSource;
import etomica.data.types.DataDoubleArray;
import etomica.data.types.DataDoubleArray.DataInfoDoubleArray;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.integrator.IntegratorRigidIterative.MoleculeAgent;
import etomica.potential.PotentialCalculationEnergySum;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialCalculationPhiSum;
import etomica.potential.PotentialCalculationTorqueSum;
import etomica.space.ISpace;
import etomica.units.Null;

/**
 * meter for AEE use mapping average
 * 
 * @author Weisong
 */
public class MeterDipoleSumSquaredMappedAverage implements IEtomicaDataSource,MoleculeAgentSource  {

	protected final DataDoubleArray data;
	protected final DataInfoDoubleArray dataInfo;
	protected final DataTag tag;
//	private IBoundary boundary;
	protected PotentialCalculationEnergySum energySum;
	protected PotentialCalculationTorqueSum torqueSum;
	protected PotentialCalculationPhiSum secondDerivativeSum;
	protected final ISpace space;
	private IBox box;
	private IVectorMutable vectorSum;
//	private IVectorMutable r;
//	private IVectorMutable [] a;
	private double dipoleMagnitude;
//	private double truncation;
	private double temperature;
    protected final IPotentialMaster potentialMaster;
    private final IteratorDirective allAtoms;
    protected IVectorMutable dr;
    protected IVectorMutable work;
    protected MoleculeAgentManager moleculeAgentManager;
    protected DipoleSource dipoleSource;
    protected AtomLeafAgentManager atomAgentManager;
    protected PotentialCalculationForceSum pcForce;

	public MeterDipoleSumSquaredMappedAverage(final ISpace space, IBox box,ISimulation sim, double dipoleMagnitude,double temperature,IPotentialMaster potentialMaster) {
		data = new DataDoubleArray(2);
		dataInfo = new DataInfoDoubleArray("stuff", Null.DIMENSION, new int[]{2});
		tag = new DataTag();
		dataInfo.addTag(tag);
		this.box = box;
		this.dipoleMagnitude = dipoleMagnitude;
		this.temperature = temperature;
		this.space = space;
		this.potentialMaster = potentialMaster;
		vectorSum = space.makeVector();
//		r = space.makeVector();
		vectorSum.setX(2, 1);
		torqueSum = new PotentialCalculationTorqueSum();
		energySum = new PotentialCalculationEnergySum();
		secondDerivativeSum = new  PotentialCalculationPhiSum(space);
		moleculeAgentManager  = new MoleculeAgentManager(sim,box,this);
		torqueSum.setMoleculeAgentManager(moleculeAgentManager);
		allAtoms = new IteratorDirective();
		dr = space.makeVector();
		work = space.makeVector();
		
		AtomLeafAgentManager.AgentSource<IntegratorVelocityVerlet.MyAgent> atomAgentSource = new AtomLeafAgentManager.AgentSource<IntegratorVelocityVerlet.MyAgent>() {
		    public IntegratorVelocityVerlet.MyAgent makeAgent(IAtom a, IBox agentBox) {
		        return new IntegratorVelocityVerlet.MyAgent(space);
		    }
		    public void releaseAgent(IntegratorVelocityVerlet.MyAgent agent, IAtom atom, IBox agentBox) {/**do nothing**/}
        };
		
		pcForce = new PotentialCalculationForceSum();
		atomAgentManager = new AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent>(atomAgentSource , box,IntegratorVelocityVerlet.MyAgent.class);
        pcForce.setAgentManager(atomAgentManager);
		
	}

	public IData getData() {		
//		IBoundary boundary = box.getBoundary();// TODO
		double[] x = data.getData();
		double bt = 1/(temperature);//beta
		
		double mu = dipoleMagnitude;//miu
		double mu2 = mu*mu;
		double bt2 = bt*bt;
		double bt3 = bt*bt*bt;
		if (box == null) throw new IllegalStateException("no box");
		IMoleculeList moleculeList = box.getMoleculeList();
		
		
		int nM = moleculeList.getMoleculeCount();	
		
		//TODO
//		IAtomList atomList0 = moleculeList.getMolecule(0).getChildList();
//		IAtomOriented atom0 = (IAtomOriented) atomList0.getAtom(0);
//		IVectorMutable  v0 =  (IVectorMutable) atom0.getOrientation().getDirection();  
//		IAtomList atomList1 = moleculeList.getMolecule(1).getChildList();
//		IAtomOriented atom1 = (IAtomOriented) atomList1.getAtom(0);
//		IVectorMutable  v1 =  (IVectorMutable) atom1.getOrientation().getDirection();  
//		v1.E(0);
//		v1.setX(0, 1);
//		System.out.println("v0 = " + v0);
//		System.out.println("v1 = " + v1);
		
		 torqueSum.reset();
		 potentialMaster.calculate(box, allAtoms, torqueSum);
		 secondDerivativeSum.zeroSum();
		 potentialMaster.calculate(box, allAtoms, secondDerivativeSum);
		
			
		 IteratorDirective id = new IteratorDirective();
		 potentialMaster.calculate(box, id, pcForce);
		 
		 
		 double A = 0;
		 vectorSum.E(0);
		 for (int i = 0;i < nM; i++){
			 dr.E(dipoleSource.getDipole(moleculeList.getMolecule(i)));
			 dr.normalize();
			 
			 A += -2.0/3.0*bt2*mu2*(dr.squared()-1);
			 
			 MoleculeAgent torqueAgent = (MoleculeAgent) moleculeAgentManager.getAgent(moleculeList.getMolecule(i));
			 dr.XE(torqueAgent.torque);
			 vectorSum.PE(dr);
		 }//i loop
		 
		 //TODO
//		x[0] = ( -2*nM*bt2*mu2+0.25*bt3*mu2*secondDerivativeSum.getSum())/3.0;
//		x[1] = 0.25*bt2*bt2*mu2*vectorSum.squared() ;
		
		
		
		x[0] = -nM*bt2*mu2 - 0.25*bt2*bt2*mu2*vectorSum.squared()+ 0.25*bt3*mu2*secondDerivativeSum.getSum();//TODO
		
		
//		x[1] = vectorSum.getX(0) +vectorSum.getX(1) + vectorSum.getX(2);
//		x[1] = -nM*bt2*mu2 - 0.25*bt2*bt2*mu2*vectorSum.squared()+ 0.25*bt3*mu2*secondDerivativeSum.getSum() + A;
		return data;
	}
	
	public void setDipoleSource(DipoleSource newDipoleSource) {
		dipoleSource = newDipoleSource;
		secondDerivativeSum.setDipoleSource(newDipoleSource); 
	}
	
	public DataTag getTag() {
		return tag;
	}

	public IEtomicaDataInfo getDataInfo() {
		return dataInfo;
	}

	public IBox getBox() {
		return box;
	}


	public Object makeAgent(IMolecule a) {
		// TODO Auto-generated method stub
		 return new MoleculeAgent(space);
	}

	public void releaseAgent(Object agent, IMolecule a) {
		// TODO Auto-generated method stub
		
	}

	public Class getMoleculeAgentClass() {
		// TODO Auto-generated method stub
		return MoleculeAgent.class;
	}
	
	public AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent> getAtomAgentManager(){
		return atomAgentManager;
	}
	

}