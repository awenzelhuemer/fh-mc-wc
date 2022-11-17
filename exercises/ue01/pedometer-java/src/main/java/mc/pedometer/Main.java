package mc.pedometer;

import org.apache.commons.math3.analysis.solvers.LaguerreSolver;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.fitting.PolynomialCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoints;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;

public class Main {

    private static double average(List<Double> values) {
        double sum = 0; //average will have decimal point

        for (Double value : values) {
            //parse string to double, note that this might fail if you encounter a non-numeric string
            //Note that we could also do Integer.valueOf( args[i] ) but this is more flexible
            sum += value;
        }

        return sum / values.size();
    }

    public static void main(String[] args) throws IOException {

        BufferedReader sc = new BufferedReader(new FileReader("data1.csv"));

        var xAxis = new ArrayList<Double>();
        var yAxis = new ArrayList<Double>();
        var zAxis = new ArrayList<Double>();
        sc.readLine(); // skip header row

        String line = sc.readLine();
        while (line != null) {
            var result = line.split(",");
            xAxis.add(Double.parseDouble(result[1]));
            yAxis.add(Double.parseDouble(result[2]));
            zAxis.add(Double.parseDouble(result[3]));
            line = sc.readLine();
        }

        sc.close();

        int fs = 100;
        int N = 512;
        double ts = 1.25;
        double res = fs / (double) N;
        int ls = (int)(ts * fs);

        int i = 0;
        int stepCount = 0;

        while (i + N < xAxis.size()) {

            var xMean = average(xAxis.subList(i, i + N).stream().map(Math::abs).toList());
            var yMean = average(yAxis.subList(i, i + N).stream().map(Math::abs).toList());
            var zMean = average(zAxis.subList(i, i + N).stream().map(Math::abs).toList());

            var maxMean = Math.max(xMean, Math.max(yMean, zMean));

            var mainAxis = new ArrayList<Double>();
            if (xMean == maxMean) {
                mainAxis.addAll(xAxis.subList(i, i + N));
            } else if (yMean == maxMean) {
                mainAxis.addAll(yAxis.subList(i, i + N));
            } else {
                mainAxis.addAll(zAxis.subList(i, i + N));
            }

            i += ls;


            var transformedComplex
                    = Arrays.stream(new FastFourierTransformer(DftNormalization.STANDARD)
                            .transform(mainAxis.stream().mapToDouble(Double::doubleValue).toArray(), TransformType.FORWARD))
                    .map(complex -> complex.abs() * 2)
                    .toList();

            var w0 = average(transformedComplex.subList(0, 2));
            var wc = average(transformedComplex.subList(2, 7));

            var obs = new WeightedObservedPoints();

            var weight = 0.0;
            for (var value : transformedComplex.subList(2, 7)) {
                weight++;
                obs.add(weight, value);
            }

            var fitter = PolynomialCurveFitter.create(4);
            var coefficients = DoubleStream.of(fitter.fit(obs.toList())).boxed().collect(Collectors.toList());
            Collections.reverse(coefficients);

            var allMaximum = new LaguerreSolver().solveAllComplex(coefficients.stream().mapToDouble(Double::doubleValue).toArray(), 1.0);
            var maximum = Collections.max(Arrays.stream(allMaximum).map(Complex::abs).toList());

            if (wc > w0 && wc > 10) {
                var fw = res * (maximum + 1);
                var c = (ts * fw);
                stepCount += c;
            }

            i += ls;
        }

        System.out.printf("StepCount: %d", stepCount);
    }
}
