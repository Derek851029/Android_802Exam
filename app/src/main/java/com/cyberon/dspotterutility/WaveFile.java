package com.cyberon.dspotterutility;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import android.content.Context;

public class WaveFile
{
	private DataOutputStream mDataStream = null;
	private FileOutputStream mFileStream = null;
	private int mSampleSizeInByte = 0;
	
	/**
	 * Constructor a new WaveFile using specified name for the specified
	 * context.
	 * 
	 * @param context
	 *        [in] The context for the file.
	 * @param name
	 *        [in] The file name to be contained for the context. Can not
	 *        contain path separators
	 */
	public WaveFile(Context context, String name) throws FileNotFoundException
	{
		//mFileStream = context.openFileOutput(name, Context.MODE_WORLD_READABLE);
		//Debug.d("WaveFile", "1. " + name);
		//mFileStream = context.openFileOutput(name, Context.MODE_WORLD_READABLE);
		File file = new File(name);
		mFileStream = new FileOutputStream(file);
		//Debug.d("WaveFile", "2. " + mFileStream);
		mDataStream = new DataOutputStream(mFileStream);
	}
	
	/**
	 * Close the wave file.
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		setDataSize(mSampleSizeInByte);
		mDataStream.flush();
		mDataStream.close();
		mFileStream.close();
	}
	
	/**
	 * Set wave file format.
	 * 
	 * @param bitsPerSample
	 *        [in] Bits per sample. 8 or 16.
	 * @param channelsPerFrame
	 *        [in] Number of channels. 1 or 2.
	 * @param sampleRate
	 *        [in] Sample rate. This value should be 8000, 16000, 11025, 22050,
	 *        or 44100.
	 * @throws IOException
	 */
	public void setFormat(
		int bitsPerSample,
		int channelsPerFrame,
		int sampleRate) throws IOException
	{
		mFileStream.getChannel().position(0);
		mDataStream.writeByte('R');
		mDataStream.writeByte('I');
		mDataStream.writeByte('F');
		mDataStream.writeByte('F');
		mDataStream.writeInt(0);
		mDataStream.writeByte('W');
		mDataStream.writeByte('A');
		mDataStream.writeByte('V');
		mDataStream.writeByte('E');
		mDataStream.writeByte('f');
		mDataStream.writeByte('m');
		mDataStream.writeByte('t');
		mDataStream.writeByte(' ');
		mDataStream.writeInt(Integer.reverseBytes(16));
		mDataStream.writeShort(Short.reverseBytes((short)1));
		mDataStream.writeShort(Short.reverseBytes((short)channelsPerFrame));
		mDataStream.writeInt(Integer.reverseBytes(sampleRate));
		mDataStream.writeInt(Integer.reverseBytes(sampleRate * channelsPerFrame
			* bitsPerSample / 8));
		mDataStream.writeShort(Short.reverseBytes((short)(channelsPerFrame
			* bitsPerSample / 8)));
		mDataStream.writeShort(Short.reverseBytes((short)bitsPerSample));
		mDataStream.writeByte('d');
		mDataStream.writeByte('a');
		mDataStream.writeByte('t');
		mDataStream.writeByte('a');
		mDataStream.writeInt(0);
		mDataStream.flush();
	}
	
	/**
	 * Write wave data to WaveFile object.
	 * 
	 * @param data
	 *        [in] Byte array data.
	 * 
	 * @throws IOException
	 */
	public void writeData(byte[] data) throws IOException
	{
		mFileStream.getChannel().position(44 + mSampleSizeInByte);
		mDataStream.write(data);
		mDataStream.flush();
		mSampleSizeInByte += data.length;
	}
	
	public void writeData(short[] data) throws IOException
	{
		mFileStream.getChannel().position(44 + mSampleSizeInByte);
		ByteBuffer byteBuf = ByteBuffer.allocate(data.length * (Short.SIZE / 8));
		byteBuf.order(ByteOrder.LITTLE_ENDIAN);
		for (int i = 0; i < data.length; i++)
			byteBuf.putShort(data[i]);
		mDataStream.write(byteBuf.array());
		mDataStream.flush();
		mSampleSizeInByte += (byteBuf.array().length);
	}
	
	/**
	 * Returns the wave data size in bytes that this object currently has.
	 * 
	 * @return The wave data size in bytes.
	 */
	public int getSize()
	{
		return mSampleSizeInByte;
	}
	
	private void setDataSize(int size) throws IOException
	{
		int chunkSize = 44 + size - 8;
		
		mFileStream.getChannel().position(4);
		mDataStream.writeInt(Integer.reverseBytes(chunkSize));
		mDataStream.flush();
		mFileStream.getChannel().position(40);
		mDataStream.writeInt(Integer.reverseBytes(size));
		mDataStream.flush();
	}
}
