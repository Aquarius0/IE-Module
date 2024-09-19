package aquarius.iemodule.processors;

import java.util.List;

public class ProcessorResult<T> {
    private List<T> data;
    private boolean valid;

    public ProcessorResult(List<T> data, boolean valid) {
        this.data = data;
        this.valid = valid;
    }

    public List<T> getData() {
        return this.data;
    }

    public boolean isValid() {
        return this.valid;
    }
}