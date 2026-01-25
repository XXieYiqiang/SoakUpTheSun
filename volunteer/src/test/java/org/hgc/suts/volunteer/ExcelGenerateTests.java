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
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

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
            // 1. 定义上班时间窗口 (例如：早上 8:00 到 10:00)

            // 随机生成小时 (8 或 9)
            int startHour = faker.number().numberBetween(8, 10);
            // 随机生成分钟 (0 到 59)
            int startMinute = faker.number().numberBetween(0, 60);

            // 创建上班时间对象
            LocalTime startTime = LocalTime.of(startHour, startMinute);


            // 2. 定义工作时长窗口 (例如：8 小时 到 10 小时)
            // 为了方便计算，我们转换成总分钟数：
            // 8小时 = 480分钟
            // 10小时 = 600分钟

            // 随机生成工作时长（分钟）
            int minDuration = 480;
            int maxDuration = 600;

            // 随机生成时长，范围包含 480 到 600
            int durationMinutes = faker.number().numberBetween(minDuration, maxDuration + 1);

            // 3. 计算下班时间
            // 在上班时间的基础上加上随机生成的时长
            LocalTime endTime = startTime.plusMinutes(durationMinutes);


            // 4. 格式化输出 (只显示 HH:mm:ss)
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            ExcelGenerateDemoData data = ExcelGenerateDemoData.builder()
                    .name(faker.name().name())
                    .sex(faker.number().numberBetween(0, 2))
                    .phone(faker.phoneNumber().phoneNumber())
                    .birthday(faker.date().birthday(18, 65))
                    .location(faker.address().latitude()+","+faker.address().longitude())
                    .score(faker.number().randomDouble(1,0,5))
                    .startTime(startTime.format(formatter))
                    .entTime(endTime.format(formatter))
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

        @ColumnWidth(30)
        @ExcelProperty("开始时间")
        private String startTime;

        @ColumnWidth(30)
        @ExcelProperty("结束时间")
        private String entTime;

    }
}