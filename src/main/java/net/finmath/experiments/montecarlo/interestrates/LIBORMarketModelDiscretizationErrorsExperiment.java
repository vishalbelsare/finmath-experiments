/*
 * (c) Copyright Christian P. Fries, Germany. Contact: email@christian-fries.de.
 *
 * Created on 10.02.2004
 */
package net.finmath.experiments.montecarlo.interestrates;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.finmath.cuda.montecarlo.RandomVariableCudaFactory;
import net.finmath.exception.CalculationException;
import net.finmath.functions.AnalyticFormulas;
import net.finmath.montecarlo.RandomVariableFactory;
import net.finmath.montecarlo.RandomVariableFromArrayFactory;
import net.finmath.montecarlo.interestrate.TermStructureMonteCarloSimulationModel;
import net.finmath.montecarlo.interestrate.products.AbstractLIBORMonteCarloProduct;
import net.finmath.montecarlo.interestrate.products.Bond;
import net.finmath.montecarlo.interestrate.products.Caplet;
import net.finmath.montecarlo.interestrate.products.Caplet.ValueUnit;
import net.finmath.montecarlo.interestrate.products.TermStructureMonteCarloProduct;
import net.finmath.opencl.montecarlo.RandomVariableOpenCLFactory;
import net.finmath.plots.Plot;
import net.finmath.plots.Plots;

/**
 * This class visualizes some of the numerical errors associated with a Monte-Carlo simulation
 * of an Euler scheme approximation of a discrete forward rate term structure model (LIBOR Market Model)
 * and its relation to the equivalent martingale measure.
 *
 * @author Christian Fries
 */
public class LIBORMarketModelDiscretizationErrorsExperiment {

	private static final int	numberOfPaths	= 50000;
	private static final int	numberOfFactors	= 1;
	private static final int	seed			= 3141;

	public static void main(String[] args) throws Exception {
		(new LIBORMarketModelDiscretizationErrorsExperiment()).testBondUnderMeasure();
		(new LIBORMarketModelDiscretizationErrorsExperiment()).testForwardRateUnderMeasure();
		(new LIBORMarketModelDiscretizationErrorsExperiment()).testCapletATMImpliedVol();
		(new LIBORMarketModelDiscretizationErrorsExperiment()).testCapletATMImpliedVolInterpolation();
		(new LIBORMarketModelDiscretizationErrorsExperiment()).testCapletSmile();
		(new LIBORMarketModelDiscretizationErrorsExperiment()).testCapletSmiles();
		//		(new LIBORMarketModelDiscretizationErrorsExperiment()).testCapletSmilesOnGPU();
	}

	public LIBORMarketModelDiscretizationErrorsExperiment() throws CalculationException {}

