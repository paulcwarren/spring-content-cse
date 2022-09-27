# Running tests

Add an application.properties with your s3 credentials:

```
spring.content.s3.accessKey=<your access key id>
spring.content.s3.secretKey=<your secret key>
```

1. Ensure docker is running 
2. Execute `AWS_BUCKET=<your bucket> mvn clean test`

