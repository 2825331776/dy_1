package com.dyt.wcc.cameracommon.entity;

/**
 * @author yp2
 * @date 2015-11-18
 * description png头部信息
 */
public class PngHeader {

	/**
	 * png文件头部信息，固定,8个字节
	 */
	private byte[] flag;

	public PngHeader () {
		flag = new byte[8];
	}

	public byte[] getFlag () {
		return flag;
	}

	public void setFlag (byte[] flag) {
		this.flag = flag;
	}


}
