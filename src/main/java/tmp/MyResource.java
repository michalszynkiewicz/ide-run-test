
package tmp;

import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class MyResource {
	@GET
	@APIResponse(responseCode = "200", description = "It did something!")       // mstodo check with it
	public String get() {
//		LogManager.getLogManager().getLogger("MyWunderbarLogger").info("logging with: " + LogManager.getLogManager());
//		LogFactory.getLog(getClass()).info("commons logging worked :O");
		return "It worked!";
	}
}
