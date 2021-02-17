package jockey;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

class PlayerTask extends Task<Void>
{
    private static Color SPECTRUM_COLOR = Color.BLUE;
    private static Color SPECTRUM_GRADIENT_COLOR = Color.CORAL;
    private static double SPECTRUM_BAR_WIDTH = 1.0; 

    private SourceDataLine sdl;
    private AudioInputStream ias;
    private GraphicsContext gc;
    private double[] previous;
    private final int readBufferSize;
    private final int playBufferSize;
    private final long amplitudeMax;
    private final double spectrumSmoothness;
    private final float frequencyMax;
    private final boolean octaveScale;
    private final double octave;
    private final boolean decibelScale;

    PlayerTask(
        SourceDataLine sdl, AudioInputStream ias, GraphicsContext gc, 
        int readBufferSize, long amplitudeMax, double spectrumSmoothness, 
        float frequencyMax, boolean octaveScale, double octave, boolean decibelScale
    )
    {
        this.sdl = sdl;
        this.ias = ias;
        this.gc = gc;
        this.readBufferSize = readBufferSize;
        this.playBufferSize = 2 * readBufferSize;
        this.amplitudeMax = amplitudeMax;
        this.spectrumSmoothness = spectrumSmoothness;
        this.frequencyMax = frequencyMax;
        this.octaveScale = octaveScale;
        this.octave = octave;
        this.decibelScale = decibelScale;
    }

    @Override
    protected Void call() throws LineUnavailableException, IOException
    {
        var format = ias.getFormat();
        if (sdl != null) { // microphone input, so no speaker output
            sdl.open(format, playBufferSize);
            sdl.start();
        }

        int total = 0;
        byte[] bytes = new byte[readBufferSize];

        double step = (format.getSampleRate()/2) / ((readBufferSize/4) * 8 / format.getSampleSizeInBits());

        // "frequency bins" are the bars that are displayed on screen
        // they are determined by their starting value, and all values >= that and < the next frequency bin will go into that bin
        var frequencyBins = SpectrumVisualizer.makeFrequencyBins(
            frequencyMax, step,
            octaveScale, octave
        );

        // loop over the whole audio file or until thread is cancelled
        while (!isCancelled() && (total < ias.getFrameLength() * format.getFrameSize() || sdl == null)) {
            int bytesRead = ias.read(bytes, 0, readBufferSize);
            if (bytesRead == -1) break; // nothing read

            var doubles = processAudioData(format, bytes);
            displaySpectrum(doubles, step, frequencyBins, frequencyMax);

            total += bytesRead;
            if (sdl != null) sdl.write(bytes, 0, readBufferSize); // same as above
        }
        return null;
    }

    /**
     * convert byte array into double array suitable for processing by the FFT
     */
    private static double[] processAudioData(AudioFormat format, byte[] bytes)
    {
        var bo = (format.isBigEndian() ? ByteOrder.BIG_ENDIAN : ByteOrder.LITTLE_ENDIAN);

        var sBuf = ByteBuffer.wrap(bytes).order(bo).asShortBuffer();
        var shorts = new short[sBuf.capacity()];
        sBuf.get(shorts);

        var doubles = new double[shorts.length / format.getChannels()];
        for (int i = 0; i < shorts.length / format.getChannels(); i++) {
            // average channels together
            for (int j = 0; j < format.getChannels(); j++) { 
                doubles[i] += (double) shorts[i+j]; 
            }
            doubles[i] /= format.getChannels();
        }
        return doubles;
    }

    /**
     * display the spectrum on screen given an array of samples
     */
    private void displaySpectrum(double[] doubles, double step, double[] frequencyBins, float frequencyMax)
    {
        var magnitudes = SpectrumVisualizer.getSpectrum(
            doubles, step,
            true, frequencyBins, 
            decibelScale, amplitudeMax, 
            frequencyMax
        );
        drawSpectrumBars(decibelScale ? 20 * Math.log10(amplitudeMax) : amplitudeMax, magnitudes);
    }

    /**
     * draw the bars of the spectrum given the results of the FFT
     */
    private void drawSpectrumBars(double amplitudeMax, double[] magnitudes)
    {
        var canvas = gc.getCanvas();
        Platform.runLater(() -> gc.clearRect(0, 0, canvas.getWidth(), canvas.getHeight()));

        double barWidth = canvas.getWidth() / magnitudes.length * SPECTRUM_BAR_WIDTH;

        Platform.runLater(() -> { // ui changes have to be executed on the javafx application thread
            for (int i = 0; i < magnitudes.length; i++) {
                if (previous != null) // smooth spectrum
                    magnitudes[i] = 1/spectrumSmoothness * (magnitudes[i] + (spectrumSmoothness-1) * previous[i]);
    
                if (SPECTRUM_GRADIENT_COLOR != null) // add a color gradient
                    gc.setFill(SPECTRUM_COLOR.interpolate(SPECTRUM_GRADIENT_COLOR, Math.pow(magnitudes[i]/amplitudeMax, 1.0/3)));
                else gc.setFill(SPECTRUM_COLOR);
    
                double barHeight = magnitudes[i] / amplitudeMax * canvas.getHeight();
                if (barHeight > canvas.getHeight()-1) barHeight = canvas.getHeight() - 1; // magnitude greater than maximum amplitude
                gc.fillRect(i*barWidth, canvas.getHeight()-1 - barHeight, barWidth, barHeight);
            }
            previous = magnitudes;
        });
    }
}