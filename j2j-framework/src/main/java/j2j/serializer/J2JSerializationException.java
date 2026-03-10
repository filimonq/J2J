package j2j.serializer;

public class J2JSerializationException extends RuntimeException {

    public J2JSerializationException(String message) {
        super(message);
    }

    public J2JSerializationException(String message, Throwable cause) {
        super(message, cause);
    }
}
