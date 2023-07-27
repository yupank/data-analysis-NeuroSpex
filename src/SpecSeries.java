/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

import java.awt.Rectangle;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.ArrayList;
import javax.swing.table.DefaultTableModel;
import java.util.regex.Pattern;


/**
 *
 * @author lssgav
 */
//this class handles a set of "components" (i.e. sweeps/model curves) with similar array of X-arguments (XBase)
//and handles the front-end data analysis and submit results to the specific SpecPanel
//names for the fields and some methods come from the legacy C++ classes "Spectrum" & "Component"
public class SpecSeries {
    private float[] SpecBase;
    private float[] xMark;  //secondary marker used for data processing
    private float[] yMark;
    private int NMark;
    private int NSPoint;
    private int NSComp;
    private int begSel;
    private int endSel;
    private SpecSweep[] SpecComp;
    private SpecSweep   BcgrComp;
    private String Title;
    public static int NCompMax = 2500; 
    static int NDataPointMax = 40000;
    static int NSelPage = 8;
    public static int NMarkMax = 64;
    //private int[]   CompSelIdx;         // indexes of sweep/component selected for show
    private int     NSelComp;
    public int      TagComp; //target component for fitting, default = 1, needed for interaction with Param Tables
    public int      BegFitComp; // number of first model component to find, needed for interaction with Param Tables
    public boolean  IsSelected;
    
    //parameters to contorl the data fit workflow by user
    public float fitAccuracy;     //accuracy by mean-squre-error, should not go over the limit set by user
    public boolean fitAtBorder;   //one of fitting parameters reached the set limit
    //parameters for flow control of fit mulitiple data sweeps
    public boolean fitThroughStarted;
    public boolean fitThroughPaused;
    //paramters for data fitting, prefix 'fit' replaces old  C++ version's 'crFitState.'
    public static float fitAccurL = (float)0.0001; //low limit for changes in accuracy
    public static float fitAccurH = (float)0.25; // maximum tolerated for 'good fit', can be changed by user
    public static float fitAccurHRej = (float)0.30; // limit for automatic rejection, can be changed by user
    public static float fitOldAccur = 0;
    public static float fitNewAccur = 0;
    public static float fitOldCycleAccur = 0;
    public static float fitNewCycleAccur = 2;
    public static boolean fitStepRel = false;
    public static boolean fitStepProgress = false;
    public static boolean fitParamInternal = true; //use parameters set by user for model sweeps
    
    public static int fitNStepMax = 77;
    public static int fitNCycleMax = 25;
    public static int fitStepPerCycle =7;
    public static int fitCrStep = 0;
    public static int fitCrCycle = 0;
    public static int fitTotStep  = 0;
    //default parameters for automatic signal detection
    public static boolean findSigInw = true;
    public static boolean findSigGauss = false;
    public static float findSigYlow = (float) 3.75;
    public static float findSigXlow = (float) 1.2;
    public static float findSigXhigh = 25;
    
	
    //initial internal parameters, check if they are really used !!!
    private  float fitStepPar[];
    
    
    public SpecSeries(){
        
        SpecComp = new SpecSweep[NCompMax];
        //CompSelIdx = new int [NCompMax];
        BcgrComp = null;
        NSComp = 0;
        NSelComp = 0;
        NSPoint = 0;  // will be set when the first component is created
        BegFitComp=2;
        TagComp=0;
        xMark=new float[NMarkMax];
        yMark=new float[NMarkMax];
        NMark=0;
        fitStepPar = new float[]{(float)0.18,(float)0.14,(float)0.4,(float)0.2,0,0,0,0}; //check if used
        fitAtBorder = false;
        fitThroughStarted = false;
        fitThroughPaused = false;

    }
    //adding X-markers for data analysis
    public void AddMark(float mX, float mY){
        int j,k;
        float mXX=mX;
        if(NSComp>0 && NMark<NMarkMax-1){
            if (mXX>SpecBase[NSPoint-1]) mXX=SpecBase[NSPoint-1];
            if (mXX<SpecBase[0]) mXX=SpecBase[0];
            if (NMark==0){
                if (mXX>SpecBase[0]){
                    xMark[0]=SpecBase[0];   yMark[0]=mY;
                    xMark[1]=mXX;    yMark[1]=mY;
                    NMark=2;
                }
                else{
                    xMark[0]=SpecBase[0];    yMark[0]=mY;
                    NMark=1;
                }
            }
            else{
                //finding place to insert - markers should be in asceding order
                k=-1;   j=0;
                while (k<0 && j<NMark){
                    if(xMark[j]>mXX)
                        k=j;
                    j++;
                }
                if(k<0){ // to the end of list
                    xMark[NMark]=mXX;
                    yMark[NMark]=mY;
                    NMark++;
                }
                else{
                    //inserting mark
                        NMark++;
                        for (j=NMark-1;j>k;j--){
                            xMark[j]=xMark[j-1];
                            yMark[j]=yMark[j-1];
                        }
                        xMark[k]=mXX;
                        yMark[k]=mY;
                    
                }
              
            }
            
        }
    }
    
