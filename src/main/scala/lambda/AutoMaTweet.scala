package lambda

import java.io.{File, FileInputStream}

import scala.collection.JavaConverters._
import java.nio.ByteBuffer
import java.nio.file.Files

import com.amazonaws.services.lambda.runtime.events.S3Event
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder
import com.amazonaws.services.rekognition.model.{AmazonRekognitionException, DetectLabelsRequest, Image, Label}
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.GetObjectRequest
import com.amazonaws.util.IOUtils
import twitter4j.auth.OAuthAuthorization
import twitter4j.{StatusUpdate, TwitterException, TwitterFactory}
import twitter4j.conf.ConfigurationBuilder

import scala.util.Random

/**
  * Created by Juan Manuel Torres
  * Simple lambda function that uses AWS Rekognition to get labels about the image, and tweets the results
  */
class AutoMaTweet {
  private val s3Client = new AmazonS3Client
  private val rekognitionClient = AmazonRekognitionClientBuilder.standard().build()
  private val tweetMessage = "I'm %f%% sure this picture contains a %s.\n#%s"

  // Twitter client initialization
  private val env = sys.env
  private val configBuilder = new ConfigurationBuilder()
  configBuilder.setDebugEnabled(true)
    .setOAuthConsumerKey(env.getOrElse("CONSUMER_KEY", ""))
    .setOAuthConsumerSecret(env.getOrElse("CONSUMER_SECRET", ""))
    .setOAuthAccessToken(env.getOrElse("ACCESS_TOKEN", ""))
    .setOAuthAccessTokenSecret(env.getOrElse("TOKEN_SECRET", ""))
  private val configuration = configBuilder.build()
  private val auth = new OAuthAuthorization(configuration)
  private val twitter = new TwitterFactory(configuration).getInstance(auth)

  /**
    * This is the entry point of the application, lambda will pass an S3 event to this handler
    *
    * @param event: S3 event sent by lambda
    */
  def lambdaHandler(event: S3Event): Unit = {
    Console.println("The AutoMaTweet handler is starting")
    val record = event.getRecords.asScala.head
    val bucket: String = record.getS3.getBucket.getName
    val key: String = record.getS3.getObject.getKey

    try {
      Console.println(s"Downloading key $key from $bucket")
      val file = downloadImage(bucket, key)
      val labels = rekognitionLabels(file)
      tweetImage(key, labels, file)
    } catch {

      // Log the error, but do not throw it to prevent lambda from re-trying
      case e: Exception => Console.err.println(e.toString)
    }
  }

  /**
    * Download the image, this image will be used in Rekognition and Twitter
    *
    * @param bucket: Name of the S3 bucket
    * @param key: Name of the key in the bucket
    * @return
    */
  def downloadImage(bucket: String, key: String): File = {
    val s3Object = s3Client.getObject(
      new GetObjectRequest(bucket, key)
    )
    val inputStream = s3Object.getObjectContent

    // Process the objectData stream.
    val filename = s"/tmp/$key"
    Console.println(s"Writing to local file $filename")
    val imageFile = new File(filename)
    Files.copy(inputStream, imageFile.toPath)
    if (imageFile.exists) Console.println(s"File $filename was created locally") else Console.println("FILE WAS NOT CREATED")
    imageFile
  }

  /**
    * Use the Rekognition client to get labels (tags) for the image.
    *
    * @param file: This is the file of the image we downloaded in the `downloadImage` method
    * @return
    */
  def rekognitionLabels(file: File): Seq[Label] = {
    Console.println(s"Detected labels for image")
    val inputStream = new FileInputStream(file)
    val imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream))
    try {
      rekognitionClient.detectLabels(
        new DetectLabelsRequest()
          .withImage(new Image()
          .withBytes(imageBytes))
          .withMaxLabels(20)
          .withMinConfidence(30F)
      ).getLabels.asScala
    } catch {
      case e: AmazonRekognitionException =>
        Console.err.println(s"There was an amazon rekognition exception")
        throw e
      case e: Exception =>
        Console.err.println(s"There was a generic exception while analyzing the image")
        throw e
    }
  }

  /**
    * Tweet the image with a simple message saying something about the image.
    *
    * @param key: Name of the key in the bucket
    * @param labels: Collection of Rekognition labels
    * @param file: This is the file of the image we downloaded in the `downloadImage` method
    */
  def tweetImage(key: String, labels: Seq[Label], file: File): Unit = {
    try{

      // build the message we are going to tweet
      labels.foreach(label => Console.println(s"${label.getName}: ${label.getConfidence.toString}"))
      val label = selectRandomLabel(labels)
      val tags = labels.map(label => label.getName).mkString(" #")
      var message: String = ""
      if (tags.isEmpty) {
        message = "Could not identify any elements in this picture"
      } else {
        message = tweetMessage.format(label.getConfidence, label.getName, tags)
      }
      Console.println(s"Message is $message")

      Console.println("Trying twitter.updateStatus")
      val text = if (message.length > 140) message.substring(0, 140) else message
      val status = new StatusUpdate(text)
      status.setMedia(file)
      twitter.updateStatus(status)
    } catch {
      case e: TwitterException =>
        Console.err.println(s"There was a twitter error")
        throw e
    }
  }

  /**
    * Get a random label from the collection of labels... to mix it up a bit :)
    *
    * @param labels: Collection of Rekognition labels
    * @return
    */
  def selectRandomLabel(labels: Seq[Label]): Label = {
    val rand = new Random(System.currentTimeMillis())
    val random_index = rand.nextInt(labels.length)
    labels(random_index)
  }
}
