

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yp
 */
// this class handles all back-end data processing
import java.util.Random;
import java.awt.Rectangle;

public class SpecSweep {
    protected float[] xBase;
    protected float[] Data;
    protected int NPoint;
    // these paraments are very frequently used in the parent SpecSeries
    public int Select; //for drawing in the parent panel
    public float recTime;
    public float Zero;  //bottom level
    public float Amp;   //total Y-coord amplitude
    public float Baseline; //baseline value averaged over the certain X-region, by default - first 20 data points
    public float Kf;    //coefficient in the least square fit
    public int Mode;    //type of theoretical curve
    public int nFitResult;
    // parameters for intialization and fitting of theoretical curves
    //first index = mnemonic type (depends on Mode), second index (0:2) - parameter and lower and high limits
    //set public for time efficiency, should be changed only via class methods
    private FitParam Param;
    private float[] FitResult;
    
    
    public SpecSweep(int Size){
        if (Size>1)
            NPoint=Size;
         else 
            NPoint=1;
        xBase = new float[NPoint];
        Data = new float[NPoint];
        Amp=1;
        Zero=0;
        Kf=1;
        Param = null;
        FitResult = null;
        nFitResult = 0;
   
    }
    public SpecSweep(float[] BaseRef, int Size){
        if (Size>2) {
            xBase=BaseRef;
            NPoint=Size;
            //xBase = new float[NPoint];
            //for (int i=0;i<NPoint;i++) xBase[i]=BaseRef[i];
            
            
        }
        else {
            NPoint=2;
            xBase = new float[NPoint];
        }
        Data = new float[NPoint];
        Kf=1;
        Amp=1;
        Zero=0;
        Param = null;
    }
    public void Add(SpecSweep swp){
        int i,n;
        n = Math.min(NPoint, swp.NPoint);
            for(i=0;i<n;i++)
                Data[i]+=swp.Data[i];
            for(i=n;i<NPoint;i++)
                Data[i]+=swp.Data[n-1];
    }
    
    public void Clip (int BegPoint, int EndPoint, float factor){
        int j,jBeg,jEnd;
        double y1,y2,x1,x2,x0,Beta,wid,amp,gss;
        if (factor>1)
            amp = 1/factor;
        else
            amp = (float) 0.15;
	jBeg=BegPoint;	jEnd=EndPoint;
	if (jBeg<8) jBeg=4;
	if (jEnd>NPoint-4) jEnd=NPoint-4;
	if (jBeg!=jEnd && EndPoint>0){
            //y1=0;   for (j=jBeg-4;j<jBeg;j++) y1+=Data[j]/4;
            //y2=0;   for (j=jEnd;j<jEnd+4;j++) y2+=Data[j]/4;
            y1=Data[jBeg-1];    y2=Data[jEnd];
            x1=xBase[jBeg-1];   x2=xBase[jEnd];
            wid=x2-x1;
            x0=(2*x1+x2)/3;
            for (j=jBeg;j<jEnd;j++){
                
                if (xBase[j] < x0)
                    Beta=(xBase[j]-x0)/(12*wid);
                else
                   Beta=(xBase[j]-x0)/(4*wid);
                Beta*=Beta; Beta/=2;
               
                gss=(float)0.3989*amp*Data[j]*Math.exp(-Beta);
                Data[j]=(float)(gss+y1+(y2-y1)*(xBase[j]-x1)/wid);
            }
            updateScale();
        }
    }
    
