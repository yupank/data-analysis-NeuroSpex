/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author lssgav
 */

import java.io.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;

public class SpecTransferHandler extends TransferHandler {
    SpecPanel   parentSpec;
    
    Toolkit toolkit;
    public SpecTransferHandler(SpecPanel parent){
        super();
        parentSpec=parent;
        toolkit = Toolkit.getDefaultToolkit();
        
    }
    //public boolean pasteSysClb(){
    public String pasteSysClb(){
        String dataStr;
        Clipboard clb=toolkit.getSystemClipboard();
        //clb.getContents(this);
        try {
            dataStr = (String)clb.getContents(this).getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("importData: unsupported data flavor");
            return null;
        } catch (IOException ioe) {
            System.out.println("importData: I/O exception");
            return null;
        }
        if (dataStr.length()>0){
            //can do paste
            //return parentSpec.pasteASCII(dataStr);
            return dataStr;
        }
        return null;
    }
    public boolean copyData2Sys(){
        Clipboard clb=toolkit.getSystemClipboard();
        NeuroSpex.clbdSpec=parentSpec.getSelectedSeries();  // setting the reference to the data-handling series
        String outValue = parentSpec.getSelectedSeries().data2text(false, "\t");
        Transferable transfer = new StringSelection(outValue);
        try {
            clb.setContents(transfer,(ClipboardOwner)transfer);
        } catch (IllegalStateException ufe) {
            System.out.println("importData: unsupported data flavor");
            return false;
        }            
        return true;
    }
    //method copies the fit results, if exist, for all sweeps to the sustem clipboard using tab as delimiter
    public boolean copyResults2Sys(boolean[] resultMask){
        Clipboard clb=toolkit.getSystemClipboard();
        //String outValue = "here are some results for you";
        //Transferable transfer = new StringSelection(outValue);
        Transferable transfer = new StringSelection(parentSpec.getSelectedSeries().getFitResults(resultMask,"\t"));
        try {
            clb.setContents(transfer,(ClipboardOwner)transfer);
        } catch (IllegalStateException ufe) {
            System.out.println("importData: unsupported data flavor");
            return false;
        }            
        return true;
    }
    
    public boolean importData(TransferHandler.TransferSupport info) {
        String data;
        System.out.println("importData");
        //If we can't handle the import, bail now.
        if (!canImport(info)) {
            return false;
        }

        
        //Fetch the data -- bail if this fails
        try {
            data = (String)info.getTransferable().getTransferData(DataFlavor.stringFlavor);
        } catch (UnsupportedFlavorException ufe) {
            System.out.println("importData: unsupported data flavor");
            return false;
        } catch (IOException ioe) {
            System.out.println("importData: I/O exception");
            return false;
        }
        if (data.length()>0){
            //can do paste
            System.out.println(data);
            return true;
        }
        return false;
    }
    protected Transferable createTransferable(SpecPanel target) {
        System.out.println("exportData");
        //String outValue = target.getTitle();
        String outValue = "some data here";
        outValue+="\t"+target.getName()+"\t"+target.getDataSize();
        //outValue+="copyying";
        /*
        for (int i=0; i< index.length;i++)
            value+="\n"+index[i]+"\t"+index[i]*i;
        */
        return new StringSelection(outValue);
        
        
    }

    /**
     * The list handles both copy and move actions.
     */
    public int getSourceActions(JComponent c) {
        return COPY;
    }

    /** 
     * When the export is complete, remove the old list entry if the
     * action was a move.
     */
    protected void exportDone(JComponent c, Transferable data, int action) {
        if (action != MOVE) {
            return;
        }
        
    }

    
}
