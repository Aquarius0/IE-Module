package aquarius.iemodule.filemanager;

import aquarius.iemodule.ReOpenableInputStream;

import java.io.*;

public class InMemoryFile extends IEFile{
    private ByteArrayOutputStream byteArrayOutputStream;

    public InMemoryFile(String filePath) {
        super(filePath);
    }

    @Override
    public long size() {
        return this.byteArrayOutputStream.toByteArray().length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(this.byteArrayOutputStream.toByteArray());
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return byteArrayOutputStream;
    }
}
