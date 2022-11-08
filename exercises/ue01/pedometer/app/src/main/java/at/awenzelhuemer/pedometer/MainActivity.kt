package at.awenzelhuemer.pedometer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.sp
import at.awenzelhuemer.pedometer.ui.theme.PedometerTheme
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader
import org.apache.commons.math3.analysis.solvers.LaguerreSolver
import org.apache.commons.math3.complex.Complex
import org.apache.commons.math3.fitting.PolynomialCurveFitter
import org.apache.commons.math3.fitting.WeightedObservedPoints
import org.apache.commons.math3.transform.DftNormalization
import org.apache.commons.math3.transform.FastFourierTransformer
import org.apache.commons.math3.transform.TransformType
import java.lang.Double.max
import kotlin.math.absoluteValue


class MainActivity : ComponentActivity() {

    var stepCount: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PedometerTheme {
                setContent {
                    StepCountComponent(stepCount = stepCount)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val file = assets.open("sampledata/data1.csv")
        var data = csvReader().readAll(file)
        var xAxis = mutableListOf<Double>()
        var yAxis = mutableListOf<Double>()
        var zAxis = mutableListOf<Double>()

        // Skip header row
        for (row in data.drop(1)) {
            xAxis.add(row[1].toDouble())
            yAxis.add(row[2].toDouble())
            zAxis.add(row[3].toDouble())
        }

        val fs = 100
        val N = 512
        val ts = 1.25
        val res = fs / N
        val ls = (ts * fs).toInt()

        var i = 0

        while (i + N < data.size) {
            val xMean = xAxis.subList(i, i + N).map { v -> v.absoluteValue }.average()
            val yMean = yAxis.subList(i, i + N).map { v -> v.absoluteValue }.average()
            val zMean = zAxis.subList(i, i + N).map { v -> v.absoluteValue }.average()

            val maxMean = max(xMean, max(yMean, (zMean)))

            val mainAxis: List<Double> = if (xMean == maxMean) {
                xAxis.subList(i, i + N)
            } else if (yMean == maxMean) {
                yAxis.subList(i, i + N)
            } else {
                zAxis.subList(i, i + N)
            }

            val transformedComplex =
                FastFourierTransformer(DftNormalization.STANDARD).transform(mainAxis.map { v ->
                    Complex(
                        v,
                        0.0
                    )
                }.toTypedArray(), TransformType.FORWARD)
                    .map { v -> v.abs() * 2 }

            val w0 = transformedComplex.subList(0, 2).average()
            val wc = transformedComplex.subList(2, 7).average()

            val obs = WeightedObservedPoints()

            var weight = 0.0
            for (value in transformedComplex.subList(2, 7)) {
                weight++
                obs.add(weight, value)
            }

            val fitter = PolynomialCurveFitter.create(4)
            val coefficients = fitter.fit(obs.toList()).reversedArray()
            val maximum = LaguerreSolver().solveComplex(coefficients, 1.0, 5).abs()

            if (wc > w0 && wc > 10) {
                val fw = res * (maximum + 1)
                val c = (ts * fw).toInt()
                stepCount += c
            }

            i += ls
        }
    }
}

@Composable
fun StepCountComponent(stepCount: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Steps", fontSize = 20.sp)
        Text(text = stepCount.toString(), fontSize = 40.sp)
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    PedometerTheme {
        StepCountComponent(0)
    }
}