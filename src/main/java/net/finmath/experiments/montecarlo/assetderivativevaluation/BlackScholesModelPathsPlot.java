package net.finmath.experiments.montecarlo.assetderivativevaluation;

import net.finmath.montecarlo.BrownianMotionFromMersenneRandomNumbers;
import net.finmath.montecarlo.assetderivativevaluation.models.BlackScholesModel;
import net.finmath.montecarlo.process.EulerSchemeFromProcessModel;
import net.finmath.plots.DoubleToRandomVariableFunction;
import net.finmath.plots.PlotProcess2D;
import net.finmath.time.TimeDiscretizationFromArray;

public class BlackScholesModelPathsPlot {

	public static void main(String[] args) {
		double modelInitialValue = 100.0;	// S(0)
		double modelRiskFreeRate = 0.08; 	// r
		double modelVolatility = 0.10;		// σ

		// Create a model
		var model = new BlackScholesModel(modelInitialValue, modelRiskFreeRate, modelVolatility);

		// Create a corresponding MC process from the model
		var timeDiscretization = new TimeDiscretizationFromArray(0.0, 500, 0.01);
		var brownianMotion = new BrownianMotionFromMersenneRandomNumbers(timeDiscretization, 1, 10000, 3231);
		var process = new EulerSchemeFromProcessModel(model, brownianMotion);

		// Create a function, plotting paths t -> S(t)
		DoubleToRandomVariableFunction paths = time -> process.getProcessValue(timeDiscretization.getTimeIndex(time), 0 /* assetIndex */);

		// Plot 100 of paths against the given time discretization.
		var plot = new PlotProcess2D(timeDiscretization, paths, 100);
		plot.setTitle("Black Scholes model paths t -> S(t, \u03c9\u1D62)").setXAxisLabel("time").setYAxisLabel("value");
		plot.show();
	}
}
