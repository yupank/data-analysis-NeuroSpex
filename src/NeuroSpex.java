/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */

/**
 *
 * @author lssgav
 */

import javax.swing.plaf.basic.BasicTabbedPaneUI; 
import javax.swing.DefaultListModel;
import javax.swing.DefaultListSelectionModel;
import javax.swing.table.DefaultTableModel;
import java.awt.event.*;
import java.awt.FileDialog;
import javax.swing.TransferHandler.TransferSupport;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.JDialog;

public class NeuroSpex extends javax.swing.JFrame {

    /**
     * Creates new form NeuroSpecFrame
     */
    private int selSpecViewIdx;
    private int totSpecViewTab;
    private SpecPanel currViewPanel; // the data view, currently selected in the tabbed panel
    private int currViewIdx;
    private DefaultListModel seriesListModel,paramListModel;
    private DefaultTableModel specTableModel;
    private String    workDirectory;
    
    public static SpecSeries clbdSpec; // internal clipboard
    public static FitParam currFitPar; // for transfer from back- to front-end 
    //private ListSelectionModel seriesSelectionModel;
    public NeuroSpex() {
        int i;
        initComponents();
        setLocationRelativeTo (null);
        workDirectory = null;
        selSpecViewIdx=0;
        totSpecViewTab=0;
        //MainTabViewPane.setUI(new BasicTabbedPaneUI());
        seriesListModel= new DefaultListModel();
        SeriesList.setModel(seriesListModel);
        paramListModel = new DefaultListModel();
        ParamList.setModel(paramListModel);
        specTableModel=(DefaultTableModel) SpecTable.getModel();
        SpecPanel.transferSeries=null;
        SpecPanel.transferDataSize=0;
        clbdSpec=null;
        
        //SpecTable.getColumn(0).setWidth(10);
        //SpecTable.getColumn(1).setWidth(20);
        FitParamTable.getColumnModel().getColumn(0).setPreferredWidth(99);
        SpecTable.getColumnModel().getColumn(0).setPreferredWidth(40);
        SpecTable.getColumnModel().getColumn(1).setPreferredWidth(55);
        for (i=2;i<14;i++)
            SpecTable.getColumnModel().getColumn(i).setPreferredWidth(50);
        SpecTable.getColumnModel().getColumn(7).setPreferredWidth(40);
        currFitPar = new FitParam();
        for (i=FitParam.RIDEC_I;i<FitParam.LORDIR;i++) {
                    paramListModel.addElement(FitParam.modeName[i-FitParam.PARAM-1]);
        }
        
    }
    public void updateDataInfo(){
        updateSeriesSelection();
        //update the param Tables
        updateSweepTable(-1);
        updateFitParValue();
    }
    public void updateLabel(String labStr){
        frameLabel.setText(labStr);
        frameLabel.repaint();
    }
    public void updateFitParName(){
        int i=ParamList.getSelectedIndex();
        currFitPar.setMode(i+FitParam.PARAM+1);
        currFitPar.setParName();
        for (i=0;i<FitParam.nTotPar;i++)
            FitParamTable.setValueAt(currFitPar.getParNameAtIdx(i), i, 0);
    }
    public void updateFitParValue(){
        int i;
        float[] val = new float[FitParam.nTotPar];
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if(currViewPanel.focusSer>0)currViewPanel.getSelectedSeries().getSelFitParam(currFitPar);
        }
        ParamList.setSelectedIndex(currFitPar.getMode()-FitParam.PARAM-1);
        updateFitParName();
        currFitPar.getPar(val);
        for (i=0;i<FitParam.nTotPar;i++)
            FitParamTable.setValueAt(val[i], i, 1);
        
