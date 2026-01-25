package org.hgc.suts.volunteer.mq.consumer.easyExcel.volunteerCreateExcel;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import lombok.Data;

import java.util.Date;


@Data
public class VolunteerExcelObject {
    @ColumnWidth(30)
    @ExcelProperty("姓名")
    private String name;

    @ColumnWidth(20)
    @ExcelProperty("性别")
    private int sex;


    @ColumnWidth(20)
    @ExcelProperty("手机号")
    private String phone;

    @ColumnWidth(20)
    @ExcelProperty("生日")
    private Date birthday;

    @ColumnWidth(30)
    @ExcelProperty("经纬度")
    private String location;

    @ColumnWidth(30)
    @ExcelProperty("评分")
    private Double score;

    @ColumnWidth(30)
    @ExcelProperty("开始时间")
    private String startTime;

    @ColumnWidth(30)
    @ExcelProperty("结束时间")
    private String endTime;
}
