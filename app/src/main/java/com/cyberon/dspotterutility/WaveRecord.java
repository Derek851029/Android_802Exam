package com.cyberon.dspotterutility;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder.AudioSource;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;

/**
 *
 * @author Alan Lin
 * @version 1.0
 *
 * Class WaveRecord provides a standard way to record audio from microphone. 
 * It also support to record from user assigned file
 */
public class WaveRecord implements AudioRecord.OnRecordPositionUpdateListener {
	public interface WaveRecordPositionListener {
		public void OnWaveRecordStartNotify();

		/**
		 * Callback method to be invoked when samples of an audio buffer has been captured
		 * during recording.  
		 * @param nPosInBytes - The total captured buffer size
		 * @return none.
		 */
		public void OnWaveRecordPositionNotify(int nPosInBytes);
	}

	private static final String LOG_TAG = "WaveRecord";
	protected int mByteArrayLength = 0;
	protected int mShortArrayLength = 0;
	protected AudioRecord mAudioRecord = null;
	protected byte[] mByteArray = null;
	protected short[] mShortArray = null;
	protected ByteBuffer mByteBuffer = null;
	protected DataInputStream mDis = null;
	protected int mDisAvailable = 0;
	protected int mFrameTime = 0;
	protected int mRecordedFrameCount = 0;
	protected WaveRecordPositionListener mWaveRecCB = null;
	protected boolean mbRecording = false;

	/**
	 * Initialization.
	 *
	 * @param channelNumber
	 *        [in] Audio channel number. Mono = 1, Stereo = 2.
	 * @param bitsPerSample
	 *        [in] Size in bytes per sample.
	 * @param sampleRate
	 *        [in] Recording sample rate.
	 * @param bufferSizeInByte
	 *        [in] Size of buffer in bytes returned every time getWave() is invoked.
	 * @param bufferNumber
	 *        [in] Ignored.
	 */
	@SuppressLint("MissingPermission")
	public synchronized boolean initialize(int nAudioSrc, int channelNumber, int bitsPerSample,
										   int sampleRate, int bufferSizeInByte, int bufferNumber) {
		if (bufferSizeInByte != mByteArrayLength) {
			mByteArray = null;
			mShortArray = null;
			mByteBuffer = null;
		}

		mByteArrayLength = bufferSizeInByte;
		mShortArrayLength = bufferSizeInByte / 2;
		mFrameTime = (int) ((long) bufferSizeInByte * 1000 / (channelNumber * bitsPerSample / 8 * sampleRate));

		try {
			int min = AudioRecord.getMinBufferSize(sampleRate,
					(channelNumber == 1) ? AudioFormat.CHANNEL_CONFIGURATION_MONO : AudioFormat.CHANNEL_CONFIGURATION_STEREO,
					(bitsPerSample == 8) ? AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT);

			Log.w(LOG_TAG, "sampleRate = " + sampleRate + ", bufferSize = " + bufferSizeInByte + ", getMinBufferSize = " + min);
			min = Math.max(min, bufferSizeInByte * bufferNumber);

//			if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
//			}
			mAudioRecord = new AudioRecord(
					nAudioSrc,
					sampleRate,
					(channelNumber == 1) ? AudioFormat.CHANNEL_CONFIGURATION_MONO : AudioFormat.CHANNEL_CONFIGURATION_STEREO,
					(bitsPerSample == 8) ? AudioFormat.ENCODING_PCM_8BIT : AudioFormat.ENCODING_PCM_16BIT,
					min);
			
			if (mAudioRecord != null)
			{
				if (!waitState(AudioRecord.STATE_INITIALIZED, 30, 50))
				{
					Log.d(LOG_TAG, "Fail to to waiting AudioRecord init state ready");
					mAudioRecord.release();
					mAudioRecord = null;
				}
			}
			
		}
		catch (Exception e)
		{
			mAudioRecord = null;
			Log.e(LOG_TAG, "Fail to open microphone input stream !!", e);
		}
		return (mAudioRecord != null);
	}
	
