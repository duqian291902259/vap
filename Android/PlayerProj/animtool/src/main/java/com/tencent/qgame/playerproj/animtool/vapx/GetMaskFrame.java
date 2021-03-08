package com.tencent.qgame.playerproj.animtool.vapx;

import com.tencent.qgame.playerproj.animtool.CommonArg;
import com.tencent.qgame.playerproj.animtool.CommonArgTool;
import com.tencent.qgame.playerproj.animtool.TLog;
import com.tencent.qgame.playerproj.animtool.data.PointRect;

import com.tencent.qgame.playerproj.animtool.vapx.FrameSet.Frame;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

/**
 * 获取融合动画遮罩
 */
public class GetMaskFrame {

    private static final String TAG = "GetMaskFrame-dq";

    public FrameSet.FrameObj getFrameObj(int frameIndex, CommonArg commonArg, int[] outputArgb) throws Exception {
        //TLog.i("dq-av","frameIndex="+frameIndex+",commonArg="+commonArg);
        FrameSet.FrameObj frameObj = new FrameSet.FrameObj();
        frameObj.frameIndex = frameIndex;

        FrameSet.Frame frame;
        // 需要放置的位置
        int x;
        int y;
        int gap = commonArg.gap;
        if (commonArg.isVLayout) {
            x = commonArg.alphaPoint.w + gap;
            y = commonArg.alphaPoint.y;
        } else {
            x = commonArg.alphaPoint.x;
            y = commonArg.alphaPoint.h + gap;
        }
        int startX = x;
        int lastMaxY = y;
        for (int i=0; i<commonArg.srcSet.srcs.size(); i++) {
            frame = getFrame(frameIndex, commonArg.srcSet.srcs.get(i), outputArgb, commonArg.outputW, commonArg.outputH, x, y, startX, lastMaxY);
            if (frame == null) continue;
            // 计算下一个遮罩起点
            x = frame.mFrame.x + frame.mFrame.w + gap;
            y = frame.mFrame.y;
            int newY = frame.mFrame.y + frame.mFrame.h + gap;
            if (newY > lastMaxY) {
                lastMaxY = newY;
            }

            frameObj.frames.add(frame);
        }

        if (frameObj.frames.isEmpty()) {
            return null;
        }
        return frameObj;
    }


    private FrameSet.Frame getFrame(int frameIndex, SrcSet.Src src, int[] outputArgb, int outW, int outH, int x, int y, int startX, int lastMaxY) throws Exception {
        File inputFile = new File(src.srcPath  + String.format("%03d", frameIndex)+".png");
        if (!inputFile.exists()) {
            return null;
        }

        BufferedImage inputBuf = ImageIO.read(inputFile);
        int maskW = inputBuf.getWidth();
        int maskH = inputBuf.getHeight();
        //TLog.i(TAG, "frameIndex=" + frameIndex + ",maskW=" + maskW + ",maskH=" + maskH);

        int[] maskArgb = inputBuf.getRGB(0, 0, maskW, maskH, null, 0, maskW);

        FrameSet.Frame frame = new FrameSet.Frame();
        frame.srcId = src.srcId;
        frame.z = src.z;

        //从图片中获取遮罩的位置
        frame.frame = getSrcFramePoint(maskArgb, maskW, maskH,frame);
        //TLog.i(TAG, "frameIndex=" + frameIndex + ",frame.frame=" +frame.frame+",mAlpha="+frame.mAlpha);

        if (frame.frame == null) {
            // 有文件，但内容是空
            return null;
        }

        //这里取颜色才是对的
        PointRect maskPoint = new PointRect(
                frame.frame.x,
                frame.frame.y,
                frame.frame.w,
                frame.frame.h
        );

        //等比例缩小遮罩的坐标位置
        frame.frame.x = (int) (frame.frame.x * CommonArgTool.VIDEO_SCALE_RATIO);
        frame.frame.y = (int) (frame.frame.y * CommonArgTool.VIDEO_SCALE_RATIO);
        frame.frame.w = (int) (frame.frame.w * CommonArgTool.VIDEO_SCALE_RATIO);
        frame.frame.h = (int) (frame.frame.h * CommonArgTool.VIDEO_SCALE_RATIO);

        //TLog.i(TAG, "frameIndex=" + frameIndex + ",frame.frame=" +frame.frame);


        //处理遮罩实际的坐标点
        x = frame.frame.x + CommonArgTool.MIN_GAP + outW / 2;
        y = frame.frame.y;


        PointRect mFrame = new PointRect(x, y, frame.frame.w, frame.frame.h);
        //TLog.i(TAG, "frameIndex=" + frameIndex + ",maskPoint=" + maskPoint + ",mFrame1=" + mFrame);

        /*
        // 计算是否能放下遮罩
        if (mFrame.x + mFrame.w > outW) { // 超宽换行
            mFrame.x = startX;
            mFrame.y = lastMaxY;
            if (mFrame.x + mFrame.w > outW) {
                // 超长后缩放mask
                float scale = (outW - mFrame.x) * 1f / mFrame.w;

                mFrame.w = outW - mFrame.x;
                mFrame.h = (int) (mFrame.h * scale);

                // 设置缩放区域
                maskPoint.x = (int) (maskPoint.x * scale);
                maskPoint.y = (int) (maskPoint.y * scale);
                maskPoint.h = mFrame.h;
                maskPoint.w = mFrame.w;

                maskArgb = scaleMask(scale, inputBuf);

                TLog.w(TAG, "frameIndex=" + frameIndex + ",src=" + src.srcId + ", no more space for(w)" + mFrame + ",scale=" + scale);
            }
        }
        if (mFrame.y + mFrame.h > outH) { // 高度不够直接错误
            TLog.e(TAG, "frameIndex=" + frameIndex + ",src=" + src.srcId + ", no more space(h)" + mFrame);
            return null;
        }

        */
        //TLog.i(TAG, "frameIndex=" + frameIndex + ",mFrame2=" + frame.mFrame);
        frame.mFrame = mFrame;
        //try {
        //fillMaskToOutput(outputArgb, outW, maskArgb, maskW, maskPoint, frame.mFrame,frame);
        /*} catch (Exception e) {
            e.printStackTrace();
            TLog.e(TAG, "dq-av error=" + e);
        }*/

        // 设置src的w,h 取所有遮罩里最大值
        /*synchronized (GetMaskFrame.class) {
            // 只按宽度进行判断防止横跳
            if (frame.frame.w > src.w) {
                src.w = frame.frame.w;
                src.h = frame.frame.h;//dq-modified:不取mFrame里面的
            }
        }*/
        return frame;
    }

