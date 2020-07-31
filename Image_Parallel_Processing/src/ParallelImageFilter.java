import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;

public class ParallelImageFilter extends ImageFilter {

    public ParallelImageFilter(String image) {
        super(image);
    }

    @Override
    public void applyMedianFilter() {
        long start = System.nanoTime();
		//////////////////////Fork join pool 선언 및 execute / join 활용	///////////////////////////////////////////////////////
        ForkJoinPool pool = new ForkJoinPool(Runtime.getRuntime().availableProcessors()); 
        
		ForkJoinAction action = new ForkJoinAction(image, filter_image, 0,height, 0, width);
		pool.execute(action);
		
		action.join();
		pool.shutdown();
		/////////////////////////////////////////////////////////////////////////////////////
        long end = System.nanoTime();
        System.out.println("Parallel Image Filter Median Filter Time : "+(end-start)/1000000 + " ms");
    }

    private static class ForkJoinAction extends RecursiveAction {
        int[][] image;
        int[][] filter_image;
        private int start_x, end_x, start_y, end_y;

        ForkJoinAction(int[][] image, int[][] filter_image, int start_x, int end_x, int start_y, int end_y) {
            this.image = image;
            this.filter_image = filter_image;
            this.start_x = start_x;
            this.end_x = end_x;
            this.start_y = start_y;
            this.end_y = end_y;
        }

		/////// 위의 applyMedianFilter에서 execute 함수 사용시 작동하는 함수
		/////// 최소 단위(가로 * 세로의 값이 32)보다 작으면 중간 값 필터링 이용(ImageFilter.java의 applyMedianFilter 코드 이용)
		/////// 최소 단위보다 클 시 divideTask() 호출
        @Override
        protected void compute() {
        	
        	int size = (end_x-start_x)*(end_y-start_y);
        	if(size > 32) {
        		List<ForkJoinAction> subtasks = divideTask();
        		subtasks.get(0).fork();
        		subtasks.get(1).compute();
        		subtasks.get(0).join();
        	}else {

        		for (int i = start_x; i < end_x; ++i) {
                    for (int j = start_y; j < end_y; ++j) {
                        int rgb = getMedianValue(image, i, j);
                        filter_image[i][j] = rgb;
                    }
                }
        	}
  
        }
        ////// subtask 생성에 대한 부분 정의하는 곳 (subtaks.add 활용)
 		////// 세로가 가로보다 큰 경우, 세로를 반 나누어 subtask 내의 작업 생성
 		////// 가로가 세로보다 큰 경우, 가로를 반 나누어 subtask 내의 작업 생성
        private List<ForkJoinAction> divideTask() {

        	List<ForkJoinAction> subTasks = new ArrayList<>();
        	int height = end_y-start_y;
        	int width = end_x-start_x;
        	
        	if(height > width) {
        		int mid_y = (end_y+start_y)/2;
        		subTasks.add(new ForkJoinAction(image, filter_image, start_x, end_x, start_y, mid_y));
        		subTasks.add(new ForkJoinAction(image, filter_image, start_x, end_x, mid_y, end_y));
        	}else {
        		int mid_x = (end_x+start_x)/2;
        		subTasks.add(new ForkJoinAction(image, filter_image, start_x, mid_x, start_y, end_y));
        		subTasks.add(new ForkJoinAction(image, filter_image, mid_x, end_x, start_y, end_y));
        	}
        	
        	return subTasks;

        }
    }
}