    // legacy function to solve the system of linear equations by Gauss method
    public static int EquSet(double[] Y, double[] A, double [] B, int N){
        int i,j,k;
        double OldPar,Mult;
        int[] Arr = new int[N+1];                           // arrangement: number of variable - number of column 
        
        for (j=0;j<N;j++)
            Arr[j]=j;
        for (k=0;k < (N-1);k++) {
            // system matrix tramsformation
            // testing the value of k-th coefficient of k-th  equation
            if ( A[N*k+k]==0 ) {
                i=k;
                while ( A[N*i+k]==0 && i < N ) i++;
                if ( A[N*i+k] !=0 ) {
                    // rearranging the aquations ( k-th and i-th)
                    for (j=0;j < N;j++){
                        OldPar = A[N*k+j];
                        A[N*k+j]= A[N*i+j];
                        A[N*i+j]=OldPar;
                    }
                    OldPar= B[k];
                    B[k]= B[i];
                    B[i] =OldPar;
                }
                else {//rearranging the variables
                    
                    i=k;
                    while ( A[N*k+i]==0 && i < N )
                          i++;
                    if ( A[N*k+i]!=0 )
                        for (j=0;j < N;j++) {
                            OldPar= A[N*j+k];
                            A[N*j+k] = A[N*j+i];
                            A[N*j+i] = OldPar;
                            Arr[i]=k;
                            Arr[k]=i;
                        }
                       else
                           return(0); // system can't been solved
                    } // of else
            }
            for (j=k+1;j < N;j++){
                // subtracting k-th equation  from j-th one
                Mult=A[N*j+k]/A[N*k+k];
                for (i=k;i < N;i++)
                    A[N*j+i]-=Mult*A[+k*N+i];
                B[j]-=Mult*B[k];
            }
        }
        i=N-1;
        Y[i]= B[i]/A[N*i+i];
        for (i=N-2;i>=0;i--){
            for (j=i+1;j < N;j++)
                B[i] -= A[N*i+j]*Y[j];
            Y[i]=B[i]/A[N*i+i];
        }
        for (k=0;k < N;k++) {
            OldPar= Y[k];
            Y[k]= Y[Arr[k]];
            Y[Arr[k]]=OldPar;
        }
        //free(Arr);
        return 1;
       
    }
    public int ExtremSearch(float dW, float CutAmp, float[]  LockX, float[] LockY, int NMax) { // NMax cannot > lenght of LockX and LockY
        
        int j, Count, Gr;
        float ExL_Y,ExL_X,ExR_X,ExR_Y;
        ExL_X= xBase[0]; 
        ExL_Y= Data[0]; 
        Count=0;
        j=0;
        if ( Data[1]>ExL_Y ) Gr=1;
                else Gr=-1;
        do {
            // Finding the current extremum
            do {
                j++; 
                ExR_X= xBase[j];
                ExR_Y= Data[j];
            }
            while (Gr*(Data[j+1]-ExR_Y) >0 && j< NPoint-2 ); 
            // Testing the current extremum
            if ( (ExR_X-ExL_X>=dW)&&(Math.abs(ExR_Y-ExL_Y)>=CutAmp) ) {
                if ( ExL_Y > ExR_Y ) {
                    //Returning current pair
                    LockX[Count]=ExL_X;
                    LockX[Count+1]=ExR_X;
                    LockY[Count]=ExL_Y;
                    LockY[Count+1]=ExR_Y;
                    Count+=2;
                }
                ExL_X= xBase[j];
                ExL_Y= Data[j];
                Gr *= -1;
            }
        }
        while( (j<NPoint-2)&&(Count<NMax-1) );
        return Count;
    }
    
