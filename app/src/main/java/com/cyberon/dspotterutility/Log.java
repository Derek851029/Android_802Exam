/**Log.java
 * Log for on-line log outputting and log file handling.
 * @author  Alger,Lin
 * @date    2009,Mar,27*/

package com.cyberon.dspotterutility;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.os.Environment;


/**On-line log message and off-line log file handler.*/
public class Log
{
    /**Flag to indicates if wave be logged.<p>
     * The wave log function not implemented in Log.java.*/
    private static boolean              LOG_WAVE = false;
    /**Log output level.<p>
     * This field must be one of definition of android.util.Log.<br>
     * Set android.util.Log.ASSERT to disable log output.*/
    public static int             		LOG_LEVEL_LOG = android.util.Log.WARN;
    /**File log level.<p>
     * This field must be one of definition of android.util.Log.<br>
     * Set android.util.Log.ASSERT to disable file log.*/
    private static int                  LOG_LEVEL_FILE = android.util.Log.ASSERT;
    /**Level to log call stack in log output.<p>
     * This field must be one of definition of android.util.Log.*/
    private static final int            LOG_LEVEL_LOG_STACK = android.util.Log.ERROR;
    /**Level to log call stack in file.<p>
     * This field must be one of definition of android.util.Log.*/
    private static final int            LOG_LEVEL_FILE_STACK = android.util.Log.ERROR;
    /**Maximum number of call stack in log output.*/
    private static final int            LOG_MAX_STACK_LOG = 10;
    /**Maximum number of call stack in log file.*/
    private static final int            LOG_MAX_STACK_FILE = 5;
    /**Maximum size of log file in bytes.<p>
     * The file size if checked when opening.*/
    private static final int            FILE_MAX_SIZE = 1024 * 1024;    //1 MB
    /**Line break in log file.*/
    private static final String         LINE_BREAK = "\r\n";
    /**Name of wave file.*/
    private static String               m_szWavName = null;
    /**Log file.*/
    private static OutputStreamWriter   m_oFile = null;
    
    
    /**Set filename of recorded wave file.<p>
     * This function available if LOG_WAV is true only, and wave file located at private directory 
     * of application. 
     * @param szFile    [in] Pure filename of recorded wave file, set null to use default filename.*/
    public static void setWavFilename(String szFile)
    {
        m_szWavName = szFile;
    }
    
    /**Log the debug message.
     * @param szMsg [in] Message for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void d(String szMsg, Object... oArgs)
    {
        log(android.util.Log.DEBUG, szMsg, null, oArgs);
    }
    /**Log the debug message.
     * @param szMsg [in] Message for logged.
     * @param oThr  [in] Throwable for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void d(String szMsg, Throwable oThr, Object... oArgs)
    {
        log(android.util.Log.DEBUG, szMsg, oThr, oArgs);
    }

    /**Log the error message.
     * @param szMsg [in] Message for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void e(String szMsg, Object... oArgs)
    {
        log(android.util.Log.ERROR, szMsg, null, oArgs);
    }
    /**Log the error message.
     * @param szMsg [in] Message for logged.
     * @param oThr  [in] Throwable for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void e(String szMsg, Throwable oThr, Object... oArgs)
    {
        log(android.util.Log.ERROR, szMsg, oThr, oArgs);
    }
    
    /**Log the info message.
     * @param szMsg [in] Message for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void i(String szMsg, Object... oArgs)
    {
        log(android.util.Log.INFO, szMsg, null, oArgs);
    }
    /**Log the info message.
     * @param szMsg [in] Message for logged.
     * @param oThr  [in] Throwable for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void i(String szMsg, Throwable oThr, Object... oArgs)
    {
        log(android.util.Log.INFO, szMsg, oThr, oArgs);
    }
    
    /**Log the verbose message.
     * @param szMsg [in] Message for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void v(String szMsg, Object... oArgs)
    {
        log(android.util.Log.VERBOSE, szMsg, null, oArgs);
    }
    /**Log the verbose message.
     * @param szMsg [in] Message for logged.
     * @param oThr  [in] Throwable for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void v(String szMsg, Throwable oThr, Object... oArgs)
    {
        log(android.util.Log.VERBOSE, szMsg, oThr, oArgs);
    }
    
    /**Log the warning message.
     * @param szMsg [in] Message for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void w(String szMsg, Object... oArgs)
    {
        log(android.util.Log.WARN, szMsg, null, oArgs);
    }
    /**Log the warning message.
     * @param szMsg [in] Message for logged.
     * @param oThr  [in] Throwable for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    public static void w(String szMsg, Throwable oThr, Object... oArgs)
    {
        log(android.util.Log.WARN, szMsg, oThr, oArgs);
    }
    
    /**Get full path and filename of recorded wave file.
     * @return  Return filename of wave file.*/
    public static String getWavFileName()
    {
        return String.format("%s_%s.wav", ((m_szWavName != null)? m_szWavName: "WavLog"), new SimpleDateFormat("yyMMddHHmmss").format(new Date()));   
    }
    