    public void AlignSweeps()
    {
        int idx;
        float shift;
        for (idx=0;idx<NSComp;idx++)
            if (SpecComp[idx].Select>0){
                shift=SpecComp[idx].Scan(begSel, endSel);
                SpecComp[idx].addNum(-shift);
                SpecComp[idx].Zero-=shift;
            }
    }
    public void Average(){
        SpecComp[0].multNum(0);
        int i;
        double count=0;
        for (i=1;i<NSComp;i++){
            if(SpecComp[i].Select>0){
                count+=1.0;
                SpecComp[0].Add(SpecComp[i]);
            }
        }
        if (count > 0) SpecComp[0].multNum(1.0/count);
        SpecComp[0].Zero=SpecComp[0].minVal();
        SpecComp[0].Amp=SpecComp[0].maxVal()-SpecComp[0].Zero;
        deSelectAll();
        selCompAtIdx(0,true);
    }
    public float AllSum(boolean ignoreSel, boolean legacyMode){ //legacy mode is used in fitting the sweep 1 with theoretical components
        float Sigma=0;
        int j;
	if ( NSComp>1){
            if (legacyMode)
                SpecComp[0].multNum(0);
            SpecComp[0].Select=1;
            
            for (j=1;j<NSComp;j++)
                if ( SpecComp[j].Select!=0||ignoreSel)
                   SpecComp[0].Add(SpecComp[j]);
	
            Sigma=SpecComp[0].GetBaseLevel(0)-SpecComp[1].GetBaseLevel(0);

            if (legacyMode){
                SpecComp[0].addNum(-Sigma);
                Sigma=SpecComp[1].normMSR(SpecComp[0]);
            }
            SpecComp[0].Zero=SpecComp[0].minVal();
            SpecComp[0].Amp=SpecComp[0].maxVal()-SpecComp[0].Zero;
        }	
        return Sigma;
    } 
    public boolean backgrSubtract(int sweepIdx){
        float crY[];
        float crX;
        int iM,jX;
        if (NMark>0){
            if (BcgrComp == null)
                BcgrComp = new SpecSweep(SpecBase, NSPoint);
            crY = BcgrComp.getData();
            jX=0;
            crX=SpecBase[0];
            for (iM=0;iM<NMark-1;iM++){
                while ((crX<=xMark[iM+1])&&jX<NSPoint){
                    crY[jX]=yMark[iM]+(crX-xMark[iM])*(yMark[iM+1]-yMark[iM])/(xMark[iM+1]-xMark[iM]);
                    jX++;
                    if (jX<NSPoint) crX=SpecBase[jX];
                }
            }
            if (xMark[NMark-1]<SpecBase[NSPoint-1]){
                while (jX<NSPoint){
                   crY[jX]=yMark[NMark-1];
                   jX++;
                }
                
            }
            if (sweepIdx>=0 && sweepIdx<NSComp ){
                //System.out.println("idx= "+sweepIdx);
                SpecComp[sweepIdx].Subtrack(BcgrComp);
                SpecComp[sweepIdx].Zero=SpecComp[sweepIdx].minVal();
                SpecComp[sweepIdx].Amp=SpecComp[sweepIdx].maxVal()-SpecComp[sweepIdx].Zero;
            }
            return true;
        }
        else
            return false;
    }
    public int calculateAC (SpecSweep acSweep, float[] res){ //data size of outSweep should be > NSPoint, period should have sizeof 3: 0 - AC score, 1 - period, 2 - rel.variation  of period
        
        int i,j,k,kMax,Count,idx;
	float S1,S2,Delta,Per,Per2,acMax,acScore;
	float[] AcExtY = new float[9];
	float[] AcExtX = new float[9];
	//TComponent* ACcomp;
	//creating the autocorrelation fraction
	//subtracting the backgound
	idx=findSelComp(-1);
	if(idx==0) idx=1;
        SpecComp[0].multNum(0);
        SpecComp[0].Add(SpecComp[idx]);
        SpecComp[0].SmoothCut(0,NSPoint,(float)0.01);
        SpecComp[0].Subtrack(SpecComp[idx]);
        SpecComp[0].multNum(-1);
        /*
        SpecComp[0].Add(SpecComp[idx]);
        SpecComp[0].multNum(0.5);
        */
        SpecComp[0].Zero = SpecComp[idx].Zero;
        SpecComp[0].Amp = SpecComp[idx].Amp;
        float[] crData = SpecComp[0].getData();
        
        float[] acBase = acSweep.getBase();
        float[] acData = acSweep.getData();
        if ( acSweep.getSize() >= NSPoint){
            //SpecComp[idx].SmoothCut(0,NSPoint,(float)0.1);
            //crData=(SpecComp)->GetData();
            Delta= (SpecBase[NSPoint-1]-SpecBase[0])/NSPoint;
            for (j=0;j<NSPoint;j++) {
                S1=0; S2=0;
                for (i=0;i<NSPoint-j;i++){
                    k=i+j;
                    if (k>NSPoint-1)k=NSPoint-1;
                    S1 += crData[i]*crData[k];//(*(crData+i))*(*(crData+k));
                    //S2 += crData[i]*crData[i];//(*(crData+i))*(*(crData+i));
                }
                //if (S2 < Float.MIN_VALUE) S2=Float.MIN_VALUE;
                acBase[j]=j*Delta;
                //acData[j]=S1/S2;
                //*(acData+j)=S1/S2;
                acData[j]=S1*NSPoint*NSPoint;
            }
            acSweep.Zero = acSweep.minVal();
            acSweep.Amp = acSweep.maxVal() - acSweep.Zero;
            //acSweep.addNum(-acSweep.Zero);
            //acSweep.multNum(1.0/acSweep.Amp);
            acMax= acData[0];
            //calculating the AC-score
            if (res.length > 2) {
                //ACcomp=new TComponent(acData,acBase,NSPoint-1);
                for (k=0;k<9;k++)	AcExtY[k]=0;
                
                kMax=acSweep.ExtremSearch(5*Delta,(float)0.02*acMax,AcExtX,AcExtY,9);
                acScore=Math.abs(AcExtY[2]-AcExtY[1]);
                Per=0; Count=0;   Per2=0;
                for (k=2;k<kMax;k+=2) {
                    Per+=2*AcExtX[k]/k;
                    Per2+=(2*AcExtX[k]/k)*(2*AcExtX[k]/k);
                    Count++;
                }
                Per/=Count;
                Per2/=Count;
                Per2= (float) Math.sqrt(Math.abs(Per2-Per*Per));
                Per2/=Per;
                acScore=0;
                for (k=2;k<kMax;k+=2) {
                    acScore += acSweep.getYdata(k*Per/2);//ACcomp->OutSpline(k*Per/2);
                    acScore -= acSweep.getYdata((k-1)*Per/2);//ACcomp->OutSpline((k-1)*Per/2);
                }
                acScore *= (1-Per2);
                res[0] = acScore; res[1] = Per; res[2] = Per2;
               
                //return acScore;
            }
            return acData.length;
        }
	else
            return 0;
    }
    public void clearAllComp(boolean ignoreSel){
        for (int j=NSComp-1;j>=0;j--)
            if (SpecComp[j].Select >0 || ignoreSel)
                removeComp(j);
    }
    public void clearAllComp(int lastNum){
	while (NSComp>lastNum)
		removeComp(NSComp-1);
    }
    public void clearAllMark(){
        NMark=0;
    }
    public void clearMarkAt(float mX){
        int j,k;
        k=0;
        //checking distance
                    if (Math.abs(mX-xMark[k]) < 0.1*(SpecBase[NSPoint-1]-SpecBase[0]) ){
                        //removing K-th mark
                        
                        for (j=k;j<NMark-1;j++){
                            xMark[j]=xMark[j+1];
                            yMark[j]=yMark[j+1];
                        }
                        NMark--;
                    }
    }
    public void clipAtIdx(int idx, boolean ignoreSel){
        if (idx>=0 && idx<NSComp)
            if (SpecComp[idx].Select!=0 || ignoreSel)
                SpecComp[idx].Clip(begSel, endSel, 15);
    }
    public String data2text(boolean allPoints, String delimiter){ // copy the data as ASCII columns for the clipboard 
        final int maxCopy = 64;
        float[] crY;
        int i,j,iBeg,iEnd,nCopy;
        if (allPoints){
            iBeg=0; iEnd=NSPoint;
        }
        else{
            iBeg=begSel;    iEnd=endSel;
        }
        String outStr="";
        //title line
        outStr+="time";
        nCopy=0;
        for (j=0;j<NSComp;j++){
            if (SpecComp[j].Select>0){
                nCopy++;
                 outStr+= delimiter;
                 outStr+= "YY"+String.valueOf(j+1);
            }
                
        }
        outStr+="\n";
        float[][] outData = new float[iEnd-iBeg][nCopy+1];
        for(i=iBeg; i<iEnd; i++)
            outData[i-iBeg][0] = SpecBase[i];
        nCopy=0;
        for (j=0;j<NSComp;j++)
            if (SpecComp[j].Select>0){
                crY = SpecComp[j].getData();
                nCopy++;
                for(i=iBeg; i<iEnd; i++)
                    outData[i-iBeg][nCopy] = crY[i];
            }
        for(i=iBeg; i<iEnd; i++){
            for(j=0; j<nCopy; j++){
                outStr+=String.format("%f%s",outData[i-iBeg][j],delimiter);
            }
            outStr+=String.format("%f%s",outData[i-iBeg][nCopy],"\n");
        }
        return outStr;
    }
    
