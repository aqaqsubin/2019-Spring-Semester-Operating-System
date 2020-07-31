class Producer extends Thread { //생산자 클래스
	Monitor monitor;
	String alpha = "abcdefghijklmnopqrstuvwxyz";
	int sleepTime = 200;

	public Producer(Monitor m) {
		monitor = m;
	}
	public void run() {
		char c;
		for (int i = 0; i < 10; i++) {
		
			c = alpha.charAt((int)(Math.random() * 25)); //알파벳의 데이터를 임의로 골라서 저장
			monitor.add(c); //Monitor클래스의 add()메소드를 호출하여 배열에 저장

			System.out.println(" 생성 -----> " + c + " 저장 개수 " + (monitor.savedNum()));

			try {
				Thread.sleep((int)(Math.random() * sleepTime));
				sleepTime = (int)(Math.random() * 1000);
			} catch (InterruptedException ex) { }
		}
	}
}

class Customer extends Thread { //소비자 클래스
	Monitor monitor;
	int sleepTime = 500;

	public Customer(Monitor m) {
		monitor = m;
	}

	public void run() {
		char c;
		for (int i = 0; i < 10; i++) {
			c = (monitor.remove()); //Monitor클래스의 remove()메소드를 호출하여 배열에서 값을 리턴시키고 삭제
			System.out.println(" ? <----- " + c + " 저장 개수 " + (monitor.savedNum()));
			try {
				Thread.sleep((int)(Math.random() * sleepTime));
				sleepTime += (int)(Math.random() * 1000);
			} catch (InterruptedException ex) { }
		}
	}
}

public class Monitor {
 
	char[] buffer = new char[5]; //저장공간
	int removeNext = 0; //지울때 저장공간 index 가리키는 용
	int addNext = 0; ////저장 할때 저장공간 index 가리키는 용
	boolean isFull = false;
	boolean isEmpty = true;

	int[] new_buffer = new int[7];
	//addNext와 removeNext에 제대로 된 동기화를 위해 객체를 하나 생성하였다. index 5는 addNext, index 6은 removeNext값이다.
	//addNext와 removeNext는 계속해서 증가하며, 버퍼에 삽입할 때는 buffer의 길이로 나눈 나머지값을 인덱스로 하여 삽입하여,
	//addNext-removeNext는 저장 개수이다.
	public void add(char c) {
		synchronized(new_buffer) {  
			
				if(isFull) {//버퍼가 가득 찼을 때, 
					try {
						//dead lock을 방지하기 위해 버퍼가 가득 차 더 이상 write할 수 없는 상황이 되면 버퍼 객체에 걸어놓았던 lock을 놓아주고, 다른 스레드가 깨워주길 대기한다.
						new_buffer.wait();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
					new_buffer[((addNext++)%buffer.length)]=c; // write할 수 있는 상황이 되면 버퍼에 문자 c를 삽입한 후, addNext를 +1한다.
					new_buffer[5] = addNext;
					//삽입 후에 isFull와 isEmpty를 다시 설정한다. 
					if(addNext-removeNext == buffer.length) { // addNext - removeNext의 값이 버퍼의 길이와 같으면
						isFull = true; isEmpty=false;// 버퍼는 가득 차 있다.
					}
					else {// isFull이 아닐 경우
						isFull= false;
						isEmpty= false; //customer를 깨워 소비하도록 한후 삽입했으므로 버퍼는 비어있는 상태가 아니다.
					}
				
				}else {//버퍼가 가득 차있지 않은 경우, 바로 삽입한다
					new_buffer[((addNext++)%buffer.length)] =c;
					new_buffer[5] = addNext;
					
					if(addNext-removeNext == buffer.length) { //버퍼가 가득 찬 경우, isFull과 isEmpty를 다시 설정한 후에, 
						isFull = true; isEmpty=false;
						new_buffer.notify();//버퍼가 가득 차 더 이상 write할 수 없는 상황이 되어, 다른 스레드 customer를 깨운다.
					}
					else {//가득 차있지 않은 경우, isFull과 isEmpty만 다시 설정한다.
						isFull= false;
						isEmpty= false;//customer를 깨워 소비하도록 한후 삽입했으므로 버퍼는 비어있는 상태가 아니다.
					}
				}
		}
		
	}

	///********************* remove 함수 구성하기 ******************************
	public char remove() {
		int returnChar;
		
		synchronized(new_buffer) {// new_buffer 객체에 synchronized를 통해 lock을 걸어 놓는다.
			
				if(isEmpty) {// 버퍼가 비어있을 시,
					System.out.println("구매할 데이터가 없습니다.");//구매할 데이터가 없다는 알림과
					try {
						//Dead lock을 방지하기 위해, 버퍼가 비어 더 이상 소비할 수 없는 상황이 되면, 버퍼 객체에 대한 lock을 풀고 다른 스레드 producer가 깨워줄때까지 대기한다.
						new_buffer.wait();
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
					returnChar = new_buffer[((removeNext++)%buffer.length)]; //소비한다.
					new_buffer[6] = removeNext;
					
					isEmpty = false;// isEmpty로 인해 producer가 write하기까지 대기 상태에 있었다. producer는 버퍼에 더이상 write할 수 없을 때 notify하므로, isEmpty가 되지 않는다.
					isFull = false;// 하나 소비했으므로 isFull상태 역시 아니다.

					
				}else {//isEmpty가 아닌 경우,
					returnChar = new_buffer[((removeNext++)%buffer.length)];//소비
					new_buffer[6] = removeNext;
					if(removeNext == addNext) {//소비로 인해 버퍼가 빈 상태가 되면
						isEmpty = true; isFull= false;//isEmpty와 isFull 재설정
						new_buffer.notify();//더 이상 소비할 수 없으므로, producer를 깨운다.
					}
					else {// 버퍼가 빈 상태가 아닌 경우, 소비하고 난 후이므로 isFull상태가 될 수 없다.
						isEmpty = false;
						isFull = false;
					}
					
				}
		}
		
		return (char)returnChar;
	
	}

	public int savedNum() { // 저장 개수 반환 메소드
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