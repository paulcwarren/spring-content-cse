package com.example.s3.cse;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

@RunWith(Ginkgo4jSpringRunner.class)
//@Ginkgo4jConfiguration(threads=1)
@SpringBootTest(classes = S3CseApplication.class, webEnvironment= SpringBootTest.WebEnvironment.RANDOM_PORT)
public class S3CseApplicationTests {

	@Autowired
	private FileRepository repo;

	@Autowired
	private FileContentStore store;

	@Autowired
	private S3Client s3;

	@LocalServerPort
	int port;

	private File f;

	{
		Describe("S3CseApplicationTests", () -> {
			BeforeEach(() -> {
				RestAssured.port = port;

				f = repo.save(new File());
			});
			AfterEach(()-> {
				List<ObjectIdentifier> objs = new ArrayList<>();
				s3.listObjects(ListObjectsRequest.builder().bucket(System.getenv("AWS_BUCKET")).build())
						.contents()
						.forEach((obj) -> {
							objs.add(ObjectIdentifier.builder().key(obj.key()).build());
						});

				Delete del = Delete.builder()
						.objects(objs)
						.build();

				try {
					DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
							.bucket(System.getenv("AWS_BUCKET"))
							.delete(del)
							.build();

					s3.deleteObjects(multiObjectDeleteRequest);
				} catch (S3Exception e) {
					System.err.println(e.awsErrorDetails().errorMessage());
					fail();
				}
			});
			Context("given content", () -> {
				BeforeEach(() -> {
					given()
						.contentType("text/plain")
						.content("Hello Client-side encryption World!")
						.when()
						.post("/files/" + f.getId() + "/content")
						.then()
						.statusCode(HttpStatus.SC_CREATED);
				});
				It("should be stored encrypted", () -> {
					Optional<File> fetched = repo.findById(f.getId());
					assertThat(fetched.isPresent(), is(true));
					f = fetched.get();

					String contents = IOUtils.toString(s3.getObject(GetObjectRequest.builder().bucket(System.getenv("AWS_BUCKET")).key(f.getContentId().toString()).build()));
					System.out.println(contents);
					assertThat(contents, is(not("Hello Client-side encryption World!")));
				});
				It("should be retrieved decrypted", () -> {
					given()
						.header("accept", "text/plain")
						.get("/files/" + f.getId() + "/content")
						.then()
						.statusCode(HttpStatus.SC_OK)
						.assertThat()
						.contentType(Matchers.startsWith("text/plain"))
						.body(Matchers.equalTo("Hello Client-side encryption World!"));
				});
			});
		});
	}

	@Test
	public void noop() {
	}
}
