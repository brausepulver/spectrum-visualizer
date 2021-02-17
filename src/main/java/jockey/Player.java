package jockey;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import javafx.application.Application;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.StringConverter;

/**
 * @author jockey
 */
public class Player extends Application 
{
    public void start(Stage stage) throws Exception
    {
        // menu scene
        var grid = new GridPane();
        grid.setAlignment(Pos.CENTER);
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(25, 25, 25, 25));

        // title
        var sceneTitle = new Text("Spectrum Analyzer");
        sceneTitle.setFont(Font.font("Tahoma", FontWeight.NORMAL, 20));
        grid.add(sceneTitle, 0, 0, 2, 1);

        // audio input
        var audioInputLabel = new Label("Audio Input");
        grid.add(audioInputLabel, 0, 1);
        var group = new ToggleGroup();
        var rb1 = new RadioButton("From file");
        rb1.setToggleGroup(group);
        var rb2 = new RadioButton("From microphone");
        rb2.setToggleGroup(group);
        var hb = new HBox();
        hb.setSpacing(10);
        hb.getChildren().addAll(rb1, rb2);
        grid.add(hb, 1, 1);
        var usesMicrophone = new SimpleBooleanProperty();

        // audio source
        var audioSourceLabel = new Label("Audio Source");
        grid.add(audioSourceLabel, 0, 2);
        var hb2 = new HBox();
        grid.add(hb2, 1, 2);
        var btn2 = new Button("Open Audio File...");
        hb2.getChildren().add(btn2);

