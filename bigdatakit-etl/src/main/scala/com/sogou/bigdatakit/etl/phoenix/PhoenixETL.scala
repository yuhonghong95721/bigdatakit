package com.sogou.bigdatakit.etl.phoenix

import com.typesafe.config.ConfigFactory
import org.apache.spark.sql.hive.HiveContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * Created by Tao Li on 2016/3/15.
  */
object PhoenixETL {
  def main(args: Array[String]) {
    if (args.length != 1) {
      System.err.println("logdate is needed")
      System.exit(1)
    }

    val config = ConfigFactory.load()
    val settings = new PhoenixETLSettings(config, args)

    val logdate = args(0)

    val conf = new SparkConf()
    for ((k, v) <- settings.sparkConfigMap) conf.set(k, v)
    conf.setAppName(s"${settings.SPARK_APP_NAME}-$logdate").setMaster(settings.SPARK_MASTER_URL)
    val sc = new SparkContext(conf)
    for((k, v) <- settings.hadoopConfigMap) sc.hadoopConfiguration.set(k, v)
    val sqlContext = new HiveContext(sc)

    Class.forName(settings.PROCESSOR_CLASS).newInstance match {
      case processor: PhoenixTransformer =>
        val df = processor.transform(sqlContext, logdate)
        PhoenixETLUtils.toPhoenix(df, settings.TABLE, logdate, settings.PARALLELISM)
      case processor: PhoenixRunner => processor.run(sqlContext, logdate)
      case _ => throw new RuntimeException(s"not support processor: ${settings.PROCESSOR_CLASS}")
    }
  }
}