    public int findSelComp(int LastNum){
	int j,Count,jSel,jStop;
	Count=0;
	jSel=1;
	if(LastNum>0) {
            if(LastNum<=NSComp)
                jStop=LastNum;
            else jStop=NSComp;
	}
	else
		jStop=NSComp;
	for (j=0;j<jStop;j++) {
            if (SpecComp[j].Select>0) {
                jSel=j;
		Count++;
            }
	}
	if (jSel==0 && Count>1 )jSel=1;
        
	return jSel;
    }
    // methods finds spurious signals ("peaks") for tagged data sweep ("component") according current detection settings (global parameters) spec
    // returns accuracy of approximation of data with set of peaks and copies X-location of peaks into provided array locX
    public float FindPeaks(int tagComp, float[] locX){
        int Count,j,i,jSel, newMode;
        float[] peakLoc;
        float Sigma = -1;
        float[] Filter  = {findSigXhigh,findSigXlow, findSigYlow, 0}; //{0.001,25,1.2,4.9,3,1,0};
        if (locX != null)
            peakLoc = locX;
        else
            peakLoc = new float[100];
        int oldMode = NeuroSpex.currFitPar.getMode();
        float oldPar[] = new float[FitParam.nFitPar];
        float newPar[] = new float[FitParam.nFitPar];
        NeuroSpex.currFitPar.getPar(oldPar);
        if (tagComp < 0){
            jSel = findSelComp(BegFitComp);
            if (jSel ==0 ) jSel = 1;
            if (BegFitComp < NSComp)
                clearAllComp(BegFitComp);
        }
        else
            jSel = tagComp;
        //TagComp = jSel;
        //System.out.println("FiC TgC "+TagComp);
        if (BcgrComp == null) {
            BcgrComp = new SpecSweep(SpecBase, NSPoint);          
        }
        Count=SpecComp[jSel].ExtremSearch(Filter,peakLoc,BcgrComp,!findSigInw,begSel,endSel);
        BcgrComp.InitData(1); BcgrComp.Zero = 0; BcgrComp.Amp = 1;
        if (Count>0){
            NeuroSpex.currFitPar.extendLimAtIdx(0, SpecBase[0], SpecBase[NSPoint-1]);
            for (i=0;i<Count;i++) {
                if (findSigGauss){
                    //spectroscopy data
                    j=i+1;
                    if (j==Count) j=Count-2;
                    newMode = FitParam.GAUSLOR;
                    newPar[0] = peakLoc[i]; newPar[1] = (peakLoc[j]-peakLoc[i])/2;  newPar[2] = 0; newPar[2] = (float)0.5;
                }
                else {
                    //neurosci data
                    newPar[0] = peakLoc[i]; newPar[1] = 1;  newPar[2] = 7;
                    if(findSigInw) newMode = FitParam.RIDEC_I;
                        else newMode = FitParam.RIDEC_O;
                }
                NeuroSpex.currFitPar.setMode(newMode);
                NeuroSpex.currFitPar.setPar(newPar);
                if(createFitComp(NeuroSpex.currFitPar))
                    SpecComp[NSComp-1].Reset();
            }
            NeuroSpex.currFitPar.setMode(oldMode);
            NeuroSpex.currFitPar.setPar(oldPar);
            if (Filter[3] > 0){
                Sigma=LeastSqr(0);
                for (j=NSComp-1;j>=BegFitComp;j--)
                    if (SpecComp[j].Amp < 0.3*Filter[3]*Filter[2])
                        removeComp(j);
            }
            //selComp(0,1); selComp(jSel,0);
                
        }
        return Sigma;
    }
    
    // methods finds spurious signals ("peaks") for target sweep (tagComp) by calling FindPeaks method and
    // appends the destination data series by the "cut-out" of the peak and some surrounding data samples (total span about 4 width of peak)
    // returns number of peak detected and appended
    // ! method re-uses locX array for each sweep (to decrease memory usage), so array should be allocated prior to calling this method
    public int GatherPeaks(int tagComp, SpecSeries destSeries, float[] locX){
        SpecSweep crPeakSweep;
        int j,jBeg,jEnd;
        float xPos, xShift;
        int Count = 0;
        float[] peakPar = new float[FitParam.nFitPar];
        int destN = destSeries.NSPoint;
        xShift = destSeries.SpecBase[0]*3/4 + destSeries.SpecBase[destN-1]/4;
        setTagComp(tagComp);
        FindPeaks(-1, locX);
        for (j=BegFitComp; j< NSComp; j++){
            FitParam sweepParam = SpecComp[j].getFitParam();
            sweepParam.getPar(peakPar);
            crPeakSweep = destSeries.createComp(destN);
            xPos = peakPar[0];
            if (xPos <= xShift){
                jBeg = 0;
            }
            else {
                jBeg = SpecComp[j].getNearestIdx(xPos-xShift);
                if (jBeg+destN > NSPoint){

                    jBeg = NSPoint - destN;
                    peakPar[0] = xShift*4 - (SpecBase[NSPoint-1]-xPos);
                    //fit param - Beg = xPos - xBase[jBeg]
                }
                else {
                    peakPar[0] = xShift;
                }
            }   
            //System.out.println("x pos: "+ peakPar[0]);
            sweepParam.setPar(peakPar);
            sweepParam.getLimH(peakPar);
            peakPar[0] = xShift*4;
            sweepParam.setLimH(peakPar);
            crPeakSweep.pasteFitParam(sweepParam);
            crPeakSweep.copyDataAtIdx(SpecComp[tagComp], jBeg);
            crPeakSweep.recTime = SpecComp[tagComp].recTime + xPos/1000;
        }
      
        Count = NSComp - BegFitComp;
        //System.out.println(destSeries.NSComp);
        // cut strech of data, surroding the found peak and populate the destination series
        return Count;
    }
    