    public int ExtremSearch(float[] FilterPar, float[] LocX, SpecSweep BcgrComp, boolean sigUp,  int startPoint, int endPoint){
        //Filter : FreqH, CutFreq, cutAmp, sdNoise
        int j,i,crIdx,jStart,jEnd;
        int NMax = LocX.length;
	float ExL_Y,ExL_X,ExR_X,ExR_Y,dW,Gr,PreLockX,PreLockY;
	float BcMean, bcgrNoise,SlopeL,SlopeR,sdN,dwL,dwR;
        int Count = 0;
        BcgrComp.InitData(this); //copy data for filtering and smoothing
        float[] CrX = xBase;
	float[] CrY = Data;
	float[] BcgrY = BcgrComp.Data;
        float[] scanRes = {0,0,0};
	if (startPoint>-1) jStart=startPoint;
            else jStart=0;
	if ((endPoint>-1)&&(endPoint<NPoint)) jEnd=endPoint;
            else jEnd=NPoint-1;
        //finding less "noisy" part of sweep - just "pure" noise, no signals
        BcgrComp.Scan(scanRes,0,(int)(0.05*NPoint));
        bcgrNoise = scanRes[1];
        BcgrComp.Scan(scanRes,(int)(0.95*NPoint),NPoint-1);
        if (scanRes[1]<bcgrNoise) bcgrNoise = scanRes[1];
        BcgrComp.Scan(scanRes,(int)(0.4*NPoint),(int)(0.6*NPoint));
        if (scanRes[1]<bcgrNoise) bcgrNoise = scanRes[1];
        FilterPar[3] = bcgrNoise;
        BcgrComp.SmoothCut(jStart, jEnd, (float) 0.02); 
        dwL = (float) 1.5*FilterPar[1];
        dwR = (float) 1.5*FilterPar[0];
        if (sigUp)
            Gr=-1; // search for maximums
        else 
            Gr=1;// search for minimums
        crIdx = jStart+1;
        ExR_Y = (Data[crIdx]+Data[crIdx-1]+Data[crIdx+1])/3;
        ExR_X = xBase[crIdx];
        PreLockX = ExR_X;
        PreLockY=Math.abs(BcgrY[crIdx]-ExR_Y);
        do {
            // Finding current extremum
            do {
                ExL_X=ExR_X; ExL_Y=ExR_Y;
                crIdx += 2; 
                ExR_Y = (Data[crIdx]+Data[crIdx-1]+Data[crIdx+1])/3;
                ExR_X = xBase[crIdx-1];

            }
            while ( Gr*(ExL_Y-ExR_Y) > 0 && crIdx<jEnd-2 );
            if ( (Gr*(BcgrY[crIdx]-ExL_Y) > 0.85*bcgrNoise*FilterPar[2]) && Count<NMax  ){
                if (ExL_X-PreLockX>0.2*dwR+0.5*dwL) {
                    Scan(scanRes,getNearestIdx(ExL_X-4*dwL) ,getNearestIdx(ExL_X-2*dwL));      
                    SlopeL = scanRes[0];
                    Scan(scanRes, getNearestIdx(ExL_X+4*dwR/5), getNearestIdx(ExL_X+2*dwR/2));  
                    SlopeR = scanRes[0];
                    if  ( Gr*(0.5*(SlopeL+SlopeR)-ExL_Y)>0.5*bcgrNoise*FilterPar[2] ) {
                        LocX[Count]= ExL_X-(float)0.4*dwL;
                        Count++;
                        PreLockX=ExL_X; 
                        PreLockY=Math.abs(BcgrY[crIdx]-ExL_Y);
                    }
                }
            }
        }
        while( crIdx < jEnd-2 && Count<NMax );

	return Count;
    }
    
    public float LikelyHoodTest(SpecSweep fitSweep){
        return 1;
    }
    
