package com.dyt.wcc.dytpir.utils;

import android.content.Context;
import android.graphics.Bitmap;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class CreateBitmap {

    /*
    private int[] readLocalFile(Context context,String name) throws IOException {

        return data;
    }

     */

    private int[] toByteArray(InputStream in) throws IOException {
        int[] buffer = new int[256 * 3];
        int[] colors = new int[256];
        int n = 0,i = 0;
        while ((n = in.read())!= -1) {
            buffer[i++]=n;
        }
        for(int j=0;j<256;j++){
            int r=buffer[3*j];
            int g=buffer[3*j+1];
            int b=buffer[3*j+2];
            int a=255;
            colors[255-j]=(a<<24)|(r<<16)|(g<<8)|(b);
        }
        return colors;
    }


    public Bitmap GenerateBitmap(Context context,String filename) throws IOException {
        InputStream inputStream = context.getAssets().open(filename);
        int[] data = toByteArray(inputStream);
        inputStream.close();
        Bitmap resultbmp = Bitmap.createBitmap(data,0,1,1,256, Bitmap.Config.ARGB_8888);
        return resultbmp;

    }

    /**
     * @param bitmap  需要保存的图片对象
     * @param picPath 需要保存的本地路径，建议设置为公用常量
     */

    public void saveBitmap(Bitmap bitmap, String picPath, String picname) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(picPath+picname);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != fos) {
                try {
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
