

import org.apache.spark.SparkConf
import org.apache.spark.sql.SparkSession
import java.util.Calendar
import java.text.SimpleDateFormat
import scala.collection.mutable.ArrayBuffer
/**    *
Created by hao 2018-04-11
  Compute holidays 
 */
object date {
  def main(args: Array[String]) {



    val conf = new SparkConf().setAppName("java_date").setMaster("local")
    val spark = SparkSession.builder().enableHiveSupport().config(conf).getOrCreate()


    val startTime: String = args(0)
    val endTime: String = args(1)
    val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    val dateFiled: Int = Calendar.DAY_OF_MONTH


    var beginDate = dateFormat.parse(startTime)

    val endDate = dateFormat.parse(endTime)

    val calendar = Calendar.getInstance()

    calendar.setTime(beginDate)
    val str = Array("2017-01-27", "2017-07-15", "2017-10-14", "2017-12-02", "2017-09-24", "2017-10-05", "2017-07-01", "2017-10-06", "2017-07-22", "2017-04-09", "2017-07-09", "2017-06-24", "2017-08-05", "2017-05-20", "2017-12-24", "2017-11-18", "2017-10-22", "2017-05-06", "2017-07-29", "2017-01-28", "2017-05-30", "2017-05-21", "2017-11-04", "2017-01-14", "2017-01-15", "2017-01-07", "2017-03-04", "2017-06-04", "2017-06-17", "2017-07-02", "2017-07-16", "2017-06-25", "2017-05-07", "2017-01-31", "2017-01-30", "2017-10-29", "2017-01-21", "2017-03-12", "2017-04-30", "2017-06-11", "2017-01-02", "2017-02-05", "2017-09-10", "2017-10-07", "2017-12-10", "2017-03-25", "2017-10-01", "2017-04-15", "2017-07-23", "2017-03-11", "2017-02-25", "2017-03-05", "2017-08-13", "2017-12-09", "2017-10-21", "2017-05-14", "2017-02-11", "2017-02-18", "2017-10-08", "2017-05-01", "2017-04-03", "2017-05-13", "2017-11-12", "2017-04-22", "2017-05-28", "2017-11-26", "2017-11-11", "2017-12-17", "2017-08-27", "2017-09-17", "2017-07-08", "2017-09-09", "2017-10-03", "2017-02-12", "2017-08-12", "2017-04-23", "2017-12-30", "2017-12-31", "2017-03-19", "2017-11-25", "2017-03-18", "2017-02-02", "2017-12-16", "2017-06-03", "2017-11-19", "2017-10-15", "2017-10-04", "2017-04-16", "2017-03-26", "2017-01-29", "2017-02-26", "2017-06-18", "2017-06-10", "2017-01-01", "2017-02-01", "2017-04-08", "2017-10-02", "2017-08-26", "2017-10-28", "2017-08-20", "2017-12-03", "2017-09-16", "2017-04-02", "2017-02-19", "2017-04-29", "2017-05-29", "2017-09-02", "2017-08-06", "2017-08-19", "2017-01-08", "2017-11-05", "2017-09-23", "2017-12-23", "2017-07-30", "2017-09-03")

    val dateArray: ArrayBuffer[String] = ArrayBuffer()

    while (beginDate.compareTo(endDate) <= 0) {
      dateArray += dateFormat.format(beginDate)
      calendar.add(dateFiled, 1)
      beginDate = calendar.getTime
    }
    val ldate = dateArray.toList
//    for (v <- str)
    println(endDate)
    var de3: Int = ldate.size


    System.out.println("de3" + de3)
    import scala.collection.JavaConversions._
    for (date <- ldate) {
      val i: Int = 1
      // int de;
      for (v <- str) {
        if (dateFormat.parse(v) == dateFormat.parse(date)) {
          System.out.println("000:" + dateFormat.parse(date))
          de3-=i
        }

      }
      // System.out.println(i);
      //System.out.println(dateFormat.parse(date))
    }
    System.out.println(de3+"个工作日")


}
}
