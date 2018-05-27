/*
package by hao 2018-01-26 10:17
项目:数据处理与清洗
数据:安阳市民之家
授权:安阳市民之家

 */
import org.apache.spark._
import org.apache.spark.sql._
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.api.java.UDF2
import org.apache.spark.sql.expressions.UserDefinedAggregateFunction
import org.apache.spark.sql.types.DataType
import java.util.Calendar
import java.util.Date
import java.text.SimpleDateFormat
object sql {

  def main (args: Array[String]) {
    val conf  = new SparkConf().setAppName("SQL").setMaster("local")

    val spark = SparkSession.builder().config(conf).getOrCreate()
    //定义一个url连接，需要指定字符以避免乱码
    val url = "jdbc:mysql://192.168.1.220:3306/mysql?useUnicode=true&characterEncoding=utf8"

    //加载数据库中表
    val jdbcDF = spark.read.format( "jdbc" ).options(
      Map( "url" -> url,
        "user" -> "root",
        "password" -> "root",
        "dbtable" -> "mysql.pre_accept" )).load()

    val joinDF1 = spark.read.format( "jdbc" ).options(
      Map("url" -> url ,
        "user" -> "root",
        "password" -> "root",
        "dbtable" -> "mysql.pre_apasinfo" )).load()

    val joinDF2 = spark.read.format( "jdbc" ).options(
      Map ( "url" -> url ,
        "user" -> "root",
        "password" -> "root",
        "dbtable" -> "mysql.pre_transact" )).load()
    //自定义函数计算工作日
    def udf(date01:String,date02:String) = {



      // 传参解析
      val stringDateBegin: String = date01
      val stringDateEnd: String =date02
      //解析日期格式
      val dateFormat: SimpleDateFormat = new SimpleDateFormat("yyyyMMdd")
      val dateBegin: Date = dateFormat.parse(stringDateBegin)
      val dateEnd: Date = dateFormat.parse(stringDateEnd)


      if (dateBegin.after(dateEnd)) throw new Exception("日期范围非法")
      var rs=0
      if (dateBegin.getTime-dateEnd.getTime==0) rs=0
      else {
        // 总天数
        val days = ((dateBegin.getTime - dateEnd.getTime) / (24 * 60 * 60 * 1000)).asInstanceOf[Int] + 1
        // 总周数，
        val weeks = days / 7
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
      }
      rs
      }
    spark.udf.register("udf",udf(_:String,_:String))

    //以projid字段作多表join操作
    val df=((((((((((joinDF1.join(jdbcDF,"projid"))
      .join(joinDF2,"projid"))
      .drop(colName = "AREACODE"))
      .drop(colName = "HANDER_DEPTID"))
      .drop(colName = "HANDER_DEPTNAME"))
      .drop(colName ="DATAVERSION"))
      .drop(colName ="SYNC_STATUS"))
      .drop(colName ="CREATE_TIME"))
      .drop(colName = "BELONGSYSTEM"))
      .drop(colName = "EXTEND"))


    val join = spark.read.format( "jdbc" ).options(
      Map ( "url" -> url ,
        "user" -> "root",
        "password" -> "root",
        "dbtable" -> "mysql.pre_apasinfo" )).load()
    //加载数据库中表

    //注册临时表
    join.createOrReplaceTempView("ffnn")

    //按权力类型分组
    val ay01=spark.sql("select ql_kind," +
      "count(ql_kind) num" +
      " from ffnn " +
      "group by ql_kind")
    //由于要调用jdbc，需要导入Java类，也可直接在url中指明
    val prop=new java.util.Properties
    prop.setProperty("user","root")
    prop.setProperty("password","root")
    //写入到数据库中
    ay01.write.mode(SaveMode.Overwrite).jdbc(url,"mysql.ql_kind",prop)
    //统计服务类型
    val ay02=spark.sql("select servicename," +
      "count(servicename) num " +
      "from ffnn " +
      "group by servicename")
    //写入到数据库中
    ay02.write.mode(SaveMode.Overwrite).jdbc(url,"mysql.servicename",prop)

    //统计权力类型为JF的即办件handlestate状态
    val ay03=spark.sql("select count(infotype)," +
      "handlestate," +
      "ql_kind " +
      "from ffnn " +
      "where ql_kind='JF' and infotype='即办件' " +
      "group by handlestate,ql_kind")

    //加载数据库中表
    val df01 = spark.read.format( "jdbc" ).options(
      Map ( "url" -> url ,
        "user" -> "root",
        "password" -> "root",
        "dbtable" -> "mysql.join" )).load()


    //注册临时表
    df01.createOrReplaceTempView("join02")
    //筛选出即办件
    val df03=spark.sql("select * from join02 where INFOTYPE='即办件'")
    //注册临时表
    df03.createOrReplaceTempView("jibanjian")
    //筛选出承诺件
    df03.printSchema()

    val df04=spark.sql("select * from join02 where INFOTYPE='承诺件'")
    //注册临时表
    df04.createOrReplaceTempView("chengnuojian")
    //统计即办件实际所用时间与许诺时间
    val ay04=spark.sql("select udf(ACCEPT_TIME,TRANSACT_TIME) actualtime," +
      "accept_time," +
      "TRANSACT_TIME," +
      "PROMISEVALUE " +
      "from jibanjian")
    //注册临时表
    ay04.show


    ay04.createOrReplaceTempView("jiban01")
    //统计各部门每天的即办件
    val ay05=spark.sql("select DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd') date " +
      ",count(projid) num " +
      ",deptname  " +
      "from jibanjian " +
      "group by DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd'),deptname")
    //统计各部门每天的即承诺件
    val ay06=spark.sql("select DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd') date ," +
      "count(projid) num ," +
      "deptname  " +
      "from chengnuojian " +
      "group by DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd'),deptname")
    //注册临时表
    ay06.createOrReplaceTempView("second")
    //统计即办件实际所用时间与许诺时间最大最小以及平均时间
    val ay07=spark.sql("select max(actualvalue)," +
      "max(promisevalue)," +
      "min(actualvalue)," +
      "min(promisevalue)," +
      "avg(actualvalue)," +
      "avg(promisevalue) " +
      "from jiban01")
    //统计承诺件实际所用时间与许诺时间
    val ay08=spark.sql("select udf(ACCEPT_TIME,TRANSACT_TIME) actualvalue ," +
      "TRANSACT_TIME," +
      "PROMISEVALUE " +
      "from chengnuojian")
    //注册临时表
    ay08.createOrReplaceTempView("chengnuo")
    //统计承诺件实际所用时间与许诺时间最大最小以及平均时间
    val ay09=spark.sql("select max(actualvalue)," +
      "max(promisevalue)," +
      "min(actualvalue)," +
      "min(promisevalue)," +
      "avg(actualvalue)," +
      "avg(promisevalue) " +
      "from chengnuo")
    //读取临时表中3个字段信息
    val ay10=spark.sql("select ACCEPT_TIME," +
      "projid," +
      "deptname  " +
      "from join02")
    //注册临时表
    ay10.createOrReplaceTempView("four")
    //指定日期格式，并统计各部门每天的办件数
    val ay11=spark.sql("select DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd') date ," +
      "count(projid) num ," +
      "deptname" +
      "  from four " +
      "group by DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd'),deptname")
    //注册临时表
    ay11.createOrReplaceTempView("five")
    //按部门统计每天办件数的最大值最小值，以及平均值
    val ay12=spark.sql("select max(num)," +
      "min(num)," +
      "avg(num)," +
      "deptname " +
      "from five " +
      "group by deptname")

    val ay13=spark.sql("select DATE_FORMAT(ACCEPT_TIME,'YY-MM-dd') date ," +
      "deptname," +
      "handlestate," +
      "projid  " +
      "from join02 ")
    //注册临时表
    ay13.createOrReplaceTempView("first")

    //统计各部门每天的承诺件办理状态
    val ay14=spark.sql("select deptname," +
      "handlestate," +
      "count(projid) num ," +
      "date  " +
      "from first " +
      "group by deptname,handlestate,date")
    //注册临时表
    ay14.createOrReplaceTempView("test000")
    //按部门统计承诺件最大最小以及平均办件数
    val ay15=spark.sql("select max(num)," +
      "min(num)," +
      "avg(num)," +
      "deptname " +
      "from test000 " +
      "group by deptname")
    //统计各部门每天的即办件办理状态
    val ay16=spark.sql("select deptname," +
      "handlestate," +
      "count(projid) num ," +
      "date " +
      "from first " +
      "group by deptname,handlestate,date")

    ay16.createOrReplaceTempView("three")
    ////按部门统计即办件最大最小以及平均办件数
    val ay17=spark.sql("select max(num)," +
      "min(num)," +
      "avg(num)," +
      "deptname " +
      "from three " +
      "group by deptname")

    //统计各部门每天的承诺件办理状态
    val ay18=spark.sql("select deptname," +
      "handlestate," +
      "count(projid) num ," +
      "date  " +
      "from first " +
      "group by deptname,handlestate,date")

    ay18.createOrReplaceTempView("test000")

    //按部门统计承诺件最大最小以及平均办件数
    val ay19=spark.sql("select max(num)," +
      "min(num)," +
      "avg(num)," +
      "deptname " +
      "from test000 " +
      "group by deptname")

    //统计各部门每天的即办件办理状态
    val ay20=spark.sql("select deptname," +
      "handlestate," +
      "count(projid) num ," +
      "date  " +
      "from first " +
      "group by deptname,handlestate,date")

    //统计各部门每天的办件数
    val ay22=spark.sql("select deptname," +
      "count(projid) num " +
      "from join02 " +
      "group by deptname")
    //统计各部门办件类目
    val ay23=spark.sql("select servicename," +
      "deptname " +
      "from join02 " +
      "group by deptname,servicename")
    //加载数据库中表
    val df02 = spark.read.format( "jdbc" ).options(
      Map ( "url" -> url ,
        "user" -> "root",
        "password" -> "root",
        "dbtable" -> "mysql.pre_evaluate" )).load()
    //注册成临时表
    df02.createOrReplaceTempView("jion03")
    //按个人来统计评价结果
    val ay24=spark.sql("select object_type," +
      "object_name," +
      "evaluate_result," +
      "count(evaluate_result) num " +
      "from jion03 " +
      "where object_type='个人' " +
      "group by object_type,object_name,evaluate_result")
    //按部门来统计评价结果
    val ay25=spark.sql("select object_type," +
      "object_name," +
      "evaluate_result," +
      "count(evaluate_result) num " +
      "from jion03 " +
      "where object_type='部门' " +
      "group by object_type,object_name,evaluate_result")
    //按办件来统计评价结果
    val ay26=spark.sql("select object_type," +
      "object_name," +
      "evaluate_result," +
      "count(evaluate_result) num " +
      "from jion03 " +
      "where object_type='办件' " +
      "group by object_type,object_name,evaluate_result")

    //统计法人的办件结果
    val ay27=spark.sql("select transact_result," +
      "count(transact_result) num  " +
      "from join02  " +
      "where apply_type='法人' " +
      "group by transact_result")
    //统计个人的办件结果
    val ay28=spark.sql("select transact_result," +
      "count(transact_result) num " +
      "from join02  " +
      "where apply_type='个人' " +
      "group by transact_result")
    //
    spark.stop

  }
}
