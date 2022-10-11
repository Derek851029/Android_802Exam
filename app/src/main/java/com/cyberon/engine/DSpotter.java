package com.cyberon.engine;

import com.cyberon.dspotterutility.DSpotterRecog;

public class DSpotter
{
	public static final int DSPOTTER_SUCCESS 			= 0;
	public static final int DSPOTTER_ERR_SDKError 		= -2000;
	public static final int DSPOTTER_ERR_LexiconError 	= -3000;
	public static final int DSPOTTER_ERR_EngineError 	= -5000;


	/** Recognition type */
	public static final int DSPOTTER_RecogType_Unknown				= 0;
	public static final int DSPOTTER_RecogType_Passed				= 1;
	public static final int DSPOTTER_RecogType_NotGoodEnough		= 2;
	public static final int DSPOTTER_RecogType_MissStartSyllable	= 3;
	public static final int DSPOTTER_RecogType_MissEndSyllable		= 4;


	/** SD */


	/** Error code */
	public static final int DSPOTTER_ERR_IllegalHandle				= DSPOTTER_ERR_SDKError -   1;
	public static final int DSPOTTER_ERR_IllegalParam				= DSPOTTER_ERR_SDKError -   2;
	public static final int DSPOTTER_ERR_LeaveNoMemory				= DSPOTTER_ERR_SDKError -   3;
	public static final int DSPOTTER_ERR_LoadDLLFailed				= DSPOTTER_ERR_SDKError -   4;
	public static final int DSPOTTER_ERR_LoadModelFailed			= DSPOTTER_ERR_SDKError -   5;
	public static final int DSPOTTER_ERR_GetFunctionFailed			= DSPOTTER_ERR_SDKError -   6;
	public static final int DSPOTTER_ERR_ParseEINFailed				= DSPOTTER_ERR_SDKError -   7;
	public static final int DSPOTTER_ERR_OpenFileFailed				= DSPOTTER_ERR_SDKError -   8;
	public static final int DSPOTTER_ERR_NeedMoreSample				= DSPOTTER_ERR_SDKError -   9;
	public static final int DSPOTTER_ERR_Timeout					= DSPOTTER_ERR_SDKError -  10;
	public static final int DSPOTTER_ERR_InitWTFFailed				= DSPOTTER_ERR_SDKError -  11;
	public static final int DSPOTTER_ERR_AddSampleFailed			= DSPOTTER_ERR_SDKError -  12;
	public static final int DSPOTTER_ERR_BuildUserCommandFailed		= DSPOTTER_ERR_SDKError -  13;
	public static final int DSPOTTER_ERR_MergeUserCommandFailed		= DSPOTTER_ERR_SDKError -  14;
	public static final int DSPOTTER_ERR_IllegalUserCommandFile		= DSPOTTER_ERR_SDKError -  15;
	public static final int DSPOTTER_ERR_IllegalWaveFile			= DSPOTTER_ERR_SDKError -  16;
	public static final int DSPOTTER_ERR_BuildCommandFailed			= DSPOTTER_ERR_SDKError -  17;
	public static final int DSPOTTER_ERR_InitFixNRFailed			= DSPOTTER_ERR_SDKError -  18;
	public static final int DSPOTTER_ERR_EXCEED_NR_BUFFER_SIZE		= DSPOTTER_ERR_SDKError -  19;
	public static final int DSPOTTER_ERR_Rejected				    = DSPOTTER_ERR_SDKError -  20;
	public static final int DSPOTTER_ERR_NoVoiceDetect		        = DSPOTTER_ERR_SDKError -  21;
	public static final int DSPOTTER_ERR_Expired					= DSPOTTER_ERR_SDKError - 100;
	public static final int DSPOTTER_ERR_LicenseFailed				= DSPOTTER_ERR_SDKError - 200;
	public static final int DSPOTTER_ERR_LoadLicenseFailed			= DSPOTTER_ERR_SDKError - 201;
	public static final int DSPOTTER_ERR_SDKNotSupport				= DSPOTTER_ERR_SDKError - 202;
	public static final int DSPOTTER_ERR_LicenseCountExceeded		= DSPOTTER_ERR_SDKError - 203;

