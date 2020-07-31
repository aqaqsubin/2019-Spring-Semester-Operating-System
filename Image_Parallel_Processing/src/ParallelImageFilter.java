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
		//////////////////////Fork join pool ���� �� execute / join Ȱ��	///////////////////////////////////////////////////////
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

		/////// ���� applyMedianFilter���� execute �Լ� ���� �۵��ϴ� �Լ�
		/////// �ּ� ����(���� * ������ ���� 32)���� ������ �߰� �� ���͸� �̿�(ImageFilter.java�� applyMedianFilter �ڵ� �̿�)
		/////// �ּ� �������� Ŭ �� divideTask() ȣ��
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
        ////// subtask ������ ���� �κ� �����ϴ� �� (subtaks.add Ȱ��)
 		////// ���ΰ� ���κ��� ū ���, ���θ� �� ������ subtask ���� �۾� ����
 		////// ���ΰ� ���κ��� ū ���, ���θ� �� ������ subtask ���� �۾� ����
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
