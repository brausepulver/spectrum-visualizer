package jockey;

import java.util.ArrayList;

/**
 * @author jockey
 */
public class SpectrumVisualizer
{
    private static Complex[] fftRec(Complex[] x)
    {
        int N = x.length;
        if (N == 1) return x;

        var even = ComplexArrayUtils.getEven(x);
        var odd = ComplexArrayUtils.getOdd(x);

        even = fftRec(even);
        odd = fftRec(odd);

        Complex w, t, r;

        for (int k = 0; k < N/2; k++) {
            w = new Complex(-2*Math.PI * k/N);
            t = new Complex(even[k]);
            r = new Complex(t);

            w.multiply(odd[k]);
            t.add(w);
            r.subtract(w);

            x[k] = t;
            x[k + N/2] = r;
        }
        return x;
    }

    public static Complex[] fft(double[] x)
    {
        var X = ComplexArrayUtils.getComplex(x);
        return fftRec(X);
    }

    public static Complex[] dft(double[] x)
    {
        int N = x.length;
        var X = new Complex[N];

        for (int k = 0; k < N; k++) {
            X[k] = new Complex(0.0, 0.0);
            for (int n = 0; n < N; n++) {
                Complex w = new Complex(-2*Math.PI * k*n/N);
                Complex t = new Complex(x[n], 0.0);
                t.multiply(w);
                X[k].add(t);
            }
        }
        return X;
    }

    /**
     * given an array of samples and frequency bins, process the samples by the FFT and fit them into the frequency bins
     */
    public static double[] getSpectrum(double[] x, double step, boolean scaleFrequency, double[] frequencyBins, boolean scaleAmplitude, double amplitudeMax, float frequencyMax)
    {
        int ml = (scaleFrequency ? frequencyBins.length : x.length);
        var magnitudes = new double[ml];

        double freq = 0;
        int bin = 0;

        var ft = SpectrumVisualizer.fft(x);

        for (int i = 0; i < ft.length / 2 && freq <= frequencyMax; i++) {
            double mag = ft[i].getNorm();

            if (scaleAmplitude && mag != 0) {
                if (mag < 1) mag = 1; // prevent negative canvas coordinates
                mag = 20 * Math.log10(mag / amplitudeMax) + 20 * Math.log10(amplitudeMax);
            }

            if (scaleFrequency) {
                while (bin < frequencyBins.length-1 && freq >= frequencyBins[bin+1]) bin++;
            } else bin++;

            magnitudes[bin] += mag;
            freq += step;
        }
        return magnitudes;
    }

    /**
     * make an array of frequency bins (scaling of the x-axis)
     * optionally logarithmic 
     */
    public static double[] makeFrequencyBins(
        float frequencyMax, double step, 
        boolean octaveScale, double octave
    )
    {
        var bins = new ArrayList<Double>();
        double freq = frequencyMax;

        if (octaveScale) {
            do {
                freq /= Math.pow(2, octave);
                bins.add(0, freq);
            } while (freq - (freq / Math.pow(2, octave)) >= step);
        }
        while (freq > 0) {
            freq -= step;
            bins.add(0, freq);
        }

        var pBins = new double[bins.size()];
        for (int i = 0; i < bins.size(); i++) 
            pBins[i] = bins.get(i).doubleValue();
        return pBins;
    }
}
