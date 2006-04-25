package etomica.modifier;

import etomica.atom.SpeciesAgent;
import etomica.units.Dimension;
import etomica.units.Quantity;

/**
 * Modifier class that enables change of the number of molecules of a particular species
 * in a particular phase.
 */
/*
 * History Created on Jan 31, 2005 by kofke
 */
public class ModifierNMolecule implements Modifier, java.io.Serializable {

    /**
     * @param speciesAgent Agent of the affected species in the affected phase.
     * Cannot be changed after construction.
     */
    public ModifierNMolecule(SpeciesAgent speciesAgent) {
        this.speciesAgent = speciesAgent;
    }

    public void setValue(double d) {
        if (d < 0) d = 0;
        previousValue = mostRecentValue;
        mostRecentValue = (int)d;
        speciesAgent.setNMolecules((int) d);
//        if (this.selector.display != null)
//            this.selector.display.repaint();
//        this.selector.integrator.reset();
    }

    public double getValue() {
        return (speciesAgent != null) ? (double)speciesAgent.getNMolecules() : 0;
    }

    public Dimension getDimension() {
        return Quantity.DIMENSION;
    }
    
    public String getLabel() {
        return speciesAgent.type.getSpecies().getName() + " molecules";
    }
    
    public String toString() {
        return "Change number of "+speciesAgent.type.getSpecies().getName()+
                " molecules from " + previousValue + " to " + mostRecentValue;
    }
    private final SpeciesAgent speciesAgent;
    private int mostRecentValue, previousValue;
}
