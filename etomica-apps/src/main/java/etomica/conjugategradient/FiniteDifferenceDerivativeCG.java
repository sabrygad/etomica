/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.conjugategradient;

import etomica.action.Activity;
import etomica.api.IAtom;
import etomica.api.IBox;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomLeafAgentManager.AgentSource;
import etomica.atom.iterator.IteratorDirective;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.paracetamol.AnalyticalDerivativeEnergyParacetamol;
import etomica.potential.PotentialCalculationForceSum;
import etomica.potential.PotentialMaster;
import etomica.space.ISpace;
import etomica.util.numerical.FiniteDifferenceDerivative;

public class FiniteDifferenceDerivativeCG {
	
	/*
	 * This class is developed to calculate for second derivative of energy
	 *  by applying finite difference derivative to derivative function,
	 *  which the first derivative is determined by calculating:
	 *  	a. the force of molecule (1st derivative for translation)
	 *      b. the torque of molecule (1st derivative for rotation)
	 * 
	 * @author Tai Tan
	 */
	
	protected IBox box;
	protected MeterPotentialEnergy meterEnergy;
	protected PotentialMaster potentialMaster;
	protected IteratorDirective allAtoms;
	protected PotentialCalculationForceSum forceSum;
	protected AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent> agentManager;
	protected Activity activity;
	
	protected AnalyticalDerivativeEnergyParacetamol derivativeFunction;
	protected double h;
	protected boolean hOptimizer;
	
	private final ISpace space;
	
	public FiniteDifferenceDerivativeCG(IBox box, PotentialMaster potentialMaster,
			             AnalyticalDerivativeEnergyParacetamol derivativeFunction,
			             ISpace _space){
		this.box = box;
		this.potentialMaster = potentialMaster;
		this.space = _space;
		this.derivativeFunction = derivativeFunction;
		meterEnergy = new MeterPotentialEnergy(potentialMaster);
		allAtoms = new IteratorDirective();
		forceSum = new PotentialCalculationForceSum();
		h = 0.00001;
		hOptimizer = false;
		
		MyAgentSource source = new MyAgentSource(space);
		agentManager = new AtomLeafAgentManager<IntegratorVelocityVerlet.MyAgent>(source, box, IntegratorVelocityVerlet.MyAgent.class);
		forceSum.setAgentManager(agentManager);
	}
	
	
	public double f(double[] u){
		return derivativeFunction.f(u);
	}
	
	public double df(int[] d, double[] u){
		return derivativeFunction.df(d, u);
	}
	
	public double[] d2f(int[] d, double[] u){
		
		int coordinateDim = u.length;
		double[] d2fdu2 = new double[coordinateDim];
		
		derivativeFunction = new AnalyticalDerivativeEnergyParacetamol(box, potentialMaster, space);
		double[] uDerivative = new double[coordinateDim];
		int[] dAssign = new int[coordinateDim];
		
		/*
		 *  To compute the first-order derivative from DerivativeFunctionParacetamol
		 *   for which uDerivative will be next input as u to compute for the second derivative
		 */
		for (int i=0; i< coordinateDim; i++){
			
			/*
			 *  To assign d to differentiate over all the dimensions
			 */
			for (int j=0; j< coordinateDim; j++){
				if (i==j){
					dAssign[j] = 1;
				} else{
					dAssign[j] = 0;
				}
			}
			uDerivative[i] = derivativeFunction.df(dAssign, u);
		}
		
		FiniteDifferenceDerivative finiteDifferenceDerivative = new FiniteDifferenceDerivative(derivativeFunction);
		finiteDifferenceDerivative.setH(0.000001);
			
		/*
		 * Using Finite Difference Derivative to compute for the second derivative
		 *  of the energy.
		 */
			
		for (int i=0; i< coordinateDim; i++){
			
			/*
			 *  To assign d to differentiate over all the dimensions
			 */
			for (int j=0; j< coordinateDim; j++){
				if (i==j){
					dAssign[j] = 1;
				} else{
					dAssign[j] = 0;
				}
			}
			d2fdu2[i] = finiteDifferenceDerivative.df(dAssign, uDerivative);
		}
		
		return d2fdu2;
	}

	
	public static class MyAgentSource implements AgentSource<IntegratorVelocityVerlet.MyAgent> {
		public MyAgentSource(ISpace space){
			this.space = space;
		}
		public void releaseAgent(IntegratorVelocityVerlet.MyAgent agent, IAtom atom, IBox agentBox){}

		public IntegratorVelocityVerlet.MyAgent makeAgent(IAtom atom, IBox agentBox){
			return new IntegratorVelocityVerlet.MyAgent(space);
		}
		protected ISpace space;
	}


	public double getH() {
		return h;
	}

	public void setH(double h) {
		this.h = h;
	}

	public boolean isHOptimizer() {
		return hOptimizer;
	}

	public void setHOptimizer(boolean optimizer) {
		hOptimizer = optimizer;
	}
}
