package garbageClassfication;

import com.intel.analytics.zoo.common.NNContext;
import com.intel.analytics.zoo.feature.image.ImageSet;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;

/**
 * <p>
 * 创建时间为 09:19 2019-09-02
 * 项目名称 wiki-edits
 * </p>
 *
 * @author wj
 * @version 0.0.1
 * @since 0.0.1
 */

public class ImagePreProcess {

    static float[] byteArrayToFloatArray(byte[] buffer) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(buffer));
        BufferedImage resizeImage = new BufferedImage(224,
                224, image.getType());
        Graphics2D g2d = resizeImage.createGraphics();
        g2d.drawImage(image, 0, 0, 224, 224, null);
        g2d.dispose();

//        BufferedImage cleanImage = cleanImage(resizeImage);
//        Raster raster = cleanImage.getData();


        Raster raster = resizeImage.getData();
        float[] temp = new float[raster.getWidth() * raster.getHeight() * raster.getNumBands()];
        return raster.getPixels(0,0, raster.getWidth(), raster.getHeight(),temp);
    }


    private static BufferedImage cleanImage(BufferedImage bufferedImage) {

        int h = bufferedImage.getHeight();
        int w = bufferedImage.getWidth();

        // 灰度化
        int[][] gray = new int[w][h];
        for (int x = 0; x < w; x++){
            for (int y = 0; y < h; y++){
                int argb = bufferedImage.getRGB(x, y);
                // 图像加亮（调整亮度识别率非常高）
                int r = (int) (((argb >> 16) & 0xFF) * 1.1 + 30);
                int g = (int) (((argb >> 8) & 0xFF) * 1.1 + 30);
                int b = (int) (((argb >> 0) & 0xFF) * 1.1 + 30);
                if (r >= 255){
                    r = 255;
                }
                if (g >= 255){
                    g = 255;
                }
                if (b >= 255){
                    b = 255;
                }
                gray[x][y] = (int) Math.pow((Math.pow(r, 2.2) * 0.2973 + Math.pow(g, 2.2)* 0.6274 + Math.pow(b, 2.2) * 0.0753), 1 / 2.2);
            }
        }

        // 二值化
        int threshold = ostu(gray, w, h);
        BufferedImage binaryBufferedImage = new BufferedImage(w, h,BufferedImage.TYPE_BYTE_BINARY);
        for (int x = 0; x < w; x++){
            for (int y = 0; y < h; y++){
                if (gray[x][y] > threshold){
                    gray[x][y] |= 0x00FFFF;
                } else{
                    gray[x][y] &= 0xFF0000;
                }
                binaryBufferedImage.setRGB(x, y, gray[x][y]);
            }
        }
        return binaryBufferedImage;
    }

    private static int ostu(int[][] gray, int w, int h){
        int[] histData = new int[w * h];
        // Calculate histogram
        for (int x = 0; x < w; x++){
            for (int y = 0; y < h; y++){
                int red = 0xFF & gray[x][y];
                histData[red]++;
            }
        }

        // Total number of pixels
        int total = w * h;

        float sum = 0;
        for (int t = 0; t < 256; t++) {
            sum += t * histData[t];
        }
        float sumB = 0;
        int wB = 0;
        int wF = 0;

        float varMax = 0;
        int threshold = 0;

        for (int t = 0; t < 256; t++){
            // Weight Background
            wB += histData[t];
            if (wB == 0) {
                continue;
            }
            // Weight Foreground
            wF = total - wB;
            if (wF == 0) {
                break;
            }
            sumB += (float) (t * histData[t]);
            // Mean Background
            float mB = sumB / wB;
            // Mean Foreground
            float mF = (sum - sumB) / wF;

            // Calculate Between Class Variance
            float varBetween = (float) wB * (float) wF * (mB - mF) * (mB - mF);

            // Check if new maximum found
            if (varBetween > varMax){
                varMax = varBetween;
                threshold = t;
            }
        }

        return threshold;
    }


    public static void gray(String srcImageFile, String destImageFile) {
        try {
            BufferedImage src = ImageIO.read(new File(srcImageFile));
            ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
            ColorConvertOp op = new ColorConvertOp(cs, null);
            src = op.filter(src, null);
            ImageIO.write(src, "JPEG", new File(destImageFile));
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
