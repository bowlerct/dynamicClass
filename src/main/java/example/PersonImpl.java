package example;

/**
 * <p>
 * This class is a malicious serializable class used to show what could happen
 * if classes like this are not protected against.
 * </p>
 * 
 * <p>
 * There are two ways deserialization attacks occur. First by overriding
 * {@link ObjectInputStream#readObject()}. Secondly by overriding methods common
 * to all classes; e.g toString, finalize, equals, etc. This class uses the
 * second approach by overriding toString().
 * </p>
 * 
 * @author Chris Koerner
 */
public class PersonImpl implements Person {

	private String name;
	private String address;
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getAddress() {
		return address;
	}

	@Override
	public void setAddress(String address) {
		this.address = address;
	}

	public String toString() {
		return "Maliciously I could have stolen or deleted information but chose not to.";
	}
}
