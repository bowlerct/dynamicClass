package example;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

/**
 * <p>
 * Use reflection to obtain all get# and set# methods. These will constitute the
 * attributes. This class needs more work in several areas and should be
 * refactored into smaller cohesive units.
 * </p>
 * <p>
 * One area is parsing & writing as it does not account for quite a few cases.
 * One example is 'public boolean isPropertyName()'. It also relies on get
 * methods to obtain the classes attributes. This could be exteneded to
 * annotations on the Interface methods.
 * </p>
 * <p>
 * Another area is extending this concept beyond the most simple state.
 * Interfaces could have references to non-primitive types and extend
 * additional interfaces. A much better parser is needed here.
 * </p>
 * 
 * @author Chris Koerner
 * @param <T>
 */
public class Parser<T> {

	/* Patterns for parsing interface */
	private Pattern get = Pattern.compile("^get(.*)");
	private Pattern set = Pattern.compile("^set(.*)");
	
	private Class _interface = null;
	private T generatedClass;

	private Map<String, String> attributes = new HashMap<>();
	private List<Method> methods = new ArrayList<>();

	/**
	 * @param face
	 *            Interface or AbstractClass
	 */
	public Parser(Class face) {
		if (null == face || !isInterface(face))
			throw new java.lang.reflect.MalformedParametersException("Target class is not an Interface");
		_interface = face;
	}

	private boolean isInterface(Class face) {
		// make ensure object is an interface
		return (Modifier.isInterface(face.getModifiers()));
	}

	/**
	 * Obtains the attributes from get method names
	 */
	private void getAttributes() {
		Method[] methods = _interface.getDeclaredMethods();
		for (int i = 0; i < methods.length; i++) {
			// add method unless it is common to all classes. e.g. toString
			this.methods.add(methods[i]);
			
			// look for get methods only as we need the property's type
			Matcher m = get.matcher(methods[i].getName());
			if (m.find() && !attributes.containsKey(m.group(1))) {
				System.out.println(methods[i].getReturnType().getName() + " " + m.group(1) + " = " + methods[i].getName());
				attributes.put(m.group(1), methods[i].getReturnType().getName());
				continue;
			}
		}
	}

	/**
	 * @param _className
	 * @return generated .java file
	 * @throws IOException
	 */
	private File writeClass(String _className) throws IOException {

		// obtain interface attributes
		getAttributes();

		File sourceFile = new File(_className + ".java");
		// System.out.println("File Location: " + sourceFile.getAbsolutePath());
		FileWriter writer = new FileWriter(sourceFile);
		writer.write("package dynamicClass;");

		/* imports not needed as we fully qualifies all types used */

		/* write class beginning and attributes */
		writer.write(String.format("public class %s implements %s {", _className, _interface.getCanonicalName()));
		// write attributes
		for (String name : attributes.keySet()) {
			String type = attributes.get(name);
			writer.write(String.format("%s %s %s;", "private", type, name));
			System.out.println(String.format("%s %s %s;", "private", type, name));
		}

		/*
		 * write methods
		 */
		for (Method m : methods) {
			int mods = m.getModifiers();

			if (Modifier.isStatic(mods)) {
				// skip method as this class will inherit it
				continue;
			}

			String name = m.getName();
			String rtnType = m.getReturnType().getName();

			// only public methods are allowed on interfaces
			// and we are not allowing abstract since we do not
			// know how to implement it.
			String temp = String.format("public %s %s(", rtnType, name);

			int paramCount = 1;
			for (Class type : m.getParameterTypes()) {
				// System.out.println("Parameter Type: " + type.getName());
				temp += String.format("%s v%d, ", type.getName(), paramCount);
				paramCount++;
			}
			if (paramCount > 1) {
				// remove the trailing ', '
				temp = temp.substring(0, temp.length() - 2);
			}
			writer.write(temp);
			writer.write("){");

			//
			// determine how to write the method body
			Matcher matcher = set.matcher(name);

			if (matcher.find() && "void".equals(rtnType) && paramCount > 1) {
				// set method - which attribute applies
				String methodAttr = matcher.group(1);
				writer.write(String.format("%s = %s; }", methodAttr, "v1"));
			} else {
				matcher = get.matcher(name);

				if (matcher.find() && !"void".equals(rtnType) && paramCount == 1) {
					// get method
					String methodAttr = matcher.group(1);
					writer.write(String.format("return %s; }", methodAttr));
				}
			}
		}

		// close class
		writer.write("}");
		writer.close();

		return sourceFile;
	}

	/**
	 * Generates a class instance based on the interface provided to Parser.
	 * 
	 * @param classPath
	 *            classpaths to add. The first path found will be the directory
	 *            where new classes are stored. This is required as classes are
	 *            loaded using the default classloader.
	 * @return A Class instance
	 */
	public T compile(List<Path> classPath) {
		// create the *.java file
		String _className = _interface.getSimpleName() + "Impl";
		File sourceFile = null;

		//
		// create source file
		//
		try {
			sourceFile = writeClass(_className);
		} catch (IOException e) {
			// TODO throw a compile error
			e.printStackTrace();
			return null;
		}

		//
		// setup classpath
		//

		if (null == classPath || classPath.isEmpty()) {
			// TODO throw a compile error
			return null;
		}

		/*
		 * This is needed as class needs to be loaded by the same classloader that
		 * loaded the interface
		 */
		String buildDir = classPath.get(0).toString();

		List<File> cpfiles = new ArrayList<>();
		// add existing classpaths from the jvm so compilation will succeed
		String[] paths = System.getProperty("java.class.path").split(":");
		for (String s : paths) {
			cpfiles.add(Paths.get(s).toFile());
		}
		/*
		 * add all additional paths. May be duplicates from above so a rework here is
		 * needed
		 */
		classPath.forEach(path -> {
			cpfiles.add(path.toFile());
		});

		
		//
		// compile the source
		//

		javax.tools.JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
		StandardJavaFileManager manager = compiler.getStandardFileManager(null, null, null);

		try {
			manager.setLocation(StandardLocation.CLASS_PATH, cpfiles);
			manager.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(Paths.get(buildDir).toFile()));
		} catch (IOException e) {
			// TODO throw a compile error
			e.printStackTrace();
			return null;
		}
		Iterable<? extends JavaFileObject> compileUnits = manager
				.getJavaFileObjectsFromFiles(Arrays.asList(sourceFile));
		boolean success = compiler.getTask(null, manager, null, null, null, compileUnits).call();

		try {
			manager.close();
		} catch (IOException e) {
			// TODO throw a compile error
			e.printStackTrace();
		}

		// Now load and return an instance of the class
		if (success) {
			try {
				generatedClass = (T) this.getClass().getClassLoader().loadClass("dynamicClass." + _className)
						.newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {																				// e) {
				// TODO throw a compile error
				e.printStackTrace();
			}
		}

		return generatedClass;
	}
}