    private void fillMaskToOutput(int[] outputArgb, int outW,
                                  int[] maskArgb, int maskW,
                                  PointRect maskPoint,
                                  PointRect mFrame, Frame frame1) {
        int maskColor2 = 0;

        for (int y=0; y < maskPoint.h; y++) {
            for (int x=0; x < maskPoint.w; x++) {
                int maskXOffset = maskPoint.x;
                int maskYOffset = maskPoint.y;
                // 先从遮罩 maskArgb 取色
                int maskColor = maskArgb[x + maskXOffset + (y + maskYOffset) * maskW];
                // 黑色部分不遮挡，红色部分被遮挡
                int alpha = maskColor >>> 24;
                int maskRed = (maskColor & 0x00ff0000) >>> 16;
                int redAlpha = 255 - maskRed; // 红色部分算遮挡
                alpha = (int) ((redAlpha / 255f) * (alpha / 255f) * 255f);
                // 最终color
                int color = 0xff000000 + (alpha << 16) + (alpha << 8) + alpha;
                maskColor2 = color;
                // 将遮罩颜色放置到视频中对应区域
                int outputXOffset = mFrame.x;
                int outputYOffset = mFrame.y;
                outputArgb[x + outputXOffset + (y + outputYOffset) * outW] = color;
            }
        }
        /*float[] transColor = transColor(maskColor2);
        float alpha1 = transColor[1];
        float alpha2 = transColor[2];
        float alpha3 = transColor[3];
        TLog.i("dq-av", "maskColor2=" + maskColor2 + ",alpha=" + alpha1 + ",alpha2=" + alpha2 + ",alpha3=" + alpha3);
        //frame1.mAlpha = (int) (alpha1*255);
        frame1.mAlpha = alpha1;*/
    }


    /**
     * 缩放遮罩
     */
    private int[] scaleMask(float scale, BufferedImage inputBuf) {
        AffineTransform at = new AffineTransform();
        at.scale(scale, scale);

        int w = inputBuf.getWidth();
        int h = inputBuf.getHeight();
        BufferedImage alphaBuf = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        AffineTransformOp scaleOp = new AffineTransformOp(at, AffineTransformOp.TYPE_BILINEAR);
        alphaBuf = scaleOp.filter(inputBuf, alphaBuf);

        return alphaBuf.getRGB(0, 0, w, h, null, 0, w);
    }

    /**
     * 获取遮罩位置信息 并转换为黑白
     */
    private PointRect getSrcFramePoint(int[] maskArgb, int w, int h,
        Frame frame) {

        PointRect point = new PointRect();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = 0;
        int maxY = 0;

        int maskColor = 0;
        for (int y=0; y<h; y++) {
            for (int x = 0; x < w; x++) {
                int alpha = maskArgb[x + y*w] >>> 24;
                /*if (maskColor==0){
                    maskColor = getAlpha(alpha);
                }*/
                if (alpha > 0) {
                    //by-dq:假设有透明度变化，遮罩的透明度必须一致，只取一个透明度
                    //frame.mAlpha = alpha;

                    if (x < minX) minX = x;
                    if (y < minY) minY = y;
                    if (x > maxX) maxX = x;
                    if (y > maxY) maxY = y;
                }
            }
        }
        //取到的颜色值都是-16777216？
        /*float[] transColor = transColor(maskColor);
        float alpha1 = transColor[0];
        float alpha2 = transColor[1];
        TLog.i("dq-av", "maskColor1=" + maskColor + ",alpha=" + alpha1 + ",alpha2=" + alpha2);*/

        point.x = minX;
        point.y = minY;
        point.w = maxX - minX + 1;
        point.h = maxY - minY + 1;
        if (point.w <=0 || point.h <= 0) return null;

        return point;

    }

    private int getAlpha(int color) {
        //int alpha = color >>> 24;
        int alpha =color;
        // r = g = b
        return 0xff000000 + (alpha << 16) + (alpha << 8) + alpha;
    }


    private final float[] transColor(int color) {
        float[] argb = new float[]{(float) (color >>> 24 & 255) / 255.0F, (float) (color >>> 16 & 255) / 255.0F,
            (float) (color >>> 8 & 255) / 255.0F, (float) (color & 255) / 255.0F};
        return argb;
    }
}
