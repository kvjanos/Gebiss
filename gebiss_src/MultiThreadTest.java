public class MultiThreadTest {
 
	static myRun r1;
	static myRun r2;
	static myRun r3;
	static myRun r4;
	static myRun r5;
 
	public MultiThreadTest() {
		r1 = new myRun();
		r2 = new myRun();
		r3 = new myRun();
		r4 = new myRun();
		r5 = new myRun();
	}
 
	public static void main(String[] args) {
 
		MultiThreadTest m = new MultiThreadTest();
		r1.run();
		r2.run();
		r3.run();
		r4.run();
		r5.run();
	}
 
	public class myRun implements Runnable {
 
		public void run() {
			for (long i=0; i<1E10; i++) {
				double a =Math.exp(Math.random() + 2.0);
			}
		}
	}
}
