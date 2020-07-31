import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) {
    	
//    	System.out.println("image filter");
//    	ImageFilter filter = new ImageFilter(Paths.get("gray_earth.png").toString());
//        filter.applyMedianFilter();
//        filter.writeFilterImage();
        
        
        System.out.println("parallel image filter");
    	ParallelImageFilter filter2 = new ParallelImageFilter(Paths.get("gray_earth.png").toString());
        filter2.applyMedianFilter();
        filter2.writeFilterImage();
        
        
    }
}
