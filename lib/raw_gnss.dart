import 'dart:async';

import 'package:flutter/services.dart';
import 'package:raw_gnss/gnss_measurement_model.dart';

class RawGnss {
  /// This channel hooks onto the stream for GnssMeasurement events
  static const EventChannel _gnssMeasurementEventChannel =
      EventChannel('dev.joshi.raw_gnss/gnss_measurement');

  /// This channel hooks onto the stream for GnssNavigationMessage events
  static const EventChannel _gnssNavigationMessageEventChannel =
      EventChannel('dev.joshi.raw_gnss/gnss_navigation_message');

  /// This channel hooks onto the stream for GnssAntennaInfo events
  static const EventChannel _gnssAntennaInfoEventChannel =
      EventChannel('dev.joshi.raw_gnss/gnss_antenna_info');

  Stream<GnssMeasurementModel>? _gnssMeasurementEvents;
  Stream? _gnssNavigationMessageEvents;
  Stream<Map<String, dynamic>>? _gnssAntennaInfoEvents;

  /// Getter for GnssMeasurement events
  Stream<GnssMeasurementModel> get gnssMeasurementEvents {
    if (_gnssMeasurementEvents == null) {
      _gnssMeasurementEvents = _gnssMeasurementEventChannel
          .receiveBroadcastStream()
          .map((event) => GnssMeasurementModel.fromJson(
              (event as Map<dynamic, dynamic>).cast()));
    }
    return _gnssMeasurementEvents!;
  }

  /// Getter for GnssNavigationMessage events
  Stream get gnssNavigationMessageEvents {
    if (_gnssNavigationMessageEvents == null) {
      _gnssNavigationMessageEvents =
          _gnssNavigationMessageEventChannel.receiveBroadcastStream();
    }
    return _gnssNavigationMessageEvents!;
  }

  /// Getter for GnssAntennaInfo events
  Stream get gnssAntennaInfoEvents {
    if (_gnssAntennaInfoEvents == null) {
      _gnssAntennaInfoEvents = _gnssAntennaInfoEventChannel
          .receiveBroadcastStream()
          .map((event) => (event as Map<dynamic, dynamic>).map<String, dynamic>(
              (key, value) => MapEntry(key.toString(), value)));
    }
    return _gnssAntennaInfoEvents!;
  }
}
