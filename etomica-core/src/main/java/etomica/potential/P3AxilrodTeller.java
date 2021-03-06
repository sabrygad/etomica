/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.potential;

import etomica.api.IAtomList;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IPotentialAtomic;
import etomica.api.IVectorMutable;
import etomica.atom.AtomTypeAgentManager;
import etomica.space.ISpace;

/**
 * Axilrod-Teller potential.  The potential is atomic.  Ionization energy and
 * polarizability are required as input for each atom type. 
 * 
 * @author Andrew Schultz
 */
public class P3AxilrodTeller implements IPotentialAtomic {

    protected final AtomTypeAgentManager paramsManager;
    protected final ISpace space;
    protected final IVectorMutable dr1, dr2;
    protected IBoundary boundary;
    
    public P3AxilrodTeller(ISpace space, AtomTypeAgentManager paramsManager) {
        this.space = space;
        this.paramsManager = paramsManager;
        dr1 = space.makeVector();
        dr2 = space.makeVector();
    }

    public double energy(IAtomList atoms) {
        double ep = 1;
        double es = 0;
        double[] eps = new double[3];
        double ap = 1;
        double cp = 3;
        double rp = 1;
        for (int i=0; i<3; i++) {
            MyAgent ag =  (MyAgent)paramsManager.getAgent(atoms.getAtom(i).getType());
            eps[(i+1)%3] += ag.E;
            eps[(i+2)%3] += ag.E;
            ep *= ag.E;
            es += ag.E;

            ap *= ag.alpha;
            dr1.Ev1Mv2(atoms.getAtom(i).getPosition(),atoms.getAtom((i+1)%3).getPosition());
            boundary.nearestImage(dr1);
            double r2 = dr1.squared();
            rp *= r2*Math.sqrt(r2);
            dr2.Ev1Mv2(atoms.getAtom((i+2)%3).getPosition(),atoms.getAtom((i+1)%3).getPosition());
            boundary.nearestImage(dr2);
            double cos = dr1.dot(dr2)/Math.sqrt(r2*dr2.squared());
            cp *= cos;
        }
        double e123 = ep*es;
        for (int i=0; i<3; i++) {
            e123 /= eps[i];
        }
        
        return 1.5*e123*ap*(cp+1)/rp;
    }

    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }

    public void setBox(IBox box) {
        boundary = box.getBoundary();
    }

    public int nBody() {
        return 3;
    }

    // CO2: alpha=2.913, alpha(omega)=2.639, I=13.7eV
    // H2O: alpha=1.444, I=12.6eV
    public static class MyAgent {
        public final double alpha, E;
        /**
         * @params alpha polarizability
         * @params E ionization energy
         */
        public MyAgent(double alpha, double E) {
            this.alpha = alpha;
            this.E = E;
        }
    }
}
