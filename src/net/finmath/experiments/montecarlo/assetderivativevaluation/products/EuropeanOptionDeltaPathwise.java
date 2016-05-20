/*
 * (c) Copyright Christian P. Fries, Germany. All rights reserved. Contact: email@christian-fries.de.
 *
 * Created on 12.02.2013
 */
package net.finmath.experiments.montecarlo.assetderivativevaluation.products;

import java.util.logging.Logger;

import net.finmath.exception.CalculationException;
import net.finmath.montecarlo.assetderivativevaluation.AssetModelMonteCarloSimulationInterface;
import net.finmath.montecarlo.assetderivativevaluation.MonteCarloBlackScholesModel;
import net.finmath.montecarlo.assetderivativevaluation.products.AbstractAssetMonteCarloProduct;
import net.finmath.stochastic.RandomVariableInterface;

/**
 * Implements calculation of the delta of a European option using the path-wise method,
 * assuming that the underlying follows a model where d S(T)/d S(0) = S(T)/S(0),
 * e.g., Black-Scholes.
 * 
 * @author Christian Fries
 * @version 1.1
 */
public class EuropeanOptionDeltaPathwise extends AbstractAssetMonteCarloProduct {

	private double	maturity;
	private double	strike;
	
	/**
	 * Construct a product representing an European option on an asset S (where S the asset with index 0 from the model - single asset case).
	 * 
	 * @param strike The strike K in the option payoff max(S(T)-K,0).
	 * @param maturity The maturity T in the option payoff max(S(T)-K,0)
	 */
	public EuropeanOptionDeltaPathwise(double maturity, double strike) {
		super();
		this.maturity = maturity;
		this.strike = strike;
	}
	
    /**
     * This method returns the value random variable of the product within the specified model, evaluated at a given evalutationTime.
     * Note: For a lattice this is often the value conditional to evalutationTime, for a Monte-Carlo simulation this is the (sum of) value discounted to evaluation time.
     * Cashflows prior evaluationTime are not considered.
     * 
     * @param evaluationTime The time on which this products value should be observed.
     * @param model The model used to price the product.
     * @return The random variable representing the value of the product discounted to evaluation time
     * @throws net.finmath.exception.CalculationException Thrown if the valuation fails, specific cause may be available via the <code>cause()</code> method.
     */
    @Override
    public RandomVariableInterface getValue(double evaluationTime, AssetModelMonteCarloSimulationInterface model) throws CalculationException {
		if(!MonteCarloBlackScholesModel.class.isInstance(model)) {
			Logger.getLogger("net.finmath").warning("This method assumes a Black-Scholes type model (MonteCarloBlackScholesModel).");
		}
    	
    	// Get S(T), S(0)
		RandomVariableInterface underlyingAtMaturity	= model.getAssetValue(maturity,0);
        RandomVariableInterface	underlyingAtEvalTime	= model.getAssetValue(evaluationTime,0);
		
		// The "payoff": values = indicator(S(T)-K) * S(T)/S(0)
		RandomVariableInterface trigger	= underlyingAtMaturity.sub(strike);
		RandomVariableInterface values	= underlyingAtMaturity.barrier(trigger, underlyingAtMaturity, 0.0).div(underlyingAtEvalTime);

		// Discounting...
		RandomVariableInterface numeraireAtMaturity		= model.getNumeraire(maturity);
		RandomVariableInterface monteCarloWeights		= model.getMonteCarloWeights(maturity);
        values = values.div(numeraireAtMaturity).mult(monteCarloWeights);

		// ...to evaluation time.
        RandomVariableInterface	numeraireAtEvalTime					= model.getNumeraire(evaluationTime);
        RandomVariableInterface	monteCarloProbabilitiesAtEvalTime	= model.getMonteCarloWeights(evaluationTime);
        values = values.mult(numeraireAtEvalTime).div(monteCarloProbabilitiesAtEvalTime);

        return values;
	}
}