    public void MultSwp(SpecSweep swp){
        int i,n;
        n = Math.min(NPoint, swp.NPoint);
            for(i=0;i<n;i++)
                Data[i]*=swp.Data[i];
            for(i=n;i<NPoint;i++)
                Data[i]=swp.Data[n-1]*swp.Data[n-1];
    }
    public void InitData(float val){
        for(int i=0;i<NPoint;i++)
                Data[i]=val;
    }
    public void InitData(SpecSweep swp){
        int i,n;
        n = Math.min(NPoint, swp.NPoint);
            for(i=0;i<n;i++)
                Data[i]=swp.Data[i];
            for(i=n;i<NPoint;i++)
                Data[i]=swp.Data[n-1];
        Amp = swp.Amp;
        Zero = swp.Zero;
    }
    public double scalarMult(SpecSweep swp){
        int i, N;
        float S=0;
        if (swp.NPoint<NPoint)
            N=swp.NPoint;
        else    N=NPoint;
            for(i=0;i<N;i++)
                S+=Data[i]*swp.Data[i];
        return S;
    }
    public void SmoothCut(int begPoint,int endPoint, float Fraction){       
    // may bring some low-frequency distortions when used for the long strech of data; 
    //better be used for short data regions
        double[] A = new double[8];
	double[] B = new double[64];
	double[] C = new double[8];
	int k,m,i,j,p;
	int NBefore,NAfter; // the power of the curve before and after smoothing
	//float far* crX;
	//float far* crY;
	double  Xk,X;
	float  BegY,EndY,BegX,EndX,CfY;
        if (endPoint > NPoint-2) endPoint = NPoint-2;
        if (begPoint < 0) begPoint = 0;
	NBefore=endPoint-begPoint+1;
	BegY= Data[begPoint];
	EndY= Data[endPoint+1];
        NAfter=1+NBefore/5;
	if (NAfter<3) NAfter=3;
	if (NAfter>8)NAfter=8;
        //crX=Base+BegPoint;
        //crY=Data+BegPoint;
        for (k=0;k<NAfter;k++){
            C[k]=0;
            for (i=0;i<NBefore;i++) {
                Xk= Data[begPoint+i]; //(*(crY+i));
                X= xBase[begPoint+i]; //(*(crX+i));
                for (p=1;p<=k;p++)
                        Xk*=X; 
                C[k]+=Xk;
            }
            for (m=0;m<NAfter;m++) {
                B[k*NAfter+m] = 0;
                for(i=0;i<NBefore;i++) {
                    Xk=1.0;
                    X= xBase[begPoint+i];//(*(crX+i));
                    for (p=1;p<=(k+m);p++)
                        Xk*=X;
                    B[NAfter*k+m] += Xk;
                }
            }
        }
        EquSet(A,B,C,NAfter);
        for (i=0;i<NBefore;i++) {
            Data[begPoint+i] *= Fraction; //*(crY+i)*=Fraction;
            //X=Beg+((End-Beg)*i/NAfter);
            //*(crX+i)=X;
            X= xBase[begPoint+i];
            for (k=0;k<NAfter;k++) {
                Xk = A[k];
                for(p=1;p<=k;p++)
                    Xk*=X;
                Data[begPoint+i] += (1-Fraction)*Xk;//*(crY+i)+=(1-Fraction)*Xk;
            }
        }
        Zero = minVal();
        Amp = maxVal() - Zero;
    }
    // may bring some high-frequency distortions ; 
    public void SmoothLong(int begPoint,int endPoint, float Fraction){       
        int jBeg,jEnd;
        if (endPoint > NPoint-2) endPoint = NPoint-2;
        if (begPoint < 1) begPoint = 1;
        if (endPoint - begPoint > 100){
            for (jBeg = begPoint;jBeg<=endPoint;jBeg+=64){
                jEnd = jBeg+64;
                if (jEnd > endPoint) jEnd = endPoint;
                SmoothCut(jBeg-1,jEnd,Fraction);
            }
            
        }
        else SmoothCut(begPoint,endPoint, Fraction);
    }
    public void Subtrack(SpecSweep swp){
        int i,n;
        n = Math.min(NPoint, swp.NPoint);
            for(i=0;i<n;i++)
                Data[i]-=swp.Data[i];
            for(i=n;i<NPoint;i++)
                Data[i]-=swp.Data[n-1];
        
    }
    
    public void pasteFitParam(FitParam destPar){
        if(destPar!=null)
            Param.setParam(destPar);
        
    }
    public void copyFitParam(FitParam destPar){
        if(Param!=null)
            destPar.setParam(Param);
        
    }
    
    
    public void addNum(float num){
        
        for(int i=0;i<NPoint;i++)
                Data[i]+=num;
    }
     public void multNum(float num){
        for(int i=0;i<NPoint;i++)
                Data[i]*=num;
        
    }
    public void addNum(double num){
        for(int i=0;i<NPoint;i++)
                Data[i]+=num;
    }
     public void multNum(double num){
        for(int i=0;i<NPoint;i++)
                Data[i]*=num;
    }
    public float[] getBase(){ return xBase;};
    
    public float[] getData(){ return Data;};
    
    public FitParam getFitParam(){return Param;};
    
    public float[] getFitResult(){
        return FitResult;
    }
    public void setFitResult(float[] values){
        if (nFitResult < values.length ){
            FitResult = new float[values.length];
        }
        nFitResult = values.length;
        for (int i=0; i<nFitResult;i++)
            FitResult[i]=values[i];
    }
    public int getMode(){
        if(Param!=null)
            return Param.getMode();
        else
            return FitParam.RAWDATA;
    }
    //finding the nearest point to the mark
    public int getNearestIdx(float xMark){
        int idx, id0,id2,l1,l2;
        id0=0;
        id2=NPoint;
        if (xMark<xBase[0])
            idx=0;
        else
            if (xMark>xBase[NPoint-1])
                idx=NPoint-1;
            else
                do{
                    idx = (id0+id2)/2;
                    l1=idx-id0;
                    l2=id2-idx;
                    if (xMark<xBase[idx]){
                        id2=idx;
                    }
                    else id0=idx;
                }
                while ((l1>1)&&(l2>1));    
        return idx;
    }
  
