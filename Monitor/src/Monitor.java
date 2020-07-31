class Producer extends Thread { //������ Ŭ����
	Monitor monitor;
	String alpha = "abcdefghijklmnopqrstuvwxyz";
	int sleepTime = 200;

	public Producer(Monitor m) {
		monitor = m;
	}
	public void run() {
		char c;
		for (int i = 0; i < 10; i++) {
		
			c = alpha.charAt((int)(Math.random() * 25)); //���ĺ��� �����͸� ���Ƿ� ��� ����
			monitor.add(c); //MonitorŬ������ add()�޼ҵ带 ȣ���Ͽ� �迭�� ����

			System.out.println(" ���� -----> " + c + " ���� ���� " + (monitor.savedNum()));

			try {
				Thread.sleep((int)(Math.random() * sleepTime));
				sleepTime = (int)(Math.random() * 1000);
			} catch (InterruptedException ex) { }
		}
	}
}

class Customer extends Thread { //�Һ��� Ŭ����
	Monitor monitor;
	int sleepTime = 500;

	public Customer(Monitor m) {
		monitor = m;
	}

	public void run() {
		char c;
		for (int i = 0; i < 10; i++) {
			c = (monitor.remove()); //MonitorŬ������ remove()�޼ҵ带 ȣ���Ͽ� �迭���� ���� ���Ͻ�Ű�� ����
			System.out.println(" ? <----- " + c + " ���� ���� " + (monitor.savedNum()));
			try {
				Thread.sleep((int)(Math.random() * sleepTime));
				sleepTime += (int)(Math.random() * 1000);
			} catch (InterruptedException ex) { }
		}
	}
}

public class Monitor {
 
	char[] buffer = new char[5]; //�������
	int removeNext = 0; //���ﶧ ������� index ����Ű�� ��
	int addNext = 0; ////���� �Ҷ� ������� index ����Ű�� ��
	boolean isFull = false;
	boolean isEmpty = true;

	int[] new_buffer = new int[7];
	//addNext�� removeNext�� ����� �� ����ȭ�� ���� ��ü�� �ϳ� �����Ͽ���. index 5�� addNext, index 6�� removeNext���̴�.
	//addNext�� removeNext�� ����ؼ� �����ϸ�, ���ۿ� ������ ���� buffer�� ���̷� ���� ���������� �ε����� �Ͽ� �����Ͽ�,
	//addNext-removeNext�� ���� �����̴�.
	public void add(char c) {
		synchronized(new_buffer) {  
			
				if(isFull) {//���۰� ���� á�� ��, 
					try {
						//dead lock�� �����ϱ� ���� ���۰� ���� �� �� �̻� write�� �� ���� ��Ȳ�� �Ǹ� ���� ��ü�� �ɾ���Ҵ� lock�� �����ְ�, �ٸ� �����尡 �����ֱ� ����Ѵ�.
						new_buffer.wait();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
					new_buffer[((addNext++)%buffer.length)]=c; // write�� �� �ִ� ��Ȳ�� �Ǹ� ���ۿ� ���� c�� ������ ��, addNext�� +1�Ѵ�.
					new_buffer[5] = addNext;
					//���� �Ŀ� isFull�� isEmpty�� �ٽ� �����Ѵ�. 
					if(addNext-removeNext == buffer.length) { // addNext - removeNext�� ���� ������ ���̿� ������
						isFull = true; isEmpty=false;// ���۴� ���� �� �ִ�.
					}
					else {// isFull�� �ƴ� ���
						isFull= false;
						isEmpty= false; //customer�� ���� �Һ��ϵ��� ���� ���������Ƿ� ���۴� ����ִ� ���°� �ƴϴ�.
					}
				
				}else {//���۰� ���� ������ ���� ���, �ٷ� �����Ѵ�
					new_buffer[((addNext++)%buffer.length)] =c;
					new_buffer[5] = addNext;
					
					if(addNext-removeNext == buffer.length) { //���۰� ���� �� ���, isFull�� isEmpty�� �ٽ� ������ �Ŀ�, 
						isFull = true; isEmpty=false;
						new_buffer.notify();//���۰� ���� �� �� �̻� write�� �� ���� ��Ȳ�� �Ǿ�, �ٸ� ������ customer�� �����.
					}
					else {//���� ������ ���� ���, isFull�� isEmpty�� �ٽ� �����Ѵ�.
						isFull= false;
						isEmpty= false;//customer�� ���� �Һ��ϵ��� ���� ���������Ƿ� ���۴� ����ִ� ���°� �ƴϴ�.
					}
				}
		}
		
	}

	///********************* remove �Լ� �����ϱ� ******************************
	public char remove() {
		int returnChar;
		
		synchronized(new_buffer) {// new_buffer ��ü�� synchronized�� ���� lock�� �ɾ� ���´�.
			
				if(isEmpty) {// ���۰� ������� ��,
					System.out.println("������ �����Ͱ� �����ϴ�.");//������ �����Ͱ� ���ٴ� �˸���
					try {
						//Dead lock�� �����ϱ� ����, ���۰� ��� �� �̻� �Һ��� �� ���� ��Ȳ�� �Ǹ�, ���� ��ü�� ���� lock�� Ǯ�� �ٸ� ������ producer�� �����ٶ����� ����Ѵ�.
						new_buffer.wait();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
					returnChar = new_buffer[((removeNext++)%buffer.length)]; //�Һ��Ѵ�.
					new_buffer[6] = removeNext;
					
					isEmpty = false;// isEmpty�� ���� producer�� write�ϱ���� ��� ���¿� �־���. producer�� ���ۿ� ���̻� write�� �� ���� �� notify�ϹǷ�, isEmpty�� ���� �ʴ´�.
					isFull = false;// �ϳ� �Һ������Ƿ� isFull���� ���� �ƴϴ�.

					
				}else {//isEmpty�� �ƴ� ���,
					returnChar = new_buffer[((removeNext++)%buffer.length)];//�Һ�
					new_buffer[6] = removeNext;
					if(removeNext == addNext) {//�Һ�� ���� ���۰� �� ���°� �Ǹ�
						isEmpty = true; isFull= false;//isEmpty�� isFull �缳��
						new_buffer.notify();//�� �̻� �Һ��� �� �����Ƿ�, producer�� �����.
					}
					else {// ���۰� �� ���°� �ƴ� ���, �Һ��ϰ� �� ���̹Ƿ� isFull���°� �� �� ����.
						isEmpty = false;
						isFull = false;
					}
					
				}
		}
		
		return (char)returnChar;
	
	}

	public int savedNum() { // ���� ���� ��ȯ �޼ҵ�
		synchronized(new_buffer) {
			return new_buffer[5]-new_buffer[6];
		}
	}

	public static void main(String[] args) {
		Monitor m = new Monitor();
		Producer p = new Producer(m);
		Customer c = new Customer(m);

		p.start();
		c.start();
	}

}