    // method performs a single cycle (several steps) of accelerated gradient optimization routine 
    //which stops when accuracy improves lower than set limit or number of cycles reaches maximal limit
    //method requires prior intialization using the static fit* parameters
    public boolean FitStep(){
        boolean fitStop = true;
        boolean fixGrad[] = new boolean[FitParam.nFitPar]; // for some types of curves, altering some parameters is not needed
        float GradPar[][] = new float[FitParam.nFitPar][64];
        float stepVal[] = new float[FitParam.nFitPar];
        double GradModulus[] = new double [FitParam.nFitPar];
        int j,i,par_idx,Mode;
        int TestMode=FitParam.TEST_LLH+1;
        FitParam crParam;
        if (fitCrStep == 0){
            if  ((fitCrCycle < fitNCycleMax)&&(Math.abs(fitNewCycleAccur-fitOldCycleAccur)>fitAccurL)){
                //initialize new cycle
                for (i=0;i<FitParam.nFitPar;i++){
                    GradModulus[i]=0.000001;
                    fixGrad[i] = false;
                }
                //caclulating accuracy gradient by parameter values
                for (j=BegFitComp;j<NSComp;j++){
                    crParam = SpecComp[j].getFitParam();
                    //crParam.setStep(fitStepPar);
                    crParam.getStep(stepVal,fitStepRel);
                    Mode = crParam.getMode();
                    fixGrad[FitParam.Mix] = !(Mode>=FitParam.STEPRESP)&&(Mode<=FitParam.SINUS);
                    fixGrad[FitParam.Split] = (Mode == FitParam.GAUSLOR) && (TestMode == FitParam.TEST_LLH);
                    for (par_idx = FitParam.Max; par_idx <= FitParam.Mix; par_idx++){
                        // gradient by value of 'max at, 'line width', 'split' and 'mix'
                        //par_idx = FitParam.Width;
                        if (!fixGrad[par_idx]){
                            crParam.makeStepAtIdx(par_idx, -1, fitStepRel);
                            if (TestMode!=FitParam.TEST_LLH)
                                SpecComp[j].Reset();
                            fitOldAccur = LeastSqr(TestMode);
                            crParam.makeStepAtIdx(par_idx, 2, fitStepRel);
                            if (TestMode!=FitParam.TEST_LLH)
                                SpecComp[j].Reset();
                            fitNewAccur = LeastSqr(TestMode);
                            GradPar[par_idx][j-BegFitComp] = (float)0.5*(fitOldAccur-fitNewAccur)/stepVal[par_idx];
                            crParam.makeStepAtIdx(par_idx, -1, fitStepRel);
                            if (TestMode!=FitParam.TEST_LLH)
                                SpecComp[j].Reset();
                        }
                        else
                            GradPar[par_idx][j-BegFitComp]=0;
                        //modulus of gradient, needed for further normalisation
                        GradModulus[par_idx] += GradPar[par_idx][j-BegFitComp]*GradPar[par_idx][j-BegFitComp];
                    }
                }
                //re-norming the gradients
                for (par_idx = FitParam.Max; par_idx <= FitParam.Mix; par_idx++){
                    if (GradModulus[par_idx] < FitParam.MinFloat) GradModulus[par_idx] = FitParam.MinFloat;
                    GradModulus[par_idx]=0.0000001+Math.sqrt(GradModulus[par_idx]);            
                    for (j=BegFitComp;j<NSComp;j++)
                        GradPar[par_idx][j-BegFitComp] *= stepVal[par_idx]/GradModulus[par_idx];
                }
                fitOldAccur = LeastSqr(TestMode);
            }
            else {
                //cycle stops
                fitNewAccur = LeastSqr(TestMode);
                return false;
            }
        }
        //continue steps with gradient calculated at 0th step
        for (j=BegFitComp;j<NSComp;j++){
            crParam = SpecComp[j].getFitParam();
            for (par_idx = FitParam.Max; par_idx <= FitParam.Mix; par_idx++)
                crParam.incrementAtIdx(par_idx, GradPar[par_idx][j-BegFitComp]);
            SpecComp[j].Reset(crParam);
        }
        fitNewAccur = LeastSqr(TestMode);
        fitCrStep++;
        fitTotStep++;
        if( fitCrStep > fitNStepMax || fitNewAccur >= fitOldAccur ){
            fitCrStep = 0;
            fitCrCycle++;
            //step refinement: 2-fold for each 5 cycles
            //calculating refined steps values for next cycle
            if (fitStepProgress)
                //for (i=0;i<FitParam.nTotPar;i++) fitStepPar[i]/=1.148;
                for (j=BegFitComp;j<NSComp;j++)
                    SpecComp[j].getFitParam().scaleStepsBy((float)1.148);
            fitOldCycleAccur = fitNewCycleAccur;
            fitNewCycleAccur = fitOldAccur;
            //half-step back for better accuracy
            for (j=BegFitComp;j<NSComp;j++){
                crParam = SpecComp[j].getFitParam();
                for (par_idx = FitParam.Max; par_idx <= FitParam.Mix; par_idx++)
                crParam.incrementAtIdx(par_idx, (float)(-0.5*GradPar[par_idx][j-BegFitComp]));
                SpecComp[j].Reset();
            }
        }
        fitAccuracy = fitOldAccur;
        fitOldAccur = fitNewAccur;
        return true;
        
    }
    // method fits the parameters of theoretical curves(components) using accelerated gradient optimization routine,
    // in each cycle, fitting steps (calling FitStep method) continue until reaching a local minimum of error function
    public void AutoFit(){
        int i,j;
        boolean FitStop = true;
	float[][] oldStepPar = new float[NSComp-BegFitComp][fitStepPar.length];
	//initialization
	fitNewCycleAccur=fitOldCycleAccur+2*fitAccurL;
	fitCrStep = 0;
	fitCrCycle = 0;
	fitTotStep = 0;
	fitStepProgress = true;
        //using the preset fit parameters of the tagComp - for the signals detected by GatherPeaks method
        FitParam tagFitParam = SpecComp[TagComp].getFitParam();
        if ( tagFitParam != null)
            SpecComp[BegFitComp].pasteFitParam(tagFitParam);

	// remember steps
        if (fitParamInternal)
            for(j=BegFitComp;j<NSComp;j++)
                SpecComp[j].getFitParam().getStep(oldStepPar[j-BegFitComp]);
        else
            for (i=0;i<fitStepPar.length;i++)
                oldStepPar[0][i] = fitStepPar[i];
	while (FitStop){
            FitStop=FitStep();     
        }
	// restore steps
        if (fitParamInternal)
            for(j=BegFitComp;j<NSComp;j++)
                SpecComp[j].getFitParam().setStep(oldStepPar[j-BegFitComp]);
        else
            for (i=0;i<fitStepPar.length;i++)
                fitStepPar[i] = oldStepPar[0][i];
    }
    /******************************************/
    public float LeastSqr(int testMode){
        int j,k,N,i;
	int BinomFlag, SinusFlag;
	float Sigma, Shift,tagZero,tagAmp ;
        N=NSComp-BegFitComp;
        double[] A = new double[(N+1)*(N+2)];
        double[] B = new double[N+1];
        double[] Y = new double[N+1];
        float[] res = new float[N*(FitParam.nFitPar+1)+1];

	if (testMode==FitParam.TEST_LLH) {
            j=TagComp;
            Sigma=(float)0.0001*SpecComp[j].LikelyHoodTest(SpecComp[BegFitComp]); //((SpecComp+j)->LikeHoodTest(SpecParam+BegFitComp));
            return Sigma;
        }
	else
	{
            if ( NSComp>BegFitComp) {
                //resetting  the sum and tagged components
                tagZero = SpecComp[TagComp].Zero; //saving previous parameters
                tagAmp = SpecComp[TagComp].Amp;
                SpecComp[TagComp].Rescale(0, 1);
                SpecComp[0].InitData(SpecComp[TagComp]);
                BinomFlag=0;
                SinusFlag=0;
                for (j=BegFitComp;j<NSComp;j++){
                    if (SpecComp[j].Mode >= FitParam.UNIMODAL && SpecComp[j].Mode < FitParam.SINUS) 
                        BinomFlag = 1;
                    if (SpecComp[j].Mode == FitParam.SINUS ) {
                        BinomFlag=0;
                        SinusFlag=j;
                    }
                }
                BcgrComp.InitData(1);
                if (SinusFlag >0) SpecComp[SinusFlag].InitData((float)0.000001);
                // re-norming  the target component, rolling scale down to normzlized one (0,1)
                SpecComp[TagComp].Amp/=SpecComp[TagComp].Kf;
                SpecComp[0].Amp/=SpecComp[0].Kf;
                
                for (j=BegFitComp;j<NSComp;j++){
                    //rolling scale down to normzlized one, saving old scale 
                    SpecComp[j].multNum(1/SpecComp[j].Kf);
                    SpecComp[j].Zero/=SpecComp[j].Kf;
                    SpecComp[j].Amp/=SpecComp[j].Kf;
                    SpecComp[j].Rescale(0,1);
                }
                N=NSComp-BegFitComp;
                for (j=0;j<N;j++){
                    B[j] = SpecComp[TagComp].scalarMult(SpecComp[BegFitComp+j]);
                    for (k=0;k<N;k++)
                        A[j*(N+1)+k] = SpecComp[k+BegFitComp].scalarMult(SpecComp[BegFitComp+j]);
                    A[j*(N+1)+N] = BcgrComp.scalarMult(SpecComp[BegFitComp+j]);
                }
                B[N] = SpecComp[TagComp].scalarMult(BcgrComp);
                for (k=0;k<N;k++)
                    A[N*(N+1)+k] = SpecComp[k+BegFitComp].scalarMult(BcgrComp);
                A[N*(N+1)+N] = BcgrComp.scalarMult(BcgrComp);
                //if (BinomFlag||ClampFlag){
                if(BinomFlag !=0){    
                    SpecSweep.EquSet(Y, A, B, N);
                    Y[N]=0;
                }
                else
                    SpecSweep.EquSet(Y,A,B,N+1);
                for (j=0;j<N;j++){
                    if (Y[j] <=0)  Y[j]=0.000001;
                    //SpecComp[BegFitComp+j].Amp *= Y[j];
                    SpecComp[BegFitComp+j].Kf = (float)Y[j];
                }
                selComp(0,1);
                selComp(TagComp,0);
                for (j=BegFitComp;j<NSComp;j++){
                    SpecComp[j].Kf = Float.max(SpecComp[j].Kf, (float)0.00000001);
                    SpecComp[j].multNum(SpecComp[j].Kf);
                    selComp(j,0); 
                }
                BcgrComp.multNum(Y[N]);
                for (j=BegFitComp;j<NSComp;j++)
                    SpecComp[0].Subtrack(SpecComp[j]);
                SpecComp[0].Subtrack(BcgrComp);
                if (SinusFlag !=0) {
                    j=SinusFlag;
                    SpecComp[j].Reset();
                    SpecComp[j].Amp = SpecComp[TagComp].Amp;
                    SpecComp[j].Zero = SpecComp[TagComp].Zero;
                    SpecComp[j].Kf = 1;
                    SpecComp[j].multNum(SpecComp[j].Amp);
                    SpecComp[j].addNum(SpecComp[j].Zero);
                    SpecComp[0].Subtrack(SpecComp[j]);
                }
                SpecComp[0].Subtrack(SpecComp[TagComp]);
                SpecComp[0].multNum(-1);
                Sigma = SpecComp[0].normMSR(SpecComp[TagComp]);
                //rolling scale up again
                SpecComp[TagComp].Rescale(tagZero,tagAmp); // restoring previous scale
                SpecComp[0].multNum(SpecComp[TagComp].Amp);
                SpecComp[0].addNum(SpecComp[TagComp].Zero);
                SpecComp[0].Zero = SpecComp[0].minVal();
                SpecComp[0].Amp = SpecComp[0].maxVal()-SpecComp[0].Zero;
                res[0]=SpecComp[0].Amp;
                for (j=BegFitComp;j<NSComp;j++){
                    SpecComp[j].multNum(SpecComp[TagComp].Amp);
                    SpecComp[j].addNum(SpecComp[TagComp].Zero);
                    SpecComp[j].Zero = SpecComp[TagComp].Zero;
                    SpecComp[j].Amp = SpecComp[TagComp].Amp * SpecComp[j].Kf;
                    i=j-BegFitComp;
                    SpecComp[j].getFitParam().getPar(res, 1+i*(FitParam.nFitPar+1), FitParam.nFitPar);
                    res[1+FitParam.nFitPar*(i+1)+i]=SpecComp[j].Amp;
                }
                SpecComp[TagComp].setFitResult(res);
                return Sigma;
            }
	else
            return -1;
	}
    }
    //method check if fit results are at preset limits of main parameters or amplitude of fitted curve is too small
    public boolean checkFitAtBorder(){
        int j,i;
        float[] scan = new float[3];
        SpecComp[TagComp].Scan(scan, 0, 30);
        float sdn = scan[1]*findSigYlow/4;

        boolean checkRes = false;
        for (j=BegFitComp;j<NSComp;j++){
            for(i=0;i<3;i++)
                checkRes = checkRes || SpecComp[j].getFitParam().parAtLimAtIdx(i);
            checkRes = checkRes || (SpecComp[j].Amp < sdn);
        }
        return checkRes;
    }
    public static String makeParsable(String dataStr){
        String outStr=new String();
        char outChar; 
        int digChar=0;
        int pChar=0;
        int eChar=0;
        int sChar=0;
        int count=0;
        boolean readGood = true;
        if (dataStr.length()>0){
            for (int i=0;i<dataStr.length();i++){
                outChar=dataStr.charAt(i);
                switch (outChar){
                    case '0':case '1':case '2':case '3':case '4':case '5': 
                    case '6':case '7':case '8':case '9':outStr+=outChar;digChar++; count++; readGood=true;break;
                    case '.':
                        if(pChar==0){
                            outStr+=outChar; count++;
                            pChar++;
                            readGood=true;
                        }
                        else
                            readGood=false;
                        break;
                    case '+':case '-':
                        if(sChar==0 && count ==0){ //string staring from + or - sign
                            outStr+=outChar; count++;
                            sChar++;
                            readGood=true;
                            
                        }
                        else
                            //if (outStr.charAt(count)=='E'||outStr.charAt(count)=='e'){
                            if (eChar==1){
                                outStr+=outChar; count++;
                                sChar++;
                                readGood=true;
                            }
                            else
                                readGood=false;
                        break;
                    case 'e':case 'E': 
                        if(eChar==0 && digChar>0){
                            outStr+=outChar; count++;
                            eChar++;
                            readGood=true;
                        }
                        else
                            readGood=false;
                        break;
                    default:
                        if (digChar==0) // string starting from wrong char
                            readGood=false;
                        break;
                }
            }
            if (digChar>0 && readGood)
                return outStr;
            else
                return null;
        }
        else
            return null;
    }
    