    public int getNearestIdx(double xMark){
        int idx, id0,id2,l1,l2;
        id0=0;
        id2=NPoint;
        if (xMark<xBase[0])
            idx=0;
        else
            if (xMark>xBase[NPoint-1])
                idx=NPoint-1;
            else
                do{
                    idx = (id0+id2)/2;
                    l1=idx-id0;
                    l2=id2-idx;
                    if (xMark<xBase[idx]){
                        id2=idx;
                    }
                    else id0=idx;
                }
                while ((l1>1)&&(l2>1));
        
        return idx;
    }
    public float getYdata(float xMark){
        int idx=getNearestIdx(xMark);
        if(idx<0) return Data[0];
        else{
            if (idx>NPoint-2)
                idx=NPoint-2;
            return  Data[idx]+(xMark-xBase[idx])*(Data[idx+1]-Data[idx])/(xBase[idx+1]-xBase[idx]);
        }
        
    }
    public float getYdata(int idx){
        if (idx>NPoint-1)
                idx=NPoint-1;
        if(idx<0) idx=0;
        return Data[idx];
    }
    //this method picks the data point withing axes and calculates plot coords
    public int getPlot(int[] plotX, int[] plotY, double[] axes, Rectangle drawRect){
        //axes[] 0:1 - xAxisBeg:Scale 2:3 - yAxisBeg:Scale
        int plotN, step,limN, begN, endN,dN,i;
        
        begN=getNearestIdx(axes[0]);
        endN=getNearestIdx(axes[0]+1.1*axes[1]);
        //System.out.println("end N= "+endN);
        dN=endN-begN+1;
        limN = plotX.length;
        double coeff=1-(double)(limN/dN);
        if(limN>dN) step=1;
        else
            if(limN>dN/2) step=2;
            else
                //step = (int)(dN/(limN*coeff));
                step=Math.floorDiv(dN, limN)+1;
        
        plotN=0;
        i=begN;
        if((limN>1)&&(axes.length>=4)) {
            while ((i < endN) && (plotN < limN)){
            plotX[plotN]=(int)(drawRect.x+drawRect.width*(xBase[i]-axes[0])/axes[1]);
            plotY[plotN]=(int)(drawRect.y+drawRect.height*(1.0-(Data[i]-axes[2])/axes[3]));
            plotN++;
            i+=step;
            }
            return plotN;
        }
        else
            return 0;
    }
    public float GetBaseLevel(int destN){
        float S=0;
        int iBeg,iEnd,i;
        if (destN>0)
            iBeg=destN;
        else iBeg=0;
        iEnd=iBeg+10;
        if (iEnd>NPoint){
            iEnd=NPoint-1;
            iBeg=iEnd-10;
            if (iBeg<0)
                iBeg=0; 
        }
        for (i=iBeg;i<=iEnd;i++)
            S+=Data[i];
        S/=(iEnd-iBeg+1);
        return S;
    }
    
