package com.cyberon.dspotterutility;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.content.Context;
import android.widget.Toast;

import com.cyberon.engine.DSpotter;
import com.cyberon.engine.VerInfo;

/**
 * The DSpotterRecog class is the main SDK class. It recognizes voice data and
 * recording with a audio record object.
 */
public class DSpotterRecog {
    // Error code
    public static final int DSPOTTER_RECOG_SUCCESS = 0;
    public static final int DSPOTTER_RECOG_ERR_NoHandler = -201;
    public static final int DSPOTTER_RECOG_ERR_Initialize_Fail = -202;
    public static final int DSPOTTER_RECOG_ERR_SetParamFail = -203;
    public static final int DSPOTTER_RECOG_ERR_Is_Recognizing = -204;

    private static final int FRAME = 30;

    private Thread m_oRecordThread = null;
    private Thread m_oRecogThread = null;
    private long[] m_lDSpotterHandle = null;
    private Context m_oContext = null;
    private boolean m_bStop = false;
    private boolean m_bDumpWave = false;
    private volatile ConcurrentLinkedQueue<short[]> m_oSampleItems = new ConcurrentLinkedQueue<short[]>();
    private String m_sDataPath = null;

    private int[] m_naCmdIdx = new int[1];
    private String[] m_straResult = new String[1];
    private int[] m_naWordDura = new int[1];
    private int[] m_naEndSil = new int[1];
    private int[] m_naNetworkLatency = new int[1];
    private int[] m_naConfi = new int[1];
    private int[] m_naSGDiff = new int[1];
    private int[] m_naFIL = new int[1];

    private int m_nIDMapping;

    private boolean m_bAGCEnable = false;
    private boolean m_bAGCSetCallback = false;
    public static volatile ConcurrentLinkedQueue<short[]> m_oAGCDataItems = new ConcurrentLinkedQueue<short[]>();

    /**
     * An instance of DSpotterRecogStatusListener interface in which callback
     * function onDSpotterRecogStatusChanged is implemented and invoked when the
     * DSpotter recognition status is changed.
     */
    private DSpotterRecogStatusListener mRecogStatusListener = null;

    /**
     * Initialize DSpotterRecog.
     *
     * @param objContext     [in] The itself of Activity.
     * @param strPackBinFile [in] The full path of pack bin file.
     * @param strLicenseFile [in] The full path of license file.
     * @param strServerFile  [in] The full path of server file. Be used to official version.
     * @param naErr          [out] The error code.
     * @return If succeed, return DSPOTTER_RECOG_SUCCESS. Otherwise return error code.
     */
    public synchronized int initWithFiles(Context objContext,
                                          String strPackBinFile,
                                          String strLicenseFile,
                                          String strServerFile, int[] naErr) {
        if (m_lDSpotterHandle != null)
            release();

        m_lDSpotterHandle = new long[1];
        m_oContext = objContext;

        // Initialize engine
        m_lDSpotterHandle[0] = DSpotter.DSpotterInitMultiWithPackBin(strPackBinFile, null, 500, null, naErr, strLicenseFile, strServerFile, objContext);
        if (m_lDSpotterHandle[0] == 0) {
            m_lDSpotterHandle = null;
            return DSPOTTER_RECOG_ERR_Initialize_Fail;
        }

        // Check license
//        if (!isCheckLicenseOK(strLicenseFile))
//            Toast.makeText(m_oContext, "It's trial version!",
//                    Toast.LENGTH_SHORT).show();

        return DSPOTTER_RECOG_SUCCESS;
    }

    /**
     * Release DSpotterRecog.
     *
     * @return If succeed, return DSPOTTER_RECOG_SUCCESS. Otherwise return error code.
     */
    public synchronized int release() {
        int nRet;
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        if (m_lDSpotterHandle[0] != 0) {
            nRet = DSpotter.DSpotterRelease(m_lDSpotterHandle[0]);
            if (nRet == DSpotter.DSPOTTER_SUCCESS) {
                m_lDSpotterHandle = null;
                return DSPOTTER_RECOG_SUCCESS;
            } else {
                return nRet;
            }
        }
        return DSPOTTER_RECOG_ERR_NoHandler;
    }

