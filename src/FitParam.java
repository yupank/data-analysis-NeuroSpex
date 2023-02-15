/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */

/**
 *
 * @author lssgav
 */
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */


/**
 *
 * @author lssgav
 */
// just an auxilliary structure to hold the parameters of model curves

public class FitParam {
    private float [] Par;   //the actual value
    private float [] ParL;  //lower limit
    private float [] ParH;  //upper limit
    private float [] Step;  // step to change during fitting
    private int Mode;       //model curve type
    private String[] parName; //names of parameters, depend on Mode
    
    public static int nFitPar=4; // number of parameters for automated fitting
    public static int nTotPar=8;
    // mnemonic indexes
    public static int Max = 0;
    public static int Width = 1;
    public static int Split = 2;
    public static int Mix = 3;
    public static int Time = 4;
    public static int Mult = 5;
    public static int begAt = 0;
    public static int tRise = 1;
    public static int tDec = 2;
    // model names & modes
    public static final int PARAM       = 100;
    public static final int RIDEC_I     = PARAM+1;
    public static final int RIDEC_O     = PARAM+2;
    public static final int STEPRESP    = PARAM+3;
    
    public static final int UNIMODAL    = PARAM+4;
    public static final int BIMODAL     = PARAM+5;    
    public static final int SKEW_UN     = PARAM+6;
    
    public static final int BINOM       = PARAM+7;
    public static final int BINOM_EF    = PARAM+8;
    public static final int BINOM_NZ    = PARAM+9;
    public static final int BINOM_QU    = PARAM+10;
    public static final int BINOM_SUB   = PARAM+11;
    public static final int SINUS       = PARAM+12;
    public static final int GAUSLOR     = PARAM+12;
    public static final int GAUSDIR     = PARAM+13;
    public static final int LORDIR      = PARAM+14;
    public static final int RAWDATA     = -1;
    public static final int TEST_LLH    = PARAM+100;
    
    public static  String[]  modeName;
    
    public static float MinFloat=(float)1.0e-37;
    
