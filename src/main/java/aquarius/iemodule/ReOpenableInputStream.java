package aquarius.iemodule;

import java.io.*;
import java.nio.file.Files;

public class ReOpenableInputStream  {
    protected final File file;

    public ReOpenableInputStream(File file) {
        this.file = file;
    }


    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(Files.newInputStream(file.toPath()));
    }
}