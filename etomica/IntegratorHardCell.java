package simulate;
import java.util.Observable;
import java.awt.Color;

public final class IntegratorHardCell extends Integrator {

private Agent nextCollider;
private AtomPair.Iterator.A upPairIterator, downPairIterator;
private Atom.Iterator upAtomIterator, downAtomIterator;
private AtomPair atomPair;

boolean bb = true;
            
public IntegratorHardCell() {
    super();
}

public void registerPhase(Phase p) {
    super.registerPhase(p);
    upPairIterator = p.makeUpIterator();
    downPairIterator = p.makeDownIterator();
    upAtomIterator = parentController.parentSimulation.space().makeUpAtomIterator();
    downAtomIterator = parentController.parentSimulation.space().makeDownAtomIterator();
    atomPair = p.makeAtomPair();
}

          
//--------------------------------------------------------------
// steps all particles across time interval tStep, handling
// all intervening collisions

public void doStep(double tStep) {

//    System.out.println(tStep+" "+nextCollider.getCollisionTime());
    if(tStep < nextCollider.getCollisionTime()) {
        advanceAcrossTimeStep(tStep);
        if(isothermal) {
            scaleMomenta(Math.sqrt(this.temperature/firstPhase.kineticTemperature()));
        }
        return;
    }

    double tStepNew = tStep - nextCollider.getCollisionTime();
    advanceToCollision();
//    Atom a = checkInCells();
//    if(a!=null) {
//        System.out.println("outside");
//    }
    doStep(tStepNew);
    return;
}

//debugging tool
private Atom checkInCells() {
    for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
        a.phase().boundary().centralImage(a.coordinate.position());
        if(!((LatticeSquare.Cell)((Space2DCell.AtomCoordinate)a.coordinate).cell).inCell(a)) {
            return a;
        }
    }
    return null;
}
//--------------------------------------------------------------
         
protected void findNextCollider() {
    //find next collision pair by looking for minimum collisionTime
    double minCollisionTime = Double.MAX_VALUE;
    for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
//        a.setColor(Color.black);
        Agent ia = (Agent)a.ia;
        double ct = ia.getCollisionTime();
        if( ct < minCollisionTime) {
            minCollisionTime = ct;
            nextCollider = ia;
        }
    }
//    if(minCollisionTime <= 0.0) {
//        AtomPair pair = firstPhase.makeAtomPair(nextCollider.atom,nextCollider.collisionPartner);
//        System.out.println("collision time "+minCollisionTime);
//    }
//    if(nextCollider.getCollisionPartner()!=null) {nextCollider.atom.setColor(Color.green);}
//    else if(bb) {nextCollider.atom.setColor(Color.blue); bb=!bb;}
//    else {nextCollider.atom.setColor(Color.red); bb=!bb;}
}
//--------------------------------------------------------------
// advances to next collision, applies collision dynamics to involved 
// species and updates collision time/partners

protected void advanceToCollision() {
            
    advanceAcrossTimeStep(nextCollider.getCollisionTime());
    Atom partner = nextCollider.getCollisionPartner();
    if(partner == nextCollider.atom) {
//        System.out.println("space collision");
        Atom a = nextCollider.atom;
//        a.parentMolecule.parentPhase.boundary().centralImage(a.coordinate.position());  //put atom at central image
//        a.coordinate.assignCell();
        atomPair.reset(a,a);
        nextCollider.getCollisionPotential().bump(atomPair);
        upList(nextCollider.atom);
        downList(nextCollider.atom);
    }
    else {
//        System.out.println("atom collision");
//        Atom partnerNextAtom = partner.nextMoleculeFirstAtom();  //put this back in for multiatomic speciesSwitch; also need to do more work with loop below
//        Atom partnerNextAtom = partner.coordinate.nextNeighbor().atom();
        Atom partnerNextAtom = null;  //remove this -- temporary
        atomPair.reset(nextCollider.atom,partner);
        nextCollider.getCollisionPotential().bump(atomPair);
//        nextCollider.getCollisionPotential().bump(nextCollider.atom,partner);
                
        boolean upListedN = false;
        boolean upListedP = false;
//        for(Atom a=firstPhase.firstAtom(); a!=partnerNextAtom; a=a.nextAtom()) {  //note that nextCollider's or partner's position in linked-list may have been moved by the bump method
//        upAtomIterator.reset(firstPhase.firstAtom());
        upAtomIterator.reset(((Space2DCell)parentController.parentSimulation.space()).cells.firstAtom().atom());  //first atom in first cell
        while(upAtomIterator.hasNext()) {
            Atom a = upAtomIterator.next().atom();
            if(a == partnerNextAtom) break;
            Atom aPartner = ((Agent)a.ia).getCollisionPartner();
            if(aPartner==nextCollider.atom || aPartner==partner) {
                upList(a);
                if(a == nextCollider.atom) {upListedN = true;}
                else if(a == partner) {upListedP = true;}
            }
        }
        if(!upListedN) {upList(nextCollider.atom);}
        if(!upListedP) {upList(partner);}
        downList(nextCollider.atom);
        downList(partner);
    }

    findNextCollider();
}


