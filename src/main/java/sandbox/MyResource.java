
package sandbox;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

@Path("/")
public class MyResource {
	@GET
	@Produces(MediaType.TEXT_PLAIN)
	@APIResponse(responseCode = "200", description = "It did something!")
	public String get() {
		return "It worked!";
	}
}