    public int text2Data(String dataStr){ //converting the text data in the single String-form into the set of \n-delimited rows and passing them to other version of text2data()
        int readChar=0; int crChar=0;
        int size=dataStr.length();
        String readStr;
        //System.out.println("size = " + size);
        if (size>4){
            dataStr+="\n";
            ArrayList<String> lines=new ArrayList();
            while (readChar<size-1 && readChar!=-1 && crChar<size-1){
                readChar=dataStr.indexOf("\n",crChar);
                if (readChar>0){
                    readStr=dataStr.substring(crChar, readChar+1);
                    lines.add(readStr);   
                                    
                }
                crChar=readChar+1;
            }
           
            return text2Data(lines);
        }
        else return 0;
    }
    public int text2Data(List<String> lines){  //parsing the tab-dilimited set of rows and converting to the data
        int col, row, nPoint, nCol,i,count;
        float[] crS, crY;
        String[] numStr1;
        String dataStr;
        nPoint=lines.size();
        //System.out.println("lines:"+nPoint);
        if(nPoint>3){ //otherwise, pasting the data will make no sense
            // skipping the non-parsable lines and detecting the parsable data columns
            nCol=0;
            for (row=0;row<4 && row<nPoint;row++){
                numStr1=lines.get(row).split("\t",-1);
                col=0;
                for (i=0;i<numStr1.length;i++){
                    //System.out.print(numStr1[i]);
                    dataStr=makeParsable(numStr1[i]);
                    if (dataStr!=null){
                        col++;
                        //System.out.println(" match:"+numStr1[i]);
                    }
                }
                if (nCol<col) nCol= col;
                if (col==0) nPoint--; // excluding non-parsable comments
            }
            
            if ((nCol>1)&&(nPoint>1)){
                for(col=1;col<nCol;col++) createComp(nPoint);
                count=0;
                crS=SpecComp[0].getData();
                for (row=0;row<nPoint;row++){
                    numStr1=lines.get(row).split("\t",-1);
                    dataStr=makeParsable(numStr1[0]);
                    
                    if (dataStr !=null){
                        SpecBase[count]=Float.parseFloat(dataStr);
                        crS[count]=0;
                        for(col=1;col<nCol;col++){
                            crY=SpecComp[col].getData();
                            dataStr=makeParsable(numStr1[col]);
                            if (dataStr !=null){
                                crY[count]=Float.parseFloat(dataStr);
                                crS[count]+=crY[count]/nCol;
                                //crS[row]=crY[row];
                            }
                       
                        }
                    count++;
                    }
                    
                    
                }
                NSPoint=count;
                for(col=0;col<NSComp;col++) {
                    SpecComp[col].trimTo(NSPoint);
                    SpecComp[col].updateScale();
                    SpecComp[col].recTime = col;
                }

            }
            else 
                NSPoint=0;

            return NSPoint;
        }
        else
            return 0;
    }
    public SpecSweep createComp (int destSize){  //destSize is used when 1st sweep is created
        if(NSComp<NCompMax)
        {
            if (NSPoint==0) {
                if (destSize<=NDataPointMax){
                    NSPoint=destSize; 
                }
                else NSPoint=NDataPointMax;
                SpecBase = new float[NSPoint];
                
            }
            if (NSComp==0){
                BcgrComp = new SpecSweep(SpecBase, NSPoint);
                SpecComp[0]=new SpecSweep(SpecBase, NSPoint); //special component to show the resulting component[0]
                SpecComp[1]=new SpecSweep(SpecBase, NSPoint);
                NSComp = 2;
                SpecComp[0].Select=0; SpecComp[1].Select=1; NSelComp=2;
                BegFitComp=2;
                
            }
            else {
                endSel=NSPoint-1;
                begSel=0;
                if (BegFitComp>=NSComp) { //there is no theortical curves, new sweep inserted at the end
                    SpecComp[NSComp]=new SpecSweep(SpecBase, NSPoint);
                    NSComp++;
                    BegFitComp=NSComp;
                }
                else { //new sweep is inserted before the theoretical curves
                    NSComp++;
                    for (int i=BegFitComp+1;i<NSComp;i++) // shifting up
                        SpecComp[i]=SpecComp[i-1];
                    SpecComp[BegFitComp]=new SpecSweep(SpecBase, NSPoint);
                    BegFitComp++;
                }
                if (NSelComp<=NSelPage) selCompAtIdx(NSComp-1,true);
                //System.out.println("nsc= "+NSelComp);
            }
            return SpecComp[NSComp-1];
        }
        else
            return null; //means that data series is already too large
        
    }
    public boolean createFitComp(FitParam newPar){ //creates model curve in non-empty data series
        int idx=0;
        int tgx=1;
        if(NSComp>0){
            SpecComp[NSComp]=new SpecSweep(SpecBase, NSPoint);
            if (BegFitComp>=NSComp)
                BegFitComp=NSComp;
            NSComp++;
            idx=NSComp-1;
            tgx=getTagComp();
            SpecComp[idx].Amp=SpecComp[tgx].Amp;
            SpecComp[idx].Zero=SpecComp[tgx].Zero;
            SpecComp[idx].Reset(newPar);
            SpecComp[idx].Select=1;
            // rescaling has already been done by Reset();
            //SpecComp[idx].multNum(SpecComp[tgx].Amp);
            //SpecComp[idx].addNum(SpecComp[tgx].Zero);
            
            return true;
           
        }
        else
            return false;
    }
    public int getCompSize(){ return NSComp;};
    public int getDataSize(){ return NSPoint;};
    public int getMarks2Plot(int[] plotX, int[] plotY, double[] axes, Rectangle drawRect){
        //axes[] 0:1 - xAxisBeg:Scale 2:3 - yAxisBeg:Scale
        int plotN, i;
        plotN=plotX.length;
        if (plotN>plotY.length) plotN=plotY.length;
        if (plotN>NMark) plotN=NMark;
        
        if (plotN>0 && axes.length>=4){
            for(i=0;i<plotN;i++){
                plotX[i]=(int)(drawRect.x+drawRect.width*(xMark[i]-axes[0])/axes[1]);
                plotY[i]=(int)(drawRect.y+drawRect.height*(1.0-(yMark[i]-axes[2])/axes[3]));
                
            }
            return plotN;
        }
        else
            return 0;
        
    }
    public void getSelMarkers(int[] marker){
        if (marker.length>=2){
            marker[0]=begSel;
            marker[1]=endSel;
        }
    }
    public boolean getPrimeMarkers(float[] marker){
        //0:1 beg X:Y, 2:3 end X:Y
        int i,idx=0; // last selected component
        for (i=0;i<NSComp;i++)
            if (isIdxSelected(i))
                idx=i;
        if (marker.length>=4){
            if (endSel>=0 && begSel >=0){
                marker[0]=SpecBase[begSel];
                marker[1]=SpecComp[idx].getYdata(begSel);
                marker[2]=SpecBase[endSel];
                marker[3]=SpecComp[idx].getYdata(endSel);
                return true;
            }
            else
                return false;
        }
        else return false;
    }
    
