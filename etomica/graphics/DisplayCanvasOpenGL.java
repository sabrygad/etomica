package etomica.graphics;
import etomica.Phase;
import etomica.PhaseEvent;
import etomica.PhaseListener;
import etomica.SimulationEvent;
import etomica.SpeciesRoot;
import etomica.action.PhaseInflate;
import gl4java.awt.GLAnimCanvas;

import java.awt.Dimension;
import java.awt.Graphics;


/**
 * Parent of classes that use OpenGL to display the phase configuration.
 *
 * @author Steve Hotchkiss
 */
 
public abstract class DisplayCanvasOpenGL extends GLAnimCanvas implements java.io.Serializable, DisplayCanvasInterface, PhaseListener {
    //protected Image offScreen;
    //protected Graphics osg;
        
    protected DisplayPhase displayPhase;
    protected PhaseInflate inflate;

    /**
     * Flag to indicate if display can be resized
     */
    boolean resizable = false;
    /**
     * Flag to indicate if display can be moved
     */
    boolean movable = false;

    /** 
     * Flag to indicate if value of scale should be superimposed on image
     */
    boolean writeScale = false;
    
    /**
     * Variable that sets the quality of the rendered image.
     */
    int quality = DRAW_QUALITY_NORMAL;
          
    /**
     * Variable specifying whether a line tracing the boundary of the display should be drawn
     * Default value is <code>BOUNDARY_OUTLINE</code>
     */
    int drawBoundary = DRAW_BOUNDARY_OUTLINE;
        

    public DisplayCanvasOpenGL(int width, int height) {
        super(width, height);
        //!!!super(false);
        setBackground(java.awt.Color.black);
    }

    public void actionPerformed(PhaseEvent evt) {
        initialize();
    }
    public void actionPerformed(SimulationEvent evt) {actionPerformed((PhaseEvent)evt);}
        
    public void createOffScreen () {
        //if (offScreen == null) { 
            //createOffScreen(getSize().width, getSize().height);
        //}
    }
    public void createOffScreen (int p) {
        //createOffScreen(p,p);
    }
    public void createOffScreen(int w, int h) {
        //offScreen = createImage(w,h);
        //if(offScreen != null) osg = offScreen.getGraphics();
    }
        
    public void doPaint(Graphics g) {}
    
    public void update(Graphics g) {paint(g);}
        
      
    public void setPhase() {
        Phase phase = displayPhase.getPhase();
        inflate = new PhaseInflate(phase);
        ((SpeciesRoot)phase.speciesMaster.node.parentGroup()).addListener(this);
    }
              

    public void setMovable(boolean b) {movable = b;}
    public boolean isMovable() {return movable;}
    public void setResizable(boolean b) {resizable = b;}
    public boolean isResizable() {return resizable;}
    public void setWriteScale(boolean s) {writeScale = s;}
    public boolean getWriteScale() {return(writeScale);}

    public void setQuality(int q) {
      if(q>DRAW_QUALITY_VERY_HIGH)
        q-=DRAW_QUALITY_MAX;
      if(q<DRAW_QUALITY_VERY_LOW)
        q+=DRAW_QUALITY_MAX;
      quality = q;
    }
    public int getQuality() {return(quality);}
    public void setDrawBoundary(int b) {
      if(b>DRAW_BOUNDARY_ALL)
        b-=DRAW_BOUNDARY_MAX;
      else if(b<DRAW_BOUNDARY_NONE)
        b+=DRAW_BOUNDARY_MAX;
      drawBoundary = b;
    }
    public int getDrawBoundary() {return drawBoundary;}

    public void setMinimumSize(Dimension temp) {}
    public void setMaximumSize(Dimension temp) {}
    public void setPreferredSize(Dimension temp) {}
    public void initialize() {}
} //end of DisplayCanvas class

