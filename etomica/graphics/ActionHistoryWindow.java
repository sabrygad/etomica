package etomica.graphics;

import java.awt.Color;
import java.awt.TextArea;

import javax.swing.JFrame;

import etomica.api.IAction;
import etomica.data.AccumulatorHistory;
import etomica.data.Data;
import etomica.data.types.DataFunction.DataInfoFunction;

/**
 * Action that opens a new window and dumps the coordinates into the window.
 * @author Andrew Schultz
 */
public class ActionHistoryWindow implements IAction {
    protected final AccumulatorHistory accumulatorHistory;
    
    public ActionHistoryWindow(AccumulatorHistory accumulatorHistory) {
        this.accumulatorHistory = accumulatorHistory;
    }
    
    public void actionPerformed() {
        JFrame f = new JFrame();
        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setBackground(Color.white);
        textArea.setForeground(Color.black);
        Data data = accumulatorHistory.getData();
        Data xData = ((DataInfoFunction)accumulatorHistory.getDataInfo()).getXDataSource().getIndependentData(0);
        for (int i=0; i<data.getLength(); i++) {
            double x = xData.getValue(i);
            double y = data.getValue(i);
            if (Double.isNaN(x) || Double.isNaN(y)) continue;
            textArea.append(x+"\t"+y+"\n");
        }
        f.add(textArea);
        f.pack();
        f.setSize(400,600);
        f.setVisible(true);
    }
}