# lambda-json-logging Project

A project to demonstrate a problem with Quarkus AWS Lambdas logging with JSON when it
throws an exception.

## The Problem

When a Quarkus AWS Lambda project is configured to log out in JSON format (by adding the
`quarkus-logging-json` extension), all logs are properly logged out in JSON format.

However, when the lambda handler throws an exception, the exception and its stack trace
is output in plain text where it should be logged out as JSON.

## Reproducing

In order to see this problem in action, check out this project and perform the following steps:

```shell script
./mvnw package
```

### Quarkus Dev Server works properly

First of all we can see that `quarkus:dev` will behave correctly.

Start the project using the Quarkus Dev server:

```shell script
./mvnw quarkus:dev
```

Notice that the logging output is in JSON format.

Then in a separate terminal, verify that the lambda works correctly:

```shell script
curl -XPOST "http://localhost:8080/2015-03-31/functions/function/invocations" -d '{"name":"works"}'
```

This will produce the follwing log output in the Quarkus logs:

```
{"timestamp":"2022-07-18T13:42:18.046Z","sequence":924,"loggerClassName":"org.slf4j.impl.Slf4jLogger","loggerName":"org.acme.lambda.GreetingLambda","level":"INFO","message":"This line will be logged as JSON","threadName":"Lambda Thread (DEVELOPMENT)","threadId":67,"mdc":{},"ndc":"","hostName":"co-1030","processName":"lambda-json-logging-dev.jar","processId":3546}
```

Then call the lambda again, forcing it to throw an exception:

```shell script
curl -XPOST "http://localhost:8080/2015-03-31/functions/function/invocations" -d '{"name":"error"}'
```

And notice that this will produce the following two log lines, both JSON formatted:

```
{"timestamp":"2022-07-18T13:45:29.148Z","sequence":925,"loggerClassName":"org.slf4j.impl.Slf4jLogger","loggerName":"org.acme.lambda.GreetingLambda","level":"INFO","message":"This line will be logged as JSON","threadName":"Lambda Thread (DEVELOPMENT)","threadId":67,"mdc":{},"ndc":"","hostName":"co-1030","processName":"lambda-json-logging-dev.jar","processId":3546}
{"timestamp":"2022-07-18T13:45:29.149Z","sequence":926,"loggerClassName":"org.jboss.logging.Logger","loggerName":"io.quarkus.amazon.lambda.runtime.AbstractLambdaPollLoop","level":"ERROR","message":"Failed to run lambda (DEVELOPMENT)","threadName":"Lambda Thread (DEVELOPMENT)","threadId":67,"mdc":{},"ndc":"","hostName":"co-1030","processName":"lambda-json-logging-dev.jar","processId":3546,"exception":{"refId":1,"exceptionType":"java.lang.RuntimeException","message":"This exception will not be logged as JSON","frames":[{"class":"org.acme.lambda.GreetingLambda","method":"handleRequest","line":17},{"class":"org.acme.lambda.GreetingLambda","method":"handleRequest","line":9},{"class":"io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder$1","method":"processRequest","line":170},{"class":"io.quarkus.amazon.lambda.runtime.AbstractLambdaPollLoop$1","method":"run","line":130},{"class":"java.lang.Thread","method":"run","line":833}]}}
```

### Running in a Docker container does not work as expected

Now let's see how logging behaves when we run within a Docker container (using the `prod` profile).

First stop the `quarkus:dev` server if it's still running.

Build the container:

```shell script
docker build -t test_quarkus_lambda_json_logging .
```

And now we'll run the Docker container, exposing port 8080:

```shell script
docker run -p 8080:8080 test_quarkus_lambda_json_logging
```

Now let's invoke the lambda with legal input (not throwing an exception):

```shell script
curl -XPOST "http://localhost:8080/2015-03-31/functions/function/invocations" -d '{"name":"works"}'
```

Notice that the output of the lambda in the container is mostly JSON, at least everything that our
code is logging out is output as JSON. There are some non JSON lines in there that contain
meta-data for AWS:

