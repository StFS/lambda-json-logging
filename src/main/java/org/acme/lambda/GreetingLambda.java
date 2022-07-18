package org.acme.lambda;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class GreetingLambda implements RequestHandler<Person, String> {

    private static final Logger log = LoggerFactory.getLogger(GreetingLambda.class);

    @Override
    public String handleRequest(Person input, Context context) {
        log.info("This line will be logged as JSON");
        if ("error".equals(input.getName())) {
            throw new RuntimeException("This exception will not be logged as JSON");
        }
        return "Hello " + input.getName();
    }
}
