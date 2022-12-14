package com.example.s3.cse;

import com.github.paulcwarren.ginkgo4j.Ginkgo4jSpringRunner;
import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.vault.core.VaultOperations;
import org.springframework.vault.core.VaultTransitOperations;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.*;

import static com.github.paulcwarren.ginkgo4j.Ginkgo4jDSL.*;
import static com.jayway.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
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

	@Autowired
	private EnvelopeEncryptionService encrypter;

	@Autowired
	private VaultOperations vaultOperations;

	@LocalServerPort
	int port;

	private File f;

	{
		Describe("Client-side encryption", () -> {
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
					if (del.objects().size() > 0) {
						DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
								.bucket(System.getenv("AWS_BUCKET"))
								.delete(del)
								.build();

						s3.deleteObjects(multiObjectDeleteRequest);
					}
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
				It("should handle byte-range requests", () -> {
					Response r =
						given()
							.header("accept", "text/plain")
							.header("range", "bytes=18-27")
							.get("/files/" + f.getId() + "/content")
							.then()
							.statusCode(HttpStatus.SC_PARTIAL_CONTENT)
							.assertThat()
							.contentType(Matchers.startsWith("text/plain"))
							.and().extract().response();

					assertThat(r.asString(), is("encryption"));
				});
				Context("when the keyring is rotated", () -> {
					BeforeEach(() -> {
						encrypter.rotate();
					});
					It("should not change the stored content key", () -> {
						f = repo.findById(f.getId()).get();

						assertThat(new String(f.getContentKey()), startsWith("vault:v1"));
					});
					It("should still retrieve content decrypted", () -> {
						given()
								.header("accept", "text/plain")
								.get("/files/" + f.getId() + "/content")
								.then()
								.statusCode(HttpStatus.SC_OK)
								.assertThat()
								.contentType(Matchers.startsWith("text/plain"))
								.body(Matchers.equalTo("Hello Client-side encryption World!"));
					});
					It("should update the content key version when next stored", () -> {
						given()
								.contentType("text/plain")
								.content("Hello Client-side encryption World!")
								.when()
								.post("/files/" + f.getId() + "/content")
								.then()
								.statusCode(HttpStatus.SC_OK);

						f = repo.findById(f.getId()).get();
						assertThat(new String(f.getContentKey()), startsWith("vault:"));
						assertThat(new String(f.getContentKey()), not(startsWith("vault:v1")));
					});
				});
			});
		});
	}

	@Test
	public void noop() {
	}
}
