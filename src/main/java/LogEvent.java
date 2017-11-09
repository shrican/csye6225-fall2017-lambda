import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.spec.PutItemSpec;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SNSEvent;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.*;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.UUID;

public class LogEvent implements RequestHandler<SNSEvent, Object> {

  private DynamoDB dynamoDb;
  private String DYNAMODB_TABLE_NAME = "csye6225";
  private Regions REGION = Regions.US_EAST_1;
  static final String FROM = "shrikant@csye6225-fall2017-mudholkars.me";
  static final String SUBJECT = "Reset Password Link";
  static final String HTMLBODY = "<h1>Amazon SES test (AWS SDK for Java)</h1>"
          + "<p>This email was sent with <a href='https://aws.amazon.com/ses/'>"
          + "Amazon SES</a> using the <a href='https://aws.amazon.com/sdk-for-java/'>"
          + "AWS SDK for Java</a>";

  private String textBody;

  public Object handleRequest(SNSEvent request, Context context) {
    String timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());

    //Loggers
    context.getLogger().log("Invocation started: " + timeStamp);
    context.getLogger().log("1: " + (request == null));
    context.getLogger().log("2: " + (request.getRecords().size()));
    context.getLogger().log(request.getRecords().get(0).getSNS().getMessage());
    timeStamp = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(Calendar.getInstance().getTime());
    context.getLogger().log("Invocation completed: " + timeStamp);

    //Execution
    String username = request.getRecords().get(0).getSNS().getMessage();
    String token = UUID.randomUUID().toString();
    this.initDynamoDbClient();
    this.dynamoDb.getTable(DYNAMODB_TABLE_NAME)
            .putItem(
                    new PutItemSpec().withItem(new Item()
                            .withString("id", username)
                            .withString("token", token)
                            .withInt("TTL", 1800)));
    textBody = "https://csye6225-fall2017.com/reset?email="+username+"&token="+token;
    try {
      AmazonSimpleEmailService client = AmazonSimpleEmailServiceClientBuilder.standard()
              .withRegion(Regions.US_EAST_1).build();
      SendEmailRequest emailRequest = new SendEmailRequest()
              .withDestination(
                      new Destination().withToAddresses(username))
              .withMessage(new Message()
                      .withBody(new Body()
                              .withHtml(new Content()
                                      .withCharset("UTF-8").withData(HTMLBODY))
                              .withText(new Content()
                                      .withCharset("UTF-8").withData(textBody)))
                      .withSubject(new Content()
                              .withCharset("UTF-8").withData(SUBJECT)))
              .withSource(FROM);
      client.sendEmail(emailRequest);
      System.out.println("Email sent!");
    } catch (Exception ex) {
      System.out.println("The email was not sent. Error message: "
              + ex.getMessage());
    }
    return null;
  }

  private void initDynamoDbClient() {
    AmazonDynamoDBClient client = new AmazonDynamoDBClient();
    client.setRegion(Region.getRegion(REGION));
    this.dynamoDb = new DynamoDB(client);
  }

}
