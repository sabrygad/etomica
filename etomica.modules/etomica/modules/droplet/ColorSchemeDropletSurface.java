package etomica.modules.droplet;

import java.awt.Color;

import etomica.api.IAtom;
import etomica.atom.AtomFilterCollective;
import etomica.graphics.ColorSchemeCollective;

public class ColorSchemeDropletSurface extends etomica.graphics.ColorScheme implements ColorSchemeCollective {
    
    public ColorSchemeDropletSurface(AtomFilterCollective liquidFilter) {
        this.liquidFilter = liquidFilter;
        setSurfaceColor(Color.RED);
        setInteriorColor(Color.GREEN);
    }

    public void setSurfaceColor(Color newSurfaceColor) {
        surfaceColor = newSurfaceColor;
    }
    
    public Color getSurfaceColor() {
        return surfaceColor;
    }

    public void setInteriorColor(Color newInteriorColor) {
        interiorColor = newInteriorColor;
    }
    
    public Color getInteriorColor() {
        return interiorColor;
    }
    
    public void colorAllAtoms() {
        liquidFilter.resetFilter();
    }
    
    public Color getAtomColor(IAtom a) {
        return liquidFilter.accept(a) ? interiorColor : surfaceColor;
    }
    
    protected Color surfaceColor, interiorColor;
    protected final AtomFilterCollective liquidFilter;
}
 