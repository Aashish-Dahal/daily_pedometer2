import 'dart:async';
import 'dart:io' show Platform;

import 'package:flutter/services.dart';

const int _stopped = 0, _walking = 1;

class DailyPedometer2 {
  static const MethodChannel _resetStepCount =
      const MethodChannel("reset_step_count");
  static const EventChannel _stepDetectionChannel =
      const EventChannel('step_detection');
  static const EventChannel _stepCountChannel =
      const EventChannel('step_count');
  static const EventChannel _dailyStepCountChannel =
      const EventChannel('daily_step_count');

  static StreamController<PedestrianStatus> _androidPedestrianController =
      StreamController.broadcast();

  /// Returns one step at a time.
  /// Events come every time a step is detected.
  static Stream<PedestrianStatus> get pedestrianStatusStream {
    Stream<PedestrianStatus> stream = _stepDetectionChannel
        .receiveBroadcastStream()
        .map((event) => PedestrianStatus._(event));
    if (Platform.isAndroid) return _androidStream(stream);
    return stream;
  }

  /// Transformed stream for the Android platform
  static Stream<PedestrianStatus> _androidStream(
      Stream<PedestrianStatus> stream) {
    /// Init a timer and a status
    Timer? t;
    int? pedestrianStatus;

    /// Listen for events on the original stream
    /// Transform these events by using the timer
    stream.listen((dynamic e) {
      /// If an event is received it means the status is 'walking'
      /// If the timer has been started, it should be cancelled
      /// to prevent sending out additional 'walking' events
      if (t != null) {
        t!.cancel();

        /// If a previous status was either not set yet, or was 'stopped'
        /// then a 'walking' event should be emitted.
        if (pedestrianStatus == null || pedestrianStatus == _stopped) {
          _androidPedestrianController.add(PedestrianStatus._(_walking));
          pedestrianStatus = _walking;
        }
      }

      /// After receiving an event, start a timer for 2 seconds, after
      /// which a 'stopped' event is emitted. If it manages to go through,
      /// it is because no events were received for the 2 second duration
      t = Timer(Duration(seconds: 2), () {
        _androidPedestrianController.add(PedestrianStatus._(_stopped));
        pedestrianStatus = _stopped;
      });
    });

    return _androidPedestrianController.stream;
  }

  /// Returns the steps taken since last system boot.
  /// Events may come with a delay.
  static Stream<StepCount> get stepCountStream =>
      _stepCountChannel.receiveBroadcastStream().map((event) {
        return StepCount._(event);
      });

  static Future<Map> resetStepCount() async {
    final status = await _resetStepCount.invokeMethod('isDifferentDay');
    return status ?? {};
  }

  /// Returns the daily steps.
  /// Events may come with a delay.
  static Stream<DailyStepCount> get dailyStepCountStream {
    return _dailyStepCountChannel.receiveBroadcastStream().map((event) {
      return DailyStepCount._(event);
    });
  }
}

/// A DTO for steps taken containing the number of steps taken.
class StepCount {
  late DateTime _timeStamp;
  int _steps = 0;

  StepCount._(dynamic e) {
    _steps = e as int;

    _timeStamp = DateTime.now();
  }

  int get steps => _steps;

  DateTime get timeStamp => _timeStamp;

  @override
  String toString() =>
      'Steps taken: $_steps at ${_timeStamp.toIso8601String()}';
}

class DailyStepCount {
  late DateTime _timeStamp;
  late StepData _stepData;

  DailyStepCount._(dynamic e) {
    final result = StepData.fromJson(e);
    _stepData = result;
    _timeStamp = DateTime.now();
  }

  DateTime get timeStamp => _timeStamp;

  StepData get dailyStepData => _stepData;
}

/// A DTO for steps taken containing a detected step and its corresponding
/// status, i.e. walking, stopped or unknown.
class PedestrianStatus {
  static const _WALKING = 'walking';
  static const _STOPPED = 'stopped';
  static const _UNKNOWN = 'unknown';

  static const Map<int, String> _STATUSES = {
    _stopped: _STOPPED,
    _walking: _WALKING
  };

  late DateTime _timeStamp;
  String _status = _UNKNOWN;

  PedestrianStatus._(dynamic t) {
    int _type = t as int;
    _status = _STATUSES[_type]!;
    _timeStamp = DateTime.now();
  }

  String get status => _status;

  DateTime get timeStamp => _timeStamp;

  @override
  String toString() => 'Status: $_status at ${_timeStamp.toIso8601String()}';
}

class StepData {
  final int dailyStepCount;
  final DateTime date;
  final bool isNewDay;

  StepData(
      {required this.dailyStepCount,
      required this.date,
      required this.isNewDay});

  factory StepData.fromJson(Map<dynamic, dynamic> json) {
    return StepData(
        dailyStepCount: json['daily_step_count'],
        date: json['save_date'] != null
            ? DateTime.fromMillisecondsSinceEpoch(json['save_date'])
            : DateTime.now(),
        isNewDay: json["is_new_day"] ?? false);
  }
  Map<String, dynamic> toJson() =>
      {'daily_step_count': dailyStepCount, 'save_date': date};
}
