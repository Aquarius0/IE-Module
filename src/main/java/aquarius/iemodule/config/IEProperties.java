package aquarius.iemodule.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "ie")
public class IEProperties {

    private String ieReportPath;

    private String ieTemplatePath;

    private String ieExportPath;

    private List<String> packageToScan;

    public List<String> getPackageToScan() {
        return packageToScan;
    }

    public void setPackageToScan(List<String> packageToScan) {
        this.packageToScan = packageToScan;
    }

    public String getIeReportPath() {
        return ieReportPath;
    }

    public void setIeReportPath(String ieReportPath) {
        this.ieReportPath = ieReportPath;
    }

    public String getIeTemplatePath() {
        return ieTemplatePath;
    }

    public void setIeTemplatePath(String ieTemplatePath) {
        this.ieTemplatePath = ieTemplatePath;
    }

    public String getIeExportPath() {
        return ieExportPath;
    }

    public void setIeExportPath(String ieExportPath) {
        this.ieExportPath = ieExportPath;
    }
}
