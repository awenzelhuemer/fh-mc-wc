package at.awenzelhuemer;

import at.awenzelhuemer.model.Classifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        try {
            var classes = new String[]{"Downstairs", "Jogging", "Sitting", "Standing", "Upstairs", "Walking"};
            File myObj = new File("WISDM_ar_v1.1_transformed.csv");
            Scanner myReader = new Scanner(myObj);
            myReader.nextLine(); // skip header
            while (myReader.hasNextLine()) {
                String line = myReader.nextLine();

                var data = Arrays.stream(line.split(","))
                        .skip(3)
                        .mapToDouble(Double::parseDouble)
                        .toArray();

                var prediction = Classifier.predictActivity(data);

                var index = 0;
                while (index < prediction.length) {

                    if (prediction[index] == 1.0) {
                        System.out.println("Actual: " + line.split(",")[2] + " => Prediction: " + classes[index]);
                        break;
                    }
                    index++;
                }
            }
            myReader.close();
        } catch (FileNotFoundException e) {
            System.out.println("An error occurred.");
            e.printStackTrace();
        }
    }
}
