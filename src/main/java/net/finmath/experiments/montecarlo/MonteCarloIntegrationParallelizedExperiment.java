/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christianfries.com.
 *
 * Created on 12.10.2013
 */

package net.finmath.experiments.montecarlo;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.finmath.experiments.montecarlo.randomnumbers.HaltonSequence;

/**
 * A simple class illustrating a Monte-Carlo integration using parallel execution of sub-tasks.
 *
 * @author Christian Fries
 */
public class MonteCarloIntegrationParallelizedExperiment {


	/**
	 * Main program to run the experiment.
	 *
	 * @param args Arguments, not used
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		final long numberOfSimulations = 200000000;
		final int numberOfThreads		= 8;
		final int numberOfTask		= 80;

		final long numberOfSimulationsPerTask	= numberOfSimulations / numberOfTask;
		final long numberOfSimulationsEffective	= numberOfSimulationsPerTask * numberOfTask;

		// Measure calculation time - start
		final long millisStart = System.currentTimeMillis();

		/*
		 * Start worker tasks (asynchronously)
		 */
		System.out.print("Distributing tasks...");
		final ExecutorService				executor	= Executors.newFixedThreadPool(numberOfThreads);
		final ArrayList<Future<Double>>	results		= new ArrayList<Future<Double>>();
		for(int taskIndex=0; taskIndex<numberOfTask; taskIndex++) {

			final long startIndex					=  taskIndex * numberOfSimulationsPerTask;
			final Future<Double> value = executor.submit(new Callable<Double>() {
				@Override
				public Double call() {
					return getMonteCarloApproximationOfPi(startIndex, numberOfSimulationsPerTask);
				}
			});

			results.add(value);
		}
		System.out.print("done.\n");

		/*
		 * Collect results
		 */
		System.out.print("Collecting results...");
		double sumOfResults = 0.0;
		for(int taskIndex=0; taskIndex<numberOfTask; taskIndex++) {
			sumOfResults += results.get(taskIndex).get().doubleValue();
		}
		System.out.print("done.\n");

		final double pi = sumOfResults / numberOfTask;

		// Measure calculation time - end
		final long millisEnd = System.currentTimeMillis();

		System.out.println("Simulation with n = " + numberOfSimulations + " resulted in approximation of pi = " + pi +"\n");

		System.out.println("Approximation error is                                = " + Math.abs(pi-Math.PI));
		System.out.println("Theoretical order of the (quasi) Monte-Carlo error is = " + Math.pow(Math.log(numberOfSimulations),2)/numberOfSimulations + "\n");

		System.out.println("Calculation took " + (millisEnd-millisStart)/1000.0 + " sec.");

		/*
		 * End/clean up thread pool
		 */
		executor.shutdown();
	}

	/**
	 * Calculates an approximation of pi via Monte-Carlo integration.
	 *
	 * @param indexStart The start index of the random number sequence.
	 * @param numberOfSimulations The number of elements to use from the random number sequence.
	 * @return An approximation of pi.
	 */
	public static double getMonteCarloApproximationOfPi(long indexStart, long numberOfSimulations) {
		long numberOfPointsInsideUnitCircle = 0;
		for(long i=indexStart; i<indexStart+numberOfSimulations; i++) {
			final double x = 2.0 * (HaltonSequence.getHaltonNumber(i, 2) - 0.5);	// quasi random number between -1 and 1
			final double y = 2.0 * (HaltonSequence.getHaltonNumber(i, 3) - 0.5);	// quasi random number between -1 and 1
			if(x*x + y*y < 1.0) {
				numberOfPointsInsideUnitCircle++;
			}
		}

		final double areaOfUnitCircle = 4.0 * numberOfPointsInsideUnitCircle / numberOfSimulations;

		// The theoretical area of a circle is pi r^2. Hence we have:
		final double pi = areaOfUnitCircle;

		return pi;
	}
}
