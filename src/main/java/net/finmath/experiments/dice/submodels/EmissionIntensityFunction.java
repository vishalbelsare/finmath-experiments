package net.finmath.experiments.dice.submodels;

import java.util.function.Function;

/**
 * The function that maps time to emission intensity.
 * 
 * Note: This is the function \( \sigma(t) \) from the original model, except that the division by \( (1-\mu(0)) \) is missing here.
 * 
 * Note: The function depends on the time step size
 * 
 * @author Christian Fries
 */
public class EmissionIntensityFunction implements Function<Double, Double> {

	private static double timeStep = 5.0;	// time step in the original model (should become a parameter)

	private static double e0 = 35.85;					// Initial emissions
	private static double q0 = 105.5;					// Initial global output
	private static double sigma0 = e0/q0;				// Calculated initial emissions intensity, the 1/(1-mu0) is outside

//	private static double mu0 = 0.03;					// Initial mitigation rate
//	private static double sigma0 = e0/(q0*(1-mu0));		// Calculated initial emissions intensity

	private final double emissionIntensityInitial;		// sigma0;
	private final double emissionIntensityRateInitial;	// = 0.0152;		// -g	// per year
	private final double emissionIntensityRateDecay;	// = 0.001;			// -d	// per year

	public EmissionIntensityFunction(double emissionIntensityInitial, double emissionIntensityRateInitial,
			double emissionIntensityRateDecay) {
		super();
		this.emissionIntensityInitial = emissionIntensityInitial;
		this.emissionIntensityRateInitial = emissionIntensityRateInitial;
		this.emissionIntensityRateDecay = emissionIntensityRateDecay;
	}

	public EmissionIntensityFunction() {
		this(sigma0, 0.0152, 0.001);
	}

	@Override
	public Double apply(Double time) {
		double emissionIntensityRate = emissionIntensityRateInitial * Math.pow(1-emissionIntensityRateDecay, time * timeStep);
		double emissionIntensity = emissionIntensityInitial * Math.exp(-emissionIntensityRate * time * timeStep);

		return emissionIntensity;
	}
}
