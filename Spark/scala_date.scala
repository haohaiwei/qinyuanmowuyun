/*
package by hao 2018-01-31
功能:计算工作日
 */
import org.apache.spark.{SparkConf, SparkContext}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.expressions.UserDefinedAggregateFunction
object java_date{
  def main(args: Array[String]) {


    //计算时间间隔
    val sparkConf = new SparkConf().setAppName("java_date").setMaster("local")
    val sparkContext = new SparkContext(sparkConf)
    val sqlContext = new SQLContext(sparkContext)
    //产生日期序列
    import java.util.Calendar
    import java.util.Date
    import java.text.SimpleDateFormat
    import scala.collection.mutable.ListBuffer

    // 输入开始日期和结束日期
    val stringDateBegin: String = args(0)
    val stringDateEnd: String =args(1)
    val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
    val dateBegin: Date = dateFormat.parse(stringDateBegin)
    val dateEnd: Date = dateFormat.parse(stringDateEnd)


    if (dateBegin.after(dateEnd)) throw new Exception("日期范围非法")
    // 总天数
    val days = ((dateBegin.getTime - dateEnd.getTime) / (24 * 60 * 60 * 1000)).asInstanceOf[Int]+1
    // 总周数，
    val weeks = days / 7
    var rs = 0
    // 整数周
    if (days % 7 == 0) rs = days - 2 * weeks
    else {
      val begCalendar = Calendar.getInstance
      val endCalendar = Calendar.getInstance
      begCalendar.setTime(dateBegin)
      endCalendar.setTime(dateEnd)
      // 周日为1，周六为7
      val beg = begCalendar.get(Calendar.DAY_OF_WEEK)
      val end = endCalendar.get(Calendar.DAY_OF_WEEK)
      if (beg > end) rs = days - 2 * weeks + 1
      else if (beg < end) if (end == 7) rs = days - 2 * weeks - 1
      else rs = days - 2 * weeks
      else if (beg == 1 || beg == 7) rs = days - 2 * weeks - 1
      else rs = days - 2 * weeks

    }

    System.out.println(dateFormat.format(dateBegin) + "到" + dateFormat.format(dateEnd) + "中间有" + rs + "个工作日")
    // 计算日期间隔天数
  }
}

