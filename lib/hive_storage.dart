import 'package:daily_pedometer2/daily_pedometer2.dart';
import 'package:hive/hive.dart';
import 'package:intl/intl.dart';

class HiveStorage {
  static registerStepDataAdapter() async {
    Hive.registerAdapter(StepDataAdapter());
  }

  static void saveDailySteps({required StepData stepData}) async {
    final date = DateFormat('yyyy-MM-dd').format(stepData.date);
    late Box<StepData> dailyStepCountBox;

    final isOpen = Hive.isBoxOpen("daily_steps");
    if (isOpen) {
      dailyStepCountBox = await Hive.box("daily_steps");
      await dailyStepCountBox.put(date, stepData);
    } else {
      dailyStepCountBox = await Hive.openBox("daily_steps");
      return dailyStepCountBox.put(date, stepData);
    }
  }

  static Future<StepData?> getDailySteps({required String date}) async {
    late Box<StepData> dailyStepCountBox;
    final isOpen = Hive.isBoxOpen("daily_steps");
    if (!isOpen) {
      dailyStepCountBox = await Hive.box("daily_steps");
      print(dailyStepCountBox.values.toList()[0]);
      return dailyStepCountBox.get(date);
    } else {
      dailyStepCountBox = await Hive.openBox("daily_steps");
      return dailyStepCountBox.get(date);
    }
  }
}
