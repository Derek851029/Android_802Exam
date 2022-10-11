package com.cyberon.dspotterutility;

public class DSpotterStatus
{
	public static final int STATUS_RECOGNITION_START						= 0x00000021;
	public static final int STATUS_RECOGNITION_END							= 0x00000022;
	public static final int STATUS_RECORD_FINISH								= 0x00000023;
	
	public static final int STATUS_RECOGNITION_OK							= 0x00000001;
	public static final int STATUS_RECOGNITION_ABORT						= 0x00000002;
	public static final int STATUS_RECOGNITION_TIMEOUT					= 0x00000003;
	public static final int STATUS_RECOGNITION_OPEN_WAVE_FAIL	= 0x00000004;
	public static final int STATUS_RECOGNITION_FAIL							= 0x00000005;
	public static final int STATUS_RECORD_FAIL									= 0x00000006;
	
	public static final int STATUS_RECORDER_INITIALIZE_FAIL			= 0x00000050;
}
