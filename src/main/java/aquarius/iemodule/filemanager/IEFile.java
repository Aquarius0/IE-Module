package aquarius.iemodule.filemanager;

import aquarius.iemodule.ReOpenableInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public abstract class IEFile {
    private final String filePath;

    public IEFile(String filePath) {
        this.filePath = filePath;
    }

    public String filePath(){
        return filePath;
    }

    public  abstract long size();

    public abstract InputStream getInputStream() throws IOException;

    public abstract OutputStream getOutputStream() throws IOException;



}