	public synchronized boolean initialize(int channelNumber, int bitsPerSample,
			int sampleRate, int bufferSizeInByte, int bufferNumber)
	{
		int nAudioSrc = AudioSource.MIC;
		if (Build.VERSION.SDK_INT >= 7)
			nAudioSrc = AudioSource.VOICE_RECOGNITION;
		
		return initialize(nAudioSrc, channelNumber, bitsPerSample,
				sampleRate, bufferSizeInByte, bufferNumber);
	}
	
	/**
	 * Release resource.
	 */
	public synchronized void release()
	{
		if (mAudioRecord != null)
		{
			mAudioRecord.release();
			mAudioRecord = null;
		}
	}
	
	public synchronized boolean isInitOK()
	{
		if (mAudioRecord == null)
			return false;
		
		return (mAudioRecord.getState() !=	AudioRecord.STATE_UNINITIALIZED);
	}

	public synchronized boolean setRecordPositionListener(WaveRecord.WaveRecordPositionListener cb)
	{
		if (mAudioRecord == null)
			return false;
		
		mWaveRecCB = cb;
		mAudioRecord.setRecordPositionUpdateListener(this);
		mAudioRecord.setNotificationMarkerPosition(1); 
		if (mAudioRecord.getAudioFormat() == AudioFormat.ENCODING_PCM_8BIT)
			mAudioRecord.setPositionNotificationPeriod(mByteArrayLength);
		else
			mAudioRecord.setPositionNotificationPeriod(mByteArrayLength/2);
		return true;
	}
	
	/**
	 * Invoke this function before getWave().
	 * 
	 * @return true if recording starts successful, false otherwise.
	 */
	public synchronized boolean start()
	{
		boolean ret = false;
		
		if (mAudioRecord != null)
		{
			if ( (mAudioRecord.getState() == AudioRecord.STATE_INITIALIZED) &&
				 (mAudioRecord.getRecordingState() == AudioRecord.RECORDSTATE_STOPPED) )
			{
				try
				{
					mRecordedFrameCount = 0;
					mAudioRecord.startRecording();
					if (waitRecordingState(AudioRecord.RECORDSTATE_RECORDING, 30, 50))
						mbRecording = true;
					ret = true;
				}
				catch(Exception e)
				{
					Log.e(LOG_TAG, "Fail to start record !!!", e);
				}
			}
		}
		else
		{
			Log.e(LOG_TAG, "AudioRecord is not init yet !!!");
		}
		
		return ret; 
	}
	
	/**
	 * Stop recording process.
	 * 
	 * @return true if stop recording successful, false otherwise.
	 */
	public synchronized boolean stop()
	{
		boolean ret = false;
		
		if (mAudioRecord != null)
		{
			mbRecording = false;
			if (mAudioRecord.getRecordingState()== AudioRecord.RECORDSTATE_RECORDING)
			{
				try
				{
					mAudioRecord.stop();
					waitRecordingState(AudioRecord.RECORDSTATE_STOPPED, 30, 50);
					ret = true;
				}
				catch(Exception e)
				{
					Log.e(LOG_TAG, "Fail to stop record !!!", e);
				}
			}
		}
		
		return ret;
	}

	public boolean isRecording()
	{
		if (mAudioRecord == null)
			return false;
		
		return mbRecording;
	}
	
	/**
	 * Get data from a specified file.
	 * 
	 * @param fileName [in] The file name to read.
	 */
	public void setWaveFile(String fileName)
	{
		try
		{
			mDis = new DataInputStream(new FileInputStream(fileName));
			mDis.skip(44); // skip header
			mDisAvailable = mDis.available();
		}
		catch (NullPointerException e)
		{
			Log.e(LOG_TAG, "Wave File Not Found !! (" + fileName +")", e);
			mDis = null;
		}
		catch (IOException e)
		{
			Log.e(LOG_TAG, "setWaveFile()", e);
			mDis = null;
		}
	}
	