// --------- advances all atom coordinates by tStep --------

protected void advanceAcrossTimeStep(double tStep) {
            
    if(firstPhase.noGravity) {
        for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
            ((Agent)a.ia).decrementCollisionTime(tStep);
            if(a.isStationary()) {continue;}  //skip if atom is stationary
//            Space.uEa1Tv1(dr,tStep*a.rm,a.p);
//            a.translate(dr);         //needs modification for nonspherical atom
            a.coordinate.translateToward(a.coordinate.momentum(),tStep*a.rm());
        }
    }
    else {
        double t2 = tStep*tStep;
        for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
            ((Agent)a.ia).decrementCollisionTime(tStep);
            if(a.isStationary()) {continue;}  //skip if atom is stationary
//            Space.uEa1Tv1Pa2Tv2(dr,tStep*a.rm,a.p,t2,firstPhase.gravity.gVector);
            a.coordinate.translateToward(a.coordinate.momentum(),tStep*a.rm());
            a.coordinate.translateToward(firstPhase.gravity.gVector,t2);
//            Space.uEa1Tv1(dr,tStep*a.mass,firstPhase.gravity.gVector);
            a.coordinate.accelerateToward(firstPhase.gravity.gVector,tStep*a.mass());
        }
    }
}

/**
* Update of collision list when gravity is changed.
*/
public void update(Observable o, Object arg) {
    if(o instanceof Gravity) {
    for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
        if(a.isStationary() || ((Agent)a.ia).getCollisionPartner().isStationary()) {
            upList(a);
        }
        if(a.isStationary()) {downList(a);}
    }
    findNextCollider();
    }
}
          
//--------------------------------------------------------------

protected void upList(Atom atom) {  //specific to 2D
            
    double minCollisionTime = Double.MAX_VALUE;
    Agent aia = (Agent)atom.ia;
//    if(!atom.isStationary() && atom.type instanceof AtomType.Disk) {  //if mobile, set collision time to time atom takes to move half a box edge
//        double tnew = Math.abs((0.5*atom.phase().parentSimulation.space.getNeighborRadius()-0.5*1.0001*((AtomType.Disk)atom.type).diameter())/Math.sqrt(atom.coordinate.momentum().squared()));  //assumes range of potential is .le. diameter
//        minCollisionTime = tnew;
//        for(int i=Simulation.D-1; i>=0; i--) {
//            double tnew = Math.abs((atom.phase().dimensions().component(i)-1.0001*((AtomType.Disk)atom.type).diameter())/atom.coordinate.momentum(i));  //assumes range of potential is .le. diameter
//            minCollisionTime = (tnew < minCollisionTime) ? tnew : minCollisionTime;
//        }
//        minCollisionTime *= 0.5*atom.mass();
//    }
    aia.setCollision(minCollisionTime, null, null);
            
//    Atom nextMoleculeAtom = atom.nextMoleculeFirstAtom();  //first atom on next molecule
//    int atomSpeciesIndex = atom.getSpeciesIndex();
            
    //Loop through remaining uplist atoms in this atom's molecule
/*    if(atom != atom.parentMolecule.lastAtom) {
        Potential1 p1 = simulation().potential1[atomSpeciesIndex];
        iterator.reset(atom.nextAtom(),atom.parentMolecule.lastAtom,atom,atom);
        while(iterator.hasNext()) {
//            for(Atom a=atom.nextAtom(); a!=nextMoleculeAtom; a=a.nextAtom()) {
//                PotentialHard potential = (PotentialHard)p1.getPotential(atom,a);
//                double time = potential.collisionTime(atom,a);
            AtomPair pair = iterator.next();
            PotentialHard potential = (PotentialHard)p1.getPotential(pair.atom1(),pair.atom2());
            double time = potential.collisionTime(pair);
            if(time < minCollisionTime) {
                minCollisionTime = time;
                aia.setCollision(time,pair.atom2(),potential);
            }
        }
    }
*/            
    //Loop through remaining uplist atoms in firstPhase
//    if(atom.parentMolecule != atom.phase().lastMolecule()) {
        upPairIterator.reset(atom,null,null);
        while(upPairIterator.hasNext()) {
            AtomPair pair = upPairIterator.next();
            if(((Agent)pair.atom2().ia).getCollisionPartner() == atom.coordinate) upList(pair.atom2());  //upList atom could have atom as collision partner if atom was just moved down list
            PotentialHard potential = (PotentialHard)simulation().getPotential(pair);
//            PotentialHard potential = (PotentialHard)simulation().potential2[pair.atom2().getSpeciesIndex()][atomSpeciesIndex].getPotential(atom,pair.atom2());
            double time = potential.collisionTime(pair);
            if(time < minCollisionTime) {
                minCollisionTime = time;
                aia.setCollision(time,pair.atom2(),potential);  //atom2 is inner loop
            }
        }
//    }
//    Potential2[] p2 = firstPhase.potential2[atomSpeciesIndex];
//    for(Atom a=nextMoleculeAtom; a!=null; a=a.nextAtom()) {
//        Potential potential = p2[a.getSpeciesIndex()].getPotential(atom,a);
//        PotentialHard potential = (PotentialHard)firstPhase.potential2[a.getSpeciesIndex()][atomSpeciesIndex].getPotential(atom,a);
//        double time = potential.collisionTime(atom,a);
//        if(time < minCollisionTime) {
//            minCollisionTime = time;
//            aia.setCollision(time,a,potential);
//        }
//    }
}

