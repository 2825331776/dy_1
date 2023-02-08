package com.dytest.wcc.cameracommon.factory;

import com.dytest.wcc.cameracommon.entity.DataBlock;
import com.dytest.wcc.cameracommon.entity.IDATBlock;
import com.dytest.wcc.cameracommon.entity.IENDBlock;
import com.dytest.wcc.cameracommon.entity.IHDRBlock;
import com.dytest.wcc.cameracommon.entity.PHYSBlock;
import com.dytest.wcc.cameracommon.entity.PLTEBlock;
import com.dytest.wcc.cameracommon.entity.Png;
import com.dytest.wcc.cameracommon.entity.SRGBBlock;
import com.dytest.wcc.cameracommon.entity.TEXTBlock;
import com.dytest.wcc.cameracommon.entity.TRNSBlock;
import com.dytest.wcc.cameracommon.utils.BlockUtil;
import com.dytest.wcc.cameracommon.utils.ByteUtil;

import java.io.IOException;
import java.io.InputStream;

public class BlockFactory {

	public static DataBlock readBlock (InputStream in, Png png, DataBlock dataBlock) throws IOException {
		String hexCode = ByteUtil.byteToHex(dataBlock.getChunkTypeCode(), 0, dataBlock.getChunkTypeCode().length);
		hexCode = hexCode.toUpperCase();
		DataBlock realDataBlock = null;
		if (BlockUtil.isIHDR(hexCode)) {
			//IHDR数据块
			realDataBlock = new IHDRBlock();
		} else if (BlockUtil.isPLTE(hexCode)) {
			//PLTE数据块
			realDataBlock = new PLTEBlock();
		} else if (BlockUtil.isIDAT(hexCode)) {
			//IDAT数据块
			realDataBlock = new IDATBlock();
		} else if (BlockUtil.isIEND(hexCode)) {
			//IEND数据块
			realDataBlock = new IENDBlock();
		} else if (BlockUtil.isSRGB(hexCode)) {
			//sRGB数据块
			realDataBlock = new SRGBBlock();
		} else if (BlockUtil.istEXt(hexCode)) {
			//tEXt数据块
			realDataBlock = new TEXTBlock();
		} else if (BlockUtil.isPHYS(hexCode)) {
			//pHYs数据块
			realDataBlock = new PHYSBlock();
		} else if (BlockUtil.istRNS(hexCode)) {
			//tRNS数据块
			realDataBlock = new TRNSBlock();
		} else {
			//其它数据块
			realDataBlock = dataBlock;
		}
		realDataBlock.setLength(dataBlock.getLength());
		realDataBlock.setChunkTypeCode(dataBlock.getChunkTypeCode());
		//读取数据,这里的测试版做法是： 把所有数据读取进内存来
		int len = -1;
		int dataLength = ByteUtil.highByteToInt(dataBlock.getLength());
		byte[] data = new byte[dataLength];
		len = in.read(data, 0, dataLength);
		realDataBlock.setData(ByteUtil.cutByte(data, 0, len));
		return realDataBlock;
	}

}
