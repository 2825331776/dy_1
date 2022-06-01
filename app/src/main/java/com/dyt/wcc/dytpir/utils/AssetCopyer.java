/**
 * copy the files and folders of assets to sdCard to ensure that we can read files in JNI part
 */
package com.dyt.wcc.dytpir.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class AssetCopyer {
	private static String TAG = "AssetCopyer";

	/**
	 * copy all the files and folders to the destination
	 *
	 * @param context     application context
	 * @param destination the destination path
	 */

	public static void copyAllAssets (Context context, String destination) {
		copyAssetsToDst(context, "", destination);
	}

	public static boolean checkPaletteFile (Context context, String[] res) {
		if (res != null) {
			//+File.separator +res[0]
			//            Log.e(TAG, "checkPaletteFile: " + context.getFilesDir().getAbsolutePath());
			String[] filesStr = context.fileList();
			//            Log.e(TAG, "checkPaletteFile:  res [0] " + res[0]);
			//            Log.e(TAG, "checkPaletteFile: " + filesStr.length);
			for (String f1 : filesStr) {
				//                    Environment.getExternalStorageState(file);
				Log.e(TAG, "checkPaletteFile: private dirs File name  ; === " + f1);
				if (res[0].equals(f1)) {
					return true;
				}
			}

		}
		return false;
	}

	/**
	 * @param context :application context
	 * @param srcPath :the path of source file
	 * @param dstPath :the path of destination
	 */

	private static void copyAssetsToDst (Context context, String srcPath, String dstPath) {
		//        Log.e(TAG, "copyAssetsToDst:srcPath ======> " + srcPath);
		//        Log.e(TAG, "copyAssetsToDst:dstPath ======> " + dstPath);
		try {
			String fileNames[] = context.getAssets().list(srcPath);
			if (fileNames.length > 0) {
				File file = new File(dstPath);
				file.mkdirs();
				for (String fileName : fileNames) {
					//                    Log.e(TAG, "copyAssetsToDst: fileName======== > " + fileName);
					if ("".equals(srcPath) && fileName.contains("dat") || (fileName.contains("configs")) || (fileName.contains(".pdf"))) {
						//                        Log.e(TAG, "copyAssetsToDst: fileName===NO=endsWith=dat=== > " + fileName);
						//                        copyAssetsToDst(context,srcPath + "/" + fileName,dstPath+"/"+fileName);
						//                    }else{
						//                        Log.e(TAG, "copyAssetsToDst: fileName=====endsWith=dat====== > " + fileName);
						copyAssetsToDst(context, fileName, dstPath + "/" + fileName);
					}
				}
			} else {
				InputStream is = context.getAssets().open(srcPath);
				FileOutputStream fos = new FileOutputStream(new File(dstPath));
				byte[] buffer = new byte[1024];
				int byteCount = 0;
				while ((byteCount = is.read(buffer)) != -1) {
					fos.write(buffer, 0, byteCount);
				}
				fos.flush();//刷新缓冲区
				is.close();
				fos.close();
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