    /**Release log handler, and save log file.*/
    public static void release()
    {
        if(m_oFile != null)
        {
            try
            {
                m_oFile.close();
            }
            catch(IOException ioe)
            {
                android.util.Log.e("Log", "[release] close file failed", ioe);
            }
            m_oFile = null;
        }
    }
    
    /**Set enable state to log wave into file.
     * @param bEnable   [in] Set true to enable log wave into file, set false to disable it.*/
    public static void setWavFileEnabled(boolean bEnable)
    {
        LOG_WAVE = bEnable;
    }
    
    /**Check if log wave into file is enabled.
     * @return  Return true if enabled, return false if not.*/
    public static boolean isWavFileEnabled()
    {
        return LOG_WAVE;
    }
    
    /**Set debug mode of log.
     * @param bDbg  [in] Set true to reduce log level, set false to reset log level to default.*/
    public static void setDebugMode(boolean bDbg)
    {
        LOG_LEVEL_LOG = bDbg ? android.util.Log.VERBOSE: android.util.Log.WARN;
    }
    
    /**Set enable state to log event into file.
     * @param bEnable   [in] Set true to enable log into file, set false to disable it.*/
    public static void setLogFileEnabled(boolean bEnable)
    {
        LOG_LEVEL_FILE = (bEnable? android.util.Log.VERBOSE: android.util.Log.ASSERT);
        if(!bEnable)
            release();
    }
    
    /**Check if log event into file is enabled.
     * @return  Return true if enabled, return false if not.*/
    public static boolean isLogFileEnabled()
    {
        return (LOG_LEVEL_FILE < android.util.Log.ASSERT);
    }
    
    public static void setLogLevel(int nLogLevel)
    {
        LOG_LEVEL_LOG = nLogLevel;
    }
    
    /**Check if log event into file is enabled.
     * @return  Return true if enabled, return false if not.*/
    public static int getLogLevel()
    {
        return LOG_LEVEL_LOG;
    }
    
    /**Set enable state to log event into file.
     * @param bEnable   [in] Set true to enable log into file, set false to disable it.*/
    public static void setLogFileLevel(int nLogLevelFile)
    {
        LOG_LEVEL_FILE = nLogLevelFile;
        if((LOG_LEVEL_FILE == android.util.Log.ASSERT) && (m_oFile != null))
        {
            try
            {
                m_oFile.close();
            }
            catch(IOException ioe)
            {
                android.util.Log.e("Log", "[setLogFileEnabled] close file failed", ioe);
            }
            m_oFile = null;
        }   
    }
    
    /**Check if log event into file is enabled.
     * @return  Return true if enabled, return false if not.*/
    public static int getLogFileLevel()
    {
        return LOG_LEVEL_FILE;
    }
    
