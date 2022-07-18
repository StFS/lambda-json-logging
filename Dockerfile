# (1)
FROM  public.ecr.aws/lambda/java:11

# (2) Copies artifacts into /function directory
ADD target/*-runner.jar /var/task/lib/runner.jar
ADD target/lib  /var/task/lib/

# (3) Setting the command to the Quarkus lambda handler
CMD ["io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler::handleRequest"]