        var fc = new FileChooser();
        fc.setTitle("Open Audio File");
        fc.setInitialDirectory(new File(System.getProperty("user.dir")));
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("WAV", "*.wav"));
        var audioSource = new SimpleObjectProperty<File>(null);
        btn2.setOnAction(e -> {
            audioSource.setValue(fc.showOpenDialog(stage));
        });

        // audio output
        var audioOutputLabel = new Label("Audio Output");
        grid.add(audioOutputLabel, 0, 3);
        var mixerOptions = FXCollections.observableArrayList(AudioSystem.getMixerInfo());
        var audioOutput = new ChoiceBox<Mixer.Info>(mixerOptions);
        audioOutput.setConverter(new StringConverter<Mixer.Info>(){
            public String toString(Mixer.Info object) {
                return object.getName();
            }
            public Mixer.Info fromString(String string) {
                return null; // not used by ChoiceBox
            }
        });
        grid.add(audioOutput, 1, 3);

        // read buffer size
        var readBufferSizeLabel = new Label("Read Buffer Size");
        grid.add(readBufferSizeLabel, 0, 5);
        var readBufferSize = new TextField("2048");
        grid.add(readBufferSize, 1, 5);

        // amplitude max
        var amplitudeMaxLabel = new Label("Amplitude Max");
        grid.add(amplitudeMaxLabel, 0, 6);
        var amplitudeMax = new TextField("5e6");
        grid.add(amplitudeMax, 1, 6);

        // spectrum smoothness
        var spectrumSmoothnessLabel = new Label("Spectrum Smoothness");
        grid.add(spectrumSmoothnessLabel, 0, 7);
        var spectrumSmoothness = new TextField("10");
        grid.add(spectrumSmoothness, 1, 7);

        // frequency max
        var frequencyMaxLabel = new Label("Frequency Max");
        grid.add(frequencyMaxLabel, 0, 8);
        var frequencyMax = new TextField();
        grid.add(frequencyMax, 1, 8);

        // octave scale
        var octaveScale = new CheckBox("Octave Scale");
        grid.add(octaveScale, 0, 9);
        var octaveParts = new TextField();
        octaveParts.setPromptText("Octave Parts");
        grid.add(octaveParts, 1, 9);
        octaveScale.selectedProperty().addListener((ov, oldVal, newVal) -> {
            if (newVal.booleanValue() == true) octaveParts.setDisable(false);
            else octaveParts.setDisable(true);
        });
        octaveScale.setSelected(true);
        octaveScale.setSelected(false);

        // decibel scale
        var decibelScale = new CheckBox("Decibel Scale");
        grid.add(decibelScale, 0, 10);

        // change audio source based on audio input choice
        group.selectedToggleProperty().addListener((ov, oldToggle, newToggle) -> {
            if (newToggle == rb1) {
                btn2.setDisable(false);
                audioOutput.setDisable(false);
                usesMicrophone.setValue(false);
                // readBufferSize.setText("4096");
                // spectrumSmoothness.setText("10");
            } else if (newToggle == rb2) {
                btn2.setDisable(true);
                audioOutput.setDisable(true);
                usesMicrophone.setValue(true);
                // readBufferSize.setText("512");
                // spectrumSmoothness.setText("2");
            }
        });
        rb1.setSelected(true); // default selection

        // play spectrum button
        var btn = new Button("Play Spectrum");
        btn.setOnMouseClicked(e -> {
            showSpectrumScene(
                stage, 
                usesMicrophone.get(), 
                audioSource.getValue(),
                audioOutput.getValue(), 
                Integer.parseInt(readBufferSize.getText()),
                Double.valueOf(amplitudeMax.getText()).longValue(),
                Double.parseDouble(spectrumSmoothness.getText()), 
                frequencyMax.getText(), 
                octaveScale.isSelected(),
                octaveParts.getText().isEmpty() ? 0 : Integer.parseInt(octaveParts.getText()),
                decibelScale.isSelected()
            );
        });
        var hbBtn = new HBox(10);
        hbBtn.setAlignment(Pos.BOTTOM_RIGHT);
        hbBtn.getChildren().add(btn);
        grid.add(hbBtn, 1, 10);

        var menu = new Scene(grid);
        stage.setTitle("Spectrum Visualizer");
        stage.setScene(menu);
        stage.show();
    }

    /**
     * prepare and display scene for showing an audio spectrum
     */
    private static void showSpectrumScene(
        Stage stage, boolean usesMicrophone, File audioSource, Mixer.Info audioOutput, 
        int readBufferSize, long amplitudeMax, double spectrumSmoothness, 
        String frequencyMaxString, boolean octaveScale, int octaveParts, boolean decibelScale
    )
    {
        var root = new StackPane();
        var sdl = (audioOutput == null ? null : getAudioOutput(audioOutput));
        var ias = (usesMicrophone ? getAudioInputFromTDL() : getAudioInputFromFile(audioSource));
        
        var canvas = new Canvas(800.0, 600.0);
        var gc = canvas.getGraphicsContext2D();
        root.getChildren().add(canvas);

        float frequencyMax;
        if (frequencyMaxString.isEmpty()) frequencyMax = ias.getFormat().getSampleRate() / 2; // no frequency max chosen by user
        else frequencyMax = Float.parseFloat(frequencyMaxString);
        double octave = 1.0/octaveParts;

        var task = new PlayerTask(
            sdl, ias, gc, 
            readBufferSize, amplitudeMax, spectrumSmoothness, 
            frequencyMax, octaveScale, octave, decibelScale
        );
        var es = Executors.newSingleThreadExecutor();

        // user has closed the window
        stage.setOnCloseRequest(e -> {
            if (task.isRunning()) task.cancel();
            if (sdl != null) {
                sdl.drain();
                sdl.stop();
                sdl.close();
            }
            try {
                ias.close();
            } catch (IOException ex) {
                throw new RuntimeException("AudioInputStream could not be closed", ex);
            }
        });
        // song is over
        task.setOnSucceeded(e -> stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST)));
        
        es.submit(task);
        es.shutdown();
        stage.setScene(new Scene(root));
    }

    private static SourceDataLine getAudioOutput(Mixer.Info mixerInfo)
    {
        var mixer = AudioSystem.getMixer(mixerInfo);
        var lineInfos = mixer.getSourceLineInfo();
        for (var li : lineInfos) System.out.println(li);
        
        SourceDataLine sdl;
        try {
            sdl = (SourceDataLine) mixer.getLine(lineInfos[0]);
        } catch (LineUnavailableException ex) {
            throw new RuntimeException("output line unavailable", ex);
        }
        return sdl;
    }

    private static AudioInputStream getAudioInputFromFile(File audioFile)
    {
        AudioInputStream ias;
        try {
            ias = AudioSystem.getAudioInputStream(audioFile);
        } catch (UnsupportedAudioFileException | IOException ex) {
            throw new RuntimeException("could not obtain AudioInputStream from file " + audioFile, ex);
        }
        return ias;
    }

    private static AudioInputStream getAudioInputFromTDL()
    {
        var format = new AudioFormat(8000.0f, 16, 1, true, true);
        TargetDataLine tdl;
        try {
            tdl = AudioSystem.getTargetDataLine(format);
            tdl.open();
        } catch (LineUnavailableException ex) {
            throw new RuntimeException("input line unavailable", ex);
        }
        tdl.start();
        return new AudioInputStream(tdl);
    }

    public static void main(String[] args)
    {
        launch();
    }
}