    /**
     * Set listener for recognition status.
     *
     * @param oRecogStatusListener [in] Set the listener for recognition status.
     * @return If succeed, return DSPOTTER_RECOG_SUCCESS. Otherwise return error code.
     */
    public synchronized int setListener(
            DSpotterRecogStatusListener oRecogStatusListener) {
        if (oRecogStatusListener == null)
            return DSPOTTER_RECOG_ERR_SetParamFail;

        mRecogStatusListener = oRecogStatusListener;

        return DSPOTTER_RECOG_SUCCESS;
    }

    /**
     * Set dump wave flag.
     *
     * @param bSaveWave   [in] true indicates save recording data to data path. Please
     *                    set false.
     * @param strDataPath [in] Set path for save recording data.(When set bSaveWave to
     *                    true)
     */
    public synchronized void setDumpWave(boolean bSaveWave, String strDataPath) {
        m_bDumpWave = bSaveWave;

        if (!bSaveWave)
            return;

        if (strDataPath.charAt(strDataPath.length() - 1) == '/')
            m_sDataPath = strDataPath.substring(0, strDataPath.length() - 1);
        else
            m_sDataPath = strDataPath;
    }

    /**
     * Start audio record process.
     */
    private synchronized void startRecord() {
        if (m_oRecordThread != null && m_oRecordThread.isAlive()) {
            return;
        }

        m_oSampleItems.clear();
        m_oRecordThread = new Thread(m_oAudioRecord);
        m_oRecordThread.start();
    }