    /**Handle log.
     * @param iLevel    [in] Log level, this parameter should be one definition of android.util.Log
     * @param szMsg     [in] Message for logged.
     * @param oThr      [in] Throwable for logged, set null if without Throwable.
     * @param oArgs     [in] Arguments referenced by the format specifiers in szMsg.*/
    private static void log(int iLevel, String szMsg, Throwable oThr, Object... oArgs)
    {
        StackInfo   oInfo = StackInfo.currentStackInfo();
        String      szTag = getClassName(oInfo);
        //Log to file
        if((iLevel >= LOG_LEVEL_FILE) || (iLevel >= LOG_LEVEL_LOG))     //((iLevel >= LOG_LEVEL_FILE) || android.util.Log.isLoggable(szTag, iLevel))
        {
            String  szTxt = getMessageText(oInfo, szMsg, oArgs);
            String  szLevel = null;
            //Log into log output
            if(iLevel == android.util.Log.ERROR)
            {
                if(oThr != null)
                    android.util.Log.e(szTag, szTxt, oThr);
                else
                    android.util.Log.e(szTag, szTxt);
                szLevel = "ERROR";
            }
            else if(iLevel == android.util.Log.WARN)
            {
                if(oThr != null)
                    android.util.Log.w(szTag, szTxt, oThr);
                else
                    android.util.Log.w(szTag, szTxt);
                szLevel = "WARN";
            }
            else if(iLevel == android.util.Log.INFO)
            {
                if(oThr != null)
                    android.util.Log.i(szTag, szTxt, oThr);
                else
                    android.util.Log.i(szTag, szTxt);
                szLevel = "INFO";
            }
            else if(iLevel == android.util.Log.DEBUG)
            {
                if(oThr != null)
                    android.util.Log.d(szTag, szTxt, oThr);
                else
                    android.util.Log.d(szTag, szTxt);
                szLevel = "DEBUG";
            }
            else if(iLevel <= android.util.Log.VERBOSE)
            {
                if(oThr != null)
                    android.util.Log.v(szTag, szTxt, oThr);
                else
                    android.util.Log.v(szTag, szTxt);
                szLevel = "VERBOSE";
            }
            //Log call stack in log output
            if(iLevel >= LOG_LEVEL_LOG_STACK)
            {
                int             iNum = Math.min((oInfo.m_oStkInfo.length - oInfo.m_iTarget), LOG_MAX_STACK_LOG);
                StringBuffer    szBuf = null;
                for(int a = 0; a < iNum; a ++)
                {
                    szBuf = new StringBuffer(" \t");
                    szBuf.append(oInfo.m_oStkInfo[oInfo.m_iTarget + a].toString());
                    if(iLevel == android.util.Log.ERROR)
                        android.util.Log.e(szTag, szBuf.toString());
                    else if(iLevel == android.util.Log.WARN)
                        android.util.Log.w(szTag, szBuf.toString());
                    else if(iLevel == android.util.Log.INFO)
                        android.util.Log.i(szTag, szBuf.toString());
                    else if(iLevel == android.util.Log.DEBUG)
                        android.util.Log.d(szTag, szBuf.toString());
                    else if(iLevel == android.util.Log.VERBOSE)
                        android.util.Log.v(szTag, szBuf.toString());
                }
            }
            //Log into file
            if(iLevel >= LOG_LEVEL_FILE)
            {
                StringBuffer    szBuf = new StringBuffer(LINE_BREAK);
                StringBuffer    szOpen = null;
                //Set time
                szBuf.append(new SimpleDateFormat("dd-HH:mm:ss").format(new Date()));
                //Set level
                szBuf.append('\t');
                szBuf.append(szLevel);
                szBuf.append(LINE_BREAK);
                //Set tag
                szBuf.append(szTag);
                szBuf.append('\t');
                //Set message
                szBuf.append(szTxt);
                szBuf.append(LINE_BREAK);
                //Set message about exception
                if(oThr != null)
                {
                    szBuf.append("\t");
                    szBuf.append(oThr.toString());
                    szBuf.append(LINE_BREAK);
                }
                //Set message for call stack
                if((iLevel >= LOG_LEVEL_FILE_STACK) || (oThr != null))
                {
                    int iNum = Math.min((oInfo.m_oStkInfo.length - oInfo.m_iTarget), LOG_MAX_STACK_FILE);
                    for(int a = 0; a < iNum; a ++)
                    {
                        szBuf.append("\t");
                        szBuf.append(oInfo.m_oStkInfo[oInfo.m_iTarget + a].toString());
                        szBuf.append(LINE_BREAK);
                    }
                }
                //Open log file
                if(m_oFile == null)
                {
                    File    oFile = new File(String.format("%s%sCvc.log", Environment.getExternalStorageDirectory().getAbsolutePath(), File.separator));
                    if(oFile.exists())
                    {
                        //Check file size
                        if(oFile.length() >= FILE_MAX_SIZE)
                            oFile.delete();
                        //Set text for appending file
                        else
                        {
                            szOpen = new StringBuffer(LINE_BREAK);
                            szOpen.append(LINE_BREAK);
                            szOpen.append(LINE_BREAK);
                        }
                    }
                    //Open file
                    try
                    {
                        m_oFile = new OutputStreamWriter(new FileOutputStream(oFile, true), "UTF-16");
                    }
                    catch(FileNotFoundException fne)
                    {
                        android.util.Log.e("Log", "[log] Open file failed", fne);
                    }
                    catch(SecurityException se)
                    {
                        android.util.Log.e("Log", "[log] Open file failed", se);
                    }
                    catch(UnsupportedEncodingException uee)
                    {
                        android.util.Log.e("Log", "[log] Open file failed", uee);
                    }
                }
                //Write into file
                if(m_oFile != null)
                {
                    try
                    {
                        //Write break for file open
                        if(szOpen != null)
                            m_oFile.write(szOpen.toString());
                        //Write context
                        m_oFile.write(szBuf.toString());
                        //Flush file
                        m_oFile.flush();
                    }
                    catch(IOException ioe)
                    {
                        android.util.Log.e("Log", "[log] Write file failed", ioe);
                    }
                }
            }
        }
    }
    