	/**
	 * Get wave data from file stream or microphone.
	 * 
	 * @return A buffer of wave data indicates success. Otherwise returns null.
	 */
	public synchronized byte[] getWave()
	{
		// Get wave data from file
		if (mByteArray == null)
			mByteArray = new byte [mByteArrayLength];
		
		if (mDis != null)
		{
			try
			{
				if (mDisAvailable > 0)
				{
					int read = 0;
					if (mDisAvailable >= mByteArray.length)
					{
						// get byte[] data
						read = mDis.read(mByteArray, 0, mByteArray.length);
					}
					else
					{
						read = mDis.read(mByteArray, 0, mDisAvailable);
						Arrays.fill(mByteArray, read, mByteArray.length, (byte)0);
					}
					
					mDisAvailable -= read;
					return mByteArray;
				}
			}
			catch (IOException e)
			{
				Log.e(LOG_TAG, "", e);
			}
		}
		else if (mAudioRecord != null)
		{
			int readLen = 0;
			int nRet;
			int nTryCount = 0;
			try
			{
				while (nTryCount++ < 5)
				{
					nRet = mAudioRecord.read(mByteArray, readLen, mByteArrayLength - readLen);
					if (nRet < 0)
					{
						Arrays.fill(mByteArray, readLen, mByteArray.length, (byte)0);
						break;
					}
					readLen += nRet;
					if (readLen >= mByteArrayLength)
					{
						break;
					}
					else
					{
						Thread.sleep(mFrameTime);
						Log.w(LOG_TAG, "readLen(" + readLen + ") != mByteArrayLength(" + mByteArrayLength + ")");
					}
				}
				
				return mByteArray;
			}
			catch(Exception e)
			{
				Log.e(LOG_TAG, "[getWav]", e);
			}
		}
		return null;
	}
	
	/**
	 * Get wave data from file stream or microphone.
	 * 
	 * @return A buffer of wave data indicates success. Otherwise returns null.
	 */
	public synchronized short[] getShortWave()
	{
		if (mShortArray == null)
			mShortArray = new short [mShortArrayLength];
		
		if (mDis != null)
		{
			if (getWave() != null)
			{
				// convert byte[] to short[] in little-endian format
				for (int k = 0; k < mShortArray.length; k++)
					mShortArray[k] = (short)(((mByteArray[2 * k + 1] & 0xff) << 8) | (mByteArray[2 * k] & 0xff));
				return mShortArray;
			}
		}
		else if (mAudioRecord != null)
		{
			int readLen = 0;
			int nRet;
			int nTryCount = 0;
			try
			{			
				while (nTryCount++ < 5)
				{
					/* The code to verify ByteBuffer NIO recording
					if (mByteBuffer == null)
					{
						mByteBuffer = ByteBuffer.allocateDirect(mByteArrayLength);
						mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
					}
					mByteBuffer.clear();
					nRet = mAudioRecord.read(mByteBuffer, mByteArrayLength);
					if (nRet < 0)
					{
						Arrays.fill(mShortArray, readLen, mShortArray.length, (short)0);
						Log.e("read(mByteBuffer) fail!");
						break;
					}
					mByteBuffer.rewind();
					int n = 0;
					while (n < nRet-2)
					{
						//If no mByteBuffer.order(ByteOrder.LITTLE_ENDIAN), 
						// we must convert by ourself
						//mShortArray[readLen + n/2] =(short)(((mByteBuffer.get(n+1) & 0xff) << 8) | (mByteBuffer.get(n) & 0xff)); 
						mShortArray[readLen + n/2] = mByteBuffer.getShort(n);
						n += 2;
					}
					readLen += (nRet/2);
					*/
					
					nRet = mAudioRecord.read(mShortArray, readLen, mShortArrayLength - readLen);
					if (nRet < 0)
					{
						Arrays.fill(mShortArray, readLen, mShortArray.length, (short)0);
						Log.e(LOG_TAG, "read(mShortArray) fail!");
						break;
					}
					readLen += nRet;
					if (readLen >= mShortArrayLength)
					{
						break;
					}
					else
					{
						Thread.sleep(mFrameTime);
						Log.w(LOG_TAG, "readLen(" + readLen + ") != mShortArrayLength(" + mShortArrayLength + ")");
					}
				}
				
				return mShortArray;
			}
			catch(Exception e)
			{
				Log.e(LOG_TAG, "", e);
			}
		}
		return null;
	}
	
