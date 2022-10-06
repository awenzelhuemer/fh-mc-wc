import 'package:csv/csv.dart';
import 'package:flutter/material.dart';
import 'package:scidart/numdart.dart';
import 'package:scidart/scidart.dart';
import 'package:sensors_plus/sensors_plus.dart';
import 'package:fmin/fmin.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  // This widget is the root of your application.
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Pedometer',
      theme: ThemeData(
        // This is the theme of your application.
        //
        // Try running your application with "flutter run". You'll see the
        // application has a blue toolbar. Then, without quitting the app, try
        // changing the primarySwatch below to Colors.green and then invoke
        // "hot reload" (press "r" in the console where you ran "flutter run",
        // or simply save your changes to "hot reload" in a Flutter IDE).
        // Notice that the counter didn't reset back to zero; the application
        // is not restarted.
        primarySwatch: Colors.green,
      ),
      home: const MyHomePage(title: 'Pedometer'),
    );
  }
}

class MyHomePage extends StatefulWidget {
  const MyHomePage({super.key, required this.title});

  final String title;

  @override
  State<MyHomePage> createState() => _MyHomePageState();
}

class _MyHomePageState extends State<MyHomePage> {
  final List<double> xAxis = [];
  final List<double> yAxis = [];
  final List<double> zAxis = [];
  int _stepCount = 0;
  static const int windowSize = 320;
  static const int samplingFrequency = 100;
  static const double duration = 1.25;
  static const double resolution = samplingFrequency / windowSize;
  static final int length = (samplingFrequency * duration).round();

  final bool readFromFile = true;

  double _x = 0;
  double _y = 0;
  double _z = 0;

  _readFile() async {
    var file = await DefaultAssetBundle.of(context).loadString('assets/data1.csv');
    List<List<dynamic>> data = const CsvToListConverter(eol: '\n').convert(file);

    // skip header line
    for (var row in data.skip(1)) {
      xAxis.add(row.elementAt(1));
      yAxis.add(row.elementAt(2));
      zAxis.add(row.elementAt(3));
    }
    _calculateSteps();
  }

  @override
  void initState() {
    super.initState();

    if(readFromFile) {
      _readFile();
    }

    accelerometerEvents.listen(
      (AccelerometerEvent event) {
        _x = event.x;
        _y = event.y;
        _z = event.z;

        setState(() {});

        if (!readFromFile) {
          xAxis.add(event.x);
          yAxis.add(event.y);
          zAxis.add(event.z);

          _calculateSteps();
        }
      },
    );
  }

  void _calculateSteps() {
    int i = 0;
    int stepCount = 0;

    while (i + windowSize < xAxis.length) {
      double xMean = mean(Array(xAxis.sublist(i, i + windowSize).map((e) => e.abs()).toList()));
      double yMean = mean(Array(yAxis.sublist(i, i + windowSize).map((e) => e.abs()).toList()));
      double zMean = mean(Array(zAxis.sublist(i, i + windowSize).map((e) => e.abs()).toList()));

      double maxMean = max(xMean, max(yMean, zMean));

      List<double> axis;

      if (xMean == maxMean) {
        axis = xAxis.sublist(i, i + windowSize);
      } else if (yMean == maxMean) {
        axis = yAxis.sublist(i, i + windowSize);
      } else {
        axis = zAxis.sublist(i, i + windowSize);
      }

      var s = arrayComplexAbs(fft(arrayToComplexArray(Array(axis))))
          .map((element) => element.abs() * 2)
          .toList();

      var w0 = mean(Array(s.sublist(0, 2)));
      var wc = mean(Array(s.sublist(2, 7)));

      var coefficients =
          PolyFit(Array([1, 2, 3, 4, 5]), Array(s.sublist(2, 7)), 4);

      // TODO Replace with working scalar function minimization
      double f(List<double> x) => coefficients.coefficient(x.first.toInt());
      var maximum = nelderMead(f, [1.0, 5.0]).fx!;

      if (wc > w0 && wc > 10) {
        var fw = resolution * (maximum + 1);
        var c = duration * fw;
        stepCount += (stepCount + c).toInt();
      }

      i += length;
    }

    _stepCount += stepCount;
    setState(() {});
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
        appBar: AppBar(
          // Here we take the value from the MyHomePage object that was created by
          // the App.build method, and use it to set our appbar title.
          title: Text(widget.title),
        ),
        body: Center(
          // Center is a layout widget. It takes a single child and positions it
          // in the middle of the parent.
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              Text(
                'Steps',
                style: Theme.of(context).textTheme.headlineLarge,
              ),
              Text(
                "(x ${_x.toStringAsFixed(2)}, y ${_y.toStringAsFixed(2)} z ${_z.toStringAsFixed(2)})",
                style: Theme.of(context).textTheme.bodyLarge,
              ),
              Text(
                "$_stepCount",
                style: Theme.of(context).textTheme.displayLarge,
              ),
            ],
          ),
        ));
  }
}
