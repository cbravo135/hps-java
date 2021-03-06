package org.hps.minuit;

/**
 * Extension of FCNBase for providing the analytical gradient of the function.
 * The user-gradient is checked at the beginning of the minimization against the
 * Minuit internal numerical gradient in order to spot problems in the
 * analytical gradient calculation.
 *
 * @see MnApplication#setUseAnalyticalDerivatives
 * @see MnApplication#setCheckAnalyticalDerivatives
 */
public interface FCNGradientBase extends FCNBase {

    /**
     * Calculate the function gradient with respect to each parameter at the
     * given point in parameter space. The size of the output gradient vector
     * must be equal to the size of the input parameter vector.
     */
    public double[] gradient(double[] par);
}
