import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class ImageFilter {
    int[][] image;
    int[][] filter_image;
    int width;
    int height;

    public ImageFilter(String path) {
    	
    	this.image = null;
        try {
        	
            File file = new File(Paths.get(path).toString());
            BufferedImage bufferedImage = ImageIO.read(file);
            Raster raster = bufferedImage.getRaster();
            width = raster.getWidth();
            height = raster.getHeight();

            image = new int[height][width];
            filter_image = new int[height][width];
 
            int[] temp = raster.getPixels(0, 0, width, height, (int[])null);

            // Convert 1d Array to 2d array
            for (int i = 0; i < height; ++i)
                System.arraycopy(temp, i*width, image[i], 0, width);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void applyMedianFilter() {
        long start = System.nanoTime();
        for (int i = 0; i < height; ++i) {
            for (int j = 0; j < width; ++j) {
                int rgb = getMedianValue(image, i, j);
                filter_image[i][j] = rgb;
            }
        }
        long end = System.nanoTime();
        System.out.println("Image Filter Median Filter Time : "+(end-start)/1000000 + " ms");
    }

    public void writeFilterImage() {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        int[] temp = new int[height * width];
        for (int i = 0; i < height; ++i)
            System.arraycopy(this.filter_image[i], 0, temp, i*width, width);
        image.setRGB(0, 0, width, height, temp, 0, width);

        File out = new File(Paths.get(".", "median_image.png").toString());
        try {
            ImageIO.write(image, "png", out);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static int getMedianValue(int[][] image, int y, int x) {
        ArrayList<Integer> list = new ArrayList<>();
        for (int i = -5; i <= 5; ++i) {
            for (int j = -5; j <= 5; ++j) {
                if (x + j < 0 || x + j >= image[y].length ||
                        y + i < 0 || y + i >= image.length)
                    continue;

                list.add(image[y+i][x+j]);
            }
        }

        int median = findNth(list.stream().mapToInt(Integer::intValue).toArray(), 0, list.size()-1,list.size()/2);

        int r = (int)(median / 0.2126);
        r = r > 255 ? 255 : r;
        int g= (int)(median / 0.7152);
        g = g > 255 ? 255 : g;
        int b = (int)(median / 0.0722);
        b = b > 255 ? 255 : b;

        return r*0x10000 + g*0x100 + b; // R  G  B
    }

    static int findNth(int[] array, int start, int end, int nth) {
        int n = array[end];
        int s, b;
        for (s = start, b = end; s < b;) {
            if (array[s] < n) {
                ++s;
            } else {
                int temp = array[s];
                array[s] = array[--b];
                array[b] = temp;
            }
        }
        array[end] = array[b];
        array[b] = n;

        if (s == nth)
            return array[s];
        if (s < nth)
            return findNth(array, s+1, end, nth);
        return findNth(array, start, s-1, nth);
    }
}