    //index of -1 always gets the last sweep
    public SpecSweep getCompAtIdx(int Idx) {
        if ((Idx >=0)&&(Idx<=NSComp)) 
            return SpecComp[Idx];
        else
            if (NSComp>0) 
                return SpecComp[NSComp-1];
            else
                return null;
    }
    //reports basic statistics: res[0:2] = mean, SD & slope 
    public boolean getScanAtIdx(float[] res, int idx){
        if (idx>=0 && idx < NSComp)
            return SpecComp[idx].Scan(res, begSel, endSel);
        else
            return false;
    }
    //converts the fit results  to the parameters of theoretical curves
    //using the same alignment as in SpecSweep FitResult, parameters high and low limits do not change,fitRes amplitudes are ignored
    //used to pass the parameters during FitThrough routine to re-try the autoFit
    public boolean convertFitResult2Par(float[] fitRes){
        int valIdx,j,count;
        if( fitRes.length >=FitParam.nFitPar+2 && BegFitComp < NSComp){
            FitParam tagFitParam = SpecComp[TagComp].getFitParam();
            if ( tagFitParam != null)
                for(valIdx=0; valIdx<FitParam.nFitPar; valIdx++)
                    tagFitParam.setValAtIdx(valIdx, fitRes[1+valIdx]);
            j = BegFitComp;
            count = 1;
            while( j<NSComp && count < fitRes.length){
                for(valIdx=0; valIdx<FitParam.nFitPar; valIdx++){
                    SpecComp[j].getFitParam().setValAtIdx(valIdx, fitRes[1+(j-BegFitComp)*(FitParam.nFitPar+1)+valIdx]);

                }
                j++;
                count += FitParam.nFitPar+1;
            }
            return true;  
        }
        else
            return false;
    }
    // returns -1 if sweep index does not exist, 0 - if sweep is not selected
    public int getSweepTableData(Object[] rowData, int sweepIdx){
        //System.out.println("table row "+sweepIdx);
        int i;
        float[] res=new float[3];
        if ((sweepIdx>=0)&&(sweepIdx<NSComp)){
            
            if (sweepIdx==0)
                rowData[0]="Sum";
            else 
            {
                if (sweepIdx>=BegFitComp)
                    rowData[0]="Fit"+String.valueOf(sweepIdx-BegFitComp+1);
                else
                    rowData[0]=String.valueOf(sweepIdx);
            }
            //rowData[1]=SpecComp[sweepIdx].recTime;
            //rowData[1]=String.valueOf(SpecComp[sweepIdx].recTime);
            if (SpecComp[sweepIdx].recTime>100)
                rowData[1]=String.format("%4.1f",SpecComp[sweepIdx].recTime);
            else
                rowData[1]=String.format("%3.2f",SpecComp[sweepIdx].recTime);
            rowData[2]=SpecComp[sweepIdx].Zero;
            rowData[3]=SpecComp[sweepIdx].Amp;
            rowData[7]=SpecComp[sweepIdx].Kf;
            if (SpecComp[sweepIdx].nFitResult>0){
                float[] fitRes = SpecComp[sweepIdx].getFitResult();
                rowData[8] = fitRes[0]; rowData[9] = fitRes[2]; rowData[10] = fitRes[3]; rowData[11] = fitRes[5];
                if (fitRes.length > 6){
                    rowData[12] = fitRes[7]; rowData[13] = fitRes[8]; rowData[14] = fitRes[10];
                }
            }
            else {
                for (i=8;i<15;i++)
                    rowData[i]=null;
            }
            
            /*
            if (fitRes != null){
                for (int j=0; j < Math.min(7,fitRes.length);j++)
                    rowData[8+j]=fitRes[j];
            }
            */
            if (SpecComp[sweepIdx].Select>-1) // currently, selection is ignored
                if (SpecComp[sweepIdx].Scan(res, begSel, endSel)){
                    for( i=0;i<3;i++)
                        rowData[4+i]=res[i];
                }
            
            return SpecComp[sweepIdx].Select;
        }
        else
            return -1;
    }
    //copies the parameters of the last model curve selected or the first one if no one is selected
    //returns the number of model curve related to the BegFitComp or -1 if there is no model curves
   
    
    public int getSelFitParam(FitParam destPar){
        int idx = -1;
        if (BegFitComp<NSComp){
            idx=BegFitComp;
            for (int i=BegFitComp;i<NSComp;i++)
                if(SpecComp[i].Select>0)
                    idx=i;
        }
        if(idx>0)
            SpecComp[idx].copyFitParam(destPar);
        return idx-BegFitComp;
    }
    public String getScanResult(boolean IgnoreSel){
        int idx, j;
        String outStr = new String();
        float[] res=new float[3];
        outStr+="time\t"+"mean\t"+"SD\t"+"Slope\n";
        for (idx = 0; idx < NSComp; idx++){
            if (SpecComp[idx].Select>0 || IgnoreSel){
                SpecComp[idx].Scan(res, begSel, endSel);
                outStr+=String.valueOf(SpecComp[idx].recTime);
                for (j=0;j<3;j++)
                    outStr+="\t"+String.valueOf(res[j]);
                outStr+="\n";
            }
        } 
        return outStr;
    }
    public String getFitResults(boolean[] fitResMask, String delimiter){
        String outStr = "";
        float [] fRes;
        int idx,j,i,count;
        count=0;
        for  (idx=1; idx<BegFitComp;idx++){
            fRes = SpecComp[idx].getFitResult();
            if(fRes != null){
                if(count==0){
                    //title line
                    outStr +="time";
                    //if (fitResMask[0]){
                    if (fitResMask[0] || (fRes.length < 7 && fitResMask[FitParam.nFitPar+1])){
                        //outStr += "\tFit Amp";
                        outStr += delimiter;
                        outStr += "FitAmp";
                    }
                    for(j=0; j<NSComp-BegFitComp; j++){
                        for(i=0; i<FitParam.nFitPar; i++){
                            if (fitResMask[i+1])
                                //outStr +=  "\t"+SpecComp[BegFitComp+j].getFitParam().getParNameAtIdx(i);
                                outStr +=  delimiter+SpecComp[BegFitComp+j].getFitParam().getParNameAtIdx(i);
                        }
                        //if ((fRes.length < 7 && !fitResMask[0]) || (fRes.length > 6 && fitResMask[FitParam.nFitPar+1])) //if only only one fitting curve, no need in separate amplitude
                        if ( ((fRes.length < 7 && !fitResMask[0])||fRes.length > 6) && fitResMask[FitParam.nFitPar+1] ) //if only only one fitting curve, no need in separate amplitude
                            //outStr += String.format("\tAmp%d",j+1);
                            outStr += delimiter + String.format("Amp%d",j+1);
                    }
                    outStr += "\n";
                }
                outStr+=String.valueOf(SpecComp[idx].recTime);
                if (fitResMask[0] || (fRes.length < 7 && fitResMask[FitParam.nFitPar+1])) 
                        //outStr += "\t"+ String.valueOf(fRes[0]);
                        outStr += delimiter + String.valueOf(fRes[0]);
                for (i=1;i<fRes.length;i++){
                    j= 1+(i-1) % (FitParam.nFitPar+1);
                    if(fitResMask[j])
                        //outStr += "\t"+String.valueOf(fRes[i]);
                        outStr += delimiter + String.valueOf(fRes[i]);
                }
                outStr += "\n";
                count ++;
            }
        }
        if(count ==0)
            outStr += "no fit result available";
        return outStr;
    }
    