	public void testBondUnderMeasure() throws Exception {
		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final double forwardRate = 0.05;
		final double periodLength = 0.5;

		for(final Boolean useDiscountCurve : new Boolean[] { false, true }) {
			for(final String measure : new String[] { "terminal", "spot"}) {
				final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
						randomVariableFactory,
						measure,
						forwardRate,
						periodLength,
						useDiscountCurve,
						0.30, 0.0, 0.0,
						numberOfFactors,
						numberOfPaths, seed);

				final List<Double> maturities = new ArrayList<Double>();
				final List<Double> errors = new ArrayList<Double>();

				for(double maturity = 0.5; maturity < 20; maturity += 0.5) {
					final TermStructureMonteCarloProduct product = new Bond(maturity);
					final double value = product.getValue(lmm);
					final double yieldMonteCarlo = -Math.log(value)/maturity;

					final double valueAnalytic = 1.0/Math.pow((1+forwardRate*periodLength), maturity/periodLength);
					final double yieldAnalytic = -Math.log(valueAnalytic)/maturity;

					maturities.add(maturity);
					errors.add(yieldMonteCarlo-yieldAnalytic);
				}

				final Plot plot = Plots.createScatter(maturities, errors, 0.0, 0.2, 5)
						.setTitle("Zero bond error when using " + measure + " measure" + (useDiscountCurve ? " and numeraire control variate." : "."))
						.setXAxisLabel("maturity")
						.setYAxisLabel("error")
						.setYAxisNumberFormat(new DecimalFormat("0.0E00"));

				final String filename = "BondDiscretizationError-measure-" + measure + (useDiscountCurve ? "-with-control" : "");
				plot.saveAsSVG(new File(filename + ".svg"), 900, 400);

				plot.show();
			}
		}
	}

	private void testForwardRateUnderMeasure() throws Exception {
		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final double forwardRate = 0.05;
		final double periodLength = 0.5;
		final boolean useDiscountCurve = true;

		for(final String measure : new String[] { "terminal", "spot"}) {
			final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
					randomVariableFactory,
					measure,
					forwardRate,
					periodLength,
					useDiscountCurve,
					0.30, 0.0, 0.0,
					numberOfFactors,
					numberOfPaths, seed);

			final List<Double> maturities = new ArrayList<Double>();
			final List<Double> errors = new ArrayList<Double>();

			for(double fixing = 0.5; fixing < 20; fixing += 0.5) {
				final TermStructureMonteCarloProduct productForwardRate = new ForwardRate(fixing, fixing, fixing+periodLength, fixing+periodLength, periodLength);
				final TermStructureMonteCarloProduct productBond = new Bond(fixing+periodLength);

				final double valueBondAnalytic = 1.0/Math.pow((1+forwardRate*periodLength), (fixing+periodLength)/periodLength);
				final double value = productForwardRate.getValue(lmm) / valueBondAnalytic;

				final double valueAnalytic = forwardRate * periodLength;

				maturities.add(fixing);
				errors.add(value-valueAnalytic);
			}

			final Plot plot = Plots.createScatter(maturities, errors, 0.0, 0.2, 5)
					.setTitle("Forward rate error when using " + measure + " measure" + (useDiscountCurve ? " and numeraire control variate." : "."))
					.setXAxisLabel("fixing")
					.setYAxisLabel("error")
					.setYAxisNumberFormat(new DecimalFormat("0.0E00"));

			final String filename = "ForwardRateDiscretizationError-measure-" + measure + (useDiscountCurve ? "-with-control" : "");
			plot.saveAsSVG(new File(filename + ".svg"), 900, 400);

			plot.show();
		}
	}

	public void testCapletATMImpliedVol() throws Exception {

		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final String measure = "spot";
		final String simulationTimeInterpolationMethod = "round_down";
		final double forwardRate = 0.05;
		final double periodLength = 0.5;
		final boolean useDiscountCurve = false;

		final double volatility = 0.30;

		final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
				randomVariableFactory,
				measure,
				simulationTimeInterpolationMethod,
				forwardRate,
				periodLength,
				useDiscountCurve,
				volatility, 0.0, 0.0,
				numberOfFactors,
				numberOfPaths, seed);

		final List<Double> maturities = new ArrayList<Double>();
		final List<Double> impliedVolatilities = new ArrayList<Double>();

		final double strike = forwardRate;
		for(double maturity = 0.5; maturity <= 19.5; maturity += 0.01) {
			final TermStructureMonteCarloProduct product = new Caplet(maturity, periodLength, strike);
			final double value = product.getValue(lmm);

			// Determine the zero bond at payment (numerically)
			final TermStructureMonteCarloProduct bondAtPayment = new Bond(maturity+periodLength);
			final double discountFactor = bondAtPayment.getValue(lmm);

			// Determine the forward rate at fixing (numerically)
			final TermStructureMonteCarloProduct forwardRateProduct = new ForwardRate(maturity, periodLength, periodLength);
			final double forward = forwardRateProduct.getValue(lmm) / discountFactor / periodLength;

			final double impliedVol = AnalyticFormulas.blackModelCapletImpliedVolatility(forward, maturity, strike, periodLength, discountFactor, value);

			maturities.add(maturity);
			impliedVolatilities.add(impliedVol);
		}

		final Plot plot = Plots.createScatter(maturities, impliedVolatilities, 0.0, 0.2, 5)
				.setTitle("Caplet implied volatility")
				.setXAxisLabel("maturity")
				.setYAxisLabel("implied volatility")
				.setYRange(0.1, 0.5)
				.setYAxisNumberFormat(new DecimalFormat("0.0%"));
		plot.show();

		final String filename = "Caplet-Impliled-Vol" + measure + (useDiscountCurve ? "-with-control" : "");
		plot.saveAsSVG(new File(filename + ".svg"), 900, 400);
	}

	public void testCapletATMImpliedVolInterpolation() throws Exception {

		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final String measure = "spot";
		final double forwardRate = 0.05;
		final double periodLength = 0.5;
		final boolean useDiscountCurve = false;

		for(final String simulationTimeInterpolationMethod : new String[] { "round_down", "round_nearest" }) {
			final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
					randomVariableFactory,
					measure,
					simulationTimeInterpolationMethod,
					forwardRate,
					periodLength,
					useDiscountCurve,
					0.30, 0.0, 0.0,
					numberOfFactors,
					numberOfPaths, seed);

			final List<Double> maturities = new ArrayList<Double>();
			final List<Double> impliedVolatilities = new ArrayList<Double>();

			final double strike = forwardRate;
			for(double maturity = 0.5; maturity <= 19.5; maturity += 0.01) {
				final TermStructureMonteCarloProduct product = new Caplet(maturity, periodLength, strike);
				final double value = product.getValue(lmm);

				// Determine the zero bond at payment (numerically)
				final TermStructureMonteCarloProduct bondAtPayment = new Bond(maturity+periodLength);
				final double discountFactor = bondAtPayment.getValue(lmm);

				// Determine the forward rate at fixing (numerically)
				final TermStructureMonteCarloProduct forwardRateProduct = new ForwardRate(maturity, periodLength, periodLength);
				final double forward = forwardRateProduct.getValue(lmm) / discountFactor / periodLength;

				final double impliedVol = AnalyticFormulas.blackModelCapletImpliedVolatility(forward, maturity, strike, periodLength, discountFactor, value);

				maturities.add(maturity);
				impliedVolatilities.add(impliedVol);
			}

			final Plot plot = Plots.createScatter(maturities, impliedVolatilities, 0.0, 0.2, 5)
					.setTitle("Caplet implied volatility using simulation time interpolation " + simulationTimeInterpolationMethod + ".")
					.setXAxisLabel("maturity")
					.setYAxisLabel("implied volatility")
					.setYRange(0.1, 0.5)
					.setYAxisNumberFormat(new DecimalFormat("0.0%"));
			plot.show();

			final String filename = "Caplet-Impliled-Vol-" + simulationTimeInterpolationMethod;
			plot.saveAsSVG(new File(filename + ".svg"), 900, 400);
		}
	}

	public void testCapletSmile() throws CalculationException {

		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final String simulationTimeInterpolationMethod = "round_down";
		final double forwardRate = 0.05;
		final double periodLength = 0.5;
		final boolean useDiscountCurve = false;

		for(final String measure : new String[] { "terminal", "spot"}) {
			final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
					randomVariableFactory,
					measure,
					simulationTimeInterpolationMethod,
					forwardRate,
					periodLength,
					useDiscountCurve,
					0.30, 0.0, 0.0,
					numberOfFactors,
					numberOfPaths, seed);

			final List<Double> strikes = new ArrayList<Double>();
			final List<Double> impliedVolatilities = new ArrayList<Double>();

			for(double strike = 0.025; strike < 0.10; strike += 0.0025) {
				final TermStructureMonteCarloProduct product = new Caplet(5.0, 0.5, strike);
				final double value = product.getValue(lmm);

				/*
				 * Conversion to implied volatility
				 */
				final double forward = 0.05;
				final double optionMaturity = 5.0;
				final AbstractLIBORMonteCarloProduct bondAtPayment = new Bond(5.5);
				final double optionStrike = strike;
				final double payoffUnit = bondAtPayment.getValue(lmm) * periodLength;
				final double optionValue = value;
				final double impliedVol = AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);

				strikes.add(strike);
				impliedVolatilities.add(impliedVol);
			}

			Plots.createScatter(strikes, impliedVolatilities, 0.0, 0.2, 5)
			.setTitle("Caplet implied volatility using " + measure + " measure.")
			.setXAxisLabel("strike")
			.setYAxisLabel("implied volatility")
			.setYRange(0.1, 0.5)
			.setYAxisNumberFormat(new DecimalFormat("0.0%")).show();
		}
	}

	public void testCapletSmiles() throws CalculationException {

		final RandomVariableFactory randomVariableFactory = new RandomVariableFromArrayFactory();

		final String simulationTimeInterpolationMethod = "round_down";
		final String measure = "spot";
		final double forwardRate = 0.05;
		final double periodLength = 0.5;
		final boolean useDiscountCurve = false;

		final List<Double> strikes = new ArrayList<Double>();
		final Map<String, List<Double>> impliedVolCurves = new HashMap();
		for(double normality = 0.0; normality <= 1.0; normality += 0.1) {
			final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
					randomVariableFactory,
					measure,
					simulationTimeInterpolationMethod,
					forwardRate,
					periodLength,
					useDiscountCurve,
					0.30, normality, 0.0,
					numberOfFactors,
					numberOfPaths, seed);

			final List<Double> impliedVolatilities = new ArrayList<Double>();
			for(double strike = 0.025; strike < 0.10; strike += 0.0025) {
				final TermStructureMonteCarloProduct product = new Caplet(5.0, 0.5, strike);
				final TermStructureMonteCarloProduct productVol = new Caplet(5.0, 0.5, strike, 0.5, false, ValueUnit.LOGNORMALVOLATILITY);
				final double value = product.getValue(lmm);
				final double vol3 = productVol.getValue(lmm);
				final double forward = 0.05;
				final double optionMaturity = 5.0;
				final AbstractLIBORMonteCarloProduct bondAtPayment = new Bond(5.5);
				final double optionStrike = strike;
				//			double payoffUnit = bondAtPayment.getValue(lmm);
				final double payoffUnit = 1.0/Math.pow(1+0.05*0.5, 5*2+1) * 0.5;
				final double optionValue = value;
				final double impliedVol = AnalyticFormulas.blackScholesOptionImpliedVolatility(forward, optionMaturity, optionStrike, payoffUnit, optionValue);
				//			final double impliedVol = AnalyticFormulas.blackModelCapletImpliedVolatility(forward, optionMaturity, optionStrike, 0.5, payoffUnit, optionValue*0.5);

				strikes.add(strike);
				impliedVolatilities.add(vol3);
			}
			impliedVolCurves.putIfAbsent(String.valueOf(normality), impliedVolatilities);

		}
		Plots.createScatter(strikes, impliedVolCurves, 0.0, 0.2, 5)
		.setTitle("Caplet implied volatility using " + measure + " measure.")
		.setXAxisLabel("strike")
		.setYAxisLabel("implied volatility")
		.setYAxisNumberFormat(new DecimalFormat("0.0%")).show();
	}

	public void testCapletSmilesOnGPU() throws CalculationException {

		final String simulationTimeInterpolationMethod = "round_down";
		final String measure = "spot";
		final double forwardRate = 0.05;
		final double periodLength = 0.5;
		final boolean useDiscountCurve = false;
		final int		numberOfPaths	= 100000;

		for(final RandomVariableFactory randomVariableFactory : List.of(new RandomVariableFromArrayFactory(), new RandomVariableCudaFactory(), new RandomVariableOpenCLFactory())) {
			final long timeStart = System.currentTimeMillis();

			final List<Double> strikes = new ArrayList<Double>();
			final Map<String, List<Double>> impliedVolCurves = new HashMap();
			for(double normality = 0.0; normality <= 1.0; normality += 0.5) {
				final TermStructureMonteCarloSimulationModel lmm = ModelFactory.createLIBORMarketModel(
						randomVariableFactory,
						measure,
						simulationTimeInterpolationMethod,
						forwardRate,
						periodLength,
						useDiscountCurve,
						0.30, normality, 0.0,
						numberOfFactors,
						numberOfPaths, seed);

				final List<Double> impliedVolatilities = new ArrayList<Double>();
				for(double strike = 0.025; strike < 0.10; strike += 0.001) {
					final TermStructureMonteCarloProduct product = new Caplet(5.0, 0.5, strike);
					final double value = product.getValue(lmm);

					final double forward = 0.05;
					final double optionMaturity = 5.0;
					final AbstractLIBORMonteCarloProduct bondAtPayment = new Bond(5.5);
					final double optionStrike = strike;
					final double payoffUnit = bondAtPayment.getValue(lmm);
					final double optionValue = value;
					final double impliedVol = AnalyticFormulas.blackModelCapletImpliedVolatility(forwardRate, optionMaturity, optionStrike, periodLength, payoffUnit, optionValue);

					strikes.add(strike);
					impliedVolatilities.add(impliedVol);
				}
				impliedVolCurves.putIfAbsent(String.valueOf(normality), impliedVolatilities);
			}

			final long timeEnd = System.currentTimeMillis();

			System.out.println("Calculation time: "+ ((timeEnd-timeStart)/1000) + " \t" + randomVariableFactory.getClass().getSimpleName());

			Plots.createScatter(strikes, impliedVolCurves, 0.0, 0.2, 5)
			.setTitle("Caplet implied volatility using " + measure + " measure.")
			.setXAxisLabel("strike")
			.setYAxisLabel("implied volatility")
			.setYRange(0.1, 0.5)
			.setYAxisNumberFormat(new DecimalFormat("0.0%")).show();
		}
	}
}