	public static final int DSPOTTER_ERR_CreateModelFailed			= DSPOTTER_ERR_SDKError - 500;
	public static final int DSPOTTER_ERR_WriteFailed				= DSPOTTER_ERR_SDKError - 501;
	public static final int DSPOTTER_ERR_NotEnoughStorage			= DSPOTTER_ERR_SDKError - 502;
	public static final int DSPOTTER_ERR_NoisyEnvironment			= DSPOTTER_ERR_SDKError - 503;
	public static final int DSPOTTER_ERR_VoiceTooShort				= DSPOTTER_ERR_SDKError - 504;
	public static final int DSPOTTER_ERR_VoiceTooLong				= DSPOTTER_ERR_SDKError - 505;

	public static final int DSPOTTER_ERR_AGCError					= DSPOTTER_ERR_SDKError - 605;

	public static final int DSPOTTER_ERR_GetTime				    = DSPOTTER_ERR_SDKError - 701;
	public static final int DSPOTTER_ERR_DifferentVersion			= DSPOTTER_ERR_SDKError - 702;
	public static final int DSPOTTER_ERR_WrongPackBin				= DSPOTTER_ERR_SDKError - 703;

	/** Main API */
	public static native long DSpotterInitMultiWithPackBin(String strPackBin, boolean[] baEnableGroup, int nMaxTime, byte[] byaPreserve, int[] naErr, String strLicenseFile, String strServerFile, Object objContext);

	public static native long DSpotterInitMultiWithMod(String strCYBaseFile, String[] straGroupFile, int nMaxTime, byte[] byaPreserve, int[] naErr, String strLicenseFile, String strServerFile, Object objContext); //Deprecated API.

	public static native int DSpotterReset(long lDSpotter);

	public static native int DSpotterRelease(long lDSpotter);

	public static native int DSpotterGetCommandNumber(long lDSpotter);

	public static native int DSpotterGetUTF8Command(long lDSpotter, int nCmdIdx, String[] straCommand);

	public static native int DSpotterGetSampleRate(long lDSpotter);

	public static native int DSpotterGetNumGroup(String strPackBin);

	public static native String DSpotterVerInfo(String strLicenseFile, Object objVerInfo, int[] naErr);

	public static native int DSpotterAddSample(long lDSpotter, short[] saSample);

	public static native boolean DSpotterIsKeywordAlive(long lDSpotter, int[] naErr);

	public static native int DSpotterGetUTF8Result(long lDSpotter, int[] naCmdIdx, String[] straResult, int[] naWordDura, int[] naEndSil, int[] naNetworkLatency, int[] naConfi, int[] naSGDiff, int[] naFIL);

	public static native int DSpotterGetUTF8ResultNoWait(long lDSpotter, int[] naCmdIdx, String[] straResult, int[] naWordDura, int[] naEndSil, int[] naNetworkLatency, int[] naConfi, int[] naSGDiff, int[] naFIL);

	public static native int DSpotterSetEnableNBest(long lDSpotter, boolean bEnable);
	
	public static native int DSpotterGetNBestUTF8ResultScore(long lDSpotter, int[] naCmdIdx, String[] straResult, int[] naScore, int nMaxNBest);

	public static native int DSpotterGetCmdEnergy(long lDSpotter);

	public static native int DSpotterSetResultIDMapping(long lDSpotter, String strIDMappingPackBin);

	public static native int DSpotterGetResultIDMapping(long lDSpotter);

	/** Threshold API */
	public static native int DSpotterSetRejectionLevel(long lDSpotter, int nRejectionLevel); //Deprecated and replaced it with DSpotterSetConfiReward.

	public static native int DSpotterSetConfiReward(long lDSpotter, int nReward);

	public static native int DSpotterSetSgLevel(long lDSpotter, int nSgLevel); //Deprecated and replaced it with DSpotterSetSGDiffReward.

