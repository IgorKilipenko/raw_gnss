import 'dart:async';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:raw_gnss/gnss_measurement_model.dart';
import 'package:raw_gnss/raw_gnss.dart';

void main() {
  runApp(MyApp());
}

const List<Tab> tabs = <Tab>[
  Tab(text: 'Gnss Measurement'),
  Tab(text: 'Navigation messages'),
  Tab(text: 'Antenna info'),
];

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
        primarySwatch: Colors.blue,
        visualDensity: VisualDensity.adaptivePlatformDensity,
      ),
      home: DefaultTabController(
        length: tabs.length,
        child: Builder(builder: (context) {
          final TabController tabController = DefaultTabController.of(context)!;
          tabController.addListener(() {
            if (!tabController.indexIsChanging) {
              // Your code goes here.
              // To get index of current tab use tabController.index
            }
          });
          return Scaffold(
            appBar: AppBar(
              title: Text("Demo"),
              bottom: const TabBar(
                tabs: tabs,
              ),
            ),
            body: HomeScreen(),
          );
        }),
      ),
    );
  }
}

class HomeScreen extends StatefulWidget {
  @override
  _HomeScreenState createState() => _HomeScreenState();
}

class _HomeScreenState extends State<HomeScreen> {
  var _hasPermissions = false;
  late RawGnss _gnss;
  StreamSubscription<GnssMeasurementModel>? _gnssMeasureStreamSubscription;
  bool _hasMeasurement = false;

  @override
  void initState() {
    super.initState();

    _gnss = RawGnss();

    Permission.location.request().then((value) {
      setState(() => _hasPermissions = value.isGranted);
      if (_hasPermissions && _gnssMeasureStreamSubscription == null) {
        _gnssMeasureStreamSubscription =
            _gnss.gnssMeasurementEvents.listen((event) {
          if (!_hasMeasurement) {
            setState(() => _hasMeasurement = true);
          }
        });
      } else if (!_hasPermissions && (_gnssMeasureStreamSubscription != null)) {
        _gnssMeasureStreamSubscription!.cancel();
        _gnssMeasureStreamSubscription = null;
      }
    });
  }

  @override
  void dispose() {
    _gnssMeasureStreamSubscription?.cancel();
    _gnssMeasureStreamSubscription = null;
    super.dispose();
  }

  Widget _buildMeasurementView(BuildContext context) => _hasPermissions
      ? StreamBuilder<GnssMeasurementModel>(
          builder: (context, snapshot) {
            if (snapshot.data == null) {
              return _loadingSpinner();
            } else {
              return ListView.builder(
                itemBuilder: (context, position) {
                  return ListTile(
                    title: Text(
                        "Satellite: ${snapshot.data!.measurements![position].svid}"),
                  );
                },
                itemCount: snapshot.data!.measurements?.length ?? 0,
              );
            }
          },
          stream: _gnss.gnssMeasurementEvents,
        )
      : _loadingSpinner();

  Widget _buildNavigationMessagesView(BuildContext context) {
    if (!_hasPermissions) {
      return _loadingSpinner();
    }
    return StreamBuilder<dynamic>(
      builder: (context, snapshot) {
        if (snapshot.data == null) {
          return _loadingSpinner();
        } else {
          List<dynamic>? navData =
              (snapshot.data as Map<String, dynamic>?)?['data'];
          return ListView.builder(
            itemBuilder: (context, position) {
              return ListTile(
                title: Text("Navigateion data: ${navData?[position]}"),
              );
            },
            itemCount: navData?.length ?? 0,
          );
        }
      },
      stream: _gnss.gnssNavigationMessageEvents,
    );
  }

  @override
  Widget build(BuildContext context) {
    return TabBarView(children: [
      _buildMeasurementView(context),
      _buildNavigationMessagesView(context),
      _loadingSpinner()
    ]);
  }

  Widget _loadingSpinner() => const Center(child: CircularProgressIndicator());
}