    public String getTitle() {return Title;};
    
    public float[] getXBase(){ return SpecBase;};
    
    // creates an array of floats (if x-coord base does exist) 
    //and sets it as X-coord for all sweeps copying values from the newBase, if newBase size is not enough, returns 0
    
    public void insertCompFrom(SpecSeries destSpec, boolean ignoreSel){ //for inserting the selected sweeps from the clipboard series
        int NN=destSpec.NSComp;
        for (int j=0;j<NN;j++)
            if (destSpec.SpecComp[j].Select>0 || ignoreSel){
                //System.out.println("psting j= "+j);
                pasteComp(destSpec.SpecComp[j]);
                SpecComp[NSComp-1].Select=1;
            }
        //Average();
        //selComp(0,1);
    }
    public void pasteComp(SpecSweep destComp){
        int i;
        float[] destY, crY,destX,crX;
        int NN=destComp.getSize();
        if (NSComp ==0) {
           
            if (NN>0) {
                createComp(NN);
                setXBase(destComp.getBase());
                destY=destComp.getData();
                for (i=0;i<NN;i++){
                    SpecComp[0].getData()[i]=destY[i];
                    SpecComp[1].getData()[i]=destY[i];
                }
            }
            SpecComp[0].updateScale();
            SpecComp[1].updateScale();
        }
        else {
            //Y-data interpolation and copying here
            //testing
            
            if (createComp(NSPoint)!=null) {
                destY=destComp.getData();
                destX=destComp.getBase();
                crY=SpecComp[NSComp-1].getData();
                crX=SpecBase;
                // simple copying, intended for copying within the same data series
                if(crX[0]==destX[0] && crX[1]==destX[1] && crX[2]==destX[2])
                    if (NN>=NSPoint)
                        for (i=0;i<NSPoint;i++) crY[i]=destY[i];
                    else {
                        for (i=0;i<NN;i++) crY[i]=destY[i];
                        for (i=NN;i<NSPoint;i++) crY[i]=(crY[i-1]+crY[i-2]+crY[i-3])/3;
                    }
                else
                    for(i=0;i<NSPoint;i++)
                        crY[i]=destComp.getYdata(crX[i]);
                SpecComp[NSComp-1].updateScale();
            }
        }
        
    }
    //re-calculates the X-Base
    public void reScale(float xBeg, float xEnd){
        if (xEnd>xBeg){
            for (int j=0;j<NSPoint;j++)
                SpecBase[j]=j*(xEnd-xBeg)/NSPoint;
        }
        //selChange(xBeg,xEnd);
    }
    public boolean selChange(float mark1, float mark2){
        float sel1, sel2;
        if (mark1 > mark2){
            sel1=mark2; sel2=mark1;
        }
        else {
            sel1=mark1; sel2=mark2;
        }
        if ((sel2<SpecBase[0])||(sel1>SpecBase[NSPoint-1])) {
            begSel=-1; endSel=-1; return false;
        }    
        else {
            begSel=SpecComp[0].getNearestIdx(sel1);
            endSel=SpecComp[0].getNearestIdx(sel2);
            if (begSel==endSel && endSel<NSPoint-1) endSel++; 
            return true;
        }
        
        
        
    }
    //saving selected data sweeps a text file in parallel columns using delimiter, "," used for CSV files
    public void saveComp2ASCII(File outFile, String delimiter){
        float[] crY;
        int i,j,iBeg,iEnd,nCopy;
        iBeg=0; iEnd=NSPoint;
        String outStr = "";
        
        outStr+="time";
        nCopy=0;
        for (j=0;j<NSComp;j++){
            if (SpecComp[j].Select>0){
                nCopy++;
                 outStr+= delimiter;
                 outStr+= "YY"+String.valueOf(j+1);
            }
        }
        outStr+="\n";
        //picking and convering the data into wide format
        float[][] outData = new float[iEnd-iBeg][nCopy+1];
        for(i=iBeg; i<iEnd; i++)
            outData[i-iBeg][0] = SpecBase[i];
        nCopy=0;
        for (j=0;j<NSComp;j++)
            if (SpecComp[j].Select>0){
                crY = SpecComp[j].getData();
                nCopy++;
                for(i=iBeg; i<iEnd; i++)
                    outData[i-iBeg][nCopy] = crY[i];
            }
        //wrting string by string
        try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath(),StandardOpenOption.CREATE,StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            writer.write(outStr, 0, outStr.length());
            for(i=iBeg; i<iEnd; i++){
                outStr = "";
                for(j=0; j<nCopy; j++){
                    outStr+=String.format("%f%s",outData[i-iBeg][j],delimiter);
                }
                outStr+=String.format("%f%s",outData[i-iBeg][nCopy],"\n");
                writer.write(outStr, 0, outStr.length());
            }
            
            writer.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
    
    // saving fit results in a text/CSV file using delimiter provided
    public void saveFitResults(File outFile, boolean[] fitResMask, String delimiter){
        //wrting all results in one string
        try (BufferedWriter writer = Files.newBufferedWriter(outFile.toPath(),StandardOpenOption.CREATE,StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            String outStr = getFitResults(fitResMask, delimiter);
            writer.write(outStr, 0, outStr.length());
            writer.close();
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        }
    }
    public void selClear(){
        begSel=-1; endSel=-1;
    }
    // sweep selection methods
    //legacy methods, can change selection status of all sweeps
    public int selComp(int DestNum, int DeSelectMode){
	int j;
	if (DeSelectMode==1)
		deSelectAll();
	if (DestNum<=0) j = 0;
	else
		if (DestNum >=NSComp) j = NSComp-1;
		else
			j = DestNum;
	SpecComp[j].Select=1;
        NSelComp++;
	return j;
    }
    //finds and return the  raw data component for fitting
    public int getTagComp(){
        if (TagComp>0 && TagComp<=BegFitComp-1)
        return TagComp;
        else{ //tagComp has not been set yet
            TagComp=BegFitComp-1;
            for(int i=0;i<NSComp;i++) //returns last selected raw data sweep
                if(SpecComp[i].getMode()<0 && SpecComp[i].Select>0)
                    TagComp=i;
            return TagComp;
        }
    }
    public boolean setTagComp(int destN){
        if(destN>0 && destN<NSComp-1 && destN<=BegFitComp-1){
            TagComp=destN;
            return true;
        }
        else { 
            return false;
        }
    }
    public void deSelectAll() {
        for (int j=0;j<NSComp;j++)
			SpecComp[j].Select=0;
        NSelComp=0;
       
    }
    public boolean isIdxSelected(int compIdx) {
        if ((NSComp>0)&&(compIdx<NSComp))
            return (SpecComp[compIdx].Select==1);
        else
            return false;
    }
    public void removeComp(int idx){
        int j;
        if (idx>0 &&idx<NSComp){
            for(j=idx;j<NSComp-1;j++)
                SpecComp[j]=SpecComp[j+1];
            NSComp--;
            if (idx<BegFitComp)
                BegFitComp--;
        }
    }
    
    
//changes section status of chosen component
    public void selCompAtIdx(int Idx, boolean state){
        if ((Idx>=0)&&(Idx<NSComp)) {
            if (state) {
                
               SpecComp[Idx].Select=1;
               NSelComp++;
            }
            else {
                SpecComp[Idx].Select=0;
                NSelComp--;
                
            }
        }
    }
    
    //replaces legacy method SelCompIndex
    public void setSelectionFromList(int[] destNum, int count){
        int NN;
        if (count >0)
            NN=count;
        else
            NN=destNum.length;
        if (NN>0) {
            deSelectAll();
            for (int i=0;i<NN;i++)
                if (destNum[i]<NSComp) {
                    selCompAtIdx(destNum[i],true);
                }
        }
    }
    public void setTitle(String newTitle) { Title = newTitle;}
    
    public int setXBase(float[] newBase){
        int i;
        if (NSPoint==0){
            if (newBase.length>1){
                NSPoint=newBase.length;
                //System.out.println("lenght=" +NSPoint);
                SpecBase = new float [NSPoint];
                for (i=0;i<NSPoint;i++) SpecBase[i]=newBase[i];
                return NSPoint;
            }
            else return 0;
        }
        else {
            if (newBase.length>=NSPoint) {
                for (i=0;i<NSPoint;i++) SpecBase[i]=newBase[i];     
            return NSPoint;
            }
            else
                return 0;
        }
    }
    public void Smooth(int nCurve,float Fraction) {
	
	if (nCurve >=0 && nCurve < NSComp){
            SpecComp[nCurve].SmoothCut(begSel, endSel, Fraction);
        }
        //SpecComp[nCurve].Zero = SpecComp[nCurve].minVal();
        //SpecComp[nCurve].Amp = SpecComp[nCurve].maxVal()-SpecComp[nCurve].Zero;
    }
    public void Subtrack(boolean ignoreSel){ 
        float Sigma=0;
        int j;
	if ( NSComp>1){
            
            SpecComp[0].Select=1;
            
            for (j=1;j<NSComp;j++)
                if ( SpecComp[j].Select!=0||ignoreSel)
                   SpecComp[0].Subtrack(SpecComp[j]);
	
            SpecComp[0].Zero=SpecComp[0].minVal();
            SpecComp[0].Amp=SpecComp[0].maxVal()-SpecComp[0].Zero;
        }	
        
    }
}
