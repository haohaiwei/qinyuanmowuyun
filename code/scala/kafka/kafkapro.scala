import java.util.{Properties, Random, UUID}
import kafka.producer.{KeyedMessage, Producer, ProducerConfig}
import org.codehaus.jettison.json.JSONObject

/**  *
Created by hao on 2018/4/17
  create a producer to send json message
  */
object kafkapro {
  private val random = new Random()
  private var pointer = -1
  private val city_no = Array(
    "371")
  private val group_no = Array("1")

  def dept_no() : Double = {
    random.nextInt(10)
  }
  def fila_no() : Int={
    random.nextInt(30)
  }
  def line_no(): Int={
    random.nextInt(1000)
  }


  def getcityno() : String = {
    pointer = pointer + 1
    if(pointer >= city_no.length) {
      pointer = 0
      city_no(pointer)
    } else {
      city_no(pointer)
    }
  }

  def main(args: Array[String]): Unit = {
    val topic = "kafkademo"
    val brokers = "hadoop-2:9092,hadoop-3:9092,hadoop-5:9092"
    val props = new Properties()
    props.put("metadata.broker.list", brokers)
    props.put("serializer.class", "kafka.serializer.StringEncoder")

    val kafkaConfig = new ProducerConfig(props)
    val producer = new Producer[String, String](kafkaConfig)

    while(true) {
      // prepare event data
      val event = new JSONObject()
      event
        .put("uid", UUID.randomUUID())//随机生成用户id
        .put("event_time", System.currentTimeMillis.toString) //记录时间发生时间
        .put("city_no", getcityno) //城市号
        .put("line_no", line_no) //线路号
        .put("dept_no",dept_no) //部门代码
        .put("group_no",group_no) //组代码
        .put("fila_no",fila_no) //次数


      // produce event message
      producer.send(new KeyedMessage[String, String](topic, event.toString))
      println("Message sent: " + event)

      Thread.sleep(200)
    }
  }
}
