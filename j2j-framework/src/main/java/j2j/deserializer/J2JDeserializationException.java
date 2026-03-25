package j2j.deserializer;

public class J2JDeserializationException extends RuntimeException {

    public J2JDeserializationException(String message) {
        super(message);
    }

    public J2JDeserializationException(String message, Throwable cause) {
        super(message, cause);
    }
}