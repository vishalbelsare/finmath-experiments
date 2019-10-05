/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 12.02.2013
 */
package net.finmath.experiments.montecarlo.assetderivativevaluation.products;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationModel;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariable;
import net.finmath.stochastic.RandomVariableAccumulatorInterface;

/**
 * Implements calculation of the delta of a European option using the pathwise method.
 *
 * @author Christian Fries
 * @version 1.0
 */
public class EuropeanOptionRhoPathwise extends AbstractAssetMonteCarloProduct {

	private double	maturity;
	private double	strike;

	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 *
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public EuropeanOptionRhoPathwise(double maturity, double strike) {
		super();
		this.maturity = maturity;
		this.strike = strike;
	}

	/**
	 * Calculates the value of the option under a given model.
	 *
	 * @param model A reference to a model
	 * @return the value
	 * @throws CalculationException
	 */
	public double getValue(AssetModelMonteCarloSimulationModel model) throws CalculationException
	{
		MonteCarloBlackScholesModel blackScholesModel = null;
		try {
			blackScholesModel = (MonteCarloBlackScholesModel)model;
		}
		catch(Exception e) {
			throw new ClassCastException("This method requires a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}

		// Get underlying and numeraire
		RandomVariable underlyingAtMaturity	= model.getAssetValue(maturity,0);
		RandomVariable numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariable underlyingAtToday		= model.getAssetValue(0.0,0);
		RandomVariable numeraireAtToday		= model.getNumeraire(0);
		RandomVariable monteCarloWeights		= model.getMonteCarloWeights(maturity);

		/*
		 *  The following way of calculating the expected value (average) is discouraged since it makes too strong
		 *  assumptions on the internals of the <code>RandomVariableAccumulatorInterface</code>. Instead you should use
		 *  the mutators sub, div, mult and the getter getAverage. This code is provided for illustrative purposes.
		 */
		double average = 0.0;
		for(int path=0; path<model.getNumberOfPaths(); path++)
		{
			if(underlyingAtMaturity.get(path) > strike)
			{
				// Get some model parameters
				double T		= maturity;
				double ST		= underlyingAtMaturity.get(path);

				double payOff			= 1;
				double modifiedPayoff	= payOff * ST * T - (ST-strike) * T;

				average += modifiedPayoff / numeraireAtMaturity.get(path) * monteCarloWeights.get(path) * numeraireAtToday.get(path);
			}
		}

		return average;
	}

	@Override
	public RandomVariableAccumulatorInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationModel model) {
		throw new RuntimeException("Method not supported.");
	}
}