	public static native int DSpotterSetSGDiffReward(long lDSpotter, int nReward);

	public static native int DSpotterSetFilLevel(long lDSpotter, int nFilLevel); //Deprecated API.
	
	public static native int DSpotterSetResponseTime(long lDSpotter, int nResponseTime); //Deprecated and replaced it with DSpotterSetEndSil.

	public static native int DSpotterSetEndSil(long lDSpotter, int nEndSil);

	public static native int DSpotterSetCmdResponseTime(long lDSpotter, int nCmdIdx, int nResponseTime); //Deprecated and replaced it with DSpotterSetCmdEndSil.

	public static native int DSpotterSetCmdEndSil(long lDSpotter, int nCmdIdx, int nEndSil);

	public static native int DSpotterSetEnergyTH(long lDSpotter, int nEnergyTH);

	public static native int DSpotterSetCmdReward(long lDSpotter, int nCmdIdx, int nReward); //Deprecated and replaced it with DSpotterSetCmdConfiReward.

	public static native int DSpotterSetCmdConfiReward(long lDSpotter, int nCmdIdx, int nReward);

	public static native int DSpotterSetCmdSGDiffReward(long lDSpotter, int nCmdIdx, int nReward);

	public static native int DSpotterGetRejectionLevel(long lDSpotter, int[] naErr); //Deprecated and replaced it with DSpotterGetConfiReward.

	public static native int DSpotterGetConfiReward(long lDSpotter, int[] naErr);

	public static native int DSpotterGetSgLevel(long lDSpotter, int[] naErr); //Deprecated and replaced it with DSpotterGetSGDiffReward.

	public static native int DSpotterGetSGDiffReward(long lDSpotter, int[] naErr);

	public static native int DSpotterGetFilLevel(long lDSpotter, int[] naErr); //Deprecated API.

	public static native int DSpotterGetCmdResponseTime(long lDSpotter, int nCmdIdx, int[] naErr); //Deprecated and replaced it with DSpotterGetCmdEndSil.

	public static native int DSpotterGetCmdEndSil(long lDSpotter, int nCmdIdx, int[] naErr);

	public static native int DSpotterGetEnergyTH(long lDSpotter, int[] naErr);

	public static native int DSpotterGetCmdReward(long lDSpotter, int nCmdIdx, int[] naErr); //Deprecated and replaced it with DSpotterGetCmdConfiReward.

	public static native int DSpotterGetCmdConfiReward(long lDSpotter, int nCmdIdx, int[] naErr);

	public static native int DSpotterGetCmdSGDiffReward(long lDSpotter, int nCmdIdx, int[] naErr);

	public static native int DSpotterGetModelRejectionLevel(long lDSpotter, int[] naErr); //Deprecated and replaced it with DSpotterGetModelConfiReward.

	public static native int DSpotterGetModelConfiReward(long lDSpotter, int[] naErr);

	public static native int DSpotterGetModelSgLevel(long lDSpotter, int[] naErr); //Deprecated and replaced it with DSpotterGetModelSGDiffReward.

	public static native int DSpotterGetModelSGDiffReward(long lDSpotter, int[] naErr);

	public static native int DSpotterGetModelFilLevel(long lDSpotter, int[] naErr); //Deprecated API.

	/** AGC API */
	public static native int DSpotterAGCEnable(long lDSpotter);

	public static native int DSpotterAGCDisable(long lDSpotter);

	public static native int DSpotterAGCSetMaxGain(long lDSpotter, float fMaxGain);

	public static native int DSpotterAGCSetIncGainThrd(long lDSpotter, short sLowerThrd);

	public static native int DSpotterAGCSetCallback(long lDSpotter, boolean bSetCallback);

	private static void AGCDataCallback(short[] saOutputSample) { DSpotterRecog.m_oAGCDataItems.add(saOutputSample); }

	/** Utility API */
	public static native int DSpotterCombinePackBin(String[] straPackBin, String strOutPackBin);

	public static native int DSpotterCombineMapIDPackBin(String[] straIDMappingPackBin, String strOutIDMappingPackBin);
}
