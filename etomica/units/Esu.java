package etomica.units;
import etomica.util.Constants;

public final class Esu extends BaseUnit.Charge {

  /**
   * Convenience instance of this unit to permit unit to be assigned
   * without creating a new instance of it each time
   */
    public static final Esu UNIT = new Esu();
    
    private Esu() {
        super(
        	Math.sqrt(Constants.AVOGADRO*1e24*1e-24), //7.76e11; conversion from (g-cm^3/s^2)^(1/2) to (amu-A^3/ps^2)^(1/2)
		    "electrostatic units",
        	"esu"
        	);   
    }
}