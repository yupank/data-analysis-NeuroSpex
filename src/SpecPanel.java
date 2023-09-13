

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yp
 */


import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.event.ActionEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import java.util.*;
import java.io.*;
import java.io.FileNotFoundException;
import java.nio.file.*;
import java.nio.ByteOrder;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.io.BufferedReader;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.TransferHandler;
//import java.lang.Object.*;

//this class provides a user inteface to the single multi-series data set, 
//including drawing, analysis and access to parameters and analysis outputs

public class SpecPanel extends JPanel 
    implements MouseListener, MouseMotionListener {
    private NeuroSpex parentFrame;
    private String xAxisTitle;
    private String yAxisTitle;
    private String DataSetTitle;
    private String fullFileName;
    private String lableStr;
    //physical scale
    private double[] axesParam; //0:1 - xAxisBeg:Scale 2:3 - yAxisBeg:Scale
    private double xAxisBeg;
    private double xAxisScale;
    private double xAxisPos;
    private double xAxisStep;
    private int xAxisTick;
    private double yAxisBeg;
    private double yAxisScale;
    private double yAxisPos;
    private double yAxisStep;
    private int yAxisTick;
    public double mouseX;
    public double mouseX1;
    public double mouseX0;
    public double mouseY;
    private int mouseClk;
    public SpecTransferHandler transferHandler;
    public static SpecSeries transferSeries; // for copying the data between panels
    public static int   transferDataSize;    // binary data flag: 0 - no data available, -1 - text only, > 64 - binary only
    private static int findPeakStart=0;     // sweep numbers for searching peaks in between
    private static int findPeakEnd=0;       
    private Rectangle drawRect;
    private Rectangle plotRect;
    private SpecSweep[] plotSweep;
    private SpecSeries[] dataSeries;
    
    public int focusSer;
    public boolean autoScaleX;
    public boolean autoScaleY;
    
    
    private int nSeries;
    private int nPlot;
    private int nSlope;
    final int MAXSWEEP = 512;      //max sweeps to show
    final int MAXSPEC = 128;
    private int[] xPlot;
    private int[] yPlot;
    final int MAXPOINT = 4096;     // max data point to plot
    final int nPen = 12;
    private Color[] penColor;
    
    
    public SpecPanel(NeuroSpex frame) {
        parentFrame = frame;
        xAxisTitle = "XXXX Axis";
        yAxisTitle = "YYY Axis";
        DataSetTitle = new String();
        lableStr = new String();
        drawRect = new Rectangle();
        plotRect = new Rectangle();
        axesParam= new double[4];
        penColor = new Color[nPen];
        penColor[0]=Color.magenta;  penColor[1]=Color.darkGray; penColor[2]=Color.orange; penColor[3]=Color.blue;
        penColor[4]=Color.black;     penColor[4]=Color.RED;   penColor[5]=Color.green; penColor[6]=Color.gray; penColor[7]=Color.getHSBColor((float)0.108,(float)0.76,(float)0.81);
        penColor[8]=Color.getHSBColor((float)0.513,(float)0.95,(float)0.71); penColor[9]=Color.PINK; penColor[10]=Color.getHSBColor((float)0.5694, (float)0.36, (float)0.95); penColor[11]=Color.getHSBColor((float)0.85,(float)0.52,(float)0.68);
        // testing
        plotSweep = new SpecSweep[MAXSWEEP];
        dataSeries= new SpecSeries[MAXSPEC];
        nPlot = 0; 
        nSlope =0;
        nSeries=0;  focusSer=-1;
        autoScaleX=true;
        autoScaleY=true;
        xPlot = new int[MAXPOINT];
        yPlot = new int[MAXPOINT];
        xAxisBeg = -0.1;
        xAxisScale = 2.2;
        xAxisTick=10;
        yAxisBeg = -100.0;
        yAxisScale = 300.0;
        yAxisTick=10;
        // mouse event processing
        mouseX=xAxisBeg;
        mouseX0=mouseX;
        mouseX1=mouseX;
        mouseY=yAxisBeg;
        mouseClk=0;
        addMouseMotionListener(this);
        addMouseListener(this);
        // clipboard events processing
        transferHandler= new SpecTransferHandler(this);
        setTransferHandler(transferHandler);
    }
    
    
    //adding new empty data series
    public void addSeries()
    {
       int i,j,n;
       SpecSweep buffSweep;    //buffer for the  sweep being created
       float[] crY,crX;           // buffer for the current sweep Y-data
        if (nSeries < MAXSPEC-1) {
            dataSeries[nSeries]= new SpecSeries();
            dataSeries[nSeries].setTitle("series "+(nSeries+1));
            dataSeries[nSeries].IsSelected=false;
            focusSer=nSeries;
            nSeries++;
        }
       
    }
    
    
    //adding data series of supplied size for reading the data into it
    public SpecSweep addSeries(int destSize, String namePrefix)
    {
       int i,j,n;
       SpecSweep buffSweep;    //buffer for the  sweep being created
       
        // buffer for the current sweep Y-data
        if (nSeries < MAXSPEC-1) {
            dataSeries[nSeries]= new SpecSeries();
            if (namePrefix!=null){
                String[] prenom = namePrefix.split(":");
                if (prenom[0].startsWith("Slope") || prenom[0].startsWith("Hst") || prenom[0].startsWith("AC") || prenom[0].startsWith("Peak")){
                    dataSeries[nSeries].setTitle(prenom[0]+"_"+(nSlope+1)+"_"+prenom[1]);
                    nSlope++;
                }
                else
                    dataSeries[nSeries].setTitle(namePrefix+"_ser "+(nSeries+1));
            }
            else
                dataSeries[nSeries].setTitle("series "+(nSeries+1));
            dataSeries[nSeries].IsSelected=true;
            SpecSweep newSweep=dataSeries[nSeries].createComp(destSize);
            focusSer=nSeries;
            nSeries++;
            return newSweep;
        }
        else
            return null;
    }
    
    public void removeSeries(int idx){
        int serIdx,j;
        if (idx<0)
            serIdx = getSelectedIdx();
        else
            serIdx = idx;
        for (j=serIdx;j<nSeries-1;j++){
            dataSeries[j]=dataSeries[j+1];
        }
        nSeries--;
        dataSeries[nSeries-1].IsSelected=true;
        
    }
    //adding sweeps to pool of sweeps for showing      
    public void addSweep(SpecSweep newSweep){
  
        if (plotSweep==null){
           plotSweep = new SpecSweep[MAXSWEEP];
           nPlot = 0; 
          
        }
        
      
        if (nPlot<MAXSWEEP-1) {
            plotSweep[nPlot] = newSweep;
            nPlot++;
        }
        else { 
            for(int i = 2;i<MAXSWEEP;i++) plotSweep[i]=plotSweep[i-1];
            plotSweep[1]=newSweep;
        } 
        //System.out.println("n plot= "+nPlot);
        //repaint();
    }
    public void averageSweeps(){
        int n;
        for (n=0;n<nSeries;n++){
            if (dataSeries[n].IsSelected){
                dataSeries[n].Average();   
            }
        }
        updateSweeps();
        
    }
    public boolean calculateAC(){
        SpecSeries crSeries = getSelectedSeries();
        if (crSeries != null){
            float[] res = new float[3];
            SpecSweep acSweep = addSeries(crSeries.getDataSize(),"AC:"+crSeries.getTitle().substring(0, 8));
            if (crSeries.calculateAC(acSweep, res) !=0){
                JOptionPane.showMessageDialog(this, "Score: "+String.format("%3.2f",res[0])+"  period: "+String.format("%3.2f",res[1]),"AutoCorrelation",JOptionPane.INFORMATION_MESSAGE);
                //System.out.println("period: "+res[1]);
                return true;
            }
            else
                return false;
        } 
        else
            return false;
    }
 
    public boolean calculatePDHist(int tagSweep){
        SpecSeries crSeries = getSelectedSeries();
        float par;
        if (crSeries != null){
            int iSel[] = new int[2];
            crSeries.getSelMarkers(iSel);
            int nHistNode = 121+(iSel[1]-iSel[0])/5;
            if (nHistNode>1001) nHistNode = 2001;
            //SpecSweep histSweep = addSeries(nHistNode,"Hst:ser_"+String.format("%d",getSelectedIdx()+1)+"."+String.format("%d",tagSweep)
            
            //setting default parameters
            float minY = crSeries.getCompAtIdx(tagSweep).minVal();
            float maxY = crSeries.getCompAtIdx(tagSweep).maxVal();
            float kern = (float)0.25;
            
            //histSweep.Zero = minY;    histSweep.Amp = maxY; histSweep.Kf = kern; histSweep.Mode = 101; // Probability Density function
            //obtaining user settings
            String userInput;
            String initValues = "minY: " + String.format("%3.2f",minY)+", maxY: " + String.format("%3.2f",maxY)
                    + ", kernel: " +String.format("%2.1f",kern) +", nNodes: "+ String.format("%d",nHistNode);
            
            userInput = (String)JOptionPane.showInputDialog(this,
                        "Values taken between data points "+String.format("%d",iSel[0])+" and "+String.format("%d",iSel[1])+"\n"
                        + "Set parameters: min value,  max value, kernel size, total nodes",
                        "Probability density function",
                        JOptionPane.PLAIN_MESSAGE,
                        null,
                        null,
                        initValues);
            
            if(userInput != null){
                SpecSweep histSweep = addSeries(nHistNode,"Hst:ser_"+String.format("%d",getSelectedIdx()+1)
                +" p"+String.format("%d",iSel[0])+" to "+String.format("%d",iSel[1]));
                histSweep.Mode = 101; // Probability Density function
                //parsing & checking
                String[] userPars = userInput.split(":|,");
                if (userPars.length>5){
                    //float x = Float.valueOf(userPars[1]);
                    try { histSweep.Zero = Float.valueOf(userPars[1]);}
                        catch (NumberFormatException fe){
                        histSweep.Zero = minY; 
                        System.out.println("parsing error: default minY used");
                    }
                    try { histSweep.Amp = Float.valueOf(userPars[3]);}
                        catch (NumberFormatException fe){
                        histSweep.Amp = maxY; 
                        System.out.println("parsing error: default maxY used");
                    }
                    try { histSweep.Kf = Float.valueOf(userPars[5]);}
                        catch (NumberFormatException fe){
                        histSweep.Kf = kern; 
                        System.out.println("parsing error: default kernel used");
                    }
                }
                else {
                    histSweep.Zero = minY;    histSweep.Amp = maxY; histSweep.Kf = kern; 
                    System.out.println("parsing error: default values used");
                }
                
                if (crSeries.getCompAtIdx(tagSweep).pdHist(histSweep, iSel)){
                    histSweep.updateScale();

                    for (int i=0;i<nSeries-1;i++)
                        dataSeries[i].IsSelected=false;
                    return true;
                }
                else
                    return false;
            
            
            }
            else
                return false;
        }
        else
            return false;
    }
    public boolean calculateSlope(){
        float res[]=new float[3];
        float outDataX[];
        float outDataY[];
        float tmStamp[];
        float tmCrData[];
        float base,stp,ltp,tmZero;
        float tmInt=60;
        int i,iSwp,iStim,nData,nTmCr1,nTmCr2,nDataSer,ser1,ser2,count;
        nDataSer=nSeries-nSlope;
        if (nDataSer<2){
            
            if (nDataSer==1){
                ser1=0;ser2=0;
                iStim=0;
                nData=dataSeries[ser1].getCompSize()-1;
                
                if (nData>3){
                    outDataX = new float[nData];
                    outDataY = new float[nData];
                    tmZero=dataSeries[ser1].getCompAtIdx(1).recTime+tmInt/2;
                    for (i=1;i<=nData;i++){
                       outDataX[i-1]=dataSeries[ser1].getCompAtIdx(i).recTime-tmZero;
                       dataSeries[ser1].getScanAtIdx(res, i);
                       //the slope
                       outDataY[i-1]=res[2];
                    }
                    //avereging the time coures over the time interval
                    nTmCr1=(int)(-outDataX[0]/tmInt);
                    nTmCr2=(int)(outDataX[nData-1]/tmInt)+nTmCr1+2;
                    //System.out.println("n1/2 :" + nTmCr1 + "/ " + nTmCr2);

                    SpecSweep tmCrSweep = addSeries(nTmCr2,"Slope");
                    tmStamp=tmCrSweep.getBase();
                    tmCrData=tmCrSweep.getData();
                    for (i=0;i<nTmCr2;i++){
                        tmStamp[i]=tmInt*(i-nTmCr1);
                        tmCrData[i]=0;
                    }

                    iSwp=0;
                    i=0;
                    count=0;
                    while (iSwp<nData && i<nTmCr2){

                        if (outDataX[iSwp]>=tmStamp[i]-tmInt && outDataX[iSwp]<tmStamp[i]){
                            tmCrData[i]+=outDataY[iSwp];
                            count++;
                        }
                        else {
                            if (count>0)
                                tmCrData[i]/=count; //finalizing the result
                            else{
                                if (i>0)
                                    tmCrData[i]=tmCrData[i-1];
                                else
                                    tmCrData[i]=0;
                            }
                            
                            i++;
                            count=0;
                            
                            
                        }
                        iSwp++;
                    }
                    tmCrSweep.updateScale();
                    return true;
                }
                else {
                    JOptionPane.showMessageDialog(this, "wrong file format");
                    return false;
                }
                
            }
            else
                return false;
        }
        else {
            if (nDataSer==2){
                ser1=0; ser2=1;
            }
            else{
                    ser1=0; ser2=2;
            }
            iStim=dataSeries[ser1].getCompSize()-1;
            nData=iStim+dataSeries[ser2].getCompSize()-1; // 0th sweep has to be ignored
            
            tmZero=dataSeries[ser1].getCompAtIdx(iStim).recTime+tmInt/4;
            //System.out.println("zero:" + tmZero);
            if (iStim>5 && nData > 10){
                outDataX = new float[nData];
                outDataY = new float[nData];
                for (i=1;i<=iStim;i++){
                   outDataX[i-1]=dataSeries[ser1].getCompAtIdx(i).recTime-tmZero;
                   dataSeries[ser1].getScanAtIdx(res, i);
                   outDataY[i-1]=res[2];
                }
                tmZero=dataSeries[ser2].getCompAtIdx(1).recTime-tmInt/4;
                for (i=iStim+1;i<=nData;i++){
                   outDataX[i-1]=dataSeries[ser2].getCompAtIdx(i-iStim).recTime-tmZero;
                   dataSeries[ser2].getScanAtIdx(res, i-iStim);
                   outDataY[i-1]=res[2];
                }
                //normalization
                base=0; count=0;
                for (i=iStim-1;i>=iStim-40 && i>0;i--){
                    base+=outDataY[i];
                    count++;
                }
                base/=count;

                for (i=0;i<nData;i++)
                    outDataY[i]/=base;
                
                //avereging the time coures over the time interval
                nTmCr1=(int)(-outDataX[0]/tmInt);
                nTmCr2=(int)(outDataX[nData-1]/tmInt)+nTmCr1+2;
                //System.out.println("n1/2 :" + nTmCr1 + "/ " + nTmCr2);
                
                SpecSweep tmCrSweep = addSeries(nTmCr2,"Slope");
                tmStamp=tmCrSweep.getBase();
                tmCrData=tmCrSweep.getData();
                for (i=0;i<nTmCr2;i++){
                    tmStamp[i]=tmInt*(i-nTmCr1);
                    tmCrData[i]=0;
                }
                    
                iSwp=0;
                i=0;
                count=0;
                while (iSwp<nData && i<nTmCr2){
                    
                    if (outDataX[iSwp]>=tmStamp[i]-tmInt && outDataX[iSwp]<tmStamp[i]){
                        tmCrData[i]+=outDataY[iSwp];
                        count++;
                    }
                    else {
                        tmCrData[i]/=count; //finalizing the result
                        i++;
                        count=0;
                    }
                    iSwp++;
                }
                if (nTmCr1 > 10 && nTmCr2 - nTmCr1 > 15){
                    base = 0; stp = 0; ltp = 0;
                    for (i=nTmCr1-9;i<nTmCr1+1;i++)
                        base+=tmCrData[i]/10;
                    for (i=nTmCr2-10;i<nTmCr2;i++)
                        ltp+=tmCrData[i]/10;
                    for (i=nTmCr1+1;i<nTmCr1+4;i++)
                        stp+=tmCrData[i]/3;
                    for (i=0;i<=nTmCr1;i++)
                        tmCrData[i]=(float)0.5*tmCrData[i]+(float)0.5*base;
                    for (i=nTmCr2-15;i<nTmCr2;i++)
                        tmCrData[i]=(float)0.5*tmCrData[i]+(float)0.5*ltp;
                    if (stp>ltp)
                        for(i=nTmCr1+6;i>=nTmCr1+1;i--){
                            stp+=(i-nTmCr1-5.5)+ltp/5;
                        } 
                }
                tmCrSweep.updateScale();
                for (i=0;i<nSeries-1;i++)
                    dataSeries[i].IsSelected=false;
                
                return true;
            }
            else
                return false;
        }
    }
    public void clearAllSweeps(){
        nPlot=0;
    }
    public void clipArtifact(boolean ignoreSel){
        
        SpecSeries tagSeries=this.getSelectedSeries();
        int nComp=tagSeries.getCompSize();
        for (int j=0;j<nComp;j++)
                        tagSeries.clipAtIdx(j, ignoreSel); 
        updateSweeps();
        
    }
    public boolean detectPeaks(){
        SpecSeries tagSeries = getSelectedSeries();
        int i,j,nDestPoint, nTagPoint, nPeak;
        String[] userPars;
        int totPeak=0;
        float[] locX = new float[200];
        float [] crX, crY; // leading arrays for shifting data windows
        float [] destX, destY; // lagging array for shifting data windows
        float[] Filter  = {SpecSeries.findSigXhigh,SpecSeries.findSigXlow, SpecSeries.findSigYlow, 0};
        float dW = (float)1.5*Filter[0];
        int[] swpNum = new int[2];
        nPeak = 0;
        if (tagSeries != null){
            int nSweep = tagSeries.getCompSize();
            nTagPoint = tagSeries.getDataSize();
            // in case of the very large dataset, user-selected sweep
            if (findPeakStart == 0 && findPeakEnd == 0){
                findPeakStart = 1; findPeakEnd = nSweep-1;
            }
            
            String userInput;
            String initValues = String.format("%d",findPeakStart)+" - " + String.format("%d",findPeakEnd);
            userInput = (String)JOptionPane.showInputDialog(this,
                            "in the sweep range from - to",
                            "Search for peaks",
                            JOptionPane.PLAIN_MESSAGE,
                            null,
                            null,
                            initValues);
            //parsing & checking
            j=0;
            if (userInput != null){
                userPars = userInput.split("-| |:");
           
                for (i=0;i<userPars.length && j<2;i++)
                    if ( userPars[i].matches("[0-9]+") ){
                        swpNum[j]=Integer.valueOf(userPars[i]);
                        j++;
                    }
            }
            if (j>1){
                findPeakStart = Math.min(swpNum[0], swpNum[1]);
                findPeakEnd = Math.max(swpNum[0], swpNum[1]);
                if (findPeakStart<1) findPeakStart = 1;
                if (findPeakEnd > nSweep-1) findPeakEnd = nSweep-1;
            }
            else {
                findPeakStart = 1; findPeakEnd = nSweep-1; 
            }
            //creating new data series to hold the peaks
            //searching and creating peaks 
            nDestPoint = tagSeries.getCompAtIdx(0).getNearestIdx(3.3*dW) - tagSeries.getCompAtIdx(0).getNearestIdx(0)-1;
            addSeries(nDestPoint,"PeakSet:"+tagSeries.getTitle().substring(0, 8));
            //data series for storing the detected peaks
            SpecSeries destSeries = dataSeries[nSeries-1];
            
            //calculating x-base for the data windows surrounding each peak
            crX = tagSeries.getXBase();
            float xShift = crX[0];
            destX = destSeries.getXBase();
            for (j=0;j<nTagPoint;j++)
                crX[j] -= xShift;
            for (j=0;j<nDestPoint;j++)
                destX[j] = crX[j];
            for (j=findPeakStart;j<=findPeakEnd;j++){
                // searching for local peaks
                tagSeries.selComp(0,1);
                tagSeries.selComp(j,0);
                tagSeries.setTagComp(j);
                //System.out.println("tag comp: "+tagSeries.TagComp);
                nPeak += tagSeries.GatherPeaks(j, destSeries,locX);
                //parentFrame.updateSweepTableRowSelection(j, false);
                //parentFrame.updateSweepTableRowSelection(j+1, true);
                parentFrame.updateDataInfo();
                parentFrame.updateSweepSelection();
                //JOptionPane.showMessageDialog(this, "sweep:"+String.format("%d",j)+ " found  "+String.format("%d",nPeak)+" peaks","Peak search",JOptionPane.INFORMATION_MESSAGE);
            }
            if (destSeries.getCompSize()>2)
                destSeries.removeComp(1);
            //creating at least one theoreticl curve for data fit
            NeuroSpex.currFitPar.setParam(destSeries.getCompAtIdx(1).getFitParam());
            destSeries.createFitComp( NeuroSpex.currFitPar);
            parentFrame.updateDataInfo();
            
            for (j=0;j<nTagPoint;j++)
                crX[j] += xShift;          
            findPeakStart = findPeakEnd + 1; findPeakEnd = nSweep-1;
            if (findPeakStart >= nSweep){
                 findPeakStart = 0; findPeakEnd = 0;
            }
            if (nPeak > 0){
                dataSeries[nSeries-1].IsSelected = true;
                for (i=0;i<nSeries-1;i++)
                            dataSeries[i].IsSelected=false;
                JOptionPane.showMessageDialog(this,  " total  "+String.format("%d",nPeak)+" peaks","Peak search",JOptionPane.INFORMATION_MESSAGE);
                return true;
            }
            else    
                return false;
        }
        else
            return false;
    }
    // this methods goes through each data sweep of currently selected data series corrspondingly 
    // calling SpecSeries AutoFit method and notifies user if fit quality is below set criteria
    // depending on user input, methods fits data again with new initial paramaters 
    // or accepts fit results with optional manual correction or discards the data (if it is a peak found by FindPeaks() method
   
    public void fitThrough(){
        int userInput,tagComp;
        float[] fitRes;
        boolean fitStop = false;
        SpecSeries tagSeries = dataSeries[getSelectedIdx()];
        int endComp = tagSeries.BegFitComp;
        tagComp = 1;
        userInput = 1;
        while (!fitStop){
            if (tagSeries.setTagComp(tagComp)){
                tagSeries.AutoFit();
                parentFrame.updateFitAccurLabel(tagSeries.fitAccuracy,tagSeries.TagComp);
                parentFrame.updateDataInfo();
                repaint();
                //  int   userInput = JOptionPane.showConfirmDialog(this, String.format("TagComp: %d nFitRes: %d",tagSeries.TagComp, nRes),
                //                    "Continue to fit data ?", JOptionPane.YES_NO_CANCEL_OPTION);
                //  fitStop = (userInput == JOptionPane.CANCEL_OPTION) || (tagComp>=endComp);
                if (tagSeries.fitAccuracy > SpecSeries.fitAccurH || tagSeries.checkFitAtBorder()){
                    userInput = parentFrame.DataFitResultAction(tagSeries);
                    if (userInput == NeuroSpex.FIT_RETRY || userInput == NeuroSpex.FIT_ACCEPT ){
                        parentFrame.getUserFitResult(tagSeries);
                        if (userInput == NeuroSpex.FIT_RETRY){
                            fitRes = tagSeries.getCompAtIdx(tagSeries.TagComp).getFitResult();
                            tagSeries.convertFitResult2Par(fitRes);
                            //going through the same sweep again
                            tagComp--;
                        }
                    }
                    else {
                        if ( userInput == NeuroSpex.FIT_DISCARD ){
                            tagSeries.removeComp(tagComp);
                            tagComp--;
                        }
                       
                    }
                        //System.out.println("input: "+userInput);
                }
                fitStop = (userInput == NeuroSpex.FIT_STOP) || (tagComp>=endComp);
                tagComp++;
            }
            else 
                //reached the end of recorings
                fitStop = true; 
        }
    }  
    public void drawAxes(Graphics g){
        double tick1, tick2;
        int x1,x2,y1,y2,x3,y3,lableW,lableH;
        g.setFont(new Font("Arial",Font.TRUETYPE_FONT,12));
        FontMetrics fm=g.getFontMetrics();
        lableH=fm.getHeight();
        g.setColor(Color.gray);
        x1=plotRect.x;  y1=plotRect.y+plotRect.height;
        x2=x1+plotRect.width;  y2=y1;
        g.drawLine(x1, y1, x2, y2);
        //drwing X-tics
        tick1=0;tick2=xAxisStep/2;
        while (tick2<xAxisScale){ 
            x3=(int)(x1+(xAxisBeg+tick1-xAxisBeg)*plotRect.width/xAxisScale);
            y2=y1+(int)(0.018*plotRect.height);
            //major tick lable, centre alignmnet
            g.setColor(Color.darkGray);
            if (Math.abs(xAxisBeg+tick1)>=10)
                lableStr=String.format("%3.0f", xAxisBeg+tick1);
            else
                if (Math.abs(xAxisBeg+tick1)>=0.1)
                    lableStr=String.format("%3.1f", xAxisBeg+tick1);
                    else
                        lableStr=String.format("%3.2f", xAxisBeg+tick1);
            lableW=fm.stringWidth(lableStr);
            g.drawString(lableStr,x3-lableW/2,y2+(int)(0.8*lableH));
            g.setColor(Color.gray);
            g.drawLine(x3, y1, x3, y2);
            g.setColor(Color.gray);
            //minor tick
            x3=(int)(x1+(xAxisBeg+tick2-xAxisBeg)*plotRect.width/xAxisScale);
            y2=y1+(int)(0.011*plotRect.height);
            g.drawLine(x3, y1, x3, y2);
            tick1+=xAxisStep;
            tick2+=xAxisStep;
        }
        
        x1=plotRect.x;  y1=plotRect.y+plotRect.height;
        x2=x1;  y2=plotRect.y;
        g.drawLine(x1, y1, x2, y2);
        //drwing Y-tics
        tick1=0;tick2=yAxisStep/2;
        while (tick2<yAxisScale){ 
            y3=(int)(y1-(yAxisBeg+tick1-yAxisBeg)*plotRect.height/yAxisScale);
            x2=x1-(int)(0.018*plotRect.height);
            //major tick lable, centre alignmnet
            g.setColor(Color.darkGray);
            if (Math.abs(yAxisBeg+tick1)>=10)
                lableStr=String.format("%3.0f", yAxisBeg+tick1);
            else
                if (Math.abs(yAxisBeg+tick1)>=0.1)
                    lableStr=String.format("%3.1f", yAxisBeg+tick1);
                    else
                        lableStr=String.format("%3.2f", yAxisBeg+tick1);
            
            
            lableW=fm.stringWidth(lableStr);
            g.drawString(lableStr,x2-lableW-2,y3+(int)(0.3*lableH));
            g.setColor(Color.gray);
            g.drawLine(x1, y3, x2, y3);
            g.setColor(Color.gray);
            //minor tick
            y3=(int)(y1-(yAxisBeg+tick2-yAxisBeg)*plotRect.height/yAxisScale);
            x2=x1-(int)(0.011*plotRect.height);            
            g.drawLine(x1, y3, x2, y3);
            tick1+=yAxisStep;
            tick2+=yAxisStep;
        }
        //markers
        float[] marker=new float[4];
        if(dataSeries[getSelectedIdx()].getPrimeMarkers(marker)){
            g.setColor(Color.LIGHT_GRAY);
            for (int i=0;i<2;i++){
                x1=plotRect.x;  y1=plotRect.y+plotRect.height;
                x2=(int)(x1+(marker[2*i]-xAxisBeg)*plotRect.width/xAxisScale);
                y2=(int)(y1-(marker[2*i+1]-yAxisBeg)*plotRect.height/yAxisScale);
                x1=x2-plotRect.width/20;
                x3=x2+plotRect.width/20;
                y1=y2-plotRect.height/20;
                y3=y2+plotRect.height/20;
                g.drawLine(x1, y2, x3, y2);
                g.drawLine(x2, y1, x2, y3);
            }
        }
        
    }
    public void drawSweep(Graphics g, int sweepNum){
        int plotN;
        axesParam[0]=xAxisBeg; axesParam[1]=xAxisScale; axesParam[2]=yAxisBeg; axesParam[3]=yAxisScale;
        plotN=plotSweep[sweepNum].getPlot(xPlot, yPlot, axesParam, plotRect);
        if (plotN>0)
            g.drawPolyline(xPlot,yPlot,plotN);

        
    }
     public String getTitle(){
        //System.out.println(DataSetTitle+xAxisTitle);
        return (DataSetTitle);
    }
    public SpecSeries getDataSeries(int nSer){
        if (nSer<nSeries && nSer >= 0)
            return dataSeries[nSer];
        else
            return null;
    }
    public int getDataSize(){
        return nSeries;
    }
    public int getSelectedIdx(){
        int i,idx=0;
        for (i=0;i<nSeries;i++){
            if (dataSeries[i].IsSelected)
                idx=i;
        }
        return idx;
    }
    public SpecSeries getSeriesAtIdx(int idx){
        if (idx>=0 && idx<nSeries)
            return dataSeries[idx];
        else
            return null;
    }
    public SpecSeries getSelectedSeries(){
        if (nSeries>0)
            return dataSeries[getSelectedIdx()];
        else
            return null;
    }
    @Override
    public void mouseMoved(MouseEvent e) {
        String labSt1,labSt2;
        //System.out.print("Mouse: " +e.getX());
        //System.out.println(", "+ e.getY());
        mouseX=xAxisBeg+xAxisScale*(e.getX()-plotRect.x)/plotRect.width;
        mouseY=yAxisBeg+yAxisScale*(plotRect.y+plotRect.height-e.getY())/plotRect.height;
        if (Math.abs(mouseX)>=500)
                labSt1=String.format("%3.0f", mouseX);
        else
            if (Math.abs(mouseX)>=10)
                labSt1=String.format("%3.1f", mouseX);
            else
                if (Math.abs(mouseX)>=0.1)
                    labSt1=String.format("%3.2f", mouseX);
                else
                    labSt1=String.format("%3.3f", mouseX);
        if (Math.abs(mouseY)>=500)
                labSt2=String.format(",%3.0f", mouseY);
        else
            if (Math.abs(mouseY)>=10)
                labSt2=String.format(",%3.1f", mouseY);
            else
                if (Math.abs(mouseY)>=0.1)
                    labSt2=String.format(",%3.2f", mouseY);
                        else
                            labSt2=String.format(",%3.3f", mouseY);
        //lableStr=String.format("[X,Y]: %3.2f, %3.3f", mouseX, mouseY);
        parentFrame.updateLabel("[X,Y]: "+labSt1+labSt2);
        //drawAxesOnly = true;
        //repaint();
        //drawAxesOnly = false;
    }
    @Override
    public void mouseDragged(MouseEvent e) {
        //System.out.print("Mouse Drag: " +e.getX());
        //System.out.println(", "+ e.getY());
    }
    @Override
    public void mousePressed(MouseEvent e) {
        //System.out.println("Mouse pressed ");
                
    }
    @Override
    public void mouseClicked(MouseEvent e) {
        
        if (e.getButton()==MouseEvent.BUTTON1){
            //System.out.println("Mouse Clicked at "+mouseX);
            if(mouseClk<1)mouseClk++;
            else {
                //not double click at the same position
                if (Math.abs(mouseX-mouseX0)>1.0e-6*mouseX){
                    for (int n=0;n<nSeries;n++){
                        if (dataSeries[n].IsSelected){
                            dataSeries[n].selChange((float)mouseX0, (float)mouseX);
                            parentFrame.updateSweepSelection();
                        }
                    }
                    mouseClk=1;
                   
                }
                else{
                    mouseClk=0;
                    dataSeries[getSelectedIdx()].selClear();
                }
            }
        }
        else{
            //Right button click sets backround markers
            
            int clk=e.getClickCount();
            //if ( ((e.getModifiersEx()& MouseEvent.SHIFT_DOWN_MASK) !=0)&& clk==2 ){
                //System.out.println("n clicks= "+clk);
            if ( ((e.getModifiersEx()& MouseEvent.SHIFT_DOWN_MASK) !=0)){
                if (clk<2)
                {
                    
                    //getSelectedSeries().clearMarkAt((float)mouseX);
                }
                else
                    getSelectedSeries().clearAllMark();
            }
            else 
                //System.out.println("X:Y = "+mouseX+" : "+mouseY);
                getSelectedSeries().AddMark((float)mouseX, (float)mouseY);
            
        }
        mouseX0=mouseX;
        repaint();
    }
    @Override
    public void mouseReleased(MouseEvent e) {
        //System.out.println("Mouse pressed ");
                
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        //System.out.println("Mouse exited ");
                
    }
    @Override
    public void mouseEntered(MouseEvent e) {
        //System.out.println("Mouse exited ");
                
    }  
    //public boolean pasteASCII (String dataStr){
    public boolean pasteSweeps(){
        if (NeuroSpex.clbdSpec!=null){
            if (nSeries>0){
                getSelectedSeries().insertCompFrom(NeuroSpex.clbdSpec, false);
            }
            else {
                addSeries();
                dataSeries[nSeries-1].insertCompFrom(NeuroSpex.clbdSpec, false);
                dataSeries[nSeries-1].IsSelected=true;
            }
            NeuroSpex.clbdSpec=null;
            return true;
        }
        else {
            JOptionPane.showMessageDialog(this, "internal clipboard is empty");
            return false;
            }
    }
    public boolean pasteASCII (){
        String dataStr=this.transferHandler.pasteSysClb();
        if(dataStr!=null){
            if (dataStr.length()>8){
                dataSeries[nSeries]= new SpecSeries();
                dataSeries[nSeries].setTitle("series "+(nSeries+1));
                //System.out.println(dataStr);
                if (dataSeries[nSeries].text2Data(dataStr)>1){
                    dataSeries[nSeries].IsSelected=true;
                    focusSer=nSeries;
                    nSeries++;
                    return true;

                }
                else {
                    JOptionPane.showMessageDialog(this, "wrong data format");
                    return false;
                }

            }
            else
            return false;
        }
        else {
            JOptionPane.showMessageDialog(this, "system clipboard is empty");
            return false;
        }
    }
    
    public boolean readASCII (File readFile) {
        String lineRead, separStr;
        int nLine,row,col,nSweep,nPoint;
        List<String> lines;
        lines=new ArrayList<String>(100);
        
        if (readFile.canRead()){
            long size=readFile.length();
            
            try (BufferedReader reader = Files.newBufferedReader(readFile.toPath())) {
                lineRead = null; 
                while ((lineRead = reader.readLine()) != null) {
                    if ((!lineRead.startsWith(";"))&&(!lineRead.startsWith("/"))&&(!lineRead.startsWith("#"))) // if not the comment line
                        lines.add(lineRead);
                }

                reader.close();
            }
            catch (IOException x){
                System.err.format("IOException: %s%n", x);
            }
            nLine=lines.size();

            if (nLine>1) {
                //System.out.println(lines.get(0));

                String[] numStr1=lines.get(0).split("[,\t]",-1);
                String[] numStr2=lines.get(1).split("[,\t]",-1);
                if (numStr1.length>=numStr2.length){
                    //creating new data series;
                    dataSeries[nSeries]= new SpecSeries();
                    dataSeries[nSeries].setTitle("series "+(nSeries+1));
                    
                    if (dataSeries[nSeries].text2Data(lines)>1){
                        dataSeries[nSeries].IsSelected=true;
                        focusSer=nSeries;
                        nSeries++;
                        return true;
                    
                    }
                    else {
                        
                        return false;
                    }

                }
                else {
                    JOptionPane.showMessageDialog(this, "wrong file format");
                    return false;
                }
                
            }
            else
                return false;
        }
        else 
            return false;
        
    }
    /*
    public void writeASCII (File outFile, String delimiter){
        String outStr = getSelectedSeries().data2text(true, delimiter);
        try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath(),StandardOpenOption.CREATE,StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(outStr, 0, outStr.length());
            writer.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

    }
    */
    public void writeASCII (File outFile, String delimiter){
        
        String outStr = getSelectedSeries().data2text(true, delimiter);
        try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath(),StandardOpenOption.CREATE,StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(outStr, 0, outStr.length());
            writer.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }

    }
    public boolean readWCPX(File readFile){
        //System.out.println("reading WCP");
        int MaxL=1024;
        ByteBuffer dataBuf;
        byte[]  cbuf=new byte[MaxL];
        int[] ChanOffset    =new int[8];
	int[] ChanOffsetRead=new int[8];
	int[] ChanGainRead  =new int[8];
        float	GrNum,GrNum0, dT1,dT0, Time1,Time0;
	float[]	ChanGain =new float[8];
	float[]	MaxAD    =new float[8];
	float[]	DataX;
	float[] DataY;
        float ADCMax=(float)2047.0;
        String lineRead,StrRead,StrPar,StrNum;
        short DataByte;
        SpecSweep crSweep;
        int nLine,row,col,nSweep,nPoint,ChNum,nData;
        int i,iOff,j,headBytes,nByteRead,readSize,readPos,ln,crChar;
        int nChan=0;    int nRec=0; int	ReadGood=0; int nDataClip=0;
        int nBlockHead=0;   int nBlockAnal=0;   int nBlockData=0;
        for(i=0;i<8;i++){
            ChanOffsetRead[i]=0;
            ChanGainRead[i]=0;
        }
        
        if (readFile.canRead()){
            long size=readFile.length();
            //in = new DataInputStream(new
            try { FileInputStream inStream = new FileInputStream(readFile);
            
                //headBuf= ByteBuffer.allocate(MaxL);
                //headBuf.order(ByteOrder.nativeOrder());
                //headBytes=btChannel.read(headBuf);
                //headBytes=inStream.read(cbuf,0,MaxL);
                headBytes=inStream.read(cbuf);
                lineRead= new String(cbuf);
                nByteRead=0; ln=0;
                while (nByteRead<headBytes && ln<64 &&(ReadGood==0))
		{
			i=lineRead.indexOf("\n",nByteRead);
                        if (i>0)
                        {
                            StrRead=lineRead.substring(nByteRead,i);
                            nByteRead=i+1;    // characters read so far
                            j=StrRead.indexOf("=");
                            StrPar=StrRead.substring(j+1, StrRead.length()-1);
                            crChar=0;
                            //seeking for parameters' names
                            if (StrRead.charAt(crChar)=='N')
                            {
                                    crChar++;
                                    switch (StrRead.charAt(crChar))
                                    {
                                            case 'C':	nChan= Integer.parseInt(StrPar);    break;
                                            case 'R':	nRec=Integer.parseInt(StrPar);	break;
                                            case 'B':	
                                                    crChar++;
                                                    switch (StrRead.charAt(crChar))
                                                    {
                                                            case 'H':	nBlockHead=Integer.parseInt(StrPar);	break;
                                                            case 'A':	nBlockAnal=Integer.parseInt(StrPar);	break;
                                                            case 'D':	nBlockData=Integer.parseInt(StrPar);	break;
                                                    }
                                                    break;
                                    }
                            }
                            if ((StrRead.charAt(0)=='A')&&(StrRead.charAt(3)=='M'))
				ADCMax=Float.parseFloat(StrPar);
                            if (StrRead.charAt(0)=='Y'){
                                StrNum=StrRead.substring(2,3);
                                ChNum=Integer.parseInt(StrNum);                                    
                                if (StrRead.charAt(1)=='G'){
                                    if (ChanGainRead[ChNum]==0){
                                            ChanGain[ChNum]=Float.parseFloat(StrPar); ChanGainRead[ChNum]++;
                                    }
                                }
                                if (StrRead.charAt(1)=='O'){
                                        if (ChanOffsetRead[ChNum]==0){
                                                ChanOffset[ChNum]=Integer.parseInt(StrPar); ChanOffsetRead[ChNum]++;
                                        }
                                }
                            }
                            ReadGood=nChan*nBlockHead*nBlockAnal*nBlockData*ChanGainRead[0];
                            ln++;
                        }
                        else {
                            //System.out.println("bad chars at line"+ln);
                            ln++;
                        }
                }
                
                //reading data in binary format
                if (ReadGood>0){
                    nByteRead=256*nBlockData;
                    nData=nByteRead/nChan;
                    readSize=nBlockAnal*512+2*nByteRead;
                    byte[] readBytes= new byte[readSize];
                    
                    byte[] fBuf=new byte[4];
                    byte[]  sBuf=new byte[2];
                    for (ChNum=0;ChNum<nChan;ChNum++) {
                        //reading channels into separate data series
                        if (nChan>1){	
                            if (JOptionPane.showConfirmDialog(parentFrame, "Channel # "+ChNum, "Read Channel ?", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                             DataByte=1;
                            else DataByte=0;
                        }
                        else
                                DataByte=1;
                        if (DataByte>0){
                            //btChannel.position(nBlockHead);
                            Time0=0; 
                            dT0=-1;
                            GrNum0=1;
                            readPos=nBlockHead;
                            headBytes=0;
                            for (j=0;j<nRec;j++){
                                
                                headBytes=inStream.read(readBytes);
                                
                                dataBuf=ByteBuffer.wrap(readBytes);
                                dataBuf.order(ByteOrder.nativeOrder());
                                
                                if(headBytes>0){
                                    iOff=12;
                                    
                                    GrNum=dataBuf.getFloat(iOff); iOff+=4;
                                    Time1=dataBuf.getFloat(iOff); iOff+=4;
                                    dT1=dataBuf.getFloat(iOff); iOff+=4;
                                    for (i=0;i<8;i++){
                                        MaxAD[i]=dataBuf.getFloat(iOff);
                                        iOff+=4;
                                    }
                                   
                                    if (dT1!=dT0){
                                      crSweep=addSeries(nData,"Ch"+ChNum);
                                      dT0=dT1;
                                    }
                                    else
                                        crSweep=dataSeries[nSeries-1].createComp(nData);
                                    if (j==0) Time0=Time1;
                                    nSweep=dataSeries[nSeries-1].getCompSize();
                                    if (nSweep<=2){
                                        DataX=dataSeries[nSeries-1].getXBase();
                                        for (i=0;i<nData;i++)
                                            DataX[i]=(float)(dT1*1000.0*i);
                                    }
                                    //reading data into the last sweep of last data series
                                    if (crSweep!=null){
                                        nDataClip=0;
                                        DataY=crSweep.getData();
                                        for (i=0;i<nData;i++){
                                            iOff=nBlockAnal*512+2*i*nChan+ChanOffset[ChNum];
                                            
                                            DataByte=dataBuf.getShort(iOff);
                                            if ((DataByte>=32766)||(DataByte<=-32767)) nDataClip++;
                                            DataY[i]=(float)(MaxAD[ChNum]*DataByte/(ChanGain[ChNum]*ADCMax));
                                        }
                                        crSweep.recTime=Time1-Time0;
                                        crSweep.updateScale();
                                        if (nDataClip>nData/5) //unfinished or bad recording sweep
                                            dataSeries[nSeries-1].removeComp(nSweep-1);
                                    }
                                    
                                }
                                  
                            }
                           
                        }
                    } //end of reading channels
                    inStream.close();
                    return true;
                
                }
                
                else {
                    inStream.close();
                    return false;
                }
               
            }
            catch (Exception x){
                System.err.format("IOException: %s%n", x);
            }
            
        return true;
        }
        else 
            return false;
        
    }
    public boolean readWCP(File readFile){
        //System.out.println("reading WCP");
        int MaxL=1024;
        ByteBuffer headBuf, dataBuf;
        byte[]  cbuf=new byte[MaxL];
        int[] ChanOffset    =new int[8];
	int[] ChanOffsetRead=new int[8];
	int[] ChanGainRead  =new int[8];
        float	GrNum,GrNum0, dT1,dT0, Time1,Time0;
	float[]	ChanGain =new float[8];
	float[]	MaxAD    =new float[8];
	float[]	DataX;
	float[] DataY;
        float ADCMax=(float)2047.0;
        String lineRead,StrRead,StrPar,StrNum;
        short DataByte;
        SpecSweep crSweep;
        int nLine,row,col,nSweep,nPoint,ChNum,nData;
        int i,iOff,j,headBytes,nByteRead,ln,crChar;
        int nChan=0;    int nRec=0; int	ReadGood=0; int nDataClip=0;
        int nBlockHead=0;   int nBlockAnal=0;   int nBlockData=0;
        for(i=0;i<8;i++){
            ChanOffsetRead[i]=0;
            ChanGainRead[i]=0;
        }
        
        if (readFile.canRead()){
            long size=readFile.length();
            try (SeekableByteChannel btChannel = Files.newByteChannel(readFile.toPath())) {
                headBuf= ByteBuffer.allocate(MaxL);
                headBuf.order(ByteOrder.nativeOrder());
                headBytes=btChannel.read(headBuf);
                for (i=0;i<MaxL;i++)
                    cbuf[i]=headBuf.get(i);
                lineRead= new String(cbuf);
                nByteRead=0; ln=0;
                while (nByteRead<headBytes && ln<64 &&(ReadGood==0))
		{
			i=lineRead.indexOf("\n",nByteRead);
                        if (i>0)
                        {
                            StrRead=lineRead.substring(nByteRead,i);
                            nByteRead=i+1;    // characters read so far
                            j=StrRead.indexOf("=");
                            StrPar=StrRead.substring(j+1, StrRead.length()-1);
                            crChar=0;
                            //seeking for parameters' names
                            if (StrRead.charAt(crChar)=='N')
                            {
                                    crChar++;
                                    switch (StrRead.charAt(crChar))
                                    {
                                            case 'C':	nChan= Integer.parseInt(StrPar);    break;
                                            case 'R':	nRec=Integer.parseInt(StrPar);	break;
                                            case 'B':	
                                                    crChar++;
                                                    switch (StrRead.charAt(crChar))
                                                    {
                                                            case 'H':	nBlockHead=Integer.parseInt(StrPar);	break;
                                                            case 'A':	nBlockAnal=Integer.parseInt(StrPar);	break;
                                                            case 'D':	nBlockData=Integer.parseInt(StrPar);	break;
                                                    }
                                                    break;
                                    }
                            }
                            if ((StrRead.charAt(0)=='A')&&(StrRead.charAt(3)=='M'))
				ADCMax=Float.parseFloat(StrPar);
                            //System.out.println(StrRead);
                            //JOptionPane.showInputDialog(StrRead);
                            if (StrRead.charAt(0)=='Y'){
                                StrNum=StrRead.substring(2,3);
                                ChNum=Integer.parseInt(StrNum);                                    
                                if (StrRead.charAt(1)=='G'){
                                    if (ChanGainRead[ChNum]==0){
                                            ChanGain[ChNum]=Float.parseFloat(StrPar); ChanGainRead[ChNum]++;
                                    }
                                }
                                if (StrRead.charAt(1)=='O'){
                                        if (ChanOffsetRead[ChNum]==0){
                                                ChanOffset[ChNum]=Integer.parseInt(StrPar); ChanOffsetRead[ChNum]++;
                                        }
                                }
                            }
                            ReadGood=nChan*nBlockHead*nBlockAnal*nBlockData*ChanGainRead[0];
                            ln++;
                        }
                        else {
                            ln++;
                        }
                }
                headBuf.clear();
                //reading data in binary format
                if (ReadGood>0){
                    nByteRead=256*nBlockData;
                    nData=nByteRead/nChan;
                    //dataBuf= ByteBuffer.allocate(nBlockAnal*512+2*nByteRead);
                    //dataBuf.order(ByteOrder.nativeOrder());
                    byte[] rbuf=new byte[nBlockAnal*512+2*nByteRead];
                    
                    for (ChNum=0;ChNum<nChan;ChNum++) {
                        //reading channels into separate data series
                        if (nChan>1){	
                            if (JOptionPane.showConfirmDialog(parentFrame, "Channel # "+ChNum, "Read Channel ?", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                             DataByte=1;
                            else DataByte=0;
                        }
                        else
                                DataByte=1;
                        if (DataByte>0){
                            btChannel.position(nBlockHead);
                            Time0=0; 
                            dT0=-1;
                            GrNum0=1;
                            for (j=0;j<nRec;j++){
                                dataBuf=ByteBuffer.wrap(rbuf);
                                dataBuf.order(ByteOrder.nativeOrder());
                                dataBuf.mark();
                                headBytes=btChannel.read(dataBuf);
                                //btChannel.
                               
                                if(headBytes>0){
                                    iOff=12;
                                    GrNum=dataBuf.getFloat(iOff); iOff+=4;
                                    Time1=dataBuf.getFloat(iOff); iOff+=4;
                                    dT1=dataBuf.getFloat(iOff); iOff+=4;
                                    for (i=0;i<8;i++){
                                        MaxAD[i]=dataBuf.getFloat(iOff);
                                        iOff+=4;
                                    }
                                    if (dT1!=dT0){
                                      crSweep=addSeries(nData,"Ch"+ChNum);
                                      dT0=dT1;
                                    }
                                    else
                                        crSweep=dataSeries[nSeries-1].createComp(nData);
                                    if (j==0) Time0=Time1;
                                    nSweep=dataSeries[nSeries-1].getCompSize();
                                    if (nSweep<=2){
                                        DataX=dataSeries[nSeries-1].getXBase();
                                        for (i=0;i<nData;i++)
                                            DataX[i]=(float)(dT1*1000.0*i);
                                    }
                                    //reading data into the last sweep of last data series
                                    if (crSweep!=null){
                                        nDataClip=0;
                                        DataY=crSweep.getData();
                                        for (i=0;i<nData;i++){
                                            iOff=nBlockAnal*512+2*i*nChan+ChanOffset[ChNum];
                                            DataByte=dataBuf.getShort(iOff);
                                            if ((DataByte>=32766)||(DataByte<=-32767)) nDataClip++;
                                            DataY[i]=(float)(MaxAD[ChNum]*DataByte/(ChanGain[ChNum]*ADCMax));
                                        }
                                        crSweep.recTime=Time1-Time0;
                                        crSweep.updateScale();
                                        if (nDataClip>nData/5) //unfinished or bad recording sweep
                                            dataSeries[nSeries-1].removeComp(nSweep-1);
                                    }
                                    
                                }
                            }
                           
                        }
                    } //end of reading channels
                    btChannel.close();
                    return true;
                }
                else {
                    btChannel.close();
                    return false;
                }
               
            }
            catch (IOException x){
                System.err.format("IOException: %s%n", x);
            }
            
        return true;
        }
        else 
            return false;
    }
    public void setTitle(String newTitle) {
        xAxisTitle = newTitle+"XXX";
        DataSetTitle = newTitle;
        
        
    }
    public void updateSweeps(){
        int n,j,NS;
        clearAllSweeps();
        for (n=0;n<nSeries;n++){
            if (dataSeries[n].IsSelected){
                NS=dataSeries[n].getCompSize(); // N sweeps in series
                for (j=0;j<NS;j++)
                    if (dataSeries[n].isIdxSelected(j))
                            addSweep(dataSeries[n].getCompAtIdx(j));            
            }
        }
    }
    public void resetScaleX(boolean ignoreSel, boolean userSet){
        int n,NS;
        float[] currX;
        double begTick, endTick, Mantiss, Mult10;
        double min = 3e50;
        double max = -3e50;
        if (nSeries>0){
            for (n=0;n<nSeries;n++){
                if ((dataSeries[n].IsSelected)||ignoreSel){
                    NS=dataSeries[n].getDataSize();
                    currX=dataSeries[n].getXBase();
                    if (min>currX[0]) min=currX[0];
                    if (max<currX[NS-1]) max=currX[NS-1];
                }
            }
            xAxisBeg=min;
            xAxisScale=max-min;
            xAxisStep=xAxisScale/xAxisTick;
            //resetting the Y-Axis postion
            yAxisPos=xAxisBeg;
            //setting rounded tick marks & scale
            if (Math.abs(xAxisStep)>0){
                Mult10=Math.pow(10.0, Math.floor(Math.log10(Math.abs(xAxisStep))));
                Mantiss=Math.floor(xAxisStep/Mult10);
                xAxisStep=Mult10*Mantiss;
                //System.out.print("mult10= "+Mult10);
                //System.out.println("    mants= "+Mantiss);
                endTick=xAxisStep*(Math.floor((xAxisBeg+xAxisScale)/xAxisStep)+0.55);
                begTick=xAxisStep*Math.floor(xAxisBeg/xAxisStep);
                if (!userSet) {
                    xAxisBeg=begTick;
                    xAxisScale=endTick-begTick;
                }
            }    
            
        }
        
    }
    public void roundScaleX(){
        double begTick, endTick, Mantiss, Mult10;
        //setting rounded tick marks & scale
        xAxisStep=xAxisScale/xAxisTick;
        if (Math.abs(xAxisStep)>0){
            Mult10=Math.pow(10.0, Math.floor(Math.log10(Math.abs(xAxisStep))));
            Mantiss=Math.floor(xAxisStep/Mult10);
            xAxisStep=Mult10*Mantiss;
            //System.out.print("mult10= "+Mult10);
            //System.out.println("    mants= "+Mantiss);
            endTick=xAxisStep*(Math.floor((xAxisBeg+xAxisScale)/xAxisStep)+0.55);
            begTick=xAxisStep*Math.floor(xAxisBeg/xAxisStep);
            xAxisBeg=begTick;
            xAxisScale=endTick-begTick;
        }   
    }
    public void resetScaleY(boolean ignoreSel,boolean userSet){
        int n,j,NS;
        float zz,am;
        double begTick, endTick, Mantiss, Mult10;
        double min = 3e50;
        double max = -3e50;
        if (nSeries>0){
            for (n=0;n<nSeries;n++){
                if (dataSeries[n].IsSelected){
                    NS=dataSeries[n].getCompSize(); // N sweeps in series
                    for (j=0;j<NS;j++)
                        if ((dataSeries[n].isIdxSelected(j))||ignoreSel){
                            zz=dataSeries[n].getCompAtIdx(j).Zero;
                            am=dataSeries[n].getCompAtIdx(j).Amp;
                            if (min>zz) min=zz;
                            if (max<zz+am) max=zz+am;
                        }
                }
            }
            yAxisBeg=min;
            yAxisScale=max-min;
            //resetting the X-Axis postion
            xAxisPos=yAxisBeg;
            yAxisStep=yAxisScale/yAxisTick;
            //setting rounded tick marks & scale
            if (Math.abs(yAxisStep)>0){
                Mult10=Math.pow(10.0, Math.floor(Math.log10(Math.abs(yAxisStep))));
                Mantiss=Math.floor(yAxisStep/Mult10);
                yAxisStep=Mult10*Mantiss;
                endTick=yAxisStep*(Math.floor((yAxisBeg+yAxisScale)/yAxisStep)+0.55);
                begTick=yAxisStep*Math.floor(yAxisBeg/yAxisStep);
                if (!userSet) {
                    yAxisBeg=begTick;
                    yAxisScale=endTick-begTick;
                }
            }    
        
        }
    }
    public void roundScaleY(){
        double begTick, endTick, Mantiss, Mult10;
        //setting rounded tick marks & scale
        yAxisStep=yAxisScale/yAxisTick;
        if (Math.abs(yAxisStep)>0){
            Mult10=Math.pow(10.0, Math.floor(Math.log10(Math.abs(yAxisStep))));
            Mantiss=Math.floor(yAxisStep/Mult10);
            yAxisStep=Mult10*Mantiss;
            endTick=yAxisStep*(Math.floor((yAxisBeg+yAxisScale)/yAxisStep)+0.55);
            begTick=yAxisStep*Math.floor(yAxisBeg/yAxisStep);
            yAxisBeg=begTick;
            yAxisScale=endTick-begTick;
        }
    }
     //updating Axis scaling
    public void updateScaleX(int action){
        double center, scale;
        center = xAxisBeg+0.5*xAxisScale;
        scale = xAxisScale;
        autoScaleX = false;
        switch (action) {
            case -2:        //shift frame left
                xAxisBeg -=0.2*scale;
                roundScaleX();
                break;
            case -1:        //zoom out 2-fold
                xAxisScale = scale*2.0;
                xAxisBeg = center-xAxisScale/2.0;
                roundScaleX();
                break;
            case 0:         //reset to full scale 
                resetScaleX(false,false);
                autoScaleX = true;
                break;
            case 1:         //zoom in 2-fold
                xAxisScale = scale/2.0;
                xAxisBeg = center-xAxisScale/2.0;
                roundScaleX();
                break;
            case 2:         //shift frame right
                xAxisBeg +=0.2*scale;
                roundScaleX();
                break;
                
        }
        
    }
    public void updateScaleY(int action){
        double center, scale;
        center = yAxisBeg+0.5*yAxisScale;
        scale = yAxisScale;
        autoScaleY = false;
        switch (action) {
            case -2:        //shift frame down
                yAxisBeg -=0.2*scale;
                roundScaleY();
                break;
            case -1:        //zoom out 2-fold
                yAxisScale = scale*2.0;
                yAxisBeg = center-yAxisScale/2.0;
                roundScaleY();
                break;
            case 0:         //reset to full scale
                resetScaleY(false,false);
                autoScaleX = true;
                break;
            case 1:         //zoom in 2-fold
                yAxisScale = scale/2.0;
                yAxisBeg = center-yAxisScale/2.0;
                roundScaleY();
                break;
            case 2:         //shift frame up
                yAxisBeg +=0.2*scale;
                roundScaleY();
                break;
            
        }
        
    }
    @Override
    public void update(Graphics g){
        
        paint(g);
        
    }
    @Override
    public void paint(Graphics g) {
        int i,x1,x2,y1,y2,clr,nMark;
        int xPlot[] = new int[SpecSeries.NMarkMax];
        int yPlot[] = new int[SpecSeries.NMarkMax];
        int dotSize = (int)(0.002*plotRect.width+0.003*plotRect.height);
        if (dotSize<4) dotSize = 4;
        g.getClipBounds(drawRect);
        g.setColor(Color.WHITE);
        g.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height);
        plotRect.height=(int)(drawRect.height*0.9);
        plotRect.width=(int)(drawRect.width*0.85);
        plotRect.x=(int)(drawRect.x+0.12*drawRect.width);
        plotRect.y=(int)(drawRect.y+0.03*drawRect.height);
        if (nPlot>0){
            
            setBackground(Color.WHITE);
            g.setColor(Color.WHITE);
            g.fillRect(drawRect.x, drawRect.y, drawRect.width, drawRect.height); 
            //drawing axis
            drawAxes(g);
            
            //calculating the scales and drawing the data series
            for (i=1;i<nPlot;i++){
                clr=i%nPen;
                g.setColor(penColor[clr]);
                drawSweep(g,i);
            }
            if (nPlot==1){
                g.setColor(penColor[11]);
            }
            else
                g.setColor(penColor[0]);
            drawSweep(g,0);
            //drawing secondary markers
            axesParam[0]=xAxisBeg; axesParam[1]=xAxisScale; axesParam[2]=yAxisBeg; axesParam[3]=yAxisScale;
            nMark=getSelectedSeries().getMarks2Plot(xPlot, yPlot, axesParam, plotRect);
            if (nMark>0){
                //System.out.println("nMark "+nMark);
                g.setColor(Color.BLUE);
                g.drawPolyline(xPlot, yPlot, nMark);
                for (i=0;i<nMark;i++){
                    //g.drawOval(xPlot[i], yPlot[i],dotSize );
                    //g.setColor(Color.DARK_GRAY);
                    g.fillOval(xPlot[i]-dotSize/2, yPlot[i]-dotSize/2, dotSize, dotSize);
                }
                
            }
            
            
        }           
    }
}
