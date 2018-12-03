package example;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;

public class Example {

	public static void main(String[] args) throws JsonParseException, JsonMappingException, IOException {	
		// show malicious class
		Person person = new PersonImpl();
		person.setAddress("132 West Elm");
		person.setName("John Doe");
		System.out.printf("%s%n%n", person);
		
		// show class interface simple example
		List<Path> classPath = Arrays.asList(Paths.get("/guests/git_workspace/dynamicClass/bin/main"));
		Parser<Person> p = new Parser(Person.class);
		person = p.compile(classPath);
		person.setAddress("132 West Elm");
		person.setName("John Doe");
		System.out.println(person.toString());
		System.out.printf("Name: %s, Address: %s%n%n", 
				person.getName(), person.getAddress());
		
		// show class creation of interface with mixed primivtives
		Parser<Group> g = new Parser(Group.class);
		Group group = g.compile(classPath);
		group.setId(1000);
		group.setName("test");
		group.setActive(true);
		System.out.println(group.toString());
		System.out.printf("ID: %d, Name: %s, active=%s%n", 
				group.getId(), group.getName(), group.getActive());
	}
}
