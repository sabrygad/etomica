package etomica.potential;

import etomica.Atom;
import etomica.Debug;
import etomica.Default;
import etomica.EtomicaInfo;
import etomica.Simulation;
import etomica.Space;
import etomica.space.CoordinatePairKinetic;
import etomica.space.ICoordinateKinetic;
import etomica.space.Tensor;
import etomica.space.Vector;
import etomica.units.Dimension;

/**
 * Potential that acts like a hard string connecting the centers of two atoms.
 * Meant for use as an intra-molecular interaction. Atoms can fluctuate between
 * a minimum and maximum separation. Atoms undergo an attractive collision when
 * attempting to separate by more than the maximum and an repulsive collision
 * when attempting to come together closer than the min distance.
 */
public class P2HardBond extends Potential2 implements PotentialHard {

    private double minBondLengthSquared;
    private double maxBondLengthSquared;
    private double bondLength;
    private double bondDelta;
    private double lastCollisionVirial = 0.0;
    private double lastCollisionVirialr2 = 0.0;
    private final Vector dr;
    private final Tensor lastCollisionVirialTensor;

    public P2HardBond() {
        this(Simulation.getDefault().space);
    }

    public P2HardBond(Space space) {
        this(space, Default.ATOM_SIZE, 0.15);
    }

    public P2HardBond(Space space, double bondLength, double bondDelta) {
        super(space);
        setBondLength(bondLength);
        setBondDelta(bondDelta);
        lastCollisionVirialTensor = space.makeTensor();
        dr = space.makeVector();
    }

    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Hard bond between atoms");
        return info;
    }

    /**
     * Accessor method for the bond length
     */
    public final double getBondLength() {
        return bondLength;
    }

    /**
     * Accessor method for the bond extension factor
     */
    public final double getBondDelta() {
        return bondDelta;
    }

    /**
     * Setter method for the bond length
     */
    public final void setBondLength(double l) {
        bondLength = l;
        double minBondLength = bondLength - bondDelta;
        double maxBondLength = bondLength + bondDelta;
        minBondLengthSquared = minBondLength * minBondLength;
        maxBondLengthSquared = maxBondLength * maxBondLength;
    }

    /**
     * Setter method for the bond extension factor
     */
    public final void setBondDelta(double d) {
        bondDelta = d;
        setBondLength(bondLength);
    }

    public final Dimension getBondLengthDimension() {
        return Dimension.LENGTH;
    }

    /**
     * Implements collision dynamics for pair attempting to separate beyond
     * tether distance
     */
    public final void bump(Atom[] pair, double falseTime) {
        cPairNbr.reset(pair[0].coord,pair[1].coord);
        ((CoordinatePairKinetic)cPairNbr).resetV();
        dr.E(cPairNbr.dr());
        Vector dv = ((CoordinatePairKinetic)cPairNbr).dv();
        dr.Ea1Tv1(falseTime,dv);
        double r2 = dr.squared();
        double bij = dr.dot(dv);
        
        if (Debug.ON) {
            if (bij<0.0 && Math.abs(r2 - minBondLengthSquared)/minBondLengthSquared > 1.e-9) {
                throw new RuntimeException("atoms "+pair[0]+" "+pair[1]+" not at the right distance "+r2+" "+minBondLengthSquared);
            }
            else if (bij>0.0 && Math.abs(r2 - maxBondLengthSquared)/maxBondLengthSquared > 1.e-9) {
                throw new RuntimeException("atoms "+pair[0]+" "+pair[1]+" not at the right distance "+r2+" "+maxBondLengthSquared);
            }
        }
        
        lastCollisionVirial = 2.0 / (pair[0].type.rm() + pair[1].type.rm()) * bij;
        lastCollisionVirialr2 = lastCollisionVirial / r2;
        dv.Ea1Tv1(lastCollisionVirialr2,dr);
        ((ICoordinateKinetic)pair[0].coord).velocity().PE(dv);
        ((ICoordinateKinetic)pair[1].coord).velocity().ME(dv);
        pair[0].coord.position().Ea1Tv1(-falseTime,dv);
        pair[1].coord.position().Ea1Tv1(falseTime,dv);
    }

    public final double lastCollisionVirial() {
        return lastCollisionVirial;
    }

    public final Tensor lastCollisionVirialTensor() {
        lastCollisionVirialTensor.E(dr, dr);
        lastCollisionVirialTensor.TE(lastCollisionVirialr2);
        return lastCollisionVirialTensor;
    }

    /**
     * Time at which two atoms will reach the end of their tether, assuming
     * free-flight kinematics
     */
    public final double collisionTime(Atom[] pair, double falseTime) {
        cPairNbr.reset(pair[0].coord,pair[1].coord);
        ((CoordinatePairKinetic)cPairNbr).resetV();
        dr.E(cPairNbr.dr());
        Vector dv = ((CoordinatePairKinetic)cPairNbr).dv();
        dr.Ea1Tv1(falseTime,dv);
        double r2 = dr.squared();
        double bij = dr.dot(dv);
        double v2 = dv.squared();
        
        if (Default.FIX_OVERLAP && ((r2 > maxBondLengthSquared && bij > 0.0) ||
                (r2 < minBondLengthSquared && bij < 0.0))) {
            //outside bond, moving apart or overalpped and moving together; collide now
            return 0.0;
        }
        if (Debug.ON && Debug.DEBUG_NOW && ((r2 > maxBondLengthSquared && bij > 0.0) ||
                (r2 < minBondLengthSquared && bij < 0.0))) {
            System.out.println("in P2HardBond.collisionTime, "+pair[0]+" "+pair[1]+" "+r2+" "+bij+" "+maxBondLengthSquared);
            System.out.println(pair[0].coord.position());
            System.out.println(pair[1].coord.position());
        }
        double discr;
        if (bij < 0.0) {
            discr = bij*bij - v2 * (r2 - minBondLengthSquared);
            if(discr > 0) {
                return (-bij - Math.sqrt(discr))/v2 +falseTime;
            }
        }

        discr = bij * bij - v2 * (r2 - maxBondLengthSquared);
        if (Debug.ON && Debug.DEBUG_NOW && ((r2 > maxBondLengthSquared && bij > 0.0) ||
                (r2 < minBondLengthSquared && bij < 0.0))) {
            System.out.println("in P2HardBond.collisionTime, "+v2+" "+discr);
        }
        return (-bij + Math.sqrt(discr)) / v2 + falseTime;
    }

    /**
     * Returns 0 if the bond is within the required distance, infinity if not.
     */
    public double energy(Atom[] pair) {
        cPair.reset(pair[0].coord, pair[1].coord);
        double r2 = cPair.r2();
        return (r2 > maxBondLengthSquared ||
                r2 < minBondLengthSquared) ? Double.MAX_VALUE : 0.0;
    }
    
    public double energyChange() {return 0.0;}

}
