package aquarius.iemodule.exception;

public class InsecureFileException  extends IllegalStateException{


    public enum Type{
        CONTAINS_MACRO
    }

    private final Type type;


    public InsecureFileException(Type type) {
        super("File containing micros not allowed");
        this.type = type;
    }
}
