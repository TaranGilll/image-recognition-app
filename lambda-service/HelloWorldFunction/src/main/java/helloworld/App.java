package helloworld;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.HttpMethod;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Map;
import java.util.Optional;

import java.net.URL;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.services.rekognition.model.DetectLabelsRequest;
import com.amazonaws.services.rekognition.model.DetectLabelsResult;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.services.rekognition.model.Label;
import com.amazonaws.services.rekognition.model.Parent;
import com.amazonaws.services.rekognition.model.S3Object;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;
import com.amazonaws.services.rekognition.model.AmazonRekognitionException;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;


public class App implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

  @Override
  public APIGatewayProxyResponseEvent handleRequest(final APIGatewayProxyRequestEvent input, final Context context) { 

    final Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", ContentType.APPLICATION_JSON.toString());
    headers.put("X-Custom-Header", ContentType.APPLICATION_JSON.toString());
    headers.put("X-Requested-With", "*");
    headers.put("Access-Control-Allow-Origin", "*");
    headers.put("Access-Control-Allow-Credentials", "true");
    headers.put("Access-Control-Allow-Methods", "GET,PUT,POST,DELETE,OPTIONS");
    headers.put("Access-Control-Expose-Headers", "date");
    headers.put("Access-Control-Allow-Headers",
    "Access-Control-Allow-Headers, Origin,Accept, X-Requested-With, Content-Type, Access-Control-Request-Method, Access-Control-Request-Headers");
    
    APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent().withHeaders(headers);
    AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.defaultClient();
    AmazonS3 s3Client = AmazonS3ClientBuilder.defaultClient();
    String bucket = "S3_BUCKET_NAME";


    Gson GSON = new Gson();
    JsonObject inputJson = Optional.ofNullable(input.getBody())
        .filter(s -> s.length() > 0)
        .map(JsonParser::parseString)
        .filter(JsonElement::isJsonObject)
        .map(JsonElement::getAsJsonObject)
        .orElse(null);


    if(input.getHttpMethod().equalsIgnoreCase(HttpMethod.POST.name())) {
        if(input.getPath().equalsIgnoreCase("/upload")) {

            String fileName = inputJson.get("fileName").getAsString();
            String directoryName = bucket;

            Instant now = Instant.now();
            Instant plus = now.plus(3, ChronoUnit.MINUTES);
            GeneratePresignedUrlRequest generatePresignedUrlRequest = new GeneratePresignedUrlRequest(directoryName,
                    fileName)
                    .withMethod(HttpMethod.PUT)
                    .withExpiration(Date.from(plus));
            URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
            
            JsonObject returnObject = new JsonObject();
            returnObject.addProperty("imageURL", url.toString());
            return response.withStatusCode(HttpStatus.SC_OK).withBody(returnObject.toString());
        }
    }

    if(input.getHttpMethod().equalsIgnoreCase(HttpMethod.POST.name())) {
        if(input.getPath().equalsIgnoreCase("/image")) {

            String photo = inputJson.get("fileName").getAsString();
            List<ImageLabel> objectsInImage = new ArrayList<ImageLabel>();

            DetectLabelsRequest request = new DetectLabelsRequest()
                .withImage(new Image().withS3Object(new S3Object().withName(photo).withBucket(bucket)))
                .withMaxLabels(10).withMinConfidence(75F);

            try {
                DetectLabelsResult result = rekognitionClient.detectLabels(request);
                List<Label> labels = result.getLabels();

                for (Label label : labels) {
                    // SET LABEL NAME + CONFIDENCE
                    ImageLabel newObject = new ImageLabel();
                    newObject.setLabel(label.getName());
                    newObject.setConfidence(label.getConfidence().toString());

                    // SET PARENT LABELS
                    List<Parent> parents = label.getParents();
                    List<String> parentLabels = new ArrayList<String>();
                    for (Parent parent : parents) {
                        parentLabels.add(parent.getName());
                    }
                    newObject.setParentLabels(parentLabels.isEmpty() ? null : parentLabels);

                    // ADD TO RETURN OBJECT
                    objectsInImage.add(newObject);
                }
            } catch (AmazonRekognitionException e) {
                e.printStackTrace();
            }
            
            JsonObject returnObj = new JsonObject();
            returnObj.add("labels", JsonParser.parseString(GSON.toJson(objectsInImage)));
            return response.withStatusCode(HttpStatus.SC_OK).withBody(GSON.toJson(returnObj));
        }
    }

    return response.withStatusCode(HttpStatus.SC_BAD_REQUEST).withBody("Error: Unable to anyalze image. Please try again later!");
    }
}