    public int getSize(){ return NPoint;}; 
    
    
    public float maxVal(){
        float Extrem=Data[0];
        for (int i=1;i<NPoint;i++)
            if(Extrem<Data[i]) Extrem=Data[i];
        return Extrem;
    }
    public float minVal(){
        float Extrem=Data[0];
        for (int i=1;i<NPoint;i++)
            if(Extrem>Data[i]) Extrem=Data[i];
        return Extrem;
    }
    // point-by-point statistical comparison
    public float normMSR(SpecSweep tagComp){ // root-mean square error
        float S=0;	float S_Diff=0;   	float S_Aver=0;
        float offset = (tagComp.Amp - tagComp.Zero)/2;
        float Y_A,Y_B;
        int j,N;
	if (tagComp.NPoint<NPoint)
            N=tagComp.NPoint;
        else    N=NPoint;
        for (j=0;j<N;j++){
            Y_A= Math.abs(Data[j]-offset);
            Y_B= Math.abs(tagComp.Data[j]-offset);
            //S_Aver+=Math.abs(Y_A)+Math.abs(Y_B);
            S_Aver+=Y_A+Y_B;
            Y_A-=Y_B;
            S_Diff+=Y_A*Y_A;
        }
        S_Aver+=FitParam.MinFloat;
        S_Diff+=FitParam.MinFloat;
	S=(float)(2*Math.sqrt(S_Diff*N)/S_Aver);
        return S;
    }
    public float normLLH(SpecSweep tagComp){ // log-likelyhood
        float S=0;
        float Y_A,Y_B;
        int j,N;
	if (tagComp.NPoint<NPoint)
            N=tagComp.NPoint;
        else    N=NPoint;
        for (j=0;j<N;j++)
         {
            Y_A= Data[j];
            Y_B= tagComp.Data[j]; 
            if (Y_B<10*FitParam.MinFloat)Y_B=10*FitParam.MinFloat;
            S+=Y_A*(-2)*Math.log(Y_B);
            }
        return S;
    }
    // method calculates probability density function of the sweep Y-values between selected boundary points 
    //and returns results via outSweep xBase & Data; 
    // addtional histogram parameters are passed as properties of outSweep: 
    //zero and amp - min and max values for sampling, Kf - kernel size, Mode - PDF or cumulative 
    public boolean pdHist(SpecSweep outSweep, int[] selBound){
        int i,j,Count;
        float crVal, extreMin, extreMax, normX;
        
        if (outSweep.NPoint>2 && NPoint>2){
            if (selBound[0] < 0 ) selBound[0]=0;
            if (selBound[1] > NPoint-1 || selBound[1]<=selBound[0]) selBound[1]=NPoint-1; // correcting wrong settings by user
            // legacy parametes of PD histrogram
            float stats[] = new float[3];
            Scan(stats, selBound[0], selBound[1]);
            float BinSize = stats[1]*outSweep.Kf;
            extreMin = Data[selBound[0]];
            extreMax = Data[selBound[0]];
            for (i=selBound[0]+1;i<=selBound[1];i++){
                if (extreMin > Data[i]) extreMin = Data[i];
                if (extreMax < Data[i]) extreMax = Data[i];
            }
            // correcting wrong settings by user: Y-value boundaries can only be narrow than data span
            if (outSweep.Zero > extreMin && outSweep.Zero < extreMax) extreMin = outSweep.Zero;
            if (outSweep.Amp > extreMin && outSweep.Amp < extreMax) extreMax = outSweep.Amp;
            int NBin = outSweep.NPoint-1;  
            normX = 10*stats[1]/(extreMax-extreMin);

            for (j=0;j<=NBin;j++){
                outSweep.xBase[j] = extreMin+j*(extreMax-extreMin)/((float)(NBin-0.5));
                outSweep.Data[j] = 0;
                Count = 0;
                for (i=0;i<NPoint;i++){
                    if (Data[i]>=extreMin && (Data[i])<=extreMax){
                        // only data values between margings, set by user, are taken into account
                        crVal = (outSweep.xBase[j]-Data[i])/BinSize;
                        outSweep.Data[j]+=0.5714*Math.exp(-crVal*crVal)/BinSize;
                        Count++;
                    }
                }
                //*(HistBase+j)=ExtreMin+j*(ExtreMax-ExtreMin)/(NBin-0.5);
                //crVal=(*(HistBase+j)-(*(crData+i)))/BinSize;
                //*(HistData+j)+=0.5714*exp(-crVal*crVal)/(Count*BinSize);
                //normalizing to the sample size and SD
                outSweep.Data[j]/=normX;
                outSweep.Data[j]/=(Count+1);
            }
         // normalisation ??
            return true;
        }
        else
            return false;
    }
    //passing negative amp makes to use own Zero and Amp
    public void Rescale(float zero, float amp){
        float newAmp = 10*Float.MIN_NORMAL;
        if (amp < newAmp){
            if(newAmp < Amp) newAmp = Amp;
        }
        else
            newAmp = amp;
        // nY = nA*(oY - oZ)/oA+nZ = oY*(nA/oA)-oZ*(nA/oA)+nZ
        float scale = newAmp/Amp;
        float shift = zero - Zero*scale;
        Amp = newAmp;
        Zero = zero;
        for (int j = 0; j<NPoint; j++){
            Data[j]*= scale;
            Data[j]+=shift;
        }
            
        
    }
    //resetting the waveform with own params
    public void Reset(){
        if (Param != null)
            Reset(Param);
    }
    //resetting the waveform with external params
    public void Reset(FitParam newPar){
        int j,n,k,i,m,crIndex,NStem;
        double Beta,Beta2,X,dX,p;
	double Wid,Max,Mix,Spl,Y,S,Z;
	double tr,td,t0,tn,tm,Fmax;
        if (Param ==null)
            Param = new FitParam();
        if (newPar!=null)
            Param.setParam(newPar);
        NStem=(int)Param.getValAtIdx(FitParam.Mult);
        Wid=Param.getValAtIdx(FitParam.Width);
        Spl=Param.getValAtIdx(FitParam.Split);
        Max=Param.getValAtIdx(FitParam.Max);
        Mix=Param.getValAtIdx(FitParam.Mix);
        tm=Param.getValAtIdx(FitParam.Time);
        Mode = Param.getMode();
        switch (Param.getMode()){
            case FitParam.RIDEC_O:
                tr=Param.getValAtIdx(FitParam.tRise);
                td=Param.getValAtIdx(FitParam.tDec);
                Max=Param.getValAtIdx(FitParam.begAt);
                X=0.00001*(xBase[NPoint-1]-xBase[0]);
                if (tr<X)tr=X;
                X*=2;
                if (td<X)td=X;
                t0=Math.log(td/tr)/(td-tr);
                Fmax=Math.exp(-t0*tr)-Math.exp(-t0*td);
                for (j=0;j<NPoint;j++) {
                    X= xBase[j];
                    if (X<Max) X=Max;
                    Beta=(Max-X)/tr;
                    Beta2=(Max-X)/td;
                    Data[j]=(float)((Math.exp(Beta2)-Math.exp(Beta))/Fmax);
                }  
                break;
            case FitParam.RIDEC_I:
                tr=Param.getValAtIdx(FitParam.tRise);
                td=Param.getValAtIdx(FitParam.tDec);
                Max=Param.getValAtIdx(FitParam.begAt);
                X=0.00001*(xBase[NPoint-1]-xBase[0]);
                if (tr<X)tr=X;
                X*=2;
                if (td<X)td=X;
                t0=Math.log(td/tr)/(td-tr);
                Fmax=Math.exp(-t0*tr)-Math.exp(-t0*td);
                for (j=0;j<NPoint;j++) {
                    X= xBase[j];
                    if (X<Max) X=Max;
                    Beta=(Max-X)/tr;
                    Beta2=(Max-X)/td;
                    Data[j]=(float)(1-(Math.exp(Beta2)-Math.exp(Beta))/Fmax);
                }  
                break;
            case FitParam.GAUSLOR:  //Gauss/lorentzian mix
                for (j=0;j<NPoint;j++) {
                    X= xBase[j];
                    Beta=2*(X-Max)/Wid;
                    Beta*=Beta;
                    Data[j]=(float)(Mix*Math.exp(-0.6931*Beta)+(1-Mix)/(1+Beta));
                }
                break;
            case FitParam.UNIMODAL: //gaussian distribution
                for (j=0;j<NPoint;j++) {
                        X= xBase[j];
                        Beta=(X-Max)/Wid;
                        Beta*=Beta; Beta/=2;
                        Data[j]=(float)Math.exp(-Beta);
                        //Data[j]=(float)0.3989*Math.exp(-Beta);
                }
                break;
            case FitParam.BIMODAL: // double gaussian
                // first peak: max,wid; 2-nd : -shift=split, rel.fraction = mix, rel.width = tm;
                // 1st peak
                for (j=0;j<NPoint;j++){
                    X= xBase[j];
                    Beta=(X-Max)/Wid;
                    Beta*=Beta; Beta/=2;
                    Data[j]=(float)((1-Mix)*Math.exp(-Beta));
                }
                //2nd peak
                for (j=0;j<NPoint;j++){
                    X= xBase[j];
                    Beta=(X-Max+Spl)/(tm*Wid);
                    Beta*=Beta; Beta/=2;
                    //*(Data+j)=0.3989*exp(-Beta)/Wid;
                    Data[j]+=(float)Mix*Math.exp(-Beta);
                }
                break;
            case FitParam.SKEW_UN:
                for (j=0;j<NPoint;j++){
                    X= xBase[j];
                    if (X<Max)
                        Beta=(X-Max)/Wid;
                    else
                       Beta=(X-Max)/(Spl*Wid);
                    Beta*=Beta; Beta/=2;
                    Data[j]=(float)Math.exp(-Beta);
                }
                break;  
        }
        // restroring scale
        for (j=0;j<NPoint;j++){
            Data[j]*=Amp;
            Data[j]+=Zero;
        }
        
        //Rescale(Zero,Amp);
        
    }
    // gives simple average between two points; always returns some value
    public float Scan(int BegPoint, int EndPoint){
        int i,jBeg,jEnd,count;
        float res;
        jBeg=BegPoint;	jEnd=EndPoint;
	if (jBeg<0) jBeg=0;
        if (jBeg>NPoint-2) jBeg = NPoint-2;
	if (jEnd>NPoint-1) jEnd=NPoint-1;
        if (jEnd<1) jEnd=1;
        count=0;
        res=0;
        for (i=jBeg; i<jEnd; i++){
            res+=Data[i];
            count++;
        }
        res/=count;
        return res;    
    }
    //reports basic statistics: res[0:2] = mean, SD & slope 
    public boolean Scan(float[] res, int BegPoint, int EndPoint){
        //
        float Mean, SD, Y,S,S1,S2,Extrem, X;
        float	MX=0;	float MY=0;	float MXY=0;	float MX2=0;
        int j, jMin, jMax, jBeg, jEnd;
        jBeg=0; jEnd=0;
	jBeg=BegPoint;	jEnd=EndPoint;
	if (jBeg<1) jBeg=1;
	if (jEnd>NPoint-1) jEnd=NPoint-1;
	if (jBeg!=jEnd && EndPoint>0)
	{
            Mean = 0; SD =0;
            for (j=jBeg-1;j<=jEnd;j++)
            {
                    Y=Data[j];
                    Mean+=Y/((float)(jEnd-jBeg+2));
                    SD+=Y*Y/((float)(jEnd-jBeg+2));
            }
            res[0]=Mean;
            res[1]=(float)Math.sqrt(Math.abs((float)(SD-Mean*Mean)));
            if (jBeg<2) jBeg=2;
            if (jEnd<3) jEnd=3;
            if (jEnd>NPoint-3) jEnd=NPoint-3;
            //simple slope
            X=  xBase[jBeg];
            MX= xBase[jEnd];
            Y=0; MY=0;
            for (j=0;j<5;j++)
            {
                Y+=0.2*Data[jBeg-2+j];
                MY+=0.2*Data[jEnd-2+j];
            }
            S2=(MY-Y)/(MX-X);
            // "advanced slope" - linear MSR fit, slope region is corrected to exclude extremum points
            Extrem=Data[jBeg-1];	jMin=jBeg-1;
            for (j=jBeg-1;j<=jEnd;j++)
                if (Data[j]<Extrem){
                 Extrem=Data[j];    jMin=j;
                 }
            Extrem=Data[jBeg-1];	jMax=jBeg-1;
            for (j=jBeg-1;j<jMin;j++)
                 if (Data[j]>Extrem){
                    Extrem=Data[j];    jMin=j;
                 }
            jBeg=(int)(jMax+0.5*(jMin-jMax));
            jEnd=(int)(0.2*(jMin-jMax));
            if (jEnd<3)     jEnd=3;
            if (jEnd>10)    jEnd=10;
            jMax=jBeg+2*jEnd;
            
            // Mean square root procedure (Kramer's rule)
            for (j=jBeg;j<jMax;j++){
                X= xBase[j];	Y= Data[j];
                MX+=X;	MX2+=X*X; MY+=Y; MXY+=X*Y;
            }
            S=(MX*MY-2*jEnd*MXY)/(2*jEnd*MX2-MX*MX);
            // average with the "simple" slopes
            res[2]=(float)(-0.7*S-0.3*S2);
            return true;
        }
        else
            return false;
        
    }
    public void trimTo(int N){
        if (N>2 && N<NPoint) NPoint=N;
    }
    public void updateScale(){
        Zero=minVal();
        Amp=maxVal()-Zero;
        
    } 
    
}
