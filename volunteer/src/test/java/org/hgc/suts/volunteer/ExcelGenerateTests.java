package org.hgc.suts.volunteer;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.util.ListUtils;
import com.github.javafaker.Faker;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Locale;

@DisplayName("easyExcel生成")
public final class ExcelGenerateTests {


    private final Faker faker = new Faker(Locale.CHINA);
    private final String excelPath = Paths.get("").toAbsolutePath().getParent() + "/tmp";

    @Test
    public void testExcelGenerate() {
        if (!FileUtil.exist(excelPath)) {
            FileUtil.mkdir(excelPath);
        }
        String fileName = excelPath + "/志愿者数量Excel.xlsx";
        EasyExcel.write(fileName, ExcelGenerateDemoData.class).sheet("志愿者保存列表").doWrite(data());
    }

    private List<ExcelGenerateDemoData> data() {
        List<ExcelGenerateDemoData> list = ListUtils.newArrayList();
        int writeNum = 10000;
        for (int i = 0; i < writeNum; i++) {
            ExcelGenerateDemoData data = ExcelGenerateDemoData.builder()
                    .name(faker.name().name())
                    .sex(faker.number().numberBetween(0, 1))
                    .phone(faker.phoneNumber().phoneNumber())
                    .birthday(faker.date().birthday(18, 65))
                    .location(faker.address().latitude()+","+faker.address().longitude())
                    .score(faker.number().randomDouble(1,0,5))
                    .build();
            list.add(data);
        }
        return list;
    }

    /**
     * Excel 生成器示例数据模型
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    static class ExcelGenerateDemoData {

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


    }
}