    public FitParam(){
        //Par = new float[nTotPar];
        //ParL = new float[nTotPar];
        //ParH = new float[nTotPar];
        //Step = new float[nFitPar];
        Mode = RIDEC_I;
        Par = new float[]{20,1,10,1,10,1,0,0};
        ParL= new float[]{0,(float)0.1,3,0,0,1,0,0};
        ParH= new float[]{100,30,90,5,20,10,0,0};
        Step= new float[]{(float)0.3,(float)0.25,(float)0.5,(float)0.3,0,0,0,0};
        modeName = new String[LORDIR-PARAM];
        modeName[RIDEC_I-PARAM-1]="Inward current"; modeName[RIDEC_O-PARAM-1]="Outward current";   modeName[UNIMODAL-PARAM-1]="Unimodal(gauss)";    modeName[BIMODAL-PARAM-1]="Bimodal(gauss)";
        modeName[STEPRESP-PARAM-1]="Concentration clamp"; modeName[SKEW_UN-PARAM-1]="Skewed unimodal";   modeName[BINOM-PARAM-1]="Binomial";    modeName[BINOM_EF-PARAM-1]="Binom extra failures";
        modeName[BINOM_NZ-PARAM-1]="Binomial no zeros";    modeName[BINOM_QU-PARAM-1]="Quasi-binom compound";   modeName[BINOM_SUB-PARAM-1]="Binomial with saturation";
        modeName[SINUS-PARAM-1]="Sinusoid"; modeName[GAUSLOR-PARAM-1]="Gauss/Lorentz";   modeName[GAUSDIR-PARAM-1]="Gauss derivative";    modeName[LORDIR-PARAM-1]="Lorentz derivative";
        parName = new String[nTotPar];
        setParName();
        
    }
    public String getParNameAtIdx(int idx){
        if (idx>=0 && idx < nTotPar){
            return parName[idx];
        }
        else
            return "";
    }
    public float getValAtIdx(int idx){
        if (idx>=0 && idx < nTotPar){
            return Par[idx];
        }
        else
            return Par[0];
    }
    public void setParam(FitParam destPar){
        setLimL(destPar.ParL);
        setLimH(destPar.ParH);
        setStep(destPar.Step);
        setPar(destPar.Par);
        Mode=destPar.Mode;
        setParName();
    }
    public void setMode(int m){
        Mode=m;
    }
    public int getMode(){
        return Mode;
    }
    public void setParName(){
        int i;
        for (i=0;i<nTotPar;i++) parName[i]="";
        if (Mode>=RIDEC_I && Mode<=STEPRESP){
            parName[begAt]+="start at";
            parName[tRise]+="rise time";
            parName[tDec]+="decay time";
            if (Mode==STEPRESP){
                parName[Mix]+="fraction";
                parName[Time]+="time step";
                parName[Mult]+="decay2,%";
            }
        }
        if (Mode>=UNIMODAL && Mode<=SKEW_UN){
            parName[Max]+="centre";
            parName[Width]+="StDev";
            if (Mode==BIMODAL){
                parName[Mix]+="fraction";
                parName[Split]+="splitting";
                parName[Mult]+="width2,rel.u";
            }
            if (Mode==SKEW_UN){
            parName[Mix]+="gauss %";
            parName[Split]+="width2,r.u.";
            }
        }
        if (Mode>=GAUSLOR && Mode<=LORDIR){
            parName[Max]+="maximum";
            parName[Width]+="width";
            parName[Split]+="splitting";
            
        }
        if (Mode>=BINOM && Mode<=BINOM_SUB){
            parName[Max]+="p";
            parName[Width]+="dQn";
            parName[Split]+="Q";
            parName[Time]+="dQs";
            if (Mode==BINOM_EF)
        	parName[Mix]+="extra failures";
            if (Mode==BINOM_SUB)
         	parName[Mix]+= "EC50";
            parName[Mult]+="N";
        }
    }
    public void incrementAtIdx(int idx, float delta){
        if (idx >=0 && idx < nFitPar){
            Par[idx]+=delta;
            if (Par[idx]>ParH[idx]) Par[idx]=ParH[idx];
            if (Par[idx]<ParL[idx]) Par[idx]=ParL[idx];
        }
    }
    // key step operation during fitting 
    public void makeStepAtIdx(int idx, float mult, boolean relStep){
        if (idx >=0 && idx < nFitPar){
            if(relStep){
                Par[idx]+=mult*Step[idx]*Par[idx];
            }
            else   
                Par[idx]+=mult*Step[idx];
            if (Par[idx]>ParH[idx]) Par[idx]=ParH[idx];
            if (Par[idx]<ParL[idx]) Par[idx]=ParL[idx];
        }
            
    }
    public void scaleStepsBy(float factor){
        if (factor != 0)
            for (int idx = 0; idx < nTotPar; idx ++)
                Step[idx] /=factor;
    }
   
    public void setLimL(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                ParL[j]=val[j];
    }
    public void getLimL(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                val[j]=ParL[j];
    }
    public void setLimH(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                ParH[j]=val[j];
    }
    public void getLimH(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                val[j]=ParH[j];
    }
    public void extendLimAtIdx(int parIdx, float low, float high){
        int idx = parIdx;
        if (idx < 0) idx = 0;
        if (idx >= nTotPar) idx = nTotPar-1;
        if (low < ParL[idx]) ParL[idx] = low;
        if (high > ParL[idx]) ParH[idx] = high;
        
    }
    public void setStep(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                if(val[j]>=MinFloat)
                    Step[j]=val[j];
                else
                    Step[j]=MinFloat;
    }
    public void getStep(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                val[j]=Step[j];
    }
    public void getStep(float[] val, boolean relStep){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) {
            for (int j=0;j<n;j++)
                val[j]=Step[j];
            if (relStep){
                for (int j=0;j<n;j++)
                val[j]*=Par[j];
            }
        }
    }
    public void setPar(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++){
                if (val[j]<ParL[j]) Par[j]=ParL[j];
                else 
                   if (val[j]>ParH[j]) Par[j]=ParH[j];
                    else
                       Par[j]=val[j];
            }          
    }
    public void getPar(float[] val){
        int n=val.length;
        if (n>=nTotPar)
            n=nTotPar;
        else
            if (n>=nFitPar)
                n=nFitPar;
            else
                n=0;
        if (n>0) 
            for (int j=0;j<n;j++)
                val[j]=Par[j];
    }
    public void getPar(float[] val, int offset, int nPar){
        int n = Math.min(nPar, nTotPar);
        for (int j=0; j<n;j++)
            val[offset+j] = Par[j];
    }
}