//--------------------------------------------------------------

protected void downList(Atom atom) {
            
    Atom previousMoleculeAtom = atom.getMolecule().firstAtom().previousAtom();
            
    int atomSpeciesIndex = atom.getSpeciesIndex();
            
    //Loop through remaining downlist atoms in this atom's molecule
/*    if(atom != atom.parentMolecule.firstAtom) {
        iterator.reset(atom.parentMolecule.firstAtom,atom.previousAtom(),atom,atom);
        Potential1 p1 = simulation().potential1[atomSpeciesIndex];
        while(iterator.hasNext()) {
            AtomPair pair = iterator.next();
            Agent aia = (Agent)pair.atom2().ia;  //atom2 is inner loop
            PotentialHard potential = (PotentialHard)p1.getPotential(pair.atom2(),atom);
            double time = potential.collisionTime(pair);
            if(time < aia.getCollisionTime()) {
                aia.setCollision(time,atom,potential);
            }
        }
    }
    */
/*        Potential1 p1 = firstPhase.potential1[atomSpeciesIndex];
        for(Atom a=atom.previousAtom(); a!=previousMoleculeAtom; a=a.previousAtom()) {
            Agent aia = (Agent)a.ia;
            PotentialHard potential = (PotentialHard)p1.getPotential(a,atom);
            double time = potential.collisionTime(a,atom);
            if(time < aia.getCollisionTime()) {
                aia.setCollision(time,atom,potential);
            }
        }*/
            
    //Loop through remaining downlist atoms in firstPhase
//   Potential2[] p2 = firstPhase.potential2[atomSpeciesIndex];
//    if(atom.parentMolecule != atom.phase().firstMolecule()) {
        downPairIterator.reset(atom,null,null);
        while(downPairIterator.hasNext()) {
            AtomPair pair = downPairIterator.next();
            Agent aia = (Agent)pair.atom2().ia;  //atom2 is inner loop
            PotentialHard potential = (PotentialHard)simulation().getPotential(pair);
    //       Potential potential = p2[a.getSpeciesIndex()].getPotential(atom,a);
            double time = potential.collisionTime(pair);
            if(time < aia.getCollisionTime()) {
                aia.setCollision(time,atom,potential);
            }
        }
//    }
 /*   for(Atom a=previousMoleculeAtom; a!=null; a=a.previousAtom()) {
        Agent aia = (Agent)a.ia;
        PotentialHard potential = (PotentialHard)firstPhase.potential2[a.getSpeciesIndex()][atomSpeciesIndex].getPotential(atom,a);
//       Potential potential = p2[a.getSpeciesIndex()].getPotential(atom,a);
        double time = potential.collisionTime(atom,a);
        if(time < aia.getCollisionTime()) {
            aia.setCollision(time,atom,potential);
        }
    }*/
    
}
          
//--------------------------------------------------------------

public void initialize() {
    deployAgents();
    for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
        upList(a);
    }
    findNextCollider();
}
          
//--------------------------------------------------------------

    public void scaleMomenta(double s) {
        double rs = 1.0/s;
        for(Atom a=firstPhase.firstAtom(); a!=null; a=a.nextAtom()) {
            a.coordinate.scaleMomentum(s);
            ((Agent)a.ia).collisionTime *= rs;
        }
    }
            
    public final Integrator.Agent makeAgent(Atom a) {
        return new Agent(a);
    }
            
    public final static class Agent implements Integrator.Agent {  //need public so to use with instanceof
        public Atom atom;
        public double time0;  //time since last collision
        private double collisionTime = Double.MAX_VALUE; //time to next collision
        private Atom collisionPartner;  //next atom scheduled for collision by atom containing this Agent
        private PotentialHard collisionPotential;  //potential governing interaction between collisionPartner and atom containing this Agent
        public double pAccumulator;  //accumulated momentum, for calculation of pressure
        public double qAccumulator;  //accumulated energy, for calculation of heat transfer

        public Agent(Atom a) {
            atom = a;
            zeroAccumulators();
        }
                  
        public final void setCollision(double time, Atom partner, PotentialHard p) {
            collisionTime = time;
            collisionPartner = partner;
            collisionPotential = p;
        }

        public final void decrementCollisionTime(double interval) {
            collisionTime -= interval;
            time0 += interval;
        }
                  
        public final double getCollisionTime() {return collisionTime;}
        public final Atom getCollisionPartner() {return collisionPartner;}
        public final PotentialHard getCollisionPotential() {return collisionPotential;}

        public final void zeroAccumulators() {
            qAccumulator = 0.0;
            pAccumulator = 0.0;
        }
        
    }
}

