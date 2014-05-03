/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 03.05.2014
 */
package net.finmath.experiments.concurrency;

import java.util.concurrent.Semaphore;
import java.util.stream.IntStream;

/**
 * This is a test of Java 8 parallel streams.
 * 
 * The idea behind this code is that the Semaphore concurrentExecutions
 * should limit the parallel executions of the outer forEach (which is an
 * <code>IntStream.range(0,numberOfTasks).parallel().forEach</code> (for example:
 * the parallel executions of the outer forEach should be limited due to a
 * memory constrain).
 * 
 * Inside the execution block of the outer forEach we use another parallel stream
 * to create an inner forEach. The number of concurrent
 * executions of the inner forEach is not limited by us (it is however limited by a
 * system property "java.util.concurrent.ForkJoinPool.common.parallelism").
 * 
 * Problem: If the semaphore is used AND the inner forEach is active, then
 * the execution will be DEADLOCKED.
 * 
 * Note: A practical application is the implementation of the parallel
 * LevenbergMarquardt optimizer in
 * {@link http://finmath.net/java/finmath-lib/apidocs/net/finmath/optimizer/LevenbergMarquardt.html}
 * In one application the number of tasks in the outer and inner loop is very large (>1000)
 * and due to memory limitation the outer loop should be limited to a small (5) number
 * of concurrent executions.
 * 
 * @author Christian Fries
 */
public class ForkJoinPoolTest {

	public static void main(String[] args) {
		
		// Any combination of the booleans works, except (true,true)
		final boolean isUseSemaphore	= true;
		final boolean isUseInnerStream	= true;

		final int		numberOfTasksInOuterLoop = 20;				// In real applications this can be a large number (e.g. > 1000).
		final int		numberOfTasksInInnerLoop = 100;				// In real applications this can be a large number (e.g. > 1000).
		final int		concurrentExecusionsLimitInOuterLoop = 5;
		final int		concurrentExecutionsLimitForStreams = 10;

		final Semaphore concurrentExecutions = new Semaphore(concurrentExecusionsLimitInOuterLoop);
				
		System.setProperty("java.util.concurrent.ForkJoinPool.common.parallelism",Integer.toString(concurrentExecutionsLimitForStreams));
		System.out.println("java.util.concurrent.ForkJoinPool.common.parallelism = " + System.getProperty("java.util.concurrent.ForkJoinPool.common.parallelism"));
		
		IntStream.range(0,numberOfTasksInOuterLoop).parallel().forEach(i -> {

			if(isUseSemaphore) {
				concurrentExecutions.acquireUninterruptibly();
			}

			try {
				System.out.println(i + "\t" + concurrentExecutions.availablePermits() + "\t" + Thread.currentThread());
				Thread.sleep(10);

				if(isUseInnerStream) {
					runCodeWhichUsesParallelStream(numberOfTasksInInnerLoop);
				}
				else {
					Thread.sleep(10*numberOfTasksInInnerLoop);
				}
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			finally {
				if(isUseSemaphore) {
					concurrentExecutions.release();
				}
			}
		});

		System.out.println("D O N E");
	}

	/**
	 * Runs code in a parallel forEach using streams.
	 * 
	 * @param numberOfTasksInInnerLoop Number of tasks to execute.
	 */
	private static void runCodeWhichUsesParallelStream(int numberOfTasksInInnerLoop) {
		IntStream.range(0,numberOfTasksInInnerLoop).parallel().forEach(j -> {
			try {
				Thread.sleep(10);
			} catch (Exception e) {
			}
		});
	}
}
