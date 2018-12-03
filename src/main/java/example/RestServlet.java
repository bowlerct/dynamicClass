package example;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

@WebServlet(urlPatterns = { "/Json" }, loadOnStartup = 1)
public class RestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static Logger LOG = Logger.getGlobal();
	
	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
		response.setContentType("text/html");
		PrintWriter pw = new PrintWriter(response.getOutputStream(), true);
		pw.println("servlet is up");
	}
	
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
		String action = request.getParameter("m");
		response.setContentType("text/html");
		PrintWriter pw = new PrintWriter(response.getOutputStream(), true);
		
		if (null == action) {
			LOG.severe("Error obtaining action");
			pw.println("Error obtaining action");
			return;
		}
		
		if ("person".equals(action)) {
			// create class dynamically
			doCreationClass(request, pw);
		} else if ("malperson".equals(action)) {
			// simulate a malicious person
			doMaliciousClass(request, pw);
		} else {
			LOG.warning("Invalid  action " + action);
			pw.println("Invalid  action " + action);
		}
	}
	
	private void doCreationClass(HttpServletRequest request, PrintWriter pw) throws IOException {		
		// Create the Person class dynamically
		ArrayList<Path> classpath = new ArrayList<>();
		classpath.add(Paths.get(this.getServletContext().getRealPath("WEB-INF/classes")));
		Parser<Person> p = new Parser<>(Person.class);
		Person person = p.compile(classpath);
		
		// Use jackson to load json into generated class
		ObjectMapper mapper = new ObjectMapper();
		mapper.readerForUpdating(person).readValue(request.getReader());
		
		// return the results of toString()
		pw.printf("%s%nName=%s, Address=%s%n",
				person.toString(), person.getName(), person.getAddress());
	}
	
	private void doMaliciousClass(HttpServletRequest request, PrintWriter pw) throws IOException {
		/* Here we use Jackson to load the class from the classpath
		 * This is dangerous as we could be loading a malicious class
		 * as the results will show
		 */
		
		ObjectMapper mapper = new ObjectMapper();
		Person person = mapper.readValue(request.getReader(), PersonImpl.class);
		
		// return the results of toString() where the malicious code resides
		pw.println(person.toString());
	}
}
