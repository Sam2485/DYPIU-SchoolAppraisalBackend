package com.director_appraisal.director_appraisal.config;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LoggingAspect {

    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut that matches all repositories, services, and Web REST controllers.
     */
    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *) || " +
              "within(@org.springframework.stereotype.Service *)")
    public void springBeanPointcut() {}

    /**
     * Advice that logs methods around their execution.
     */
    @Around("springBeanPointcut()")
    public Object logAround(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getSignature().getDeclaringTypeName();
        String methodName = joinPoint.getSignature().getName();
        String arguments = Arrays.toString(joinPoint.getArgs());

        log.info("==> ENTER: {}.{}() with args: {}", className, methodName, arguments);

        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - start;
            
            // Avoid printing very large returned objects in logs (e.g. byte arrays or file uploads)
            String resultStr;
            if (result instanceof byte[]) {
                resultStr = "[Byte Array: " + ((byte[]) result).length + " bytes]";
            } else if (result != null) {
                String temp = result.toString();
                resultStr = temp.length() > 500 ? temp.substring(0, 500) + "... [truncated]" : temp;
            } else {
                resultStr = "null";
            }

            log.info("<== EXIT: {}.{}() successfully executed in {} ms. Return value: {}", 
                    className, methodName, duration, resultStr);
            return result;
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - start;
            log.error("<!! EXCEPTION in {}.{}() executed in {} ms. Message: {}", 
                    className, methodName, duration, e.getMessage(), e);
            throw e;
        }
    }
}