    /**Get class name.
     * @param oInfo [in] Call stack information array, this parameter should be retrieved by 
     *              Thread.getStackTrace().
     * @return      Return pure class name of last stack.*/
    private static String getClassName(StackInfo oInfo)
    {
        String  returnObj = null;
        if((oInfo != null) && (oInfo.m_iTarget >= 0))
        {
            returnObj = oInfo.m_oStkInfo[oInfo.m_iTarget].getClassName().substring(oInfo.m_oStkInfo[oInfo.m_iTarget].getClassName().lastIndexOf(".") + 1);
        }
        return returnObj;
    }
    
    /**Get message text.
     * @param oInfo [in] Call stack information array, this parameter should be retrieved by 
     *              Thread.getStackTrace().
     * @param szMsg [in] Message for logged.
     * @param oArgs [in] Arguments referenced by the format specifiers in szMsg.*/
    private static String getMessageText(StackInfo oInfo, String szMsg, Object... oArgs)
    {
        StringBuffer    returnObj = new StringBuffer();
        try
        {
	        if((oInfo != null) && (oInfo.m_iTarget >= 0))
	        {
	            returnObj.append('[');
	            returnObj.append(oInfo.m_oStkInfo[oInfo.m_iTarget].getMethodName());
	            returnObj.append("] ");
	        }
	        returnObj.append(String.format(szMsg, oArgs));
        }
        catch (Exception ex)
        {
        }
        return returnObj.toString();
    }
    
    /**Stack information.*/
    private static class StackInfo
    {
        /**Class name of stack information.*/
        protected static final String   CLASS_NAME = StackInfo.class.getName();
        /**Target element's index in general.*/
        protected static int            m_iIdx = -1;
        /**Call stack information array.*/
        protected StackTraceElement[]   m_oStkInfo = null;
        /**Index of target element in m_oStkInfo.*/
        protected int                   m_iTarget = -1;
        
        /**Generate stack information.*/
        protected static StackInfo currentStackInfo()
        {
            StackInfo   returnObj = new StackInfo();
            //Get call stack 
            returnObj.m_oStkInfo = Thread.currentThread().getStackTrace();
            //Find target element
            if((m_iIdx < 0) && (returnObj.m_oStkInfo != null))
            {
                for(int a = 0; a < returnObj.m_oStkInfo.length; a ++)
                {
                    if(returnObj.m_oStkInfo[a].getClassName().equals(CLASS_NAME))
                    {
                        m_iIdx = a + 3; //One for and log function (such as Log.d, Log.w, etc.), one for StackInfo.currentStackInfo()
                        break;
                    }
                }
            }
            returnObj.m_iTarget = (((returnObj.m_oStkInfo != null) && (returnObj.m_oStkInfo.length > m_iIdx))? m_iIdx: -1);
            return returnObj;
        }
    }
}