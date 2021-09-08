package com.dyt.wcc.cameracommon.widget;

/**
 * 触碰的区域是由两个点构成：
 * 起点：TouchPoint touchPoint1
 *  终止点：TouchPoint touchPoint2
 *  indexOfArea： 编号索引，用于删除
 *  isSelected：  是否被选中，用于移动
 */
public class TouchArea {
    public TouchPoint touchPoint1;
    public TouchPoint touchPoint2;
    public int         indexOfArea;
    public boolean isSelected = false;

}
