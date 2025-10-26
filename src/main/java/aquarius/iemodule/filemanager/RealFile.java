package aquarius.iemodule.filemanager;

import aquarius.iemodule.ReOpenableInputStream;

import java.io.*;

public class RealFile extends IEFile {
    private final File file;

    public RealFile(String filePath) {
        super(filePath);
        this.file = new File(filePath);
    }

    @Override
    public long size() {
        return file.length();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new BufferedInputStream(new FileInputStream(this.file));
    }

    @Override
    public OutputStream getOutputStream() throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file));
    }
}
