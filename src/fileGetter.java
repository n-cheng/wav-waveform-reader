import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;

import static javax.sound.sampled.AudioSystem.getAudioFileFormat;

public class fileGetter extends Component {
    static File selectedFile;
    static BufferedImage img;
    ArrayList<Double> amps = new ArrayList<Double>();
    public fileGetter(){
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            selectedFile = fileChooser.getSelectedFile();
            System.out.println("Selected file: " + selectedFile.getAbsolutePath());
        }
        if(selectedFile.getAbsolutePath().length()> 4 && selectedFile.getAbsolutePath().endsWith(".wav")){
            System.out.println("Wav file found");
        }
        else{
            System.out.println("Not a wav file");
        }
    }

    static class WavePrint extends JFrame{
        int[] leftSide;
        int[] rightSide;
        int[] audioData;
        int nlengthInSamples;
        int maxAmp = 0;
        float sampleRate;
        float sampleSize;
        public WavePrint(int[] a, int len,float sRate, float sSize){
            super("Wav File Display");
            setContentPane(new drawPane());
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(1200,800);
            setVisible(true);
            this.audioData = a;
            this.nlengthInSamples = len;
            this.sampleRate = sRate;
            this.sampleSize = sSize;
        }

        class drawPane extends JPanel {
            public void paintComponent(Graphics g){
                g.drawLine(0,200,1200,200);
                g.drawLine(0,500,1200,500);

                Graphics2D g2d = (Graphics2D) g;

                for (int audioDatum : audioData) {
                    if (maxAmp < Math.abs(audioDatum)) {
                        maxAmp = Math.abs(audioDatum);
                    }
                }

                for(int k=0;k<audioData.length;k+=4){
                    g2d.draw(new Line2D.Double((float)(k*1000/audioData.length),200,(float)k*1000/audioData.length,200+(float)(audioData[k]*150/maxAmp)));
                    g2d.draw(new Line2D.Double((float)(k+1)*1000/audioData.length,200,(float)(k+1)*1000/audioData.length,200+(float)(audioData[k+1]*150/maxAmp)));
                }
                for(int k=2;k<audioData.length;k+=4){
                    g2d.draw(new Line2D.Double((float)k*1000/audioData.length,500,(float)k*1000/audioData.length,500+(float)(audioData[k]*150/maxAmp)));
                    g2d.draw(new Line2D.Double((float)(k+1)*1000/audioData.length,500,(float)(k+1)*1000/audioData.length,500+(float)(audioData[(k+1)]*150/maxAmp)));
                }

                g2d.drawString("# of Samples: "+sampleSize+" bits Sample Frequency: "+sampleRate+" samples per second",10,750);
            }
        }
    }

    public static void main(String[] args) {
        new fileGetter();
        try {
            WavFile wavFile = WavFile.openWavFile(selectedFile);


            wavFile.display();

            int numChannels = wavFile.getNumChannels();

            double[] buffer = new double[100 * numChannels];

            int framesRead;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;

            do {
                framesRead = wavFile.readFrames(buffer, 100);

                for (int k = 0; k < framesRead * numChannels; k++) {
                    if (buffer[k] > max) max = buffer[k];
                    if (buffer[k] < min) min = buffer[k];
                }
            }
            while (framesRead != 0);
            wavFile.close();



            AudioInputStream audioInputStream = null;
            int nlengthInSamples;
            if (selectedFile != null && selectedFile.isFile()) {
                try {
                    audioInputStream = AudioSystem.getAudioInputStream(selectedFile);
                } catch (Exception ex) {
                    System.out.println(ex.toString());
                    throw ex;
                }
            }
            else {
                System.out.println("Audio file required.");
            }

            AudioFormat format = audioInputStream.getFormat();
            byte[] audioBytes = null;
            float sampleRate = format.getSampleRate();
            float sampleSize = format.getSampleSizeInBits();
            if (audioBytes == null) {
                try {
                    audioBytes = new byte[
                            (int) (audioInputStream.getFrameLength()
                                    * format.getFrameSize())];
                    audioInputStream.read(audioBytes);
                } catch (Exception ex) {
                    System.out.println(ex.getMessage());
                }
            }

            int w = 1000;
            int h = 800;
            int[] audioData = null;
            //ArrayList<int> leftSide = new ArrayList<int>();
            int[] leftSide = null;
            int[] rightSide = null;
            if (format.getSampleSizeInBits() == 16) {
                nlengthInSamples = audioBytes.length / 2;
                audioData = new int[nlengthInSamples];
                leftSide = new int[nlengthInSamples/2+1];
                rightSide = new int[nlengthInSamples/2+1];
                if (format.isBigEndian()) {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        int MSB = (int) audioBytes[2*i];
                        int LSB = (int) audioBytes[2*i+1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                } else {
                    for (int i = 0; i < nlengthInSamples; i++) {
                        int LSB = (int) audioBytes[2*i];
                        int MSB = (int) audioBytes[2*i+1];
                        audioData[i] = MSB << 8 | (255 & LSB);
                    }
                }
            }

            new WavePrint(audioData,audioBytes.length/2,sampleRate,sampleSize);
        }
        catch(Exception e){
            System.err.println(e);
        }
    }
}