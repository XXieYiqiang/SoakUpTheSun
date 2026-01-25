package org.hgc.suts.volunteer.service.impl.easyExcel.VolunteerPrizes;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

@Data
public class CreateVolunteerPrizesExcelObject {

    @ExcelProperty("志愿者ID")
    @ColumnWidth(20) // 可选：设置列宽
    private Long id;

    @ExcelProperty("姓名")
    @ColumnWidth(15)
    private String name;

    @ExcelProperty("手机号")
    @ColumnWidth(20)
    private String phone;

    @ExcelProperty("当前积分")
    private Double score;
}