	public synchronized short[] getShortWaveClone()
	{
		mShortArray = new short [mShortArrayLength];
		return getShortWave();
	}
	
	public synchronized int readShortWave(short [] sBuffer)
	{
		if (sBuffer == null || sBuffer.length != mShortArrayLength)
			return 0;
		mShortArray = sBuffer;
		if (getShortWave() == null)
			return 0;
		return mShortArrayLength;
	}
	
	public synchronized ByteBuffer getByteBufferWave()
	{
		if (mByteBuffer == null)
		{
			mByteBuffer = ByteBuffer.allocateDirect(mByteArrayLength);
			mByteBuffer.order(ByteOrder.LITTLE_ENDIAN);
		}
		
		if (mAudioRecord != null)
		{
			int nRet;
			try
			{
				mByteBuffer.clear();
				nRet = mAudioRecord.read(mByteBuffer, mByteArrayLength);
				if (nRet > 0)
				{
					mByteBuffer.rewind();
					mByteBuffer.limit(nRet);
					return mByteBuffer;
				}
			}
			catch(Exception e)
			{
				Log.e(LOG_TAG, "", e);
			}
		}
		
		return null;
	}
	
	@Override
	protected void finalize() throws Throwable
	{
		stop();
		release();
		mByteArray = null;
		mShortArray = null;
		mByteBuffer = null;
		super.finalize();
	}
	
	@Override
    public void onMarkerReached(AudioRecord recorder)
	{
		//Log.d("onMarkerReached !");
		if (mWaveRecCB != null)
			mWaveRecCB.OnWaveRecordStartNotify();
	}

	@Override
    public void onPeriodicNotification(AudioRecord recorder)
	{
		//Log.d("onPeriodicNotification !");
		mRecordedFrameCount++;
		if (mWaveRecCB != null)
			mWaveRecCB.OnWaveRecordPositionNotify(mRecordedFrameCount * mByteArrayLength);
	}
	
	protected boolean waitState(int nWaitState, int nWaitDuration, int nMaxWaitCount)
	{
		int nState;
		int nWaitCount = 0;
		
		if ( (nWaitState != AudioRecord.STATE_INITIALIZED) &&
			 (nWaitState != AudioRecord.STATE_UNINITIALIZED) )
				return false;
			
		while (true)
		{
			synchronized (WaveRecord.this)
			{
				nState = mAudioRecord.getState();
			}
			if (nState == nWaitState)
				break;
			if (nWaitCount++ > nMaxWaitCount)
				break;
			try
			{
				Thread.sleep(nWaitDuration);
			}
			catch (Exception ex)
			{
			}
		}
		Log.d(LOG_TAG, "Waiting AudioRecord state " + nWaitState + " use " + nWaitCount*nWaitDuration + " ms");
		
		return (nWaitCount <= nMaxWaitCount);
	}

	protected boolean waitRecordingState(int nWaitRecordingState, int nWaitDuration, int nMaxWaitCount)
	{
		int nRecordingState;
		int nWaitCount = 0;
		
		if ( (nWaitRecordingState != AudioRecord.RECORDSTATE_RECORDING) &&
			 (nWaitRecordingState != AudioRecord.RECORDSTATE_STOPPED) )
			return false;
		
		while (true)
		{
			synchronized (WaveRecord.this)
			{
				nRecordingState = mAudioRecord.getRecordingState();
			}
			if (nRecordingState == nWaitRecordingState)
				break;
			if (nWaitCount++ > nMaxWaitCount)
				break;
			try
			{
				Thread.sleep(nWaitDuration);
			}
			catch (Exception ex)
			{
			}
		}
		Log.d(LOG_TAG, "Waiting AudioRecord recording state " + nWaitRecordingState + " use " + nWaitCount*nWaitDuration + " ms");
		
		return (nWaitCount <= nMaxWaitCount);
	}
}
