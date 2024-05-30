import 'package:daily_pedometer2/daily_pedometer2.dart';
import 'package:hive/hive.dart';
import 'package:intl/intl.dart';

class HiveStorage {
  final String boxKey = "daily_steps";
  HiveStorage._();
  static HiveStorage get instance => HiveStorage._();
  registerStepDataAdapter() async {
    Hive.registerAdapter(StepDataAdapter());
  }

  void saveDailySteps({required StepData stepData}) async {
    final date = DateFormat('yyyy-MM-dd').format(stepData.date);
    late Box<StepData> dailyStepCountBox;

    final isOpen = Hive.isBoxOpen(boxKey);
    if (isOpen) {
      dailyStepCountBox = await Hive.box(boxKey);
      await dailyStepCountBox.put(date, stepData);
    } else {
      dailyStepCountBox = await Hive.openBox(boxKey);
      return dailyStepCountBox.put(date, stepData);
    }
  }

  Future<StepData?> getDailySteps({required String date}) async {
    late Box<StepData> dailyStepCountBox;
    final isOpen = Hive.isBoxOpen(boxKey);
    if (!isOpen) {
      dailyStepCountBox = await Hive.box(boxKey);
      return dailyStepCountBox.get(date);
    } else {
      dailyStepCountBox = await Hive.openBox(boxKey);
      return dailyStepCountBox.get(date);
    }
  }

  Future<List<StepData>?> getBulkSteps() async {
    late Box<StepData> dailyStepCountBox;
    final isOpen = Hive.isBoxOpen(boxKey);
    if (!isOpen) {
      dailyStepCountBox = await Hive.box(boxKey);
      return dailyStepCountBox.values.toList();
    } else {
      dailyStepCountBox = await Hive.openBox(boxKey);
      return dailyStepCountBox.values.toList();
    }
  }
}