    /**
     * Stop audio record process.
     */
    private synchronized void stopRecord() {
        try {
            // Waiting for recognition thread to end
            if (m_oRecordThread != null) {
                m_oRecordThread.join();
                m_oRecordThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }

    /**
     * Start DSpotter recognition process.
     *
     * @param bSync [in] Set synchronous flag.
     * @return If succeed, return DSPOTTER_RECOG_SUCCESS. Otherwise return error code.
     */
    public int start(boolean bSync) {
        synchronized (this) {
            if (m_lDSpotterHandle == null)
                return DSPOTTER_RECOG_ERR_NoHandler;

            if (m_oRecogThread != null && m_oRecogThread.isAlive())
                return DSPOTTER_RECOG_ERR_Is_Recognizing;

            startRecord();
            m_bStop = false;
            m_oRecogThread = new Thread(m_oDSpotterRecog);
            m_oRecogThread.start();

            if (m_bAGCEnable && m_bAGCSetCallback) {
                m_oAGCDataItems.clear();
                new Thread(m_oAGCDataRunnable).start();
            }
        }

        if (bSync) {
            try {
                // Waiting for recognition thread to end
                if (m_oRecogThread != null) {
                    m_oRecogThread.join();
                    m_oRecogThread = null;
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return DSPOTTER_RECOG_SUCCESS;
    }

    /**
     * Stop DSpotter recognition process.
     */
    public void stop() {
        synchronized (this) {
            if (m_oRecogThread == null)
                return;
            m_bStop = true;
            stopRecord();
        }

        try {
            // Waiting for recognition thread to end
            if (m_oRecogThread != null) {
                m_oRecogThread.join();
                m_oRecogThread = null;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Determine whether the DSpotter recognition is running.
     *
     * @return If the recognition is running, return true.
     */
    public synchronized boolean isRecog() {
        if (m_oRecogThread != null)
            return m_oRecogThread.isAlive();
        else
            return false;
    }

    /**
     * Get recognition result.
     *
     * @param naCmdIdx         [out] The command index of recognition result.
     * @param straResult       [out] The recognition result.
     * @param naWordDura       [out] The duration of the result in number of samples.
     * @param naEndSil         [out] The ending silence length in number of samples.
     * @param naNetworkLatency [out] The model delay length in number of samples.
     * @param naConfi          [out] The score of confidence.
     * @param naSGDiff         [out] The score of SG difference.
     * @param naFIL            [out] The score of FIL.
     */
    public synchronized void getResult(int[] naCmdIdx, String[] straResult, int[] naWordDura, int[] naEndSil, int[] naNetworkLatency, int[] naConfi, int[] naSGDiff, int[] naFIL) {
        if (m_lDSpotterHandle == null)
            return;

        if (naCmdIdx != null)
            naCmdIdx[0] = m_naCmdIdx[0];
        if (straResult != null)
            straResult[0] = m_straResult[0];
        if (naWordDura != null)
            naWordDura[0] = m_naWordDura[0];
        if (naEndSil != null)
            naEndSil[0] = m_naEndSil[0];
        if (naNetworkLatency != null)
            naNetworkLatency[0] = m_naNetworkLatency[0];
        if (naConfi != null)
            naConfi[0] = m_naConfi[0];
        if (naSGDiff != null)
            naSGDiff[0] = m_naSGDiff[0];
        if (naFIL != null)
            naFIL[0] = m_naFIL[0];
    }

    /**
     * Get trigger word(s).
     *
     * @return If succeed, return an array of trigger word(s). Otherwise return null.
     */
    public synchronized String[] getTriggerWord() {
        ArrayList<String> oTriggerWordList = new ArrayList<String>();
        String[] straCommand = new String[1];

        if (m_lDSpotterHandle == null)
            return null;

        int nCommandNumber = DSpotter
                .DSpotterGetCommandNumber(m_lDSpotterHandle[0]);

        for (int i = 0; i < nCommandNumber; i++) {
            DSpotter.DSpotterGetUTF8Command(m_lDSpotterHandle[0], i, straCommand);

            if (!oTriggerWordList.contains(straCommand[0]))
                oTriggerWordList.add(straCommand[0]);
        }

        oTriggerWordList.trimToSize();
        return oTriggerWordList.toArray(new String[0]);
    }

    /**
     * Set NBest enabled flag.
     *
     * @param bEnable [in] true or false.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setEnableNBest(boolean bEnable) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetEnableNBest(m_lDSpotterHandle[0], bEnable);
    }

    /**
     * Get NBest recognition results.
     *
     * @param naCmdIdx   [out] An array for saving command index of NBest recognition results. The length of array must be nMaxNBest.
     * @param straResult [out] An array for saving NBest recognition results.
     * @param naScore    [out] An array for saving recognition score of NBest recognition results. The length of array must be nMaxNBest.
     * @param nMaxNBest  [in] The number of NBest recognition results you want to get.
     * @return If succeed, return the number of NBest recognition results engine output. Otherwise return error code.
     * Remark: Caller should make sure that the length of array must be greater than the number of NBest recognition results engine output.
     */
    public synchronized int getNBestResultScore(int[] naCmdIdx, String[] straResult, int[] naScore, int nMaxNBest) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetNBestUTF8ResultScore(m_lDSpotterHandle[0], naCmdIdx, straResult, naScore, nMaxNBest);
    }

    /**
     * Get command energy.
     *
     * @return If succeed, return command energy. Otherwise return error code.
     */
    public synchronized int getCmdEnergy() {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetCmdEnergy(m_lDSpotterHandle[0]);
    }

    /**
     * Set result ID mapping.
     *
     * @param strIDMappingPackBinFile [in] The full path of ID mapping pack bin file.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setResultIDMapping(String strIDMappingPackBinFile) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetResultIDMapping(m_lDSpotterHandle[0], strIDMappingPackBinFile);
    }

    /**
     * Get result ID mapping.
     *
     * @return If succeed, return result ID mapping. Otherwise return error code.
     */
    public synchronized int getResultIDMapping() {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return m_nIDMapping;
    }

    /**
     * Set rejection level.
     *
     * @param nRejectionLevel [in] The range is [-100, 100]. Higher rejection level will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     * Deprecated and replaced it with setConfiReward.
     */
    public synchronized int setRejectionLevel(int nRejectionLevel) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetRejectionLevel(m_lDSpotterHandle[0],
                nRejectionLevel);
    }

    /**
     * Set confidence reward.
     *
     * @param nReward [in] The range is [-100, 100]. Lower reward will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setConfiReward(int nReward) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetConfiReward(m_lDSpotterHandle[0],
                nReward);
    }

    /**
     * Set SG level.
     *
     * @param nSgLevel [in] The range is [-100, 100]. Higher rejection level will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     * Deprecated and replaced it with setSGDiffReward.
     */
    public synchronized int setSgLevel(int nSgLevel) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetSgLevel(m_lDSpotterHandle[0],
                nSgLevel);
    }

    /**
     * Set SG difference reward.
     *
     * @param nReward [in] The range is [-100, 100]. Lower reward will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setSGDiffReward(int nReward) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetSGDiffReward(m_lDSpotterHandle[0],
                nReward);
    }

    /**
     * Set FIL level.
     *
     * @param nFilLevel [in] The range is [-100, 100]. Higher rejection level will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     * Deprecated API.
     */
    public synchronized int setFilLevel(int nFilLevel) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetFilLevel(m_lDSpotterHandle[0],
                nFilLevel);
    }

    /**
     * Set response time.
     *
     * @param nResponseTime [in] The range is [0, 16]. Lower value will make the engine quicker to return a result. One unit is 0.03s, and the default is 8(0.24s).
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     * Deprecated and replaced it with setEndSil.
     */
    public synchronized int setResponseTime(int nResponseTime) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetResponseTime(m_lDSpotterHandle[0], nResponseTime);
    }

    /**
     * Set ending silence.
     *
     * @param nEndSil [in] The range is [0, 16]. Lower value will make the engine quicker to return a result. One unit is 0.03s, and the default is 8(0.24s).
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setEndSil(int nEndSil) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetEndSil(m_lDSpotterHandle[0], nEndSil);
    }

    /**
     * Set response time of specific command.
     *
     * @param nCmdIdx       [in] The command index.
     * @param nResponseTime [in] The range is [0, 16]. Lower value will make the engine quicker to return a result. One unit is 0.03s, and the default is 8(0.24s).
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     * Deprecated and replaced it with setCmdEndSil.
     */
    public synchronized int setCmdResponseTime(int nCmdIdx, int nResponseTime) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetCmdResponseTime(m_lDSpotterHandle[0], nCmdIdx, nResponseTime);
    }

    /**
     * Set ending silence of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param nEndSil [in] The range is [0, 16]. Lower value will make the engine quicker to return a result. One unit is 0.03s, and the default is 8(0.24s).
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setCmdEndSil(int nCmdIdx, int nEndSil) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetCmdEndSil(m_lDSpotterHandle[0], nCmdIdx, nEndSil);
    }

    /**
     * Set energy threshold.
     *
     * @param nEnergyTH [in] The range is [0, 32767], and higher energy threshold will result in less false triggers.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setEnergyTH(int nEnergyTH) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetEnergyTH(m_lDSpotterHandle[0], nEnergyTH);
    }

    /**
     * Set reward of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param nReward [in] The range is [-100, 100], and lower command reward will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     * Deprecated and replaced it with setCmdConfiReward.
     */
    public synchronized int setCmdReward(int nCmdIdx, int nReward) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetCmdReward(m_lDSpotterHandle[0], nCmdIdx, nReward);
    }

    /**
     * Set confidence reward of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param nReward [in] The range is [-100, 100], and lower command reward will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setCmdConfiReward(int nCmdIdx, int nReward) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetCmdConfiReward(m_lDSpotterHandle[0], nCmdIdx, nReward);
    }

    /**
     * Set SG difference reward of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param nReward [in] The range is [-100, 100], and lower command reward will make the engine more "picky" to return a result.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int setCmdSGDiffReward(int nCmdIdx, int nReward) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterSetCmdSGDiffReward(m_lDSpotterHandle[0], nCmdIdx, nReward);
    }

    /**
     * Get rejection level.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return rejection level. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated and replaced it with getConfiReward.
     */
    public synchronized int getRejectionLevel(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetRejectionLevel(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get confidence reward.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return confidence reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getConfiReward(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetConfiReward(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get SG level.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return SG level. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated and replaced it with getSGDiffReward.
     */
    public synchronized int getSgLevel(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetSgLevel(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get SG difference reward.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return SG difference reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getSGDiffReward(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetSGDiffReward(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get FIL level.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return FIL level. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated API.
     */
    public synchronized int getFilLevel(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetFilLevel(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get response time of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param naErr   [out] The return code.
     * @return If succeed, return response time. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated and replaced it with getCmdEndSil.
     */
    public synchronized int getCmdResponseTime(int nCmdIdx, int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetCmdResponseTime(m_lDSpotterHandle[0], nCmdIdx, naErr);
    }

    /**
     * Get ending silence of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param naErr   [out] The return code.
     * @return If succeed, return ending silence. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getCmdEndSil(int nCmdIdx, int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetCmdEndSil(m_lDSpotterHandle[0], nCmdIdx, naErr);
    }

    /**
     * Get energy threshold.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return energy threshold. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getEnergyTH(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetEnergyTH(m_lDSpotterHandle[0], naErr);
    }

    /**
     * Get reward of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param naErr   [out] The return code.
     * @return If succeed, return reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated and replaced it with getCmdConfiReward.
     */
    public synchronized int getCmdReward(int nCmdIdx, int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetCmdReward(m_lDSpotterHandle[0], nCmdIdx, naErr);
    }

    /**
     * Get confidence reward of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param naErr   [out] The return code.
     * @return If succeed, return confidence reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getCmdConfiReward(int nCmdIdx, int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetCmdConfiReward(m_lDSpotterHandle[0], nCmdIdx, naErr);
    }

    /**
     * Get SG difference reward of specific command.
     *
     * @param nCmdIdx [in] The command index.
     * @param naErr   [out] The return code.
     * @return If succeed, return SG difference reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getCmdSGDiffReward(int nCmdIdx, int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetCmdSGDiffReward(m_lDSpotterHandle[0], nCmdIdx, naErr);
    }

    /**
     * Get built-in rejection level of model.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return rejection level. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated and replaced it with getModelConfiReward.
     */
    public synchronized int getModelRejectionLevel(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetModelRejectionLevel(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get built-in confidence reward of model.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return confidence reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getModelConfiReward(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetModelConfiReward(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get built-in SG level of model.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return SG level. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated and replaced it with getModelSGDiffReward.
     */
    public synchronized int getModelSgLevel(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetModelSgLevel(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get built-in SG difference reward of model.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return SG difference reward. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     */
    public synchronized int getModelSGDiffReward(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetModelSGDiffReward(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Get built-in FIL level of model.
     *
     * @param naErr [out] The return code.
     * @return If succeed, return FIL level. The returned value is valid only when output value of naErr is DSPOTTER_SUCCESS.
     * Deprecated API.
     */
    public synchronized int getModelFilLevel(int[] naErr) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterGetModelFilLevel(m_lDSpotterHandle[0],
                naErr);
    }

    /**
     * Enable AGC.
     *
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int AGCEnable() {
        int nRet;

        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        nRet = DSpotter.DSpotterAGCEnable(m_lDSpotterHandle[0]);

        if (nRet == DSpotter.DSPOTTER_SUCCESS)
            m_bAGCEnable = true;

        return nRet;
    }

    /**
     * Disable AGC.
     *
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int AGCDisable() {
        int nRet;

        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        nRet = DSpotter.DSpotterAGCDisable(m_lDSpotterHandle[0]);

        if (nRet == DSpotter.DSPOTTER_SUCCESS)
            m_bAGCEnable = false;

        return nRet;
    }

    /**
     * Set the upper bound of AGC gain.
     *
     * @param fMaxGain [in] The range is [1, 32]. The default is 32.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int AGCSetMaxGain(float fMaxGain) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterAGCSetMaxGain(m_lDSpotterHandle[0], fMaxGain);
    }

    /**
     * Set gain increment threshold of AGC.
     *
     * @param sLowerThrd [in] The range is [0, 10000]. The default is 5000.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int AGCSetIncGainThrd(short sLowerThrd) {
        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        return DSpotter.DSpotterAGCSetIncGainThrd(m_lDSpotterHandle[0], sLowerThrd);
    }

    /**
     * Set callback which is used to obtain audio data that is after AGC processing.
     *
     * @param bSetCallback [in] Enable or disable AGC callback.
     * @return If succeed, return DSPOTTER_SUCCESS. Otherwise return error code.
     */
    public synchronized int AGCSetCallback(boolean bSetCallback) {
        int nRet;

        if (m_lDSpotterHandle == null)
            return DSPOTTER_RECOG_ERR_NoHandler;

        nRet = DSpotter.DSpotterAGCSetCallback(m_lDSpotterHandle[0], bSetCallback);

        if (nRet == DSpotter.DSPOTTER_SUCCESS)
            m_bAGCSetCallback = bSetCallback;

        return nRet;
    }

    /**
     * AGC data process.
     */
    private Runnable m_oAGCDataRunnable = new Runnable() {
        @Override
        public void run() {
            WaveFile oWaveFile = null;
            int nSampleRate;

            if (m_bDumpWave) {
                nSampleRate = DSpotter.DSpotterGetSampleRate(m_lDSpotterHandle[0]);

                Date oCurrDateTime = new Date();
                SimpleDateFormat oDateFormat = new SimpleDateFormat(
                        "yyyyMMdd_HHmmss", Locale.TAIWAN);

                String strRecordFile = m_sDataPath
                        + "/"
                        + String.format("DSpotterAGC_%s.wav",
                        oDateFormat.format(oCurrDateTime));
                try {
                    oWaveFile = new WaveFile(m_oContext, strRecordFile);
                    oWaveFile.setFormat(16, 1, nSampleRate);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }


            while (!m_bStop) {
                if (m_oAGCDataItems.peek() == null) {
                    ToolKit.sleep(FRAME / 2);
                    continue;
                }

                try {
                    if (oWaveFile != null)
                        oWaveFile.writeData(m_oAGCDataItems.poll());
                    else
                        m_oAGCDataItems.poll();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            try {
                while (m_oAGCDataItems.peek() != null) {
                    if (oWaveFile != null)
                        oWaveFile.writeData(m_oAGCDataItems.poll());
                }

                if (oWaveFile != null)
                    oWaveFile.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    /**
     * Audio record process.
     */
    private Runnable m_oAudioRecord = new Runnable() {

        @Override
        public void run() {
            WaveRecord oWaveRecord = new WaveRecord();
            WaveFile oWaveFile = null;
            short[] saSampleData = null;
            int nSampleRate;

            android.os.Process
                    .setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

            nSampleRate = DSpotter.DSpotterGetSampleRate(m_lDSpotterHandle[0]);

            int nBufferSizeInByte = (Short.SIZE / 8) * FRAME * nSampleRate
                    / 1000; // 30 milliseconds buffer
            oWaveRecord.initialize(1, 16, nSampleRate, nBufferSizeInByte, 40); // Total
            // 30*40
            // =
            // 1200
            // milliseconds
            // buffer

            if (!oWaveRecord.isInitOK()) {
                oWaveRecord.initialize(1, 16, nSampleRate, nBufferSizeInByte,
                        25); // Total
                // 40*25
                // =
                // 1000
                // milliseconds
                // buffer
                if (!oWaveRecord.isInitOK()) {
                    if (mRecogStatusListener != null)
                        mRecogStatusListener
                                .onDSpotterRecogStatusChanged(DSpotterStatus.STATUS_RECORDER_INITIALIZE_FAIL);
                    return;
                }
            }

            Date oCurrDateTime = new Date();
            SimpleDateFormat oDateFormat = new SimpleDateFormat(
                    "yyyyMMdd_HHmmss", Locale.TAIWAN);

            // Dump wave file
            if (m_bDumpWave) {
                String strRecordFile = m_sDataPath
                        + "/"
                        + String.format("DSpotter_%s.wav",
                        oDateFormat.format(oCurrDateTime));
                try {
                    oWaveFile = new WaveFile(m_oContext, strRecordFile);
                    oWaveFile.setFormat(16, 1, nSampleRate);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            // Start Recorder.
            oWaveRecord.start();

            while (!m_bStop) {
                if ((saSampleData = oWaveRecord.getShortWaveClone()) == null) {
                    if (mRecogStatusListener != null)
                        mRecogStatusListener
                                .onDSpotterRecogStatusChanged(DSpotterStatus.STATUS_RECORD_FAIL);
                    break;
                }

                if (oWaveFile != null) {
                    try {
                        oWaveFile.writeData(saSampleData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                m_oSampleItems.add(saSampleData);
            }

            if (oWaveFile != null)
                try {
                    oWaveFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            oWaveRecord.stop();
            oWaveRecord.release();
        }

    };

    /**
     * Recognizer process.
     */
    private Runnable m_oDSpotterRecog = new Runnable() {
        @Override
        public void run() {
            int nRet;
            short[] saSampleData;

            android.os.Process
                    .setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO);

            if (mRecogStatusListener != null)
                mRecogStatusListener
                        .onDSpotterRecogStatusChanged(DSpotterStatus.STATUS_RECOGNITION_START);

            // Start recognizing
            nRet = DSpotter.DSpotterReset(m_lDSpotterHandle[0]);
            if (nRet != DSpotter.DSPOTTER_SUCCESS) {
                if (mRecogStatusListener != null)
                    mRecogStatusListener
                            .onDSpotterRecogStatusChanged(DSpotterStatus.STATUS_RECOGNITION_FAIL);
                return;
            }

            while (true) {
                // User cancel
                if (m_bStop) {
                    if (mRecogStatusListener != null)
                        mRecogStatusListener
                                .onDSpotterRecogStatusChanged(DSpotterStatus.STATUS_RECOGNITION_ABORT);
                    break;
                }

                // Get sample
                if (m_oSampleItems.peek() == null) {
                    if (!m_oRecordThread.isAlive())
                        break;

                    ToolKit.sleep(FRAME / 2);
                    continue;
                }

                saSampleData = m_oSampleItems.poll();
                nRet = DSpotter.DSpotterAddSample(m_lDSpotterHandle[0],
                        saSampleData);

                if (nRet == DSpotter.DSPOTTER_SUCCESS) {
                    DSpotter.DSpotterGetUTF8Result(m_lDSpotterHandle[0], m_naCmdIdx, m_straResult, m_naWordDura, m_naEndSil, m_naNetworkLatency, m_naConfi, m_naSGDiff, m_naFIL);
                    m_nIDMapping = DSpotter.DSpotterGetResultIDMapping(m_lDSpotterHandle[0]);

                    // Send message to recognize OK
                    if (mRecogStatusListener != null)
                        mRecogStatusListener
                                .onDSpotterRecogStatusChanged(DSpotterStatus.STATUS_RECOGNITION_OK);

                    DSpotter.DSpotterReset(m_lDSpotterHandle[0]);
                }
            }
        }
    };

    /**
     * The interface of DSpotterRecogStatusListener.
     */
    public interface DSpotterRecogStatusListener {
        /**
         * Inform recognition status.
         *
         * @param nStatus [out] The recognition status.
         */
        public void onDSpotterRecogStatusChanged(int nStatus);
    }

    /**
     * Get information about SDK.
     *
     * @param strLicenseFile [in] The full path of license file.
     * @param objVerInfo     [out] The object of Class VerInfo.
     * @param naErr          [out] The return code.
     * @return If succeed, return SDK information. Otherwise return null.
     */
    public synchronized String getSDKVersionInfo(String strLicenseFile, VerInfo objVerInfo, int[] naErr) {
        return DSpotter.DSpotterVerInfo(strLicenseFile, objVerInfo, naErr);
    }

    /**
     * Check License.
     *
     * @param strLicenseFile [in] The full path of license file.
     * @return If license is valid, return true. Otherwise return false.
     */
    public synchronized boolean isCheckLicenseOK(String strLicenseFile) {
        int naErr[] = new int[1];
        String strSDKVerInfo = DSpotter.DSpotterVerInfo(strLicenseFile, null, naErr);

        if (naErr[0] != DSpotter.DSPOTTER_SUCCESS || strSDKVerInfo.contains("Trial"))
            return false;

        return true;
    }
}