```
START RequestId: 07cf8ff9-6e4d-4c3a-8740-8a28f10cd1c7 Version: $LATEST
{"timestamp":"2022-07-18T13:49:59.573Z","sequence":6,"loggerClassName":"org.jboss.logging.Logger","loggerName":"io.quarkus","level":"INFO","message":"lambda-json-logging 1.0.0-SNAPSHOT on JVM (powered by Quarkus 2.10.2.Final) started in 0.917s. ","threadName":"main","threadId":1,"mdc":{},"ndc":"","hostName":"badd0c233df0","processName":"lambdainternal.AWSLambda","processId":13}
{"timestamp":"2022-07-18T13:49:59.58Z","sequence":7,"loggerClassName":"org.jboss.logging.Logger","loggerName":"io.quarkus","level":"INFO","message":"Profile prod activated. ","threadName":"main","threadId":1,"mdc":{},"ndc":"","hostName":"badd0c233df0","processName":"lambdainternal.AWSLambda","processId":13}
{"timestamp":"2022-07-18T13:49:59.581Z","sequence":8,"loggerClassName":"org.jboss.logging.Logger","loggerName":"io.quarkus","level":"INFO","message":"Installed features: [amazon-lambda, cdi]","threadName":"main","threadId":1,"mdc":{},"ndc":"","hostName":"badd0c233df0","processName":"lambdainternal.AWSLambda","processId":13}
{"timestamp":"2022-07-18T13:49:59.602Z","sequence":9,"loggerClassName":"org.slf4j.impl.Slf4jLogger","loggerName":"org.acme.lambda.GreetingLambda","level":"INFO","message":"This line will be logged as JSON","threadName":"main","threadId":1,"mdc":{},"ndc":"","hostName":"badd0c233df0","processName":"lambdainternal.AWSLambda","processId":13}
END RequestId: 07cf8ff9-6e4d-4c3a-8740-8a28f10cd1c7
REPORT RequestId: 07cf8ff9-6e4d-4c3a-8740-8a28f10cd1c7	Init Duration: 0.26 ms	Duration: 1167.89 ms	Billed Duration: 1168 ms	Memory Size: 3008 MB	Max Memory Used: 3008 MB
```

I believe these meta-data lines must be output using this format for the AWS infrastructure to 
pick it up as input into Cloud Watch (START, END and REPORT lines).

But now, let's invoke the lambda, forcing it to throw an exception:

```shell script
curl -XPOST "http://localhost:8080/2015-03-31/functions/function/invocations" -d '{"name":"error"}'
```

Now, we see the following in the lambda logs:

```
START RequestId: 133294a2-4948-4811-82a8-d5fd08414da6 Version: $LATEST
{"timestamp":"2022-07-18T13:54:42.354Z","sequence":10,"loggerClassName":"org.slf4j.impl.Slf4jLogger","loggerName":"org.acme.lambda.GreetingLambda","level":"INFO","message":"This line will be logged as JSON","threadName":"main","threadId":1,"mdc":{},"ndc":"","hostName":"badd0c233df0","processName":"lambdainternal.AWSLambda","processId":13}
This exception will not be logged as JSON: java.lang.RuntimeException
java.lang.RuntimeException: This exception will not be logged as JSON
	at org.acme.lambda.GreetingLambda.handleRequest(GreetingLambda.java:17)
	at org.acme.lambda.GreetingLambda.handleRequest(GreetingLambda.java:9)
	at io.quarkus.amazon.lambda.runtime.AmazonLambdaRecorder.handle(AmazonLambdaRecorder.java:85)
	at io.quarkus.amazon.lambda.runtime.QuarkusStreamHandler.handleRequest(QuarkusStreamHandler.java:58)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(Unknown Source)
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(Unknown Source)
	at java.base/java.lang.reflect.Method.invoke(Unknown Source)

END RequestId: 133294a2-4948-4811-82a8-d5fd08414da6
REPORT RequestId: 133294a2-4948-4811-82a8-d5fd08414da6	Duration: 79.10 ms	Billed Duration: 80 ms	Memory Size: 3008 MB	Max Memory Used: 3008 MB
```

I would have imagined that the exception being logged out should have been logged out in JSON
format?