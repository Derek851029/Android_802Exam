/*
 * Created by Ulysses 2008, Jun 1
 */
package com.cyberon.dspotterutility;

import java.lang.reflect.Field;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Build;
import android.util.Log;


public class ToolKit
{
	private static final String LOG_TAG = "ToolKit";
	private static int nOSVer = Integer.MAX_VALUE;
	
	public static final int getOSVer()
	{
		if (nOSVer == Integer.MAX_VALUE)
		{
			int n = Integer.MAX_VALUE;
			
			try
			{
				//n = Build.VERSION.SDK_INT;	//Need 1.6 and up
				Field oField = android.os.Build.VERSION.class.getDeclaredField("SDK_INT");
				n = oField.getInt(null);
			}
			catch (Exception ex)
			{
				//For 1.0 ~ 1.5
				try
				{
					n = Integer.valueOf(Build.VERSION.SDK);
				}
				catch (Exception e)
				{
					Log.w(LOG_TAG, "Build.VERSION.SDK = " + Build.VERSION.SDK + " is not integer.");
				}
			}

			switch (n)
			// switch(Build.VERSION.SDK_INT)
			{
			case 1:// Build.VERSION_CODES.BASE:
				nOSVer = 100;
				break;
			case 2:// Build.VERSION_CODES.BASE_1_1:
				nOSVer = 110;
				break;
			case 3:// Build.VERSION_CODES.CUPCAKE:
				nOSVer = 150;
				break;
			case 4:// Build.VERSION_CODES.DONUT:
				nOSVer = 160;
				break;
			case 5:// Build.VERSION_CODES.ECLAIR:
				nOSVer = 200;
				break;
			case 6: // Build.VERSION_CODES.ECLAIR_0_1
				nOSVer = 201;
				break;
			case 7: // Build.VERSION_CODES.ECLAIR_MR1
				nOSVer = 210;
				break;
			case 8: // Build.VERSION_CODES.FROYO
				nOSVer = 220;
				break;
			case 9: // Build.VERSION_CODES.GINGERBREAD
				nOSVer = 230;
				break;
			case 10: // Build.VERSION_CODES.GINGERBREAD_MR1
				nOSVer = 233;
				break;
			case 11: // Build.VERSION_CODES.HONEYCOMB
				nOSVer = 300;
				break;
			case 12: // Build.VERSION_CODES.HONEYCOMB_MR1
				nOSVer = 310;
				break;
			case 13: // Build.VERSION_CODES.HONEYCOMB_MR2
				nOSVer = 320;
				break;
			case 14: // Build.VERSION_CODES.ICE_CREAM_SANDWICH
				nOSVer = 400;
				break;
			case 15: // Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1
				nOSVer = 403;
				break;
			case 16: // Build.VERSION_CODES.JELLY_BEAN
				nOSVer = 410;
				break;
			case 17: // Build.VERSION_CODES.JELLY_BEAN_MR1
				nOSVer = 420;
				break;
			default:
				Log.w(LOG_TAG, "Newer SDK version: " + n);
			}
		}
		
		return nOSVer;
	}
	
	public static String getNativeLibPath(Context oContext)
	{
		String sLibPath = oContext.getApplicationInfo().dataDir + "/lib";
		try
		{
			if (getOSVer() >= 230)
			{
				ApplicationInfo oApplicationInfo = oContext.getApplicationInfo();
				Class<?> c = Class.forName("android.content.pm.ApplicationInfo");
				Field oField = c.getDeclaredField("nativeLibraryDir");
				sLibPath = oField.get(oApplicationInfo).toString();
			}
		}
		catch (Exception ex)
		{
		}

		return sLibPath;
	}

	public static void sleep(int nMiliSecond)
	{
		try
		{
			Thread.sleep(nMiliSecond);
		}
		catch (Exception e)
		{
			Log.e(LOG_TAG, e.toString());
		}
	}
}
