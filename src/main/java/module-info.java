
module net.finmath.experiments {
	exports net.finmath.experiments.hedgesimulator;
	exports net.finmath.experiments.shortrate;
	exports net.finmath.experiments.montecarlo.assetderivativevaluation;

	requires transitive net.finmath.lib;
	requires net.finmath.plots;

	requires jfreechart;
	requires junit;
	requires commons.math3;

	requires javafx.controls;
	requires javafx.base;
	requires transitive javafx.graphics;
	requires javafx.swing;
	
	requires java.logging;
	requires java.management;
}