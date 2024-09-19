package aquarius.iemodule;

import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;

public interface IEExcelStyle {
    void headerStyle(CellStyle style);
    void headerFont(Font font);

    void oddStyle(CellStyle style);
    void oddFont(Font font);

    void evenStyle(CellStyle style);
    void evenFont(Font font);


    void evenHypeLinkFont(Font font);
    void oddHypeLinkFont(Font font);

}