        currFitPar.getLimL(val);
        for (i=0;i<FitParam.nTotPar;i++)
            FitParamTable.setValueAt(val[i], i, 2);
        currFitPar.getLimH(val);
        for (i=0;i<FitParam.nTotPar;i++)
            FitParamTable.setValueAt(val[i], i, 3);
        currFitPar.getStep(val);
        for (i=0;i<FitParam.nTotPar;i++)
            FitParamTable.setValueAt(val[i], i, 4);
    }
    //updates the series list and the focus series
    public void updateSeriesSelection(){
        
        String serName="::";
        int i;
        int[] selRow=SeriesList.getSelectedIndices();
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        for (i=0;i<currViewPanel.getDataSize();i++) 
            currViewPanel.getDataSeries(i).IsSelected=false;
        for (i=0;i<selRow.length;i++)
            currViewPanel.getDataSeries(selRow[i]).IsSelected=true;     
        currViewPanel.updateSweeps();
        //updating the param table title      
        currViewPanel.focusSer=SeriesList.getSelectedIndex();
        if (currViewPanel.focusSer>=0)
            serName+=currViewPanel.getDataSeries(currViewPanel.focusSer).getTitle();
        SpecTitleTxt.setText(currViewPanel.getTitle()+serName);
       
        if (currViewPanel.autoScaleX)
                currViewPanel.resetScaleX(false,false);
        if (currViewPanel.autoScaleY)
                currViewPanel.resetScaleY(false,false);
       
       
    }
    public void updateSeriesList(){
        
        int i, NN;
        seriesListModel.removeAllElements();
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel !=null){
            NN=currViewPanel.getDataSize();
            if (NN>0){
                for (i=0;i<NN;i++) {
                    seriesListModel.addElement(currViewPanel.getDataSeries(i).getTitle());
                //updating the selection
                    if(currViewPanel.getDataSeries(i).IsSelected)
                        SeriesList.getSelectionModel().addSelectionInterval(i, i);

                }

            }
        }
        
    }
    
    
    public void updateSweepSelection(){
        
        int i,j,nCol;
        nCol=specTableModel.getColumnCount();
        Object [] rowData=new Object[nCol];   
        int[] selRow=SpecTable.getSelectedRows();
        SpecSeries buffSeries=currViewPanel.getDataSeries(currViewPanel.focusSer);
        if (buffSeries!=null){
            buffSeries.setSelectionList(selRow,0);
            currViewPanel.updateSweeps();
            if (currViewPanel.autoScaleX)
                currViewPanel.resetScaleX(false, false);
            if (currViewPanel.autoScaleY)
                currViewPanel.resetScaleY(true, false);
        }
        for (i=0;i<selRow.length;i++){
            buffSeries.getSweepTableData(rowData, selRow[i]);
            if(selRow[i]>=buffSeries.BegFitComp){
                FitCompTxt.setText(String.valueOf(selRow[i]-buffSeries.BegFitComp+1));
                buffSeries.getSelFitParam(currFitPar);
                updateFitParValue();
                }
            else
                FitCompTxt.setText("0");
            for (j=2;j<8;j++)
               SpecTable.setValueAt(rowData[j], selRow[i], j); 
        }
        
    }
    
    public void updateSweepTable(int rowIdx){
        int nCol, nRow, sel,i;
        nCol=specTableModel.getColumnCount();
        Object [] rowData=new Object[nCol];    
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        nRow=specTableModel.getRowCount();
        for (i=nRow-1;i>=0;i--)
            specTableModel.removeRow(i);
        if(currViewPanel!=null){
            if (currViewPanel.focusSer>=0){
                SpecSeries currSeries=currViewPanel.getDataSeries(currViewPanel.focusSer);
               if (rowIdx<0){
                    nRow=currSeries.getCompSize();
                    for (i=0;i<nRow;i++){
                        sel=currSeries.getSweepTableData(rowData, i);
                        if (sel>-1){
                            specTableModel.addRow(rowData);
                            if (sel>0){
                                SpecTable.getSelectionModel().addSelectionInterval(i, i);
                                if(i>=currSeries.BegFitComp){
                                    FitCompTxt.setText(String.valueOf(i-currSeries.BegFitComp+1));
                                    currSeries.getCompAtIdx(i).copyFitParam(currFitPar);
                                    updateFitParValue();
                                }
                                else
                                    FitCompTxt.setText("0");
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        xyBtnGroup = new javax.swing.ButtonGroup();
        FitSetDlg = new javax.swing.JDialog();
        fitSetOKBtn = new javax.swing.JButton();
        fitSetCancelBtn = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        fitSetYlowTxt = new javax.swing.JTextField();
        fitSetXlowTxt = new javax.swing.JTextField();
        fitSetXhighTxt = new javax.swing.JTextField();
        fitSetAccurLowTxt = new javax.swing.JTextField();
        fitSetAccurHighTxt = new javax.swing.JTextField();
        fitSetCycleTxt = new javax.swing.JTextField();
        fitSetStepTxt = new javax.swing.JTextField();
        fitSetInward = new javax.swing.JRadioButton();
        fitSetOutward = new javax.swing.JRadioButton();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jSeparator2 = new javax.swing.JSeparator();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jSeparator3 = new javax.swing.JSeparator();
        jLabel11 = new javax.swing.JLabel();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        fitSetGauss = new javax.swing.JRadioButton();
        fitSetBtnGroup1 = new javax.swing.ButtonGroup();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        MainTabViewPane = new javax.swing.JTabbedPane();
        jPanel4 = new javax.swing.JPanel();
        frameLabel = new javax.swing.JLabel();
        xToggleBtn = new javax.swing.JToggleButton();
        yToggleBtn = new javax.swing.JToggleButton();
        axisLeftBtn = new javax.swing.JButton();
        axisMinBtn = new javax.swing.JButton();
        axisPlusBtn = new javax.swing.JButton();
        axisRightBtn = new javax.swing.JButton();
        axisResetBtn = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        SeriesList = new javax.swing.JList<>();
        SpecTitleTxt = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        SpecTable = new javax.swing.JTable();
        CompCreateBtn = new javax.swing.JButton();
        ComDelBtn = new javax.swing.JButton();
        CompAlignBtn = new javax.swing.JButton();
        CompAveBtn = new javax.swing.JButton();
        CompSumBtn = new javax.swing.JButton();
        CompSubtrackBtn = new javax.swing.JButton();
        CompApplyBtn = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        ParamList = new javax.swing.JList<>();
        jScrollPane4 = new javax.swing.JScrollPane();
        FitParamTable = new javax.swing.JTable();
        FitCompTxt = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
        filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 32767));
        FitLstSqrBtn = new javax.swing.JButton();
        FitAccurTxt = new javax.swing.JLabel();
        FitAutoBtn = new javax.swing.JButton();
        jMenuBar1 = new javax.swing.JMenuBar();
        FileMenu = new javax.swing.JMenu();
        FileNew = new javax.swing.JMenuItem();
        FileOpen = new javax.swing.JMenuItem();
        FileSave = new javax.swing.JMenuItem();
        FileClose = new javax.swing.JMenuItem();
        FileCloseAll = new javax.swing.JMenuItem();
        EditMenu = new javax.swing.JMenu();
        CopySweep = new javax.swing.JMenuItem();
        CopyInternal = new javax.swing.JMenuItem();
        PasteSweep = new javax.swing.JMenuItem();
        InsertSweep = new javax.swing.JMenuItem();
        CopyResMenu = new javax.swing.JMenu();
        CopyRes = new javax.swing.JMenuItem();
        CopyAmp = new javax.swing.JCheckBoxMenuItem();
        CopyTRise = new javax.swing.JCheckBoxMenuItem();
        CopyTDec = new javax.swing.JCheckBoxMenuItem();
        CopyTotAmp = new javax.swing.JCheckBoxMenuItem();
        ToolsMenu = new javax.swing.JMenu();
        ToolsClip = new javax.swing.JMenuItem();
        ToolsBackgSub = new javax.swing.JMenuItem();
        ToolsSmooth = new javax.swing.JMenuItem();
        DataMenu = new javax.swing.JMenu();
        DataSlope = new javax.swing.JMenuItem();
        DataHist = new javax.swing.JMenuItem();
        DataAC = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        DataSearch = new javax.swing.JMenu();
        DataFindPeak = new javax.swing.JMenuItem();
        DataPeaksAnalysis = new javax.swing.JMenuItem();
        FitSettings = new javax.swing.JMenuItem();

        FitSetDlg.setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        FitSetDlg.setTitle("Peak Search and Analysis Settings");
        FitSetDlg.setAlwaysOnTop(true);
        FitSetDlg.setBounds(new java.awt.Rectangle(200, 400, 440, 320));
        FitSetDlg.setName("fitSetDialog"); // NOI18N
        FitSetDlg.setPreferredSize(new java.awt.Dimension(448, 300));

        fitSetOKBtn.setBackground(new java.awt.Color(204, 236, 220));
        fitSetOKBtn.setText("OK");
        fitSetOKBtn.setMaximumSize(new java.awt.Dimension(90, 29));
        fitSetOKBtn.setMinimumSize(new java.awt.Dimension(90, 29));
        fitSetOKBtn.setPreferredSize(new java.awt.Dimension(90, 29));
        fitSetOKBtn.setSize(new java.awt.Dimension(90, 29));
        fitSetOKBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fitSetOKBtn(evt);
            }
        });

        fitSetCancelBtn.setBackground(new java.awt.Color(255, 214, 227));
        fitSetCancelBtn.setText("Cancel");
        fitSetCancelBtn.setMaximumSize(new java.awt.Dimension(90, 29));
        fitSetCancelBtn.setMinimumSize(new java.awt.Dimension(90, 29));
        fitSetCancelBtn.setPreferredSize(new java.awt.Dimension(90, 29));
        fitSetCancelBtn.setSize(new java.awt.Dimension(90, 29));
        fitSetCancelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fitSetCancelBtn(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel2.setText("Detection thresholds :");

        fitSetYlowTxt.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        fitSetXlowTxt.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        fitSetXhighTxt.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        fitSetXhighTxt.setPreferredSize(new java.awt.Dimension(86, 25));

        fitSetStepTxt.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N

        fitSetBtnGroup1.add(fitSetInward);
        fitSetInward.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        fitSetInward.setText("Inward / negative");

        fitSetBtnGroup1.add(fitSetOutward);
        fitSetOutward.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        fitSetOutward.setText("Outward / positive");
        fitSetOutward.setToolTipText("");

        jLabel3.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel3.setText("Y (rel. to noise) :");

        jLabel4.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel4.setText("X  low :");

        jLabel5.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel5.setText("X high :");

        jLabel6.setText("Signal waveform :");

        jLabel7.setFont(new java.awt.Font("Lucida Grande", 1, 13)); // NOI18N
        jLabel7.setText("Automatic fit");

        jLabel9.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Low :");

        jLabel10.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("High :");

        jLabel8.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel8.setText("rejection accuracy limit");

        jLabel11.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel11.setText("maximum");

        jLabel12.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("cycles :");

        jLabel13.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        jLabel13.setText("steps :");

        fitSetBtnGroup1.add(fitSetGauss);
        fitSetGauss.setText("Gauss ");

        javax.swing.GroupLayout FitSetDlgLayout = new javax.swing.GroupLayout(FitSetDlg.getContentPane());
        FitSetDlg.getContentPane().setLayout(FitSetDlgLayout);
        FitSetDlgLayout.setHorizontalGroup(
            FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FitSetDlgLayout.createSequentialGroup()
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jSeparator2, javax.swing.GroupLayout.DEFAULT_SIZE, 448, Short.MAX_VALUE)
                    .addComponent(jSeparator3))
                .addGap(0, 0, Short.MAX_VALUE))
            .addGroup(FitSetDlgLayout.createSequentialGroup()
                .addGap(67, 67, 67)
                .addComponent(fitSetOKBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 87, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(fitSetCancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(97, 97, 97))
            .addGroup(FitSetDlgLayout.createSequentialGroup()
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(FitSetDlgLayout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jLabel2)
                            .addGroup(FitSetDlgLayout.createSequentialGroup()
                                .addGap(79, 79, 79)
                                .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(fitSetXhighTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))
                            .addGroup(FitSetDlgLayout.createSequentialGroup()
                                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(FitSetDlgLayout.createSequentialGroup()
                                        .addGap(79, 79, 79)
                                        .addComponent(jLabel4))
                                    .addComponent(jLabel3, javax.swing.GroupLayout.Alignment.TRAILING))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(fitSetXlowTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE)
                                    .addComponent(fitSetYlowTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 80, Short.MAX_VALUE))))
                        .addGap(36, 36, 36)
                        .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(fitSetOutward)
                            .addComponent(fitSetInward)
                            .addComponent(fitSetGauss)
                            .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(FitSetDlgLayout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jLabel7)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 48, Short.MAX_VALUE)
                        .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(FitSetDlgLayout.createSequentialGroup()
                        .addGap(106, 106, 106)
                        .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 52, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 56, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(fitSetAccurLowTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 73, Short.MAX_VALUE)
                            .addComponent(fitSetAccurHighTxt))
                        .addGap(41, 41, 41)
                        .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel13)
                            .addComponent(jLabel12))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(fitSetCycleTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 40, Short.MAX_VALUE)
                            .addComponent(fitSetStepTxt))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        FitSetDlgLayout.setVerticalGroup(
            FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(FitSetDlgLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 12, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(fitSetYlowTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel3)
                        .addComponent(fitSetInward)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel4)
                        .addComponent(fitSetXlowTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(fitSetOutward))
                .addGap(8, 8, 8)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(fitSetXhighTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fitSetGauss))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator2, javax.swing.GroupLayout.PREFERRED_SIZE, 6, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel7, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel11)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fitSetAccurLowTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(jLabel12)
                    .addComponent(fitSetCycleTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel10)
                    .addComponent(fitSetAccurHighTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel13)
                    .addComponent(fitSetStepTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator3, javax.swing.GroupLayout.PREFERRED_SIZE, 10, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(FitSetDlgLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(fitSetOKBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(fitSetCancelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("NeuroSpex");
        setPreferredSize(new java.awt.Dimension(1350, 640));

        jSplitPane1.setDividerLocation(700);
        jSplitPane1.setDividerSize(5);
        jSplitPane1.setResizeWeight(1.0);
        jSplitPane1.setPreferredSize(new java.awt.Dimension(1120, 715));

        MainTabViewPane.setMinimumSize(new java.awt.Dimension(321, 221));
        MainTabViewPane.setPreferredSize(new java.awt.Dimension(821, 721));
        MainTabViewPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                TabViewChanged(evt);
            }
        });

        jPanel4.setAutoscrolls(true);

        frameLabel.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        frameLabel.setText("[X,Y]:");

        xyBtnGroup.add(xToggleBtn);
        xToggleBtn.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        xToggleBtn.setText("X");

        xyBtnGroup.add(yToggleBtn);
        yToggleBtn.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        yToggleBtn.setSelected(true);
        yToggleBtn.setText("Y");
        yToggleBtn.setMaximumSize(new java.awt.Dimension(80, 29));
        yToggleBtn.setMinimumSize(new java.awt.Dimension(80, 29));

        axisLeftBtn.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        axisLeftBtn.setText("<<");
        axisLeftBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                axisLeftBtnActionPerformed(evt);
            }
        });

        axisMinBtn.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        axisMinBtn.setText("-");
        axisMinBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                axisMinBtnActionPerformed(evt);
            }
        });

        axisPlusBtn.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        axisPlusBtn.setText("+");
        axisPlusBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                axisPlusBtnActionPerformed(evt);
            }
        });

        axisRightBtn.setFont(new java.awt.Font("Lucida Grande", 0, 14)); // NOI18N
        axisRightBtn.setText(">>");
        axisRightBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                axisRightBtnActionPerformed(evt);
            }
        });

        axisResetBtn.setBackground(new java.awt.Color(255, 204, 204));
        axisResetBtn.setFont(new java.awt.Font("Lucida Grande", 1, 14)); // NOI18N
        axisResetBtn.setText("#");
        axisResetBtn.setToolTipText("re-scale,<Shift> ignores selection");
        axisResetBtn.setMixingCutoutShape(null);
        axisResetBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                axisResetBtnActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(frameLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(xToggleBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(yToggleBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 40, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(axisLeftBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(axisMinBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 43, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(axisPlusBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(axisRightBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(axisResetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(196, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(axisResetBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(axisRightBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(axisPlusBtn)
                    .addComponent(frameLabel)
                    .addComponent(xToggleBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(yToggleBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(axisLeftBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(axisMinBtn)))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(15, 15, 15))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(MainTabViewPane, javax.swing.GroupLayout.PREFERRED_SIZE, 689, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(542, Short.MAX_VALUE)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(30, 30, 30))
            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel1Layout.createSequentialGroup()
                    .addComponent(MainTabViewPane, javax.swing.GroupLayout.PREFERRED_SIZE, 536, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 71, Short.MAX_VALUE)))
        );

        jSplitPane1.setLeftComponent(jPanel1);

        jPanel2.setPreferredSize(new java.awt.Dimension(598, 689));

        SeriesList.setSelectionBackground(new java.awt.Color(153, 153, 255));
        SeriesList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SeriesListClicked(evt);
            }
        });
        SeriesList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                SeriesListKeyPressed(evt);
            }
        });
        jScrollPane1.setViewportView(SeriesList);

        SpecTable.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        SpecTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "N", "Time", "Bottom", "Scale", "Scan", "SDscan", "Slope", "Cfn", "FitAmp", "tDec", "tRise", "Amp1", "tDec2", "tRise2", "Amp2"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, true, true, false, false, false, true, true, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        SpecTable.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
        SpecTable.setGridColor(new java.awt.Color(204, 204, 204));
        SpecTable.setSelectionBackground(new java.awt.Color(204, 204, 255));
        SpecTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        SpecTable.setShowGrid(true);
        SpecTable.setShowHorizontalLines(false);
        SpecTable.getTableHeader().setReorderingAllowed(false);
        SpecTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                SpecTableMouseClicked(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                SpecTableMouseReleased(evt);
            }
        });
        SpecTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                SpecTableKeyPressed(evt);
            }
            public void keyReleased(java.awt.event.KeyEvent evt) {
                SpecTableKeyReleased(evt);
            }
        });
        jScrollPane3.setViewportView(SpecTable);

        CompCreateBtn.setBackground(new java.awt.Color(170, 225, 197));
        CompCreateBtn.setText("Create");
        CompCreateBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompCreate(evt);
            }
        });

        ComDelBtn.setBackground(new java.awt.Color(248, 209, 209));
        ComDelBtn.setText("Delete");
        ComDelBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompDelete(evt);
            }
        });

        CompAlignBtn.setText("Align");
        CompAlignBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompAlign(evt);
            }
        });

        CompAveBtn.setText("< Mean >");
        CompAveBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompAverage(evt);
            }
        });

        CompSumBtn.setText("+ Sum");
        CompSumBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompSum(evt);
            }
        });

        CompSubtrackBtn.setText("- Sub");
        CompSubtrackBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompSubtrack(evt);
            }
        });

        CompApplyBtn.setText("Apply");
        CompApplyBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CompApply(evt);
            }
        });

        ParamList.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        ParamList.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        ParamList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                ParamListMouseClicked(evt);
            }
        });
        ParamList.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                ParamListKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(ParamList);

        FitParamTable.setFont(new java.awt.Font("Arial", 0, 11)); // NOI18N
        FitParamTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "Parameter", "Value", "Limit Lo", "Limit Hi", "Step"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.String.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class, java.lang.Float.class
            };
            boolean[] canEdit = new boolean [] {
                false, true, true, true, true
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        FitParamTable.setGridColor(new java.awt.Color(204, 204, 204));
        FitParamTable.setSelectionBackground(new java.awt.Color(255, 204, 204));
        FitParamTable.setShowGrid(true);
        FitParamTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                FitParamTableKeyReleased(evt);
            }
        });
        jScrollPane4.setViewportView(FitParamTable);

        FitCompTxt.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        FitCompTxt.setAlignmentY(0.0F);

        jLabel1.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        jLabel1.setText("Model Curve");

        FitLstSqrBtn.setBackground(new java.awt.Color(204, 204, 255));
        FitLstSqrBtn.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        FitLstSqrBtn.setText("LeastSqr");
        FitLstSqrBtn.setEnabled(false);
        FitLstSqrBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FitLeastSquare(evt);
            }
        });

        FitAccurTxt.setFont(new java.awt.Font("Lucida Grande", 0, 11)); // NOI18N
        FitAccurTxt.setText("Accuracy :");
        FitAccurTxt.setEnabled(false);

        FitAutoBtn.setBackground(new java.awt.Color(204, 204, 255));
        FitAutoBtn.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        FitAutoBtn.setText("Auto Fit");
        FitAutoBtn.setEnabled(false);
        FitAutoBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FitAuto(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                        .addGap(0, 350, Short.MAX_VALUE)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(313, 313, 313))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(218, 218, 218))))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(CompSubtrackBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CompSumBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CompAveBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CompAlignBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(SpecTitleTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 381, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(CompApplyBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(10, 10, 10)
                                .addComponent(jLabel1)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(FitCompTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(FitAccurTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 223, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(filler3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(ComDelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(CompCreateBtn, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 127, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 381, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addComponent(FitLstSqrBtn)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(FitAutoBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 81, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE))))
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                    .addContainerGap(137, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 499, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(33, 33, 33)))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(SpecTitleTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CompAlignBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CompAveBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CompSumBtn)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(CompSubtrackBtn)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CompApplyBtn)
                        .addGap(16, 16, 16)
                        .addComponent(CompCreateBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ComDelBtn, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(filler3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(FitAccurTxt)
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(FitCompTxt, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, 20, Short.MAX_VALUE))
                                .addGap(3, 3, 3)
                                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 157, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(FitLstSqrBtn)
                                            .addComponent(FitAutoBtn)))
                                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 197, javax.swing.GroupLayout.PREFERRED_SIZE))))))
                .addGap(41, 41, 41)
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(93, 93, 93)
                .addComponent(filler2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                    .addContainerGap(30, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 269, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(379, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, 586, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        jSplitPane1.setRightComponent(jPanel2);

        FileMenu.setText("File");

        FileNew.setText("New");
        FileNew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileNewActionPerformed(evt);
            }
        });
        FileMenu.add(FileNew);

        FileOpen.setText("Open");
        FileOpen.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileOpenActionPerformed(evt);
            }
        });
        FileMenu.add(FileOpen);

        FileSave.setText("Save");
        FileSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileSaveActionPerformed(evt);
            }
        });
        FileMenu.add(FileSave);

        FileClose.setText("Close");
        FileClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileCloseActionPerformed(evt);
            }
        });
        FileMenu.add(FileClose);

        FileCloseAll.setText("CloseAll");
        FileCloseAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                FileCloseAllActionPerformed(evt);
            }
        });
        FileMenu.add(FileCloseAll);

        jMenuBar1.add(FileMenu);

        EditMenu.setText("Edit");

        CopySweep.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.META_DOWN_MASK));
        CopySweep.setText("Copy");
        CopySweep.setToolTipText("internal & system clipboard");
        CopySweep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditCopySweep(evt);
            }
        });
        EditMenu.add(CopySweep);

        CopyInternal.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_DOWN_MASK));
        CopyInternal.setText("Copy internal");
        CopyInternal.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CopyInternal(evt);
            }
        });
        EditMenu.add(CopyInternal);

        PasteSweep.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.META_DOWN_MASK));
        PasteSweep.setText("Paste");
        PasteSweep.setToolTipText("from System");
        PasteSweep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditPaste(evt);
            }
        });
        EditMenu.add(PasteSweep);

        InsertSweep.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_I, java.awt.event.InputEvent.CTRL_DOWN_MASK));
        InsertSweep.setText("Insert");
        InsertSweep.setToolTipText("from internal clipboard");
        InsertSweep.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditInsertSweep(evt);
            }
        });
        EditMenu.add(InsertSweep);

        CopyResMenu.setText("Copy Results");

        CopyRes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.SHIFT_DOWN_MASK | java.awt.event.InputEvent.CTRL_DOWN_MASK));
        CopyRes.setText("Copy Fit results");
        CopyRes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                EditCopyRes(evt);
            }
        });
        CopyResMenu.add(CopyRes);

        CopyAmp.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        CopyAmp.setSelected(true);
        CopyAmp.setText("Component Amplitude");
        CopyResMenu.add(CopyAmp);

        CopyTRise.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        CopyTRise.setText("Rise time");
        CopyResMenu.add(CopyTRise);

        CopyTDec.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        CopyTDec.setSelected(true);
        CopyTDec.setText("Decay time");
        CopyResMenu.add(CopyTDec);

        CopyTotAmp.setFont(new java.awt.Font("Lucida Grande", 0, 12)); // NOI18N
        CopyTotAmp.setText("Total amplitude");
        CopyResMenu.add(CopyTotAmp);

        EditMenu.add(CopyResMenu);

        jMenuBar1.add(EditMenu);

        ToolsMenu.setText("Tools");
        ToolsMenu.setToolTipText("");

        ToolsClip.setText("Trim stimulus artefact");
        ToolsClip.setToolTipText("+<shift> ignores selection");
        ToolsClip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToolsClip(evt);
            }
        });
        ToolsMenu.add(ToolsClip);

        ToolsBackgSub.setText("Subtract Backround");
        ToolsBackgSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToolsBackgSubActionPerformed(evt);
            }
        });
        ToolsMenu.add(ToolsBackgSub);

        ToolsSmooth.setText("Smooth");
        ToolsSmooth.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ToolsSmooth(evt);
            }
        });
        ToolsMenu.add(ToolsSmooth);

        jMenuBar1.add(ToolsMenu);

        DataMenu.setText("Data analysis");

        DataSlope.setText("Slope");
        DataSlope.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataSlope(evt);
            }
        });
        DataMenu.add(DataSlope);

        DataHist.setText("Histogram");
        DataHist.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataHist(evt);
            }
        });
        DataMenu.add(DataHist);

        DataAC.setText("Autocorrelation");
        DataAC.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataAC(evt);
            }
        });
        DataMenu.add(DataAC);
        DataMenu.add(jSeparator1);

        DataSearch.setText("Signal peaks");

        DataFindPeak.setText("... in the selected");
        DataFindPeak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataFindPeaks(evt);
            }
        });
        DataSearch.add(DataFindPeak);

        DataPeaksAnalysis.setText("... in sweep range");
        DataPeaksAnalysis.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataPeaksAnalysis(evt);
            }
        });
        DataSearch.add(DataPeaksAnalysis);

        DataMenu.add(DataSearch);

        FitSettings.setText("Settings");
        FitSettings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                DataFitSettings(evt);
            }
        });
        DataMenu.add(FitSettings);

        jMenuBar1.add(DataMenu);

        setJMenuBar(jMenuBar1);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 1379, javax.swing.GroupLayout.PREFERRED_SIZE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 611, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void FileNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileNewActionPerformed
        // TODO add your handling code here:
        SpecPanel bufViewPanel = new SpecPanel((NeuroSpex)this);
        bufViewPanel.setTitle("Data Set "+(totSpecViewTab+1));
        // if the system clipboard contains suitable data

        /*
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel.transferHandler.pasteSysClb()){
            currViewPanel.repaint();
            updateDataInfo();
        }
        */
        MainTabViewPane.addTab("Data Set "+(totSpecViewTab+1), bufViewPanel);
        MainTabViewPane.setSelectedIndex(totSpecViewTab);

        totSpecViewTab++;
        updateDataInfo();

    }//GEN-LAST:event_FileNewActionPerformed

    private void FileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileOpenActionPerformed
        // TODO add your handling code here:
        //currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        String extStr, titleStr;
        FileDialog fdLoad = new FileDialog (this, "Open File", FileDialog.LOAD);
        fdLoad.setVisible (true);

        File[] fileLoad=fdLoad.getFiles();
        if(fileLoad.length>0){
            String nameStr=fileLoad[0].getName();
            workDirectory=fdLoad.getDirectory();
            //finding and cutting the extension
            int ptIdx=nameStr.lastIndexOf(".");
            if (ptIdx>0){
                extStr=nameStr.substring(ptIdx+1);
                titleStr=nameStr.substring(0, ptIdx);
            }
            else { // no extension - will be treated as ASCII file
                extStr ="";
                titleStr = nameStr;
            }
            SpecPanel bufViewPanel = new SpecPanel((NeuroSpex)this);
            if (extStr.equalsIgnoreCase("wcp"))
                bufViewPanel.readWCPX(fileLoad[0]);
            else
                if ((extStr.equalsIgnoreCase("FDR"))||(extStr.equalsIgnoreCase("EDR")))
                    System.out.println("reading FDR");
                else
                    if ((extStr.equalsIgnoreCase("pul"))||(extStr.equalsIgnoreCase("pgf")))
                        System.out.println("reading Pulse");
            else
                bufViewPanel.readASCII(fileLoad[0]);
            //System.out.println("ext:"+extStr+":");
            bufViewPanel.setTitle(titleStr);
            MainTabViewPane.addTab(titleStr, bufViewPanel);

            MainTabViewPane.setSelectedIndex(totSpecViewTab);
            totSpecViewTab++;
            updateDataInfo();
        }
        String dirName = fdLoad.getDirectory();
        String fileName =fdLoad.getFile();
        //System.out.println("dir: "+workDirectory);
        //System.out.println("file: "+fileName);

    }//GEN-LAST:event_FileOpenActionPerformed

    private void FileSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileSaveActionPerformed
        // TODO add your handling code here:
        FileDialog fdLoad = new FileDialog (this, "Save File", FileDialog.SAVE);
        fdLoad.setVisible (true);
        File[] fileSave=fdLoad.getFiles();
        if (fileSave.length>0){
            String dirName = fdLoad.getDirectory();
            String fileName =fdLoad.getFile ();
            currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
            currViewPanel.writeASCII(fileSave[0]);
        }
        //else
            //System.out.println("no file");
            
    }//GEN-LAST:event_FileSaveActionPerformed

    private void FileCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileCloseActionPerformed
        // TODO add your handling code here:
        MainTabViewPane.remove(currViewPanel);
        totSpecViewTab=MainTabViewPane.getTabCount();
        MainTabViewPane.repaint();
        this.repaint();
       
    }//GEN-LAST:event_FileCloseActionPerformed

    private void FileCloseAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FileCloseAllActionPerformed
        // TODO add your handling code here:
        int i,nRow;
        MainTabViewPane.removeAll();
        seriesListModel.removeAllElements();
        nRow=specTableModel.getRowCount();
        for (i=nRow-1;i>=0;i--)
        specTableModel.removeRow(i);
        totSpecViewTab=MainTabViewPane.getTabCount();
        MainTabViewPane.repaint();
    }//GEN-LAST:event_FileCloseAllActionPerformed

    private void EditCopySweep(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditCopySweep

        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        //if(clbdSpec==null)
        clbdSpec=currViewPanel.getSelectedSeries();
        currViewPanel.transferHandler.copyData2Sys(); // copying selected sweeps to System clipboard

    }//GEN-LAST:event_EditCopySweep

    private void EditPaste(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditPaste
        // pastes ASCII-text data as a new series
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
      
            if (currViewPanel.pasteASCII()){
                updateSeriesList(); //needed becase new series has been created
                updateDataInfo();
                currViewPanel.repaint();
            }
            //else System.out.println("Wrong format");

    }//GEN-LAST:event_EditPaste

    private void EditInsertSweep(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditInsertSweep
        // inserts binary data, using interpolation if needed
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        
            if (currViewPanel.pasteSweeps()){
                updateSeriesList(); //needed becase new series has been created
                updateDataInfo();
                currViewPanel.repaint();
                
            }
            
    }//GEN-LAST:event_EditInsertSweep

    private void EditCopyRes(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_EditCopyRes
        // TODO add your handling code here:
        System.out.println("Copy Results");
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        currViewPanel.transferHandler.copyResults2Sys();
    }//GEN-LAST:event_EditCopyRes

    private void ToolsClip(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToolsClip

        boolean ignoreSel;
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if ((evt.getModifiers()& ActionEvent.SHIFT_MASK) !=0)
        ignoreSel=true;
        else
        ignoreSel=false;

        currViewPanel.clipArtifact(ignoreSel);
        updateDataInfo();
        currViewPanel.resetScaleY(false,false);
        currViewPanel.repaint();

    }//GEN-LAST:event_ToolsClip

    private void DataSlope(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataSlope

        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        
            if (currViewPanel.calculateSlope()){
                updateSeriesList(); //needed becase new series has been created
                updateDataInfo();
                currViewPanel.resetScaleY(false,false);
                currViewPanel.repaint();
            }
    }//GEN-LAST:event_DataSlope

    private void TabViewChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_TabViewChanged
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        currViewIdx = MainTabViewPane.getSelectedIndex();
        if (currViewPanel!=null){
            updateSeriesList();
            updateDataInfo();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_TabViewChanged

    private void axisResetBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_axisResetBtnActionPerformed
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
       
        if (currViewPanel!=null){
            if(xToggleBtn.isSelected()){
                if ((evt.getModifiers()& ActionEvent.SHIFT_MASK) !=0)
                currViewPanel.resetScaleX(true,false);
                else
                    currViewPanel.updateScaleX(0);
            }
            else {
                 if ((evt.getModifiers()& ActionEvent.SHIFT_MASK) !=0)
                    currViewPanel.resetScaleY(true,false);
                 else
                    currViewPanel.updateScaleY(0);
            }
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_axisResetBtnActionPerformed

    private void axisPlusBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_axisPlusBtnActionPerformed
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if(xToggleBtn.isSelected())
                currViewPanel.updateScaleX(1);
            else
                currViewPanel.updateScaleY(1);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_axisPlusBtnActionPerformed

    private void axisMinBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_axisMinBtnActionPerformed
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if(xToggleBtn.isSelected())
                currViewPanel.updateScaleX(-1);
            else
                currViewPanel.updateScaleY(-1);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_axisMinBtnActionPerformed

    private void axisLeftBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_axisLeftBtnActionPerformed
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if(xToggleBtn.isSelected())
                currViewPanel.updateScaleX(-2);
            else
                currViewPanel.updateScaleY(-2);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_axisLeftBtnActionPerformed

    private void SeriesListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SeriesListKeyPressed
        // TODO add your handling code here:
        if (evt.isActionKey()){
            updateDataInfo();
            currViewPanel.repaint();

        }
    }//GEN-LAST:event_SeriesListKeyPressed

    private void SeriesListClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SeriesListClicked
        // TODO add your handling code here:
        updateDataInfo();
        currViewPanel.repaint();
    }//GEN-LAST:event_SeriesListClicked

    private void SpecTableKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SpecTableKeyPressed
        // TODO add your handling code here:
        if (evt.isActionKey()){
            //updateDataInfo();
            //System.out.println("key");
            updateSweepSelection();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_SpecTableKeyPressed

    private void SpecTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_SpecTableKeyReleased
        // TODO add your handling code here:
        float x,oldKf;
        SpecSweep crSweep;
        int col, row;
        if (evt.getKeyCode()==KeyEvent.VK_ENTER){
            currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
            row=SpecTable.getSelectedRow();
            col=SpecTable.getSelectedColumn();
            x=(float)specTableModel.getValueAt(row, col);
            crSweep=currViewPanel.getSelectedSeries().getCompAtIdx(row);
            
            switch (col){
                case 2:
                    crSweep.addNum(x-crSweep.Zero);
                    crSweep.Zero=x;
                    break;
                case 3:
                    if (x!=0){
                        crSweep.addNum(-crSweep.Zero);
                        crSweep.multNum(x/crSweep.Amp);
                        crSweep.addNum(crSweep.Zero);
                        crSweep.Amp=x;
                    }
                    break;
                case 7:
                    oldKf=crSweep.Kf;
                    crSweep.Kf=x; // will be used for Summ and LeasSqr procedures
                    crSweep.multNum(x/oldKf);
                    break;
            }
            //updateSweepSelection();
            //updateDataInfo();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_SpecTableKeyReleased

    private void SpecTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SpecTableMouseClicked
        // TODO add your handling code here:
        updateSweepSelection();
        currViewPanel.repaint();
    }//GEN-LAST:event_SpecTableMouseClicked

    private void SpecTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_SpecTableMouseReleased
        // TODO add your handling code here:
        updateSweepSelection();
        currViewPanel.repaint();
        
    }//GEN-LAST:event_SpecTableMouseReleased

    private void CompCreate(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompCreate
        // TODO add your handling code here:
        int nP=2000;
        int nS=0;
        SpecSeries buffSpec;
        float xBeg = 0;
        float xEnd=200;
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            nS=currViewPanel.getDataSize();
            if (nS<1){
                currViewPanel.addSeries(nP,null);
                currViewPanel.getSeriesAtIdx(0).reScale(xBeg, xEnd);
                updateSeriesList();
                updateDataInfo();
            }
            buffSpec = currViewPanel.getSelectedSeries();
            buffSpec.createFitComp(currFitPar);
            FitLstSqrBtn.setEnabled(true);
            FitAutoBtn.setEnabled(true);
            updateDataInfo();
            //updateSweepSelection();
            //updateSweepTable(-1);
            //updateSeriesSelection();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompCreate

    private void CompDelete(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompDelete
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if ((evt.getModifiers()&ActionEvent.SHIFT_MASK) !=0 && (evt.getModifiers()&ActionEvent.CTRL_MASK) !=0 ){
                if (JOptionPane.showConfirmDialog(this, currViewPanel.getSelectedSeries().getTitle(), "Delete data series?", JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)
                    System.out.println("delete ser ??");
                
            }
                
            currViewPanel.getSelectedSeries().clearAllComp(false);
            int idx=SpecTable.getSelectedRow();
            if (idx>0){
                currViewPanel.getSelectedSeries().selComp(idx-1, 0);
                SpecTable.changeSelection(idx-1,0,false,false);
            }
            updateDataInfo();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompDelete

    private void CompAlign(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompAlign
        // TODO add your handling code here:
        if (currViewPanel!=null){
            currViewPanel.getSelectedSeries().AlignSweeps();
            //SpecTable.changeSelection(0,0,false,false);
            updateDataInfo();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompAlign

    private void CompAverage(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompAverage
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            currViewPanel.averageSweeps();
            SpecTable.changeSelection(0,0,false,false);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompAverage

    private void CompSum(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompSum
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if ((evt.getModifiers()& ActionEvent.SHIFT_MASK) !=0)
                currViewPanel.getSelectedSeries().AllSum(false, false);
            else
                currViewPanel.getSelectedSeries().AllSum(false, true);
            SpecTable.changeSelection(0,0,false,false);
            updateDataInfo();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompSum

    private void CompSubtrack(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompSubtrack
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            currViewPanel.getSelectedSeries().Subtrack(false);
            SpecTable.changeSelection(0,0,false,false);
            updateDataInfo();
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompSubtrack

    private void CopyInternal(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CopyInternal
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        //if(clbdSpec==null)
            clbdSpec=currViewPanel.getSelectedSeries();
       
    }//GEN-LAST:event_CopyInternal

    private void ParamListKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_ParamListKeyPressed
        // TODO add your handling code here:
        if (evt.isActionKey()){
            updateFitParName();
            

        }
    }//GEN-LAST:event_ParamListKeyPressed

    private void ParamListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ParamListMouseClicked
        // TODO add your handling code here:
        updateFitParName();
    }//GEN-LAST:event_ParamListMouseClicked

    private void CompApply(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_CompApply
        // TODO add your handling code here:
        int i,col,idx;
        SpecSweep crSweep;
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        /*
        row=SpecTable.getSelectedRow();
        col = 2;
        x=(float)specTableModel.getValueAt(row, col);
        System.out.println("zero: "+x);
        col = 3;
        x=(float)specTableModel.getValueAt(row, col);
        System.out.println("amp : "+x);
        int i;
        */
        float[] val = new float[FitParam.nTotPar];
        idx=Integer.parseInt(FitCompTxt.getText());
        if (idx>0){
            i=ParamList.getSelectedIndex();
            currFitPar.setMode(i+FitParam.PARAM+1);
            for (col=1;col<=4;col++){
                for (i=0;i<FitParam.nTotPar;i++)
                        val[i]=(float)FitParamTable.getValueAt(i, col);
                switch(col){
                    case 1: currFitPar.setPar(val);break;
                    case 2: currFitPar.setLimL(val);break;
                    case 3: currFitPar.setLimH(val);break;
                    case 4: currFitPar.setStep(val);break;
                }
            }
            idx+=currViewPanel.getSelectedSeries().BegFitComp-1;
            //System.out.println("idx: "+idx);
            crSweep=currViewPanel.getSelectedSeries().getCompAtIdx(idx);
            crSweep.Reset(currFitPar);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_CompApply

    private void FitParamTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_FitParamTableKeyReleased
        // TODO add your handling code here:
        float x;
        int i;
        
        float[] val = new float[FitParam.nTotPar];
        SpecSweep crSweep;
        int col, row,idx;
        if (evt.getKeyCode()==KeyEvent.VK_ENTER){
            currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
            row=FitParamTable.getSelectedRow();
            col=FitParamTable.getSelectedColumn();
            idx=Integer.parseInt(FitCompTxt.getText());
            i=ParamList.getSelectedIndex();
            currFitPar.setMode(i+FitParam.PARAM+1);
            
            /*for (i=0;i<FitParam.nTotPar;i++)
                val[i]=(float)FitParamTable.getValueAt(i, col);
            */
            
            switch(col){
                case 1:
                    currFitPar.getPar(val);
                    val[row]=(float)FitParamTable.getValueAt(row, col);
                    currFitPar.setPar(val);
                    break;
                case 2: 
                    currFitPar.getLimL(val);
                    val[row]=(float)FitParamTable.getValueAt(row, col);
                    currFitPar.setLimL(val);
                    break;
                case 3: 
                    currFitPar.getLimH(val);
                    val[row]=(float)FitParamTable.getValueAt(row, col);
                    currFitPar.setLimH(val);
                    break;
                case 4: 
                    currFitPar.getStep(val);
                    val[row]=(float)FitParamTable.getValueAt(row, col);
                    currFitPar.setStep(val);
                    break;
            }
            if (idx>0){
                idx+=currViewPanel.getSelectedSeries().BegFitComp-1;
                //System.out.println("idx2: "+idx);
                crSweep=currViewPanel.getSelectedSeries().getCompAtIdx(idx);
                crSweep.Reset(currFitPar);
                currViewPanel.repaint();
            }
        }
    }//GEN-LAST:event_FitParamTableKeyReleased

    private void axisRightBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_axisRightBtnActionPerformed
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            if(xToggleBtn.isSelected())
            currViewPanel.updateScaleX(2);
            else
            currViewPanel.updateScaleY(2);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_axisRightBtnActionPerformed

    private void ToolsBackgSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToolsBackgSubActionPerformed
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            int idx=SpecTable.getSelectedRow();
            if (currViewPanel.getSelectedSeries().backgrSubtract(idx)){
                updateDataInfo();
                currViewPanel.repaint();
            } else
                JOptionPane.showMessageDialog(this, "no backround line is selected");
        }
    }//GEN-LAST:event_ToolsBackgSubActionPerformed

    private void DataHist(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataHist
        
        int selRow=SpecTable.getSelectedRow();
            if (currViewPanel.calculatePDHist(selRow)){
                updateSeriesList(); //needed becase new series has been created
                updateDataInfo();
                currViewPanel.resetScaleY(false,false);
                currViewPanel.repaint();
            }
            
    }//GEN-LAST:event_DataHist

    private void ToolsSmooth(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ToolsSmooth
        // TODO add your handling code here:
         currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            int idx=SpecTable.getSelectedRow();
            currViewPanel.getSelectedSeries().Smooth(idx,(float) 0.01);
            updateDataInfo();
            currViewPanel.resetScaleY(false,true);
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_ToolsSmooth

    private void DataAC(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataAC
        // TODO add your handling code here:        
            if (currViewPanel.calculateAC()){
                updateSeriesList(); //needed becase new series has been created
                updateDataInfo();
                currViewPanel.resetScaleY(false,false);
                currViewPanel.repaint();
            }
    }//GEN-LAST:event_DataAC

    private void FitLeastSquare(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FitLeastSquare

        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            //currViewPanel.getSelectedSeries().Subtrack(false);
            currViewPanel.getSelectedSeries().setTagComp(SpecTable.getSelectedRow());
            float accur = currViewPanel.getSelectedSeries().LeastSqr(0);
            FitAccurTxt.setText(String.format("Accuracy: %2.2f %s", accur*100,'%'));
            FitAccurTxt.setEnabled(true);
            FitAccurTxt.repaint();
            //System.out.println("tag = "+currViewPanel.getSelectedSeries().TagComp + " begFit = " + currViewPanel.getSelectedSeries().BegFitComp);
            updateDataInfo();
            
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_FitLeastSquare

    private void FitAuto(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_FitAuto

        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            //currViewPanel.getSelectedSeries().Subtrack(false);
            currViewPanel.getSelectedSeries().setTagComp(SpecTable.getSelectedRow());
            currViewPanel.getSelectedSeries().AutoFit();
            int cyc = SpecSeries.fitCrCycle+1;
            int stp = SpecSeries.fitTotStep+1;
            float accur = currViewPanel.getSelectedSeries().fitAccuracy;
            FitAccurTxt.setText(String.format("Accuracy: %2.2f %s cycle %d step %d", accur*100,'%',cyc,stp));
            FitAccurTxt.setEnabled(true);
            FitAccurTxt.repaint();
            //System.out.println("tag = "+currViewPanel.getSelectedSeries().TagComp + " begFit = " + currViewPanel.getSelectedSeries().BegFitComp);
            updateDataInfo();
        
            currViewPanel.repaint();
        }
    }//GEN-LAST:event_FitAuto

    private void DataFindPeaks(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataFindPeaks
        // TODO add your handling code here:
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        if (currViewPanel!=null){
            //currViewPanel.getSelectedSeries().Subtrack(false);
            currViewPanel.getSelectedSeries().setTagComp(SpecTable.getSelectedRow());
            float accur = currViewPanel.getSelectedSeries().FindPeaks(-1);
            if (accur > 0) {
                FitAccurTxt.setText(String.format("Accuracy: %2.2f %s", accur*100,'%'));
                FitAccurTxt.setEnabled(true);
                FitAccurTxt.repaint();
            }
            FitLstSqrBtn.setEnabled(true);
            FitAutoBtn.setEnabled(true);
            updateDataInfo();      
            currViewPanel.repaint();
        }
        
    }//GEN-LAST:event_DataFindPeaks

    private void DataPeaksAnalysis(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataPeaksAnalysis
        currViewPanel = (SpecPanel)MainTabViewPane.getSelectedComponent();
        
            if (currViewPanel.detectPeaks()){
                updateSeriesList(); //needed becase new series has been created
                updateDataInfo();
                currViewPanel.resetScaleY(false,false);
                currViewPanel.repaint();
                FitLstSqrBtn.setEnabled(true);
                FitAutoBtn.setEnabled(true);
            }
        
    }//GEN-LAST:event_DataPeaksAnalysis

    private void fitSetOKBtn(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fitSetOKBtn
        
        SpecSeries.fitAccurHRej = Float.parseFloat(fitSetAccurHighTxt.getText())/100;
        SpecSeries.fitAccurH = Float.parseFloat(fitSetAccurLowTxt.getText())/100;
        SpecSeries.fitNCycleMax = Integer.parseInt(fitSetCycleTxt.getText());
        SpecSeries.fitNStepMax = Integer.parseInt(fitSetStepTxt.getText());
        SpecSeries.findSigXlow = Float.parseFloat(fitSetXlowTxt.getText());
        SpecSeries.findSigXhigh = Float.parseFloat(fitSetXhighTxt.getText());
        SpecSeries.findSigYlow = Float.parseFloat(fitSetYlowTxt.getText());
        SpecSeries.findSigInw = fitSetInward.isSelected();
        SpecSeries.findSigGauss = fitSetGauss.isSelected();
        FitSetDlg.setVisible(false);
        FitSetDlg.toBack();
    }//GEN-LAST:event_fitSetOKBtn

    private void fitSetCancelBtn(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fitSetCancelBtn
        
        FitSetDlg.setVisible(false);
        FitSetDlg.toBack();
        FitSetDlg.dispose();
    }//GEN-LAST:event_fitSetCancelBtn

    private void DataFitSettings(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_DataFitSettings
        
        fitSetAccurHighTxt.setText(String.format("%2.2f", 100*SpecSeries.fitAccurHRej));
        fitSetAccurLowTxt.setText(String.format("%2.2f", 100*SpecSeries.fitAccurH));
        fitSetCycleTxt.setText(String.format("%d",SpecSeries.fitNCycleMax));
        fitSetStepTxt.setText(String.format("%d",SpecSeries.fitNStepMax));
        fitSetXlowTxt.setText(String.format("%2.2f",SpecSeries.findSigXlow));
        fitSetXhighTxt.setText(String.format("%2.2f",SpecSeries.findSigXhigh));
        fitSetYlowTxt.setText(String.format("%2.2f",SpecSeries.findSigYlow));
        FitSetDlg.setVisible(true);
        FitSetDlg.toFront();
        if(SpecSeries.findSigInw) fitSetInward.doClick();
            else 
                if(SpecSeries.findSigGauss) fitSetGauss.doClick();
                    else fitSetOutward.doClick();
    }//GEN-LAST:event_DataFitSettings

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(NeuroSpex.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(NeuroSpex.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(NeuroSpex.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(NeuroSpex.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new NeuroSpex().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton ComDelBtn;
    private javax.swing.JButton CompAlignBtn;
    private javax.swing.JButton CompApplyBtn;
    private javax.swing.JButton CompAveBtn;
    private javax.swing.JButton CompCreateBtn;
    private javax.swing.JButton CompSubtrackBtn;
    private javax.swing.JButton CompSumBtn;
    private javax.swing.JCheckBoxMenuItem CopyAmp;
    private javax.swing.JMenuItem CopyInternal;
    private javax.swing.JMenuItem CopyRes;
    private javax.swing.JMenu CopyResMenu;
    private javax.swing.JMenuItem CopySweep;
    private javax.swing.JCheckBoxMenuItem CopyTDec;
    private javax.swing.JCheckBoxMenuItem CopyTRise;
    private javax.swing.JCheckBoxMenuItem CopyTotAmp;
    private javax.swing.JMenuItem DataAC;
    private javax.swing.JMenuItem DataFindPeak;
    private javax.swing.JMenuItem DataHist;
    private javax.swing.JMenu DataMenu;
    private javax.swing.JMenuItem DataPeaksAnalysis;
    private javax.swing.JMenu DataSearch;
    private javax.swing.JMenuItem DataSlope;
    private javax.swing.JMenu EditMenu;
    private javax.swing.JMenuItem FileClose;
    private javax.swing.JMenuItem FileCloseAll;
    private javax.swing.JMenu FileMenu;
    private javax.swing.JMenuItem FileNew;
    private javax.swing.JMenuItem FileOpen;
    private javax.swing.JMenuItem FileSave;
    private javax.swing.JLabel FitAccurTxt;
    private javax.swing.JButton FitAutoBtn;
    private javax.swing.JLabel FitCompTxt;
    private javax.swing.JButton FitLstSqrBtn;
    private javax.swing.JTable FitParamTable;
    private javax.swing.JDialog FitSetDlg;
    private javax.swing.JMenuItem FitSettings;
    private javax.swing.JMenuItem InsertSweep;
    private javax.swing.JTabbedPane MainTabViewPane;
    private javax.swing.JList<String> ParamList;
    private javax.swing.JMenuItem PasteSweep;
    private javax.swing.JList<String> SeriesList;
    private javax.swing.JTable SpecTable;
    private javax.swing.JLabel SpecTitleTxt;
    private javax.swing.JMenuItem ToolsBackgSub;
    private javax.swing.JMenuItem ToolsClip;
    private javax.swing.JMenu ToolsMenu;
    private javax.swing.JMenuItem ToolsSmooth;
    private javax.swing.JButton axisLeftBtn;
    private javax.swing.JButton axisMinBtn;
    private javax.swing.JButton axisPlusBtn;
    private javax.swing.JButton axisResetBtn;
    private javax.swing.JButton axisRightBtn;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JTextField fitSetAccurHighTxt;
    private javax.swing.JTextField fitSetAccurLowTxt;
    private javax.swing.ButtonGroup fitSetBtnGroup1;
    private javax.swing.JButton fitSetCancelBtn;
    private javax.swing.JTextField fitSetCycleTxt;
    private javax.swing.JRadioButton fitSetGauss;
    private javax.swing.JRadioButton fitSetInward;
    private javax.swing.JButton fitSetOKBtn;
    private javax.swing.JRadioButton fitSetOutward;
    private javax.swing.JTextField fitSetStepTxt;
    private javax.swing.JTextField fitSetXhighTxt;
    private javax.swing.JTextField fitSetXlowTxt;
    private javax.swing.JTextField fitSetYlowTxt;
    private javax.swing.JLabel frameLabel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JMenuBar jMenuBar1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JToggleButton xToggleBtn;
    private javax.swing.ButtonGroup xyBtnGroup;
    private javax.swing.JToggleButton yToggleBtn;
    // End of variables declaration//GEN-END:variables
}
