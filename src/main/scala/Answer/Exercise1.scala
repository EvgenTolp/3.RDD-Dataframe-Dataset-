/*  Загрузка данных в DataFrame в Parquet и в csv. */

package Answer

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.DataFrame
import org.apache.spark.sql.functions._


object Exercise1 extends App {

  def readCSV(path: String)(implicit spark: SparkSession): DataFrame =
    spark.read
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(path)

  def readParquet(path: String)(implicit spark: SparkSession): DataFrame = spark.read.load(path)

  def writeParquet(df: DataFrame, path: String)(implicit spark: SparkSession): Unit = {
    try {
      df.repartition(1).write.mode("overwrite").parquet(path)
      println(s"Writing to the file on path '$path' was successful")
    } catch {
      case _: Throwable => println("Got some other kind of Throwable exception")
    }
  }

  def processPopularBorough(taxiDF: DataFrame, taxiZonesDF: DataFrame) = {
    taxiDF
      .select("DOLocationID", "trip_distance")
      .filter(col("trip_distance") > 0)
      .join(taxiZonesDF, col("DOLocationID") === col("LocationID"))
      .groupBy(col("Borough"))
      .agg(
        count(col("Borough")) as "count"
      )
      .sort(col("count").desc)

  }

  implicit val spark = SparkSession.builder()
    .appName("Joins")
    .config("spark.master", "local")
    .getOrCreate()

  val countOutRow = 10
  val outPath = "out/TopLocation"

  val taxiFactsDF = readParquet("src/main/resources/data/yellow_taxi_jan_25_2018")

  val taxiZoneDF = readCSV("src/main/resources/data/taxi_zones.csv")

  val resultDf = processPopularBorough(taxiFactsDF, taxiZoneDF)

  resultDf.show(false)

  writeParquet(resultDf, outPath)

  spark.stop()
}